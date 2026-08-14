package com.avenra.app.data.remote.dto

import com.avenra.app.domain.model.UserProfile
import com.google.gson.annotations.SerializedName

data class SignUpRequestDto(
    @SerializedName("fullName") val fullName: String,
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String,
    @SerializedName("mobileNumber") val mobileNumber: String? = null,
    @SerializedName("address") val address: String? = null
)

data class SignInRequestDto(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String
)

data class AuthResponseDto(
    @SerializedName("user") val user: UserDto,
    @SerializedName("token") val token: String
)

data class ProfileResponseDto(
    @SerializedName("user") val user: UserDto
)

data class UserDto(
    @SerializedName("id") val id: String,
    @SerializedName("fullName") val fullName: String,
    @SerializedName("email") val email: String,
    @SerializedName("mobileNumber") val mobileNumber: String? = null,
    @SerializedName("address") val address: String? = null,
    @SerializedName("createdAt") val createdAt: String? = null
) {
    fun toUserProfile(isLoggedIn: Boolean = true): UserProfile {
        return UserProfile(
            id = id,
            fullName = fullName,
            email = email,
            passwordMasked = "******************",
            mobileNumber = mobileNumber ?: "",
            address = address ?: "",
            isLoggedIn = isLoggedIn
        )
    }
}
