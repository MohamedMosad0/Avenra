package com.avenra.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class HomeResponseDto(
    @SerializedName("banners") val banners: List<BannerDto>,
    @SerializedName("categories") val categories: List<CategoryDto>,
    @SerializedName("featuredProducts") val featuredProducts: List<ProductDto>
)
