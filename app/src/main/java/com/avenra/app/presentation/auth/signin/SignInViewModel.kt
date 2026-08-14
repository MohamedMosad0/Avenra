package com.avenra.app.presentation.auth.signin

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.avenra.app.data.repository.AuthRepository
import com.avenra.app.domain.model.NetworkResult
import com.avenra.app.presentation.util.toDisplayMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SignInViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SignInUiState())
    val uiState: StateFlow<SignInUiState> = _uiState.asStateFlow()

    fun onEmailChange(email: String) {
        _uiState.update {
            it.copy(
                email = email,
                emailError = null,
                generalError = null
            )
        }
    }

    fun onPasswordChange(password: String) {
        _uiState.update {
            it.copy(
                password = password,
                passwordError = null,
                generalError = null
            )
        }
    }

    fun togglePasswordVisibility() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun signIn() {
        val currentState = _uiState.value
        val email = currentState.email.trim()
        val password = currentState.password

        var hasError = false
        var emailErr: String? = null
        var passErr: String? = null

        if (email.isBlank()) {
            emailErr = "Email is required"
            hasError = true
        } else if (!EMAIL_REGEX.matches(email)) {
            emailErr = "Please enter a valid email address"
            hasError = true
        }

        if (password.isBlank()) {
            passErr = "Password is required"
            hasError = true
        } else if (password.length < 6) {
            passErr = "Password must be at least 6 characters"
            hasError = true
        }

        if (hasError) {
            _uiState.update {
                it.copy(
                    emailError = emailErr,
                    passwordError = passErr,
                    generalError = null
                )
            }
            return
        }

        _uiState.update {
            it.copy(
                isLoading = true,
                emailError = null,
                passwordError = null,
                generalError = null
            )
        }

        viewModelScope.launch {
            authRepository.signIn(email, password).collect { result ->
                when (result) {
                    is NetworkResult.Success -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isSuccess = true,
                                generalError = null
                            )
                        }
                    }
                    is NetworkResult.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                generalError = result.error.toDisplayMessage()
                            )
                        }
                    }
                }
            }
        }
    }

    companion object {
        private val EMAIL_REGEX = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$".toRegex()

        fun provideFactory(context: Context): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val repo = AuthRepository.getInstance(context)
                return SignInViewModel(repo) as T
            }
        }
    }
}
