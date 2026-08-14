package com.avenra.app.domain.model

data class CartItem(
    val id: String, // Variant-aware ID
    val productId: String,
    val title: String,
    val imageUrl: String,
    val price: Double,
    val discountPrice: Double?,
    val quantity: Int,
    val selectedSize: String,
    val selectedColor: String
) {
    val effectiveUnitPrice: Double
        get() = discountPrice ?: price

    val itemSubtotal: Double
        get() = effectiveUnitPrice * quantity

    val formattedUnitPrice: String
        get() = "EGP %.2f".format(effectiveUnitPrice)

    val formattedItemSubtotal: String
        get() = "EGP %.2f".format(itemSubtotal)

    val variantInfo: String
        get() = listOfNotNull(
            selectedColor.takeIf { it.isNotBlank() },
            selectedSize.takeIf { it.isNotBlank() }?.let { "Size: $it" }
        ).joinToString(" | ")
}
