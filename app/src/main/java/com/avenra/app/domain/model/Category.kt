package com.avenra.app.domain.model

data class Subcategory(
    val id: String,
    val name: String,
    val categoryId: String
)

data class Category(
    val id: String,
    val name: String,
    val imageUrl: String,
    val subcategories: List<Subcategory> = emptyList()
)
