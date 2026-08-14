# Avenra Backend API Specification

**Status:** Current Implemented REST Contract (`/v1`).

---

## 1. Overview & General Conventions

- **Base URL:** `http://<host>:3000/v1`
- **Protocol:** HTTP/1.1 with JSON payloads (`Content-Type: application/json`).
- **Currency:** `EGP` (Egyptian Pounds).
- **Authentication:** Bearer token authorization header: `Authorization: Bearer <token>`.
- **Idempotency:** Order creation requires an `Idempotency-Key` header for safe retries and replay deduplication.

### Standard Error Envelope
All error responses return a standardized JSON structure:
```json
{
  "status": "error",
  "code": "ERROR_CODE",
  "message": "Human-readable explanation of the error."
}
```

Common error codes:
| HTTP Status | Code | Meaning / Client Handling |
| --- | --- | --- |
| 400 | `VALIDATION_ERROR` | Missing or invalid request parameters. |
| 401 | `UNAUTHORIZED` / `INVALID_CREDENTIALS` | Missing/invalid token or wrong credentials. |
| 404 | `PRODUCT_NOT_FOUND` | The requested product ID does not exist. |
| 409 | `EMAIL_ALREADY_EXISTS` | Registration email is already in use. |
| 409 | `OUT_OF_STOCK` | Requested quantity exceeds available inventory. |
| 409 | `PRICE_CHANGED` | Product price changed since quote generation. |
| 409 | `QUOTE_EXPIRED` | The 15-minute checkout quote has expired. |
| 409 | `DUPLICATE_REQUEST` | Idempotency key reused with different request payload. |
| 500 / 503 | `SERVICE_UNAVAILABLE` | Server or disk persistence failure; retryable. |

---

## 2. Authentication Endpoints

### 2.1 POST `/v1/auth/signup`
Creates a new customer account and returns an active session token.

**Request Body:**
```json
{
  "fullName": "Jane Doe",
  "email": "jane@example.com",
  "password": "SecurePassword123",
  "mobileNumber": "01012345678",
  "address": "123 Nile Street, Cairo"
}
```
*Validation:* `fullName` >= 2 chars, valid email format, `password` >= 10 chars.

**Response (201 Created):**
```json
{
  "user": {
    "id": "usr_abc123",
    "fullName": "Jane Doe",
    "email": "jane@example.com",
    "mobileNumber": "01012345678",
    "address": "123 Nile Street, Cairo",
    "createdAt": "2026-08-14T00:00:00.000Z"
  },
  "token": "tok_xyz789"
}
```

---

### 2.2 POST `/v1/auth/signin`
Authenticates existing credentials.

**Request Body:**
```json
{
  "email": "jane@example.com",
  "password": "SecurePassword123"
}
```

**Response (200 OK):** Same structure as `POST /v1/auth/signup`.

---

### 2.3 GET `/v1/auth/me`
Retrieves the currently authenticated profile.

**Headers:** `Authorization: Bearer <token>`

**Response (200 OK):**
```json
{
  "user": {
    "id": "usr_abc123",
    "fullName": "Jane Doe",
    "email": "jane@example.com",
    "mobileNumber": "01012345678",
    "address": "123 Nile Street, Cairo",
    "createdAt": "2026-08-14T00:00:00.000Z"
  }
}
```

---

### 2.4 POST `/v1/auth/revoke`
Invalidates the active bearer session token.

**Headers:** `Authorization: Bearer <token>`

**Response (204 No Content):** Empty body.

---

## 3. Catalog Endpoints

### 3.1 GET `/v1/home`
Supplies the Home feed composition.

**Response (200 OK):**
```json
{
  "banners": [
    {
      "id": "b1",
      "title": "Up to 50% Off",
      "subtitle": "Discover Autumn Styles",
      "imageUrl": "http://localhost:3000/assets/images/banners/banner_fashion.jpg",
      "targetCategoryId": "women"
    }
  ],
  "categories": [
    {
      "id": "women",
      "name": "Women's Fashion",
      "imageUrl": "http://localhost:3000/assets/images/categories/cat_women.jpg",
      "subcategories": [
        { "id": "w_dresses", "name": "Dresses", "categoryId": "women" }
      ]
    }
  ],
  "featuredProducts": [
    {
      "id": "p1",
      "title": "Woman Shawl",
      "description": "Soft warm winter shawl.",
      "price": 1200.0,
      "discountPrice": 900.0,
      "imageUrl": "http://localhost:3000/assets/images/products/shawl_cover.jpg",
      "galleryImages": [],
      "rating": 4.7,
      "reviewCount": 320,
      "categoryId": "women",
      "isAvailable": true,
      "availableQuantity": 15,
      "sizes": ["S", "M", "L"],
      "colors": ["Beige", "Navy"]
    }
  ]
}
```

---

### 3.2 GET `/v1/categories`
Retrieves all top-level categories and subcategories.

**Response (200 OK):**
```json
{
  "categories": [
    {
      "id": "women",
      "name": "Women's Fashion",
      "imageUrl": "http://localhost:3000/assets/images/categories/cat_women.jpg",
      "subcategories": [
        { "id": "w_dresses", "name": "Dresses", "categoryId": "women" }
      ]
    }
  ]
}
```

---

### 3.3 GET `/v1/products`
Retrieves products filtered by category or search query.

**Query Parameters:**
- `categoryId` (optional): Filter products belonging to a specific category.
- `q` (optional): Keyword search matching product title or description.

**Response (200 OK):**
```json
{
  "products": [
    {
      "id": "p1",
      "title": "Woman Shawl",
      "description": "Soft warm winter shawl.",
      "price": 1200.0,
      "discountPrice": 900.0,
      "imageUrl": "http://localhost:3000/assets/images/products/shawl_cover.jpg",
      "galleryImages": [],
      "rating": 4.7,
      "reviewCount": 320,
      "categoryId": "women",
      "isAvailable": true,
      "availableQuantity": 15,
      "sizes": ["S", "M", "L"],
      "colors": ["Beige", "Navy"]
    }
  ]
}
```

---

### 3.4 GET `/v1/products/:productId`
Retrieves single product details.

**Response (200 OK):**
```json
{
  "product": {
    "id": "p1",
    "title": "Woman Shawl",
    "description": "Soft warm winter shawl.",
    "price": 1200.0,
    "discountPrice": 900.0,
    "imageUrl": "http://localhost:3000/assets/images/products/shawl_cover.jpg",
    "galleryImages": [
      "http://localhost:3000/assets/images/products/shawl_1.jpg"
    ],
    "rating": 4.7,
    "reviewCount": 320,
    "categoryId": "women",
    "isAvailable": true,
    "availableQuantity": 15,
    "sizes": ["S", "M", "L"],
    "colors": ["Beige", "Navy"]
  }
}
```

---

## 4. Checkout & Order Endpoints

### 4.1 POST `/v1/checkout/quotes`
Calculates authoritative prices, discounts, and delivery fee. Generates a temporary checkout quote with a 15-minute TTL.

**Headers:** `Authorization: Bearer <token>`

**Request Body:**
```json
{
  "items": [
    { "productId": "p1", "quantity": 1 }
  ],
  "shippingAddress": {
    "fullName": "Jane Doe",
    "phone": "01012345678",
    "city": "Cairo",
    "addressLine": "123 Nile St"
  },
  "deliveryMethod": "STANDARD"
}
```

**Response (201 Created):**
```json
{
  "quoteId": "quote_1723590000000_abc12",
  "items": [
    {
      "productId": "p1",
      "title": "Woman Shawl",
      "unitPrice": 900.0,
      "quantity": 1,
      "totalPrice": 900.0
    }
  ],
  "itemSubtotal": 1200.0,
  "discountTotal": 300.0,
  "deliveryFee": 50.0,
  "finalTotal": 950.0,
  "currency": "EGP",
  "quoteExpiry": "2026-08-14T00:15:00.000Z",
  "deliveryMethod": "STANDARD"
}
```

---

### 4.2 POST `/v1/orders`
Creates a confirmed order record, reserves stock, and records simulated payment.

**Headers:**
- `Authorization: Bearer <token>`
- `Idempotency-Key: <unique-uuid-or-string>`

**Request Body:**
```json
{
  "quoteId": "quote_1723590000000_abc12",
  "shippingAddress": {
    "fullName": "Jane Doe",
    "phone": "01012345678",
    "city": "Cairo",
    "addressLine": "123 Nile St"
  },
  "mockPaymentMethod": "CASH_ON_DELIVERY"
}
```

**Response (201 Created):**
```json
{
  "orderId": "ord_1723590005000_def34",
  "orderReference": "AVN-582914",
  "quoteId": "quote_1723590000000_abc12",
  "items": [
    {
      "productId": "p1",
      "title": "Woman Shawl",
      "unitPrice": 900.0,
      "quantity": 1,
      "totalPrice": 900.0
    }
  ],
  "itemSubtotal": 1200.0,
  "discountTotal": 300.0,
  "deliveryMethod": "STANDARD",
  "deliveryFee": 50.0,
  "finalTotal": 950.0,
  "currency": "EGP",
  "shippingAddress": {
    "fullName": "Jane Doe",
    "phone": "01012345678",
    "city": "Cairo",
    "addressLine": "123 Nile St"
  },
  "mockPaymentMethod": "CASH_ON_DELIVERY",
  "mockPaymentStatus": "SIMULATED_SUCCESS",
  "status": "CONFIRMED",
  "createdAt": "2026-08-14T00:00:05.000Z"
}
```
