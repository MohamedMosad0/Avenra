package com.avenra.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cart_items")
data class CartEntity(
    @PrimaryKey
    val id: String, // Variant-aware ID: "${productId}_${selectedSize}_${selectedColor}"
    val productId: String,
    val title: String,
    val imageUrl: String,
    val price: Double,
    val discountPrice: Double?,
    val quantity: Int,
    val selectedSize: String,
    val selectedColor: String,
    val addedAt: Long = System.currentTimeMillis()
)
