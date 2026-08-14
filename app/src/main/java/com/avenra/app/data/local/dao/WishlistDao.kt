package com.avenra.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.avenra.app.data.local.entity.WishlistEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WishlistDao {

    @Query("SELECT * FROM wishlist_items ORDER BY createdAt DESC")
    fun observeWishlistItems(): Flow<List<WishlistEntity>>

    @Query("SELECT productId FROM wishlist_items")
    fun observeWishlistProductIds(): Flow<List<String>>

    @Query("SELECT EXISTS(SELECT 1 FROM wishlist_items WHERE productId = :productId)")
    fun observeIsWishlisted(productId: String): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM wishlist_items WHERE productId = :productId)")
    suspend fun isWishlisted(productId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWishlistItem(entity: WishlistEntity)

    @Query("DELETE FROM wishlist_items WHERE productId = :productId")
    suspend fun deleteWishlistItemById(productId: String)

    @Query("DELETE FROM wishlist_items")
    suspend fun clearWishlist()
}
