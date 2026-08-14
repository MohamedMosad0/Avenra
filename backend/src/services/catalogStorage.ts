import fs from "fs";
import path from "path";
import { fileURLToPath } from "url";
import { Banner, Category, Product } from "../models/index.js";
import { NormalizedData } from "../providers/externalDataProvider.js";

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const defaultSeedDir = path.join(__dirname, "..", "data", "seed");
const defaultCacheDir = path.join(__dirname, "..", "data", "cache");

export class CatalogStorage {
  private seedDir: string;
  private cacheDir: string;
  private cacheFile: string;

  constructor(seedDir?: string, cacheDir?: string) {
    this.seedDir = seedDir || defaultSeedDir;
    this.cacheDir = cacheDir || defaultCacheDir;
    this.cacheFile = path.join(this.cacheDir, "catalogCache.json");
  }

  public saveCacheSnapshot(data: NormalizedData): void {
    try {
      if (!fs.existsSync(this.cacheDir)) {
        fs.mkdirSync(this.cacheDir, { recursive: true });
      }
      fs.writeFileSync(this.cacheFile, JSON.stringify(data, null, 2), "utf-8");
    } catch (e) {
      console.warn("[CatalogStorage] Could not save catalog cache snapshot:", e);
    }
  }

  public loadCacheSnapshot(): NormalizedData | null {
    try {
      if (fs.existsSync(this.cacheFile)) {
        const raw = fs.readFileSync(this.cacheFile, "utf-8");
        return JSON.parse(raw);
      }
    } catch (e) {
      console.warn("[CatalogStorage] Could not read catalog cache snapshot:", e);
    }
    return null;
  }

  public loadSeedData(): NormalizedData | null {
    try {
      const bannersData: Banner[] = JSON.parse(fs.readFileSync(path.join(this.seedDir, "banners.json"), "utf-8"));
      const categoriesData: Category[] = JSON.parse(fs.readFileSync(path.join(this.seedDir, "categories.json"), "utf-8"));
      const productsData: Product[] = JSON.parse(fs.readFileSync(path.join(this.seedDir, "products.json"), "utf-8"));

      return {
        banners: bannersData,
        categories: categoriesData,
        products: productsData,
      };
    } catch (e) {
      console.error("[CatalogStorage] Failed to load local seed data:", e);
      return null;
    }
  }
}
