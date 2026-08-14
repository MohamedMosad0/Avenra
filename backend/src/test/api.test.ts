import { Server } from "http";
import fs from "fs";
import path from "path";
import { fileURLToPath } from "url";

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const runtimeStorageDir = path.join(__dirname, "temp_runtime_storage");

fs.rmSync(runtimeStorageDir, { recursive: true, force: true });
process.env.AVENRA_CHECKOUT_STORAGE_DIR = path.join(runtimeStorageDir, "checkout");
process.env.AVENRA_AUTH_STORAGE_DIR = path.join(runtimeStorageDir, "auth");

const { createApp } = await import("../app.js");
const { CheckoutService } = await import("../services/checkoutService.js");
const { catalogService } = await import("../services/catalogService.js");
const { hashToken } = await import("../utils/auth.js");

async function runTests() {
  console.log("--- Starting Avenra Comprehensive API Integration Test Suite ---");
  const app = createApp();

  const server: Server = app.listen(3002);
  const baseUrl = "http://localhost:3002/v1";

  function assertErrorResponse(data: any, expectedCode: string, stepName: string) {
    if (data.status !== "error" || data.code !== expectedCode || typeof data.message !== "string") {
      throw new Error(`[${stepName}] Inconsistent error response structure: ${JSON.stringify(data)}, expected code: ${expectedCode}`);
    }
  }

  try {
    // 0. GET /v1/health
    console.log("Test 0: GET /v1/health");
    const resHealth = await fetch(`${baseUrl}/health`);
    if (resHealth.status !== 200) throw new Error(`Health failed with status ${resHealth.status}`);
    const dataHealth = (await resHealth.json()) as any;
    if (dataHealth.status !== "ok") throw new Error(`Health expected { status: "ok" }, got ${JSON.stringify(dataHealth)}`);
    console.log("  PASSED");

    // 1. GET /v1/home
    console.log("Test 1: GET /v1/home");
    const resHome = await fetch(`${baseUrl}/home`);
    if (resHome.status !== 200) throw new Error(`Home failed with status ${resHome.status}`);
    const dataHome = (await resHome.json()) as any;
    if (!dataHome.banners || !dataHome.categories || !dataHome.featuredProducts) {
      throw new Error("Home payload missing required keys");
    }
    if (!Array.isArray(dataHome.banners) || !Array.isArray(dataHome.categories) || !Array.isArray(dataHome.featuredProducts)) {
      throw new Error("Home payload keys must be arrays");
    }
    console.log("  PASSED");

    // 2. GET /v1/categories
    console.log("Test 2: GET /v1/categories");
    const resCat = await fetch(`${baseUrl}/categories`);
    if (resCat.status !== 200) throw new Error(`Categories failed with status ${resCat.status}`);
    const dataCat = (await resCat.json()) as any;
    if (!Array.isArray(dataCat.categories) || dataCat.categories.length === 0) {
      throw new Error("Categories array empty");
    }
    const sampleCategory = dataCat.categories[0];
    console.log("  PASSED");

    // 3. GET /v1/products (Full catalog, Category filter, & Search filter)
    console.log("Test 3: GET /v1/products (Full, Category Filter, and Search)");
    const resProd = await fetch(`${baseUrl}/products`);
    if (resProd.status !== 200) throw new Error(`Products failed with status ${resProd.status}`);
    const dataProd = (await resProd.json()) as any;
    if (!Array.isArray(dataProd.products) || dataProd.products.length === 0) {
      throw new Error("Products list empty");
    }
    const sampleProduct = dataProd.products[0];

    // Category filter
    const resCatFilter = await fetch(`${baseUrl}/products?categoryId=${encodeURIComponent(sampleCategory.id)}`);
    if (resCatFilter.status !== 200) throw new Error("Category filter query failed");
    const dataCatFilter = (await resCatFilter.json()) as any;
    if (!Array.isArray(dataCatFilter.products)) throw new Error("Category filter did not return products array");
    for (const p of dataCatFilter.products) {
      if (p.categoryId !== sampleCategory.id) throw new Error(`Product ${p.id} categoryId ${p.categoryId} did not match filter ${sampleCategory.id}`);
    }

    // Search filter
    const searchKeyword = sampleProduct.title.split(" ")[0];
    const resSearch = await fetch(`${baseUrl}/products?q=${encodeURIComponent(searchKeyword)}`);
    const dataSearch = (await resSearch.json()) as any;
    if (!Array.isArray(dataSearch.products) || dataSearch.products.length === 0) {
      throw new Error("Search filter failed");
    }
    console.log("  PASSED");

    // 4. GET /v1/products/:productId (Success & 404)
    console.log("Test 4: GET /v1/products/:productId (and 404 test)");
    const resDetail = await fetch(`${baseUrl}/products/${sampleProduct.id}`);
    if (resDetail.status !== 200) throw new Error("Product detail failed");
    const dataDetail = (await resDetail.json()) as any;
    if (!dataDetail.product || dataDetail.product.id !== sampleProduct.id) {
      throw new Error("Product detail returned incorrect product");
    }

    const res404 = await fetch(`${baseUrl}/products/invalid-id-xyz`);
    if (res404.status !== 404) throw new Error(`Expected 404 got ${res404.status}`);
    const data404 = (await res404.json()) as any;
    assertErrorResponse(data404, "PRODUCT_NOT_FOUND", "Product 404");
    console.log("  PASSED");

    // Create primary test user
    const authRes = await fetch(`${baseUrl}/auth/signup`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        fullName: "Checkout Test User",
        email: `checkout_${Date.now()}@avenra.com`,
        password: "CheckoutPassword123",
      }),
    });
    if (authRes.status !== 201) throw new Error("Checkout test authentication failed");
    const authData = (await authRes.json()) as any;
    const authHeaders = {
      "Content-Type": "application/json",
      Authorization: `Bearer ${authData.token}`,
    };

    // Create secondary test user for IDOR testing
    const authRes2 = await fetch(`${baseUrl}/auth/signup`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        fullName: "Secondary Test User",
        email: `secondary_${Date.now()}@avenra.com`,
        password: "SecondaryPassword123",
      }),
    });
    const authData2 = (await authRes2.json()) as any;
    const authHeaders2 = {
      "Content-Type": "application/json",
      Authorization: `Bearer ${authData2.token}`,
    };

    const baseAddress = {
      fullName: "Jane Doe",
      phone: "01010000000",
      city: "Cairo",
      addressLine: "456 Market St",
    };

    // 5. Checkout Quote Validation & Pricing Calculation
    console.log("Test 5: Checkout Quote Validation (Unauthenticated, Empty Items, Missing Address, Invalid Product, Out of Stock, and Calculations)");

    // 5.1 Unauthenticated Quote
    const unauthenticatedQuote = await fetch(`${baseUrl}/checkout/quotes`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({}),
    });
    if (unauthenticatedQuote.status !== 401) {
      throw new Error(`Expected unauthenticated quote to return 401, got ${unauthenticatedQuote.status}`);
    }
    assertErrorResponse(await unauthenticatedQuote.json(), "UNAUTHORIZED", "Unauthenticated Quote");

    // 5.2 Empty Items Array
    const emptyItemsQuote = await fetch(`${baseUrl}/checkout/quotes`, {
      method: "POST",
      headers: authHeaders,
      body: JSON.stringify({ items: [], shippingAddress: baseAddress }),
    });
    if (emptyItemsQuote.status !== 400) throw new Error("Expected empty items quote to return 400");
    assertErrorResponse(await emptyItemsQuote.json(), "VALIDATION_ERROR", "Empty Items Quote");

    // 5.3 Missing Shipping Address
    const missingAddressQuote = await fetch(`${baseUrl}/checkout/quotes`, {
      method: "POST",
      headers: authHeaders,
      body: JSON.stringify({
        items: [{ productId: sampleProduct.id, quantity: 1 }],
        shippingAddress: { fullName: "", phone: "", city: "", addressLine: "" },
      }),
    });
    if (missingAddressQuote.status !== 400) throw new Error("Expected missing address quote to return 400");
    assertErrorResponse(await missingAddressQuote.json(), "VALIDATION_ERROR", "Missing Address Quote");

    // 5.4 Invalid Product in Quote
    const invalidProdQuote = await fetch(`${baseUrl}/checkout/quotes`, {
      method: "POST",
      headers: authHeaders,
      body: JSON.stringify({
        items: [{ productId: "nonexistent_prod_999", quantity: 1 }],
        shippingAddress: baseAddress,
      }),
    });
    if (invalidProdQuote.status !== 404) throw new Error("Expected invalid product quote to return 404");
    assertErrorResponse(await invalidProdQuote.json(), "PRODUCT_NOT_FOUND", "Invalid Product Quote");

    // 5.5 Out of Stock in Quote
    const outOfStockQuote = await fetch(`${baseUrl}/checkout/quotes`, {
      method: "POST",
      headers: authHeaders,
      body: JSON.stringify({
        items: [{ productId: sampleProduct.id, quantity: 99999 }],
        shippingAddress: baseAddress,
      }),
    });
    if (outOfStockQuote.status !== 409) throw new Error("Expected out of stock quote to return 409");
    assertErrorResponse(await outOfStockQuote.json(), "OUT_OF_STOCK", "Out of Stock Quote");

    // 5.6 Quantity validation (positive integer requirement)
    const invalidQuantities = [1.5, 0.5, 0, -1, -5, "2", NaN, null, undefined];
    for (const invalidQty of invalidQuantities) {
      const resInvalidQty = await fetch(`${baseUrl}/checkout/quotes`, {
        method: "POST",
        headers: authHeaders,
        body: JSON.stringify({
          items: [{ productId: sampleProduct.id, quantity: invalidQty }],
          shippingAddress: baseAddress,
          deliveryMethod: "STANDARD",
        }),
      });
      const dataInvalidQty = (await resInvalidQty.json()) as any;
      if (resInvalidQty.status !== 400 || dataInvalidQty.code !== "VALIDATION_ERROR") {
        throw new Error(`Expected 400 VALIDATION_ERROR for quantity ${invalidQty}, got ${resInvalidQty.status}`);
      }
    }

    // 5.7 Valid Standard Delivery Quote (50 EGP fee)
    const resStandard = await fetch(`${baseUrl}/checkout/quotes`, {
      method: "POST",
      headers: authHeaders,
      body: JSON.stringify({
        items: [{ productId: sampleProduct.id, quantity: 1 }],
        shippingAddress: baseAddress,
        deliveryMethod: "STANDARD",
      }),
    });
    const quoteStandard = (await resStandard.json()) as any;
    if (quoteStandard.deliveryMethod !== "STANDARD" || quoteStandard.deliveryFee !== 50.0) {
      throw new Error("STANDARD delivery calculation failed");
    }
    const expectedStandardTotal = (sampleProduct.discountPrice || sampleProduct.price) + 50.0;
    if (quoteStandard.finalTotal !== expectedStandardTotal) {
      throw new Error(`Unexpected standard final total: ${quoteStandard.finalTotal}, expected: ${expectedStandardTotal}`);
    }
    if (quoteStandard.tax !== undefined || quoteStandard.taxes !== undefined || quoteStandard.taxRate !== undefined) {
      throw new Error("Tax fields illegally detected in checkout quote payload!");
    }

    // 5.8 Valid Express Delivery Quote (100 EGP fee)
    const resExpress = await fetch(`${baseUrl}/checkout/quotes`, {
      method: "POST",
      headers: authHeaders,
      body: JSON.stringify({
        items: [{ productId: sampleProduct.id, quantity: 1 }],
        shippingAddress: baseAddress,
        deliveryMethod: "EXPRESS",
      }),
    });
    const quoteExpress = (await resExpress.json()) as any;
    if (quoteExpress.deliveryMethod !== "EXPRESS" || quoteExpress.deliveryFee !== 100.0) {
      throw new Error("EXPRESS delivery calculation failed");
    }
    const expectedExpressTotal = (sampleProduct.discountPrice || sampleProduct.price) + 100.0;
    if (quoteExpress.finalTotal !== expectedExpressTotal) {
      throw new Error(`Unexpected express final total: ${quoteExpress.finalTotal}, expected: ${expectedExpressTotal}`);
    }
    console.log("  PASSED");

    // 6. Order Creation & Validation (Missing Key, Missing Quote, Cross-User IDOR, MOCK_CARD & CASH_ON_DELIVERY)
    console.log("Test 6: Order Creation & Validation (Missing Key, Missing Quote, Cross-User IDOR, MOCK_CARD, and CASH_ON_DELIVERY)");

    // 6.1 Missing Idempotency-Key
    const missingIdempotencyKey = await fetch(`${baseUrl}/orders`, {
      method: "POST",
      headers: authHeaders,
      body: JSON.stringify({
        quoteId: quoteStandard.quoteId,
        mockPaymentMethod: "MOCK_CARD",
        shippingAddress: baseAddress,
      }),
    });
    if (missingIdempotencyKey.status !== 400) {
      throw new Error(`Expected missing Idempotency-Key to return 400, got ${missingIdempotencyKey.status}`);
    }
    assertErrorResponse(await missingIdempotencyKey.json(), "VALIDATION_ERROR", "Missing Idempotency-Key Order");

    // 6.2 Missing Quote ID
    const missingQuoteId = await fetch(`${baseUrl}/orders`, {
      method: "POST",
      headers: { ...authHeaders, "Idempotency-Key": "idemp_no_quote" },
      body: JSON.stringify({
        mockPaymentMethod: "MOCK_CARD",
        shippingAddress: baseAddress,
      }),
    });
    if (missingQuoteId.status !== 400) throw new Error("Expected missing quote ID order to return 400");
    assertErrorResponse(await missingQuoteId.json(), "VALIDATION_ERROR", "Missing Quote ID Order");

    // 6.3 Cross-User Quote IDOR Attack
    const crossUserOrder = await fetch(`${baseUrl}/orders`, {
      method: "POST",
      headers: { ...authHeaders2, "Idempotency-Key": "idemp_idor_attempt" },
      body: JSON.stringify({
        quoteId: quoteStandard.quoteId,
        mockPaymentMethod: "MOCK_CARD",
        shippingAddress: baseAddress,
      }),
    });
    if (crossUserOrder.status !== 400) throw new Error("Expected cross-user quote order to return 400");
    assertErrorResponse(await crossUserOrder.json(), "VALIDATION_ERROR", "Cross-User Quote Order");

    // 6.4 Successful MOCK_CARD Order
    const resOrderCard = await fetch(`${baseUrl}/orders`, {
      method: "POST",
      headers: {
        ...authHeaders,
        "Idempotency-Key": "idemp_card_001",
      },
      body: JSON.stringify({
        quoteId: quoteStandard.quoteId,
        mockPaymentMethod: "MOCK_CARD",
        shippingAddress: baseAddress,
      }),
    });
    if (resOrderCard.status !== 201) throw new Error(`MOCK_CARD order failed with status ${resOrderCard.status}`);
    const orderCard = (await resOrderCard.json()) as any;
    if (orderCard.mockPaymentMethod !== "MOCK_CARD" || orderCard.mockPaymentStatus !== "SIMULATED_SUCCESS" || orderCard.status !== "CONFIRMED") {
      throw new Error("MOCK_CARD payment status failed to yield SIMULATED_SUCCESS / CONFIRMED");
    }
    if (orderCard.deliveryMethod !== "STANDARD" || orderCard.deliveryFee !== 50.0) {
      throw new Error("Order snapshot failed to preserve deliveryMethod and deliveryFee");
    }

    // 6.5 Successful CASH_ON_DELIVERY Order
    const resOrderCod = await fetch(`${baseUrl}/orders`, {
      method: "POST",
      headers: {
        ...authHeaders,
        "Idempotency-Key": "idemp_cod_002",
      },
      body: JSON.stringify({
        quoteId: quoteExpress.quoteId,
        mockPaymentMethod: "CASH_ON_DELIVERY",
        shippingAddress: baseAddress,
      }),
    });
    if (resOrderCod.status !== 201) throw new Error(`CASH_ON_DELIVERY order failed with status ${resOrderCod.status}`);
    const orderCod = (await resOrderCod.json()) as any;
    if (orderCod.mockPaymentMethod !== "CASH_ON_DELIVERY" || orderCod.mockPaymentStatus !== "SIMULATED_SUCCESS") {
      throw new Error("CASH_ON_DELIVERY payment status failed to yield SIMULATED_SUCCESS");
    }
    if (orderCod.deliveryMethod !== "EXPRESS" || orderCod.deliveryFee !== 100.0) {
      throw new Error("Order snapshot failed to preserve EXPRESS delivery settings");
    }
    console.log("  PASSED");

    // 7. Idempotency Key Handling (Replay & Conflict)
    console.log("Test 7: Idempotency Key Handling (Exact Replay and Conflict 409)");
    
    // Exact Replay
    const resIdempRetry = await fetch(`${baseUrl}/orders`, {
      method: "POST",
      headers: {
        ...authHeaders,
        "Idempotency-Key": "idemp_card_001",
      },
      body: JSON.stringify({
        quoteId: quoteStandard.quoteId,
        mockPaymentMethod: "MOCK_CARD",
        shippingAddress: baseAddress,
      }),
    });
    const orderIdempRetry = (await resIdempRetry.json()) as any;
    if (orderIdempRetry.orderId !== orderCard.orderId) {
      throw new Error("Idempotency key failed to return identical order");
    }

    // Same Key with Modified Request Payload -> 409 DUPLICATE_REQUEST
    const resModifiedKey = await fetch(`${baseUrl}/orders`, {
      method: "POST",
      headers: {
        ...authHeaders,
        "Idempotency-Key": "idemp_card_001",
      },
      body: JSON.stringify({
        quoteId: quoteStandard.quoteId,
        mockPaymentMethod: "CASH_ON_DELIVERY", // modified payment method
        shippingAddress: baseAddress,
      }),
    });
    if (resModifiedKey.status !== 409) throw new Error(`Expected 409 DUPLICATE_REQUEST on modified request, got ${resModifiedKey.status}`);
    assertErrorResponse(await resModifiedKey.json(), "DUPLICATE_REQUEST", "Modified Idempotency Request");
    console.log("  PASSED");

    // 8. Price Change Protection
    console.log("Test 8: Price Change Protection (409 PRICE_CHANGED)");
    const priceChangeQuoteRes = await fetch(`${baseUrl}/checkout/quotes`, {
      method: "POST",
      headers: authHeaders,
      body: JSON.stringify({
        items: [{ productId: sampleProduct.id, quantity: 1 }],
        shippingAddress: baseAddress,
        deliveryMethod: "STANDARD",
      }),
    });
    const priceChangeQuote = (await priceChangeQuoteRes.json()) as any;

    // Simulate price modification in catalog service
    const targetProduct = catalogService.getProductById(sampleProduct.id)!;
    const originalPrice = targetProduct.price;
    targetProduct.price += 100;
    try {
      const orderPriceChangeRes = await fetch(`${baseUrl}/orders`, {
        method: "POST",
        headers: { ...authHeaders, "Idempotency-Key": "idemp_price_change_001" },
        body: JSON.stringify({
          quoteId: priceChangeQuote.quoteId,
          mockPaymentMethod: "CASH_ON_DELIVERY",
          shippingAddress: baseAddress,
        }),
      });
      if (orderPriceChangeRes.status !== 409) throw new Error(`Expected 409 PRICE_CHANGED, got ${orderPriceChangeRes.status}`);
      assertErrorResponse(await orderPriceChangeRes.json(), "PRICE_CHANGED", "Price Change Order");
    } finally {
      targetProduct.price = originalPrice;
    }
    console.log("  PASSED");

    // 9. Quote Expiration Protection
    console.log("Test 9: Quote Expiration Protection (409 QUOTE_EXPIRED)");
    const testCheckoutService = new CheckoutService(runtimeStorageDir);
    const expiredQuote = testCheckoutService.createQuote({
      items: [{ productId: sampleProduct.id, quantity: 1 }],
      shippingAddress: baseAddress,
      deliveryMethod: "STANDARD",
    }, authData.user.id);

    // Manually force quote expiry timestamp into the past
    expiredQuote.quoteExpiry = new Date(Date.now() - 60 * 1000).toISOString();
    try {
      testCheckoutService.createOrder({
        quoteId: expiredQuote.quoteId,
        mockPaymentMethod: "CASH_ON_DELIVERY",
        shippingAddress: baseAddress,
      }, "idemp_expired_001", authData.user.id, hashToken(authData.token));
      throw new Error("Expected createOrder to fail for expired quote");
    } catch (err: any) {
      if (err.code !== "QUOTE_EXPIRED" || err.statusCode !== 409) {
        throw new Error(`Expected 409 QUOTE_EXPIRED, got ${err.statusCode} / ${err.code}`);
      }
    }
    console.log("  PASSED");

    // 10. Stock Reservation & Overselling Protection
    console.log("Test 10: Stock Reservation & Overselling Protection");
    const stockProduct = dataProd.products.find((product: any) => product.id !== sampleProduct.id && product.availableQuantity > 0);
    if (!stockProduct) throw new Error("No independent in-stock product available for stock test");
    const availableQty = stockProduct.availableQuantity;

    const fullStockQuoteRes = await fetch(`${baseUrl}/checkout/quotes`, {
      method: "POST",
      headers: authHeaders,
      body: JSON.stringify({
        items: [{ productId: stockProduct.id, quantity: availableQty }],
        shippingAddress: baseAddress,
        deliveryMethod: "STANDARD",
      }),
    });
    const fullStockQuote = (await fullStockQuoteRes.json()) as any;
    const fullStockOrderRes = await fetch(`${baseUrl}/orders`, {
      method: "POST",
      headers: { ...authHeaders, "Idempotency-Key": "idemp_stock_001" },
      body: JSON.stringify({
        quoteId: fullStockQuote.quoteId,
        mockPaymentMethod: "CASH_ON_DELIVERY",
        shippingAddress: baseAddress,
      }),
    });
    if (fullStockOrderRes.status !== 201) throw new Error("Stock reservation order failed");

    // Product stock should now be 0
    const oversellQuoteRes = await fetch(`${baseUrl}/checkout/quotes`, {
      method: "POST",
      headers: authHeaders,
      body: JSON.stringify({
        items: [{ productId: stockProduct.id, quantity: 1 }],
        shippingAddress: baseAddress,
        deliveryMethod: "STANDARD",
      }),
    });
    if (oversellQuoteRes.status !== 409) throw new Error(`Expected oversell quote to return 409, got ${oversellQuoteRes.status}`);
    assertErrorResponse(await oversellQuoteRes.json(), "OUT_OF_STOCK", "Oversell Quote");
    console.log("  PASSED");

    // 11. Persistence and Restart Safe Idempotency
    console.log("Test 11: Persistence and Restart Safe Idempotency");
    const restartStorageDir = path.join(__dirname, "temp_checkout_storage");
    fs.rmSync(restartStorageDir, { recursive: true, force: true });
    try {
      const restartRequest = {
        quoteId: "",
        mockPaymentMethod: "CASH_ON_DELIVERY" as const,
        shippingAddress: baseAddress,
      };
      const serviceBeforeRestart = new CheckoutService(restartStorageDir);
      const restartQuote = serviceBeforeRestart.createQuote({
        items: [{ productId: sampleProduct.id, quantity: 1 }],
        shippingAddress: baseAddress,
        deliveryMethod: "STANDARD",
      }, authData.user.id);
      restartRequest.quoteId = restartQuote.quoteId;
      const sessionHash = hashToken(authData.token);
      const persistedOrder = serviceBeforeRestart.createOrder(restartRequest, "idemp_restart_001", authData.user.id, sessionHash);

      // Simulate service restart by creating a new CheckoutService reading from the same directory
      const serviceAfterRestart = new CheckoutService(restartStorageDir);
      const replayedOrder = serviceAfterRestart.createOrder(restartRequest, "idemp_restart_001", authData.user.id, sessionHash);
      if (replayedOrder.orderId !== persistedOrder.orderId) {
        throw new Error("Restart replay created a duplicate order");
      }
    } finally {
      fs.rmSync(restartStorageDir, { recursive: true, force: true });
    }
    console.log("  PASSED");

    console.log("--- ALL COMPREHENSIVE INTEGRATION TESTS PASSED SUCCESSFULLY! ---");
  } catch (err) {
    console.error("Test execution failed:", err);
    process.exitCode = 1;
  } finally {
    server.close();
    fs.rmSync(runtimeStorageDir, { recursive: true, force: true });
  }
}

runTests();

