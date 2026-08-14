package com.avenra.app.domain.model

/** The product snapshot that is actually persisted for a wishlist entry. */
data class WishlistItem(
    val id: String,
    val title: String,
    val imageUrl: String,
    val price: Double,
    val discountPrice: Double? = null
) {
    val hasDiscount: Boolean
        get() = discountPrice != null && discountPrice < price

    val formattedPrice: String
        get() = "EGP %.2f".format(discountPrice ?: price)
}
