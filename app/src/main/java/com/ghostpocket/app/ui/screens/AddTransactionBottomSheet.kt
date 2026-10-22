package com.ghostpocket.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghostpocket.app.model.CategoryEntity
import com.ghostpocket.app.model.MoneyUtils
import com.ghostpocket.app.model.TransactionEntity
import com.ghostpocket.app.model.TransactionType
import com.ghostpocket.app.ui.components.CalculatorOperatorRow
import com.ghostpocket.app.ui.components.ClayButton
import com.ghostpocket.app.ui.components.ClayCard

/**
 * ============================================================================
 * AddTransactionBottomSheet
 * ============================================================================
 * Bottom sheet modal for casting a new transaction spell or editing an existing one.
 *
 * Core Components:
 * - Mode switcher: [TransactionTypeSwitcher] (Expense 💸 vs Income 🪙).
 * - Bento amount card with inline calculator row ([CalculatorOperatorRow]).
 * - Quick amount preset chips: [QuickAmountChipsRow] (+₹10, +₹20, +₹50, +₹100, +₹500).
 * - Realm assignment: [CategorySelectionRow] with "+ New" shortcut and "None" option.
 * - Mandatory note validation when no category is selected.
 * - Edit / delete options for existing transactions.
 *
 * @param categories Active categories list.
 * @param onDismiss Dismiss callback.
 * @param onSave Save callback with parsed amount, type, category, and note.
 * @param onAddNewCategoryPrompt Callback opening new category creator.
 * @param transactionToEdit Transaction being edited, or null if creating new.
 * @param onDelete Optional delete callback for existing transaction.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionBottomSheet(
    categories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onSave: (amountPaise: Long, type: TransactionType, category: String, note: String?) -> Unit,
    onAddNewCategoryPrompt: () -> Unit,
    transactionToEdit: TransactionEntity? = null,
    onDelete: ((String) -> Unit)? = null
) {
    // ------------------------------------------------------------------------
    // 1. Sheet State Initialization
    // ------------------------------------------------------------------------
    var amountInput by remember {
        val initialText = if (transactionToEdit != null) {
            if (transactionToEdit.amountPaise % 100 == 0L) {
                (transactionToEdit.amountPaise / 100).toString()
            } else {
                (transactionToEdit.amountPaise / 100.0).toString()
            }
        } else ""
        mutableStateOf(TextFieldValue(initialText, selection = TextRange(initialText.length)))
    }
    var selectedType by remember { mutableStateOf(transactionToEdit?.type ?: TransactionType.EXPENSE) }
    var selectedCategory by remember {
        mutableStateOf<String?>(
            transactionToEdit?.category?.takeIf { it.isNotBlank() && it != "Uncategorized" }
        )
    }
    var noteInput by remember { mutableStateOf(transactionToEdit?.note ?: "") }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    val modalBottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // ------------------------------------------------------------------------
    // 2. Bottom Sheet Layout
    // ------------------------------------------------------------------------
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = modalBottomSheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(48.dp)
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .padding(bottom = 36.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (transactionToEdit != null) "Edit Spell ✏️" else if (selectedType == TransactionType.EXPENSE) "Record Expense 💸" else "Record Income 🪙",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold
                )

                Text(
                    text = "GhostPocket 👻",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Switcher: Expense / Income
            TransactionTypeSwitcher(
                selectedType = selectedType,
                onSelectType = { selectedType = it }
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Amount Input Card with Calculator & Quick Chips
            ClayCard(
                modifier = Modifier.fillMaxWidth(),
                shapeRadius = 22.dp,
                elevation = 4.dp
            ) {
                Text(
                    text = "ENTER AMOUNT",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.1.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "₹",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = amountInput,
                        onValueChange = { input ->
                            if (input.text.all { MoneyUtils.isValidAmountInputChar(it) }) {
                                amountInput = input
                            }
                        },
                        placeholder = { Text("0", style = MaterialTheme.typography.displayMedium) },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent
                        )
                    )
                }

                // Live Calculator Result Preview (e.g. "= ₹160")
                val mathPreview = remember(amountInput.text) { MoneyUtils.formatEvaluatedPreview(amountInput.text) }
                if (mathPreview != null) {
                    Text(
                        text = mathPreview,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 28.dp, top = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Tactile Calculator Operator Row (+, −, ×, ÷, =, ⌫)
                CalculatorOperatorRow(
                    textFieldValue = amountInput,
                    onValueChange = { amountInput = it }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Quick Amount Preset Chips (+₹10, +₹20, +₹50, +₹100, +₹500)
                QuickAmountChipsRow(
                    onPresetClick = { preset ->
                        val currentVal = MoneyUtils.parseToPaise(amountInput.text) ?: 0L
                        val newVal = (currentVal / 100) + preset
                        val newStr = newVal.toString()
                        amountInput = TextFieldValue(newStr, selection = TextRange(newStr.length))
                    }
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Category Realm Selection Row
            CategorySelectionRow(
                categories = categories,
                selectedCategory = selectedCategory,
                onSelectCategory = { selectedCategory = it },
                onAddNewCategoryPrompt = onAddNewCategoryPrompt
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Note Field with mandatory validation if no category is picked
            val isCategorySelected = !selectedCategory.isNullOrBlank()
            val isNoteRequired = !isCategorySelected

            OutlinedTextField(
                value = noteInput,
                onValueChange = { noteInput = it },
                label = {
                    Text(if (isNoteRequired) "Spell Note (Required)*" else "Spell Note (Optional)")
                },
                placeholder = {
                    Text(
                        if (isNoteRequired) "e.g. Chai at canteen, auto fare, printing..."
                        else "Chai with team, bus pass, pizza..."
                    )
                },
                supportingText = {
                    if (isNoteRequired && noteInput.isBlank()) {
                        Text(
                            text = "Note is mandatory when no category is selected",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                isError = isNoteRequired && noteInput.isBlank(),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                textStyle = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Cast / Save Action Button
            val parsedPaise = MoneyUtils.parseToPaise(amountInput.text)
            val effectiveCat = selectedCategory?.trim()?.takeIf { it.isNotBlank() } ?: "Uncategorized"
            val isMetadataValid = if (isCategorySelected) true else noteInput.isNotBlank()
            val canSave = parsedPaise != null && parsedPaise > 0 && isMetadataValid

            ClayButton(
                text = if (transactionToEdit != null) "Save Changes ✨" else "Cast to Ledger ✨",
                enabled = canSave,
                onClick = {
                    if (parsedPaise != null && isMetadataValid) {
                        onSave(parsedPaise, selectedType, effectiveCat, noteInput.trim().takeIf { it.isNotBlank() })
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            // Banish (Delete) Option for Existing Transaction
            if (transactionToEdit != null && onDelete != null) {
                Spacer(modifier = Modifier.height(10.dp))
                ClayButton(
                    text = "Banish Spell 🗑️",
                    isSecondary = true,
                    onClick = { showDeleteConfirmation = true },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirmation && transactionToEdit != null && onDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Banish Spell? ⚠️") },
            text = {
                Text(
                    text = "Are you sure you want to permanently delete this ${MoneyUtils.formatPaiseToInr(transactionToEdit.amountPaise)} spell? This cannot be undone."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmation = false
                        onDelete(transactionToEdit.id)
                        onDismiss()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Banish", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Keep Spell")
                }
            }
        )
    }
}
