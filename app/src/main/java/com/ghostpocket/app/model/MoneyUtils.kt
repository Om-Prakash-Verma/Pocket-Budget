package com.ghostpocket.app.model

import java.text.NumberFormat
import java.util.Locale

object MoneyUtils {
    /**
     * Formats an amount in paise to Indian Rupee representation (e.g. ₹240 or ₹240.50).
     */
    fun formatPaiseToInr(amountPaise: Long, showSign: Boolean = false): String {
        val rupees = amountPaise / 100.0
        val isNegative = amountPaise < 0
        val absRupees = kotlin.math.abs(rupees)

        val formatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
        // Some systems format as INR 240.00; we standardize cleanly to '₹'
        val formatted = if (absRupees % 1.0 == 0.0) {
            String.format(Locale("en", "IN"), "₹%,d", absRupees.toLong())
        } else {
            String.format(Locale("en", "IN"), "₹%,.2f", absRupees)
        }

        return when {
            showSign && isNegative -> "-$formatted"
            showSign && amountPaise > 0 -> "+$formatted"
            isNegative -> "-$formatted"
            else -> formatted
        }
    }

    /**
     * Converts a rupee string (or float representation) into integer paise safely.
     */
    fun parseToPaise(input: String): Long? {
        val sanitized = input.trim().replace("₹", "").replace(",", "")
        if (sanitized.isEmpty()) return null
        return try {
            val doubleVal = sanitized.toDouble()
            if (doubleVal < 0) null else (doubleVal * 100 + 0.5).toLong()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Splits an amount in paise among N participants fairly, distributing remainder paise
     * one-by-one to prevent loss of precision.
     */
    fun splitEqually(totalPaise: Long, count: Int): List<Long> {
        if (count <= 0) return emptyList()
        val baseShare = totalPaise / count
        val remainder = (totalPaise % count).toInt()
        return List(count) { index ->
            if (index < remainder) baseShare + 1 else baseShare
        }
    }
}
