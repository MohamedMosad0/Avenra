package com.avenra.app.data.remote.api

import com.avenra.app.data.remote.dto.AuthResponseDto
import com.avenra.app.data.remote.dto.CategoriesResponseDto
import com.avenra.app.data.remote.dto.CheckoutQuoteRequestDto
import com.avenra.app.data.remote.dto.CheckoutQuoteResponseDto
import com.avenra.app.data.remote.dto.CreateOrderRequestDto
import com.avenra.app.data.remote.dto.HomeResponseDto
import com.avenra.app.data.remote.dto.OrderResponseDto
import com.avenra.app.data.remote.dto.ProductDetailResponseDto
import com.avenra.app.data.remote.dto.ProfileResponseDto
import com.avenra.app.data.remote.dto.ProductsResponseDto
import com.avenra.app.data.remote.dto.SignInRequestDto
import com.avenra.app.data.remote.dto.SignUpRequestDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {
    @POST("v1/auth/signup")
    suspend fun signUp(
        @Body request: SignUpRequestDto
    ): AuthResponseDto

    @POST("v1/auth/signin")
    suspend fun signIn(
        @Body request: SignInRequestDto
    ): AuthResponseDto

    @GET("v1/auth/me")
    suspend fun getProfile(
        @Header("Authorization") token: String
    ): ProfileResponseDto

    @POST("v1/auth/revoke")
    suspend fun revokeSession(
        @Header("Authorization") token: String
    )

    @GET("v1/home")
    suspend fun getHome(): HomeResponseDto

    @GET("v1/categories")
    suspend fun getCategories(): CategoriesResponseDto

    @GET("v1/products")
    suspend fun getProducts(
        @Query("categoryId") categoryId: String? = null,
        @Query("q") query: String? = null
    ): ProductsResponseDto

    @GET("v1/products/{productId}")
    suspend fun getProductById(
        @Path("productId") productId: String
    ): ProductDetailResponseDto

    @POST("v1/checkout/quotes")
    suspend fun createCheckoutQuote(
        @Header("Authorization") token: String,
        @Body request: CheckoutQuoteRequestDto
    ): CheckoutQuoteResponseDto

    @POST("v1/orders")
    suspend fun createOrder(
        @Header("Authorization") token: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body request: CreateOrderRequestDto
    ): OrderResponseDto
}
