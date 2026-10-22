package com.ghostpocket.app.data.repository

import android.util.Log
import com.ghostpocket.app.data.local.*
import com.ghostpocket.app.data.remote.D1SyncPullResponse
import com.ghostpocket.app.model.*
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * CloudSyncEngine handles synchronization operations between local Room storage
 * and remote backends (Firebase Firestore and Cloudflare D1).
 *
 * Responsibilities:
 * - Aggregating pending local modifications for upload
 * - Marking entities as SYNCED or executing hard deletes for PENDING_DELETE records
 * - Pulling and merging delta changes from Firestore
 * - Reconciling changes pulled from Cloudflare D1
 */
class CloudSyncEngine(
    private val transactionDao: TransactionDao,
    private val friendDao: FriendDao,
    private val friendLedgerDao: FriendLedgerDao,
    private val categoryDao: CategoryDao,
    private val budgetDao: BudgetDao
) {

    /**
     * Gathers all local entities that have mutations pending sync with the cloud.
     */
    suspend fun getPendingSyncItems(): PendingSyncPayload {
        val txs = transactionDao.getPendingSyncTransactions()
        val friends = friendDao.getPendingSyncFriends()
        val ledgers = friendLedgerDao.getPendingSyncLedgers()
        val categories = categoryDao.getPendingSyncCategories()
        val budgets = budgetDao.getPendingSyncBudgets()
        return PendingSyncPayload(txs, friends, ledgers, categories, budgets)
    }

    /**
     * Marks a transaction as synced, or hard-deletes it if it was flagged for deletion.
     */
    suspend fun markTransactionSynced(id: String) {
        val item = transactionDao.getTransactionById(id)
        if (item?.syncStatus == SyncStatus.PENDING_DELETE) {
            transactionDao.hardDelete(id)
        } else {
            transactionDao.updateSyncStatus(id, SyncStatus.SYNCED)
        }
    }

    /**
     * Marks a friend entity as synced, or hard-deletes it if flagged for deletion.
     */
    suspend fun markFriendSynced(id: String) {
        val item = friendDao.getFriendById(id)
        if (item?.syncStatus == SyncStatus.PENDING_DELETE) {
            friendDao.hardDelete(id)
        } else {
            friendDao.updateSyncStatus(id, SyncStatus.SYNCED)
        }
    }

    /**
     * Marks a ledger entry as synced, or hard-deletes it if flagged for deletion.
     */
    suspend fun markLedgerSynced(id: String) {
        val item = friendLedgerDao.getPendingSyncLedgers().find { it.id == id }
        if (item?.syncStatus == SyncStatus.PENDING_DELETE) {
            friendLedgerDao.hardDelete(id)
        } else {
            friendLedgerDao.updateSyncStatus(id, SyncStatus.SYNCED)
        }
    }

    /**
     * Marks a category as synced, or hard-deletes it if flagged for deletion.
     */
    suspend fun markCategorySynced(name: String) {
        val item = categoryDao.getCategoryByName(name)
        if (item?.syncStatus == SyncStatus.PENDING_DELETE) {
            categoryDao.hardDelete(name)
        } else {
            categoryDao.updateSyncStatus(name, SyncStatus.SYNCED)
        }
    }

    /**
     * Marks a budget entity as synced.
     */
    suspend fun markBudgetSynced(id: String) {
        budgetDao.updateSyncStatus(id, SyncStatus.SYNCED)
    }

    /**
     * Pulls latest user collections from Cloud Firestore and resolves conflicts
     * via last-write-wins (only when local state is not PENDING_UPLOAD / PENDING_DELETE).
     */
    suspend fun pullFromCloud(userId: String) {
        if (userId.isBlank() || userId == "local_offline_user") return
        val db = FirebaseFirestore.getInstance()

        try {
            val userDoc = db.collection("users").document(userId)

            // 1. Transactions
            val txSnap = userDoc.collection("transactions").get().await()
            for (doc in txSnap.documents) {
                val data = doc.data ?: continue
                val local = transactionDao.getTransactionById(doc.id)
                val cloudUpdatedAt = (data["updatedAt"] as? Long) ?: 0L
                if (local == null || (local.syncStatus == SyncStatus.SYNCED && cloudUpdatedAt > local.updatedAt)) {
                    val entity = TransactionEntity(
                        id = doc.id,
                        amountPaise = (data["amountPaise"] as? Long) ?: 0L,
                        type = TransactionType.valueOf((data["type"] as? String) ?: TransactionType.EXPENSE.name),
                        category = (data["category"] as? String) ?: "Other",
                        timestamp = (data["timestamp"] as? Long) ?: System.currentTimeMillis(),
                        note = data["note"] as? String,
                        syncStatus = SyncStatus.SYNCED,
                        userId = userId,
                        updatedAt = cloudUpdatedAt
                    )
                    transactionDao.insertTransaction(entity)
                }
            }

            // 2. Friends
            val friendSnap = userDoc.collection("friends").get().await()
            for (doc in friendSnap.documents) {
                val data = doc.data ?: continue
                val local = friendDao.getFriendById(doc.id)
                val cloudUpdatedAt = (data["updatedAt"] as? Long) ?: 0L
                if (local == null || (local.syncStatus == SyncStatus.SYNCED && cloudUpdatedAt > local.updatedAt)) {
                    val entity = FriendEntity(
                        id = doc.id,
                        name = (data["name"] as? String) ?: "Friend",
                        phone = data["phone"] as? String,
                        syncStatus = SyncStatus.SYNCED,
                        userId = userId,
                        updatedAt = cloudUpdatedAt
                    )
                    friendDao.insertFriend(entity)
                }
            }

            // 3. Friend Ledgers
            val ledgerSnap = userDoc.collection("friend_ledgers").get().await()
            for (doc in ledgerSnap.documents) {
                val data = doc.data ?: continue
                val cloudUpdatedAt = (data["updatedAt"] as? Long) ?: 0L
                val entity = FriendLedgerEntity(
                    id = doc.id,
                    friendId = (data["friendId"] as? String) ?: "",
                    amountPaise = (data["amountPaise"] as? Long) ?: 0L,
                    direction = LedgerDirection.valueOf((data["direction"] as? String) ?: LedgerDirection.THEY_OWE_YOU.name),
                    note = data["note"] as? String,
                    timestamp = (data["timestamp"] as? Long) ?: System.currentTimeMillis(),
                    isSettled = (data["isSettled"] as? Boolean) ?: false,
                    syncStatus = SyncStatus.SYNCED,
                    userId = userId,
                    updatedAt = cloudUpdatedAt
                )
                friendLedgerDao.insertLedger(entity)
            }

            // 4. Categories
            val catSnap = userDoc.collection("categories").get().await()
            for (doc in catSnap.documents) {
                val data = doc.data ?: continue
                val entity = CategoryEntity(
                    name = doc.id,
                    iconKey = (data["iconKey"] as? String) ?: "ghost",
                    budgetPaise = (data["budgetPaise"] as? Long) ?: 0L,
                    syncStatus = SyncStatus.SYNCED,
                    userId = userId,
                    updatedAt = (data["updatedAt"] as? Long) ?: System.currentTimeMillis()
                )
                categoryDao.insertCategory(entity)
            }

            // 5. User Budget Settings
            val budgetDoc = userDoc.collection("settings").document("budget").get().await()
            if (budgetDoc.exists()) {
                val data = budgetDoc.data
                val budgetPaise = (data?.get("budgetPaise") as? Long) ?: 0L
                budgetDao.setBudget(
                    UserBudgetEntity(
                        id = "current_budget",
                        budgetPaise = budgetPaise,
                        syncStatus = SyncStatus.SYNCED,
                        userId = userId,
                        updatedAt = (data?.get("updatedAt") as? Long) ?: System.currentTimeMillis()
                    )
                )
            }
        } catch (e: Exception) {
            Log.e("GhostPocketSync", "Cloud pull failed: ${e.message}")
        }
    }

    /**
     * Applies changes pulled from Cloudflare D1 into local Room storage.
     */
    suspend fun applyD1PullResults(pullRes: D1SyncPullResponse, userId: String) {
        // 1. Transactions
        for (tx in pullRes.transactions) {
            val local = transactionDao.getTransactionById(tx.id)
            if (tx.isDeleted) {
                if (local != null && local.syncStatus != SyncStatus.PENDING_UPLOAD) {
                    transactionDao.hardDelete(tx.id)
                }
            } else if (local == null || (local.syncStatus == SyncStatus.SYNCED && tx.updatedAt > local.updatedAt)) {
                val entity = TransactionEntity(
                    id = tx.id,
                    amountPaise = tx.amountPaise,
                    type = try { TransactionType.valueOf(tx.type) } catch (e: Exception) { TransactionType.EXPENSE },
                    category = tx.category,
                    timestamp = tx.timestamp,
                    note = tx.note,
                    paymentMethod = tx.paymentMethod,
                    syncStatus = SyncStatus.SYNCED,
                    userId = userId,
                    updatedAt = tx.updatedAt
                )
                transactionDao.insertTransaction(entity)
            }
        }

        // 2. Friends
        for (f in pullRes.friends) {
            val local = friendDao.getFriendById(f.id)
            if (f.isDeleted) {
                if (local != null && local.syncStatus != SyncStatus.PENDING_UPLOAD) {
                    friendDao.hardDelete(f.id)
                }
            } else if (local == null || (local.syncStatus == SyncStatus.SYNCED && f.updatedAt > local.updatedAt)) {
                val entity = FriendEntity(
                    id = f.id,
                    name = f.name,
                    phone = f.phone,
                    syncStatus = SyncStatus.SYNCED,
                    userId = userId,
                    updatedAt = f.updatedAt
                )
                friendDao.insertFriend(entity)
            }
        }

        // 3. Friend Ledgers
        for (l in pullRes.ledgers) {
            val local = friendLedgerDao.getPendingSyncLedgers().find { it.id == l.id }
            if (l.isDeleted) {
                if (local != null && local.syncStatus != SyncStatus.PENDING_UPLOAD) {
                    friendLedgerDao.hardDelete(l.id)
                }
            } else {
                val entity = FriendLedgerEntity(
                    id = l.id,
                    friendId = l.friendId,
                    amountPaise = l.amountPaise,
                    direction = try { LedgerDirection.valueOf(l.direction) } catch (e: Exception) { LedgerDirection.THEY_OWE_YOU },
                    note = l.note,
                    timestamp = l.timestamp,
                    isSettled = l.isSettled,
                    syncStatus = SyncStatus.SYNCED,
                    userId = userId,
                    updatedAt = l.updatedAt
                )
                friendLedgerDao.insertLedger(entity)
            }
        }

        // 4. Categories
        for (cat in pullRes.categories) {
            val local = categoryDao.getCategoryByName(cat.name)
            if (cat.isDeleted) {
                if (local != null && local.syncStatus != SyncStatus.PENDING_UPLOAD) {
                    categoryDao.hardDelete(cat.name)
                }
            } else {
                val entity = CategoryEntity(
                    name = cat.name,
                    iconKey = cat.iconKey,
                    budgetPaise = cat.budgetPaise,
                    syncStatus = SyncStatus.SYNCED,
                    userId = userId,
                    updatedAt = cat.updatedAt
                )
                categoryDao.insertCategory(entity)
            }
        }

        // 5. User Budget Settings
        for (b in pullRes.budgets) {
            val budgetId = if (b.id.isNotBlank()) b.id else "current_budget"
            budgetDao.setBudget(
                UserBudgetEntity(
                    id = budgetId,
                    budgetPaise = b.budgetPaise,
                    syncStatus = SyncStatus.SYNCED,
                    userId = userId,
                    updatedAt = b.updatedAt
                )
            )
        }
    }
}
