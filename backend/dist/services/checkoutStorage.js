import fs from "fs";
import path from "path";
function getDefaultStorageDir() {
    if (process.env.AVENRA_CHECKOUT_STORAGE_DIR) {
        return process.env.AVENRA_CHECKOUT_STORAGE_DIR;
    }
    const stateRoot = process.env.LOCALAPPDATA
        || process.env.XDG_STATE_HOME
        || path.join(process.env.HOME || process.cwd(), ".local", "state");
    return path.join(stateRoot, "Avenra", "checkout");
}
export class CheckoutStorage {
    storageDir;
    stateFile;
    constructor(customStorageDir) {
        this.storageDir = customStorageDir || getDefaultStorageDir();
        this.stateFile = path.join(this.storageDir, "orders.json");
    }
    getStorageDir() {
        return this.storageDir;
    }
    getStateFile() {
        return this.stateFile;
    }
    loadState() {
        try {
            if (!fs.existsSync(this.stateFile))
                return null;
            const raw = fs.readFileSync(this.stateFile, "utf-8");
            return JSON.parse(raw);
        }
        catch {
            throw new Error("Unable to load persisted checkout state safely.");
        }
    }
    saveState(state) {
        if (!fs.existsSync(this.storageDir)) {
            fs.mkdirSync(this.storageDir, { recursive: true });
        }
        const tempFile = `${this.stateFile}.tmp`;
        fs.writeFileSync(tempFile, JSON.stringify(state, null, 2), "utf-8");
        fs.renameSync(tempFile, this.stateFile);
    }
}
