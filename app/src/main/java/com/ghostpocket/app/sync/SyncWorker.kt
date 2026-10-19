package com.ghostpocket.app.sync

import android.content.Context
import androidx.work.*
import com.ghostpocket.app.GhostPocketApp
import com.ghostpocket.app.model.SyncStatus
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

/**
 * Robust, batched, idempotent synchronization worker for GhostPocket.
 * Features:
 * - Batched Firestore writes (respecting Firestore limits <= 500 ops per batch).
 * - Full entity coverage: transactions, friends, friend ledgers, categories, and budget settings.
 * - Dependent operation ordering preserved: Friends -> Ledgers -> Transactions.
 * - Idempotent SetOptions.merge() to eliminate duplicate writes on network retry.
 * - Exponential backoff retry on transient failures.
 * - Two-way sync: uploads pending local changes, then pulls any confirmed remote updates.
 */
class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? GhostPocketApp ?: return Result.failure()
        val repo = app.repository
        val payload = repo.getPendingSyncItems()

        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser
        val userId = currentUser?.uid ?: "local_offline_user"

        // If offline user or no internet yet, exit cleanly
        if (userId == "local_offline_user" || userId.isBlank()) {
            return Result.success()
        }

        val db = FirebaseFirestore.getInstance()
        val userDoc = db.collection("users").document(userId)

        val res = try {
            var currentBatch = db.batch()
            var operationCount = 0
            val maxBatchOps = 450 // Safe threshold well below Firestore's 500 limit

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

            // 1. Friends (Dependency first: friends must exist before ledgers refer to them)
            for (f in payload.friends) {
                val docRef = userDoc.collection("friends").document(f.id)
                if (f.syncStatus == SyncStatus.PENDING_DELETE) {
                    currentBatch.delete(docRef)
                } else {
                    val data = hashMapOf(
                        "id" to f.id,
                        "name" to f.name,
                        "phone" to (f.phone ?: ""),
                        "userId" to userId,
                        "updatedAt" to f.updatedAt
                    )
                    currentBatch.set(docRef, data, SetOptions.merge())
                }
                syncedFriendIds.add(f.id)
                operationCount++
                commitBatchIfNeeded()
            }

            // 2. Friend Ledgers (Dependency second: bills, debts and repayments)
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
                        "userId" to userId,
                        "updatedAt" to ledger.updatedAt
                    )
                    currentBatch.set(docRef, data, SetOptions.merge())
                }
                syncedLedgerIds.add(ledger.id)
                operationCount++
                commitBatchIfNeeded()
            }

            // 3. Transactions
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
                        "userId" to userId,
                        "updatedAt" to tx.updatedAt
                    )
                    currentBatch.set(docRef, data, SetOptions.merge())
                }
                syncedTxIds.add(tx.id)
                operationCount++
                commitBatchIfNeeded()
            }

            // 4. Categories
            for (cat in payload.categories) {
                val docRef = userDoc.collection("categories").document(cat.name)
                if (cat.syncStatus == SyncStatus.PENDING_DELETE) {
                    currentBatch.delete(docRef)
                } else {
                    val data = hashMapOf(
                        "name" to cat.name,
                        "iconKey" to cat.iconKey,
                        "budgetPaise" to cat.budgetPaise,
                        "userId" to userId,
                        "updatedAt" to cat.updatedAt
                    )
                    currentBatch.set(docRef, data, SetOptions.merge())
                }
                syncedCategoryNames.add(cat.name)
                operationCount++
                commitBatchIfNeeded()
            }

            // 5. User Budget Settings
            for (b in payload.budgets) {
                val docRef = userDoc.collection("settings").document("budget")
                val data = hashMapOf(
                    "budgetPaise" to b.budgetPaise,
                    "userId" to userId,
                    "updatedAt" to b.updatedAt
                )
                currentBatch.set(docRef, data, SetOptions.merge())
                syncedBudgetIds.add(b.id)
                operationCount++
                commitBatchIfNeeded()
            }

            // Commit final batch if there are pending operations
            if (operationCount > 0) {
                currentBatch.commit().await()
            }

            // Only mark as synchronized in local Room AFTER successful cloud confirmation
            syncedFriendIds.forEach { repo.markFriendSynced(it) }
            syncedLedgerIds.forEach { repo.markLedgerSynced(it) }
            syncedTxIds.forEach { repo.markTransactionSynced(it) }
            syncedCategoryNames.forEach { repo.markCategorySynced(it) }
            syncedBudgetIds.forEach { repo.markBudgetSynced(it) }

            // Pull latest remote updates for two-way synchronization
            repo.pullFromCloud(userId)

            android.util.Log.d(
                "GhostPocketSync",
                "Batched sync committed for user $userId (Txs: ${syncedTxIds.size}, Friends: ${syncedFriendIds.size}, Ledgers: ${syncedLedgerIds.size}, Cats: ${syncedCategoryNames.size})"
            )
            Result.success()
        } catch (e: Exception) {
            android.util.Log.e("GhostPocketSync", "Batched sync failed: ${e.message}", e)
            if (runAttemptCount < 4) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
        return res
    }

    companion object {
        private const val SYNC_WORK_NAME = "ghostpocket_sync_work"

        /**
         * Schedules background synchronization with exponential backoff and network constraints.
         * Can be invoked with a debounce delay (3-5 seconds idle period).
         */
        fun scheduleSync(context: Context, initialDelaySeconds: Long = 3L) {
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
