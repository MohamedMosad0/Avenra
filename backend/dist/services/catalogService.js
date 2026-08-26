import { ExternalDataProvider } from "../providers/externalDataProvider.js";
import { AppError } from "../utils/error.js";
import { config } from "../config/index.js";
import { CatalogStorage } from "./catalogStorage.js";
export class CatalogService {
    externalDataProvider;
    catalogStorage;
    products = [];
    categories = [];
    banners = [];
    isInitialized = false;
    constructor(externalDataProvider, catalogStorage) {
        this.externalDataProvider = externalDataProvider || new ExternalDataProvider();
        this.catalogStorage = catalogStorage || new CatalogStorage();
    }
    async initialize() {
        if (this.isInitialized)
            return;
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
    getHomeData() {
        return {
            banners: this.banners,
            categories: this.categories,
            featuredProducts: this.products.filter((p) => p.isAvailable).slice(0, 6)
        };
    }
    getCategories() {
        return this.categories;
    }
    getProducts(categoryId, query) {
        let result = [...this.products];
        if (categoryId) {
            result = result.filter((p) => p.categoryId === categoryId);
        }
        if (query && query.trim().length > 0) {
            const q = query.trim().toLowerCase();
            result = result.filter((p) => p.title.toLowerCase().includes(q) || p.description.toLowerCase().includes(q));
        }
        return result;
    }
    getProductById(productId) {
        return this.products.find((p) => p.id === productId);
    }
    reserveStock(items) {
        const quantities = this.aggregateQuantities(items);
        for (const [productId, quantity] of quantities) {
            const product = this.getProductById(productId);
            if (!product || !product.isAvailable || product.availableQuantity < quantity) {
                throw new AppError(409, "OUT_OF_STOCK", "A requested product is no longer available in the requested quantity.");
            }
        }
        for (const [productId, quantity] of quantities) {
            const product = this.getProductById(productId);
            product.availableQuantity -= quantity;
            product.isAvailable = product.availableQuantity > 0;
        }
    }
    releaseStock(items) {
        for (const [productId, quantity] of this.aggregateQuantities(items)) {
            const product = this.getProductById(productId);
            if (product) {
                product.availableQuantity += quantity;
                product.isAvailable = true;
            }
        }
    }
    applyReservedStock(reservedQuantities) {
        for (const [productId, quantity] of reservedQuantities) {
            const product = this.getProductById(productId);
            if (product && quantity > 0) {
                product.availableQuantity = Math.max(0, product.availableQuantity - quantity);
                product.isAvailable = product.availableQuantity > 0;
            }
        }
    }
    getCatalogSummary() {
        return {
            totalCategories: this.categories.length,
            totalProducts: this.products.length,
            totalBanners: this.banners.length,
            providerName: this.externalDataProvider.name
        };
    }
    aggregateQuantities(items) {
        const quantities = new Map();
        for (const item of items) {
            quantities.set(item.productId, (quantities.get(item.productId) || 0) + item.quantity);
        }
        return quantities;
    }
    resolveLocalAssetUrls(data) {
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
    resolveAssetUrl(imageUrl) {
        if (!imageUrl.startsWith("/assets/"))
            return imageUrl;
        return `${config.baseUrl.replace(/\/$/, "")}${imageUrl}`;
    }
}
export const catalogService = new CatalogService();
