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
    val userMessage: String? = null,
    val isAuthenticated: Boolean = false,
    val authLoading: Boolean = false,
    val authError: String? = null
)

class GhostPocketViewModel(
    private val repository: GhostPocketRepository,
    private val triggerSync: () -> Unit
) : ViewModel() {

    private val _userMessage = MutableStateFlow<String?>(null)
    private val _isSyncing = MutableStateFlow(false)
    private val _authLoading = MutableStateFlow(false)
    private val _authError = MutableStateFlow<String?>(null)
    private val _isAuthenticated = MutableStateFlow(
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
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = GhostPocketUiState()
    )

    fun signIn(email: String, pass: String) {
        viewModelScope.launch {
            _authLoading.value = true
            _authError.value = null
            try {
                com.google.firebase.auth.FirebaseAuth.getInstance()
                    .signInWithEmailAndPassword(email, pass)
                _isAuthenticated.value = true
                _userMessage.value = "Welcome back! 👻"
                triggerSync()
            } catch (e: Exception) {
                // If offline or firebase auth fails, allow guest / demo access
                _authError.value = e.localizedMessage ?: "Crypt doors sealed! Check credentials."
            } finally {
                _authLoading.value = false
            }
        }
    }

    fun signUp(email: String, pass: String) {
        viewModelScope.launch {
            _authLoading.value = true
            _authError.value = null
            try {
                com.google.firebase.auth.FirebaseAuth.getInstance()
                    .createUserWithEmailAndPassword(email, pass)
                _isAuthenticated.value = true
                _userMessage.value = "Account conjured! 🪄"
                triggerSync()
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
                val credential = com.google.firebase.auth.GoogleAuthProvider.getCredential(idToken, null)
                com.google.firebase.auth.FirebaseAuth.getInstance().signInWithCredential(credential)
                _isAuthenticated.value = true
                _userMessage.value = "Welcome, Google Traveler! 👻✨"
                triggerSync()
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
            try {
                com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
            } catch (e: Exception) {
                // Ignore
            }
            // Clear local cache for account switching security & data isolation
            repository.clearLocalCache()
            _isAuthenticated.value = false
            _userMessage.value = "Departed the crypt! Cache cleared 🚪"
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

    fun deleteFriend(id: String) {
        viewModelScope.launch {
            repository.deleteFriend(id)
            _userMessage.value = "Mortal departed! 💨"
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
        }
    }

    fun deleteCategory(category: CategoryEntity) {
        viewModelScope.launch {
            repository.deleteCategory(category)
            _userMessage.value = "${category.name} disintegrated! 💨"
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
    private val triggerSync: () -> Unit
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GhostPocketViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GhostPocketViewModel(repository, triggerSync) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
