import dotenv from "dotenv";

dotenv.config();

export const config = {
  port: parseInt(process.env.PORT || "3000", 10),
  nodeEnv: process.env.NODE_ENV || "development",
  baseUrl: process.env.BASE_URL || "http://localhost:3000",
  upstreamApiBaseUrl: process.env.UPSTREAM_API_BASE_URL || "",
  allowedOrigins: (process.env.ALLOWED_ORIGINS || "http://localhost:3000,http://127.0.0.1:3000")
    .split(",")
    .map((origin) => origin.trim())
    .filter(Boolean),
  upstreamRequestTimeoutMs: parseInt(process.env.UPSTREAM_REQUEST_TIMEOUT_MS || "8000", 10),
  quoteExpiryMinutes: 15,
};
