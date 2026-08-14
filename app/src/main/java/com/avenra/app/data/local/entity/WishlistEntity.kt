package com.avenra.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "wishlist_items")
data class WishlistEntity(
    @PrimaryKey
    val id: String, // product ID
    val productId: String,
    val title: String,
    val imageUrl: String,
    val price: Double,
    val discountPrice: Double? = null,
    val createdAt: Long = System.currentTimeMillis()
)
