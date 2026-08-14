package com.avenra.app.presentation.categories

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.avenra.app.data.repository.CartRepository
import com.avenra.app.data.repository.CategoryRepository
import com.avenra.app.domain.model.Category
import com.avenra.app.domain.model.NetworkResult
import com.avenra.app.presentation.util.toDisplayMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CategoriesViewModel(
    private val repository: CategoryRepository,
    private val cartRepository: CartRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<CategoriesUiState>(CategoriesUiState.Loading)
    val uiState: StateFlow<CategoriesUiState> = _uiState.asStateFlow()
    val cartCount = cartRepository.cartCount

    init {
        loadCategories()
    }

    fun loadCategories() {
        viewModelScope.launch {
            _uiState.value = CategoriesUiState.Loading
            when (val result = repository.getCategories()) {
                is NetworkResult.Success -> {
                    val categories = result.data
                    if (categories.isEmpty()) {
                        _uiState.value = CategoriesUiState.Empty
                    } else {
                        val initialSelected = categories.firstOrNull()
                        _uiState.value = CategoriesUiState.Success(
                            categories = categories,
                            selectedCategory = initialSelected
                        )
                    }
                }
                is NetworkResult.Error -> {
                    _uiState.value = CategoriesUiState.Error(result.error.toDisplayMessage())
                }
            }
        }
    }

    fun selectCategory(categoryId: String) {
        val currentState = _uiState.value
        if (currentState is CategoriesUiState.Success) {
            val selected = currentState.categories.find { it.id == categoryId }
            if (selected != null) {
                _uiState.value = currentState.copy(selectedCategory = selected)
            }
        }
    }

    fun retry() {
        loadCategories()
    }

    class Factory(context: Context) : ViewModelProvider.Factory {
        private val appContext = context.applicationContext

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CategoriesViewModel(
                repository = CategoryRepository(),
                cartRepository = CartRepository.getInstance(appContext)
            ) as T
        }
    }
}
