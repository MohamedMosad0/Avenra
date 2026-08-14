package com.avenra.app.domain.model

data class ShippingAddress(
    val fullName: String,
    val phone: String,
    val city: String,
    val addressLine: String
)

data class QuoteItemSnapshot(
    val productId: String,
    val title: String,
    val unitPrice: Double,
    val quantity: Int,
    val totalPrice: Double
)

data class CheckoutQuote(
    val quoteId: String,
    val items: List<QuoteItemSnapshot>,
    val itemSubtotal: Double,
    val discountTotal: Double,
    val deliveryFee: Double,
    val finalTotal: Double,
    val currency: String,
    val quoteExpiry: String,
    val deliveryMethod: String
)

data class OrderResult(
    val orderId: String,
    val orderReference: String,
    val quoteId: String,
    val finalTotal: Double,
    val currency: String,
    val status: String,
    val createdAt: String
)
