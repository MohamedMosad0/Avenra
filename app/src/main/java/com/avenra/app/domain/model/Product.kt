package com.avenra.app.domain.model

data class Product(
    val id: String,
    val title: String,
    val description: String,
    val price: Double,
    val discountPrice: Double? = null,
    val imageUrl: String,
    val galleryImages: List<String> = emptyList(),
    val rating: Double,
    val reviewCount: Int,
    val categoryId: String,
    val isAvailable: Boolean,
    val availableQuantity: Int,
    val sizes: List<String> = emptyList(),
    val colors: List<String> = emptyList()
) {
    val hasDiscount: Boolean
        get() = discountPrice != null && discountPrice < price

    val formattedPrice: String
        get() = "EGP %.2f".format(discountPrice ?: price)

    val formattedOriginalPrice: String?
        get() = if (hasDiscount) "EGP %.2f".format(price) else null
}
