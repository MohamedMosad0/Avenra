package com.avenra.app.data.repository

import android.content.Context
import com.avenra.app.data.remote.api.ApiService
import com.avenra.app.data.remote.api.NetworkModule
import com.avenra.app.data.remote.dto.CheckoutQuoteItemRequestDto
import com.avenra.app.data.remote.dto.CheckoutQuoteRequestDto
import com.avenra.app.data.remote.dto.CreateOrderRequestDto
import com.avenra.app.data.remote.dto.ShippingAddressDto
import com.avenra.app.data.local.session.SessionStorage
import com.avenra.app.data.local.session.UserSessionStorage
import com.avenra.app.domain.model.CartItem
import com.avenra.app.domain.model.CheckoutQuote
import com.avenra.app.domain.model.OrderResult
import com.avenra.app.domain.model.QuoteItemSnapshot
import com.avenra.app.domain.model.ShippingAddress
import com.google.gson.Gson
import com.google.gson.JsonObject
import retrofit2.HttpException
import java.util.UUID

class CheckoutRepository(
    private val apiService: ApiService = NetworkModule.apiService,
    private val sessionStorage: SessionStorage
) {
    private val gson = Gson()

    suspend fun requestQuote(
        cartItems: List<CartItem>,
        address: ShippingAddress
    ): Result<CheckoutQuote> {
        val token = sessionStorage.getToken()
            ?: return Result.failure(Exception("No active session found."))
        return try {
            val itemDtos = cartItems.map {
                CheckoutQuoteItemRequestDto(
                    productId = it.productId,
                    quantity = it.quantity
                )
            }
            val addressDto = address.toDto()
            val requestDto = CheckoutQuoteRequestDto(
                items = itemDtos,
                shippingAddress = addressDto,
                deliveryMethod = "STANDARD"
            )

            val response = apiService.createCheckoutQuote("Bearer $token", requestDto)
            val domainQuote = CheckoutQuote(
                quoteId = response.quoteId,
                items = response.items.map {
                    QuoteItemSnapshot(
                        productId = it.productId,
                        title = it.title,
                        unitPrice = it.unitPrice,
                        quantity = it.quantity,
                        totalPrice = it.totalPrice
                    )
                },
                itemSubtotal = response.itemSubtotal,
                discountTotal = response.discountTotal,
                deliveryFee = response.deliveryFee,
                finalTotal = response.finalTotal,
                currency = response.currency,
                quoteExpiry = response.quoteExpiry,
                deliveryMethod = response.deliveryMethod
            )
            Result.success(domainQuote)
        } catch (e: HttpException) {
            val errorMsg = parseErrorMessage(e)
            Result.failure(Exception(errorMsg))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createOrder(
        quoteId: String,
        address: ShippingAddress,
        idempotencyKey: String = UUID.randomUUID().toString()
    ): Result<OrderResult> {
        val token = sessionStorage.getToken()
            ?: return Result.failure(Exception("No active session found."))
        return try {
            val addressDto = address.toDto()
            val requestDto = CreateOrderRequestDto(
                quoteId = quoteId,
                shippingAddress = addressDto,
                mockPaymentMethod = "CASH_ON_DELIVERY"
            )

            val response = apiService.createOrder(
                token = "Bearer $token",
                idempotencyKey = idempotencyKey,
                request = requestDto
            )

            val result = OrderResult(
                orderId = response.orderId,
                orderReference = response.orderReference,
                quoteId = response.quoteId,
                finalTotal = response.finalTotal,
                currency = response.currency,
                status = response.status,
                createdAt = response.createdAt
            )
            Result.success(result)
        } catch (e: HttpException) {
            val errorMsg = parseErrorMessage(e)
            Result.failure(Exception(errorMsg))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun ShippingAddress.toDto(): ShippingAddressDto {
        return ShippingAddressDto(
            fullName = this.fullName,
            phone = this.phone,
            city = this.city,
            addressLine = this.addressLine
        )
    }

    private fun parseErrorMessage(e: HttpException): String {
        return try {
            val errorBody = e.response()?.errorBody()?.string()
            if (!errorBody.isNullOrEmpty()) {
                val json = gson.fromJson(errorBody, JsonObject::class.java)
                val code = json.get("code")?.asString ?: ""
                val message = json.get("message")?.asString ?: "Checkout failed"
                if (code.isNotEmpty()) "$code: $message" else message
            } else {
                "HTTP ${e.code()}: ${e.message()}"
            }
        } catch (_: Exception) {
            "HTTP ${e.code()}: ${e.message()}"
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: CheckoutRepository? = null

        fun getInstance(context: Context): CheckoutRepository {
            return INSTANCE ?: synchronized(this) {
                val repository = CheckoutRepository(sessionStorage = UserSessionStorage.getInstance(context))
                INSTANCE = repository
                repository
            }
        }
    }
}
