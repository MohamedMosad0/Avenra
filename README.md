# Avenra

Avenra is an Android e-commerce application built with Jetpack Compose. It supports authentication, product discovery, wishlist and cart management, and a server-validated checkout flow through a lightweight supporting REST API.

## Features

- Authentication, session restoration, sign-out, and session invalidation
- Product catalog, categories, search, filtering, and product details
- Local wishlist and cart management
- Standard and Express checkout with server-side stock and price validation
- Loading, empty, error, and network failure handling

## Tech Stack

**Android:** Kotlin, Jetpack Compose, MVVM, StateFlow, Retrofit, Room, Hilt, EncryptedSharedPreferences, Coil

**Backend:** Node.js, TypeScript, Express, REST API

**CI/CD:** GitHub Actions

## Architecture

```text
Compose UI
    ↓
ViewModel
    ↓
Repository
    ↓
Remote API / Local Data
```

The Android app uses MVVM with repositories coordinating remote API calls and local Room data. Cart and wishlist data are stored locally, while signed-in session data is stored using encrypted shared preferences.

## Checkout

The backend is authoritative for checkout pricing and stock. The app requests a checkout quote, then creates the order only after the server validates the quote and current stock. Order creation uses an idempotency key to safely handle retries.

## Testing & Build

- Android unit tests
- Backend API tests
- Debug builds verified locally
- GitHub Actions CI/CD
- Signed APK release workflow

## Screenshots

<p align="center">
  <img src="docs/screenshots/home.jpg" width="220" alt="Home screen">
  <img src="docs/screenshots/categories.jpg" width="220" alt="Categories screen">
  <img src="docs/screenshots/catalog.jpg" width="220" alt="Catalog screen">
</p>

<p align="center">
  <img src="docs/screenshots/wishlist.jpg" width="220" alt="Wishlist screen">
  <img src="docs/screenshots/cart.jpg" width="220" alt="Cart screen">
  <img src="docs/screenshots/checkout.jpg" width="220" alt="Checkout screen">
</p>

## Project Structure

```text
app/        Android application
backend/    Supporting TypeScript/Express REST API
docs/       Screenshots and project references
```

## Getting Started

### Android

Open the project in Android Studio with JDK 17 and an Android SDK, then run:

```powershell
.\gradlew.bat assembleDebug
```

### Backend

With Node.js 18+:

```powershell
cd backend
npm install
Copy-Item .env.example .env
npm run dev
```

Do not commit API keys or other secrets.

## License

This project is licensed under the MIT License.