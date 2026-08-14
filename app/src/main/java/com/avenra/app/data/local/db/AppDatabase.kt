package com.avenra.app.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.avenra.app.data.local.dao.CartDao
import com.avenra.app.data.local.dao.WishlistDao
import com.avenra.app.data.local.entity.CartEntity
import com.avenra.app.data.local.entity.WishlistEntity

@Database(
    entities = [CartEntity::class, WishlistEntity::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cartDao(): CartDao
    abstract fun wishlistDao(): WishlistDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "avenra_database.db"
                )
                    .addMigrations(MIGRATION_2_3)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE wishlist_items_new (
                        id TEXT NOT NULL,
                        productId TEXT NOT NULL,
                        title TEXT NOT NULL,
                        imageUrl TEXT NOT NULL,
                        price REAL NOT NULL,
                        discountPrice REAL,
                        createdAt INTEGER NOT NULL,
                        PRIMARY KEY(id)
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    INSERT INTO wishlist_items_new (id, productId, title, imageUrl, price, discountPrice, createdAt)
                    SELECT id, productId, title, imageUrl, price, discountPrice, createdAt FROM wishlist_items
                    """.trimIndent()
                )
                database.execSQL("DROP TABLE wishlist_items")
                database.execSQL("ALTER TABLE wishlist_items_new RENAME TO wishlist_items")
            }
        }
    }
}
