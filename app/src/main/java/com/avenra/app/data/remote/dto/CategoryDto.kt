package com.avenra.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class SubcategoryDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("categoryId") val categoryId: String
)

data class CategoryDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("imageUrl") val imageUrl: String,
    @SerializedName("subcategories") val subcategories: List<SubcategoryDto>? = null
)
