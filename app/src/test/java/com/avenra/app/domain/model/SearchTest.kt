package com.avenra.app.domain.model

import com.avenra.app.presentation.products.ProductListUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchTest {

    @Test
    fun productListUiState_empty_formatsSearchQueryMessage() {
        val query = "Sneakers"
        val state = ProductListUiState.Empty("No products found matching \"$query\"")

        assertEquals("No products found matching \"Sneakers\"", state.message)
    }

    @Test
    fun productListUiState_success_holdsFilteredProducts() {
        val products = listOf(
            Product(
                id = "prod_1",
                title = "Nike Shoes",
                description = "Running shoes",
                price = 1200.0,
                discountPrice = 1000.0,
                imageUrl = "https://example.com/shoes.png",
                rating = 4.5,
                reviewCount = 100,
                categoryId = "cat_1",
                isAvailable = true,
                availableQuantity = 10
            )
        )

        val state = ProductListUiState.Success(products)
        assertEquals(1, state.products.size)
        assertEquals("Nike Shoes", state.products[0].title)
    }

    @Test
    fun product_matchesSearchQuery_caseInsensitive() {
        val product = Product(
            id = "prod_1",
            title = "Nike Air Jordan",
            description = "Iconic sneaker",
            price = 3500.0,
            discountPrice = null,
            imageUrl = "",
            rating = 4.8,
            reviewCount = 7500,
            categoryId = "cat_men",
            isAvailable = true,
            availableQuantity = 5
        )

        val query = "jordan"
        val matches = product.title.contains(query, ignoreCase = true)
        assertTrue(matches)
    }
}
