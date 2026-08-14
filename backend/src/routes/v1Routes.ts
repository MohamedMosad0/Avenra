import { Router, Request, Response, NextFunction } from "express";
import { catalogService } from "../services/catalogService.js";
import { checkoutService } from "../services/checkoutService.js";
import { authService } from "../services/authService.js";
import { AppError, sendError } from "../utils/error.js";
import { hashToken } from "../utils/auth.js";

export const v1Router = Router();

// --- Health Check Endpoint ---
// GET /v1/health (unauthenticated, non-blocking, container health check)
v1Router.get("/health", (_req: Request, res: Response) => {
  res.status(200).json({ status: "ok" });
});

function requireAuthentication(req: Request, res: Response, next: NextFunction): void {
  const authHeader = req.header("Authorization") || "";
  const token = authHeader.startsWith("Bearer ") ? authHeader.substring(7) : "";
  const user = token ? authService.getUserByToken(token) : null;

  if (!user) {
    sendError(res, 401, "UNAUTHORIZED", "Missing, expired, or invalid authorization token.");
    return;
  }

  res.locals.authToken = token;
  res.locals.authUser = user;
  next();
}

// Ensure catalog data is initialized
v1Router.use(async (req: Request, res: Response, next: NextFunction) => {
  try {
    await catalogService.initialize();
    next();
  } catch (err) {
    next(err);
  }
});

// --- Auth Endpoints ---

// POST /v1/auth/signup
v1Router.post("/auth/signup", (req: Request, res: Response) => {
  try {
    const authResponse = authService.signUp(req.body);
    res.status(201).json(authResponse);
  } catch (error) {
    if (error instanceof AppError) {
      return sendError(res, error.statusCode, error.code, error.message);
    }
    return sendError(res, 500, "SERVICE_UNAVAILABLE", "An unexpected error occurred during signup.");
  }
});

// POST /v1/auth/signin
v1Router.post("/auth/signin", (req: Request, res: Response) => {
  try {
    const authResponse = authService.signIn(req.body, req.ip);
    res.status(200).json(authResponse);
  } catch (error) {
    if (error instanceof AppError) {
      return sendError(res, error.statusCode, error.code, error.message);
    }
    return sendError(res, 500, "SERVICE_UNAVAILABLE", "An unexpected error occurred during signin.");
  }
});

// GET /v1/auth/me
v1Router.get("/auth/me", requireAuthentication, (_req: Request, res: Response) => {
  res.status(200).json({ user: res.locals.authUser });
});

// POST /v1/auth/revoke
v1Router.post("/auth/revoke", requireAuthentication, (_req: Request, res: Response) => {
  authService.revokeToken(res.locals.authToken);
  res.status(204).send();
});

// --- Catalog Endpoints ---

// 1. GET /v1/home
v1Router.get("/home", (req: Request, res: Response) => {
  const data = catalogService.getHomeData();
  res.json(data);
});

// 2. GET /v1/categories
v1Router.get("/categories", (req: Request, res: Response) => {
  const categories = catalogService.getCategories();
  res.json({ categories });
});

// 3. GET /v1/products
v1Router.get("/products", (req: Request, res: Response) => {
  const categoryId = req.query.categoryId as string | undefined;
  const q = req.query.q as string | undefined;

  const products = catalogService.getProducts(categoryId, q);
  res.json({ products });
});

// 4. GET /v1/products/:productId
v1Router.get("/products/:productId", (req: Request, res: Response, next: NextFunction) => {
  const { productId } = req.params;
  const product = catalogService.getProductById(productId);

  if (!product) {
    return sendError(res, 404, "PRODUCT_NOT_FOUND", `Product '${productId}' not found.`);
  }

  res.json({ product });
});

// 5. POST /v1/checkout/quotes
v1Router.post("/checkout/quotes", requireAuthentication, (req: Request, res: Response, next: NextFunction) => {
  try {
    const quote = checkoutService.createQuote(req.body, res.locals.authUser.id);
    res.status(201).json(quote);
  } catch (error) {
    if (error instanceof AppError) {
      return sendError(res, error.statusCode, error.code, error.message);
    }
    return sendError(res, 500, "SERVICE_UNAVAILABLE", "An unexpected error occurred during quote generation.");
  }
});

// 6. POST /v1/orders
v1Router.post("/orders", requireAuthentication, (req: Request, res: Response, next: NextFunction) => {
  try {
    const idempotencyKey = req.header("Idempotency-Key");
    const order = checkoutService.createOrder(
      req.body,
      idempotencyKey,
      res.locals.authUser.id,
      hashToken(res.locals.authToken),
    );
    res.status(201).json(order);
  } catch (error) {
    if (error instanceof AppError) {
      return sendError(res, error.statusCode, error.code, error.message);
    }
    return sendError(res, 500, "SERVICE_UNAVAILABLE", "An unexpected error occurred during order creation.");
  }
});
