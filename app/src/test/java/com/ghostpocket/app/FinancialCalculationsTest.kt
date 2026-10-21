package com.ghostpocket.app

import com.ghostpocket.app.model.UserBudgetEntity
import com.ghostpocket.app.model.FriendBalance
import com.ghostpocket.app.model.FriendEntity
import com.ghostpocket.app.model.MonthlySummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FinancialCalculationsTest {

    @Test
    fun testMonthlySummaryBalance() {
        val summary = MonthlySummary(
            totalIncomePaise = 1500000L, // ₹15,000
            totalExpensePaise = 850000L  // ₹8,500
        )
        // Balance = Income - Expense = ₹6,500 = 650000L
        assertEquals(650000L, summary.balancePaise)
    }

    @Test
    fun testAllTimeNetBalanceIndependentOfSelectedMonth() {
        val summary = MonthlySummary(
            totalIncomePaise = 200000L,      // Selected month income: ₹2,000
            totalExpensePaise = 500000L,     // Selected month expense: ₹5,000
            initialBudgetPaise = 1000000L,   // Monthly allowance / initial budget: ₹10,000
            allTimeIncomePaise = 2500000L,   // All-time income: ₹25,000
            allTimeExpensePaise = 1200000L   // All-time expense: ₹12,000
        )
        // All-time net balance = 25,000 - 12,000 = ₹13,000 = 1300000L (pure cashflow, no fake budget addition)
        assertEquals(1300000L, summary.balancePaise)
        // Selected month remaining budget = 10,000 - 5,000 = ₹5,000 = 500000L
        assertEquals(500000L, summary.remainingBudgetPaise)
    }

    @Test
    fun testFriendLedgerNetCalculations() {
        val friend = FriendEntity(name = "Aarav")
        
        // Scenario 1: Friend owes user ₹240
        val balance1 = FriendBalance(
            friend = friend,
            theyOweYouPaise = 24000L,
            youOweThemPaise = 0L
        )
        assertEquals(24000L, balance1.netPaise)
        assertEquals(false, balance1.isSettled)

        // Scenario 2: Friend repays ₹50 (represented as offsetting youOweThemPaise = 5000L)
        val balance2 = FriendBalance(
            friend = friend,
            theyOweYouPaise = 24000L,
            youOweThemPaise = 5000L
        )
        // Remaining owed: 240 - 50 = ₹190 = 19000L
        assertEquals(19000L, balance2.netPaise)

        // Scenario 3: Full repayment
        val balance3 = FriendBalance(
            friend = friend,
            theyOweYouPaise = 24000L,
            youOweThemPaise = 24000L
        )
        assertEquals(0L, balance3.netPaise)
        assertTrue(balance3.isSettled)
    }

    @Test
    fun testMonthScopedBudgetResolution() {
        val augBudget = UserBudgetEntity(id = "2026-08", budgetPaise = 1000000L) // ₹10,000
        val octBudget = UserBudgetEntity(id = "2026-10", budgetPaise = 1500000L) // ₹15,000
        val allBudgets: List<UserBudgetEntity> = listOf(augBudget, octBudget)

        fun resolveBudgetForMonth(monthKey: String): Long {
            return allBudgets.find { it.id == monthKey }?.budgetPaise
                ?: allBudgets
                    .filter { it.id.matches(Regex("\\d{4}-\\d{2}")) && it.id < monthKey }
                    .maxByOrNull { it.id }
                    ?.budgetPaise
                ?: allBudgets.find { it.id == "current_budget" }?.budgetPaise
                ?: 0L
        }

        // August 2026 has explicit budget of ₹10,000
        assertEquals(1000000L, resolveBudgetForMonth("2026-08"))

        // September 2026 has no explicit budget -> inherits August 2026 (₹10,000)
        assertEquals(1000000L, resolveBudgetForMonth("2026-09"))

        // October 2026 was raised to ₹15,000
        assertEquals(1500000L, resolveBudgetForMonth("2026-10"))

        // November 2026 has no explicit budget yet -> inherits October 2026 (₹15,000)
        assertEquals(1500000L, resolveBudgetForMonth("2026-11"))

        // July 2026 (before any budget was set) -> returns 0L
        assertEquals(0L, resolveBudgetForMonth("2026-07"))
    }

    @Test
    fun testMonthScopedCategoryBudgetResolution() {
        val foodAug = UserBudgetEntity(id = "cat_Food_2026-08", budgetPaise = 300000L) // ₹3,000
        val foodOct = UserBudgetEntity(id = "cat_Food_2026-10", budgetPaise = 600000L) // ₹6,000
        val baselineCategories = listOf(com.ghostpocket.app.model.CategoryEntity(name = "Food", budgetPaise = 200000L))
        val allBudgets = listOf(foodAug, foodOct)

        fun resolveCatBudget(catName: String, monthKey: String): Long {
            return allBudgets.find { it.id == "cat_${catName}_${monthKey}" }?.budgetPaise
                ?: allBudgets
                    .filter { it.id.startsWith("cat_${catName}_") && it.id.removePrefix("cat_${catName}_") < monthKey }
                    .maxByOrNull { it.id }
                    ?.budgetPaise
                ?: baselineCategories.find { it.name == catName }?.budgetPaise
                ?: 0L
        }

        // August 2026: explicit ₹3,000
        assertEquals(300000L, resolveCatBudget("Food", "2026-08"))
        // September 2026: inherits August 2026 (₹3,000), NOT October!
        assertEquals(300000L, resolveCatBudget("Food", "2026-09"))
        // October 2026: raised to ₹6,000
        assertEquals(600000L, resolveCatBudget("Food", "2026-10"))
        // November 2026: inherits October 2026 (₹6,000)
        assertEquals(600000L, resolveCatBudget("Food", "2026-11"))
        // July 2026: falls back to baseline category budget (₹2,000)
        assertEquals(200000L, resolveCatBudget("Food", "2026-07"))
    }

    @Test
    fun testSplitBillAndRepaymentCashflowReconciliation() {
        // User pays ₹1,000 for dinner for 4 people (₹250 user's share, ₹750 owed by 3 friends)
        var totalIncome = 500000L // Starting with ₹5,000
        var totalExpense = 0L

        // Step 1: Upfront full bill payment (₹1,000 leaves wallet)
        totalExpense += 100000L // ₹1,000
        val balanceAfterBill = totalIncome - totalExpense
        assertEquals(400000L, balanceAfterBill) // Wallet has ₹4,000

        // Step 2: 3 friends each repay ₹250 (recorded as income inflow)
        totalIncome += 25000L // Friend 1
        totalIncome += 25000L // Friend 2
        totalIncome += 25000L // Friend 3

        val finalBalance = totalIncome - totalExpense
        // Final net wallet balance: 5,000 - 250 = ₹4,750
        assertEquals(475000L, finalBalance)
    }
}
