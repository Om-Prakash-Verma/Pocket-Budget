package com.ghostpocket.app.sync

import android.content.Context
import androidx.work.*
import com.ghostpocket.app.GhostPocketApp
import com.ghostpocket.app.data.remote.*
import com.ghostpocket.app.model.SyncStatus
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit


/**
 * Dual Sync Worker for GhostPocket:
 * Supports Cloudflare D1 (primary high-speed edge SQLite) with fallback to Firestore.
 * Features:
 * - Batched writes to Cloudflare D1 backend.
 * - Coalesces repeated and canceled local edits.
 * - Two-way sync: pushes pending local records, then pulls updated remote records.
 * - Exponential backoff retry via WorkManager.
 */
class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? GhostPocketApp ?: return Result.failure()
        val repo = app.repository
        val d1Client = app.d1ApiClient

        val payload = repo.getPendingSyncItems()
        val hasPendingChanges = payload.transactions.isNotEmpty() ||
                payload.friends.isNotEmpty() ||
                payload.ledgers.isNotEmpty() ||
                payload.categories.isNotEmpty() ||
                payload.budgets.isNotEmpty()

        // -------------------------------------------------------------
        // OPTIMIZATION 1: ONLY-WHEN-DIRTY GATE
        // If there are 0 local edits and we synced recently, skip network completely!
        // Prevents wasted Worker invocations on simple app opens.
        // -------------------------------------------------------------
        if (!hasPendingChanges && (System.currentTimeMillis() - d1Client.lastSyncTimestamp < 300_000L)) {
            return Result.success()
        }

        // Determine user identity
        val userId = if (d1Client.currentUserId.isNotBlank() && d1Client.currentUserId != "guest_user") {
            d1Client.currentUserId
        } else {
            try {
                FirebaseAuth.getInstance().currentUser?.uid ?: "guest_user"
            } catch (e: Exception) {
                "guest_user"
            }
        }

        // -------------------------------------------------------------
        // OPTIMIZATION 2: UNIFIED 2-WAY SYNC (1 HTTP REQUEST)
        // Pushes all local changes and pulls latest cloud records together!
        // -------------------------------------------------------------
        try {
            val unifiedRequest = UnifiedSyncRequest(
                sinceTimestamp = d1Client.lastSyncTimestamp,
                transactions = payload.transactions.map { tx ->
                    D1TransactionDto(
                        id = tx.id,
                        amountPaise = tx.amountPaise,
                        type = tx.type.name,
                        category = tx.category,
                        timestamp = tx.timestamp,
                        note = tx.note,
                        paymentMethod = tx.paymentMethod,
                        updatedAt = tx.updatedAt,
                        isDeleted = (tx.syncStatus == SyncStatus.PENDING_DELETE)
                    )
                },
                friends = payload.friends.map { f ->
                    D1FriendDto(
                        id = f.id,
                        name = f.name,
                        phone = f.phone,
                        updatedAt = f.updatedAt,
                        isDeleted = (f.syncStatus == SyncStatus.PENDING_DELETE)
                    )
                },
                ledgers = payload.ledgers.map { l ->
                    D1LedgerDto(
                        id = l.id,
                        friendId = l.friendId,
                        amountPaise = l.amountPaise,
                        direction = l.direction.name,
                        note = l.note,
                        timestamp = l.timestamp,
                        isSettled = l.isSettled,
                        updatedAt = l.updatedAt,
                        isDeleted = (l.syncStatus == SyncStatus.PENDING_DELETE)
                    )
                },
                categories = payload.categories.map { c ->
                    D1CategoryDto(
                        name = c.name,
                        iconKey = c.iconKey,
                        budgetPaise = c.budgetPaise,
                        updatedAt = c.updatedAt,
                        isDeleted = (c.syncStatus == SyncStatus.PENDING_DELETE)
                    )
                },
                budgets = payload.budgets.map { b ->
                    D1BudgetDto(
                        id = b.id,
                        budgetPaise = b.budgetPaise,
                        updatedAt = b.updatedAt
                    )
                }
            )

            val unifiedRes = d1Client.unifiedSync(unifiedRequest)
            if (unifiedRes.success) {
                // Mark local items as SYNCED
                payload.friends.forEach { repo.markFriendSynced(it.id) }
                payload.ledgers.forEach { repo.markLedgerSynced(it.id) }
                payload.transactions.forEach { repo.markTransactionSynced(it.id) }
                payload.categories.forEach { repo.markCategorySynced(it.name) }
                payload.budgets.forEach { repo.markBudgetSynced(it.id) }

                // Apply piggybacked remote updates directly (no second request!)
                unifiedRes.remoteUpdates?.let { remoteUpdates ->
                    repo.applyD1PullResults(remoteUpdates, userId)
                }

                android.util.Log.d("GhostPocketSync", "Unified D1 sync complete (1 request) for user $userId")
                return Result.success()
            }
        } catch (e: Exception) {
            android.util.Log.w("GhostPocketSync", "D1 sync error: ${e.message}")
        }


        // 2. Fallback to Firebase Firestore if D1 backend is offline or unconfigured
        return try {
            val auth = FirebaseAuth.getInstance()
            val currentUser = auth.currentUser
            val fbUserId = currentUser?.uid

            if (fbUserId == null || fbUserId.isBlank()) {
                return Result.success()
            }

            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            val userDoc = db.collection("users").document(fbUserId)
            var currentBatch = db.batch()
            var operationCount = 0
            val maxBatchOps = 450

            val syncedTxIds = mutableListOf<String>()
            val syncedFriendIds = mutableListOf<String>()
            val syncedLedgerIds = mutableListOf<String>()
            val syncedCategoryNames = mutableListOf<String>()
            val syncedBudgetIds = mutableListOf<String>()

            suspend fun commitBatchIfNeeded() {
                if (operationCount >= maxBatchOps) {
                    currentBatch.commit().await()
                    currentBatch = db.batch()
                    operationCount = 0
                }
            }


            // Friends
            for (f in payload.friends) {
                val docRef = userDoc.collection("friends").document(f.id)
                if (f.syncStatus == SyncStatus.PENDING_DELETE) {
                    currentBatch.delete(docRef)
                } else {
                    val data = hashMapOf(
                        "id" to f.id,
                        "name" to f.name,
                        "phone" to (f.phone ?: ""),
                        "userId" to fbUserId,
                        "updatedAt" to f.updatedAt
                    )
                    currentBatch.set(docRef, data, com.google.firebase.firestore.SetOptions.merge())
                }
                syncedFriendIds.add(f.id)
                operationCount++
                commitBatchIfNeeded()
            }

            // Friend Ledgers
            for (ledger in payload.ledgers) {
                val docRef = userDoc.collection("friend_ledgers").document(ledger.id)
                if (ledger.syncStatus == SyncStatus.PENDING_DELETE) {
                    currentBatch.delete(docRef)
                } else {
                    val data = hashMapOf(
                        "id" to ledger.id,
                        "friendId" to ledger.friendId,
                        "amountPaise" to ledger.amountPaise,
                        "direction" to ledger.direction.name,
                        "note" to (ledger.note ?: ""),
                        "timestamp" to ledger.timestamp,
                        "isSettled" to ledger.isSettled,
                        "userId" to fbUserId,
                        "updatedAt" to ledger.updatedAt
                    )
                    currentBatch.set(docRef, data, com.google.firebase.firestore.SetOptions.merge())
                }
                syncedLedgerIds.add(ledger.id)
                operationCount++
                commitBatchIfNeeded()
            }

            // Transactions
            for (tx in payload.transactions) {
                val docRef = userDoc.collection("transactions").document(tx.id)
                if (tx.syncStatus == SyncStatus.PENDING_DELETE) {
                    currentBatch.delete(docRef)
                } else {
                    val data = hashMapOf(
                        "id" to tx.id,
                        "amountPaise" to tx.amountPaise,
                        "type" to tx.type.name,
                        "category" to tx.category,
                        "timestamp" to tx.timestamp,
                        "note" to (tx.note ?: ""),
                        "userId" to fbUserId,
                        "updatedAt" to tx.updatedAt
                    )
                    currentBatch.set(docRef, data, com.google.firebase.firestore.SetOptions.merge())
                }
                syncedTxIds.add(tx.id)
                operationCount++
                commitBatchIfNeeded()
            }

            // Categories
            for (cat in payload.categories) {
                val docRef = userDoc.collection("categories").document(cat.name)
                if (cat.syncStatus == SyncStatus.PENDING_DELETE) {
                    currentBatch.delete(docRef)
                } else {
                    val data = hashMapOf(
                        "name" to cat.name,
                        "iconKey" to cat.iconKey,
                        "budgetPaise" to cat.budgetPaise,
                        "userId" to fbUserId,
                        "updatedAt" to cat.updatedAt
                    )
                    currentBatch.set(docRef, data, com.google.firebase.firestore.SetOptions.merge())
                }
                syncedCategoryNames.add(cat.name)
                operationCount++
                commitBatchIfNeeded()
            }

            // User Budget Settings
            for (b in payload.budgets) {
                val docRef = userDoc.collection("settings").document("budget")
                val data = hashMapOf(
                    "budgetPaise" to b.budgetPaise,
                    "userId" to fbUserId,
                    "updatedAt" to b.updatedAt
                )
                currentBatch.set(docRef, data, com.google.firebase.firestore.SetOptions.merge())
                syncedBudgetIds.add(b.id)
                operationCount++
                commitBatchIfNeeded()
            }

            if (operationCount > 0) {
                currentBatch.commit().await()
            }


            syncedFriendIds.forEach { repo.markFriendSynced(it) }
            syncedLedgerIds.forEach { repo.markLedgerSynced(it) }
            syncedTxIds.forEach { repo.markTransactionSynced(it) }
            syncedCategoryNames.forEach { repo.markCategorySynced(it) }
            syncedBudgetIds.forEach { repo.markBudgetSynced(it) }

            repo.pullFromCloud(fbUserId)
            Result.success()
        } catch (e: Exception) {
            android.util.Log.e("GhostPocketSync", "Sync failed: ${e.message}", e)
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    companion object {
        private const val SYNC_WORK_NAME = "ghostpocket_sync_work"

        fun scheduleSync(context: Context, initialDelaySeconds: Long = 6L) {
            val constraints = Constraints.Builder()

                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .setInitialDelay(initialDelaySeconds, TimeUnit.SECONDS)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    15,
                    TimeUnit.SECONDS
                )
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                SYNC_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                syncRequest
            )
        }
    }
}
