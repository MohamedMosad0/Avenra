package com.avenra.app.presentation.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.avenra.app.data.repository.CartRepository
import com.avenra.app.data.repository.HomeRepository
import com.avenra.app.data.repository.WishlistRepository
import com.avenra.app.domain.model.NetworkResult
import com.avenra.app.domain.model.Product
import com.avenra.app.presentation.util.toDisplayMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(
    private val repository: HomeRepository,
    private val cartRepository: CartRepository,
    private val wishlistRepository: WishlistRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadHomeData()
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

    fun loadHomeData() {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            when (val result = repository.getHomeData()) {
                is NetworkResult.Success -> {
                    _uiState.value = HomeUiState.Success(result.data)
                }
                is NetworkResult.Error -> {
                    _uiState.value = HomeUiState.Error(result.error.toDisplayMessage())
                }
            }
        }
    }

    fun retry() {
        loadHomeData()
    }

    class Factory(context: Context) : ViewModelProvider.Factory {
        private val appContext = context.applicationContext

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HomeViewModel(
                repository = HomeRepository(),
                cartRepository = CartRepository.getInstance(appContext),
                wishlistRepository = WishlistRepository.getInstance(appContext)
            ) as T
        }
    }
}
