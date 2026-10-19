package com.ghostpocket.app

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
}
