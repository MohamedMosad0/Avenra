package com.avenra.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ProductDetailResponseDto(
    @SerializedName("product") val product: ProductDto
)
