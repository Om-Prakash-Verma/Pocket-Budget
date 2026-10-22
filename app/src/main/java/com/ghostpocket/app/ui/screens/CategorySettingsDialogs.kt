package com.ghostpocket.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ghostpocket.app.model.CategoryEntity
import com.ghostpocket.app.model.MoneyUtils
import com.ghostpocket.app.ui.components.ClayButton

/**
 * ============================================================================
 * AddCategoryDialog
 * ============================================================================
 * Dialog for creating a new custom spending realm (category) with an optional budget.
 *
 * @param onDismiss Dismiss handler.
 * @param onConfirm Callback receiving category name and budget in paise.
 */
@Composable
fun AddCategoryDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, budgetPaise: Long) -> Unit
) {
    var newCatName by remember { mutableStateOf("") }
    var newCatBudget by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Summon New Category 🏷️") },
        text = {
            Column {
                OutlinedTextField(
                    value = newCatName,
                    onValueChange = { newCatName = it },
                    label = { Text("Category Name") },
                    placeholder = { Text("e.g. Chai, Gym, Hostel WiFi") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = newCatBudget,
                    onValueChange = { input ->
                        if (input.all { MoneyUtils.isValidAmountInputChar(it) }) {
                            newCatBudget = input
                        }
                    },
                    label = { Text("Allocated Budget ₹ (Optional)") },
                    placeholder = { Text("e.g. 1500 (or 50*30)") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                val mathPreview = remember(newCatBudget) { MoneyUtils.formatEvaluatedPreview(newCatBudget) }
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
                text = "Summon ✨",
                enabled = newCatName.isNotBlank(),
                onClick = {
                    if (newCatName.isNotBlank()) {
                        val budgetPaise = MoneyUtils.parseToPaise(newCatBudget) ?: 0L
                        onConfirm(newCatName.trim(), budgetPaise)
                        onDismiss()
                    }
                }
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

/**
 * ============================================================================
 * EditCategoryDialog
 * ============================================================================
 * Dialog for renaming an existing realm and updating its monthly budget allowance.
 *
 * @param category The category entity being edited.
 * @param onDismiss Dismiss handler.
 * @param onConfirm Callback receiving updated name and budget in paise.
 */
@Composable
fun EditCategoryDialog(
    category: CategoryEntity,
    onDismiss: () -> Unit,
    onConfirm: (newName: String, newBudgetPaise: Long) -> Unit
) {
    var editName by remember(category) { mutableStateOf(category.name) }
    var editBudget by remember(category) {
        mutableStateOf(if (category.budgetPaise > 0L) (category.budgetPaise / 100).toString() else "")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Transform Realm 🏷️") },
        text = {
            Column {
                OutlinedTextField(
                    value = editName,
                    onValueChange = { editName = it },
                    label = { Text("Realm Name") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = editBudget,
                    onValueChange = { input ->
                        if (input.all { MoneyUtils.isValidAmountInputChar(it) }) {
                            editBudget = input
                        }
                    },
                    label = { Text("Monthly Budget Allowance ₹") },
                    placeholder = { Text("0 to uncapp") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                val mathPreview = remember(editBudget) { MoneyUtils.formatEvaluatedPreview(editBudget) }
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
                text = "Save Realm ✨",
                enabled = editName.isNotBlank(),
                onClick = {
                    if (editName.isNotBlank()) {
                        val budgetPaise = MoneyUtils.parseToPaise(editBudget) ?: 0L
                        onConfirm(editName.trim(), budgetPaise)
                        onDismiss()
                    }
                }
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

/**
 * ============================================================================
 * DeleteCategoryConfirmDialog
 * ============================================================================
 * Safeguard confirmation dialog before deleting a category realm.
 *
 * @param category The category to be deleted.
 * @param onDismiss Dismiss handler.
 * @param onConfirm Delete confirmation callback.
 */
@Composable
fun DeleteCategoryConfirmDialog(
    category: CategoryEntity,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Banish Category? ⚠️") },
        text = {
            Text("Are you sure you want to banish '${category.name}'? Existing spells under this category will safely shift to 'Other'.")
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm()
                    onDismiss()
                },
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Banish", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Keep") }
        }
    )
}
