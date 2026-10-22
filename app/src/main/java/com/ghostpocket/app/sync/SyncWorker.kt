package com.ghostpocket.app.sync

import android.content.Context
import androidx.work.*
import com.ghostpocket.app.GhostPocketApp
import com.ghostpocket.app.data.remote.*
import com.ghostpocket.app.model.SyncStatus
import java.util.concurrent.TimeUnit

/**
 * Cloud Sync Worker for GhostPocket:
 * Synchronizes with Cloudflare D1 high-speed edge SQLite database.
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
            "guest_user"
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
            } else {
                android.util.Log.w("GhostPocketSync", "D1 sync error: ${unifiedRes.error}")
                return if (runAttemptCount < 3) Result.retry() else Result.failure()
            }
        } catch (e: Exception) {
            android.util.Log.e("GhostPocketSync", "D1 sync failed: ${e.message}", e)
            return if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    companion object {
        private const val SYNC_WORK_NAME = "ghostpocket_sync_work"
        private const val DAILY_SYNC_WORK_NAME = "ghostpocket_daily_periodic_sync"

        /**
         * Manual or immediate sync (e.g. from Settings "Sync Now" button or login)
         */
        fun scheduleImmediateSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                SYNC_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                syncRequest
            )
        }

        /**
         * Legacy scheduleSync for backward compatibility
         */
        fun scheduleSync(context: Context, initialDelaySeconds: Long = 10L) {
            scheduleOnAppBackground(context)
        }

        /**
         * WhatsApp-style Daily Periodic Sync with Jitter.
         * Runs once per 24 hours in a 4-hour flex window with per-user jitter to prevent
         * the Thundering Herd Problem on Cloudflare D1.
         */
        fun scheduleDailyPeriodicSync(context: Context, userId: String) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()

            // Jitter: Spread requests evenly across 180 minutes (3 hours)
            val jitterMinutes = (Math.abs(userId.hashCode()) % 180).toLong()

            val periodicRequest = PeriodicWorkRequestBuilder<SyncWorker>(
                repeatInterval = 24, TimeUnit.HOURS,
                flexTimeInterval = 4, TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .setInitialDelay(jitterMinutes, TimeUnit.MINUTES)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                DAILY_SYNC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                periodicRequest
            )
        }

        /**
         * Fires when the user leaves or minimizes the app, bundling all recent edits
         * into a single lightweight background sync.
         */
        fun scheduleOnAppBackground(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .setInitialDelay(3, TimeUnit.SECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                SYNC_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                syncRequest
            )
        }
    }
}
