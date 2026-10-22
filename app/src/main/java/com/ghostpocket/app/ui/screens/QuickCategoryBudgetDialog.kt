package com.ghostpocket.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ghostpocket.app.model.CategorySpend
import com.ghostpocket.app.model.MoneyUtils
import com.ghostpocket.app.ui.components.ClayButton

/**
 * ============================================================================
 * QuickCategoryBudgetDialog
 * ============================================================================
 * 1-Tap Budget Editor Dialog launched directly from the Home Dashboard's Realms breakdown.
 *
 * Capabilities:
 * - Direct numerical input for monthly budget allowance.
 * - Inline mathematical calculator evaluation preview (e.g. "50*30 = ₹1,500").
 * - Instant persistence for the selected month.
 *
 * @param spend Category spending model containing category name and current allocation.
 * @param monthLabel Human-readable label for the active month (e.g., "October 2026").
 * @param onDismiss Invoked when the user dismisses the dialog.
 * @param onSave Invoked with the new budget amount in paise.
 */
@Composable
fun QuickCategoryBudgetDialog(
    spend: CategorySpend,
    monthLabel: String,
    onDismiss: () -> Unit,
    onSave: (budgetPaise: Long) -> Unit
) {
    var budgetInput by remember(spend) {
        mutableStateOf(if (spend.allocatedBudgetPaise > 0L) (spend.allocatedBudgetPaise / 100).toString() else "")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "${spend.category} Budget 🏷️",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "Set monthly spending allowance for ${spend.category} in $monthLabel.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedTextField(
                    value = budgetInput,
                    onValueChange = { input ->
                        if (input.all { MoneyUtils.isValidAmountInputChar(it) }) {
                            budgetInput = input
                        }
                    },
                    label = { Text("Allocated Budget ₹") },
                    placeholder = { Text("e.g. 1500 (or 50*30)") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                val mathPreview = remember(budgetInput) { MoneyUtils.formatEvaluatedPreview(budgetInput) }
                if (mathPreview != null) {
                    Text(
                        text = mathPreview,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            ClayButton(
                text = "Save Budget ✨",
                onClick = {
                    val budgetPaise = MoneyUtils.parseToPaise(budgetInput) ?: 0L
                    onSave(budgetPaise)
                    onDismiss()
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
