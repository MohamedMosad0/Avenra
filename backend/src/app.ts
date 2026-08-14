import express, { Express, Request, Response } from "express";
import cors from "cors";
import path from "path";
import { fileURLToPath } from "url";
import { v1Router } from "./routes/v1Routes.js";
import { sendError } from "./utils/error.js";
import { config } from "./config/index.js";

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

export function createApp(): Express {
  const app = express();

  app.use(cors({
    origin: config.allowedOrigins,
    methods: ["GET", "POST", "OPTIONS"],
    allowedHeaders: ["Content-Type", "Authorization", "Idempotency-Key"],
  }));
  app.use(express.json());

  // Serve static assets from public/assets
  const publicAssetsPath = path.resolve(__dirname, "..", "public", "assets");
  app.use("/assets", express.static(publicAssetsPath));

  // Mount approved API v1 router
  app.use("/v1", v1Router);

  // 404 handler for undefined routes
  app.use((req: Request, res: Response) => {
    sendError(res, 404, "PRODUCT_NOT_FOUND", `Endpoint '${req.method} ${req.path}' not found.`);
  });

  return app;
}
