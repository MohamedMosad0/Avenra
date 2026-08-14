import crypto from "crypto";

const KEY_LENGTH = 64;

/**
 * Securely hashes a plaintext password using crypto.scryptSync with a unique salt.
 * Stored format: <salt_hex>:<derived_key_hex>
 */
export function hashPassword(password: string): string {
  const salt = crypto.randomBytes(16).toString("hex");
  const derivedKey = crypto.scryptSync(password, salt, KEY_LENGTH);
  return `${salt}:${derivedKey.toString("hex")}`;
}

/**
 * Validates a plaintext password against a stored salted hash using timingSafeEqual.
 */
export function verifyPassword(password: string, storedHash: string): boolean {
  if (!storedHash || !storedHash.includes(":")) {
    return false;
  }

  const [salt, originalKeyHex] = storedHash.split(":");
  if (!salt || !originalKeyHex) {
    return false;
  }

  const originalKeyBuffer = Buffer.from(originalKeyHex, "hex");
  const derivedKeyBuffer = crypto.scryptSync(password, salt, originalKeyBuffer.length);

  if (originalKeyBuffer.length !== derivedKeyBuffer.length) {
    return false;
  }

  return crypto.timingSafeEqual(originalKeyBuffer, derivedKeyBuffer);
}

/**
 * Generates a cryptographically strong random token for session management.
 */
export function generateToken(): string {
  return "tok_" + crypto.randomBytes(24).toString("hex");
}

/**
 * Produces the value retained server-side for a bearer token.
 */
export function hashToken(token: string): string {
  return crypto.createHash("sha256").update(token, "utf8").digest("hex");
}
