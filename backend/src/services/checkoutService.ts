import crypto from "crypto";
import {
  CheckoutQuote,
  CheckoutQuoteRequest,
  CreateOrderRequest,
  DeliveryMethod,
  Order,
  QuoteItemSnapshot,
} from "../models/index.js";
import { AppError } from "../utils/error.js";
import { CatalogService, catalogService as defaultCatalogService } from "./catalogService.js";
import { CheckoutStorage, PersistedCheckoutState } from "./checkoutStorage.js";

interface StoredQuote {
  quote: CheckoutQuote;
  userId: string;
}

interface IdempotencyRecord {
  orderId: string;
  requestHash: string;
}

const MAX_ACTIVE_QUOTES = 1_000;
const COMPLETED_ORDER_RETENTION_MS = 30 * 24 * 60 * 60 * 1000;
const MAX_RETAINED_ORDERS = 10_000;

export class CheckoutService {
  private catalogService: CatalogService;
  private storage: CheckoutStorage;
  private quotes = new Map<string, StoredQuote>();
  private orders = new Map<string, Order>();
  private idempotencyKeys = new Map<string, IdempotencyRecord>();
  private reservedQuantities = new Map<string, number>();
  private stockReconciled = false;

  constructor(
    catalogServiceOrStorageDir?: CatalogService | string,
    customStorageOrDir?: CheckoutStorage | string,
  ) {
    if (typeof catalogServiceOrStorageDir === "string") {
      this.catalogService = defaultCatalogService;
      this.storage = new CheckoutStorage(catalogServiceOrStorageDir);
    } else if (catalogServiceOrStorageDir instanceof CatalogService) {
      this.catalogService = catalogServiceOrStorageDir;
      if (typeof customStorageOrDir === "string") {
        this.storage = new CheckoutStorage(customStorageOrDir);
      } else if (customStorageOrDir instanceof CheckoutStorage) {
        this.storage = customStorageOrDir;
      } else {
        this.storage = new CheckoutStorage();
      }
    } else {
      this.catalogService = defaultCatalogService;
      if (typeof customStorageOrDir === "string") {
        this.storage = new CheckoutStorage(customStorageOrDir);
      } else if (customStorageOrDir instanceof CheckoutStorage) {
        this.storage = customStorageOrDir;
      } else {
        this.storage = new CheckoutStorage();
      }
    }

    this.loadState();
    if (this.prunePersistedState()) {
      this.persistState();
    }
  }

  public createQuote(request: CheckoutQuoteRequest, userId: string): CheckoutQuote {
    this.purgeExpiredQuotes();
    if (this.prunePersistedState()) {
      this.persistState();
    }
    this.reconcileReservedStock();

    if (!request.items || !Array.isArray(request.items) || request.items.length === 0) {
      throw new AppError(400, "VALIDATION_ERROR", "Items array must not be empty.");
    }

    if (!request.shippingAddress || !request.shippingAddress.fullName || !request.shippingAddress.phone || !request.shippingAddress.city || !request.shippingAddress.addressLine) {
      throw new AppError(400, "VALIDATION_ERROR", "Complete shipping address is required.");
    }

    const deliveryMethod: DeliveryMethod = request.deliveryMethod === "EXPRESS" ? "EXPRESS" : "STANDARD";
    const deliveryFee = deliveryMethod === "EXPRESS" ? 100.0 : 50.0;

    let itemSubtotal = 0;
    let discountTotal = 0;
    const snapshots: QuoteItemSnapshot[] = [];

    for (const itemReq of request.items) {
      if (
        !itemReq ||
        typeof itemReq.quantity !== "number" ||
        !Number.isFinite(itemReq.quantity) ||
        !Number.isInteger(itemReq.quantity) ||
        itemReq.quantity <= 0
      ) {
        throw new AppError(400, "VALIDATION_ERROR", "Item quantity must be a positive integer.");
      }

      const product = this.catalogService.getProductById(itemReq.productId);
      if (!product) {
        throw new AppError(404, "PRODUCT_NOT_FOUND", `Product with ID '${itemReq.productId}' not found.`);
      }

      if (!product.isAvailable || product.availableQuantity < itemReq.quantity) {
        throw new AppError(
          409,
          "OUT_OF_STOCK",
          `Product '${product.title}' has insufficient stock (Requested: ${itemReq.quantity}, Available: ${product.availableQuantity}).`
        );
      }

      const unitPrice = product.discountPrice ?? product.price;
      const originalSubtotal = product.price * itemReq.quantity;
      const effectiveSubtotal = unitPrice * itemReq.quantity;
      const discountAmount = (product.price - unitPrice) * itemReq.quantity;

      itemSubtotal += originalSubtotal;
      discountTotal += discountAmount;

      snapshots.push({
        productId: product.id,
        title: product.title,
        unitPrice,
        quantity: itemReq.quantity,
        totalPrice: effectiveSubtotal,
      });
    }

    const quoteId = `quote_${Date.now()}_${Math.random().toString(36).substring(2, 7)}`;
    const quote: CheckoutQuote = {
      quoteId,
      items: snapshots,
      itemSubtotal,
      discountTotal,
      deliveryFee,
      finalTotal: itemSubtotal - discountTotal + deliveryFee,
      currency: "EGP",
      quoteExpiry: new Date(Date.now() + 15 * 60 * 1000).toISOString(),
      deliveryMethod,
    };

    this.quotes.set(quoteId, { quote, userId });
    this.enforceQuoteLimit();
    return quote;
  }

  public getQuote(quoteId: string): CheckoutQuote | undefined {
    return this.quotes.get(quoteId)?.quote;
  }

  public createOrder(
    request: CreateOrderRequest,
    idempotencyKey: string | undefined,
    userId: string,
    sessionTokenHash: string,
  ): Order {
    if (this.prunePersistedState()) {
      this.persistState();
    }
    if (!request.quoteId) {
      throw new AppError(400, "VALIDATION_ERROR", "Quote ID is required to create an order.");
    }
    if (!idempotencyKey?.trim()) {
      throw new AppError(400, "VALIDATION_ERROR", "Idempotency-Key is required to create an order.");
    }

    this.reconcileReservedStock();

    const scopeKey = this.createScopeKey(userId, sessionTokenHash, idempotencyKey);
    const requestHash = this.createRequestHash(request);
    const existingRecord = this.idempotencyKeys.get(scopeKey);
    if (existingRecord) {
      if (existingRecord.requestHash !== requestHash) {
        throw new AppError(409, "DUPLICATE_REQUEST", "Idempotency-Key was already used for a different order request.");
      }
      const existingOrder = this.orders.get(existingRecord.orderId);
      if (existingOrder) return existingOrder;
    }

    const storedQuote = this.quotes.get(request.quoteId);
    if (!storedQuote || storedQuote.userId !== userId) {
      throw new AppError(400, "VALIDATION_ERROR", "Invalid or unknown Quote ID.");
    }

    const quote = storedQuote.quote;
    if (new Date() > new Date(quote.quoteExpiry)) {
      this.quotes.delete(request.quoteId);
      throw new AppError(409, "QUOTE_EXPIRED", "The checkout quote has expired. Please request a new quote.");
    }

    for (const item of quote.items) {
      const product = this.catalogService.getProductById(item.productId);
      if (!product || !product.isAvailable || product.availableQuantity < item.quantity) {
        throw new AppError(409, "OUT_OF_STOCK", `Product '${item.title}' is no longer available in the requested quantity.`);
      }
      if ((product.discountPrice ?? product.price) !== item.unitPrice) {
        throw new AppError(409, "PRICE_CHANGED", `The price for product '${item.title}' has changed. Please generate a new checkout quote.`);
      }
    }

    const order: Order = {
      orderId: `ord_${Date.now()}_${Math.random().toString(36).substring(2, 7)}`,
      orderReference: `AVN-${Math.floor(100000 + Math.random() * 900000)}`,
      quoteId: quote.quoteId,
      items: quote.items,
      itemSubtotal: quote.itemSubtotal,
      discountTotal: quote.discountTotal,
      deliveryMethod: quote.deliveryMethod,
      deliveryFee: quote.deliveryFee,
      finalTotal: quote.finalTotal,
      currency: quote.currency,
      shippingAddress: request.shippingAddress,
      mockPaymentMethod: request.mockPaymentMethod || "CASH_ON_DELIVERY",
      mockPaymentStatus: "SIMULATED_SUCCESS",
      status: "CONFIRMED",
      createdAt: new Date().toISOString(),
    };

    this.catalogService.reserveStock(quote.items);
    this.reserveQuantities(quote.items);
    this.orders.set(order.orderId, order);
    this.idempotencyKeys.set(scopeKey, { orderId: order.orderId, requestHash });

    try {
      this.persistState();
    } catch (error) {
      this.idempotencyKeys.delete(scopeKey);
      this.orders.delete(order.orderId);
      this.releaseQuantities(quote.items);
      this.catalogService.releaseStock(quote.items);
      throw new AppError(503, "SERVICE_UNAVAILABLE", "Unable to persist order safely. Please retry with the same Idempotency-Key.");
    }

    return order;
  }

  private loadState(): void {
    const state = this.storage.loadState();
    if (!state) return;

    for (const order of state.orders || []) {
      this.orders.set(order.orderId, order);
    }
    for (const record of state.idempotencyRecords || []) {
      this.idempotencyKeys.set(record.scopeKey, {
        orderId: record.orderId,
        requestHash: record.requestHash,
      });
    }
    for (const [productId, quantity] of Object.entries(state.reservedQuantities || {})) {
      if (quantity > 0) {
        this.reservedQuantities.set(productId, quantity);
      }
    }
  }

  private persistState(): void {
    const state: PersistedCheckoutState = {
      orders: Array.from(this.orders.values()),
      idempotencyRecords: Array.from(this.idempotencyKeys.entries()).map(([scopeKey, record]) => ({
        scopeKey,
        ...record,
      })),
      reservedQuantities: Object.fromEntries(this.reservedQuantities),
    };
    this.storage.saveState(state);
  }

  private reconcileReservedStock(): void {
    if (this.stockReconciled) return;
    this.catalogService.applyReservedStock(this.reservedQuantities);
    this.stockReconciled = true;
  }

  private purgeExpiredQuotes(): void {
    const now = Date.now();
    for (const [quoteId, storedQuote] of this.quotes) {
      if (new Date(storedQuote.quote.quoteExpiry).getTime() <= now) {
        this.quotes.delete(quoteId);
      }
    }
  }

  private enforceQuoteLimit(): void {
    if (this.quotes.size <= MAX_ACTIVE_QUOTES) return;

    const excessQuotes = Array.from(this.quotes.entries())
      .sort(([, left], [, right]) =>
        new Date(left.quote.quoteExpiry).getTime() - new Date(right.quote.quoteExpiry).getTime()
      )
      .slice(0, this.quotes.size - MAX_ACTIVE_QUOTES);
    for (const [quoteId] of excessQuotes) {
      this.quotes.delete(quoteId);
    }
  }

  private prunePersistedState(): boolean {
    const now = Date.now();
    const retainedOrders = Array.from(this.orders.values())
      .filter((order) => {
        const createdAt = new Date(order.createdAt).getTime();
        return Number.isFinite(createdAt) && now - createdAt <= COMPLETED_ORDER_RETENTION_MS;
      })
      .sort((left, right) => new Date(left.createdAt).getTime() - new Date(right.createdAt).getTime());

    const boundedOrders = retainedOrders.slice(Math.max(0, retainedOrders.length - MAX_RETAINED_ORDERS));
    const retainedOrderIds = new Set(boundedOrders.map((order) => order.orderId));
    var changed = retainedOrderIds.size !== this.orders.size;

    if (changed) {
      this.orders.clear();
      for (const order of boundedOrders) this.orders.set(order.orderId, order);
    }

    for (const [scopeKey, record] of this.idempotencyKeys) {
      if (!retainedOrderIds.has(record.orderId)) {
        this.idempotencyKeys.delete(scopeKey);
        changed = true;
      }
    }
    return changed;
  }

  private reserveQuantities(items: ReadonlyArray<{ productId: string; quantity: number }>): void {
    for (const item of items) {
      this.reservedQuantities.set(item.productId, (this.reservedQuantities.get(item.productId) || 0) + item.quantity);
    }
  }

  private releaseQuantities(items: ReadonlyArray<{ productId: string; quantity: number }>): void {
    for (const item of items) {
      const remaining = (this.reservedQuantities.get(item.productId) || 0) - item.quantity;
      if (remaining > 0) this.reservedQuantities.set(item.productId, remaining);
      else this.reservedQuantities.delete(item.productId);
    }
  }

  private createScopeKey(userId: string, sessionTokenHash: string, idempotencyKey: string): string {
    return crypto.createHash("sha256").update(`${userId}:${sessionTokenHash}:${idempotencyKey.trim()}`).digest("hex");
  }

  private createRequestHash(request: CreateOrderRequest): string {
    return crypto.createHash("sha256").update(JSON.stringify({
      quoteId: request.quoteId,
      mockPaymentMethod: request.mockPaymentMethod || "CASH_ON_DELIVERY",
      shippingAddress: request.shippingAddress,
    })).digest("hex");
  }
}

export const checkoutService = new CheckoutService();

