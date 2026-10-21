package com.ghostpocket.app

import com.ghostpocket.app.model.SelectedMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SelectedMonthTest {

    @Test
    fun testSelectedMonthComparison() {
        val jan2026 = SelectedMonth(2026, 1)
        val feb2026 = SelectedMonth(2026, 2)
        val dec2025 = SelectedMonth(2025, 12)

        assertTrue(jan2026 > dec2025)
        assertTrue(feb2026 > jan2026)
        assertTrue(jan2026 == SelectedMonth(2026, 1))
        assertFalse(dec2025 >= jan2026)
        assertTrue(jan2026 >= jan2026)
    }

    @Test
    fun testFromMonthKey() {
        val month = SelectedMonth.fromMonthKey("2026-03")
        assertEquals(2026, month.year)
        assertEquals(3, month.month)
        assertEquals("2026-03", month.monthKey)
    }
}
