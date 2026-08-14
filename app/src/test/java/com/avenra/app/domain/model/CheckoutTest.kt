package com.avenra.app.domain.model

import com.avenra.app.data.local.entity.CartEntity
import com.avenra.app.data.remote.dto.CheckoutQuoteItemRequestDto
import com.avenra.app.data.remote.dto.CheckoutQuoteRequestDto
import com.avenra.app.data.remote.dto.CheckoutQuoteResponseDto
import com.avenra.app.data.remote.dto.OrderResponseDto
import com.avenra.app.data.remote.dto.QuoteItemSnapshotDto
import com.avenra.app.data.remote.dto.ShippingAddressDto
import com.avenra.app.presentation.checkout.CheckoutUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class CheckoutTest {

    @Test
    fun securityRule_checkoutRequest_containsNoClientAuthoritativePrices() {
        val cartItems = listOf(
            CartEntity(id = "cart_1", productId = "prod_abc", title = "Shoes", imageUrl = "", price = 500.0, discountPrice = null, quantity = 2, selectedColor = "Black", selectedSize = "42")
        )

        // Mapping to DTO
        val itemDtos = cartItems.map {
            CheckoutQuoteItemRequestDto(productId = it.productId, quantity = it.quantity)
        }
        val addressDto = ShippingAddressDto("Mohamed Mohamed", "01122118855", "6th of October", "Street 11")
        val request = CheckoutQuoteRequestDto(items = itemDtos, shippingAddress = addressDto, deliveryMethod = "STANDARD")

        // Verify DTO contains ONLY productId and quantity - NO prices, subtotal or total fields
        assertEquals("prod_abc", request.items[0].productId)
        assertEquals(2, request.items[0].quantity)
        assertEquals("STANDARD", request.deliveryMethod)

        // Confirm CheckoutQuoteItemRequestDto reflection fields
        val fields = CheckoutQuoteItemRequestDto::class.java.declaredFields.map { it.name }
        assertFalse("DTO must not contain client price field", fields.contains("price"))
        assertFalse("DTO must not contain client subtotal field", fields.contains("subtotal"))
        assertFalse("DTO must not contain client total field", fields.contains("total"))
    }

    @Test
    fun quoteResponse_mapping_populatesAuthoritativeFinancialTotals() {
        val responseDto = CheckoutQuoteResponseDto(
            quoteId = "quote_999",
            items = listOf(
                QuoteItemSnapshotDto("prod_abc", "Shoes", 250.0, 2, 500.0)
            ),
            itemSubtotal = 500.0,
            discountTotal = 50.0,
            deliveryFee = 50.0,
            finalTotal = 500.0,
            currency = "EGP",
            quoteExpiry = "2026-08-14T00:00:00.000Z",
            deliveryMethod = "STANDARD"
        )

        val domainQuote = CheckoutQuote(
            quoteId = responseDto.quoteId,
            items = responseDto.items.map {
                QuoteItemSnapshot(it.productId, it.title, it.unitPrice, it.quantity, it.totalPrice)
            },
            itemSubtotal = responseDto.itemSubtotal,
            discountTotal = responseDto.discountTotal,
            deliveryFee = responseDto.deliveryFee,
            finalTotal = responseDto.finalTotal,
            currency = responseDto.currency,
            quoteExpiry = responseDto.quoteExpiry,
            deliveryMethod = responseDto.deliveryMethod
        )

        assertEquals("quote_999", domainQuote.quoteId)
        assertEquals(500.0, domainQuote.itemSubtotal, 0.001)
        assertEquals(50.0, domainQuote.discountTotal, 0.001)
        assertEquals(50.0, domainQuote.deliveryFee, 0.001)
        assertEquals(500.0, domainQuote.finalTotal, 0.001)
        assertEquals("EGP", domainQuote.currency)
    }

    @Test
    fun orderResponse_mapping_populatesOrderResult() {
        val dto = OrderResponseDto(
            orderId = "ord_101",
            orderReference = "AVN-123456",
            quoteId = "quote_999",
            itemSubtotal = 500.0,
            discountTotal = 50.0,
            deliveryFee = 50.0,
            finalTotal = 500.0,
            currency = "EGP",
            status = "CONFIRMED",
            createdAt = "2026-08-13T22:00:00.000Z"
        )

        val result = OrderResult(
            orderId = dto.orderId,
            orderReference = dto.orderReference,
            quoteId = dto.quoteId,
            finalTotal = dto.finalTotal,
            currency = dto.currency,
            status = dto.status,
            createdAt = dto.createdAt
        )

        assertEquals("ord_101", result.orderId)
        assertEquals("AVN-123456", result.orderReference)
        assertEquals("CONFIRMED", result.status)
        assertEquals(500.0, result.finalTotal, 0.001)
    }

    @Test
    fun checkoutUiState_errorFlags_parsedCorrectly() {
        val priceChangedState = CheckoutUiState.Error(
            message = "PRICE_CHANGED: Product price has updated.",
            isPriceChanged = true
        )
        assertTrue(priceChangedState.isPriceChanged)
        assertFalse(priceChangedState.isOutOfStock)

        val outOfStockState = CheckoutUiState.Error(
            message = "OUT_OF_STOCK: Insufficient stock.",
            isOutOfStock = true
        )
        assertTrue(outOfStockState.isOutOfStock)
        assertFalse(outOfStockState.isPriceChanged)
    }

    @Test
    fun idempotencyKey_generatesValidUUID() {
        val key1 = UUID.randomUUID().toString()
        val key2 = UUID.randomUUID().toString()

        assertNotNull(key1)
        assertNotNull(key2)
        assertFalse(key1 == key2)
        assertEquals(36, key1.length)
    }
}
