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
import com.ghostpocket.app.model.MoneyUtils
import com.ghostpocket.app.ui.components.ClayButton
import com.ghostpocket.app.ui.components.ClayCard

/**
 * ============================================================================
 * RecordRepaymentDialog
 * ============================================================================
 * Dialog allowing the user to record a debt settlement or partial repayment.
 *
 * Repayment Flows:
 * - Friend repaid user (reduces amount friend owes user).
 * - User repaid friend (reduces amount user owes friend).
 *
 * @param friendName Name of the friend.
 * @param currentNetPaise Current net balance in paise.
 * @param onDismiss Dismiss handler.
 * @param onConfirm Repayment handler with parsed paise and repayment direction.
 */
@Composable
fun RecordRepaymentDialog(
    friendName: String,
    currentNetPaise: Long,
    onDismiss: () -> Unit,
    onConfirm: (amountPaise: Long, isRepayingToUser: Boolean) -> Unit
) {
    // ------------------------------------------------------------------------
    // 1. Initial State
    // ------------------------------------------------------------------------
    var amountInput by remember {
        val initial = if (currentNetPaise != 0L) (kotlin.math.abs(currentNetPaise) / 100).toString() else ""
        mutableStateOf(initial)
    }
    var isRepayingToUser by remember { mutableStateOf(currentNetPaise >= 0) }

    // ------------------------------------------------------------------------
    // 2. Dialog Layout
    // ------------------------------------------------------------------------
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Record Repayment 🤝",
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
                        true to "$friendName Repaid 🪙",
                        false to "You Repaid 💸"
                    ).forEach { (toUser, label) ->
                        val isSelected = isRepayingToUser == toUser
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { isRepayingToUser = toUser }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Section: Repayment Amount Input
                ClayCard(
                    modifier = Modifier.fillMaxWidth(),
                    shapeRadius = 18.dp,
                    elevation = 2.dp
                ) {
                    Text(
                        text = "REPAYMENT AMOUNT",
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
                            value = amountInput,
                            onValueChange = { input ->
                                if (input.all { MoneyUtils.isValidAmountInputChar(it) }) {
                                    amountInput = input
                                }
                            },
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
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 24.dp, top = 2.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            val paise = MoneyUtils.parseToPaise(amountInput)
            ClayButton(
                text = "Confirm Repayment ✓",
                enabled = paise != null && paise > 0,
                onClick = {
                    if (paise != null && paise > 0) {
                        onConfirm(paise, isRepayingToUser)
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
 * WhatsAppReminderDialog
 * ============================================================================
 * Dialog for generating and launching a WhatsApp payment reminder message & image.
 *
 * @param friendName Name of the friend.
 * @param currentPhone Friend's phone number if already known.
 * @param amountPaise Pending balance amount in paise.
 * @param onDismiss Dismiss handler.
 * @param onSend Handler receiving phone number and reminder note to trigger WhatsApp.
 */
@Composable
fun WhatsAppReminderDialog(
    friendName: String,
    currentPhone: String?,
    amountPaise: Long,
    onDismiss: () -> Unit,
    onSend: (whatsAppNumber: String?, note: String?) -> Unit
) {
    var whatsAppInput by remember { mutableStateOf(currentPhone ?: "") }
    var noteInput by remember { mutableStateOf("Shared expense from college") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "WhatsApp Reminder 💬🖼️",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "Send a clean, friendly reminder card directly to $friendName's WhatsApp for ${MoneyUtils.formatPaiseToInr(amountPaise)}.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedTextField(
                    value = whatsAppInput,
                    onValueChange = { whatsAppInput = it },
                    label = { Text("WhatsApp Number (with country code)") },
                    placeholder = { Text("e.g. 919876543210") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = noteInput,
                    onValueChange = { noteInput = it },
                    label = { Text("Optional Context / Note") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            ClayButton(
                text = "Send via WhatsApp 🚀",
                onClick = {
                    onSend(whatsAppInput.trim().takeIf { it.isNotBlank() }, noteInput.trim().takeIf { it.isNotBlank() })
                }
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
