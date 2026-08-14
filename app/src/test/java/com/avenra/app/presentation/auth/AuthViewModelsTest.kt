package com.avenra.app.presentation.auth

import com.avenra.app.data.local.session.SessionStorage
import com.avenra.app.data.remote.api.ApiService
import com.avenra.app.data.remote.dto.AuthResponseDto
import com.avenra.app.data.remote.dto.CategoriesResponseDto
import com.avenra.app.data.remote.dto.CheckoutQuoteRequestDto
import com.avenra.app.data.remote.dto.CheckoutQuoteResponseDto
import com.avenra.app.data.remote.dto.CreateOrderRequestDto
import com.avenra.app.data.remote.dto.HomeResponseDto
import com.avenra.app.data.remote.dto.OrderResponseDto
import com.avenra.app.data.remote.dto.ProductDetailResponseDto
import com.avenra.app.data.remote.dto.ProfileResponseDto
import com.avenra.app.data.remote.dto.ProductsResponseDto
import com.avenra.app.data.remote.dto.SignInRequestDto
import com.avenra.app.data.remote.dto.SignUpRequestDto
import com.avenra.app.data.remote.dto.UserDto
import com.avenra.app.data.repository.AuthRepository
import com.avenra.app.domain.model.UserProfile
import com.avenra.app.presentation.account.AccountUiState
import com.avenra.app.presentation.account.AccountViewModel
import com.avenra.app.presentation.auth.signin.SignInViewModel
import com.avenra.app.presentation.auth.signup.SignUpViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

import com.avenra.app.domain.model.DataError
import com.avenra.app.domain.model.NetworkResult
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelsTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeApiService(
        var shouldSucceed: Boolean = true,
        var errorCode: String = "ERROR",
        var errorMessage: String = "Error",
        var profileException: Exception? = null,
        var revokeException: Exception? = null,
        var revokedTokens: MutableList<String> = mutableListOf()
    ) : ApiService {
        override suspend fun signUp(request: SignUpRequestDto): AuthResponseDto {
            if (!shouldSucceed) {
                val errorBody = "{\"status\":\"error\",\"code\":\"$errorCode\",\"message\":\"$errorMessage\"}"
                    .toResponseBody("application/json".toMediaTypeOrNull())
                throw HttpException(Response.error<Any>(409, errorBody))
            }
            return AuthResponseDto(
                user = UserDto(
                    id = "usr_101",
                    fullName = request.fullName,
                    email = request.email,
                    mobileNumber = request.mobileNumber,
                    address = request.address,
                    createdAt = "2026-08-13T00:00:00Z"
                ),
                token = "tok_signup_123"
            )
        }

        override suspend fun signIn(request: SignInRequestDto): AuthResponseDto {
            if (!shouldSucceed) {
                val errorBody = "{\"status\":\"error\",\"code\":\"$errorCode\",\"message\":\"$errorMessage\"}"
                    .toResponseBody("application/json".toMediaTypeOrNull())
                throw HttpException(Response.error<Any>(401, errorBody))
            }
            return AuthResponseDto(
                user = UserDto(
                    id = "usr_101",
                    fullName = "Test LoggedIn User",
                    email = request.email,
                    mobileNumber = "01012345678",
                    address = "Cairo, Egypt",
                    createdAt = "2026-08-13T00:00:00Z"
                ),
                token = "tok_signin_123"
            )
        }

        override suspend fun getProfile(token: String): ProfileResponseDto {
            if (profileException != null) {
                throw profileException!!
            }
            if (!shouldSucceed) {
                val errorBody = "{\"status\":\"error\",\"code\":\"$errorCode\",\"message\":\"$errorMessage\"}"
                    .toResponseBody("application/json".toMediaTypeOrNull())
                throw HttpException(Response.error<Any>(401, errorBody))
            }
            return ProfileResponseDto(
                user = UserDto(
                    id = "usr_restored",
                    fullName = "Restored Profile User",
                    email = "restored@avenra.com",
                    mobileNumber = "01234567890",
                    address = "Alexandria, Egypt",
                    createdAt = "2026-08-13T00:00:00Z"
                )
            )
        }

        override suspend fun revokeSession(token: String) {
            if (revokeException != null) {
                throw revokeException!!
            }
            revokedTokens.add(token)
        }

        override suspend fun getHome(): HomeResponseDto = throw NotImplementedError()
        override suspend fun getCategories(): CategoriesResponseDto = throw NotImplementedError()
        override suspend fun getProducts(categoryId: String?, query: String?): ProductsResponseDto = throw NotImplementedError()
        override suspend fun getProductById(productId: String): ProductDetailResponseDto = throw NotImplementedError()
        override suspend fun createCheckoutQuote(token: String, request: CheckoutQuoteRequestDto): CheckoutQuoteResponseDto = throw NotImplementedError()
        override suspend fun createOrder(token: String, idempotencyKey: String, request: CreateOrderRequestDto): OrderResponseDto = throw NotImplementedError()
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

    @Test
    fun signInViewModel_validationErrors_triggersCorrectly() {
        val fakeApi = FakeApiService()
        val fakeStorage = FakeSessionStorage()
        val authRepo = AuthRepository(apiService = fakeApi, sessionStorage = fakeStorage)
        val viewModel = SignInViewModel(authRepo)

        // Initial empty submit
        viewModel.signIn()
        assertNotNull(viewModel.uiState.value.emailError)
        assertNotNull(viewModel.uiState.value.passwordError)

        // Invalid short password
        viewModel.onEmailChange("valid@example.com")
        viewModel.onPasswordChange("123")
        viewModel.signIn()
        assertNull(viewModel.uiState.value.emailError)
        assertEquals("Password must be at least 6 characters", viewModel.uiState.value.passwordError)

        // Toggle password visibility
        assertFalse(viewModel.uiState.value.isPasswordVisible)
        viewModel.togglePasswordVisibility()
        assertTrue(viewModel.uiState.value.isPasswordVisible)
    }

    @Test
    fun signInViewModel_successfulSignIn_updatesStateAndPersistsSession() = runTest(testDispatcher) {
        val fakeApi = FakeApiService(shouldSucceed = true)
        val fakeStorage = FakeSessionStorage()
        val authRepo = AuthRepository(apiService = fakeApi, sessionStorage = fakeStorage)
        val viewModel = SignInViewModel(authRepo)

        viewModel.onEmailChange("user@avenra.com")
        viewModel.onPasswordChange("ValidPassword123")
        viewModel.signIn()

        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isSuccess)
        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.generalError)

        // Verify session was persisted
        assertTrue(fakeStorage.isLoggedIn.value)
        assertEquals("user@avenra.com", fakeStorage.getUserProfile()?.email)
        assertEquals("tok_signin_123", fakeStorage.getToken())
    }

    @Test
    fun signInViewModel_invalidCredentials_showsError() = runTest(testDispatcher) {
        val fakeApi = FakeApiService(
            shouldSucceed = false,
            errorCode = "INVALID_CREDENTIALS",
            errorMessage = "Invalid email or password."
        )
        val fakeStorage = FakeSessionStorage()
        val authRepo = AuthRepository(apiService = fakeApi, sessionStorage = fakeStorage)
        val viewModel = SignInViewModel(authRepo)

        viewModel.onEmailChange("user@avenra.com")
        viewModel.onPasswordChange("WrongPassword123")
        viewModel.signIn()

        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isSuccess)
        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals("Invalid email or password.", viewModel.uiState.value.generalError)
        assertFalse(fakeStorage.isLoggedIn.value)
    }

    @Test
    fun signUpViewModel_validationErrors_triggersCorrectly() {
        val fakeApi = FakeApiService()
        val fakeStorage = FakeSessionStorage()
        val authRepo = AuthRepository(apiService = fakeApi, sessionStorage = fakeStorage)
        val viewModel = SignUpViewModel(authRepo)

        // Initial submit
        viewModel.signUp()
        assertNotNull(viewModel.uiState.value.fullNameError)
        assertNotNull(viewModel.uiState.value.emailError)
        assertNotNull(viewModel.uiState.value.passwordError)

        // Short full name
        viewModel.onFullNameChange("A")
        viewModel.onEmailChange("user@avenra.com")
        viewModel.onPasswordChange("ValidPass123")
        viewModel.signUp()
        assertEquals("Full name must be at least 2 characters", viewModel.uiState.value.fullNameError)
    }

    @Test
    fun signUpViewModel_successfulSignUp_updatesStateAndPersistsSession() = runTest(testDispatcher) {
        val fakeApi = FakeApiService(shouldSucceed = true)
        val fakeStorage = FakeSessionStorage()
        val authRepo = AuthRepository(apiService = fakeApi, sessionStorage = fakeStorage)
        val viewModel = SignUpViewModel(authRepo)

        viewModel.onFullNameChange("Mohamed Nabil")
        viewModel.onMobileChange("01012345678")
        viewModel.onEmailChange("newuser@avenra.com")
        viewModel.onPasswordChange("Secret12345")
        viewModel.onAddressChange("Cairo")
        viewModel.signUp()

        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isSuccess)
        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.generalError)

        assertTrue(fakeStorage.isLoggedIn.value)
        assertEquals("newuser@avenra.com", fakeStorage.getUserProfile()?.email)
        assertEquals("Mohamed Nabil", fakeStorage.getUserProfile()?.fullName)
        assertEquals("tok_signup_123", fakeStorage.getToken())
    }

    @Test
    fun signUpViewModel_duplicateEmail_showsError() = runTest(testDispatcher) {
        val fakeApi = FakeApiService(
            shouldSucceed = false,
            errorCode = "EMAIL_ALREADY_EXISTS",
            errorMessage = "An account with this email already exists."
        )
        val fakeStorage = FakeSessionStorage()
        val authRepo = AuthRepository(apiService = fakeApi, sessionStorage = fakeStorage)
        val viewModel = SignUpViewModel(authRepo)

        viewModel.onFullNameChange("Duplicate User")
        viewModel.onEmailChange("duplicate@avenra.com")
        viewModel.onPasswordChange("Secret12345")
        viewModel.signUp()

        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isSuccess)
        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals("An account with this email already exists.", viewModel.uiState.value.generalError)
    }

    @Test
    fun accountViewModel_reactsToSessionChangesAndSignOut() = runTest(testDispatcher) {
        val fakeStorage = FakeSessionStorage()
        val viewModel = AccountViewModel(
            sessionStorage = fakeStorage,
            authRepository = AuthRepository(apiService = FakeApiService(), sessionStorage = fakeStorage)
        )

        // Initially no session -> Unauthenticated
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is AccountUiState.Unauthenticated)

        // Save session (User signs in)
        fakeStorage.saveSession(
            UserProfile(
                id = "usr_1",
                fullName = "Ahmed Ali",
                email = "ahmed@avenra.com",
                mobileNumber = "01122334455",
                address = "Alexandria",
                isLoggedIn = true
            ),
            "tok_abc"
        )
        advanceUntilIdle()

        val stateAfterLogin = viewModel.uiState.value
        assertTrue(stateAfterLogin is AccountUiState.Success)
        assertEquals("Ahmed Ali", (stateAfterLogin as AccountUiState.Success).profile.fullName)

        // Sign Out
        viewModel.signOut()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is AccountUiState.Unauthenticated)
        assertNull(fakeStorage.getUserProfile())
        assertFalse(fakeStorage.isLoggedIn.value)
    }

    @Test
    fun fetchCurrentProfile_withValidToken_restoresProfileAndEmitsSuccess() = runTest(testDispatcher) {
        val fakeApi = FakeApiService(shouldSucceed = true)
        val fakeStorage = FakeSessionStorage()
        fakeStorage.saveSession(
            UserProfile(
                id = "usr_old",
                fullName = "Old Name",
                email = "old@avenra.com",
                mobileNumber = "",
                address = "",
                isLoggedIn = true
            ),
            "tok_valid_token"
        )

        val authRepo = AuthRepository(apiService = fakeApi, sessionStorage = fakeStorage)
        var emittedResult: NetworkResult<UserProfile>? = null
        authRepo.fetchCurrentProfile().collect { result ->
            emittedResult = result
        }

        assertTrue(emittedResult is NetworkResult.Success)
        val user = (emittedResult as NetworkResult.Success).data
        assertEquals("usr_restored", user.id)
        assertEquals("Restored Profile User", user.fullName)
        assertEquals("restored@avenra.com", user.email)
        assertTrue(fakeStorage.isLoggedIn.value)
    }

    @Test
    fun fetchCurrentProfile_with401Unauthorized_clearsSessionAndEmitsError() = runTest(testDispatcher) {
        val fakeApi = FakeApiService(
            shouldSucceed = false,
            errorCode = "UNAUTHORIZED",
            errorMessage = "Invalid or expired token."
        )
        val fakeStorage = FakeSessionStorage()
        fakeStorage.saveSession(
            UserProfile(
                id = "usr_expired",
                fullName = "Expired User",
                email = "expired@avenra.com",
                mobileNumber = "",
                address = "",
                isLoggedIn = true
            ),
            "tok_expired_token"
        )

        val authRepo = AuthRepository(apiService = fakeApi, sessionStorage = fakeStorage)
        var emittedResult: NetworkResult<UserProfile>? = null
        authRepo.fetchCurrentProfile().collect { result ->
            emittedResult = result
        }

        assertTrue(emittedResult is NetworkResult.Error)
        val error = (emittedResult as NetworkResult.Error).error
        assertTrue(error is DataError.Server)
        assertEquals(401, (error as DataError.Server).statusCode)
        assertEquals("UNAUTHORIZED", error.errorCode)

        // Session must be cleared on 401
        assertFalse(fakeStorage.isLoggedIn.value)
        assertNull(fakeStorage.getToken())
        assertNull(fakeStorage.getUserProfile())
    }

    @Test
    fun fetchCurrentProfile_withNoToken_emitsUnauthorizedWithoutNetworkCall() = runTest(testDispatcher) {
        val fakeApi = FakeApiService(shouldSucceed = true)
        val fakeStorage = FakeSessionStorage() // no saved token

        val authRepo = AuthRepository(apiService = fakeApi, sessionStorage = fakeStorage)
        var emittedResult: NetworkResult<UserProfile>? = null
        authRepo.fetchCurrentProfile().collect { result ->
            emittedResult = result
        }

        assertTrue(emittedResult is NetworkResult.Error)
        assertEquals(DataError.Unauthorized, (emittedResult as NetworkResult.Error).error)
    }

    @Test
    fun fetchCurrentProfile_withNetworkIOException_fallsBackToCachedProfile() = runTest(testDispatcher) {
        val fakeApi = FakeApiService(
            profileException = IOException("Network connection timed out")
        )
        val fakeStorage = FakeSessionStorage()
        val cachedUser = UserProfile(
            id = "usr_cached",
            fullName = "Cached Offline User",
            email = "offline@avenra.com",
            mobileNumber = "01000000000",
            address = "Giza",
            isLoggedIn = true
        )
        fakeStorage.saveSession(cachedUser, "tok_offline_token")

        val authRepo = AuthRepository(apiService = fakeApi, sessionStorage = fakeStorage)
        var emittedResult: NetworkResult<UserProfile>? = null
        authRepo.fetchCurrentProfile().collect { result ->
            emittedResult = result
        }

        // On network error with cache available, cache is returned
        assertTrue(emittedResult is NetworkResult.Success)
        assertEquals("usr_cached", (emittedResult as NetworkResult.Success).data.id)
        assertEquals("Cached Offline User", (emittedResult as NetworkResult.Success).data.fullName)
        assertTrue(fakeStorage.isLoggedIn.value)
    }

    @Test
    fun signOut_invokesRevokeAndClearsLocalSession() = runTest(testDispatcher) {
        val fakeApi = FakeApiService()
        val fakeStorage = FakeSessionStorage()
        fakeStorage.saveSession(
            UserProfile(id = "u1", fullName = "Name", email = "email@avenra.com", isLoggedIn = true),
            "tok_to_revoke"
        )

        val authRepo = AuthRepository(apiService = fakeApi, sessionStorage = fakeStorage)
        authRepo.signOut()

        assertEquals(1, fakeApi.revokedTokens.size)
        assertEquals("Bearer tok_to_revoke", fakeApi.revokedTokens[0])
        assertFalse(fakeStorage.isLoggedIn.value)
        assertNull(fakeStorage.getToken())
    }

    @Test
    fun signOut_whenRevokeThrows_stillClearsLocalSession() = runTest(testDispatcher) {
        val fakeApi = FakeApiService(
            revokeException = RuntimeException("Revoke endpoint unreachable")
        )
        val fakeStorage = FakeSessionStorage()
        fakeStorage.saveSession(
            UserProfile(id = "u1", fullName = "Name", email = "email@avenra.com", isLoggedIn = true),
            "tok_to_revoke"
        )

        val authRepo = AuthRepository(apiService = fakeApi, sessionStorage = fakeStorage)
        authRepo.signOut()

        // Local session must still be cleared
        assertFalse(fakeStorage.isLoggedIn.value)
        assertNull(fakeStorage.getToken())
    }
}
