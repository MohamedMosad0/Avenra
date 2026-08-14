package com.avenra.app.presentation.auth.signup

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

class SignUpViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SignUpUiState())
    val uiState: StateFlow<SignUpUiState> = _uiState.asStateFlow()

    fun onFullNameChange(name: String) {
        _uiState.update { it.copy(fullName = name, fullNameError = null, generalError = null) }
    }

    fun onMobileChange(phone: String) {
        _uiState.update { it.copy(mobileNumber = phone, mobileError = null, generalError = null) }
    }

    fun onEmailChange(email: String) {
        _uiState.update { it.copy(email = email, emailError = null, generalError = null) }
    }

    fun onPasswordChange(password: String) {
        _uiState.update { it.copy(password = password, passwordError = null, generalError = null) }
    }

    fun onAddressChange(address: String) {
        _uiState.update { it.copy(address = address, generalError = null) }
    }

    fun togglePasswordVisibility() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun signUp() {
        val currentState = _uiState.value
        val fullName = currentState.fullName.trim()
        val email = currentState.email.trim()
        val password = currentState.password
        val mobile = currentState.mobileNumber.trim()
        val address = currentState.address.trim()

        var hasError = false
        var nameErr: String? = null
        var emailErr: String? = null
        var passErr: String? = null
        var mobErr: String? = null

        if (fullName.isBlank()) {
            nameErr = "Full name is required"
            hasError = true
        } else if (fullName.length < 2) {
            nameErr = "Full name must be at least 2 characters"
            hasError = true
        }

        if (mobile.isNotBlank() && mobile.length < 8) {
            mobErr = "Mobile number must be at least 8 digits"
            hasError = true
        }

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
        } else if (password.length < 10) {
            passErr = "Password must be at least 10 characters"
            hasError = true
        }

        if (hasError) {
            _uiState.update {
                it.copy(
                    fullNameError = nameErr,
                    mobileError = mobErr,
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
                fullNameError = null,
                mobileError = null,
                emailError = null,
                passwordError = null,
                generalError = null
            )
        }

        viewModelScope.launch {
            authRepository.signUp(
                fullName = fullName,
                email = email,
                password = password,
                mobileNumber = mobile.ifBlank { null },
                address = address.ifBlank { null }
            ).collect { result ->
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
                return SignUpViewModel(repo) as T
            }
        }
    }
}
