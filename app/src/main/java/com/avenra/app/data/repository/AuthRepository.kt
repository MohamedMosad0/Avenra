package com.avenra.app.data.repository

import android.content.Context
import com.avenra.app.data.local.session.SessionStorage
import com.avenra.app.data.local.session.UserSessionStorage
import com.avenra.app.data.remote.api.ApiService
import com.avenra.app.data.remote.api.NetworkModule
import com.avenra.app.data.remote.dto.SignInRequestDto
import com.avenra.app.data.remote.dto.SignUpRequestDto
import com.avenra.app.domain.model.DataError
import com.avenra.app.domain.model.NetworkResult
import com.avenra.app.domain.model.UserProfile
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import retrofit2.HttpException
import java.io.IOException

class AuthRepository(
    private val apiService: ApiService = NetworkModule.apiService,
    private val sessionStorage: SessionStorage
) {
    private val gson = Gson()

    val currentUser: StateFlow<UserProfile?> = sessionStorage.currentUser
    val isLoggedIn: StateFlow<Boolean> = sessionStorage.isLoggedIn

    fun signUp(
        fullName: String,
        email: String,
        password: String,
        mobileNumber: String?,
        address: String?
    ): Flow<NetworkResult<UserProfile>> = flow {
        try {
            val request = SignUpRequestDto(
                fullName = fullName.trim(),
                email = email.trim(),
                password = password,
                mobileNumber = mobileNumber?.trim(),
                address = address?.trim()
            )
            val response = apiService.signUp(request)
            val userProfile = response.user.toUserProfile(isLoggedIn = true)
            sessionStorage.saveSession(userProfile, response.token)
            emit(NetworkResult.Success(userProfile))
        } catch (e: HttpException) {
            emit(NetworkResult.Error(parseHttpError(e), e))
        } catch (e: IOException) {
            emit(NetworkResult.Error(DataError.Network, e))
        } catch (e: Exception) {
            emit(NetworkResult.Error(DataError.Unknown(e.message), e))
        }
    }

    fun signIn(
        email: String,
        password: String
    ): Flow<NetworkResult<UserProfile>> = flow {
        try {
            val request = SignInRequestDto(
                email = email.trim(),
                password = password
            )
            val response = apiService.signIn(request)
            val userProfile = response.user.toUserProfile(isLoggedIn = true)
            sessionStorage.saveSession(userProfile, response.token)
            emit(NetworkResult.Success(userProfile))
        } catch (e: HttpException) {
            emit(NetworkResult.Error(parseHttpError(e), e))
        } catch (e: IOException) {
            emit(NetworkResult.Error(DataError.Network, e))
        } catch (e: Exception) {
            emit(NetworkResult.Error(DataError.Unknown(e.message), e))
        }
    }

    fun fetchCurrentProfile(): Flow<NetworkResult<UserProfile>> = flow {
        val token = sessionStorage.getToken()
        if (token.isNullOrBlank()) {
            emit(NetworkResult.Error(DataError.Unauthorized))
            return@flow
        }

        try {
            val response = apiService.getProfile("Bearer $token")
            val userProfile = response.user.toUserProfile(isLoggedIn = true)
            sessionStorage.saveSession(userProfile, token)
            emit(NetworkResult.Success(userProfile))
        } catch (e: HttpException) {
            if (e.code() == 401) {
                sessionStorage.clearSession()
            }
            emit(NetworkResult.Error(parseHttpError(e), e))
        } catch (e: IOException) {
            val cached = sessionStorage.getUserProfile()
            if (cached != null) {
                emit(NetworkResult.Success(cached))
            } else {
                emit(NetworkResult.Error(DataError.Network, e))
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error(DataError.Unknown(e.message), e))
        }
    }

    suspend fun signOut() {
        val token = sessionStorage.getToken()
        try {
            if (!token.isNullOrBlank()) {
                apiService.revokeSession("Bearer $token")
            }
        } catch (_: Exception) {
            // Local sign-out must complete even when the backend is unreachable.
        } finally {
            sessionStorage.clearSession()
        }
    }

    private fun parseHttpError(exception: HttpException): DataError.Server {
        return try {
            val errorBody = exception.response()?.errorBody()?.string()
            if (!errorBody.isNullOrBlank()) {
                val json = gson.fromJson(errorBody, JsonObject::class.java)
                val code = json.get("code")?.asString
                val message = json.get("message")?.asString
                DataError.Server(statusCode = exception.code(), errorCode = code, message = message)
            } else {
                DataError.Server(statusCode = exception.code())
            }
        } catch (_: Exception) {
            DataError.Server(statusCode = exception.code())
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: AuthRepository? = null

        fun getInstance(context: Context): AuthRepository {
            return INSTANCE ?: synchronized(this) {
                val sessionStorage = UserSessionStorage.getInstance(context)
                val instance = AuthRepository(
                    apiService = NetworkModule.apiService,
                    sessionStorage = sessionStorage
                )
                INSTANCE = instance
                instance
            }
        }
    }
}
