package com.avenra.app.data.repository

import android.content.Context
import com.avenra.app.data.local.dao.WishlistDao
import com.avenra.app.data.local.db.AppDatabase
import com.avenra.app.data.local.entity.WishlistEntity
import com.avenra.app.domain.model.Product
import com.avenra.app.domain.model.WishlistItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class WishlistRepository(
    private val wishlistDao: WishlistDao
) {

    val wishlistItems: Flow<List<WishlistItem>> = wishlistDao.observeWishlistItems().map { entities ->
        entities.map { it.toDomain() }
    }

    val wishlistProductIds: Flow<Set<String>> = wishlistDao.observeWishlistProductIds().map { ids ->
        ids.toSet()
    }

    fun isWishlisted(productId: String): Flow<Boolean> {
        return wishlistDao.observeIsWishlisted(productId)
    }

    suspend fun toggleWishlist(product: Product) {
        if (wishlistDao.isWishlisted(product.id)) {
            wishlistDao.deleteWishlistItemById(product.id)
        } else {
            val entity = WishlistEntity(
                id = product.id,
                productId = product.id,
                title = product.title,
                imageUrl = product.imageUrl,
                price = product.price,
                discountPrice = product.discountPrice
            )
            wishlistDao.insertWishlistItem(entity)
        }
    }

    suspend fun addToWishlist(product: Product) {
        val entity = WishlistEntity(
            id = product.id,
            productId = product.id,
            title = product.title,
            imageUrl = product.imageUrl,
            price = product.price,
            discountPrice = product.discountPrice
        )
        wishlistDao.insertWishlistItem(entity)
    }

    suspend fun removeFromWishlist(productId: String) {
        wishlistDao.deleteWishlistItemById(productId)
    }

    suspend fun clearWishlist() {
        wishlistDao.clearWishlist()
    }

    companion object {
        @Volatile
        private var INSTANCE: WishlistRepository? = null

        fun getInstance(context: Context): WishlistRepository {
            return INSTANCE ?: synchronized(this) {
                val db = AppDatabase.getInstance(context)
                val repository = WishlistRepository(db.wishlistDao())
                INSTANCE = repository
                repository
            }
        }
    }
}

private fun WishlistEntity.toDomain(): WishlistItem {
    return WishlistItem(
        id = productId,
        title = title,
        price = price,
        discountPrice = discountPrice,
        imageUrl = imageUrl
    )
}
