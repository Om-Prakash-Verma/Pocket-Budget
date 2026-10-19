package com.ghostpocket.app.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val amountPaise: Long,
    val type: TransactionType,
    val category: String,
    val timestamp: Long = System.currentTimeMillis(),
    val note: String? = null,
    val paymentMethod: String? = null,
    val syncStatus: SyncStatus = SyncStatus.PENDING_UPLOAD,
    val userId: String = "local_user",
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "friends")
data class FriendEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val phone: String? = null,
    val syncStatus: SyncStatus = SyncStatus.PENDING_UPLOAD,
    val userId: String = "local_user",
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "friend_ledgers")
data class FriendLedgerEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val friendId: String,
    val amountPaise: Long,
    val direction: LedgerDirection,
    val note: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isSettled: Boolean = false,
    val syncStatus: SyncStatus = SyncStatus.PENDING_UPLOAD,
    val userId: String = "local_user",
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey
    val name: String,
    val iconKey: String = "ghost",
    val budgetPaise: Long = 0L, // 0 means unallocated
    val syncStatus: SyncStatus = SyncStatus.PENDING_UPLOAD,
    val userId: String = "local_user",
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_settings")
data class UserBudgetEntity(
    @PrimaryKey
    val id: String = "current_budget",
    val budgetPaise: Long = 0L,
    val syncStatus: SyncStatus = SyncStatus.PENDING_UPLOAD,
    val userId: String = "local_user",
    val updatedAt: Long = System.currentTimeMillis()
)
