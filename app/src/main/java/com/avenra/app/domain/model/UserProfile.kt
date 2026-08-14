package com.avenra.app.domain.model

data class UserProfile(
    val id: String = "",
    val fullName: String,
    val email: String,
    val passwordMasked: String = "******************",
    val mobileNumber: String = "",
    val address: String = "",
    val isLoggedIn: Boolean = true
)

