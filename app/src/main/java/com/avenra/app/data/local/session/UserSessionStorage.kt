package com.avenra.app.data.local.session

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.avenra.app.domain.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class UserSessionStorage(
    private val prefs: SharedPreferences
) : SessionStorage {

    private val _currentUser = MutableStateFlow<UserProfile?>(loadSavedProfile())
    override val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(hasValidSession())
    override val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    override fun saveSession(user: UserProfile, token: String) {
        prefs.edit()
            .putString(KEY_TOKEN, token)
            .putString(KEY_USER_ID, user.id)
            .putString(KEY_USER_NAME, user.fullName)
            .putString(KEY_USER_EMAIL, user.email)
            .putString(KEY_USER_PHONE, user.mobileNumber)
            .putString(KEY_USER_ADDRESS, user.address)
            .apply()

        val loggedInUser = user.copy(isLoggedIn = true)
        _currentUser.value = loggedInUser
        _isLoggedIn.value = true
    }

    override fun clearSession() {
        prefs.edit()
            .remove(KEY_TOKEN)
            .remove(KEY_USER_ID)
            .remove(KEY_USER_NAME)
            .remove(KEY_USER_EMAIL)
            .remove(KEY_USER_PHONE)
            .remove(KEY_USER_ADDRESS)
            .apply()

        _currentUser.value = null
        _isLoggedIn.value = false
    }

    override fun getToken(): String? {
        return prefs.getString(KEY_TOKEN, null)
    }

    override fun getUserProfile(): UserProfile? {
        return _currentUser.value
    }

    private fun hasValidSession(): Boolean {
        val token = prefs.getString(KEY_TOKEN, null)
        val email = prefs.getString(KEY_USER_EMAIL, null)
        return !token.isNullOrBlank() && !email.isNullOrBlank()
    }

    private fun loadSavedProfile(): UserProfile? {
        val email = prefs.getString(KEY_USER_EMAIL, null) ?: return null
        val id = prefs.getString(KEY_USER_ID, "") ?: ""
        val name = prefs.getString(KEY_USER_NAME, "") ?: ""
        val phone = prefs.getString(KEY_USER_PHONE, "") ?: ""
        val address = prefs.getString(KEY_USER_ADDRESS, "") ?: ""

        return UserProfile(
            id = id,
            fullName = name,
            email = email,
            passwordMasked = "******************",
            mobileNumber = phone,
            address = address,
            isLoggedIn = true
        )
    }

    companion object {
        private const val ENCRYPTED_PREFS_NAME = "avenra_auth_session_encrypted"
        private const val KEY_TOKEN = "auth_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_PHONE = "user_phone"
        private const val KEY_USER_ADDRESS = "user_address"

        @Volatile
        private var INSTANCE: UserSessionStorage? = null

        fun createEncryptedPrefs(context: Context): SharedPreferences {
            val masterKey = MasterKey.Builder(context.applicationContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            return EncryptedSharedPreferences.create(
                context.applicationContext,
                ENCRYPTED_PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }

        fun getInstance(context: Context): UserSessionStorage {
            return INSTANCE ?: synchronized(this) {
                val encryptedPrefs = createEncryptedPrefs(context)
                val instance = UserSessionStorage(encryptedPrefs)
                INSTANCE = instance
                instance
            }
        }
    }
}
