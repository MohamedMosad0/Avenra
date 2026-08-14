package com.avenra.app.presentation.products.details

import com.avenra.app.domain.model.Product
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductDetailsViewModelTest {

    @Test
    fun productDetails_stateInitialization_hasCorrectFormatting() {
        val sampleProduct = Product(
            id = "prod-101",
            title = "Nike Air Jordon",
            description = "Nike is a multinational corporation that designs athletic footwear.",
            price = 3500.0,
            discountPrice = null,
            imageUrl = "http://localhost:3000/assets/images/products/jordan_cover.jpg",
            galleryImages = listOf("http://localhost:3000/assets/images/products/jordan_1.jpg"),
            rating = 4.8,
            reviewCount = 7500,
            categoryId = "shoes",
            isAvailable = true,
            availableQuantity = 50,
            sizes = listOf("38", "39", "40", "41", "42"),
            colors = listOf("Black", "Red", "Blue")
        )

        assertFalse(sampleProduct.hasDiscount)
        assertEquals("EGP 3500.00", sampleProduct.formattedPrice)
        assertNull(sampleProduct.formattedOriginalPrice)
        assertEquals(5, sampleProduct.sizes.size)
        assertEquals(3, sampleProduct.colors.size)
    }

    @Test
    fun productDetails_withDiscount_calculatesFormattedPrices() {
        val sampleProductWithDiscount = Product(
            id = "prod-102",
            title = "Woman Shawl",
            description = "Soft warm winter shawl.",
            price = 1200.0,
            discountPrice = 900.0,
            imageUrl = "http://localhost:3000/assets/images/products/shawl_cover.jpg",
            galleryImages = emptyList(),
            rating = 4.7,
            reviewCount = 320,
            categoryId = "women",
            isAvailable = true,
            availableQuantity = 15,
            sizes = listOf("S", "M", "L"),
            colors = listOf("Beige", "Navy")
        )

        assertTrue(sampleProductWithDiscount.hasDiscount)
        assertEquals("EGP 900.00", sampleProductWithDiscount.formattedPrice)
        assertEquals("EGP 1200.00", sampleProductWithDiscount.formattedOriginalPrice)
    }
}
