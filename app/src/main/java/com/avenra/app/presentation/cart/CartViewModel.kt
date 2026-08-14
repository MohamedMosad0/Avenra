package com.avenra.app.presentation.cart

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.avenra.app.data.repository.CartRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CartViewModel(
    application: Application,
    private val repository: CartRepository = CartRepository.getInstance(application)
) : AndroidViewModel(application) {

    val uiState: StateFlow<CartUiState> = repository.cartItems
        .map { items ->
            if (items.isEmpty()) {
                CartUiState.Empty
            } else {
                val subtotal = items.sumOf { it.itemSubtotal }
                val formattedSubtotal = "EGP %.2f".format(subtotal)
                CartUiState.Success(
                    items = items,
                    subtotal = subtotal,
                    formattedSubtotal = formattedSubtotal
                )
            }
        }
        .catch { error ->
            emit(CartUiState.Error(error.message ?: "Failed to load cart items."))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = CartUiState.Loading
        )

    val cartCount: StateFlow<Int> = repository.cartCount
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    fun updateQuantity(cartItemId: String, newQuantity: Int) {
        viewModelScope.launch {
            repository.updateQuantity(cartItemId, newQuantity)
        }
    }

    fun removeItem(cartItemId: String) {
        viewModelScope.launch {
            repository.removeItem(cartItemId)
        }
    }

    fun clearCart() {
        viewModelScope.launch {
            repository.clearCart()
        }
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            return CartViewModel(application) as T
        }
    }
}
