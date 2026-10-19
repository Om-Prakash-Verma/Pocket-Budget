package com.ghostpocket.app

import com.ghostpocket.app.model.LedgerDirection
import com.ghostpocket.app.model.MoneyUtils
import org.junit.Assert.assertEquals
import org.junit.Test

class SmartSyncLogicTest {

    @Test
    fun testRemainderDistributedSplitBillPreservesSum() {
        val totalPaise = 250000L // ₹2500.00
        val participantsCount = 7
        val shares = MoneyUtils.splitEqually(totalPaise, participantsCount)
        
        assertEquals(participantsCount, shares.size)
        assertEquals(totalPaise, shares.sum())
    }

    @Test
    fun testBilateralLedgerCalculation() {
        // Friend owes user ₹500, user owes friend ₹200 -> Net +₹300
        val theyOweYouPaise = 50000L
        val youOweThemPaise = 20000L
        val net = theyOweYouPaise - youOweThemPaise
        assertEquals(30000L, net)

        // After ₹300 repayment to user
        val remainingNet = net - 30000L
        assertEquals(0L, remainingNet)
    }

    @Test
    fun testBatchSizeBoundary() {
        val maxBatchOps = 450
        val totalOperations = 1000
        val batchesNeeded = (totalOperations + maxBatchOps - 1) / maxBatchOps
        assertEquals(3, batchesNeeded)
    }
}
