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
     * Converts a rupee string (or mathematical expression representation) into integer paise safely.
     * Supports addition (+), subtraction (-), multiplication (*, ×), and division (/, ÷).
     */
    fun parseToPaise(input: String): Long? {
        val eval = evaluateMathExpression(input) ?: return null
        if (eval < 0) return null
        return (eval * 100 + 0.5).toLong()
    }

    /**
     * Safely evaluates mathematical expressions (e.g. "60 + 100", "150 * 2 - 50", "45.50 + 12.25").
     * Respects standard BODMAS precedence (* / before + -).
     */
    fun evaluateMathExpression(input: String): Double? {
        val sanitized = input.trim()
            .replace("₹", "")
            .replace(",", "")
            .replace("×", "*")
            .replace("÷", "/")
            .replace(" ", "")

        if (sanitized.isEmpty()) return null

        return try {
            val parser = ExpressionParser(sanitized)
            val result = parser.parse()
            if (result.isNaN() || result.isInfinite()) null else result
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Returns a live formatted preview if the input contains a mathematical operation (e.g. "= ₹160").
     */
    fun formatEvaluatedPreview(input: String): String? {
        val hasOperator = input.any { it == '+' || it == '-' || it == '*' || it == '/' || it == '×' || it == '÷' }
        if (!hasOperator) return null
        val paise = parseToPaise(input) ?: return null
        return "= ${formatPaiseToInr(paise)}"
    }

    /**
     * Evaluates a mathematical expression to a clean number string (e.g. "60 + 100" -> "160").
     */
    fun evaluateToCleanString(input: String): String? {
        val eval = evaluateMathExpression(input) ?: return null
        if (eval < 0) return null
        return if (eval % 1.0 == 0.0) {
            eval.toLong().toString()
        } else {
            String.format(Locale.US, "%.2f", eval).trimEnd('0').trimEnd('.')
        }
    }

    /**
     * Checks if a character is valid for calculator-enabled amount input fields.
     */
    fun isValidAmountInputChar(c: Char): Boolean {
        return c.isDigit() || c == '.' || c == '+' || c == '-' || c == '*' || c == '/' ||
                c == '×' || c == '÷' || c == '(' || c == ')' || c == ' '
    }

    private class ExpressionParser(private val src: String) {
        private var pos = -1
        private var ch = 0

        private fun nextChar() {
            ch = if (++pos < src.length) src[pos].code else -1
        }

        private fun eat(charToEat: Int): Boolean {
            while (ch == ' '.code) nextChar()
            if (ch == charToEat) {
                nextChar()
                return true
            }
            return false
        }

        fun parse(): Double {
            nextChar()
            val x = parseExpression()
            while (ch == ' '.code) nextChar()
            if (pos < src.length) throw IllegalArgumentException("Unexpected char: " + ch.toChar())
            return x
        }

        // expression = term (('+' | '-') term)*
        private fun parseExpression(): Double {
            var x = parseTerm()
            while (true) {
                when {
                    eat('+'.code) -> x += parseTerm()
                    eat('-'.code) -> x -= parseTerm()
                    else -> return x
                }
            }
        }

        // term = factor (('*' | '/') factor)*
        private fun parseTerm(): Double {
            var x = parseFactor()
            while (true) {
                when {
                    eat('*'.code) -> x *= parseFactor()
                    eat('/'.code) -> {
                        val divisor = parseFactor()
                        if (divisor == 0.0) throw ArithmeticException("Division by zero")
                        x /= divisor
                    }
                    else -> return x
                }
            }
        }

        // factor = ('+' | '-') factor | '(' expression ')' | number
        private fun parseFactor(): Double {
            if (eat('+'.code)) return parseFactor() // unary plus
            if (eat('-'.code)) return -parseFactor() // unary minus

            var x: Double
            val startPos = pos
            if (eat('('.code)) {
                x = parseExpression()
                if (!eat(')'.code)) throw IllegalArgumentException("Missing closing parenthesis")
            } else if ((ch >= '0'.code && ch <= '9'.code) || ch == '.'.code) {
                while ((ch >= '0'.code && ch <= '9'.code) || ch == '.'.code) nextChar()
                val numStr = src.substring(startPos, pos)
                x = numStr.toDouble()
            } else {
                throw IllegalArgumentException("Unexpected char: " + if (ch == -1) "EOF" else ch.toChar().toString())
            }
            return x
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
