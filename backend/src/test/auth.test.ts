import { createApp } from "../app.js";
import { Server } from "http";
import { AuthService } from "../services/authService.js";
import fs from "fs";
import path from "path";
import { fileURLToPath } from "url";

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

async function runAuthTests() {
  console.log("--- Starting Avenra Auth Verification Suite ---");

  // Create isolated temp storage dir for test
  const testStorageDir = path.join(__dirname, "temp_auth_storage");
  if (fs.existsSync(testStorageDir)) {
    fs.rmSync(testStorageDir, { recursive: true, force: true });
  }

  const app = createApp();
  const server: Server = app.listen(3003);
  const baseUrl = "http://localhost:3003/v1/auth";

  try {
    // 1. Sign Up Test
    console.log("Test 1: POST /v1/auth/signup (Valid)");
    const testEmail = `test_${Date.now()}@avenra.com`;
    const signupRes = await fetch(`${baseUrl}/signup`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        fullName: "Test User",
        email: testEmail,
        password: "SecretPassword123",
        mobileNumber: "01099998888",
        address: "123 Nile St, Cairo"
      })
    });

    if (signupRes.status !== 201) {
      const err = await signupRes.text();
      throw new Error(`Signup failed with status ${signupRes.status}: ${err}`);
    }

    const signupData = (await signupRes.json()) as any;
    if (!signupData.token || !signupData.user || signupData.user.email !== testEmail) {
      throw new Error("Invalid signup response structure");
    }
    if ((signupData.user as any).password || (signupData.user as any).passwordHash) {
      throw new Error("SECURITY VIOLATION: Password hash leaked in public user payload!");
    }
    console.log("  PASSED");

    // 2. Password policy
    console.log("Test 2: POST /v1/auth/signup rejects passwords below 10 characters");
    const shortPasswordRes = await fetch(`${baseUrl}/signup`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        fullName: "Short Password",
        email: `short_${Date.now()}@avenra.com`,
        password: "Short123",
      })
    });
    const shortPasswordData = (await shortPasswordRes.json()) as any;
    if (shortPasswordRes.status !== 400 || shortPasswordData.code !== "VALIDATION_ERROR") {
      throw new Error("Short password was not rejected with validation error");
    }
    console.log("  PASSED");

    // 2b. Field type validation (rejects non-string types with HTTP 400 VALIDATION_ERROR)
    console.log("Test 2b: POST /v1/auth/signup & signin reject non-string field types");
    const malformedSignupRes = await fetch(`${baseUrl}/signup`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        fullName: 12345,
        email: true,
        password: {},
      })
    });
    const malformedSignupData = (await malformedSignupRes.json()) as any;
    if (malformedSignupRes.status !== 400 || malformedSignupData.code !== "VALIDATION_ERROR") {
      throw new Error(`Expected 400 VALIDATION_ERROR for malformed signup types, got ${malformedSignupRes.status}`);
    }

    const malformedSigninRes = await fetch(`${baseUrl}/signin`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        email: 12345,
        password: false,
      })
    });
    const malformedSigninData = (await malformedSigninRes.json()) as any;
    if (malformedSigninRes.status !== 400 || malformedSigninData.code !== "VALIDATION_ERROR") {
      throw new Error(`Expected 400 VALIDATION_ERROR for malformed signin types, got ${malformedSigninRes.status}`);
    }
    console.log("  PASSED");

    // 2c. Full name length validation
    console.log("Test 2c: POST /v1/auth/signup rejects empty or single-character full name");
    const shortNameRes = await fetch(`${baseUrl}/signup`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        fullName: "A",
        email: `valid_${Date.now()}@avenra.com`,
        password: "ValidPassword123",
      })
    });
    const shortNameData = (await shortNameRes.json()) as any;
    if (shortNameRes.status !== 400 || shortNameData.code !== "VALIDATION_ERROR") {
      throw new Error("Short full name was not rejected with 400 VALIDATION_ERROR");
    }
    console.log("  PASSED");

    // 2d. Email format validation
    console.log("Test 2d: POST /v1/auth/signup rejects malformed email formats");
    const malformedEmails = ["invalid-email", "@no-user.com", "user@", "user@domain", "user space@domain.com"];
    for (const badEmail of malformedEmails) {
      const badEmailRes = await fetch(`${baseUrl}/signup`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          fullName: "Valid Name",
          email: badEmail,
          password: "ValidPassword123",
        })
      });
      const badEmailData = (await badEmailRes.json()) as any;
      if (badEmailRes.status !== 400 || badEmailData.code !== "VALIDATION_ERROR") {
        throw new Error(`Expected 400 VALIDATION_ERROR for malformed email '${badEmail}', got ${badEmailRes.status}`);
      }
    }
    console.log("  PASSED");

    // 3. Duplicate Email Test
    console.log("Test 3: POST /v1/auth/signup (Duplicate Email)");
    const duplicateRes = await fetch(`${baseUrl}/signup`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        fullName: "Another Person",
        email: testEmail,
        password: "AnotherPassword123"
      })
    });

    if (duplicateRes.status !== 409) {
      throw new Error(`Expected 409 Duplicate Email, got ${duplicateRes.status}`);
    }
    const dupData = (await duplicateRes.json()) as any;
    if (dupData.code !== "EMAIL_ALREADY_EXISTS") {
      throw new Error(`Expected code EMAIL_ALREADY_EXISTS, got ${dupData.code}`);
    }
    console.log("  PASSED");

    // 4. Sign In Test (Valid)
    console.log("Test 4: POST /v1/auth/signin (Valid Credentials)");
    const signinRes = await fetch(`${baseUrl}/signin`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        email: testEmail,
        password: "SecretPassword123"
      })
    });

    if (signinRes.status !== 200) {
      throw new Error(`Signin failed with status ${signinRes.status}`);
    }
    const signinData = (await signinRes.json()) as any;
    if (!signinData.token || signinData.user.email !== testEmail) {
      throw new Error("Invalid signin payload");
    }
    const activeToken = signinData.token;
    console.log("  PASSED");

    // 5. Sign In Test (Invalid Password)
    console.log("Test 5: POST /v1/auth/signin (Invalid Password)");
    const badPassRes = await fetch(`${baseUrl}/signin`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        email: testEmail,
        password: "WrongPassword999"
      })
    });

    if (badPassRes.status !== 401) {
      throw new Error(`Expected 401 Invalid Credentials, got ${badPassRes.status}`);
    }
    const badPassData = (await badPassRes.json()) as any;
    if (badPassData.code !== "INVALID_CREDENTIALS") {
      throw new Error(`Expected code INVALID_CREDENTIALS, got ${badPassData.code}`);
    }
    for (let attempt = 0; attempt < 4; attempt += 1) {
      await fetch(`${baseUrl}/signin`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email: testEmail, password: "WrongPassword999" })
      });
    }
    const throttledRes = await fetch(`${baseUrl}/signin`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ email: testEmail, password: "SecretPassword123" })
    });
    const throttledData = (await throttledRes.json()) as any;
    if (throttledRes.status !== 401 || throttledData.code !== "INVALID_CREDENTIALS" || throttledData.message !== "Invalid email or password.") {
      throw new Error("Sign-in throttle did not preserve the generic authentication error response");
    }
    console.log("  PASSED");

    // 6. Sign In Test (Non-existent Email)
    console.log("Test 6: POST /v1/auth/signin (Non-existent Email)");
    const noUserRes = await fetch(`${baseUrl}/signin`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        email: "nonexistent@avenra.com",
        password: "Password123"
      })
    });

    if (noUserRes.status !== 401) {
      throw new Error(`Expected 401 for non-existent user, got ${noUserRes.status}`);
    }
    const noUserData = (await noUserRes.json()) as any;
    if (noUserData.code !== "INVALID_CREDENTIALS" || noUserData.message !== badPassData.message) {
      throw new Error("Non-existent account response leaked a different authentication outcome");
    }
    console.log("  PASSED");

    // 7. GET /v1/auth/me (Authenticated)
    console.log("Test 7: GET /v1/auth/me (With Valid Token)");
    const meRes = await fetch(`${baseUrl}/me`, {
      headers: { Authorization: `Bearer ${activeToken}` }
    });
    if (meRes.status !== 200) {
      throw new Error(`GET /me failed with status ${meRes.status}`);
    }
    const meData = (await meRes.json()) as any;
    if (meData.user.email !== testEmail) {
      throw new Error("GET /me returned wrong user");
    }
    if ("token" in meData) {
      throw new Error("GET /me must not return the bearer token");
    }
    console.log("  PASSED");

    // 8. GET /v1/auth/me (Invalid Token)
    console.log("Test 8: GET /v1/auth/me (With Invalid Token)");
    const badTokenRes = await fetch(`${baseUrl}/me`, {
      headers: { Authorization: "Bearer invalid_token_123" }
    });
    if (badTokenRes.status !== 401) {
      throw new Error(`Expected 401 Unauthorized, got ${badTokenRes.status}`);
    }
    console.log("  PASSED");

    // 8b. GET /v1/auth/me (Missing and Malformed Headers)
    console.log("Test 8b: GET /v1/auth/me (Missing and Malformed Headers)");
    const noHeaderRes = await fetch(`${baseUrl}/me`);
    if (noHeaderRes.status !== 401) throw new Error("Expected missing auth header to return 401");

    const malformedHeaderRes = await fetch(`${baseUrl}/me`, {
      headers: { Authorization: "Basic dXNlcjpwYXNz" },
    });
    if (malformedHeaderRes.status !== 401) throw new Error("Expected Basic auth header to return 401 UNAUTHORIZED");
    console.log("  PASSED");

    // 9. Revoke an authenticated session
    console.log("Test 9: POST /v1/auth/revoke invalidates the active token");
    const revokeRes = await fetch(`${baseUrl}/revoke`, {
      method: "POST",
      headers: { Authorization: `Bearer ${activeToken}` },
    });
    if (revokeRes.status !== 204) {
      throw new Error(`Expected 204 from revoke, got ${revokeRes.status}`);
    }
    const revokedProfileRes = await fetch(`${baseUrl}/me`, {
      headers: { Authorization: `Bearer ${activeToken}` },
    });
    if (revokedProfileRes.status !== 401) {
      throw new Error(`Expected revoked token to return 401, got ${revokedProfileRes.status}`);
    }
    const revokedCheckoutRes = await fetch("http://localhost:3003/v1/checkout/quotes", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${activeToken}`,
      },
      body: JSON.stringify({}),
    });
    if (revokedCheckoutRes.status !== 401) {
      throw new Error(`Expected revoked token to be rejected by checkout, got ${revokedCheckoutRes.status}`);
    }
    console.log("  PASSED");

    // 10. Disk Persistence Test
    console.log("Test 10: Persistent Storage & Hash Security Check");
    const isolatedService1 = new AuthService(testStorageDir);
    const isolatedEmail = "persist@avenra.com";
    isolatedService1.signUp({
      fullName: "Persist User",
      email: isolatedEmail,
      password: "PersistPassword123",
      mobileNumber: "01100001111",
      address: "Giza, Egypt"
    });

    // Check file on disk directly
    const diskUsers = JSON.parse(fs.readFileSync(path.join(testStorageDir, "users.json"), "utf-8"));
    const diskUser = diskUsers.find((u: any) => u.email === isolatedEmail);
    if (!diskUser) throw new Error("User was not saved to disk file!");
    if (!diskUser.passwordHash.includes(":") || diskUser.passwordHash.length < 50) {
      throw new Error("Password was not securely salted and hashed on disk!");
    }
    if (diskUser.password) {
      throw new Error("SECURITY VIOLATION: Plaintext password found in disk storage!");
    }

    const diskSessions = JSON.parse(fs.readFileSync(path.join(testStorageDir, "sessions.json"), "utf-8"));
    if (diskSessions.some((session: any) => session.token || !session.tokenHash)) {
      throw new Error("SECURITY VIOLATION: Plaintext bearer token found in disk storage!");
    }

    // Fresh instance reload
    const isolatedService2 = new AuthService(testStorageDir);
    const loginResult = isolatedService2.signIn({
      email: isolatedEmail,
      password: "PersistPassword123"
    });
    if (!loginResult || loginResult.user.email !== isolatedEmail) {
      throw new Error("Failed to sign in from reloaded disk storage!");
    }
    if (!isolatedService2.getUserByToken(loginResult.token)) {
      throw new Error("Hashed session token was not accepted after reload!");
    }
    console.log("  PASSED");

    console.log("--- ALL AUTH INTEGRATION TESTS PASSED SUCCESSFULLY! ---");
  } finally {
    server.close();
    if (fs.existsSync(testStorageDir)) {
      fs.rmSync(testStorageDir, { recursive: true, force: true });
    }
  }
}

runAuthTests().catch((err) => {
  console.error("Auth test failed:", err);
  process.exit(1);
});
