package com.avenra.app.presentation.cart

import com.avenra.app.domain.model.CartItem

sealed interface CartUiState {
    data object Loading : CartUiState
    data object Empty : CartUiState
    data class Success(
        val items: List<CartItem>,
        val subtotal: Double,
        val formattedSubtotal: String
    ) : CartUiState
    data class Error(val message: String) : CartUiState
}
