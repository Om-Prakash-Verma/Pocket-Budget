package com.ghostpocket.app.data.repository

import com.ghostpocket.app.data.local.*
import com.ghostpocket.app.data.remote.D1SyncPullResponse
import com.ghostpocket.app.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import java.util.Calendar
import java.util.UUID

/**
 * GhostPocketRepositoryImpl provides the concrete implementation of [GhostPocketRepository].
 * Coordinates Room DAOs with [CloudSyncEngine] and [BillSplitEngine].
 */
class GhostPocketRepositoryImpl(
    private val transactionDao: TransactionDao,
    private val friendDao: FriendDao,
    private val friendLedgerDao: FriendLedgerDao,
    private val categoryDao: CategoryDao,
    private val budgetDao: BudgetDao,
    private val currentUserIdProvider: () -> String
) : GhostPocketRepository {

    private val syncEngine = CloudSyncEngine(
        transactionDao = transactionDao,
        friendDao = friendDao,
        friendLedgerDao = friendLedgerDao,
        categoryDao = categoryDao,
        budgetDao = budgetDao
    )

    // ========================================================================
    // Transactions
    // ========================================================================

    override val allTransactions: Flow<List<TransactionEntity>> =
        transactionDao.getAllTransactions()

    override suspend fun getTransactionById(id: String): TransactionEntity? =
        transactionDao.getTransactionById(id)

    override suspend fun addTransaction(transaction: TransactionEntity) {
        val withUser = transaction.copy(
            userId = currentUserIdProvider(),
            syncStatus = SyncStatus.PENDING_UPLOAD,
            updatedAt = System.currentTimeMillis()
        )
        transactionDao.insertTransaction(withUser)
    }

    override suspend fun updateTransaction(transaction: TransactionEntity) {
        val withUser = transaction.copy(
            userId = currentUserIdProvider(),
            syncStatus = SyncStatus.PENDING_UPLOAD,
            updatedAt = System.currentTimeMillis()
        )
        transactionDao.updateTransaction(withUser)
    }

    override suspend fun deleteTransaction(id: String) {
        val item = transactionDao.getTransactionById(id) ?: return
        if (item.syncStatus == SyncStatus.PENDING_UPLOAD) {
            transactionDao.hardDelete(id)
        } else {
            transactionDao.markForDeletion(id)
        }
    }

    override val monthlySummary: Flow<MonthlySummary> =
        combine(
            transactionDao.getAllTransactions(),
            budgetDao.getBudget(),
            currentMonthTimeRangeFlow()
        ) { txs, budgetEntity, (start, end) ->
            val monthTxs = txs.filter { it.timestamp in start..end }
            val income = monthTxs.filter { it.type == TransactionType.INCOME }.sumOf { it.amountPaise }
            val expense = monthTxs.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amountPaise }
            MonthlySummary(
                totalIncomePaise = income,
                totalExpensePaise = expense,
                initialBudgetPaise = budgetEntity?.budgetPaise ?: 0L
            )
        }

    override val categorySpendsCurrentMonth: Flow<List<CategorySpend>> =
        combine(
            transactionDao.getAllTransactions(),
            categoryDao.getAllCategories(),
            currentMonthTimeRangeFlow()
        ) { txs, categories, (start, end) ->
            val monthExpenses = txs.filter { it.type == TransactionType.EXPENSE && it.timestamp in start..end }
            val totalExpense = monthExpenses.sumOf { it.amountPaise }
            val catBudgetMap = categories.associate { it.name to it.budgetPaise }

            if (categories.isEmpty() && totalExpense <= 0L) {
                emptyList()
            } else {
                val groupMap = monthExpenses.groupBy { it.category }
                val allCatNames = (categories.map { it.name } + groupMap.keys).distinct()

                allCatNames.map { cat ->
                    val catTotal = groupMap[cat]?.sumOf { it.amountPaise } ?: 0L
                    val allocated = catBudgetMap[cat] ?: 0L
                    val pct = if (totalExpense > 0L) catTotal.toFloat() / totalExpense.toFloat() else 0f
                    CategorySpend(
                        category = cat,
                        totalPaise = catTotal,
                        percentage = pct,
                        allocatedBudgetPaise = allocated
                    )
                }.sortedByDescending { it.totalPaise }
            }
        }

    // ========================================================================
    // Budgets
    // ========================================================================

    override val userBudget: Flow<UserBudgetEntity?> =
        budgetDao.getBudget()

    override val allUserBudgets: Flow<List<UserBudgetEntity>> =
        budgetDao.getAllBudgets()

    override suspend fun setUserBudget(amountPaise: Long, monthKey: String) {
        val now = System.currentTimeMillis()
        val userId = currentUserIdProvider()

        // 1. Set global current budget
        budgetDao.setBudget(
            UserBudgetEntity(
                id = "current_budget",
                budgetPaise = amountPaise,
                syncStatus = SyncStatus.PENDING_UPLOAD,
                userId = userId,
                updatedAt = now
            )
        )

        // 2. If monthKey provided, also persist month-scoped budget
        if (monthKey.isNotBlank()) {
            budgetDao.setBudget(
                UserBudgetEntity(
                    id = "month_$monthKey",
                    budgetPaise = amountPaise,
                    syncStatus = SyncStatus.PENDING_UPLOAD,
                    userId = userId,
                    updatedAt = now
                )
            )
        }
    }

    // ========================================================================
    // Categories
    // ========================================================================

    override val allCategories: Flow<List<CategoryEntity>> =
        categoryDao.getAllCategories()

    override suspend fun addCategory(name: String, budgetPaise: Long) {
        val trimmed = name.trim()
        if (trimmed.isNotBlank()) {
            val entity = CategoryEntity(
                name = trimmed,
                iconKey = "ghost",
                budgetPaise = budgetPaise,
                syncStatus = SyncStatus.PENDING_UPLOAD,
                userId = currentUserIdProvider(),
                updatedAt = System.currentTimeMillis()
            )
            categoryDao.insertCategory(entity)
        }
    }

    override suspend fun updateCategoryBudget(name: String, budgetPaise: Long, monthKey: String) {
        val trimmed = name.trim()
        if (trimmed.isNotBlank()) {
            val now = System.currentTimeMillis()
            val userId = currentUserIdProvider()
            val targetMonthKey = if (monthKey.isNotBlank()) monthKey else SelectedMonth.current().monthKey

            budgetDao.setBudget(
                UserBudgetEntity(
                    id = "cat_${trimmed}_${targetMonthKey}",
                    budgetPaise = budgetPaise,
                    syncStatus = SyncStatus.PENDING_UPLOAD,
                    userId = userId,
                    updatedAt = now
                )
            )

            val existing = categoryDao.getCategoryByName(trimmed)
            if (existing != null) {
                categoryDao.updateBudget(trimmed, budgetPaise)
            } else {
                categoryDao.insertCategory(
                    CategoryEntity(
                        name = trimmed,
                        iconKey = "ghost",
                        budgetPaise = budgetPaise,
                        syncStatus = SyncStatus.PENDING_UPLOAD,
                        userId = userId,
                        updatedAt = now
                    )
                )
            }
        }
    }

    override suspend fun renameCategory(oldName: String, newName: String, newBudgetPaise: Long, monthKey: String) {
        val trimmedOld = oldName.trim()
        val trimmedNew = newName.trim()
        if (trimmedNew.isBlank()) return

        if (trimmedOld.equals(trimmedNew, ignoreCase = false)) {
            updateCategoryBudget(trimmedNew, newBudgetPaise, monthKey)
            return
        }

        transactionDao.reassignCategory(trimmedOld, trimmedNew)

        val oldCat = categoryDao.getCategoryByName(trimmedOld)
        val iconKey = oldCat?.iconKey ?: "ghost"
        val now = System.currentTimeMillis()
        val userId = currentUserIdProvider()

        categoryDao.insertCategory(
            CategoryEntity(
                name = trimmedNew,
                iconKey = iconKey,
                budgetPaise = newBudgetPaise,
                syncStatus = SyncStatus.PENDING_UPLOAD,
                userId = userId,
                updatedAt = now
            )
        )

        if (monthKey.isNotBlank()) {
            budgetDao.setBudget(
                UserBudgetEntity(
                    id = "cat_${trimmedNew}_${monthKey}",
                    budgetPaise = newBudgetPaise,
                    syncStatus = SyncStatus.PENDING_UPLOAD,
                    userId = userId,
                    updatedAt = now
                )
            )
        }

        // Migrate all historical month-scoped category budgets from old name to new name
        val historicalBudgets = budgetDao.getCategoryBudgetsForCategory(trimmedOld)
        for (b in historicalBudgets) {
            val newId = b.id.replace("cat_${trimmedOld}_", "cat_${trimmedNew}_")
            budgetDao.setBudget(b.copy(id = newId, updatedAt = now, syncStatus = SyncStatus.PENDING_UPLOAD))
            budgetDao.deleteBudgetById(b.id)
        }

        if (oldCat != null) {
            deleteCategory(oldCat)
        }
    }

    override suspend fun deleteCategory(category: CategoryEntity) {
        if (category.syncStatus == SyncStatus.PENDING_UPLOAD) {
            categoryDao.hardDelete(category.name)
        } else {
            categoryDao.markForDeletion(category.name)
        }
    }

    override suspend fun deleteCategorySafe(category: CategoryEntity, fallbackCategory: String) {
        transactionDao.reassignCategory(category.name, fallbackCategory)
        if (categoryDao.getCategoryByName(fallbackCategory) == null) {
            addCategory(fallbackCategory, 0L)
        }
        deleteCategory(category)
    }

    // ========================================================================
    // Friends & Ledgers
    // ========================================================================

    override val allFriends: Flow<List<FriendEntity>> =
        friendDao.getAllFriends()

    override val allFriendsWithBalances: Flow<List<FriendBalance>> =
        friendDao.getAllFriends().combine(friendLedgerDao.getAllLedgerEntries()) { friends, ledgers ->
            friends.map { friend ->
                val friendLedgers = ledgers.filter { it.friendId == friend.id && !it.isSettled }
                val theyOweYou = friendLedgers
                    .filter { it.direction == LedgerDirection.THEY_OWE_YOU }
                    .sumOf { it.amountPaise }
                val youOweThem = friendLedgers
                    .filter { it.direction == LedgerDirection.YOU_OWE_THEM }
                    .sumOf { it.amountPaise }

                FriendBalance(
                    friend = friend,
                    theyOweYouPaise = theyOweYou,
                    youOweThemPaise = youOweThem
                )
            }
        }

    override fun getLedgerEntriesForFriend(friendId: String): Flow<List<FriendLedgerEntity>> =
        friendLedgerDao.getLedgerEntriesForFriend(friendId)

    override suspend fun addFriend(name: String, phone: String?): FriendEntity {
        val friend = FriendEntity(
            id = UUID.randomUUID().toString(),
            name = name.trim(),
            phone = phone?.trim()?.takeIf { it.isNotBlank() },
            syncStatus = SyncStatus.PENDING_UPLOAD,
            userId = currentUserIdProvider(),
            updatedAt = System.currentTimeMillis()
        )
        friendDao.insertFriend(friend)
        return friend
    }

    override suspend fun updateFriend(friend: FriendEntity) {
        val updated = friend.copy(
            syncStatus = SyncStatus.PENDING_UPLOAD,
            updatedAt = System.currentTimeMillis()
        )
        friendDao.updateFriend(updated)
    }

    override suspend fun deleteFriend(id: String) {
        val item = friendDao.getFriendById(id) ?: return
        if (item.syncStatus == SyncStatus.PENDING_UPLOAD) {
            friendDao.hardDelete(id)
            friendLedgerDao.hardDeleteAllForFriend(id)
        } else {
            friendDao.markForDeletion(id)
            friendLedgerDao.markAllForDeletionForFriend(id)
        }
    }

    override suspend fun deleteFriendSafe(id: String, settleRemaining: Boolean) {
        if (settleRemaining) {
            settleFriendLedger(id)
        }
        deleteFriend(id)
    }

    override suspend fun addLedgerEntry(
        friendId: String,
        amountPaise: Long,
        direction: LedgerDirection,
        note: String?
    ) {
        if (amountPaise <= 0) return
        val entry = FriendLedgerEntity(
            id = UUID.randomUUID().toString(),
            friendId = friendId,
            amountPaise = amountPaise,
            direction = direction,
            note = note,
            timestamp = System.currentTimeMillis(),
            isSettled = false,
            syncStatus = SyncStatus.PENDING_UPLOAD,
            userId = currentUserIdProvider(),
            updatedAt = System.currentTimeMillis()
        )
        friendLedgerDao.insertLedger(entry)
    }

    override suspend fun updateLedgerEntry(ledger: FriendLedgerEntity) {
        val updated = ledger.copy(
            syncStatus = SyncStatus.PENDING_UPLOAD,
            updatedAt = System.currentTimeMillis()
        )
        friendLedgerDao.updateLedger(updated)
    }

    override suspend fun deleteLedgerEntry(id: String) {
        val item = friendLedgerDao.getLedgerById(id) ?: return
        if (item.syncStatus == SyncStatus.PENDING_UPLOAD) {
            friendLedgerDao.hardDelete(id)
        } else {
            friendLedgerDao.markForDeletion(id)
        }
    }

    override suspend fun recordRepayment(
        friendId: String,
        repaymentAmountPaise: Long,
        isRepayingToUser: Boolean,
        note: String?
    ) {
        if (repaymentAmountPaise <= 0) return
        val direction = if (isRepayingToUser) {
            LedgerDirection.YOU_OWE_THEM
        } else {
            LedgerDirection.THEY_OWE_YOU
        }

        val friend = friendDao.getFriendById(friendId)
        val friendName = friend?.name ?: "Friend"

        val entry = FriendLedgerEntity(
            id = UUID.randomUUID().toString(),
            friendId = friendId,
            amountPaise = repaymentAmountPaise,
            direction = direction,
            note = note ?: if (isRepayingToUser) "Repaid you" else "You repaid",
            timestamp = System.currentTimeMillis(),
            isSettled = false,
            syncStatus = SyncStatus.PENDING_UPLOAD,
            userId = currentUserIdProvider(),
            updatedAt = System.currentTimeMillis()
        )
        friendLedgerDao.insertLedger(entry)
        // Option A: Friend Squad Ledger is an isolated Udhaar book.
        // Debt settlement does not artificially distort personal income or expense cashflow.
    }

    override suspend fun settleFriendLedger(friendId: String) {
        friendLedgerDao.settleAllForFriend(friendId)
    }

    // ========================================================================
    // Bill Splitting
    // ========================================================================

    override suspend fun splitBill(
        title: String,
        totalPaise: Long,
        payerFriendId: String?,
        participantFriendIds: List<String>,
        category: String,
        customShares: Map<String, Long>?
    ) {
        BillSplitEngine.executeSplit(
            title = title,
            totalPaise = totalPaise,
            payerFriendId = payerFriendId,
            participantFriendIds = participantFriendIds,
            category = category,
            customShares = customShares,
            currentUserId = currentUserIdProvider(),
            onAddTransaction = { addTransaction(it) },
            onAddLedgerEntry = { fId, amount, dir, note -> addLedgerEntry(fId, amount, dir, note) }
        )
    }

    // ========================================================================
    // Cloud Sync Operations (Delegated to CloudSyncEngine)
    // ========================================================================

    override suspend fun getPendingSyncItems(): PendingSyncPayload =
        syncEngine.getPendingSyncItems()

    override suspend fun markTransactionSynced(id: String) =
        syncEngine.markTransactionSynced(id)

    override suspend fun markFriendSynced(id: String) =
        syncEngine.markFriendSynced(id)

    override suspend fun markLedgerSynced(id: String) =
        syncEngine.markLedgerSynced(id)

    override suspend fun markCategorySynced(name: String) =
        syncEngine.markCategorySynced(name)

    override suspend fun markBudgetSynced(id: String) =
        syncEngine.markBudgetSynced(id)

    override suspend fun applyD1PullResults(pullRes: D1SyncPullResponse, userId: String) =
        syncEngine.applyD1PullResults(pullRes, userId)

    override suspend fun clearLocalCache() {
        transactionDao.clearAll()
        friendDao.clearAll()
        friendLedgerDao.clearAll()
        categoryDao.clearAll()
        budgetDao.clearAll()
    }

    private fun currentMonthTimeRangeFlow(): Flow<Pair<Long, Long>> = flow {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis

        cal.add(Calendar.MONTH, 1)
        cal.add(Calendar.MILLISECOND, -1)
        val end = cal.timeInMillis

        emit(Pair(start, end))
    }
}
