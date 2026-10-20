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
    val initialBudgetPaise: Long = 0L,
    val allTimeIncomePaise: Long = totalIncomePaise,
    val allTimeExpensePaise: Long = totalExpensePaise
) {
    /**
     * Net balance: User's set budget + all-time income - all-time expenses.
     * Represents all remaining money across all time, independent of historical month views.
     */
    val balancePaise: Long
        get() = initialBudgetPaise + allTimeIncomePaise - allTimeExpensePaise

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

data class SelectedMonth(
    val year: Int,
    val month: Int // 1 to 12
) {
    val label: String
        get() {
            val cal = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.YEAR, year)
                set(java.util.Calendar.MONTH, month - 1)
                set(java.util.Calendar.DAY_OF_MONTH, 1)
            }
            return java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale.getDefault()).format(cal.time)
        }

    val isCurrentMonth: Boolean
        get() {
            val now = java.util.Calendar.getInstance()
            return now.get(java.util.Calendar.YEAR) == year && (now.get(java.util.Calendar.MONTH) + 1) == month
        }

    fun previous(): SelectedMonth {
        return if (month == 1) SelectedMonth(year - 1, 12) else SelectedMonth(year, month - 1)
    }

    fun next(): SelectedMonth {
        return if (month == 12) SelectedMonth(year + 1, 1) else SelectedMonth(year, month + 1)
    }

    fun timeRange(): Pair<Long, Long> {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.YEAR, year)
        cal.set(java.util.Calendar.MONTH, month - 1)
        cal.set(java.util.Calendar.DAY_OF_MONTH, 1)
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis

        cal.set(java.util.Calendar.DAY_OF_MONTH, cal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH))
        cal.set(java.util.Calendar.HOUR_OF_DAY, 23)
        cal.set(java.util.Calendar.MINUTE, 59)
        cal.set(java.util.Calendar.SECOND, 59)
        cal.set(java.util.Calendar.MILLISECOND, 999)
        val end = cal.timeInMillis
        return Pair(start, end)
    }

    companion object {
        fun current(): SelectedMonth {
            val now = java.util.Calendar.getInstance()
            return SelectedMonth(now.get(java.util.Calendar.YEAR), now.get(java.util.Calendar.MONTH) + 1)
        }
    }
}
