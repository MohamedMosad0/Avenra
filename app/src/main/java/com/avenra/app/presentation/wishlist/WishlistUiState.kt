package com.avenra.app.presentation.wishlist

import com.avenra.app.domain.model.WishlistItem

sealed interface WishlistUiState {
    data object Loading : WishlistUiState
    data object Empty : WishlistUiState
    data class Success(val products: List<WishlistItem>) : WishlistUiState
    data class Error(val message: String) : WishlistUiState
}
