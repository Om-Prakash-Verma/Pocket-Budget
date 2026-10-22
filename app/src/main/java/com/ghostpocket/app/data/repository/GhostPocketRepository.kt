package com.ghostpocket.app.data.repository

import com.ghostpocket.app.data.remote.D1SyncPullResponse
import com.ghostpocket.app.model.*
import kotlinx.coroutines.flow.Flow

/**
 * GhostPocketRepository serves as the unified data access contract for Ghost Pocket.
 *
 * It manages:
 * - Local reactive flows for Transactions, Categories, and User Budgets
 * - Real-time friend debt balancing (who owes whom)
 * - Cloud syncing with Firebase Firestore and Cloudflare D1
 * - Complex ledger operations such as bill splitting and debt repayment reconciliation
 */
interface GhostPocketRepository {

    // ------------------------------------------------------------------------
    // Transactions
    // ------------------------------------------------------------------------

    /** Continuous Flow of all transactions, ordered chronologically. */
    val allTransactions: Flow<List<TransactionEntity>>

    /** Retrieves a single transaction by its unique ID, or null if not found. */
    suspend fun getTransactionById(id: String): TransactionEntity?

    /** Adds a new transaction and marks it for cloud sync. */
    suspend fun addTransaction(transaction: TransactionEntity)

    /** Updates an existing transaction and flags it for cloud sync. */
    suspend fun updateTransaction(transaction: TransactionEntity)

    /** Deletes a transaction, soft-deleting if synced or hard-deleting if pending upload. */
    suspend fun deleteTransaction(id: String)

    /** Emits a MonthlySummary containing current month's total income, expense, and budget. */
    val monthlySummary: Flow<MonthlySummary>

    /** Emits spend breakdowns by category for the current calendar month. */
    val categorySpendsCurrentMonth: Flow<List<CategorySpend>>

    // ------------------------------------------------------------------------
    // Budgets
    // ------------------------------------------------------------------------

    /** Emits the user's primary budget entity. */
    val userBudget: Flow<UserBudgetEntity?>

    /** Emits all user budget entities (overall and month-scoped). */
    val allUserBudgets: Flow<List<UserBudgetEntity>>

    /** Sets or updates the monthly budget amount in paise for a specified monthKey. */
    suspend fun setUserBudget(amountPaise: Long, monthKey: String = "")

    // ------------------------------------------------------------------------
    // Friends & Ledgers
    // ------------------------------------------------------------------------

    /** Emits list of friends combined with their calculated net debt balances. */
    val allFriendsWithBalances: Flow<List<FriendBalance>>

    /** Emits raw list of friends without computed debt aggregations. */
    val allFriends: Flow<List<FriendEntity>>

    /** Emits all ledger entries for a specific friend. */
    fun getLedgerEntriesForFriend(friendId: String): Flow<List<FriendLedgerEntity>>

    /** Summons a new friend mortal into the local ledger. */
    suspend fun addFriend(name: String, phone: String? = null): FriendEntity

    /** Updates friend profile details (e.g. name or contact number). */
    suspend fun updateFriend(friend: FriendEntity)

    /** Deletes a friend record immediately. */
    suspend fun deleteFriend(id: String)

    /** Deletes a friend safely, optionally settling all outstanding debts first. */
    suspend fun deleteFriendSafe(id: String, settleRemaining: Boolean = false)

    /** Adds a debt or credit ledger entry between user and a friend. */
    suspend fun addLedgerEntry(
        friendId: String,
        amountPaise: Long,
        direction: LedgerDirection,
        note: String? = null
    )

    /** Updates a ledger entry's amount, direction, or note. */
    suspend fun updateLedgerEntry(ledger: FriendLedgerEntity)

    /** Deletes an individual ledger entry. */
    suspend fun deleteLedgerEntry(id: String)

    /**
     * Records a repayment between user and friend.
     * Automatically adjusts the friend ledger and reconciles wallet cashflow (income/expense).
     */
    suspend fun recordRepayment(
        friendId: String,
        repaymentAmountPaise: Long,
        isRepayingToUser: Boolean,
        note: String? = "Repayment"
    )

    /** Marks all ledger entries for a given friend as settled. */
    suspend fun settleFriendLedger(friendId: String)

    // ------------------------------------------------------------------------
    // Bill Splitting
    // ------------------------------------------------------------------------

    /**
     * Splits an expense between the user and selected friends.
     * Supports equal distribution or custom per-member allocations.
     */
    suspend fun splitBill(
        title: String,
        totalPaise: Long,
        payerFriendId: String?,
        participantFriendIds: List<String>,
        category: String,
        customShares: Map<String, Long>? = null
    )

    // ------------------------------------------------------------------------
    // Categories
    // ------------------------------------------------------------------------

    /** Emits all available spending categories. */
    val allCategories: Flow<List<CategoryEntity>>

    /** Adds a new category with an optional default budget. */
    suspend fun addCategory(name: String, budgetPaise: Long = 0L)

    /** Updates category budget allocation for a given monthKey. */
    suspend fun updateCategoryBudget(name: String, budgetPaise: Long, monthKey: String = "")

    /** Renames a category and re-associates all existing transactions seamlessly. */
    suspend fun renameCategory(oldName: String, newName: String, newBudgetPaise: Long, monthKey: String = "")

    /** Deletes a category. */
    suspend fun deleteCategory(category: CategoryEntity)

    /** Deletes a category safely by reassigning linked transactions to a fallback category. */
    suspend fun deleteCategorySafe(category: CategoryEntity, fallbackCategory: String = "Other")

    // ------------------------------------------------------------------------
    // Sync & Remote Operations
    // ------------------------------------------------------------------------

    /** Gathers all local entities awaiting upload to the cloud. */
    suspend fun getPendingSyncItems(): PendingSyncPayload

    /** Marks a transaction as synced. */
    suspend fun markTransactionSynced(id: String)

    /** Marks a friend as synced. */
    suspend fun markFriendSynced(id: String)

    /** Marks a ledger item as synced. */
    suspend fun markLedgerSynced(id: String)

    /** Marks a category as synced. */
    suspend fun markCategorySynced(name: String)

    /** Marks a budget as synced. */
    suspend fun markBudgetSynced(id: String)

    /** Pulls delta updates from Cloud Firestore. */
    suspend fun pullFromCloud(userId: String)

    /** Merges delta updates from Cloudflare D1. */
    suspend fun applyD1PullResults(pullRes: D1SyncPullResponse, userId: String)

    /** Clears all local Room tables (used upon logout or clean reset). */
    suspend fun clearLocalCache()
}

/**
 * Encapsulates all entities pending synchronization to cloud backends.
 */
data class PendingSyncPayload(
    val transactions: List<TransactionEntity>,
    val friends: List<FriendEntity>,
    val ledgers: List<FriendLedgerEntity>,
    val categories: List<CategoryEntity>,
    val budgets: List<UserBudgetEntity>
)
