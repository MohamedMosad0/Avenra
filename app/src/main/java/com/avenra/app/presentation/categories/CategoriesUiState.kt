package com.avenra.app.presentation.categories

import com.avenra.app.domain.model.Category

sealed interface CategoriesUiState {
    data object Loading : CategoriesUiState
    data class Success(
        val categories: List<Category>,
        val selectedCategory: Category?
    ) : CategoriesUiState
    data object Empty : CategoriesUiState
    data class Error(val message: String) : CategoriesUiState
}
