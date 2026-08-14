package com.avenra.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ShippingAddressDto(
    @SerializedName("fullName") val fullName: String,
    @SerializedName("phone") val phone: String,
    @SerializedName("city") val city: String,
    @SerializedName("addressLine") val addressLine: String
)

data class CheckoutQuoteItemRequestDto(
    @SerializedName("productId") val productId: String,
    @SerializedName("quantity") val quantity: Int
)

data class CheckoutQuoteRequestDto(
    @SerializedName("items") val items: List<CheckoutQuoteItemRequestDto>,
    @SerializedName("shippingAddress") val shippingAddress: ShippingAddressDto,
    @SerializedName("deliveryMethod") val deliveryMethod: String = "STANDARD"
)

data class QuoteItemSnapshotDto(
    @SerializedName("productId") val productId: String,
    @SerializedName("title") val title: String,
    @SerializedName("unitPrice") val unitPrice: Double,
    @SerializedName("quantity") val quantity: Int,
    @SerializedName("totalPrice") val totalPrice: Double
)

data class CheckoutQuoteResponseDto(
    @SerializedName("quoteId") val quoteId: String,
    @SerializedName("items") val items: List<QuoteItemSnapshotDto>,
    @SerializedName("itemSubtotal") val itemSubtotal: Double,
    @SerializedName("discountTotal") val discountTotal: Double,
    @SerializedName("deliveryFee") val deliveryFee: Double,
    @SerializedName("finalTotal") val finalTotal: Double,
    @SerializedName("currency") val currency: String,
    @SerializedName("quoteExpiry") val quoteExpiry: String,
    @SerializedName("deliveryMethod") val deliveryMethod: String
)

data class CreateOrderRequestDto(
    @SerializedName("quoteId") val quoteId: String,
    @SerializedName("shippingAddress") val shippingAddress: ShippingAddressDto,
    @SerializedName("mockPaymentMethod") val mockPaymentMethod: String = "CASH_ON_DELIVERY"
)

data class OrderResponseDto(
    @SerializedName("orderId") val orderId: String,
    @SerializedName("orderReference") val orderReference: String,
    @SerializedName("quoteId") val quoteId: String,
    @SerializedName("itemSubtotal") val itemSubtotal: Double,
    @SerializedName("discountTotal") val discountTotal: Double,
    @SerializedName("deliveryFee") val deliveryFee: Double,
    @SerializedName("finalTotal") val finalTotal: Double,
    @SerializedName("currency") val currency: String,
    @SerializedName("status") val status: String,
    @SerializedName("createdAt") val createdAt: String
)
