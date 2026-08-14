package com.avenra.app.data.repository

import android.content.Context
import com.avenra.app.data.local.dao.CartDao
import com.avenra.app.data.local.db.AppDatabase
import com.avenra.app.data.local.entity.CartEntity
import com.avenra.app.domain.model.CartItem
import com.avenra.app.domain.model.Product
import com.avenra.app.domain.model.WishlistItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CartRepository(
    private val cartDao: CartDao
) {
    val cartItems: Flow<List<CartItem>> = cartDao.observeCartItems().map { entities ->
        entities.map { it.toDomain() }
    }

    val cartCount: Flow<Int> = cartDao.observeTotalQuantity().map { count ->
        count ?: 0
    }

    suspend fun addToCart(
        product: Product,
        quantity: Int,
        selectedSize: String? = null,
        selectedColor: String? = null
    ) {
        addToCartSnapshot(
            productId = product.id,
            title = product.title,
            imageUrl = product.imageUrl,
            price = product.price,
            discountPrice = product.discountPrice,
            quantity = quantity,
            selectedSize = selectedSize,
            selectedColor = selectedColor
        )
    }

    suspend fun addToCart(product: WishlistItem, quantity: Int) {
        addToCartSnapshot(
            productId = product.id,
            title = product.title,
            imageUrl = product.imageUrl,
            price = product.price,
            discountPrice = product.discountPrice,
            quantity = quantity,
            selectedSize = null,
            selectedColor = null
        )
    }

    private suspend fun addToCartSnapshot(
        productId: String,
        title: String,
        imageUrl: String,
        price: Double,
        discountPrice: Double?,
        quantity: Int,
        selectedSize: String?,
        selectedColor: String?
    ) {
        val safeQuantity = quantity.coerceAtLeast(1)
        val size = selectedSize?.trim().orEmpty()
        val color = selectedColor?.trim().orEmpty()
        val variantId = listOf(productId, size, color)
            .filter { it.isNotEmpty() }
            .joinToString("_")

        val existing = cartDao.getCartItemById(variantId)
        if (existing != null) {
            val updatedQty = (existing.quantity + safeQuantity).coerceAtMost(99)
            cartDao.updateCartItem(existing.copy(quantity = updatedQty))
        } else {
            val newEntity = CartEntity(
                id = variantId,
                productId = productId,
                title = title,
                imageUrl = imageUrl,
                price = price,
                discountPrice = discountPrice,
                quantity = safeQuantity.coerceAtMost(99),
                selectedSize = size,
                selectedColor = color
            )
            cartDao.upsertCartItem(newEntity)
        }
    }

    suspend fun updateQuantity(cartItemId: String, newQuantity: Int) {
        if (newQuantity <= 0) {
            cartDao.deleteCartItemById(cartItemId)
        } else {
            val existing = cartDao.getCartItemById(cartItemId)
            if (existing != null) {
                cartDao.updateCartItem(existing.copy(quantity = newQuantity.coerceAtMost(99)))
            }
        }
    }

    suspend fun removeItem(cartItemId: String) {
        cartDao.deleteCartItemById(cartItemId)
    }

    suspend fun clearCart() {
        cartDao.clearCart()
    }

    companion object {
        @Volatile
        private var INSTANCE: CartRepository? = null

        fun getInstance(context: Context): CartRepository {
            return INSTANCE ?: synchronized(this) {
                val db = AppDatabase.getInstance(context)
                val repository = CartRepository(db.cartDao())
                INSTANCE = repository
                repository
            }
        }
    }
}

// Mapper extension
private fun CartEntity.toDomain(): CartItem {
    return CartItem(
        id = id,
        productId = productId,
        title = title,
        imageUrl = imageUrl,
        price = price,
        discountPrice = discountPrice,
        quantity = quantity,
        selectedSize = selectedSize,
        selectedColor = selectedColor
    )
}
