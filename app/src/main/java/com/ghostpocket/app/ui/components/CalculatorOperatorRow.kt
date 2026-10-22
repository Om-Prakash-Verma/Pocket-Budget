package com.ghostpocket.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.ghostpocket.app.model.MoneyUtils

/**
 * ============================================================================
 * CalculatorOperatorRow
 * ============================================================================
 * Tactile Bento Clay Calculator operator row (+, −, ×, ÷, =, ⌫) for inline math calculations.
 *
 * Cursor Placement:
 * - When an operator is clicked, the cursor is automatically repositioned directly
 *   after the inserted operator so typing continues naturally.
 * - Operator replacement: Consecutive operator taps replace the previous operator.
 * - Evaluation '=' evaluates the expression using [MoneyUtils.evaluateToCleanString].
 *
 * @param textFieldValue Current [TextFieldValue] of the input field.
 * @param onValueChange Callback receiving the updated [TextFieldValue] with updated cursor position.
 * @param modifier Composable modifier.
 */
@Composable
fun CalculatorOperatorRow(
    textFieldValue: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val operators = listOf(
            "+" to "+",
            "−" to "-",
            "×" to "*",
            "÷" to "/"
        )

        // Mathematical Operator Pills (+, −, ×, ÷)
        operators.forEach { (label, op) ->
            ClayCalculatorPill(
                label = label,
                modifier = Modifier.weight(1f),
                onClick = {
                    val text = textFieldValue.text
                    val selStart = textFieldValue.selection.min.coerceIn(0, text.length)
                    val selEnd = textFieldValue.selection.max.coerceIn(0, text.length)

                    val before = text.substring(0, selStart)
                    val after = text.substring(selEnd)

                    // If user clicks an operator while immediately preceding char is another operator, replace it
                    val trimmedBefore = before.trimEnd()
                    val cleanBefore = if (trimmedBefore.isNotEmpty() &&
                        (trimmedBefore.endsWith("+") || trimmedBefore.endsWith("-") ||
                         trimmedBefore.endsWith("*") || trimmedBefore.endsWith("/") ||
                         trimmedBefore.endsWith("×") || trimmedBefore.endsWith("÷"))
                    ) {
                        trimmedBefore.dropLast(1)
                    } else {
                        before
                    }

                    if (cleanBefore.isNotEmpty() || op == "-") {
                        val newText = cleanBefore + op + after
                        val newCursorPos = cleanBefore.length + op.length
                        onValueChange(
                            TextFieldValue(
                                text = newText,
                                selection = TextRange(newCursorPos)
                            )
                        )
                    }
                }
            )
        }

        // Evaluate '=' Button
        ClayCalculatorPill(
            label = "=",
            isAccent = true,
            modifier = Modifier.weight(1f),
            onClick = {
                val evaluated = MoneyUtils.evaluateToCleanString(textFieldValue.text)
                if (evaluated != null) {
                    onValueChange(
                        TextFieldValue(
                            text = evaluated,
                            selection = TextRange(evaluated.length)
                        )
                    )
                }
            }
        )

        // Backspace '⌫' Button
        ClayCalculatorPill(
            label = "⌫",
            modifier = Modifier.weight(1f),
            onClick = {
                val text = textFieldValue.text
                val selStart = textFieldValue.selection.min.coerceIn(0, text.length)
                val selEnd = textFieldValue.selection.max.coerceIn(0, text.length)

                if (selStart != selEnd) {
                    val newText = text.substring(0, selStart) + text.substring(selEnd)
                    onValueChange(
                        TextFieldValue(
                            text = newText,
                            selection = TextRange(selStart)
                        )
                    )
                } else if (selStart > 0) {
                    val newText = text.substring(0, selStart - 1) + text.substring(selStart)
                    onValueChange(
                        TextFieldValue(
                            text = newText,
                            selection = TextRange(selStart - 1)
                        )
                    )
                }
            }
        )
    }
}

/**
 * Convenience overload accepting plain String and callback.
 */
@Composable
fun CalculatorOperatorRow(
    currentInput: String,
    onInputChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    CalculatorOperatorRow(
        textFieldValue = TextFieldValue(currentInput, selection = TextRange(currentInput.length)),
        onValueChange = { onInputChanged(it.text) },
        modifier = modifier
    )
}

/**
 * ============================================================================
 * ClayCalculatorPill
 * ============================================================================
 * Tactile pill button styled specifically for calculator keypad rows.
 */
@Composable
fun ClayCalculatorPill(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isAccent: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val shape = remember { RoundedCornerShape(12.dp) }
    val isDark = MaterialTheme.colorScheme.background.red < 0.5f

    val bgBrush = remember(isDark, isAccent) {
        if (isAccent) {
            Brush.verticalGradient(listOf(Color(0xFF8B5CF6), Color(0xFF6D28D9)))
        } else if (isDark) {
            Brush.verticalGradient(listOf(Color(0xFF242430), Color(0xFF191922)))
        } else {
            Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0xFFE8E8F0)))
        }
    }

    val textColor = when {
        isAccent -> Color.White
        isDark -> Color(0xFFEEEEF5)
        else -> Color(0xFF1B1B20)
    }

    val elevation = if (isPressed) 1.dp else 3.dp

    Box(
        modifier = modifier
            .height(38.dp)
            .shadow(elevation, shape)
            .clip(shape)
            .background(bgBrush)
            .border(
                1.dp,
                if (isAccent) Color(0xFFA78BFA) else if (isDark) Color(0xFF333344) else Color(0xFFD6D6E2),
                shape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}
