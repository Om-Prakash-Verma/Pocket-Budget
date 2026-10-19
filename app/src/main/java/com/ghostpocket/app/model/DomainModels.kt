package com.ghostpocket.app.model

data class FriendBalance(
    val friend: FriendEntity,
    val theyOweYouPaise: Long,
    val youOweThemPaise: Long
) {
    val netPaise: Long
        get() = theyOweYouPaise - youOweThemPaise

    val isSettled: Boolean
        get() = netPaise == 0L
}

data class MonthlySummary(
    val totalIncomePaise: Long = 0L,
    val totalExpensePaise: Long = 0L,
    val initialBudgetPaise: Long = 0L
) {
    /**
     * Effective balance: User's set budget + total income - total expenses
     */
    val balancePaise: Long
        get() = initialBudgetPaise + totalIncomePaise - totalExpensePaise

    val remainingBudgetPaise: Long
        get() = initialBudgetPaise - totalExpensePaise

    val budgetUsageFraction: Float
        get() = if (initialBudgetPaise > 0L) {
            (totalExpensePaise.toFloat() / initialBudgetPaise.toFloat()).coerceIn(0f, 1.5f)
        } else 0f
}

data class CategorySpend(
    val category: String,
    val totalPaise: Long,
    val percentage: Float,
    val allocatedBudgetPaise: Long = 0L
) {
    val budgetUsageFraction: Float
        get() = if (allocatedBudgetPaise > 0L) {
            (totalPaise.toFloat() / allocatedBudgetPaise.toFloat()).coerceIn(0f, 1.5f)
        } else 0f

    val isOverBudget: Boolean
        get() = allocatedBudgetPaise > 0L && totalPaise > allocatedBudgetPaise
}
