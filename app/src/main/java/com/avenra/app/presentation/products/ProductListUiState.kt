package com.avenra.app.presentation.products

import com.avenra.app.domain.model.Product

sealed interface ProductListUiState {
    data object Loading : ProductListUiState
    data class Success(val products: List<Product>) : ProductListUiState
    data class Empty(val message: String = "No products available at this time.") : ProductListUiState
    data class Error(val message: String) : ProductListUiState
}
