# Avenra Technology Decisions

**Status:** Implemented and Active Architecture.

---

## 1. Android Architecture & Technology Stack

- **Language:** Kotlin (Coroutines + Flow)
- **UI Framework:** Jetpack Compose + Material 3 Design System
- **Presentation Architecture:** Feature-oriented MVVM (Model-View-ViewModel)
- **State Management:** `StateFlow` and `asStateFlow()` exposing immutable UI state
- **Navigation:** Jetpack Navigation Component for Compose (`NavHost`)
- **Networking:** Retrofit 2 with OkHttp 4 client and Gson converter
- **Local Persistence:**
  - **Room Database (`AppDatabase`):** Persists `CartEntity` and `WishlistEntity` for guest-capable offline accessibility.
  - **Session Storage (`UserSessionStorage`):** Encapsulated session credentials (bearer token and `UserProfile`).
- **Image Loading:** Coil for Compose (`AsyncImage` / `rememberAsyncImagePainter`)

### Presentation Flow
```
Composable Screen -> ViewModel (StateFlow) -> Repository -> Retrofit (API) / Room (DAO)
```
Composables remain declarative and observe `StateFlow`. ViewModels manage UI state and coordinate data operations. Repositories encapsulate network and database boundaries, emitting typed `NetworkResult` and `DataError` without hardcoding UI presentation strings in the data layer.

---

## 2. Supporting Backend Architecture

The backend is a focused REST API supporting the Android client.

- **Runtime & Language:** Node.js + TypeScript (ES Modules)
- **HTTP Server:** Express 4 with CORS and JSON body parsing
- **Persistence & Boundaries:**
  - **Catalog Storage (`CatalogStorage`):** Manages local seed data (`data/seed/`) and disk cache snapshots (`data/cache/catalogCache.json`).
  - **Checkout Storage (`CheckoutStorage`):** Manages durable JSON state (`orders.json`) with atomic temporary file writes (`.tmp` -> rename) and 30-day / 10k order retention pruning.
- **Authentication & Security:**
  - Salted scrypt password hashing with 16-byte random salt and 64-byte key length (10+ char policy); SHA-256 session token hashing.
  - Hashed session tokens stored on disk; bearer tokens never stored in plaintext.
  - Sign-in brute-force protection (lockout window after repeated failed attempts).
- **Checkout & Inventory Authority:**
  - Server-authoritative quote calculation (subtotal, discount, delivery fee).
  - Configured currency: `EGP`.
  - Delivery fees: `STANDARD` (50.00 EGP), `EXPRESS` (100.00 EGP). No tax fields in quotes or orders.
  - 15-minute quote TTL (`QUOTE_EXPIRED` conflict on timeout).
  - Price change validation (`PRICE_CHANGED` conflict).
  - Session-scoped, restart-safe idempotency via `Idempotency-Key` headers.
  - In-memory stock reservation with reconciliation on startup.

---

## 3. Data Authority & Commercial Boundaries

- **Single Source of Truth:** The backend is authoritative for all pricing, discounts, delivery fees, stock levels, and order states. Local Room data is an offline convenience snapshot and is revalidated whenever a checkout quote is requested.
- **Simulated Payment:** Payment methods (`CASH_ON_DELIVERY` and `MOCK_CARD`) result in `SIMULATED_SUCCESS`. No real financial processing or third-party payment gateways exist.
- **Store Merchandising:** Promotional banners are store-managed assets, not third-party advertisements.

---

## 4. Historical Decisions & Evolution

- *Early Proposal Note (Historical):* Initial exploratory drafts proposed guest-only checkout without authentication endpoints. The project subsequently standardized on authenticated checkout with token-based session management, hashed storage, and session-scoped idempotency keys to ensure production-grade security and robust error recovery.
