import { createApp } from "./app.js";
import { config } from "./config/index.js";
import { catalogService } from "./services/catalogService.js";

async function startServer() {
  try {
    await catalogService.initialize();
  } catch (err) {
    console.error("[Avenra REST API] Error during initial catalog load:", err);
  }

  const app = createApp();

  const server = app.listen(config.port, () => {
    console.log(`[Avenra REST API] Server running at ${config.baseUrl} (Port ${config.port})`);
  });

  const handleShutdown = (signal: string) => {
    console.log(`[Avenra REST API] Received ${signal}. Closing server...`);
    server.close(() => {
      console.log("[Avenra REST API] Server stopped gracefully.");
      process.exit(0);
    });
  };

  process.on("SIGINT", () => handleShutdown("SIGINT"));
  process.on("SIGTERM", () => handleShutdown("SIGTERM"));

  process.on("uncaughtException", (err) => {
    console.error("[Avenra REST API] Uncaught Exception:", err);
  });

  process.on("unhandledRejection", (reason, promise) => {
    console.error("[Avenra REST API] Unhandled Rejection at:", promise, "reason:", reason);
  });
}

startServer();

