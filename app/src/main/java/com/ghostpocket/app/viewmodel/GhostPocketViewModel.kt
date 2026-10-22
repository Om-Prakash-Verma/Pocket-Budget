package com.ghostpocket.app.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ghostpocket.app.data.remote.D1ApiClient
import com.ghostpocket.app.data.remote.UnifiedSyncRequest
import com.ghostpocket.app.data.repository.GhostPocketRepository
import com.ghostpocket.app.model.*
import com.ghostpocket.app.update.AppUpdateInfo
import com.ghostpocket.app.update.DownloadState
import com.ghostpocket.app.update.UpdateCheckResult
import com.ghostpocket.app.update.UpdateManager
import com.ghostpocket.app.util.AppPreferences
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.util.Calendar

/**
 * GhostPocketViewModel coordinates UI interactions, state derivation,
 * authentication, and background synchronization across the Ghost Pocket app.
 */
class GhostPocketViewModel(
    private val repository: GhostPocketRepository,
    private val d1ApiClient: D1ApiClient? = null,
    private val updateManager: UpdateManager? = null,
    private val context: Context? = null,
    private val triggerSync: () -> Unit
) : ViewModel() {

    // ------------------------------------------------------------------------
    // Month Selection & History Navigation
    // ------------------------------------------------------------------------

    private val _selectedMonth = MutableStateFlow(SelectedMonth.current())
    val selectedMonth: StateFlow<SelectedMonth> = _selectedMonth.asStateFlow()

    private val _creationMonth = MutableStateFlow(
        context?.let { AppPreferences.getAccountCreationMonthKey(it) }?.let {
            SelectedMonth.fromMonthKey(it)
        } ?: SelectedMonth.current()
    )
    val creationMonth: StateFlow<SelectedMonth> = _creationMonth.asStateFlow()

    // ------------------------------------------------------------------------
    // In-App Update States
    // ------------------------------------------------------------------------

    val availableUpdate = MutableStateFlow<AppUpdateInfo?>(null)
    val isCheckingForUpdate = MutableStateFlow(false)
    val downloadState = updateManager?.downloadState ?: MutableStateFlow<DownloadState>(DownloadState.Idle)

    init {
        // Asynchronous throttled check for updates on app launch
        checkForUpdates(isManual = false)
    }

    fun checkForUpdates(isManual: Boolean = false) {
        if (updateManager == null) return
        viewModelScope.launch {
            if (isManual) isCheckingForUpdate.value = true
            when (val res = updateManager.checkForUpdates(isManual)) {
                is UpdateCheckResult.UpdateAvailable -> {
                    availableUpdate.value = res.info
                }
                is UpdateCheckResult.UpToDate -> {
                    if (isManual) {
                        val (vName, _) = updateManager.getCurrentVersionInfo()
                        _userMessage.value = "GhostPocket is already up to date (v$vName)! 👻✨"
                    }
                }
                is UpdateCheckResult.Error -> {
                    if (isManual) {
                        _userMessage.value = res.message
                    }
                }
            }
            if (isManual) isCheckingForUpdate.value = false
        }
    }

    fun startUpdateDownload(info: AppUpdateInfo) {
        if (updateManager == null) return
        viewModelScope.launch {
            updateManager.downloadApk(info)
        }
    }

    fun cancelUpdateDownload() {
        updateManager?.cancelDownload()
    }

    fun dismissUpdateDialog() {
        availableUpdate.value = null
        updateManager?.resetDownloadState()
    }

    fun installApk(file: File): Boolean {
        return updateManager?.installApk(file) ?: false
    }

    fun getCurrentVersionInfo(): Pair<String, Int> {
        return updateManager?.getCurrentVersionInfo() ?: Pair("2.0.0", 2)
    }

    fun selectPreviousMonth() {
        if (_selectedMonth.value > _creationMonth.value) {
            _selectedMonth.value = _selectedMonth.value.previous()
        }
    }

    fun selectNextMonth() {
        _selectedMonth.value = _selectedMonth.value.next()
    }

    fun selectCurrentMonth() {
        _selectedMonth.value = SelectedMonth.current()
    }

    fun selectMonth(month: SelectedMonth) {
        if (month >= _creationMonth.value) {
            _selectedMonth.value = month
        }
    }

    // ------------------------------------------------------------------------
    // UI Feedback & Auth State
    // ------------------------------------------------------------------------

    private val _userMessage = MutableStateFlow<String?>(null)
    private val _isSyncing = MutableStateFlow(false)
    private val _isInitialDataFetching = MutableStateFlow(false)
    private val _authLoading = MutableStateFlow(false)
    private val _authError = MutableStateFlow<String?>(null)
    private val _isAuthenticated = MutableStateFlow(
        d1ApiClient?.authToken != null && d1ApiClient.currentUserId != "guest_user"
    )

    // ------------------------------------------------------------------------
    // Reactive UI State Stream
    // ------------------------------------------------------------------------

    val uiState: StateFlow<GhostPocketUiState> = combine(
        repository.allTransactions,
        repository.allFriendsWithBalances,
        repository.allFriends,
        repository.allCategories
    ) { allTxs, friends, rawFriends, categories ->
        GhostPocketUiState(
            allTransactions = allTxs,
            friends = friends,
            rawFriends = rawFriends,
            categories = categories
        )
    }.combine(repository.allUserBudgets) { state, budgets ->
        state.copy(allBudgets = budgets)
    }.combine(_selectedMonth) { state, month ->
        val allTxs = state.allTransactions
        val categories = state.categories
        val allBudgets = state.allBudgets

        if (context != null) {
            val earliestTimestamp = allTxs.minOfOrNull { it.timestamp }
            if (earliestTimestamp != null) {
                val cal = Calendar.getInstance().apply { timeInMillis = earliestTimestamp }
                val txCreationMonth = SelectedMonth(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
                val currentSavedKey = AppPreferences.getAccountCreationMonthKey(context)
                val currentCreation = currentSavedKey?.let { SelectedMonth.fromMonthKey(it) } ?: SelectedMonth.current()
                if (txCreationMonth < currentCreation || currentSavedKey == null) {
                    AppPreferences.setAccountCreationMonthKey(context, txCreationMonth.monthKey)
                    _creationMonth.value = txCreationMonth
                }
            } else if (AppPreferences.getAccountCreationMonthKey(context) == null) {
                val current = SelectedMonth.current()
                AppPreferences.setAccountCreationMonthKey(context, current.monthKey)
                _creationMonth.value = current
            }
        }

        val breakdown = MonthlyFinancialCalculator.compute(
            allTxs = allTxs,
            categories = categories,
            allBudgets = allBudgets,
            month = month
        )

        state.copy(
            summary = breakdown.summary,
            recentTransactions = breakdown.monthTransactions,
            categorySpends = breakdown.categorySpends,
            userBudget = breakdown.monthBudgetEntity,
            selectedMonth = month
        )
    }.combine(_isSyncing) { state, syncing ->
        state.copy(isSyncing = syncing)
    }.combine(_userMessage) { state, msg ->
        state.copy(userMessage = msg)
    }.combine(_isAuthenticated) { state, auth ->
        state.copy(isAuthenticated = auth)
    }.combine(_authLoading) { state, loading ->
        state.copy(authLoading = loading)
    }.combine(_authError) { state, err ->
        state.copy(authError = err)
    }.combine(_isInitialDataFetching) { state, fetching ->
        state.copy(isInitialDataFetching = fetching)
    }.combine(_creationMonth) { state, creation ->
        state.copy(creationMonth = creation)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = GhostPocketUiState()
    )

    // ------------------------------------------------------------------------
    // Authentication Operations
    // ------------------------------------------------------------------------

    fun signIn(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            _authError.value = "Email and password cannot be empty."
            return
        }
        viewModelScope.launch {
            _authLoading.value = true
            _authError.value = null
            try {
                val res = d1ApiClient?.login(email.trim(), pass)
                if (res?.userId != null) {
                    _isAuthenticated.value = true
                    _isInitialDataFetching.value = true
                    _userMessage.value = "Welcome back! 👻"

                    viewModelScope.launch {
                        try {
                            if (d1ApiClient != null) {
                                val pullRes = d1ApiClient.unifiedSync(UnifiedSyncRequest(sinceTimestamp = 0L))
                                pullRes.remoteUpdates?.let { remote ->
                                    repository.applyD1PullResults(remote, d1ApiClient.currentUserId)
                                }
                            }
                        } catch (e: Exception) {
                            triggerSync()
                        } finally {
                            _isInitialDataFetching.value = false
                        }
                    }
                } else {
                    _authError.value = res?.error ?: "Crypt doors sealed! Invalid email or password."
                }
            } catch (e: Exception) {
                _authError.value = e.localizedMessage ?: "Crypt doors sealed! Check credentials."
            } finally {
                _authLoading.value = false
            }
        }
    }

    fun signUp(email: String, pass: String) {
        if (email.isBlank() || pass.length < 6) {
            _authError.value = "Password must be at least 6 characters."
            return
        }
        viewModelScope.launch {
            _authLoading.value = true
            _authError.value = null
            try {
                val res = d1ApiClient?.register(email.trim(), pass)
                if (res?.userId != null) {
                    _isAuthenticated.value = true
                    _userMessage.value = "Account conjured! 🪄"
                    triggerSync()
                } else {
                    _authError.value = res?.error ?: "Spell failed! Could not create account."
                }
            } catch (e: Exception) {
                _authError.value = e.localizedMessage ?: "Spell failed! Could not create account."
            } finally {
                _authLoading.value = false
            }
        }
    }

    fun continueAsGuest() {
        viewModelScope.launch {
            _authLoading.value = true
            try {
                d1ApiClient?.clearAuth()
                _isAuthenticated.value = true
                _userMessage.value = "Ghost Walk mode active! 👻"
            } finally {
                _authLoading.value = false
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            d1ApiClient?.clearAuth()
            repository.clearLocalCache()
            _isAuthenticated.value = false
            _userMessage.value = "Vanished into thin air... 👻"
        }
    }

    fun clearMessage() {
        _userMessage.value = null
    }

    // ------------------------------------------------------------------------
    // Budget & Transaction CRUD
    // ------------------------------------------------------------------------

    fun setUserBudget(budgetPaise: Long, monthKey: String = "") {
        val targetMonthKey = if (monthKey.isNotBlank()) monthKey else _selectedMonth.value.monthKey
        viewModelScope.launch {
            repository.setUserBudget(budgetPaise, targetMonthKey)
            _userMessage.value = "Budget locked in! 💰👻"
        }
    }

    fun addTransaction(
        amountPaise: Long,
        type: TransactionType,
        category: String,
        note: String?,
        timestamp: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch {
            val tx = TransactionEntity(
                amountPaise = amountPaise,
                type = type,
                category = category,
                note = note,
                timestamp = timestamp
            )
            repository.addTransaction(tx)
            _userMessage.value = if (type == TransactionType.EXPENSE) "Money vanished! 💸👻" else "Pocket fed! 🪙✨"
        }
    }

    fun editTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.updateTransaction(transaction)
            _userMessage.value = "Spell altered in the crypt! 🪄"
        }
    }

    fun deleteTransaction(id: String) {
        viewModelScope.launch {
            repository.deleteTransaction(id)
            _userMessage.value = "Banished into the void! 🪄"
        }
    }

    // ------------------------------------------------------------------------
    // Squad & Ledger Operations
    // ------------------------------------------------------------------------

    fun addFriend(name: String, phone: String?) {
        viewModelScope.launch {
            repository.addFriend(name, phone)
            _userMessage.value = "New mortal summoned! 👥👻"
        }
    }

    fun updateFriend(friend: FriendEntity) {
        viewModelScope.launch {
            repository.updateFriend(friend)
            _userMessage.value = "Mortal's scroll updated! 👥✨"
        }
    }

    fun deleteFriend(id: String) {
        viewModelScope.launch {
            repository.deleteFriend(id)
            _userMessage.value = "Mortal departed! 💨"
        }
    }

    fun deleteFriendSafe(id: String, settleRemaining: Boolean) {
        viewModelScope.launch {
            repository.deleteFriendSafe(id, settleRemaining)
            _userMessage.value = if (settleRemaining) "All debts settled & mortal removed! 💨" else "Mortal departed! 💨"
        }
    }

    fun addLedgerEntry(friendId: String, amountPaise: Long, direction: LedgerDirection, note: String?) {
        viewModelScope.launch {
            repository.addLedgerEntry(friendId, amountPaise, direction, note)
            _userMessage.value = if (direction == LedgerDirection.THEY_OWE_YOU) "Recorded: You Gave 🔴" else "Recorded: You Got 🟢"
        }
    }

    fun updateLedgerEntry(ledger: FriendLedgerEntity) {
        viewModelScope.launch {
            repository.updateLedgerEntry(ledger)
            _userMessage.value = "Debt ledger updated! 📜✨"
        }
    }

    fun deleteLedgerEntry(id: String) {
        viewModelScope.launch {
            repository.deleteLedgerEntry(id)
            _userMessage.value = "Ledger entry banished! 💨"
        }
    }

    fun recordRepayment(friendId: String, amountPaise: Long, isRepayingToUser: Boolean) {
        viewModelScope.launch {
            repository.recordRepayment(friendId, amountPaise, isRepayingToUser)
            _userMessage.value = "Debt exorcised! Finally! ✓"
        }
    }

    fun settleFriend(friendId: String) {
        viewModelScope.launch {
            repository.settleFriendLedger(friendId)
            _userMessage.value = "All debts vanished! Clean slate! 👻✨"
        }
    }

    fun splitBill(
        title: String,
        totalPaise: Long,
        payerFriendId: String?,
        participantFriendIds: List<String>,
        category: String,
        customShares: Map<String, Long>? = null
    ) {
        viewModelScope.launch {
            repository.splitBill(title, totalPaise, payerFriendId, participantFriendIds, category, customShares)
            _userMessage.value = "Bill sliced into pieces! ⚔️👻"
        }
    }

    // ------------------------------------------------------------------------
    // Category Management
    // ------------------------------------------------------------------------

    fun addCategory(name: String, budgetPaise: Long = 0L) {
        viewModelScope.launch {
            repository.addCategory(name, budgetPaise)
            val budgetText = if (budgetPaise > 0L) " with ₹${budgetPaise / 100} budget" else ""
            _userMessage.value = "New realm unlocked: $name$budgetText 🏷️"
        }
    }

    fun updateCategoryBudget(name: String, budgetPaise: Long) {
        viewModelScope.launch {
            repository.updateCategoryBudget(name, budgetPaise)
            _userMessage.value = "$name budget updated to ₹${budgetPaise / 100}! 💰"
        }
    }

    fun editCategory(oldName: String, newName: String, newBudgetPaise: Long, monthKey: String = "") {
        val targetMonthKey = if (monthKey.isNotBlank()) monthKey else _selectedMonth.value.monthKey
        viewModelScope.launch {
            repository.renameCategory(oldName, newName, newBudgetPaise, targetMonthKey)
            _userMessage.value = "Realm transformed to $newName! 🏷️✨"
        }
    }

    fun deleteCategory(category: CategoryEntity) {
        viewModelScope.launch {
            repository.deleteCategory(category)
            _userMessage.value = "${category.name} disintegrated! 💨"
        }
    }

    fun deleteCategorySafe(category: CategoryEntity, fallbackCategory: String = "Other") {
        viewModelScope.launch {
            repository.deleteCategorySafe(category, fallbackCategory)
            _userMessage.value = "${category.name} disintegrated! Records preserved in $fallbackCategory 💨"
        }
    }

    fun getLedgerEntriesForFriend(friendId: String): Flow<List<FriendLedgerEntity>> {
        return repository.getLedgerEntriesForFriend(friendId)
    }

    fun manualSync() {
        viewModelScope.launch {
            _isSyncing.value = true
            triggerSync()
            delay(1200)
            _isSyncing.value = false
            _userMessage.value = "Cloud spirits synced! ☁️👻"
        }
    }
}

/**
 * Factory for creating [GhostPocketViewModel] with constructor dependencies.
 */
class GhostPocketViewModelFactory(
    private val repository: GhostPocketRepository,
    private val d1ApiClient: D1ApiClient? = null,
    private val updateManager: UpdateManager? = null,
    private val context: Context? = null,
    private val triggerSync: () -> Unit
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GhostPocketViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GhostPocketViewModel(repository, d1ApiClient, updateManager, context, triggerSync) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
