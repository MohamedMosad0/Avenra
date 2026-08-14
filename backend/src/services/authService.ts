import fs from "fs";
import path from "path";
import { fileURLToPath } from "url";
import crypto from "crypto";
import {
  AuthResponse,
  AuthSession,
  SignInRequest,
  SignUpRequest,
  User,
  UserRecord,
} from "../models/index.js";
import { generateToken, hashPassword, hashToken, verifyPassword } from "../utils/auth.js";
import { AppError } from "../utils/error.js";

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const legacyStorageDir = path.join(__dirname, "..", "data", "storage");

function getDefaultStorageDir(): string {
  if (process.env.AVENRA_AUTH_STORAGE_DIR) {
    return process.env.AVENRA_AUTH_STORAGE_DIR;
  }

  const stateRoot = process.env.LOCALAPPDATA
    || process.env.XDG_STATE_HOME
    || path.join(process.env.HOME || process.cwd(), ".local", "state");
  return path.join(stateRoot, "Avenra", "auth");
}

const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const SESSION_EXPIRY_DAYS = 30;
const MINIMUM_PASSWORD_LENGTH = 10;
const MAX_SIGN_IN_FAILURES = 5;
const SIGN_IN_WINDOW_MS = 15 * 60 * 1000;
const MAX_SIGN_IN_ATTEMPT_ENTRIES = 10_000;

interface SignInAttempt {
  failureCount: number;
  windowStartedAt: number;
  blockedUntil?: number;
}

export class AuthService {
  private users: Map<string, UserRecord> = new Map(); // id -> UserRecord
  private emailIndex: Map<string, string> = new Map(); // normalized email -> id
  private sessions: Map<string, AuthSession> = new Map(); // token hash -> AuthSession
  private signInAttempts: Map<string, SignInAttempt> = new Map();
  private isLoaded = false;

  constructor(private customStorageDir?: string) {
    this.initializeStorage();
  }

  private getStoragePaths() {
    const dir = this.customStorageDir || getDefaultStorageDir();
    return {
      dir,
      users: path.join(dir, "users.json"),
      sessions: path.join(dir, "sessions.json")
    };
  }

  public initializeStorage(): void {
    if (this.isLoaded) return;

    const paths = this.getStoragePaths();

    try {
      if (!fs.existsSync(paths.dir)) {
        fs.mkdirSync(paths.dir, { recursive: true });
      }

      this.migrateLegacyUsers(paths);

      if (fs.existsSync(paths.users)) {
        const raw = fs.readFileSync(paths.users, "utf-8");
        const list: UserRecord[] = JSON.parse(raw);
        for (const u of list) {
          this.users.set(u.id, u);
          this.emailIndex.set(u.email.toLowerCase(), u.id);
        }
      }

      if (fs.existsSync(paths.sessions)) {
        const raw = fs.readFileSync(paths.sessions, "utf-8");
        const list: AuthSession[] = JSON.parse(raw);
        for (const s of list) {
          if (s.tokenHash) {
            this.sessions.set(s.tokenHash, s);
          }
        }
      }

      if (this.pruneExpiredSessions()) {
        this.persistSessions();
      }

      this.isLoaded = true;
    } catch (error) {
      console.error("[AuthService] Error loading storage files:", error);
      this.isLoaded = true;
    }
  }

  private migrateLegacyUsers(paths: ReturnType<AuthService["getStoragePaths"]>): void {
    if (this.customStorageDir || !fs.existsSync(legacyStorageDir)) {
      return;
    }

    const legacyUsersFile = path.join(legacyStorageDir, "users.json");
    const legacySessionsFile = path.join(legacyStorageDir, "sessions.json");

    if (!fs.existsSync(paths.users) && fs.existsSync(legacyUsersFile)) {
      fs.copyFileSync(legacyUsersFile, paths.users);
    }

    // Legacy sessions contained plaintext bearer tokens. Do not migrate them.
    if (fs.existsSync(legacySessionsFile)) {
      fs.rmSync(legacySessionsFile);
    }
    if (fs.existsSync(legacyUsersFile) && fs.existsSync(paths.users)) {
      fs.rmSync(legacyUsersFile);
    }
    if (fs.existsSync(legacyStorageDir) && fs.readdirSync(legacyStorageDir).length === 0) {
      fs.rmdirSync(legacyStorageDir);
    }
  }

  private persistUsers(): void {
    const paths = this.getStoragePaths();
    try {
      if (!fs.existsSync(paths.dir)) {
        fs.mkdirSync(paths.dir, { recursive: true });
      }
      const data = Array.from(this.users.values());
      fs.writeFileSync(paths.users, JSON.stringify(data, null, 2), "utf-8");
    } catch (error) {
      console.error("[AuthService] Failed to persist users:", error);
    }
  }

  private persistSessions(): void {
    const paths = this.getStoragePaths();
    try {
      if (!fs.existsSync(paths.dir)) {
        fs.mkdirSync(paths.dir, { recursive: true });
      }
      const data = Array.from(this.sessions.values());
      fs.writeFileSync(paths.sessions, JSON.stringify(data, null, 2), "utf-8");
    } catch (error) {
      console.error("[AuthService] Failed to persist sessions:", error);
    }
  }

  public signUp(request: SignUpRequest): AuthResponse {
    if (
      !request ||
      typeof request.fullName !== "string" ||
      typeof request.email !== "string" ||
      typeof request.password !== "string"
    ) {
      throw new AppError(400, "VALIDATION_ERROR", "Full name, email, and password must be valid strings.");
    }

    if (request.mobileNumber !== undefined && request.mobileNumber !== null && typeof request.mobileNumber !== "string") {
      throw new AppError(400, "VALIDATION_ERROR", "Mobile number must be a string.");
    }

    if (request.address !== undefined && request.address !== null && typeof request.address !== "string") {
      throw new AppError(400, "VALIDATION_ERROR", "Address must be a string.");
    }

    const fullName = request.fullName.trim();
    const email = request.email.trim().toLowerCase();
    const password = request.password;
    const mobileNumber = request.mobileNumber?.trim() || "";
    const address = request.address?.trim() || "";

    if (!fullName || fullName.length < 2) {
      throw new AppError(400, "VALIDATION_ERROR", "Full name must be at least 2 characters.");
    }

    if (!email || !EMAIL_REGEX.test(email)) {
      throw new AppError(400, "VALIDATION_ERROR", "A valid email address is required.");
    }

    if (!password || password.length < MINIMUM_PASSWORD_LENGTH) {
      throw new AppError(400, "VALIDATION_ERROR", `Password must be at least ${MINIMUM_PASSWORD_LENGTH} characters.`);
    }

    if (this.emailIndex.has(email)) {
      throw new AppError(409, "EMAIL_ALREADY_EXISTS", "An account with this email already exists.");
    }

    const userId = "usr_" + crypto.randomBytes(12).toString("hex");
    const passwordHash = hashPassword(password);
    const now = new Date().toISOString();

    const userRecord: UserRecord = {
      id: userId,
      fullName,
      email,
      passwordHash,
      mobileNumber,
      address,
      createdAt: now
    };

    this.users.set(userId, userRecord);
    this.emailIndex.set(email, userId);
    this.persistUsers();

    const token = this.createSession(userId);

    return {
      user: this.toPublicUser(userRecord),
      token
    };
  }

  public signIn(request: SignInRequest, clientAddress = "unknown"): AuthResponse {
    if (
      !request ||
      typeof request.email !== "string" ||
      typeof request.password !== "string"
    ) {
      throw new AppError(400, "VALIDATION_ERROR", "Email and password are required.");
    }

    const email = request.email.trim().toLowerCase();
    const password = request.password;

    if (!email || !password) {
      throw new AppError(400, "VALIDATION_ERROR", "Email and password are required.");
    }

    this.pruneSignInAttempts();
    const attemptKey = `${clientAddress}:${email}`;
    this.ensureSignInAllowed(attemptKey);

    const userId = this.emailIndex.get(email);
    if (!userId) {
      this.recordFailedSignIn(attemptKey);
      throw this.invalidCredentialsError();
    }

    const userRecord = this.users.get(userId);
    if (!userRecord || !verifyPassword(password, userRecord.passwordHash)) {
      this.recordFailedSignIn(attemptKey);
      throw this.invalidCredentialsError();
    }

    this.signInAttempts.delete(attemptKey);

    const token = this.createSession(userId);

    return {
      user: this.toPublicUser(userRecord),
      token
    };
  }

  public getUserByToken(token: string): User | null {
    if (!token) return null;
    if (this.pruneExpiredSessions()) {
      this.persistSessions();
    }
    const tokenHash = hashToken(token);
    const session = this.sessions.get(tokenHash);
    if (!session) return null;

    if (new Date(session.expiresAt) < new Date()) {
      this.sessions.delete(tokenHash);
      this.persistSessions();
      return null;
    }

    const userRecord = this.users.get(session.userId);
    return userRecord ? this.toPublicUser(userRecord) : null;
  }

  public revokeToken(token: string): void {
    const pruned = this.pruneExpiredSessions();
    const revoked = this.sessions.delete(hashToken(token));
    if (pruned || revoked) this.persistSessions();
  }

  private pruneExpiredSessions(): boolean {
    const now = Date.now();
    let changed = false;
    for (const [tokenHash, session] of this.sessions) {
      if (new Date(session.expiresAt).getTime() <= now) {
        this.sessions.delete(tokenHash);
        changed = true;
      }
    }
    return changed;
  }

  private pruneSignInAttempts(): void {
    const now = Date.now();
    for (const [attemptKey, attempt] of this.signInAttempts) {
      if (now - attempt.windowStartedAt >= SIGN_IN_WINDOW_MS) {
        this.signInAttempts.delete(attemptKey);
      }
    }

    if (this.signInAttempts.size <= MAX_SIGN_IN_ATTEMPT_ENTRIES) return;
    const oldestAttempts = Array.from(this.signInAttempts.entries())
      .sort(([, left], [, right]) => left.windowStartedAt - right.windowStartedAt)
      .slice(0, this.signInAttempts.size - MAX_SIGN_IN_ATTEMPT_ENTRIES);
    for (const [attemptKey] of oldestAttempts) {
      this.signInAttempts.delete(attemptKey);
    }
  }

  private ensureSignInAllowed(attemptKey: string): void {
    const attempt = this.signInAttempts.get(attemptKey);
    if (!attempt) return;

    const now = Date.now();
    if (attempt.blockedUntil && attempt.blockedUntil > now) {
      throw this.invalidCredentialsError();
    }
    if (now - attempt.windowStartedAt >= SIGN_IN_WINDOW_MS) {
      this.signInAttempts.delete(attemptKey);
    }
  }

  private recordFailedSignIn(attemptKey: string): void {
    const now = Date.now();
    const existing = this.signInAttempts.get(attemptKey);
    const attempt = !existing || now - existing.windowStartedAt >= SIGN_IN_WINDOW_MS
      ? { failureCount: 0, windowStartedAt: now }
      : existing;

    attempt.failureCount += 1;
    if (attempt.failureCount >= MAX_SIGN_IN_FAILURES) {
      attempt.blockedUntil = now + SIGN_IN_WINDOW_MS;
    }
    this.signInAttempts.set(attemptKey, attempt);
    this.pruneSignInAttempts();
  }

  private invalidCredentialsError(): AppError {
    return new AppError(401, "INVALID_CREDENTIALS", "Invalid email or password.");
  }

  public getUserById(userId: string): User | null {
    const userRecord = this.users.get(userId);
    return userRecord ? this.toPublicUser(userRecord) : null;
  }

  private createSession(userId: string): string {
    this.pruneExpiredSessions();
    const token = generateToken();
    const now = new Date();
    const expiresAt = new Date(now.getTime() + SESSION_EXPIRY_DAYS * 24 * 60 * 60 * 1000).toISOString();

    const session: AuthSession = {
      tokenHash: hashToken(token),
      userId,
      createdAt: now.toISOString(),
      expiresAt
    };

    this.sessions.set(session.tokenHash, session);
    this.persistSessions();
    return token;
  }

  private toPublicUser(record: UserRecord): User {
    return {
      id: record.id,
      fullName: record.fullName,
      email: record.email,
      mobileNumber: record.mobileNumber,
      address: record.address,
      createdAt: record.createdAt
    };
  }
}

export const authService = new AuthService();
