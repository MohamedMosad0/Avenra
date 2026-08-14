package com.avenra.app.presentation.products

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.avenra.app.data.repository.CartRepository
import com.avenra.app.data.repository.ProductRepository
import com.avenra.app.data.repository.WishlistRepository
import com.avenra.app.domain.model.NetworkResult
import com.avenra.app.domain.model.Product
import com.avenra.app.presentation.util.toDisplayMessage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class ProductListViewModel(
    private val repository: ProductRepository,
    private val cartRepository: CartRepository,
    private val wishlistRepository: WishlistRepository,
    private val categoryId: String? = null,
    initialQuery: String? = null
) : ViewModel() {

    private val _searchQuery = MutableStateFlow(initialQuery)
    val searchQuery: StateFlow<String?> = _searchQuery.asStateFlow()

    private val _uiState = MutableStateFlow<ProductListUiState>(ProductListUiState.Loading)
    val uiState: StateFlow<ProductListUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _searchQuery
                .debounce(350)
                .distinctUntilChanged()
                .collectLatest { query ->
                    fetchProducts(query)
                }
        }
    }

    val cartCount: Flow<Int> = cartRepository.cartCount

    val wishlistProductIds: Flow<Set<String>> = wishlistRepository.wishlistProductIds

    fun addToCart(product: Product) {
        viewModelScope.launch {
            cartRepository.addToCart(
                product = product,
                quantity = 1
            )
        }
    }

    fun toggleWishlist(product: Product) {
        viewModelScope.launch {
            wishlistRepository.toggleWishlist(product)
        }
    }

    fun onSearchQueryChanged(query: String?) {
        val trimmed = query?.trim()?.ifBlank { null }
        _searchQuery.value = trimmed
    }

    private suspend fun fetchProducts(query: String?) {
        _uiState.value = ProductListUiState.Loading
        when (val result = repository.getProducts(categoryId = categoryId, query = query)) {
            is NetworkResult.Success -> {
                if (result.data.isEmpty()) {
                    val emptyMessage = if (!query.isNullOrBlank()) {
                        "No products found matching \"$query\""
                    } else {
                        "No products available in this category."
                    }
                    _uiState.value = ProductListUiState.Empty(emptyMessage)
                } else {
                    _uiState.value = ProductListUiState.Success(result.data)
                }
            }
            is NetworkResult.Error -> {
                _uiState.value = ProductListUiState.Error(result.error.toDisplayMessage())
            }
        }
    }

    fun retry() {
        viewModelScope.launch {
            fetchProducts(_searchQuery.value)
        }
    }

    class Factory(
        context: Context,
        private val categoryId: String? = null,
        private val initialQuery: String? = null
    ) : ViewModelProvider.Factory {
        private val appContext = context.applicationContext

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ProductListViewModel(
                repository = ProductRepository(),
                cartRepository = CartRepository.getInstance(appContext),
                wishlistRepository = WishlistRepository.getInstance(appContext),
                categoryId = categoryId,
                initialQuery = initialQuery
            ) as T
        }
    }
}
