import fs from "fs";
import path from "path";
import { fileURLToPath } from "url";
const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const defaultSeedDir = path.join(__dirname, "..", "data", "seed");
function getDefaultCacheDir() {
    if (process.env.AVENRA_CATALOG_CACHE_DIR) {
        return process.env.AVENRA_CATALOG_CACHE_DIR;
    }
    const stateRoot = process.env.LOCALAPPDATA
        || process.env.XDG_STATE_HOME
        || path.join(process.env.HOME || process.cwd(), ".local", "state");
    return path.join(stateRoot, "Avenra", "catalog");
}
export class CatalogStorage {
    seedDir;
    cacheDir;
    cacheFile;
    constructor(seedDir, cacheDir) {
        this.seedDir = seedDir || defaultSeedDir;
        this.cacheDir = cacheDir || getDefaultCacheDir();
        this.cacheFile = path.join(this.cacheDir, "catalogCache.json");
    }
    saveCacheSnapshot(data) {
        try {
            if (!fs.existsSync(this.cacheDir)) {
                fs.mkdirSync(this.cacheDir, { recursive: true });
            }
            fs.writeFileSync(this.cacheFile, JSON.stringify(data, null, 2), "utf-8");
        }
        catch (e) {
            console.warn("[CatalogStorage] Could not save catalog cache snapshot:", e);
        }
    }
    loadCacheSnapshot() {
        try {
            if (fs.existsSync(this.cacheFile)) {
                const raw = fs.readFileSync(this.cacheFile, "utf-8");
                return JSON.parse(raw);
            }
            const legacyCache = path.join(__dirname, "..", "data", "cache", "catalogCache.json");
            if (fs.existsSync(legacyCache)) {
                const raw = fs.readFileSync(legacyCache, "utf-8");
                return JSON.parse(raw);
            }
        }
        catch (e) {
            console.warn("[CatalogStorage] Could not read catalog cache snapshot:", e);
        }
        return null;
    }
    loadSeedData() {
        try {
            const bannersData = JSON.parse(fs.readFileSync(path.join(this.seedDir, "banners.json"), "utf-8"));
            const categoriesData = JSON.parse(fs.readFileSync(path.join(this.seedDir, "categories.json"), "utf-8"));
            const productsData = JSON.parse(fs.readFileSync(path.join(this.seedDir, "products.json"), "utf-8"));
            return {
                banners: bannersData,
                categories: categoriesData,
                products: productsData,
            };
        }
        catch (e) {
            console.error("[CatalogStorage] Failed to load local seed data:", e);
            return null;
        }
    }
}
