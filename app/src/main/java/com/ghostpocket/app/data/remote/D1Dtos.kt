package com.ghostpocket.app.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class AuthRequest(
    val email: String? = null,
    val password: String? = null
)

@Serializable
data class AuthResponse(
    val token: String? = null,
    val userId: String? = null,
    val email: String? = null,
    val name: String? = null,
    val error: String? = null
)

/**
 * Unified 2-Way Sync Request
 * Sends pending changes AND sinceTimestamp in a single HTTP POST request
 */
@Serializable
data class UnifiedSyncRequest(
    val sinceTimestamp: Long = 0L,
    val transactions: List<D1TransactionDto> = emptyList(),
    val friends: List<D1FriendDto> = emptyList(),
    val ledgers: List<D1LedgerDto> = emptyList(),
    val categories: List<D1CategoryDto> = emptyList(),
    val budgets: List<D1BudgetDto> = emptyList()
)

@Serializable
data class UnifiedSyncResponse(
    val success: Boolean = false,
    val processedCount: Int = 0,
    val serverTimestamp: Long = 0L,
    val remoteUpdates: D1SyncPullResponse? = null,
    val error: String? = null
)

@Serializable
data class D1SyncPullResponse(
    val transactions: List<D1TransactionDto> = emptyList(),
    val friends: List<D1FriendDto> = emptyList(),
    val ledgers: List<D1LedgerDto> = emptyList(),
    val categories: List<D1CategoryDto> = emptyList(),
    val budgets: List<D1BudgetDto> = emptyList(),
    val serverTimestamp: Long = 0L,
    val error: String? = null
)

@Serializable
data class D1TransactionDto(
    val id: String,
    val amountPaise: Long,
    val type: String,
    val category: String,
    val timestamp: Long,
    val note: String? = null,
    val paymentMethod: String? = null,
    val updatedAt: Long,
    val isDeleted: Boolean = false
)

@Serializable
data class D1FriendDto(
    val id: String,
    val name: String,
    val phone: String? = null,
    val updatedAt: Long,
    val isDeleted: Boolean = false
)

@Serializable
data class D1LedgerDto(
    val id: String,
    val friendId: String,
    val amountPaise: Long,
    val direction: String,
    val note: String? = null,
    val timestamp: Long,
    val isSettled: Boolean,
    val updatedAt: Long,
    val isDeleted: Boolean = false
)

@Serializable
data class D1CategoryDto(
    val name: String,
    val iconKey: String = "ghost",
    val budgetPaise: Long = 0L,
    val updatedAt: Long,
    val isDeleted: Boolean = false
)

@Serializable
data class D1BudgetDto(
    val id: String,
    val budgetPaise: Long,
    val updatedAt: Long
)
