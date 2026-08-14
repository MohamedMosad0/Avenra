import { Server } from "http";
import fs from "fs";
import path from "path";
import { fileURLToPath } from "url";

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const runtimeStorageDir = path.join(__dirname, "temp_runtime_storage");

fs.rmSync(runtimeStorageDir, { recursive: true, force: true });
process.env.AVENRA_CHECKOUT_STORAGE_DIR = path.join(runtimeStorageDir, "checkout");
process.env.AVENRA_AUTH_STORAGE_DIR = path.join(runtimeStorageDir, "auth");

const { createApp } = await import("../app.js");
const { CheckoutService } = await import("../services/checkoutService.js");
const { hashToken } = await import("../utils/auth.js");

async function runTests() {
  console.log("--- Starting Avenra Focused API Verification Suite ---");
  const app = createApp();

  const server: Server = app.listen(3002);
  const baseUrl = "http://localhost:3002/v1";

  try {
    // 0. GET /v1/health
    console.log("Test 0: GET /v1/health");
    const resHealth = await fetch(`${baseUrl}/health`);
    if (resHealth.status !== 200) throw new Error(`Health failed with status ${resHealth.status}`);
    const dataHealth = (await resHealth.json()) as any;
    if (dataHealth.status !== "ok") throw new Error(`Health expected { status: "ok" }, got ${JSON.stringify(dataHealth)}`);
    console.log("  PASSED");

    // 1. GET /v1/home
    console.log("Test 1: GET /v1/home");
    const resHome = await fetch(`${baseUrl}/home`);
    if (resHome.status !== 200) throw new Error(`Home failed with status ${resHome.status}`);
    const dataHome = (await resHome.json()) as any;
    if (!dataHome.banners || !dataHome.categories || !dataHome.featuredProducts) {
      throw new Error("Home payload missing required keys");
    }
    console.log("  PASSED");

    // 2. GET /v1/categories
    console.log("Test 2: GET /v1/categories");
    const resCat = await fetch(`${baseUrl}/categories`);
    if (resCat.status !== 200) throw new Error(`Categories failed with status ${resCat.status}`);
    const dataCat = (await resCat.json()) as any;
    if (!Array.isArray(dataCat.categories) || dataCat.categories.length === 0) {
      throw new Error("Categories array empty");
    }
    console.log("  PASSED");

    // 3. GET /v1/products (Full catalog & search filter)
    console.log("Test 3: GET /v1/products");
    const resProd = await fetch(`${baseUrl}/products`);
    if (resProd.status !== 200) throw new Error(`Products failed with status ${resProd.status}`);
    const dataProd = (await resProd.json()) as any;
    if (!Array.isArray(dataProd.products) || dataProd.products.length === 0) {
      throw new Error("Products list empty");
    }
    const sampleProduct = dataProd.products[0];

    // Test search filter dynamically using sample product title
    const searchKeyword = sampleProduct.title.split(" ")[0];
    const resSearch = await fetch(`${baseUrl}/products?q=${encodeURIComponent(searchKeyword)}`);
    const dataSearch = (await resSearch.json()) as any;
    if (!Array.isArray(dataSearch.products) || dataSearch.products.length === 0) {
      throw new Error("Search filter failed");
    }
    console.log("  PASSED");

    // 4. GET /v1/products/:productId (Success & 404)
    console.log("Test 4: GET /v1/products/:productId (and 404 test)");
    const resDetail = await fetch(`${baseUrl}/products/${sampleProduct.id}`);
    if (resDetail.status !== 200) throw new Error("Product detail failed");

    const res404 = await fetch(`${baseUrl}/products/invalid-id-xyz`);
    if (res404.status !== 404) throw new Error(`Expected 404 got ${res404.status}`);
    const data404 = (await res404.json()) as any;
    if (data404.code !== "PRODUCT_NOT_FOUND") throw new Error("Incorrect 404 error code");
    console.log("  PASSED");

    const authRes = await fetch(`${baseUrl}/auth/signup`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        fullName: "Checkout Test User",
        email: `checkout_${Date.now()}@avenra.com`,
        password: "CheckoutPassword123",
      }),
    });
    if (authRes.status !== 201) throw new Error("Checkout test authentication failed");
    const authData = (await authRes.json()) as any;
    const authHeaders = {
      "Content-Type": "application/json",
      Authorization: `Bearer ${authData.token}`,
    };

    // 5. STANDARD vs EXPRESS delivery quote & NO TAX check
    console.log("Test 5: STANDARD vs EXPRESS quote calculation & NO TAX verification");
    const baseAddress = {
      fullName: "Jane Doe",
      phone: "01010000000",
      city: "Cairo",
      addressLine: "456 Market St",
    };

    const unauthenticatedQuote = await fetch(`${baseUrl}/checkout/quotes`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({}),
    });
    if (unauthenticatedQuote.status !== 401) {
      throw new Error(`Expected unauthenticated quote to return 401, got ${unauthenticatedQuote.status}`);
    }

    // Standard Delivery Quote (50 EGP delivery fee)
    const resStandard = await fetch(`${baseUrl}/checkout/quotes`, {
      method: "POST",
      headers: authHeaders,
      body: JSON.stringify({
        items: [{ productId: sampleProduct.id, quantity: 1 }],
        shippingAddress: baseAddress,
        deliveryMethod: "STANDARD",
      }),
    });
    const quoteStandard = (await resStandard.json()) as any;
    if (quoteStandard.deliveryMethod !== "STANDARD" || quoteStandard.deliveryFee !== 50.0) {
      throw new Error("STANDARD delivery calculation failed");
    }

    const expectedStandardTotal = (sampleProduct.discountPrice || sampleProduct.price) + 50.0;
    if (quoteStandard.finalTotal !== expectedStandardTotal) {
      throw new Error(`Unexpected standard final total: ${quoteStandard.finalTotal}, expected: ${expectedStandardTotal}`);
    }
    // Verify NO TAX fields exist
    if (quoteStandard.tax !== undefined || quoteStandard.taxes !== undefined || quoteStandard.taxRate !== undefined) {
      throw new Error("Tax fields illegally detected in checkout quote payload!");
    }

    // Express Delivery Quote (100 EGP delivery fee)
    const resExpress = await fetch(`${baseUrl}/checkout/quotes`, {
      method: "POST",
      headers: authHeaders,
      body: JSON.stringify({
        items: [{ productId: sampleProduct.id, quantity: 1 }],
        shippingAddress: baseAddress,
        deliveryMethod: "EXPRESS",
      }),
    });
    const quoteExpress = (await resExpress.json()) as any;
    if (quoteExpress.deliveryMethod !== "EXPRESS" || quoteExpress.deliveryFee !== 100.0) {
      throw new Error("EXPRESS delivery calculation failed");
    }

    const expectedExpressTotal = (sampleProduct.discountPrice || sampleProduct.price) + 100.0;
    if (quoteExpress.finalTotal !== expectedExpressTotal) {
      throw new Error(`Unexpected express final total: ${quoteExpress.finalTotal}, expected: ${expectedExpressTotal}`);
    }
    console.log("  PASSED");

    // 5b. Quantity validation (positive integer requirement)
    console.log("Test 5b: Quantity validation rejects fractional, zero, negative, and string quantities");
    const invalidQuantities = [1.5, 0.5, 0, -1, -5, "2", NaN, null, undefined];
    for (const invalidQty of invalidQuantities) {
      const resInvalidQty = await fetch(`${baseUrl}/checkout/quotes`, {
        method: "POST",
        headers: authHeaders,
        body: JSON.stringify({
          items: [{ productId: sampleProduct.id, quantity: invalidQty }],
          shippingAddress: baseAddress,
          deliveryMethod: "STANDARD",
        }),
      });
      const dataInvalidQty = (await resInvalidQty.json()) as any;
      if (resInvalidQty.status !== 400 || dataInvalidQty.code !== "VALIDATION_ERROR") {
        throw new Error(`Expected 400 VALIDATION_ERROR for quantity ${invalidQty}, got ${resInvalidQty.status}`);
      }
    }
    console.log("  PASSED");

    // 6. Mock Payment & Order Snapshot Preservation
    console.log("Test 6: Mock Payment (MOCK_CARD & CASH_ON_DELIVERY) -> SIMULATED_SUCCESS & Snapshot preservation");
    
    // MOCK_CARD Order
    const missingIdempotencyKey = await fetch(`${baseUrl}/orders`, {
      method: "POST",
      headers: authHeaders,
      body: JSON.stringify({
        quoteId: quoteStandard.quoteId,
        mockPaymentMethod: "MOCK_CARD",
        shippingAddress: baseAddress,
      }),
    });
    if (missingIdempotencyKey.status !== 400) {
      throw new Error(`Expected missing Idempotency-Key to return 400, got ${missingIdempotencyKey.status}`);
    }

    const resOrderCard = await fetch(`${baseUrl}/orders`, {
      method: "POST",
      headers: {
        ...authHeaders,
        "Idempotency-Key": "idemp_card_001",
      },
      body: JSON.stringify({
        quoteId: quoteStandard.quoteId,
        mockPaymentMethod: "MOCK_CARD",
        shippingAddress: baseAddress,
      }),
    });
    const orderCard = (await resOrderCard.json()) as any;
    if (orderCard.mockPaymentMethod !== "MOCK_CARD" || orderCard.mockPaymentStatus !== "SIMULATED_SUCCESS") {
      throw new Error("MOCK_CARD payment status failed to yield SIMULATED_SUCCESS");
    }
    if (orderCard.deliveryMethod !== "STANDARD" || orderCard.deliveryFee !== 50.0) {
      throw new Error("Order snapshot failed to preserve deliveryMethod and deliveryFee");
    }

    // CASH_ON_DELIVERY Order
    const resOrderCod = await fetch(`${baseUrl}/orders`, {
      method: "POST",
      headers: {
        ...authHeaders,
        "Idempotency-Key": "idemp_cod_002",
      },
      body: JSON.stringify({
        quoteId: quoteExpress.quoteId,
        mockPaymentMethod: "CASH_ON_DELIVERY",
        shippingAddress: baseAddress,
      }),
    });
    const orderCod = (await resOrderCod.json()) as any;
    if (orderCod.mockPaymentMethod !== "CASH_ON_DELIVERY" || orderCod.mockPaymentStatus !== "SIMULATED_SUCCESS") {
      throw new Error("CASH_ON_DELIVERY payment status failed to yield SIMULATED_SUCCESS");
    }
    if (orderCod.deliveryMethod !== "EXPRESS" || orderCod.deliveryFee !== 100.0) {
      throw new Error("Order snapshot failed to preserve EXPRESS delivery settings");
    }
    console.log("  PASSED");

    // 7. Idempotency Key Verification
    console.log("Test 7: Idempotency Key Verification");
    const resIdempRetry = await fetch(`${baseUrl}/orders`, {
      method: "POST",
      headers: {
        ...authHeaders,
        "Idempotency-Key": "idemp_card_001",
      },
      body: JSON.stringify({
        quoteId: quoteStandard.quoteId,
        mockPaymentMethod: "MOCK_CARD",
        shippingAddress: baseAddress,
      }),
    });
    const orderIdempRetry = (await resIdempRetry.json()) as any;
    if (orderIdempRetry.orderId !== orderCard.orderId) {
      throw new Error("Idempotency key failed to return identical order");
    }

    const secondSessionRes = await fetch(`${baseUrl}/auth/signin`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        email: authData.user.email,
        password: "CheckoutPassword123",
      }),
    });
    const secondSession = (await secondSessionRes.json()) as any;
    const secondSessionHeaders = {
      "Content-Type": "application/json",
      Authorization: `Bearer ${secondSession.token}`,
    };
    const secondSessionQuoteRes = await fetch(`${baseUrl}/checkout/quotes`, {
      method: "POST",
      headers: secondSessionHeaders,
      body: JSON.stringify({
        items: [{ productId: sampleProduct.id, quantity: 1 }],
        shippingAddress: baseAddress,
        deliveryMethod: "STANDARD",
      }),
    });
    const secondSessionQuote = (await secondSessionQuoteRes.json()) as any;
    const secondSessionOrderRes = await fetch(`${baseUrl}/orders`, {
      method: "POST",
      headers: { ...secondSessionHeaders, "Idempotency-Key": "idemp_card_001" },
      body: JSON.stringify({
        quoteId: secondSessionQuote.quoteId,
        mockPaymentMethod: "MOCK_CARD",
        shippingAddress: baseAddress,
      }),
    });
    const secondSessionOrder = (await secondSessionOrderRes.json()) as any;
    if (secondSessionOrder.orderId === orderCard.orderId) {
      throw new Error("Idempotency key was not scoped to the authenticated session");
    }
    console.log("  PASSED");

    // 8. Stock reservation and restart-safe idempotency
    console.log("Test 8: Stock reservation and restart-safe idempotency");
    const stockProduct = dataProd.products.find((product: any) => product.id !== sampleProduct.id && product.availableQuantity > 0);
    if (!stockProduct) throw new Error("No independent in-stock product available for stock test");
    const fullStockQuoteRes = await fetch(`${baseUrl}/checkout/quotes`, {
      method: "POST",
      headers: authHeaders,
      body: JSON.stringify({
        items: [{ productId: stockProduct.id, quantity: stockProduct.availableQuantity }],
        shippingAddress: baseAddress,
        deliveryMethod: "STANDARD",
      }),
    });
    const fullStockQuote = (await fullStockQuoteRes.json()) as any;
    const fullStockOrderRes = await fetch(`${baseUrl}/orders`, {
      method: "POST",
      headers: { ...authHeaders, "Idempotency-Key": "idemp_stock_001" },
      body: JSON.stringify({
        quoteId: fullStockQuote.quoteId,
        mockPaymentMethod: "CASH_ON_DELIVERY",
        shippingAddress: baseAddress,
      }),
    });
    if (fullStockOrderRes.status !== 201) throw new Error("Stock reservation order failed");
    const oversellQuoteRes = await fetch(`${baseUrl}/checkout/quotes`, {
      method: "POST",
      headers: authHeaders,
      body: JSON.stringify({
        items: [{ productId: stockProduct.id, quantity: 1 }],
        shippingAddress: baseAddress,
        deliveryMethod: "STANDARD",
      }),
    });
    if (oversellQuoteRes.status !== 409) throw new Error(`Expected oversell quote to return 409, got ${oversellQuoteRes.status}`);

    const restartStorageDir = path.join(__dirname, "temp_checkout_storage");
    fs.rmSync(restartStorageDir, { recursive: true, force: true });
    try {
      const restartRequest = {
        quoteId: "",
        mockPaymentMethod: "CASH_ON_DELIVERY" as const,
        shippingAddress: baseAddress,
      };
      const serviceBeforeRestart = new CheckoutService(restartStorageDir);
      const restartQuote = serviceBeforeRestart.createQuote({
        items: [{ productId: sampleProduct.id, quantity: 1 }],
        shippingAddress: baseAddress,
        deliveryMethod: "STANDARD",
      }, authData.user.id);
      restartRequest.quoteId = restartQuote.quoteId;
      const sessionHash = hashToken(authData.token);
      const persistedOrder = serviceBeforeRestart.createOrder(restartRequest, "idemp_restart_001", authData.user.id, sessionHash);
      const serviceAfterRestart = new CheckoutService(restartStorageDir);
      const replayedOrder = serviceAfterRestart.createOrder(restartRequest, "idemp_restart_001", authData.user.id, sessionHash);
      if (replayedOrder.orderId !== persistedOrder.orderId) {
        throw new Error("Restart replay created a duplicate order");
      }
    } finally {
      fs.rmSync(restartStorageDir, { recursive: true, force: true });
    }
    console.log("  PASSED");

    console.log("--- ALL FOCUSED INTEGRATION TESTS PASSED SUCCESSFULLY! ---");
  } catch (err) {
    console.error("Test execution failed:", err);
    process.exitCode = 1;
  } finally {
    server.close();
    fs.rmSync(runtimeStorageDir, { recursive: true, force: true });
  }
}

runTests();
