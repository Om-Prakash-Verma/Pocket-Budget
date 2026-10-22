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
import com.ghostpocket.app.model.LedgerDirection
import com.ghostpocket.app.model.MoneyUtils
import com.ghostpocket.app.ui.components.ClayButton
import com.ghostpocket.app.ui.components.ClayCard

/**
 * ============================================================================
 * AddLedgerEntryDialog
 * ============================================================================
 * Dialog for adding a new debt entry to a friend's ledger.
 *
 * Streamlined Options:
 * - "You Gave 🔴": Friend owes the user (Direction: [LedgerDirection.THEY_OWE_YOU]).
 * - "You Got 🟢": User owes the friend (Direction: [LedgerDirection.YOU_OWE_THEM]).
 *
 * Includes inline mathematical expression evaluation preview (e.g. "60+100 = ₹160").
 *
 * @param initialDirection Default direction selected on open.
 * @param friendName Name of the friend for whom the entry is being logged.
 * @param onDismiss Invoked when the user closes or cancels the dialog.
 * @param onConfirm Invoked when confirmed, returning amount in paise, direction, and note.
 */
@Composable
fun AddLedgerEntryDialog(
    initialDirection: LedgerDirection = LedgerDirection.THEY_OWE_YOU,
    friendName: String = "Friend",
    onDismiss: () -> Unit,
    onConfirm: (amountPaise: Long, direction: LedgerDirection, note: String?) -> Unit
) {
    // ------------------------------------------------------------------------
    // 1. Dialog State
    // ------------------------------------------------------------------------
    var amountInput by remember { mutableStateOf("") }
    var selectedDirection by remember(initialDirection) { mutableStateOf(initialDirection) }
    var noteInput by remember { mutableStateOf("") }

    val isGave = selectedDirection == LedgerDirection.THEY_OWE_YOU
    val themeColor = if (isGave) Color(0xFFEF4444) else Color(0xFF10B981)

    // ------------------------------------------------------------------------
    // 2. Alert Dialog UI
    // ------------------------------------------------------------------------
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isGave) "You Gave $friendName 🔴" else "You Got from $friendName 🟢",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                // Section: Direction Selector Toggle (Gave vs Got)
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

                // Section: Amount Input with Live Math Evaluation
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

                // Section: Optional Note / Description
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
            ClayButton(
                text = if (isGave) "Confirm Gave 🔴" else "Confirm Got 🟢",
                customColor = themeColor,
                enabled = paise != null && paise > 0,
                onClick = {
                    if (paise != null && paise > 0) {
                        onConfirm(paise, selectedDirection, noteInput.takeIf { it.isNotBlank() })
                    }
                }
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
