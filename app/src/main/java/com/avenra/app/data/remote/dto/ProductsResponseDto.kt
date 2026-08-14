package com.avenra.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ProductsResponseDto(
    @SerializedName("products") val products: List<ProductDto>
)
