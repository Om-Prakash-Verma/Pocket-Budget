package com.ghostpocket.app.viewmodel

import com.ghostpocket.app.model.*

/**
 * GhostPocketUiState represents the entire observable UI state for the Ghost Pocket application.
 *
 * @property summary Aggregated income, expense, and budget summary for the selected calendar month.
 * @property recentTransactions Transactions executed within the active month.
 * @property allTransactions Complete transaction history across all months.
 * @property friends Squad members mapped with their current net debt balances.
 * @property rawFriends Raw list of friend entities without debt computation.
 * @property categories All available spending categories.
 * @property categorySpends Category spending breakdown and budget utilization percentages.
 * @property userBudget Current active monthly budget entity.
 * @property allBudgets All user budget entities (month-scoped and global).
 * @property selectedMonth Currently viewed calendar year and month.
 * @property creationMonth Account creation or first transaction month (lower bound for history navigation).
 * @property isSyncing Whether background cloud sync is actively executing.
 * @property isInitialDataFetching Whether initial cloud data pull is taking place on authentication.
 * @property userMessage Transient snackbar/toast feedback message for user actions.
 * @property isAuthenticated Whether user is currently authenticated via Cloudflare D1.
 * @property authLoading Whether an auth request (sign in/sign up) is processing.
 * @property authError Error message from an authentication failure, if any.
 */
data class GhostPocketUiState(
    val summary: MonthlySummary = MonthlySummary(),
    val recentTransactions: List<TransactionEntity> = emptyList(),
    val allTransactions: List<TransactionEntity> = emptyList(),
    val friends: List<FriendBalance> = emptyList(),
    val rawFriends: List<FriendEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val categorySpends: List<CategorySpend> = emptyList(),
    val userBudget: UserBudgetEntity? = null,
    val allBudgets: List<UserBudgetEntity> = emptyList(),
    val selectedMonth: SelectedMonth = SelectedMonth.current(),
    val creationMonth: SelectedMonth = SelectedMonth.current(),
    val isSyncing: Boolean = false,
    val isInitialDataFetching: Boolean = false,
    val userMessage: String? = null,
    val isAuthenticated: Boolean = false,
    val authLoading: Boolean = false,
    val authError: String? = null
) {
    /**
     * Determines whether the user can navigate to an earlier month in the calendar carousel.
     * Blocked if current selection is equal to or earlier than account creation month.
     */
    val canSelectPreviousMonth: Boolean
        get() = selectedMonth > creationMonth
}
