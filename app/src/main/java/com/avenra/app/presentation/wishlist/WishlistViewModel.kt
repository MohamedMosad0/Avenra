package com.avenra.app.presentation.wishlist

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.avenra.app.data.repository.CartRepository
import com.avenra.app.data.repository.WishlistRepository
import com.avenra.app.domain.model.WishlistItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class WishlistViewModel(
    private val wishlistRepository: WishlistRepository,
    private val cartRepository: CartRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<WishlistUiState>(WishlistUiState.Loading)
    val uiState: StateFlow<WishlistUiState> = _uiState.asStateFlow()

    val cartCount: Flow<Int> = cartRepository.cartCount

    init {
        observeWishlist()
    }

    private fun observeWishlist() {
        viewModelScope.launch {
            wishlistRepository.wishlistItems
                .catch { error ->
                    _uiState.value = WishlistUiState.Error(error.message ?: "Failed to load wishlist")
                }
                .collect { items ->
                    if (items.isEmpty()) {
                        _uiState.value = WishlistUiState.Empty
                    } else {
                        _uiState.value = WishlistUiState.Success(items)
                    }
                }
        }
    }

    fun removeFromWishlist(productId: String) {
        viewModelScope.launch {
            wishlistRepository.removeFromWishlist(productId)
        }
    }

    fun addToCart(product: WishlistItem) {
        viewModelScope.launch {
            cartRepository.addToCart(product = product, quantity = 1)
        }
    }

    class Factory(context: Context) : ViewModelProvider.Factory {
        private val appContext = context.applicationContext

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return WishlistViewModel(
                wishlistRepository = WishlistRepository.getInstance(appContext),
                cartRepository = CartRepository.getInstance(appContext)
            ) as T
        }
    }
}
