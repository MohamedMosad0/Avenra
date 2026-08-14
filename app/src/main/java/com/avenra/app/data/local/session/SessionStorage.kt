package com.avenra.app.data.local.session

import com.avenra.app.domain.model.UserProfile
import kotlinx.coroutines.flow.StateFlow

interface SessionStorage {
    val currentUser: StateFlow<UserProfile?>
    val isLoggedIn: StateFlow<Boolean>
    fun saveSession(user: UserProfile, token: String)
    fun clearSession()
    fun getToken(): String?
    fun getUserProfile(): UserProfile?
}
