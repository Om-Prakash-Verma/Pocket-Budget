package com.ghostpocket.app.viewmodel

import com.ghostpocket.app.model.*

/**
 * Result of computing monthly financial metrics for a selected calendar month.
 */
data class MonthlyBreakdown(
    val summary: MonthlySummary,
    val monthTransactions: List<TransactionEntity>,
    val categorySpends: List<CategorySpend>,
    val monthBudgetEntity: UserBudgetEntity?
)

/**
 * MonthlyFinancialCalculator computes monthly aggregations, budget inheritance,
 * and category spending breakdowns.
 */
object MonthlyFinancialCalculator {

    /**
     * Resolves month-scoped budget, filters transactions, and calculates percentage shares.
     *
     * @param allTxs All transactions in the local database.
     * @param categories All configured user categories.
     * @param allBudgets All saved user budgets (global and month-scoped).
     * @param month The target calendar month.
     */
    fun compute(
        allTxs: List<TransactionEntity>,
        categories: List<CategoryEntity>,
        allBudgets: List<UserBudgetEntity>,
        month: SelectedMonth
    ): MonthlyBreakdown {
        // Month-scoped budget resolution:
        // 1. Exact match for current selected month (e.g. "2026-10")
        // 2. Fallback to latest earlier configured month (e.g. "2026-09")
        // 3. Fallback to legacy "current_budget"
        // 4. Fallback to 0L
        val monthBudgetEntity = allBudgets.find { it.id == month.monthKey }
            ?: allBudgets
                .filter { it.id.matches(Regex("\\d{4}-\\d{2}")) && it.id < month.monthKey }
                .maxByOrNull { it.id }
            ?: allBudgets.find { it.id == "current_budget" }

        val monthBudgetPaise: Long = monthBudgetEntity?.budgetPaise ?: 0L

        val (start, end) = month.timeRange()
        val monthTxs = allTxs.filter { it.timestamp in start..end }
        val income = monthTxs.filter { it.type == TransactionType.INCOME }.sumOf { it.amountPaise }
        val expense = monthTxs.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amountPaise }

        // All-time income and expense for global net balance calculation (all remaining money)
        val allTimeIncome = allTxs.filter { it.type == TransactionType.INCOME }.sumOf { it.amountPaise }
        val allTimeExpense = allTxs.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amountPaise }

        val summary = MonthlySummary(
            totalIncomePaise = income,
            totalExpensePaise = expense,
            initialBudgetPaise = monthBudgetPaise,
            allTimeIncomePaise = allTimeIncome,
            allTimeExpensePaise = allTimeExpense
        )

        val monthExpenses = monthTxs.filter { it.type == TransactionType.EXPENSE }
        val totalExpense = monthExpenses.sumOf { it.amountPaise }
        val groupMap = monthExpenses.groupBy { it.category }
        val allCatNames = (categories.map { it.name } + groupMap.keys).distinct()

        // Month-scoped category budget resolution:
        // 1. Exact match for current selected month: cat_Food_2026-10
        // 2. Fallback to latest earlier configured month: cat_Food_2026-09
        // 3. Fallback to category baseline: CategoryEntity.budgetPaise
        val catBudgetMap = allCatNames.associateWith { catName ->
            allBudgets.find { it.id == "cat_${catName}_${month.monthKey}" }?.budgetPaise
                ?: allBudgets
                    .filter { it.id.startsWith("cat_${catName}_") && it.id.removePrefix("cat_${catName}_") < month.monthKey }
                    .maxByOrNull { it.id }
                    ?.budgetPaise
                ?: categories.find { it.name == catName }?.budgetPaise
                ?: 0L
        }

        val categorySpends = if (categories.isEmpty() && totalExpense <= 0L) {
            emptyList()
        } else {
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

        return MonthlyBreakdown(
            summary = summary,
            monthTransactions = monthTxs,
            categorySpends = categorySpends,
            monthBudgetEntity = monthBudgetEntity
        )
    }
}
