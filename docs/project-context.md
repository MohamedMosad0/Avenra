# Avenra Project Context

## 1. Identity and Purpose

- **Application:** Avenra
- **Type:** Android E-Commerce Application (Portfolio / CV Project)
- **Purpose:** Demonstrates modern Android engineering, clean architecture boundaries, robust state management, and real REST API integration.
- **Branding:** Avenra is the application identity. Legacy Route branding from early UI mockups has been replaced with Avenra across the codebase.

---

## 2. Implemented Architecture & Technology Stack

### Android Client
- **Language & Runtime:** Kotlin (Coroutines + Flow)
- **UI Framework:** Jetpack Compose with Material 3 design system
- **Architecture:** Feature-oriented MVVM with unidirectional data flow (`StateFlow`)
- **Navigation:** Jetpack Navigation Component for Compose
- **Networking:** Retrofit 2 + OkHttp 4 + Gson Converter
- **Local Persistence:**
  - **Room Database:** Guest-friendly local persistence for Cart and Wishlist items
  - **Session Storage:** Encapsulated token and user profile storage
- **Image Loading:** Coil for Compose

### Supporting Backend
- **Language & Runtime:** TypeScript / Node.js (Express)
- **Catalog Management:** Owned REST API with seed data and disk cache fallback snapshots (`CatalogStorage`)
- **Checkout Persistence:** Durable JSON order runtime state persistence (`CheckoutStorage`) with atomic file writes and restart-safe idempotency
- **Authentication & Security:** Salted scrypt password hashing (64-byte key, 16-byte salt), SHA-256 hashed session tokens, brute-force sign-in throttling, and token revocation

---

## 3. Implemented Product Features & User Flows

1. **Authentication & Session Management:**
   - Registration (`POST /v1/auth/signup`) and Sign-In (`POST /v1/auth/signin`) with validation (min 10-char password, email formatting).
   - Authenticated profile resolution (`GET /v1/auth/me`) and session revocation (`POST /v1/auth/revoke`).
   - Secure token storage with automatic session hydration on app startup.

2. **Catalog & Search:**
   - Home banner carousels, category grid, and featured product feed (`GET /v1/home`).
   - Category hierarchy and subcategories (`GET /v1/categories`).
   - Full product listing with category filtering and keyword search (`GET /v1/products?categoryId=...&q=...`).
   - Product details screen with multi-image gallery, variant selection (sizes, colors), discount formatting, and available stock badges (`GET /v1/products/:productId`).

3. **Cart & Wishlist:**
   - Local Room-backed cart with quantity manipulation, size/color variant snapshotting, and total calculation.
   - Local Room-backed wishlist with one-tap toggle across catalog and detail screens.

4. **Authoritative Checkout & Orders:**
   - Shipping address validation.
   - Delivery method selection: `STANDARD` (50.00 EGP) and `EXPRESS` (100.00 EGP). No tax calculation (EGP currency).
   - Server-authoritative quote generation (`POST /v1/checkout/quotes`) with a 15-minute TTL.
   - Quote validation and conflict handling for `PRICE_CHANGED`, `OUT_OF_STOCK`, and `QUOTE_EXPIRED`.
   - Simulated payment processing (`CASH_ON_DELIVERY` and `MOCK_CARD`).
   - Session-scoped, restart-safe idempotency using client-supplied `Idempotency-Key` headers (`POST /v1/orders`).
   - Authoritative stock reservation and rollback on persistence failures.

---

## 4. Product Boundaries & Constraints

- **Payment Processing:** Mock / simulated only (`SIMULATED_SUCCESS`). No real credit card or payment gateway integration.
- **Data Authority:** The backend is the single source of truth for pricing, discounts, delivery fees, quote validity, stock availability, and order status. Local Room data is purely for client convenience and is revalidated during checkout quote generation.
- **Third-Party Advertising:** Promotional banners are store-owned merchandising assets; no third-party advertising SDKs are included.
