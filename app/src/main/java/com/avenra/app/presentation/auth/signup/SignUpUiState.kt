package com.avenra.app.presentation.auth.signup

data class SignUpUiState(
    val fullName: String = "",
    val mobileNumber: String = "",
    val email: String = "",
    val password: String = "",
    val address: String = "",
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val fullNameError: String? = null,
    val mobileError: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val generalError: String? = null,
    val isSuccess: Boolean = false
)
