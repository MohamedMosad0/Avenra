package com.avenra.app.presentation.products.details

import com.avenra.app.domain.model.Product

sealed interface ProductDetailsUiState {
    data object Loading : ProductDetailsUiState
    data class Success(val product: Product) : ProductDetailsUiState
    data class Error(val message: String) : ProductDetailsUiState
}
