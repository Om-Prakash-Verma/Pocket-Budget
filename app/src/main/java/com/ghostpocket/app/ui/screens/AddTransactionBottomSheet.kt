package com.ghostpocket.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghostpocket.app.model.CategoryEntity
import com.ghostpocket.app.model.MoneyUtils
import com.ghostpocket.app.model.TransactionEntity
import com.ghostpocket.app.model.TransactionType
import com.ghostpocket.app.ui.components.CategoryIcon
import com.ghostpocket.app.ui.components.ClayButton
import com.ghostpocket.app.ui.components.ClayCard

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
    var amountInput by remember {
        mutableStateOf(
            if (transactionToEdit != null) {
                if (transactionToEdit.amountPaise % 100 == 0L) {
                    (transactionToEdit.amountPaise / 100).toString()
                } else {
                    (transactionToEdit.amountPaise / 100.0).toString()
                }
            } else ""
        )
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

            // Bento Switcher: Expense / Income
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Expense Pill
                val isExpense = selectedType == TransactionType.EXPENSE
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isExpense) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { selectedType = TransactionType.EXPENSE }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = null,
                            tint = if (isExpense) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Expense 💸",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (isExpense) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Income Pill
                val isIncome = selectedType == TransactionType.INCOME
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isIncome) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { selectedType = TransactionType.INCOME }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ArrowDownward,
                            contentDescription = null,
                            tint = if (isIncome) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Income 🪙",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (isIncome) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Bento Clay Card for Amount Input
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
                            if (input.all { it.isDigit() || it == '.' } && input.count { it == '.' } <= 1) {
                                amountInput = input
                            }
                        },
                        placeholder = { Text("0", style = MaterialTheme.typography.displayMedium) },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                            unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Category Selection Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Choose Realm (Optional)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (selectedCategory == null) "No realm selected • Note required below" else "Selected: ${selectedCategory}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selectedCategory == null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Quick Add Category Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { onAddNewCategoryPrompt() }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+ New", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // "None / Uncategorized" option
                item {
                    val isNoneSelected = selectedCategory == null
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isNoneSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { selectedCategory = null }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "✨ None",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isNoneSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                items(categories) { cat ->
                    val isSelected = cat.name == selectedCategory
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                            .clickable {
                                selectedCategory = if (selectedCategory == cat.name) null else cat.name
                            }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CategoryIcon(
                            categoryName = cat.name,
                            modifier = Modifier.size(16.dp),
                            tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = cat.name,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            val isCategorySelected = !selectedCategory.isNullOrBlank()
            val isNoteRequired = !isCategorySelected

            // Note Field with rounded container
            OutlinedTextField(
                value = noteInput,
                onValueChange = { noteInput = it },
                label = {
                    Text(
                        if (isNoteRequired) "Spell Note (Required)*" else "Spell Note (Optional)"
                    )
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
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Cast / Save Button
            val parsedPaise = MoneyUtils.parseToPaise(amountInput)
            val effectiveCat = selectedCategory?.trim()?.takeIf { it.isNotBlank() } ?: "Uncategorized"
            val isMetadataValid = if (isCategorySelected) true else noteInput.isNotBlank()
            val canSave = parsedPaise != null && parsedPaise > 0 && isMetadataValid

            val saveButtonText = if (transactionToEdit != null) "Save Changes ✨" else "Cast to Ledger ✨"
            ClayButton(
                text = saveButtonText,
                enabled = canSave,
                onClick = {
                    if (parsedPaise != null && isMetadataValid) {
                        onSave(parsedPaise, selectedType, effectiveCat, noteInput.trim().takeIf { it.isNotBlank() })
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

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
