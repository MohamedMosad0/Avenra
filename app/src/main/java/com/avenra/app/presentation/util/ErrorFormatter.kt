package com.avenra.app.presentation.util

import com.avenra.app.domain.model.DataError

fun DataError.toDisplayMessage(): String {
    return when (this) {
        is DataError.Network -> "Network error. Please check your connection."
        is DataError.NotFound -> "Product not found."
        is DataError.Unauthorized -> "No active session found."
        is DataError.Server -> {
            when (errorCode) {
                "INVALID_CREDENTIALS" -> "Invalid email or password."
                "EMAIL_ALREADY_EXISTS" -> "An account with this email already exists."
                "VALIDATION_ERROR" -> message ?: "Validation error."
                "PRICE_CHANGED" -> message ?: "PRICE_CHANGED: Product price has changed."
                "OUT_OF_STOCK" -> message ?: "OUT_OF_STOCK: Product is out of stock."
                "QUOTE_EXPIRED" -> message ?: "QUOTE_EXPIRED: Quote has expired."
                else -> message ?: if (statusCode != null) "Server error ($statusCode). Please try again later." else "Server error. Please try again later."
            }
        }
        is DataError.Unknown -> message ?: "An unexpected error occurred."
    }
}
