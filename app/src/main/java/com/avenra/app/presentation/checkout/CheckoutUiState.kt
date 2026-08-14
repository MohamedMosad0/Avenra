package com.avenra.app.presentation.checkout

import com.avenra.app.domain.model.CheckoutQuote
import com.avenra.app.domain.model.OrderResult
import com.avenra.app.domain.model.ShippingAddress

sealed interface CheckoutUiState {
    data class AddressForm(
        val shippingAddress: ShippingAddress = ShippingAddress("", "", "", ""),
        val validationError: String? = null
    ) : CheckoutUiState

    data object QuoteLoading : CheckoutUiState

    data class QuoteSuccess(
        val quote: CheckoutQuote,
        val shippingAddress: ShippingAddress,
        val paymentMethod: String = "CASH_ON_DELIVERY"
    ) : CheckoutUiState

    data object OrderSubmitting : CheckoutUiState

    data class OrderSuccess(
        val orderResult: OrderResult
    ) : CheckoutUiState

    data class Error(
        val message: String,
        val isPriceChanged: Boolean = false,
        val isOutOfStock: Boolean = false,
        val isExpired: Boolean = false,
        val shippingAddress: ShippingAddress? = null
    ) : CheckoutUiState
}
