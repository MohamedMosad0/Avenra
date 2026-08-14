import { Banner, Category, Product } from "../models/index.js";
import { ExternalDataProvider, IExternalDataProvider, NormalizedData } from "../providers/externalDataProvider.js";
import { AppError } from "../utils/error.js";
import { config } from "../config/index.js";
import { CatalogStorage } from "./catalogStorage.js";

export class CatalogService {
  private externalDataProvider: IExternalDataProvider;
  private catalogStorage: CatalogStorage;
  private products: Product[] = [];
  private categories: Category[] = [];
  private banners: Banner[] = [];
  private isInitialized = false;

  constructor(
    externalDataProvider?: IExternalDataProvider,
    catalogStorage?: CatalogStorage
  ) {
    this.externalDataProvider = externalDataProvider || new ExternalDataProvider();
    this.catalogStorage = catalogStorage || new CatalogStorage();
  }

  public async initialize(): Promise<void> {
    if (this.isInitialized) return;

    // 1. Primary: Try External Data Provider
    const normalized = await this.externalDataProvider.fetchNormalizedData();
    if (normalized && normalized.products.length > 0 && normalized.categories.length > 0) {
      const resolved = this.resolveLocalAssetUrls(normalized);
      this.banners = resolved.banners;
      this.categories = resolved.categories;
      this.products = resolved.products;
      this.catalogStorage.saveCacheSnapshot(normalized);
      this.isInitialized = true;
      return;
    }

    // 2. Fallback: Local Cache Snapshot
    const cachedData = this.catalogStorage.loadCacheSnapshot();
    if (cachedData && cachedData.products.length > 0) {
      console.log("[CatalogService] Serving catalog from local cache snapshot.");
      const resolved = this.resolveLocalAssetUrls(cachedData);
      this.banners = resolved.banners;
      this.categories = resolved.categories;
      this.products = resolved.products;
      this.isInitialized = true;
      return;
    }

    // 3. Fallback: Local Seed Data
    console.log("[CatalogService] Serving catalog from repository local seed data.");
    const seedData = this.catalogStorage.loadSeedData();
    if (seedData) {
      const resolved = this.resolveLocalAssetUrls(seedData);
      this.banners = resolved.banners;
      this.categories = resolved.categories;
      this.products = resolved.products;
    }
    this.isInitialized = true;
  }

  public getHomeData(): { banners: Banner[]; categories: Category[]; featuredProducts: Product[] } {
    return {
      banners: this.banners,
      categories: this.categories,
      featuredProducts: this.products.filter((p) => p.isAvailable).slice(0, 6)
    };
  }

  public getCategories(): Category[] {
    return this.categories;
  }

  public getProducts(categoryId?: string, query?: string): Product[] {
    let result = [...this.products];

    if (categoryId) {
      result = result.filter((p) => p.categoryId === categoryId);
    }

    if (query && query.trim().length > 0) {
      const q = query.trim().toLowerCase();
      result = result.filter(
        (p) => p.title.toLowerCase().includes(q) || p.description.toLowerCase().includes(q)
      );
    }

    return result;
  }

  public getProductById(productId: string): Product | undefined {
    return this.products.find((p) => p.id === productId);
  }

  public reserveStock(items: ReadonlyArray<{ productId: string; quantity: number }>): void {
    const quantities = this.aggregateQuantities(items);

    for (const [productId, quantity] of quantities) {
      const product = this.getProductById(productId);
      if (!product || !product.isAvailable || product.availableQuantity < quantity) {
        throw new AppError(409, "OUT_OF_STOCK", "A requested product is no longer available in the requested quantity.");
      }
    }

    for (const [productId, quantity] of quantities) {
      const product = this.getProductById(productId)!;
      product.availableQuantity -= quantity;
      product.isAvailable = product.availableQuantity > 0;
    }
  }

  public releaseStock(items: ReadonlyArray<{ productId: string; quantity: number }>): void {
    for (const [productId, quantity] of this.aggregateQuantities(items)) {
      const product = this.getProductById(productId);
      if (product) {
        product.availableQuantity += quantity;
        product.isAvailable = true;
      }
    }
  }

  public applyReservedStock(reservedQuantities: ReadonlyMap<string, number>): void {
    for (const [productId, quantity] of reservedQuantities) {
      const product = this.getProductById(productId);
      if (product && quantity > 0) {
        product.availableQuantity = Math.max(0, product.availableQuantity - quantity);
        product.isAvailable = product.availableQuantity > 0;
      }
    }
  }

  public getCatalogSummary(): { totalCategories: number; totalProducts: number; totalBanners: number; providerName: string } {
    return {
      totalCategories: this.categories.length,
      totalProducts: this.products.length,
      totalBanners: this.banners.length,
      providerName: this.externalDataProvider.name
    };
  }

  private aggregateQuantities(items: ReadonlyArray<{ productId: string; quantity: number }>): Map<string, number> {
    const quantities = new Map<string, number>();
    for (const item of items) {
      quantities.set(item.productId, (quantities.get(item.productId) || 0) + item.quantity);
    }
    return quantities;
  }

  private resolveLocalAssetUrls(data: NormalizedData): NormalizedData {
    return {
      banners: data.banners.map((banner) => ({ ...banner, imageUrl: this.resolveAssetUrl(banner.imageUrl) })),
      categories: data.categories.map((category) => ({ ...category, imageUrl: this.resolveAssetUrl(category.imageUrl) })),
      products: data.products.map((product) => ({
        ...product,
        imageUrl: this.resolveAssetUrl(product.imageUrl),
        galleryImages: product.galleryImages.map((imageUrl) => this.resolveAssetUrl(imageUrl)),
      })),
    };
  }

  private resolveAssetUrl(imageUrl: string): string {
    if (!imageUrl.startsWith("/assets/")) return imageUrl;
    return `${config.baseUrl.replace(/\/$/, "")}${imageUrl}`;
  }
}

export const catalogService = new CatalogService();

