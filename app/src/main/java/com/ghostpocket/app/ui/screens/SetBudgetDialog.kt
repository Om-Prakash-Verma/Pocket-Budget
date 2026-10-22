package com.ghostpocket.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghostpocket.app.model.MoneyUtils
import com.ghostpocket.app.model.SelectedMonth
import com.ghostpocket.app.ui.components.ClayButton
import com.ghostpocket.app.ui.components.ClayCard

/**
 * ============================================================================
 * SetBudgetDialog
 * ============================================================================
 * Dialog allowing the user to set or adjust the overall monthly budget allowance
 * for the selected month.
 *
 * @param selectedMonth Currently active month descriptor.
 * @param initialBudgetPaise Current budget allocation in paise.
 * @param onDismiss Dismiss handler.
 * @param onConfirm Callback receiving confirmed budget in paise.
 */
@Composable
fun SetBudgetDialog(
    selectedMonth: SelectedMonth,
    initialBudgetPaise: Long,
    onDismiss: () -> Unit,
    onConfirm: (budgetPaise: Long) -> Unit
) {
    var budgetInput by remember(selectedMonth, initialBudgetPaise) {
        mutableStateOf(
            if (initialBudgetPaise > 0L) (initialBudgetPaise / 100).toString() else ""
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Budget for ${selectedMonth.label} 💰👻",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "Set your gold allowance specifically for ${selectedMonth.label}. Other months will preserve their own budgets intact.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))

                ClayCard(
                    modifier = Modifier.fillMaxWidth(),
                    shapeRadius = 18.dp,
                    elevation = 2.dp
                ) {
                    Text(
                        text = "MONTHLY ALLOWANCE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "₹",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        OutlinedTextField(
                            value = budgetInput,
                            onValueChange = { input ->
                                if (input.all { MoneyUtils.isValidAmountInputChar(it) }) {
                                    budgetInput = input
                                }
                            },
                            placeholder = { Text("10000") },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent
                            )
                        )
                    }

                    val mathPreview = remember(budgetInput) { MoneyUtils.formatEvaluatedPreview(budgetInput) }
                    if (mathPreview != null) {
                        Text(
                            text = mathPreview,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 24.dp, top = 2.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            val paise = MoneyUtils.parseToPaise(budgetInput)
            ClayButton(
                text = "Lock In 🔒",
                enabled = paise != null,
                onClick = {
                    if (paise != null) {
                        onConfirm(paise)
                        onDismiss()
                    }
                }
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
