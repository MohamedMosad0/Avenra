package com.avenra.app.presentation.products

import com.avenra.app.domain.model.Product
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductListViewModelTest {

    @Test
    fun product_priceFormatting_calculatesDiscountAndPricesCorrectly() {
        val discountedProduct = Product(
            id = "prod-1",
            title = "Cotton T-Shirt",
            description = "Soft premium cotton t-shirt",
            price = 500.0,
            discountPrice = 350.0,
            imageUrl = "http://localhost:3000/images/tshirt.jpg",
            galleryImages = emptyList(),
            rating = 4.5,
            reviewCount = 12,
            categoryId = "men",
            isAvailable = true,
            availableQuantity = 10,
            sizes = listOf("M", "L"),
            colors = listOf("Black", "White")
        )

        assertTrue(discountedProduct.hasDiscount)
        assertEquals("EGP 350.00", discountedProduct.formattedPrice)
        assertEquals("EGP 500.00", discountedProduct.formattedOriginalPrice)
    }

    @Test
    fun product_withoutDiscount_returnsNullOriginalPrice() {
        val regularProduct = Product(
            id = "prod-2",
            title = "Leather Jacket",
            description = "Classic leather jacket",
            price = 2500.0,
            discountPrice = null,
            imageUrl = "http://localhost:3000/images/jacket.jpg",
            galleryImages = emptyList(),
            rating = 4.8,
            reviewCount = 25,
            categoryId = "men",
            isAvailable = true,
            availableQuantity = 5,
            sizes = listOf("L", "XL"),
            colors = listOf("Black")
        )

        assertFalse(regularProduct.hasDiscount)
        assertEquals("EGP 2500.00", regularProduct.formattedPrice)
        assertNull(regularProduct.formattedOriginalPrice)
    }
}
