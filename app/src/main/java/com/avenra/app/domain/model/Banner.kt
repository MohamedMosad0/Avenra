package com.avenra.app.domain.model

data class Banner(
    val id: String,
    val title: String,
    val subtitle: String,
    val imageUrl: String,
    val targetCategoryId: String? = null
)
