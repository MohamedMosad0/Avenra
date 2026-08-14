package com.avenra.app.presentation.account

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.avenra.app.data.local.session.SessionStorage
import com.avenra.app.data.local.session.UserSessionStorage
import com.avenra.app.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class AccountViewModel(
    private val sessionStorage: SessionStorage,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AccountUiState>(AccountUiState.Loading)
    val uiState: StateFlow<AccountUiState> = _uiState.asStateFlow()

    init {
        loadUserProfile()
    }

    fun loadUserProfile() {
        viewModelScope.launch {
            sessionStorage.currentUser
                .catch { error ->
                    _uiState.value = AccountUiState.Error(error.message ?: "Failed to load profile")
                }
                .collect { profile ->
                    if (profile != null && profile.isLoggedIn) {
                        _uiState.value = AccountUiState.Success(profile)
                    } else {
                        _uiState.value = AccountUiState.Unauthenticated
                    }
                }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            _uiState.value = AccountUiState.Unauthenticated
        }
    }

    companion object {
        fun provideFactory(context: Context): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val appContext = context.applicationContext
                return AccountViewModel(
                    sessionStorage = UserSessionStorage.getInstance(appContext),
                    authRepository = AuthRepository.getInstance(appContext)
                ) as T
            }
        }
    }
}
