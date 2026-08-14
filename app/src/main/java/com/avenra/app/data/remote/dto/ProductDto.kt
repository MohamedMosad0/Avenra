package com.avenra.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ProductDto(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String,
    @SerializedName("price") val price: Double,
    @SerializedName("discountPrice") val discountPrice: Double? = null,
    @SerializedName("imageUrl") val imageUrl: String,
    @SerializedName("galleryImages") val galleryImages: List<String>? = null,
    @SerializedName("rating") val rating: Double,
    @SerializedName("reviewCount") val reviewCount: Int,
    @SerializedName("categoryId") val categoryId: String,
    @SerializedName("isAvailable") val isAvailable: Boolean,
    @SerializedName("availableQuantity") val availableQuantity: Int,
    @SerializedName("sizes") val sizes: List<String>? = null,
    @SerializedName("colors") val colors: List<String>? = null
)
