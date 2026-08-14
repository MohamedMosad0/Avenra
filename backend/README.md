# Avenra Supporting REST API Backend

Minimal Node.js / TypeScript / Express supporting backend service for the Avenra Android E-Commerce portfolio application.

## Endpoints (Strict 6-Endpoint Contract)

- `GET /v1/home` — Banner carousel, category previews, and featured product catalog.
- `GET /v1/categories` — Complete normalized category hierarchy and subcategory mapping.
- `GET /v1/products` — Controlled product catalog with optional `categoryId` and `q` search filters (NO pagination).
- `GET /v1/products/:productId` — Single product detail view (returns 404 if missing).
- `POST /v1/checkout/quotes` — Server-authoritative price validation, stock verification, subtotal, discount, delivery fee, and short-lived quote expiration (NO taxes).
- `POST /v1/orders` — In-memory order snapshot creation from valid quote with idempotency check (`Idempotency-Key`).

## Setup & Execution

### Quick Start for Android Development:
Run from project root:
```powershell
.\start-backend.ps1
# or: .\start-backend.bat
```
This automatically resolves Node.js, sets up ADB reverse port forwarding for connected Android devices/emulators (`adb reverse tcp:3000 tcp:3000`), compiles TypeScript, and keeps the server running with auto-restart resilience.

### Manual / Standard Execution:
```bash
npm install
npm run dev     # Starts dev server with live reload on port 3000
npm run test    # Runs automated endpoint integration tests
npm run build   # Compiles TypeScript to dist/
npm start       # Starts production dist build
```

