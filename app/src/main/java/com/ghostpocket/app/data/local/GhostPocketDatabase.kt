package com.ghostpocket.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.ghostpocket.app.model.CategoryEntity
import com.ghostpocket.app.model.FriendEntity
import com.ghostpocket.app.model.FriendLedgerEntity
import com.ghostpocket.app.model.TransactionEntity
import com.ghostpocket.app.model.UserBudgetEntity

@Database(
    entities = [
        TransactionEntity::class,
        FriendEntity::class,
        FriendLedgerEntity::class,
        CategoryEntity::class,
        UserBudgetEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class GhostPocketDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun friendDao(): FriendDao
    abstract fun friendLedgerDao(): FriendLedgerDao
    abstract fun categoryDao(): CategoryDao
    abstract fun budgetDao(): BudgetDao

    companion object {
        @Volatile
        private var INSTANCE: GhostPocketDatabase? = null

        fun getDatabase(context: Context): GhostPocketDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GhostPocketDatabase::class.java,
                    "ghostpocket.db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
