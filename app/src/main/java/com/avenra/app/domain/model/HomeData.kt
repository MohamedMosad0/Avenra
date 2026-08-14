package com.avenra.app.domain.model

data class HomeData(
    val banners: List<Banner>,
    val categories: List<Category>,
    val featuredProducts: List<Product>
)
