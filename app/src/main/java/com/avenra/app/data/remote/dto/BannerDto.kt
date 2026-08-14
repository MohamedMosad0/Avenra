package com.avenra.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class BannerDto(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("subtitle") val subtitle: String,
    @SerializedName("imageUrl") val imageUrl: String,
    @SerializedName("targetCategoryId") val targetCategoryId: String? = null
)
