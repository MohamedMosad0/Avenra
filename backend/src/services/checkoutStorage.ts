import fs from "fs";
import path from "path";
import { Order } from "../models/index.js";

export interface PersistedIdempotencyRecord {
  orderId: string;
  requestHash: string;
  scopeKey: string;
}

export interface PersistedCheckoutState {
  orders: Order[];
  idempotencyRecords: PersistedIdempotencyRecord[];
  reservedQuantities: Record<string, number>;
}

function getDefaultStorageDir(): string {
  if (process.env.AVENRA_CHECKOUT_STORAGE_DIR) {
    return process.env.AVENRA_CHECKOUT_STORAGE_DIR;
  }

  const stateRoot = process.env.LOCALAPPDATA
    || process.env.XDG_STATE_HOME
    || path.join(process.env.HOME || process.cwd(), ".local", "state");
  return path.join(stateRoot, "Avenra", "checkout");
}

export class CheckoutStorage {
  private storageDir: string;
  private stateFile: string;

  constructor(customStorageDir?: string) {
    this.storageDir = customStorageDir || getDefaultStorageDir();
    this.stateFile = path.join(this.storageDir, "orders.json");
  }

  public getStorageDir(): string {
    return this.storageDir;
  }

  public getStateFile(): string {
    return this.stateFile;
  }

  public loadState(): PersistedCheckoutState | null {
    try {
      if (!fs.existsSync(this.stateFile)) return null;
      const raw = fs.readFileSync(this.stateFile, "utf-8");
      return JSON.parse(raw) as PersistedCheckoutState;
    } catch {
      throw new Error("Unable to load persisted checkout state safely.");
    }
  }

  public saveState(state: PersistedCheckoutState): void {
    if (!fs.existsSync(this.storageDir)) {
      fs.mkdirSync(this.storageDir, { recursive: true });
    }

    const tempFile = `${this.stateFile}.tmp`;
    fs.writeFileSync(tempFile, JSON.stringify(state, null, 2), "utf-8");
    fs.renameSync(tempFile, this.stateFile);
  }
}
