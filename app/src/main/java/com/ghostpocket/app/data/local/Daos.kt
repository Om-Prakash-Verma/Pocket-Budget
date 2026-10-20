package com.ghostpocket.app.data.local

import androidx.room.*
import com.ghostpocket.app.model.CategoryEntity
import com.ghostpocket.app.model.FriendEntity
import com.ghostpocket.app.model.FriendLedgerEntity
import com.ghostpocket.app.model.SyncStatus
import com.ghostpocket.app.model.TransactionEntity
import com.ghostpocket.app.model.UserBudgetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE syncStatus != 'PENDING_DELETE' ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionById(id: String): TransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Query("UPDATE transactions SET syncStatus = 'PENDING_DELETE', updatedAt = :now WHERE id = :id")
    suspend fun markForDeletion(id: String, now: Long = System.currentTimeMillis())

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun hardDelete(id: String)

    @Query("UPDATE transactions SET category = :newCategory WHERE category = :oldCategory")
    suspend fun reassignCategory(oldCategory: String, newCategory: String)

    @Query("SELECT * FROM transactions WHERE syncStatus IN ('PENDING_UPLOAD', 'PENDING_DELETE')")
    suspend fun getPendingSyncTransactions(): List<TransactionEntity>

    @Query("UPDATE transactions SET syncStatus = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: String, status: SyncStatus)

    @Query("DELETE FROM transactions")
    suspend fun clearAll()
}

@Dao
interface FriendDao {
    @Query("SELECT * FROM friends WHERE syncStatus != 'PENDING_DELETE' ORDER BY name ASC")
    fun getAllFriends(): Flow<List<FriendEntity>>

    @Query("SELECT * FROM friends WHERE id = :id LIMIT 1")
    suspend fun getFriendById(id: String): FriendEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFriend(friend: FriendEntity)

    @Update
    suspend fun updateFriend(friend: FriendEntity)

    @Query("UPDATE friends SET syncStatus = 'PENDING_DELETE', updatedAt = :now WHERE id = :id")
    suspend fun markForDeletion(id: String, now: Long = System.currentTimeMillis())

    @Query("DELETE FROM friends WHERE id = :id")
    suspend fun hardDelete(id: String)

    @Query("SELECT * FROM friends WHERE syncStatus IN ('PENDING_UPLOAD', 'PENDING_DELETE')")
    suspend fun getPendingSyncFriends(): List<FriendEntity>

    @Query("UPDATE friends SET syncStatus = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: String, status: SyncStatus)

    @Query("DELETE FROM friends")
    suspend fun clearAll()
}

@Dao
interface FriendLedgerDao {
    @Query("SELECT * FROM friend_ledgers WHERE syncStatus != 'PENDING_DELETE' ORDER BY timestamp DESC")
    fun getAllLedgerEntries(): Flow<List<FriendLedgerEntity>>

    @Query("SELECT * FROM friend_ledgers WHERE id = :id LIMIT 1")
    suspend fun getLedgerById(id: String): FriendLedgerEntity?

    @Query("SELECT * FROM friend_ledgers WHERE friendId = :friendId AND syncStatus != 'PENDING_DELETE' ORDER BY timestamp DESC")
    fun getLedgerEntriesForFriend(friendId: String): Flow<List<FriendLedgerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLedger(ledger: FriendLedgerEntity)

    @Update
    suspend fun updateLedger(ledger: FriendLedgerEntity)

    @Query("UPDATE friend_ledgers SET isSettled = 1, syncStatus = 'PENDING_UPLOAD', updatedAt = :now WHERE friendId = :friendId AND isSettled = 0")
    suspend fun settleAllForFriend(friendId: String, now: Long = System.currentTimeMillis())

    @Query("UPDATE friend_ledgers SET syncStatus = 'PENDING_DELETE', updatedAt = :now WHERE id = :id")
    suspend fun markForDeletion(id: String, now: Long = System.currentTimeMillis())

    @Query("DELETE FROM friend_ledgers WHERE id = :id")
    suspend fun hardDelete(id: String)

    @Query("SELECT * FROM friend_ledgers WHERE syncStatus IN ('PENDING_UPLOAD', 'PENDING_DELETE')")
    suspend fun getPendingSyncLedgers(): List<FriendLedgerEntity>

    @Query("UPDATE friend_ledgers SET syncStatus = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: String, status: SyncStatus)

    @Query("DELETE FROM friend_ledgers")
    suspend fun clearAll()
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories WHERE syncStatus != 'PENDING_DELETE' ORDER BY name ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE name = :name LIMIT 1")
    suspend fun getCategoryByName(name: String): CategoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity)

    @Query("UPDATE categories SET budgetPaise = :budgetPaise, syncStatus = 'PENDING_UPLOAD', updatedAt = :now WHERE name = :name")
    suspend fun updateBudget(name: String, budgetPaise: Long, now: Long = System.currentTimeMillis())

    @Query("UPDATE categories SET syncStatus = 'PENDING_DELETE', updatedAt = :now WHERE name = :name")
    suspend fun markForDeletion(name: String, now: Long = System.currentTimeMillis())

    @Query("DELETE FROM categories WHERE name = :name")
    suspend fun hardDelete(name: String)

    @Delete
    suspend fun deleteCategory(category: CategoryEntity)

    @Query("SELECT * FROM categories WHERE syncStatus IN ('PENDING_UPLOAD', 'PENDING_DELETE')")
    suspend fun getPendingSyncCategories(): List<CategoryEntity>

    @Query("UPDATE categories SET syncStatus = :status WHERE name = :name")
    suspend fun updateSyncStatus(name: String, status: SyncStatus)

    @Query("DELETE FROM categories")
    suspend fun clearAll()
}

@Dao
interface BudgetDao {
    @Query("SELECT * FROM user_settings WHERE id = 'current_budget' LIMIT 1")
    fun getBudget(): Flow<UserBudgetEntity?>

    @Query("SELECT * FROM user_settings WHERE id = 'current_budget' LIMIT 1")
    suspend fun getBudgetSnapshot(): UserBudgetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setBudget(budget: UserBudgetEntity)

    @Query("SELECT * FROM user_settings WHERE syncStatus = 'PENDING_UPLOAD'")
    suspend fun getPendingSyncBudgets(): List<UserBudgetEntity>

    @Query("UPDATE user_settings SET syncStatus = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: String, status: SyncStatus)

    @Query("DELETE FROM user_settings")
    suspend fun clearAll()
}
