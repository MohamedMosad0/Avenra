package com.avenra.app.presentation.checkout

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.avenra.app.data.local.session.UserSessionStorage
import com.avenra.app.data.repository.CartRepository
import com.avenra.app.data.repository.CheckoutRepository
import com.avenra.app.domain.model.CheckoutQuote
import com.avenra.app.domain.model.ShippingAddress
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID

class CheckoutViewModel(
    application: Application,
    private val checkoutRepository: CheckoutRepository = CheckoutRepository.getInstance(application),
    private val cartRepository: CartRepository = CartRepository.getInstance(application),
    private val sessionStorage: UserSessionStorage = UserSessionStorage.getInstance(application)
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<CheckoutUiState>(
        CheckoutUiState.AddressForm(shippingAddress = buildInitialAddress())
    )
    val uiState: StateFlow<CheckoutUiState> = _uiState.asStateFlow()

    private fun buildInitialAddress(): ShippingAddress {
        val profile = sessionStorage.getUserProfile()
        return ShippingAddress(
            fullName = profile?.fullName.orEmpty(),
            phone = profile?.mobileNumber.orEmpty(),
            city = "",
            addressLine = profile?.address.orEmpty()
        )
    }

    fun requestQuote(address: ShippingAddress) {
        if (address.fullName.isBlank() || address.phone.isBlank() || address.city.isBlank() || address.addressLine.isBlank()) {
            _uiState.value = CheckoutUiState.AddressForm(
                shippingAddress = address,
                validationError = "All shipping address fields are required."
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = CheckoutUiState.QuoteLoading
            val currentCartItems = cartRepository.cartItems.first()
            if (currentCartItems.isEmpty()) {
                _uiState.value = CheckoutUiState.Error(
                    message = "Your cart is empty.",
                    shippingAddress = address
                )
                return@launch
            }

            val result = checkoutRepository.requestQuote(currentCartItems, address)
            result.onSuccess { quote ->
                _uiState.value = CheckoutUiState.QuoteSuccess(
                    quote = quote,
                    shippingAddress = address
                )
            }.onFailure { error ->
                val msg = error.message ?: "Failed to generate quote"
                val isPriceChanged = msg.contains("PRICE_CHANGED", ignoreCase = true)
                val isOutOfStock = msg.contains("OUT_OF_STOCK", ignoreCase = true)
                val isExpired = msg.contains("QUOTE_EXPIRED", ignoreCase = true)
                _uiState.value = CheckoutUiState.Error(
                    message = msg,
                    isPriceChanged = isPriceChanged,
                    isOutOfStock = isOutOfStock,
                    isExpired = isExpired,
                    shippingAddress = address
                )
            }
        }
    }

    fun confirmOrder() {
        val currentState = _uiState.value
        if (currentState !is CheckoutUiState.QuoteSuccess) return

        viewModelScope.launch {
            val quote = currentState.quote
            val address = currentState.shippingAddress
            val idempotencyKey = UUID.randomUUID().toString()

            _uiState.value = CheckoutUiState.OrderSubmitting

            val result = checkoutRepository.createOrder(
                quoteId = quote.quoteId,
                address = address,
                idempotencyKey = idempotencyKey
            )

            result.onSuccess { orderResult ->
                // ONLY clear cart after successful backend order creation
                cartRepository.clearCart()
                _uiState.value = CheckoutUiState.OrderSuccess(orderResult)
            }.onFailure { error ->
                // DO NOT clear cart on failure
                val msg = error.message ?: "Failed to create order"
                val isPriceChanged = msg.contains("PRICE_CHANGED", ignoreCase = true)
                val isOutOfStock = msg.contains("OUT_OF_STOCK", ignoreCase = true)
                val isExpired = msg.contains("QUOTE_EXPIRED", ignoreCase = true)

                _uiState.value = CheckoutUiState.Error(
                    message = msg,
                    isPriceChanged = isPriceChanged,
                    isOutOfStock = isOutOfStock,
                    isExpired = isExpired,
                    shippingAddress = address
                )
            }
        }
    }

    fun backToAddress(address: ShippingAddress? = null) {
        val currentAddress = address ?: (uiState.value as? CheckoutUiState.QuoteSuccess)?.shippingAddress
            ?: (uiState.value as? CheckoutUiState.Error)?.shippingAddress
            ?: buildInitialAddress()
        _uiState.value = CheckoutUiState.AddressForm(shippingAddress = currentAddress)
    }

    companion object {
        fun provideFactory(application: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return CheckoutViewModel(application) as T
                }
            }
    }
}
