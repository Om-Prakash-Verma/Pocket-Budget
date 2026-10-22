package com.ghostpocket.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghostpocket.app.model.FriendLedgerEntity
import com.ghostpocket.app.model.LedgerDirection
import com.ghostpocket.app.model.MoneyUtils
import com.ghostpocket.app.ui.components.ClayButton
import com.ghostpocket.app.ui.components.ClayCard

/**
 * ============================================================================
 * EditLedgerEntryDialog
 * ============================================================================
 * Dialog allowing users to modify an existing friend ledger entry or banish (delete) it.
 *
 * Capabilities:
 * - Switch direction between Gave 🔴 and Got 🟢.
 * - Modify amount with inline expression evaluation.
 * - Update description note.
 * - Banish (delete) with a confirmation safeguard dialog.
 *
 * @param entry The existing ledger entity being edited.
 * @param friendName Name of the friend associated with this entry.
 * @param onDismiss Invoked when the dialog is dismissed without changes.
 * @param onUpdate Invoked with the updated [FriendLedgerEntity] when confirmed.
 * @param onDelete Invoked with the entry's unique ID when deleted.
 */
@Composable
fun EditLedgerEntryDialog(
    entry: FriendLedgerEntity,
    friendName: String = "Friend",
    onDismiss: () -> Unit,
    onUpdate: (FriendLedgerEntity) -> Unit,
    onDelete: (String) -> Unit
) {
    // ------------------------------------------------------------------------
    // 1. Initial State Initialization
    // ------------------------------------------------------------------------
    var amountInput by remember(entry) {
        mutableStateOf(
            if (entry.amountPaise % 100 == 0L) (entry.amountPaise / 100).toString()
            else (entry.amountPaise / 100.0).toString()
        )
    }
    var selectedDirection by remember(entry) { mutableStateOf(entry.direction) }
    var noteInput by remember(entry) { mutableStateOf(entry.note ?: "") }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val isGave = selectedDirection == LedgerDirection.THEY_OWE_YOU
    val themeColor = if (isGave) Color(0xFFEF4444) else Color(0xFF10B981)

    // ------------------------------------------------------------------------
    // 2. Edit Dialog Body
    // ------------------------------------------------------------------------
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isGave) "Edit: You Gave $friendName 🔴" else "Edit: You Got from $friendName 🟢",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                // Section: Direction Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        LedgerDirection.THEY_OWE_YOU to "You Gave 🔴",
                        LedgerDirection.YOU_OWE_THEM to "You Got 🟢"
                    ).forEach { (dir, label) ->
                        val isSelected = selectedDirection == dir
                        val activeColor = if (dir == LedgerDirection.THEY_OWE_YOU) Color(0xFFEF4444) else Color(0xFF10B981)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) activeColor else Color.Transparent)
                                .clickable { selectedDirection = dir }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Section: Amount Input with Math Evaluation
                ClayCard(
                    modifier = Modifier.fillMaxWidth(),
                    shapeRadius = 18.dp,
                    elevation = 2.dp
                ) {
                    Text(
                        text = if (isGave) "AMOUNT GIVEN" else "AMOUNT RECEIVED",
                        style = MaterialTheme.typography.labelSmall,
                        color = themeColor,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "₹",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = themeColor
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        OutlinedTextField(
                            value = amountInput,
                            onValueChange = { input ->
                                if (input.all { MoneyUtils.isValidAmountInputChar(it) }) {
                                    amountInput = input
                                }
                            },
                            placeholder = { Text("0") },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent
                            )
                        )
                    }

                    val mathPreview = remember(amountInput) { MoneyUtils.formatEvaluatedPreview(amountInput) }
                    if (mathPreview != null) {
                        Text(
                            text = mathPreview,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = themeColor,
                            modifier = Modifier.padding(start = 24.dp, top = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Section: Description Note
                OutlinedTextField(
                    value = noteInput,
                    onValueChange = { noteInput = it },
                    label = { Text("Note / Description") },
                    placeholder = { Text(if (isGave) "e.g. Chai, petrol, movie ticket..." else "e.g. Dinner share, repaid via UPI...") },
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            val paise = MoneyUtils.parseToPaise(amountInput)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    onClick = { showDeleteConfirm = true },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Banish 🗑️")
                }
                ClayButton(
                    text = "Save Changes ✨",
                    customColor = themeColor,
                    enabled = paise != null && paise > 0,
                    onClick = {
                        if (paise != null && paise > 0) {
                            onUpdate(
                                entry.copy(
                                    amountPaise = paise,
                                    direction = selectedDirection,
                                    note = noteInput.takeIf { it.isNotBlank() }
                                )
                            )
                        }
                    }
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )

    // ------------------------------------------------------------------------
    // 3. Delete Safeguard Confirmation Dialog
    // ------------------------------------------------------------------------
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Banish Debt Entry? ⚠️") },
            text = {
                Text(
                    text = "Are you sure you want to delete this ${MoneyUtils.formatPaiseToInr(entry.amountPaise)} entry? This will immediately recalculate your squad balance."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete(entry.id)
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Banish Entry", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Keep Entry")
                }
            }
        )
    }
}
