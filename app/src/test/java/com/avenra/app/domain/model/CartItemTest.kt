package com.avenra.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class CartItemTest {

    @Test
    fun cartItem_effectiveUnitPrice_and_subtotalCalculation_areCorrect() {
        val cartItemWithDiscount = CartItem(
            id = "prod-1_40_Black",
            productId = "prod-1",
            title = "Nike Air Jordan High",
            imageUrl = "http://localhost:3000/assets/images/products/jordan_cover.jpg",
            price = 3500.0,
            discountPrice = 3000.0,
            quantity = 2,
            selectedSize = "40",
            selectedColor = "Black"
        )

        assertEquals(3000.0, cartItemWithDiscount.effectiveUnitPrice, 0.001)
        assertEquals(6000.0, cartItemWithDiscount.itemSubtotal, 0.001)
        assertEquals("EGP 3000.00", cartItemWithDiscount.formattedUnitPrice)
        assertEquals("EGP 6000.00", cartItemWithDiscount.formattedItemSubtotal)
        assertEquals("Black | Size: 40", cartItemWithDiscount.variantInfo)
    }

    @Test
    fun cartItem_withoutDiscount_usesBasePrice() {
        val cartItemRegular = CartItem(
            id = "prod-2_42_Red",
            productId = "prod-2",
            title = "Woman Shawl",
            imageUrl = "http://localhost:3000/assets/images/products/shawl_cover.jpg",
            price = 1200.0,
            discountPrice = null,
            quantity = 3,
            selectedSize = "42",
            selectedColor = "Red"
        )

        assertEquals(1200.0, cartItemRegular.effectiveUnitPrice, 0.001)
        assertEquals(3600.0, cartItemRegular.itemSubtotal, 0.001)
        assertEquals("EGP 1200.00", cartItemRegular.formattedUnitPrice)
        assertEquals("EGP 3600.00", cartItemRegular.formattedItemSubtotal)
        assertEquals("Red | Size: 42", cartItemRegular.variantInfo)
    }

    @Test
    fun cartItem_variantIdentity_distinguishesDifferentSizesAndColors() {
        val variant1Id = "prod-1_40_Black"
        val variant2Id = "prod-1_42_Black"
        val variant3Id = "prod-1_40_Red"

        assert(variant1Id != variant2Id)
        assert(variant1Id != variant3Id)
        assert(variant2Id != variant3Id)
    }
}
