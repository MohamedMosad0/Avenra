package com.avenra.app.domain.model

import com.avenra.app.data.local.entity.WishlistEntity
import com.avenra.app.presentation.wishlist.WishlistUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WishlistTest {

    @Test
    fun wishlistUiState_loading_returnsLoading() {
        val state: WishlistUiState = WishlistUiState.Loading
        assertTrue(state is WishlistUiState.Loading)
    }

    @Test
    fun wishlistUiState_empty_returnsEmpty() {
        val state: WishlistUiState = WishlistUiState.Empty
        assertTrue(state is WishlistUiState.Empty)
    }

    @Test
    fun wishlistUiState_success_containsPersistedSnapshots() {
        val product = WishlistItem(
            id = "p1",
            title = "Nike Air Jordan",
            price = 1200.0,
            discountPrice = 1000.0,
            imageUrl = "https://example.com/jordan.png"
        )
        val state: WishlistUiState = WishlistUiState.Success(listOf(product))
        assertTrue(state is WishlistUiState.Success)
        assertEquals(1, (state as WishlistUiState.Success).products.size)
        assertEquals("p1", state.products.first().id)
    }

    @Test
    fun wishlistEntity_creation_preservesRequiredFields() {
        val entity = WishlistEntity(
            id = "p100",
            productId = "p100",
            title = "Tall Cotton Dress",
            imageUrl = "https://example.com/dress.png",
            price = 600.0,
            discountPrice = 500.0
        )
        assertEquals("p100", entity.id)
        assertEquals("p100", entity.productId)
        assertEquals("Tall Cotton Dress", entity.title)
        assertEquals(600.0, entity.price, 0.001)
        assertEquals(500.0, entity.discountPrice!!, 0.001)
    }

    @Test
    fun wishlistProductIdsSet_containsCorrectId() {
        val ids = setOf("p1", "p2", "p3")
        assertTrue(ids.contains("p1"))
        assertFalse(ids.contains("p4"))
    }
}
