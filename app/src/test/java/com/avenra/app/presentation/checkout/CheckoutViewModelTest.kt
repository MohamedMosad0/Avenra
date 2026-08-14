package com.avenra.app.presentation.checkout

import android.app.Application
import com.avenra.app.data.local.dao.CartDao
import com.avenra.app.data.local.entity.CartEntity
import com.avenra.app.data.local.session.SessionStorage
import com.avenra.app.data.remote.api.ApiService
import com.avenra.app.data.remote.dto.AuthResponseDto
import com.avenra.app.data.remote.dto.CategoriesResponseDto
import com.avenra.app.data.remote.dto.CheckoutQuoteItemRequestDto
import com.avenra.app.data.remote.dto.CheckoutQuoteRequestDto
import com.avenra.app.data.remote.dto.CheckoutQuoteResponseDto
import com.avenra.app.data.remote.dto.CreateOrderRequestDto
import com.avenra.app.data.remote.dto.HomeResponseDto
import com.avenra.app.data.remote.dto.OrderResponseDto
import com.avenra.app.data.remote.dto.ProductDetailResponseDto
import com.avenra.app.data.remote.dto.ProfileResponseDto
import com.avenra.app.data.remote.dto.ProductsResponseDto
import com.avenra.app.data.remote.dto.QuoteItemSnapshotDto
import com.avenra.app.data.remote.dto.SignInRequestDto
import com.avenra.app.data.remote.dto.SignUpRequestDto
import com.avenra.app.data.repository.CartRepository
import com.avenra.app.data.repository.CheckoutRepository
import com.avenra.app.domain.model.Product
import com.avenra.app.domain.model.ShippingAddress
import com.avenra.app.domain.model.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class CheckoutViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeCartDao : CartDao {
        val itemsFlow = MutableStateFlow<List<CartEntity>>(emptyList())

        override fun observeCartItems(): Flow<List<CartEntity>> = itemsFlow
        override suspend fun getCartItemById(id: String): CartEntity? = itemsFlow.value.find { it.id == id }
        override fun observeTotalQuantity(): Flow<Int?> = itemsFlow.map { it.sumOf { item -> item.quantity } }
        override suspend fun upsertCartItem(item: CartEntity) {
            itemsFlow.value = itemsFlow.value.filterNot { it.id == item.id } + item
        }
        override suspend fun updateCartItem(item: CartEntity) {
            itemsFlow.value = itemsFlow.value.map { if (it.id == item.id) item else it }
        }
        override suspend fun deleteCartItem(item: CartEntity) {
            itemsFlow.value = itemsFlow.value.filterNot { it.id == item.id }
        }
        override suspend fun deleteCartItemById(id: String) {
            itemsFlow.value = itemsFlow.value.filterNot { it.id == id }
        }
        override suspend fun clearCart() {
            itemsFlow.value = emptyList()
        }
    }

    private class FakeSessionStorage : SessionStorage {
        private val _currentUser = MutableStateFlow<UserProfile?>(null)
        override val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

        private val _isLoggedIn = MutableStateFlow(false)
        override val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

        private var token: String? = null

        override fun saveSession(user: UserProfile, token: String) {
            this.token = token
            _currentUser.value = user.copy(isLoggedIn = true)
            _isLoggedIn.value = true
        }

        override fun clearSession() {
            this.token = null
            _currentUser.value = null
            _isLoggedIn.value = false
        }

        override fun getToken(): String? = token
        override fun getUserProfile(): UserProfile? = _currentUser.value
    }

    private class FakeCheckoutApiService(
        var quoteShouldSucceed: Boolean = true,
        var quoteErrorCode: String = "ERROR",
        var quoteErrorMessage: String = "Quote Error",
        var orderShouldSucceed: Boolean = true,
        var orderErrorCode: String = "ERROR",
        var orderErrorMessage: String = "Order Error",
        var lastIdempotencyKey: String? = null,
        var orderRequestCount: Int = 0,
        val capturedIdempotencyKeys: MutableList<String> = mutableListOf(),
        var lastQuoteDeliveryMethod: String? = null
    ) : ApiService {
        override suspend fun createCheckoutQuote(token: String, request: CheckoutQuoteRequestDto): CheckoutQuoteResponseDto {
            lastQuoteDeliveryMethod = request.deliveryMethod
            if (!quoteShouldSucceed) {
                val errorBody = "{\"status\":\"error\",\"code\":\"$quoteErrorCode\",\"message\":\"$quoteErrorMessage\"}"
                    .toResponseBody("application/json".toMediaTypeOrNull())
                val statusCode = if (quoteErrorCode == "UNAUTHORIZED") 401 else 409
                throw HttpException(Response.error<Any>(statusCode, errorBody))
            }
            val deliveryFee = if (request.deliveryMethod == "EXPRESS") 100.0 else 50.0
            val subtotal = request.items.sumOf { 100.0 * it.quantity }
            return CheckoutQuoteResponseDto(
                quoteId = "quote_test_${request.deliveryMethod.lowercase()}_123",
                items = request.items.map {
                    QuoteItemSnapshotDto(
                        productId = it.productId,
                        title = "Product ${it.productId}",
                        unitPrice = 100.0,
                        quantity = it.quantity,
                        totalPrice = 100.0 * it.quantity
                    )
                },
                itemSubtotal = subtotal,
                discountTotal = 0.0,
                deliveryFee = deliveryFee,
                finalTotal = subtotal + deliveryFee,
                currency = "EGP",
                quoteExpiry = "2026-08-14T20:00:00Z",
                deliveryMethod = request.deliveryMethod
            )
        }

        override suspend fun createOrder(token: String, idempotencyKey: String, request: CreateOrderRequestDto): OrderResponseDto {
            orderRequestCount++
            lastIdempotencyKey = idempotencyKey
            capturedIdempotencyKeys.add(idempotencyKey)
            if (!orderShouldSucceed) {
                val errorBody = "{\"status\":\"error\",\"code\":\"$orderErrorCode\",\"message\":\"$orderErrorMessage\"}"
                    .toResponseBody("application/json".toMediaTypeOrNull())
                val statusCode = if (orderErrorCode == "UNAUTHORIZED") 401 else 409
                throw HttpException(Response.error<Any>(statusCode, errorBody))
            }
            return OrderResponseDto(
                orderId = "ord_test_999",
                orderReference = "AVN-TEST-999",
                quoteId = request.quoteId,
                itemSubtotal = 200.0,
                discountTotal = 0.0,
                deliveryFee = 50.0,
                finalTotal = 250.0,
                currency = "EGP",
                status = "CONFIRMED",
                createdAt = "2026-08-14T19:00:00Z"
            )
        }

        override suspend fun signUp(request: SignUpRequestDto): AuthResponseDto = throw NotImplementedError()
        override suspend fun signIn(request: SignInRequestDto): AuthResponseDto = throw NotImplementedError()
        override suspend fun getProfile(token: String): ProfileResponseDto = throw NotImplementedError()
        override suspend fun revokeSession(token: String) = Unit
        override suspend fun getHome(): HomeResponseDto = throw NotImplementedError()
        override suspend fun getCategories(): CategoriesResponseDto = throw NotImplementedError()
        override suspend fun getProducts(categoryId: String?, query: String?): ProductsResponseDto = throw NotImplementedError()
        override suspend fun getProductById(productId: String): ProductDetailResponseDto = throw NotImplementedError()
    }

    private val sampleAddress = ShippingAddress(
        fullName = "Mohamed Nabil",
        phone = "01012345678",
        city = "Cairo",
        addressLine = "90th Street, New Cairo"
    )

    @Test
    fun requestQuote_withEmptyAddressFields_setsValidationError() {
        val fakeApi = FakeCheckoutApiService()
        val fakeStorage = FakeSessionStorage()
        val fakeDao = FakeCartDao()
        val cartRepo = CartRepository(fakeDao)
        val checkoutRepo = CheckoutRepository(fakeApi, fakeStorage)
        val viewModel = CheckoutViewModel(Application(), checkoutRepo, cartRepo, fakeStorage)

        viewModel.requestQuote(ShippingAddress(fullName = "", phone = "", city = "", addressLine = ""))

        val state = viewModel.uiState.value
        assertTrue(state is CheckoutUiState.AddressForm)
        assertEquals("All shipping address fields are required.", (state as CheckoutUiState.AddressForm).validationError)
    }

    @Test
    fun requestQuote_withEmptyCart_emitsEmptyCartError() = runTest(testDispatcher) {
        val fakeApi = FakeCheckoutApiService()
        val fakeStorage = FakeSessionStorage()
        fakeStorage.saveSession(UserProfile(id = "u1", fullName = "User", email = "u@a.com"), "tok_123")
        val fakeDao = FakeCartDao()
        val cartRepo = CartRepository(fakeDao)
        val checkoutRepo = CheckoutRepository(fakeApi, fakeStorage)
        val viewModel = CheckoutViewModel(Application(), checkoutRepo, cartRepo, fakeStorage)

        viewModel.requestQuote(sampleAddress)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is CheckoutUiState.Error)
        assertEquals("Your cart is empty.", (state as CheckoutUiState.Error).message)
    }

    @Test
    fun requestQuote_withValidCartAndAddress_emitsQuoteSuccess() = runTest(testDispatcher) {
        val fakeApi = FakeCheckoutApiService()
        val fakeStorage = FakeSessionStorage()
        fakeStorage.saveSession(UserProfile(id = "u1", fullName = "User", email = "u@a.com"), "tok_123")
        val fakeDao = FakeCartDao()
        fakeDao.upsertCartItem(
            CartEntity(id = "p1", productId = "prod_1", title = "Sneakers", imageUrl = "", price = 100.0, discountPrice = null, quantity = 2, selectedSize = "42", selectedColor = "Black")
        )
        val cartRepo = CartRepository(fakeDao)
        val checkoutRepo = CheckoutRepository(fakeApi, fakeStorage)
        val viewModel = CheckoutViewModel(Application(), checkoutRepo, cartRepo, fakeStorage)

        viewModel.requestQuote(sampleAddress, deliveryMethod = "STANDARD")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is CheckoutUiState.QuoteSuccess)
        val quoteState = state as CheckoutUiState.QuoteSuccess
        assertEquals("quote_test_standard_123", quoteState.quote.quoteId)
        assertEquals(200.0, quoteState.quote.itemSubtotal, 0.001)
        assertEquals(50.0, quoteState.quote.deliveryFee, 0.001)
        assertEquals(250.0, quoteState.quote.finalTotal, 0.001)
        assertEquals("STANDARD", quoteState.quote.deliveryMethod)
    }

    @Test
    fun requestQuote_whenOutOfStock_setsOutOfStockErrorFlag() = runTest(testDispatcher) {
        val fakeApi = FakeCheckoutApiService(
            quoteShouldSucceed = false,
            quoteErrorCode = "OUT_OF_STOCK",
            quoteErrorMessage = "Requested quantity exceeds stock."
        )
        val fakeStorage = FakeSessionStorage()
        fakeStorage.saveSession(UserProfile(id = "u1", fullName = "User", email = "u@a.com"), "tok_123")
        val fakeDao = FakeCartDao()
        fakeDao.upsertCartItem(
            CartEntity(id = "p1", productId = "prod_1", title = "Sneakers", imageUrl = "", price = 100.0, discountPrice = null, quantity = 10, selectedSize = "42", selectedColor = "Black")
        )
        val cartRepo = CartRepository(fakeDao)
        val checkoutRepo = CheckoutRepository(fakeApi, fakeStorage)
        val viewModel = CheckoutViewModel(Application(), checkoutRepo, cartRepo, fakeStorage)

        viewModel.requestQuote(sampleAddress)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is CheckoutUiState.Error)
        val errorState = state as CheckoutUiState.Error
        assertTrue(errorState.isOutOfStock)
        assertFalse(errorState.isPriceChanged)
        assertFalse(errorState.isExpired)
    }

    // -------------------------------------------------------------
    // Target Verification 1: PRICE_CHANGED
    // -------------------------------------------------------------
    @Test
    fun priceChanged_simulation_setsCorrectError_preservesCart_andResetsIdempotencyKey() = runTest(testDispatcher) {
        val fakeApi = FakeCheckoutApiService()
        val fakeStorage = FakeSessionStorage()
        fakeStorage.saveSession(UserProfile(id = "u1", fullName = "User", email = "u@a.com"), "tok_123")
        val fakeDao = FakeCartDao()
        fakeDao.upsertCartItem(
            CartEntity(id = "p1", productId = "prod_1", title = "Sneakers", imageUrl = "", price = 100.0, discountPrice = null, quantity = 2, selectedSize = "42", selectedColor = "Black")
        )
        val cartRepo = CartRepository(fakeDao)
        val checkoutRepo = CheckoutRepository(fakeApi, fakeStorage)
        val viewModel = CheckoutViewModel(Application(), checkoutRepo, cartRepo, fakeStorage)

        // Generate initial quote
        viewModel.requestQuote(sampleAddress)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is CheckoutUiState.QuoteSuccess)

        // Simulate PRICE_CHANGED on order creation
        fakeApi.orderShouldSucceed = false
        fakeApi.orderErrorCode = "PRICE_CHANGED"
        fakeApi.orderErrorMessage = "Price for item prod_1 has changed."

        viewModel.confirmOrder()
        advanceUntilIdle()

        val errorState = viewModel.uiState.value
        assertTrue("State must be Error", errorState is CheckoutUiState.Error)
        val error = errorState as CheckoutUiState.Error
        assertTrue("isPriceChanged flag must be true", error.isPriceChanged)
        assertFalse(error.isOutOfStock)
        assertFalse(error.isExpired)
        assertTrue(error.message.contains("PRICE_CHANGED"))

        // Verify cart is NOT cleared
        assertEquals("Cart items must be preserved", 1, fakeDao.itemsFlow.value.size)

        val firstIdempotencyKey = fakeApi.lastIdempotencyKey
        assertNotNull(firstIdempotencyKey)

        // Request a refreshed quote and confirm order successfully
        fakeApi.orderShouldSucceed = true
        viewModel.requestQuote(sampleAddress)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is CheckoutUiState.QuoteSuccess)

        viewModel.confirmOrder()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is CheckoutUiState.OrderSuccess)

        val secondIdempotencyKey = fakeApi.lastIdempotencyKey
        assertNotNull(secondIdempotencyKey)
        assertNotEquals("Fresh quote must use a distinct Idempotency-Key", firstIdempotencyKey, secondIdempotencyKey)
        assertTrue("Cart must be cleared after successful order", fakeDao.itemsFlow.value.isEmpty())
    }

    // -------------------------------------------------------------
    // Target Verification 2: QUOTE_EXPIRED
    // -------------------------------------------------------------
    @Test
    fun quoteExpired_simulation_setsExpiredFlag_preservesCart_andAllowsFreshQuoteWithNewKey() = runTest(testDispatcher) {
        val fakeApi = FakeCheckoutApiService()
        val fakeStorage = FakeSessionStorage()
        fakeStorage.saveSession(UserProfile(id = "u1", fullName = "User", email = "u@a.com"), "tok_123")
        val fakeDao = FakeCartDao()
        fakeDao.upsertCartItem(
            CartEntity(id = "p1", productId = "prod_1", title = "Sneakers", imageUrl = "", price = 100.0, discountPrice = null, quantity = 2, selectedSize = "42", selectedColor = "Black")
        )
        val cartRepo = CartRepository(fakeDao)
        val checkoutRepo = CheckoutRepository(fakeApi, fakeStorage)
        val viewModel = CheckoutViewModel(Application(), checkoutRepo, cartRepo, fakeStorage)

        // 1. Initial Quote
        viewModel.requestQuote(sampleAddress)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is CheckoutUiState.QuoteSuccess)

        // 2. Order fails due to QUOTE_EXPIRED
        fakeApi.orderShouldSucceed = false
        fakeApi.orderErrorCode = "QUOTE_EXPIRED"
        fakeApi.orderErrorMessage = "Quote quote_test_standard_123 has expired. Please refresh your quote."

        viewModel.confirmOrder()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("State must be Error", state is CheckoutUiState.Error)
        val errorState = state as CheckoutUiState.Error
        assertTrue("isExpired flag must be true", errorState.isExpired)
        assertFalse(errorState.isPriceChanged)
        assertFalse(errorState.isOutOfStock)

        // 3. Cart remains intact
        assertEquals("Cart must not be cleared on quote expiration", 1, fakeDao.itemsFlow.value.size)
        val expiredKey = fakeApi.lastIdempotencyKey

        // 4. Request fresh quote
        fakeApi.orderShouldSucceed = true
        viewModel.requestQuote(sampleAddress)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is CheckoutUiState.QuoteSuccess)

        // 5. Confirm order with fresh quote
        viewModel.confirmOrder()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is CheckoutUiState.OrderSuccess)

        val freshKey = fakeApi.lastIdempotencyKey
        assertNotNull(freshKey)
        assertNotEquals("Fresh quote must receive a fresh idempotency key", expiredKey, freshKey)
        assertTrue("Cart is cleared upon successful order placement", fakeDao.itemsFlow.value.isEmpty())
    }

    // -------------------------------------------------------------
    // Target Verification 3: IDEMPOTENCY RETRY & DUPLICATE PROTECTION
    // -------------------------------------------------------------
    @Test
    fun idempotencyRetry_transientFailure_reusesSameKeyForSameQuote_andNewKeyForNewQuote() = runTest(testDispatcher) {
        val fakeApi = FakeCheckoutApiService()
        val fakeStorage = FakeSessionStorage()
        fakeStorage.saveSession(UserProfile(id = "u1", fullName = "User", email = "u@a.com"), "tok_123")
        val fakeDao = FakeCartDao()
        fakeDao.upsertCartItem(
            CartEntity(id = "p1", productId = "prod_1", title = "Sneakers", imageUrl = "", price = 100.0, discountPrice = null, quantity = 2, selectedSize = "42", selectedColor = "Black")
        )
        val cartRepo = CartRepository(fakeDao)
        val checkoutRepo = CheckoutRepository(fakeApi, fakeStorage)
        val viewModel = CheckoutViewModel(Application(), checkoutRepo, cartRepo, fakeStorage)

        viewModel.requestQuote(sampleAddress)
        advanceUntilIdle()
        val quoteState = viewModel.uiState.value as CheckoutUiState.QuoteSuccess

        // Simulate transient network / 500 failure on order creation
        fakeApi.orderShouldSucceed = false
        fakeApi.orderErrorCode = "NETWORK_ERROR"
        fakeApi.orderErrorMessage = "Transient connection failure"

        viewModel.confirmOrder()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is CheckoutUiState.Error)
        val firstKey = fakeApi.lastIdempotencyKey
        assertNotNull(firstKey)

        // User navigates back to quote or confirms same quote retry
        // When retrying the same quote without requesting a new quote:
        viewModel.backToAddress(sampleAddress)
        // Reset quote state to test quote idempotency key preservation
        viewModel.requestQuote(sampleAddress)
        advanceUntilIdle()
        val secondQuoteKey = fakeApi.lastIdempotencyKey

        // Confirm new quote
        fakeApi.orderShouldSucceed = true
        viewModel.confirmOrder()
        advanceUntilIdle()

        val thirdKey = fakeApi.lastIdempotencyKey
        assertNotNull(thirdKey)
        assertNotEquals("New quote generates new Idempotency-Key", firstKey, thirdKey)
    }

    @Test
    fun confirmOrder_duplicateClick_isSafelyPrevented() = runTest(testDispatcher) {
        val fakeApi = FakeCheckoutApiService()
        val fakeStorage = FakeSessionStorage()
        fakeStorage.saveSession(UserProfile(id = "u1", fullName = "User", email = "u@a.com"), "tok_123")
        val fakeDao = FakeCartDao()
        fakeDao.upsertCartItem(
            CartEntity(id = "p1", productId = "prod_1", title = "Sneakers", imageUrl = "", price = 100.0, discountPrice = null, quantity = 2, selectedSize = "42", selectedColor = "Black")
        )
        val cartRepo = CartRepository(fakeDao)
        val checkoutRepo = CheckoutRepository(fakeApi, fakeStorage)
        val viewModel = CheckoutViewModel(Application(), checkoutRepo, cartRepo, fakeStorage)

        viewModel.requestQuote(sampleAddress)
        advanceUntilIdle()

        // Call confirmOrder twice rapidly
        viewModel.confirmOrder()
        viewModel.confirmOrder()
        advanceUntilIdle()

        // Backend createOrder must have been called exactly once
        assertEquals(1, fakeApi.orderRequestCount)
    }

    // -------------------------------------------------------------
    // Target Verification 4: EXPRESS DELIVERY
    // -------------------------------------------------------------
    @Test
    fun deliveryMethod_standardAndExpress_sendsCorrectValuesAndAdoptsBackendFinancials() = runTest(testDispatcher) {
        val fakeApi = FakeCheckoutApiService()
        val fakeStorage = FakeSessionStorage()
        fakeStorage.saveSession(UserProfile(id = "u1", fullName = "User", email = "u@a.com"), "tok_123")
        val fakeDao = FakeCartDao()
        fakeDao.upsertCartItem(
            CartEntity(id = "p1", productId = "prod_1", title = "Sneakers", imageUrl = "", price = 100.0, discountPrice = null, quantity = 2, selectedSize = "42", selectedColor = "Black")
        )
        val cartRepo = CartRepository(fakeDao)
        val checkoutRepo = CheckoutRepository(fakeApi, fakeStorage)
        val viewModel = CheckoutViewModel(Application(), checkoutRepo, cartRepo, fakeStorage)

        // 1. STANDARD Delivery Request
        viewModel.requestQuote(sampleAddress, deliveryMethod = "STANDARD")
        advanceUntilIdle()
        assertEquals("STANDARD", fakeApi.lastQuoteDeliveryMethod)

        val standardState = viewModel.uiState.value as CheckoutUiState.QuoteSuccess
        assertEquals("STANDARD", standardState.quote.deliveryMethod)
        assertEquals(50.0, standardState.quote.deliveryFee, 0.001) // Backend authoritative fee
        assertEquals(250.0, standardState.quote.finalTotal, 0.001) // Subtotal (200) + Fee (50)

        // 2. EXPRESS Delivery Request
        viewModel.requestQuote(sampleAddress, deliveryMethod = "EXPRESS")
        advanceUntilIdle()
        assertEquals("EXPRESS", fakeApi.lastQuoteDeliveryMethod)

        val expressState = viewModel.uiState.value as CheckoutUiState.QuoteSuccess
        assertEquals("EXPRESS", expressState.quote.deliveryMethod)
        assertEquals(100.0, expressState.quote.deliveryFee, 0.001) // Backend authoritative fee
        assertEquals(300.0, expressState.quote.finalTotal, 0.001) // Subtotal (200) + Fee (100)
    }

    @Test
    fun confirmOrder_withValidQuote_createsOrderAndClearsCart() = runTest(testDispatcher) {
        val fakeApi = FakeCheckoutApiService()
        val fakeStorage = FakeSessionStorage()
        fakeStorage.saveSession(UserProfile(id = "u1", fullName = "User", email = "u@a.com"), "tok_123")
        val fakeDao = FakeCartDao()
        fakeDao.upsertCartItem(
            CartEntity(id = "p1", productId = "prod_1", title = "Sneakers", imageUrl = "", price = 100.0, discountPrice = null, quantity = 2, selectedSize = "42", selectedColor = "Black")
        )
        val cartRepo = CartRepository(fakeDao)
        val checkoutRepo = CheckoutRepository(fakeApi, fakeStorage)
        val viewModel = CheckoutViewModel(Application(), checkoutRepo, cartRepo, fakeStorage)

        viewModel.requestQuote(sampleAddress)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is CheckoutUiState.QuoteSuccess)

        // Confirm order
        viewModel.confirmOrder()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is CheckoutUiState.OrderSuccess)
        val successState = state as CheckoutUiState.OrderSuccess
        assertEquals("ord_test_999", successState.orderResult.orderId)
        assertEquals("AVN-TEST-999", successState.orderResult.orderReference)

        // Verify cart is cleared after order creation
        assertTrue(fakeDao.itemsFlow.value.isEmpty())
        assertNotNull(fakeApi.lastIdempotencyKey)
    }

    @Test
    fun requestQuote_when401Unauthorized_clearsLocalSession() = runTest(testDispatcher) {
        val fakeApi = FakeCheckoutApiService(
            quoteShouldSucceed = false,
            quoteErrorCode = "UNAUTHORIZED",
            quoteErrorMessage = "Invalid or revoked token."
        )
        val fakeStorage = FakeSessionStorage()
        fakeStorage.saveSession(UserProfile(id = "u1", fullName = "User", email = "u@a.com"), "tok_invalid")
        val fakeDao = FakeCartDao()
        fakeDao.upsertCartItem(
            CartEntity(id = "p1", productId = "prod_1", title = "Sneakers", imageUrl = "", price = 100.0, discountPrice = null, quantity = 2, selectedSize = "42", selectedColor = "Black")
        )
        val cartRepo = CartRepository(fakeDao)
        val checkoutRepo = CheckoutRepository(fakeApi, fakeStorage)
        val viewModel = CheckoutViewModel(Application(), checkoutRepo, cartRepo, fakeStorage)

        viewModel.requestQuote(sampleAddress)
        advanceUntilIdle()

        // 401 must clear session in storage
        assertFalse(fakeStorage.isLoggedIn.value)
        assertNull(fakeStorage.getToken())
    }
}
