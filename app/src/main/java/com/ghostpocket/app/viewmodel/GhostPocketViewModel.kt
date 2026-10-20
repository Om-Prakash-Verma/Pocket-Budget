package com.ghostpocket.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ghostpocket.app.data.repository.GhostPocketRepository
import com.ghostpocket.app.model.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class GhostPocketUiState(
    val summary: MonthlySummary = MonthlySummary(),
    val recentTransactions: List<TransactionEntity> = emptyList(),
    val allTransactions: List<TransactionEntity> = emptyList(),
    val friends: List<FriendBalance> = emptyList(),
    val rawFriends: List<FriendEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val categorySpends: List<CategorySpend> = emptyList(),
    val userBudget: UserBudgetEntity? = null,
    val isSyncing: Boolean = false,
    val isInitialDataFetching: Boolean = false,
    val userMessage: String? = null,
    val isAuthenticated: Boolean = false,
    val authLoading: Boolean = false,
    val authError: String? = null
)

class GhostPocketViewModel(
    private val repository: GhostPocketRepository,
    private val d1ApiClient: com.ghostpocket.app.data.remote.D1ApiClient? = null,
    private val triggerSync: () -> Unit
) : ViewModel() {

    private val _userMessage = MutableStateFlow<String?>(null)
    private val _isSyncing = MutableStateFlow(false)
    private val _isInitialDataFetching = MutableStateFlow(false)
    private val _authLoading = MutableStateFlow(false)
    private val _authError = MutableStateFlow<String?>(null)
    private val _isAuthenticated = MutableStateFlow(
        (d1ApiClient?.authToken != null && d1ApiClient.currentUserId != "guest_user") ||
        try {
            com.google.firebase.auth.FirebaseAuth.getInstance().currentUser != null
        } catch (e: Exception) {
            false
        }
    )


    val uiState: StateFlow<GhostPocketUiState> = combine(
        repository.monthlySummary,
        repository.allTransactions,
        repository.allFriendsWithBalances,
        repository.allFriends,
        repository.allCategories
    ) { summary, allTxs, friends, rawFriends, categories ->
        GhostPocketUiState(
            summary = summary,
            recentTransactions = allTxs,
            allTransactions = allTxs,
            friends = friends,
            rawFriends = rawFriends,
            categories = categories
        )
    }.combine(repository.categorySpendsCurrentMonth) { state, spends ->
        state.copy(categorySpends = spends)
    }.combine(repository.userBudget) { state, budget ->
        state.copy(userBudget = budget)
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
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = GhostPocketUiState()
    )


    fun signIn(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            _authError.value = "Email and password cannot be empty."
            return
        }
        viewModelScope.launch {
            _authLoading.value = true
            _authError.value = null
            try {
                // 1. Try Cloudflare D1 Auth
                var authSuccess = false
                var failureReason: String? = null

                if (d1ApiClient != null) {
                    val res = d1ApiClient.login(email.trim(), pass)
                    if (res.userId != null) {
                        authSuccess = true
                    } else if (res.error != null) {
                        failureReason = res.error
                    }
                }

                // 2. If D1 failed due to network, try Firebase as fallback
                if (!authSuccess && failureReason?.contains("Invalid email or password", ignoreCase = true) != true) {
                    try {
                        com.google.firebase.auth.FirebaseAuth.getInstance()
                            .signInWithEmailAndPassword(email.trim(), pass)
                        authSuccess = true
                    } catch (e: Exception) {
                        if (failureReason == null) {
                            failureReason = e.localizedMessage
                        }
                    }
                }

                if (authSuccess) {
                    _isAuthenticated.value = true
                    _isInitialDataFetching.value = true
                    _userMessage.value = "Welcome back! 👻"
                    
                    // Fetch cloud data immediately on login
                    viewModelScope.launch {
                        try {
                            if (d1ApiClient != null) {
                                val pullRes = d1ApiClient.unifiedSync(
                                    com.ghostpocket.app.data.remote.UnifiedSyncRequest(sinceTimestamp = 0L)
                                )
                                pullRes.remoteUpdates?.let { remote ->
                                    repository.applyD1PullResults(remote, d1ApiClient.currentUserId)
                                }
                            }
                        } catch (e: Exception) {
                            // If network failure, schedule background retry
                            triggerSync()
                        } finally {
                            _isInitialDataFetching.value = false
                        }
                    }
                } else {
                    _authError.value = failureReason ?: "Crypt doors sealed! Invalid email or password."
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
                var authSuccess = false
                var failureReason: String? = null

                if (d1ApiClient != null) {
                    val res = d1ApiClient.register(email.trim(), pass)
                    if (res.userId != null) {
                        authSuccess = true
                    } else if (res.error != null) {
                        failureReason = res.error
                    }
                }

                if (!authSuccess && failureReason?.contains("already exists", ignoreCase = true) != true) {
                    try {
                        com.google.firebase.auth.FirebaseAuth.getInstance()
                            .createUserWithEmailAndPassword(email.trim(), pass)
                        authSuccess = true
                    } catch (e: Exception) {
                        if (failureReason == null) {
                            failureReason = e.localizedMessage
                        }
                    }
                }

                if (authSuccess) {
                    _isAuthenticated.value = true
                    _userMessage.value = "Account conjured! 🪄"
                    triggerSync()
                } else {
                    _authError.value = failureReason ?: "Spell failed! Could not create account."
                }
            } catch (e: Exception) {
                _authError.value = e.localizedMessage ?: "Spell failed! Could not create account."
            } finally {
                _authLoading.value = false
            }
        }
    }


    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _authLoading.value = true
            _authError.value = null
            try {
                // Cloudflare D1 Google OAuth Verification
                if (d1ApiClient != null) {
                    d1ApiClient.googleSignIn(idToken)
                }

                try {
                    val credential = com.google.firebase.auth.GoogleAuthProvider.getCredential(idToken, null)
                    com.google.firebase.auth.FirebaseAuth.getInstance().signInWithCredential(credential)
                } catch (e: Exception) {
                    // Firebase offline / optional
                }

                _isAuthenticated.value = true
                _isInitialDataFetching.value = true
                _userMessage.value = "Welcome, Google Traveler! 👻✨"
                
                viewModelScope.launch {
                    try {
                        if (d1ApiClient != null) {
                            val pullRes = d1ApiClient.unifiedSync(
                                com.ghostpocket.app.data.remote.UnifiedSyncRequest(sinceTimestamp = 0L)
                            )
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
            } catch (e: Exception) {
                _authError.value = e.localizedMessage ?: "Google sign-in spell failed."
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
                try {
                    com.google.firebase.auth.FirebaseAuth.getInstance().signInAnonymously()
                } catch (e: Exception) {
                    // Offline fallback
                }
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
            try {
                com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
            } catch (e: Exception) {
                // Ignore
            }
            // Clear local cache for account switching security & data isolation
            repository.clearLocalCache()
            _isAuthenticated.value = false
            _userMessage.value = "Vanished into thin air... 👻"
        }
    }

    fun clearMessage() {

        _userMessage.value = null
    }

    fun setUserBudget(budgetPaise: Long) {
        viewModelScope.launch {
            repository.setUserBudget(budgetPaise)
            _userMessage.value = "Budget locked in! 💰👻"
        }
    }

    fun addTransaction(amountPaise: Long, type: TransactionType, category: String, note: String?) {
        viewModelScope.launch {
            val tx = TransactionEntity(
                amountPaise = amountPaise,
                type = type,
                category = category,
                note = note
            )
            repository.addTransaction(tx)
            _userMessage.value = if (type == TransactionType.EXPENSE) "Money vanished! 💸👻" else "Pocket fed! 🪙✨"
            triggerSync()
        }
    }

    fun editTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.updateTransaction(transaction)
            _userMessage.value = "Spell altered in the crypt! 🪄"
            triggerSync()
        }
    }

    fun deleteTransaction(id: String) {
        viewModelScope.launch {
            repository.deleteTransaction(id)
            _userMessage.value = "Banished into the void! 🪄"
            triggerSync()
        }
    }

    fun addFriend(name: String, phone: String?) {
        viewModelScope.launch {
            repository.addFriend(name, phone)
            _userMessage.value = "New mortal summoned! 👥👻"
            triggerSync()
        }
    }

    fun updateFriend(friend: FriendEntity) {
        viewModelScope.launch {
            repository.updateFriend(friend)
            _userMessage.value = "Mortal's scroll updated! 👥✨"
            triggerSync()
        }
    }

    fun deleteFriend(id: String) {
        viewModelScope.launch {
            repository.deleteFriend(id)
            _userMessage.value = "Mortal departed! 💨"
            triggerSync()
        }
    }

    fun deleteFriendSafe(id: String, settleRemaining: Boolean) {
        viewModelScope.launch {
            repository.deleteFriendSafe(id, settleRemaining)
            _userMessage.value = if (settleRemaining) "All debts settled & mortal removed! 💨" else "Mortal departed! 💨"
            triggerSync()
        }
    }

    fun addLedgerEntry(friendId: String, amountPaise: Long, direction: LedgerDirection, note: String?) {
        viewModelScope.launch {
            repository.addLedgerEntry(friendId, amountPaise, direction, note)
            _userMessage.value = if (direction == LedgerDirection.THEY_OWE_YOU) "Debt spell cast! 📜✨" else "You owe them gold! 🪙"
            triggerSync()
        }
    }

    fun updateLedgerEntry(ledger: FriendLedgerEntity) {
        viewModelScope.launch {
            repository.updateLedgerEntry(ledger)
            _userMessage.value = "Debt ledger updated! 📜✨"
            triggerSync()
        }
    }

    fun deleteLedgerEntry(id: String) {
        viewModelScope.launch {
            repository.deleteLedgerEntry(id)
            _userMessage.value = "Ledger entry banished! 💨"
            triggerSync()
        }
    }

    fun recordRepayment(friendId: String, amountPaise: Long, isRepayingToUser: Boolean) {
        viewModelScope.launch {
            repository.recordRepayment(friendId, amountPaise, isRepayingToUser)
            _userMessage.value = "Debt exorcised! Finally! ✓"
            triggerSync()
        }
    }

    fun settleFriend(friendId: String) {
        viewModelScope.launch {
            repository.settleFriendLedger(friendId)
            _userMessage.value = "All debts vanished! Clean slate! 👻✨"
            triggerSync()
        }
    }

    fun splitBill(title: String, totalPaise: Long, payerFriendId: String?, participantFriendIds: List<String>, category: String) {
        viewModelScope.launch {
            repository.splitBill(title, totalPaise, payerFriendId, participantFriendIds, category)
            _userMessage.value = "Bill sliced into pieces! ⚔️👻"
            triggerSync()
        }
    }

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
            triggerSync()
        }
    }

    fun editCategory(oldName: String, newName: String, newBudgetPaise: Long) {
        viewModelScope.launch {
            repository.renameCategory(oldName, newName, newBudgetPaise)
            _userMessage.value = "Realm transformed to $newName! 🏷️✨"
            triggerSync()
        }
    }

    fun deleteCategory(category: CategoryEntity) {
        viewModelScope.launch {
            repository.deleteCategory(category)
            _userMessage.value = "${category.name} disintegrated! 💨"
            triggerSync()
        }
    }

    fun deleteCategorySafe(category: CategoryEntity, fallbackCategory: String = "Other") {
        viewModelScope.launch {
            repository.deleteCategorySafe(category, fallbackCategory)
            _userMessage.value = "${category.name} disintegrated! Records preserved in $fallbackCategory 💨"
            triggerSync()
        }
    }

    fun getLedgerEntriesForFriend(friendId: String): Flow<List<FriendLedgerEntity>> {
        return repository.getLedgerEntriesForFriend(friendId)
    }

    fun manualSync() {
        viewModelScope.launch {
            _isSyncing.value = true
            triggerSync()
            kotlinx.coroutines.delay(1200)
            _isSyncing.value = false
            _userMessage.value = "Cloud spirits synced! ☁️👻"
        }
    }
}

class GhostPocketViewModelFactory(
    private val repository: GhostPocketRepository,
    private val d1ApiClient: com.ghostpocket.app.data.remote.D1ApiClient? = null,
    private val triggerSync: () -> Unit
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GhostPocketViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GhostPocketViewModel(repository, d1ApiClient, triggerSync) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

