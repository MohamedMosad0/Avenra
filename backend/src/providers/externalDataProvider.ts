import { Banner, Category, Product, Subcategory } from "../models/index.js";
import { config } from "../config/index.js";

export interface NormalizedData {
  banners: Banner[];
  categories: Category[];
  products: Product[];
}

export interface IExternalDataProvider {
  readonly name: string;
  fetchNormalizedData(): Promise<NormalizedData | null>;
}

export class ExternalDataProvider implements IExternalDataProvider {
  public readonly name = "ExternalDataProvider";
  private baseUrl: string;

  constructor(baseUrl?: string) {
    this.baseUrl = baseUrl || config.upstreamApiBaseUrl || process.env.UPSTREAM_API_BASE_URL || "";
  }

  public async fetchNormalizedData(): Promise<NormalizedData | null> {
    if (!this.baseUrl) {
      console.warn(`[${this.name}] UPSTREAM_API_BASE_URL is not configured. Falling back to local data.`);
      return null;
    }

    try {
      const [categoriesRes, subcategoriesRes, productsRes] = await Promise.allSettled([
        fetch(`${this.baseUrl}/categories`, { signal: AbortSignal.timeout(config.upstreamRequestTimeoutMs) }),
        fetch(`${this.baseUrl}/subcategories`, { signal: AbortSignal.timeout(config.upstreamRequestTimeoutMs) }),
        fetch(`${this.baseUrl}/products?limit=100`, { signal: AbortSignal.timeout(config.upstreamRequestTimeoutMs) })
      ]);

      if (categoriesRes.status !== "fulfilled" || !categoriesRes.value.ok) {
        console.warn(`[${this.name}] Failed to fetch categories from upstream provider.`);
        return null;
      }

      if (productsRes.status !== "fulfilled" || !productsRes.value.ok) {
        console.warn(`[${this.name}] Failed to fetch products from upstream provider.`);
        return null;
      }

      const categoriesPayload = (await categoriesRes.value.json()) as any;
      const rawCategories: any[] = categoriesPayload?.data || [];

      let rawSubcategories: any[] = [];
      if (subcategoriesRes.status === "fulfilled" && subcategoriesRes.value.ok) {
        const subPayload = (await subcategoriesRes.value.json()) as any;
        rawSubcategories = subPayload?.data || [];
      }

      const productsPayload = (await productsRes.value.json()) as any;
      const rawProducts: any[] = productsPayload?.data || [];

      if (!rawCategories.length || !rawProducts.length) {
        console.warn(`[${this.name}] Upstream provider returned empty payload.`);
        return null;
      }

      // Map Subcategories
      const subcategoriesMap = new Map<string, Subcategory[]>();
      rawSubcategories.forEach((sub) => {
        const catId = typeof sub.category === "object" ? sub.category?._id || sub.category?.id : sub.category;
        if (catId && (sub._id || sub.id) && sub.name) {
          const item: Subcategory = {
            id: sub._id || sub.id,
            name: sub.name,
            categoryId: catId
          };
          const list = subcategoriesMap.get(catId) || [];
          list.push(item);
          subcategoriesMap.set(catId, list);
        }
      });

      // Preserve the upstream category artwork instead of substituting local demo assets.
      const categories: Category[] = rawCategories.map((cat) => {
        const catId = cat._id || cat.id;
        return {
          id: catId,
          name: cat.name || "Category",
          imageUrl: typeof cat.image === "string" ? cat.image : "",
          subcategories: subcategoriesMap.get(catId) || []
        };
      });

      const categoryIds = new Set(categories.map((c) => c.id));

      // Map & Normalize Products
      const seenProductIds = new Set<string>();
      const products: Product[] = [];

      for (let i = 0; i < rawProducts.length; i++) {
        const raw = rawProducts[i];
        const prodId = raw._id || raw.id;
        if (!prodId || seenProductIds.has(prodId)) continue;

        const rawCat = raw.category;
        const catId = typeof rawCat === "object" ? rawCat?._id || rawCat?.id : rawCat;
        const assignedCatId = (catId && categoryIds.has(catId)) ? catId : (categories[0] ? categories[0].id : "cat-1");

        const price = Number(raw.price) || 100;
        const rawDiscount = Number(raw.priceAfterDiscount);
        const discountPrice = (rawDiscount > 0 && rawDiscount < price) ? rawDiscount : undefined;

        const product: Product = {
          id: prodId,
          title: raw.title || "E-Commerce Product",
          description: raw.description || raw.title || "High quality product from our catalog.",
          price: price,
          discountPrice: discountPrice,
          imageUrl: typeof raw.imageCover === "string" ? raw.imageCover : "",
          galleryImages: Array.isArray(raw.images)
            ? raw.images.filter((image: unknown): image is string => typeof image === "string")
            : [],
          rating: Number(raw.ratingsAverage) || 4.5,
          reviewCount: Number(raw.ratingsQuantity) || 12,
          categoryId: assignedCatId,
          isAvailable: (raw.quantity === undefined || Number(raw.quantity) > 0),
          availableQuantity: Number(raw.quantity) || 50,
          sizes: ["S", "M", "L", "XL"],
          colors: ["Black", "White", "Navy"]
        };

        seenProductIds.add(prodId);
        products.push(product);
      }

      // Relative asset paths are resolved to this backend's configured public URL by CatalogService.
      const banners: Banner[] = [
        {
          id: "banner-1",
          title: "Summer Fashion Sale",
          subtitle: "Up to 30% OFF on Top Collections",
          imageUrl: "/assets/images/banners/banner_fashion.jpg",
          targetCategoryId: categories[0]?.id
        },
        {
          id: "banner-2",
          title: "Electronics & Tech Essentials",
          subtitle: "Discover high-performance gear & audio",
          imageUrl: "/assets/images/banners/banner_audio.jpg",
          targetCategoryId: categories[1]?.id
        }
      ];

      return {
        banners,
        categories,
        products
      };
    } catch (error) {
      console.warn(`[${this.name}] Exception while fetching upstream data:`, error);
      return null;
    }
  }
}
