package com.ghostpocket.app

import com.ghostpocket.app.model.MoneyUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoneyUtilsTest {

    @Test
    fun testParseToPaise() {
        assertEquals(24000L, MoneyUtils.parseToPaise("240"))
        assertEquals(24050L, MoneyUtils.parseToPaise("240.50"))
        assertEquals(100L, MoneyUtils.parseToPaise("1.00"))
        assertEquals(0L, MoneyUtils.parseToPaise("0"))
        assertNull(MoneyUtils.parseToPaise("-50"))
        assertNull(MoneyUtils.parseToPaise("abc"))
    }

    @Test
    fun testMathematicalExpressions() {
        // Addition: 60 + 100 = 160 -> 16000 paise
        assertEquals(16000L, MoneyUtils.parseToPaise("60 + 100"))
        assertEquals(16000L, MoneyUtils.parseToPaise("60+100"))

        // Subtraction
        assertEquals(7500L, MoneyUtils.parseToPaise("100 - 25"))

        // Multiplication (with * and ×)
        assertEquals(15000L, MoneyUtils.parseToPaise("50 * 3"))
        assertEquals(15000L, MoneyUtils.parseToPaise("50 × 3"))

        // Division (with / and ÷)
        assertEquals(2500L, MoneyUtils.parseToPaise("100 / 4"))
        assertEquals(2500L, MoneyUtils.parseToPaise("100 ÷ 4"))

        // Operator precedence (BODMAS: 2 + 3 * 4 = 14)
        assertEquals(1400L, MoneyUtils.parseToPaise("2 + 3 * 4"))

        // Parentheses
        assertEquals(2000L, MoneyUtils.parseToPaise("(2 + 3) * 4"))

        // Decimals in math
        assertEquals(5775L, MoneyUtils.parseToPaise("45.50 + 12.25"))

        // Live preview formatting
        assertEquals("= ₹160", MoneyUtils.formatEvaluatedPreview("60 + 100"))
        assertNull(MoneyUtils.formatEvaluatedPreview("160")) // Plain numbers don't show preview
        assertNull(MoneyUtils.formatEvaluatedPreview("60 + ")) // Incomplete expressions return null

        // Clean evaluated string
        assertEquals("160", MoneyUtils.evaluateToCleanString("60 + 100"))
        assertEquals("57.75", MoneyUtils.evaluateToCleanString("45.50 + 12.25"))
    }

    @Test
    fun testSplitEquallyRounding() {
        // ₹10 split 3 ways -> 1000 paise split 3 ways
        // 1000 / 3 = 333, remainder 1 -> [334, 333, 333]
        val shares = MoneyUtils.splitEqually(1000L, 3)
        assertEquals(3, shares.size)
        assertEquals(334L, shares[0])
        assertEquals(333L, shares[1])
        assertEquals(333L, shares[2])
        assertEquals(1000L, shares.sum())
    }

    @Test
    fun testFormatPaiseToInr() {
        assertEquals("₹240", MoneyUtils.formatPaiseToInr(24000L))
        assertEquals("₹240.50", MoneyUtils.formatPaiseToInr(24050L))
        assertEquals("-₹50", MoneyUtils.formatPaiseToInr(-5000L))
        assertEquals("+₹100", MoneyUtils.formatPaiseToInr(10000L, showSign = true))
    }
}
