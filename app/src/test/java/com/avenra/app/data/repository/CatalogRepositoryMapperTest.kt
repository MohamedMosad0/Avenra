package com.avenra.app.data.repository

import com.avenra.app.data.remote.dto.CategoryDto
import com.avenra.app.data.remote.dto.ProductDto
import com.avenra.app.data.remote.dto.SubcategoryDto
import com.avenra.app.domain.model.DataError
import com.avenra.app.presentation.util.toDisplayMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogRepositoryMapperTest {

    @Test
    fun categoryDto_mapsTo_domainCategoryWithSubcategories() {
        val dto = CategoryDto(
            id = "cat-electronics",
            name = "Electronics",
            imageUrl = "https://avenra-api.bonto.run/assets/images/categories/electronics.png",
            subcategories = listOf(
                SubcategoryDto("sub-phones", "Smartphones", "cat-electronics"),
                SubcategoryDto("sub-laptops", "Laptops", "cat-electronics")
            )
        )

        val domain = dto.toDomain()

        assertEquals("cat-electronics", domain.id)
        assertEquals("Electronics", domain.name)
        assertEquals("https://avenra-api.bonto.run/assets/images/categories/electronics.png", domain.imageUrl)
        assertEquals(2, domain.subcategories.size)
        assertEquals("sub-phones", domain.subcategories[0].id)
        assertEquals("Smartphones", domain.subcategories[0].name)
    }

    @Test
    fun categoryDto_withNullSubcategories_mapsToEmptyList() {
        val dto = CategoryDto(
            id = "cat-books",
            name = "Books",
            imageUrl = "https://avenra-api.bonto.run/assets/images/categories/books.png",
            subcategories = null
        )

        val domain = dto.toDomain()

        assertEquals("cat-books", domain.id)
        assertTrue(domain.subcategories.isEmpty())
    }

    @Test
    fun productDto_mapsTo_domainProductWithFullAttributes() {
        val dto = ProductDto(
            id = "prod-99",
            title = "Avenra Wireless Headphones",
            description = "Noise-cancelling wireless headphones with 40h battery life.",
            price = 4500.0,
            discountPrice = 3800.0,
            imageUrl = "https://avenra-api.bonto.run/assets/images/products/headphones.png",
            galleryImages = listOf(
                "https://avenra-api.bonto.run/assets/images/products/headphones_1.png",
                "https://avenra-api.bonto.run/assets/images/products/headphones_2.png"
            ),
            rating = 4.9,
            reviewCount = 420,
            categoryId = "cat-electronics",
            isAvailable = true,
            availableQuantity = 25,
            sizes = listOf("Standard"),
            colors = listOf("Matte Black", "Silver")
        )

        val domain = dto.toDomain()

        assertEquals("prod-99", domain.id)
        assertEquals("Avenra Wireless Headphones", domain.title)
        assertEquals(4500.0, domain.price, 0.001)
        assertEquals(3800.0, domain.discountPrice ?: 0.0, 0.001)
        assertTrue(domain.hasDiscount)
        assertEquals("EGP 3800.00", domain.formattedPrice)
        assertEquals("EGP 4500.00", domain.formattedOriginalPrice)
        assertEquals(2, domain.galleryImages.size)
        assertEquals(2, domain.colors.size)
        assertEquals(1, domain.sizes.size)
    }

    @Test
    fun productDto_withNullOptionalFields_mapsToSafeDefaults() {
        val dto = ProductDto(
            id = "prod-minimal",
            title = "Minimal Product",
            description = "Basic item",
            price = 150.0,
            discountPrice = null,
            imageUrl = "https://avenra-api.bonto.run/assets/images/products/item.png",
            galleryImages = null,
            rating = 0.0,
            reviewCount = 0,
            categoryId = "cat-general",
            isAvailable = false,
            availableQuantity = 0,
            sizes = null,
            colors = null
        )

        val domain = dto.toDomain()

        assertEquals("prod-minimal", domain.id)
        assertFalse(domain.hasDiscount)
        assertNull(domain.formattedOriginalPrice)
        assertEquals("EGP 150.00", domain.formattedPrice)
        assertTrue(domain.galleryImages.isEmpty())
        assertTrue(domain.sizes.isEmpty())
        assertTrue(domain.colors.isEmpty())
    }

    @Test
    fun catalogDataErrors_formatCorrectlyForUI() {
        assertEquals("Product not found.", DataError.NotFound.toDisplayMessage())
        assertEquals("Network error. Please check your connection.", DataError.Network.toDisplayMessage())
        assertEquals("Server error (500). Please try again later.", DataError.Server(statusCode = 500).toDisplayMessage())
    }
}
