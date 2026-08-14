package com.avenra.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.avenra.app.data.local.entity.CartEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CartDao {
    @Query("SELECT * FROM cart_items ORDER BY addedAt DESC")
    fun observeCartItems(): Flow<List<CartEntity>>

    @Query("SELECT * FROM cart_items WHERE id = :id LIMIT 1")
    suspend fun getCartItemById(id: String): CartEntity?

    @Query("SELECT SUM(quantity) FROM cart_items")
    fun observeTotalQuantity(): Flow<Int?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCartItem(item: CartEntity)

    @Update
    suspend fun updateCartItem(item: CartEntity)

    @Delete
    suspend fun deleteCartItem(item: CartEntity)

    @Query("DELETE FROM cart_items WHERE id = :id")
    suspend fun deleteCartItemById(id: String)

    @Query("DELETE FROM cart_items")
    suspend fun clearCart()
}
