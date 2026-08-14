package com.avenra.app.presentation.products.details

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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProductDetailsViewModel(
    private val productId: String,
    private val repository: ProductRepository,
    private val cartRepository: CartRepository,
    private val wishlistRepository: WishlistRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProductDetailsUiState>(ProductDetailsUiState.Loading)
    val uiState: StateFlow<ProductDetailsUiState> = _uiState.asStateFlow()

    init {
        loadProductDetails()
    }

    val cartCount: Flow<Int> = cartRepository.cartCount

    val wishlistProductIds: Flow<Set<String>> = wishlistRepository.wishlistProductIds

    fun addToCart(
        product: Product,
        quantity: Int,
        selectedSize: String?,
        selectedColor: String?
    ) {
        viewModelScope.launch {
            cartRepository.addToCart(
                product = product,
                quantity = quantity,
                selectedSize = selectedSize,
                selectedColor = selectedColor
            )
        }
    }

    fun toggleWishlist(product: Product) {
        viewModelScope.launch {
            wishlistRepository.toggleWishlist(product)
        }
    }

    fun loadProductDetails() {
        if (productId.isBlank()) {
            _uiState.value = ProductDetailsUiState.Error("Invalid product ID.")
            return
        }

        viewModelScope.launch {
            _uiState.value = ProductDetailsUiState.Loading
            when (val result = repository.getProductById(productId)) {
                is NetworkResult.Success -> {
                    _uiState.value = ProductDetailsUiState.Success(result.data)
                }
                is NetworkResult.Error -> {
                    _uiState.value = ProductDetailsUiState.Error(result.error.toDisplayMessage())
                }
            }
        }
    }

    fun retry() {
        loadProductDetails()
    }

    class Factory(
        context: Context,
        private val productId: String
    ) : ViewModelProvider.Factory {
        private val appContext = context.applicationContext

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ProductDetailsViewModel(
                productId = productId,
                repository = ProductRepository(),
                cartRepository = CartRepository.getInstance(appContext),
                wishlistRepository = WishlistRepository.getInstance(appContext)
            ) as T
        }
    }
}
