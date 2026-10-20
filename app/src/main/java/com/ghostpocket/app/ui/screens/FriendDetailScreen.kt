package com.ghostpocket.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghostpocket.app.model.FriendBalance
import com.ghostpocket.app.model.FriendLedgerEntity
import com.ghostpocket.app.model.LedgerDirection
import com.ghostpocket.app.model.MoneyUtils
import com.ghostpocket.app.ui.components.ClayButton
import com.ghostpocket.app.ui.components.ClayCard
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendDetailScreen(
    balance: FriendBalance,
    ledgers: List<FriendLedgerEntity>,
    onBack: () -> Unit,
    onAddLedger: (amountPaise: Long, direction: LedgerDirection, note: String?) -> Unit,
    onUpdateLedger: (FriendLedgerEntity) -> Unit = {},
    onDeleteLedger: (String) -> Unit = {},
    onRecordRepayment: (amountPaise: Long, isRepayingToUser: Boolean) -> Unit,
    onSettleAll: () -> Unit,
    onUpdateFriend: (com.ghostpocket.app.model.FriendEntity) -> Unit = {},
    onDeleteFriend: (settleRemaining: Boolean) -> Unit,
    onSendWhatsAppReminder: (whatsAppNumber: String?, amountPaise: Long, note: String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddLedgerDialog by remember { mutableStateOf(false) }
    var showRepayDialog by remember { mutableStateOf(false) }
    var showWhatsAppReminderDialog by remember { mutableStateOf(false) }
    var showEditFriendDialog by remember { mutableStateOf(false) }
    var showDeleteFriendConfirm by remember { mutableStateOf(false) }
    var selectedLedgerForEdit by remember { mutableStateOf<FriendLedgerEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(balance.friend.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showEditFriendDialog = true }) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Friend")
                    }
                    IconButton(onClick = { showDeleteFriendConfirm = true }) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete Friend")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 18.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 40.dp)
        ) {
            // Net Balance Bento Clay Card
            item {
                ClayCard(
                    modifier = Modifier.fillMaxWidth(),
                    shapeRadius = 24.dp,
                    elevation = 6.dp
                ) {
                    Text(
                        text = "DEBT BALANCE 📜",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    val net = balance.netPaise
                    val (label, amount) = when {
                        net > 0 -> Pair("They owe you gold 🪙", MoneyUtils.formatPaiseToInr(net))
                        net < 0 -> Pair("You owe them gold 💸", MoneyUtils.formatPaiseToInr(kotlin.math.abs(net)))
                        else -> Pair("Settled! Peace restored ✨", "₹0")
                    }

                    Text(
                        text = amount,
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ClayButton(
                            text = "+ Entry",
                            onClick = { showAddLedgerDialog = true },
                            modifier = Modifier.weight(1f)
                        )
                        ClayButton(
                            text = "Repay 🤝",
                            onClick = { showRepayDialog = true },
                            isSecondary = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (net > 0L) {
                        Spacer(modifier = Modifier.height(10.dp))
                        ClayButton(
                            text = "WhatsApp Reminder 💬🖼️",
                            onClick = { showWhatsAppReminderDialog = true },
                            isSecondary = false,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    if (net != 0L) {
                        Spacer(modifier = Modifier.height(10.dp))
                        ClayButton(
                            text = "Exorcise All Debts (Settle) ✓",
                            onClick = onSettleAll,
                            isSecondary = true,
                            icon = Icons.Default.Check,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Debt History 📜",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Ledger Entries
            if (ledgers.isEmpty()) {
                item {
                    Text(
                        text = "No spell history with this mortal yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(ledgers) { entry ->
                    BentoLedgerItem(
                        entry = entry,
                        onClick = { selectedLedgerForEdit = entry }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }

    if (showAddLedgerDialog) {
        AddLedgerEntryDialog(
            onDismiss = { showAddLedgerDialog = false },
            onConfirm = { paise, direction, note ->
                onAddLedger(paise, direction, note)
                showAddLedgerDialog = false
            }
        )
    }

    if (showRepayDialog) {
        RecordRepaymentDialog(
            friendName = balance.friend.name,
            currentNetPaise = balance.netPaise,
            onDismiss = { showRepayDialog = false },
            onConfirm = { paise, isRepayingToUser ->
                onRecordRepayment(paise, isRepayingToUser)
                showRepayDialog = false
            }
        )
    }

    if (showWhatsAppReminderDialog) {
        WhatsAppReminderDialog(
            friendName = balance.friend.name,
            currentPhone = balance.friend.phone,
            amountPaise = balance.netPaise,
            onDismiss = { showWhatsAppReminderDialog = false },
            onSend = { num, note ->
                onSendWhatsAppReminder(num, balance.netPaise, note)
                showWhatsAppReminderDialog = false
            }
        )
    }

    // Edit Friend Dialog
    if (showEditFriendDialog) {
        var editName by remember { mutableStateOf(balance.friend.name) }
        var editPhone by remember { mutableStateOf(balance.friend.phone ?: "") }

        AlertDialog(
            onDismissRequest = { showEditFriendDialog = false },
            title = { Text("Edit Mortal's Scroll 👥") },
            text = {
                Column {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Name") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = editPhone,
                        onValueChange = { editPhone = it },
                        label = { Text("Phone Number / WhatsApp (Optional)") },
                        placeholder = { Text("+91 9876543210") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (editName.isNotBlank()) {
                            onUpdateFriend(
                                balance.friend.copy(
                                    name = editName.trim(),
                                    phone = editPhone.trim().takeIf { it.isNotBlank() }
                                )
                            )
                            showEditFriendDialog = false
                        }
                    },
                    enabled = editName.isNotBlank()
                ) {
                    Text("Save Changes")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditFriendDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Safe Delete Friend Confirmation Dialog
    if (showDeleteFriendConfirm) {
        val hasUnsettledDebt = balance.netPaise != 0L
        AlertDialog(
            onDismissRequest = { showDeleteFriendConfirm = false },
            title = {
                Text(
                    text = if (hasUnsettledDebt) "Unsettled Gold Alert! 🪙⚠️" else "Remove Mortal from Squad? 👥"
                )
            },
            text = {
                if (hasUnsettledDebt) {
                    val debtDesc = if (balance.netPaise > 0) "they owe you" else "you owe them"
                    Text(
                        text = "${balance.friend.name} has an active balance of ${MoneyUtils.formatPaiseToInr(kotlin.math.abs(balance.netPaise))} ($debtDesc).\n\nTo preserve your financial ledger safely without discarded balances, choose 'Settle & Remove' to mark debts resolved before departing."
                    )
                } else {
                    Text(
                        text = "Are you sure you want to remove ${balance.friend.name}? All past records are safely archived in your overall budget."
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteFriendConfirm = false
                        onDeleteFriend(hasUnsettledDebt)
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(
                        text = if (hasUnsettledDebt) "Settle & Remove" else "Remove Mortal",
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteFriendConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Edit / Delete Ledger Entry Dialog
    selectedLedgerForEdit?.let { ledger ->
        EditLedgerEntryDialog(
            entry = ledger,
            onDismiss = { selectedLedgerForEdit = null },
            onUpdate = { updatedLedger ->
                onUpdateLedger(updatedLedger)
                selectedLedgerForEdit = null
            },
            onDelete = { id ->
                onDeleteLedger(id)
                selectedLedgerForEdit = null
            }
        )
    }
}

@Composable
fun BentoLedgerItem(
    entry: FriendLedgerEntity,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val dateStr = remember(entry.timestamp) {
        SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(entry.timestamp))
    }
    val directionText = when (entry.direction) {
        LedgerDirection.THEY_OWE_YOU -> "Lent"
        LedgerDirection.YOU_OWE_THEM -> "Borrowed"
    }

    ClayCard(
        modifier = modifier.fillMaxWidth(),
        shapeRadius = 16.dp,
        elevation = 2.dp,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = entry.note ?: directionText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "$directionText · $dateStr",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = MoneyUtils.formatPaiseToInr(entry.amountPaise),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun AddLedgerEntryDialog(
    onDismiss: () -> Unit,
    onConfirm: (amountPaise: Long, direction: LedgerDirection, note: String?) -> Unit
) {
    var amountInput by remember { mutableStateOf("") }
    var selectedDirection by remember { mutableStateOf(LedgerDirection.THEY_OWE_YOU) }
    var noteInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Add Debt Entry 📜",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        LedgerDirection.THEY_OWE_YOU to "You Lent 🪙",
                        LedgerDirection.YOU_OWE_THEM to "You Borrowed 💸"
                    ).forEach { (dir, label) ->
                        val isSelected = selectedDirection == dir
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { selectedDirection = dir }
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

                ClayCard(
                    modifier = Modifier.fillMaxWidth(),
                    shapeRadius = 18.dp,
                    elevation = 2.dp
                ) {
                    Text(
                        text = "AMOUNT",
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
                                if (input.all { it.isDigit() || it == '.' } && input.count { it == '.' } <= 1) {
                                    amountInput = input
                                }
                            },
                            placeholder = { Text("0") },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                                unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = noteInput,
                    onValueChange = { noteInput = it },
                    label = { Text("Spell Note") },
                    placeholder = { Text("e.g. Cinema ticket, dinner, metro...") },
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            val paise = MoneyUtils.parseToPaise(amountInput)
            ClayButton(
                text = "Cast Spell ✨",
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

@Composable
fun RecordRepaymentDialog(
    friendName: String,
    currentNetPaise: Long,
    onDismiss: () -> Unit,
    onConfirm: (amountPaise: Long, isRepayingToUser: Boolean) -> Unit
) {
    var amountInput by remember {
        val initial = if (currentNetPaise != 0L) (kotlin.math.abs(currentNetPaise) / 100).toString() else ""
        mutableStateOf(initial)
    }
    var isRepayingToUser by remember { mutableStateOf(currentNetPaise >= 0) }

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
                                if (input.all { it.isDigit() || it == '.' } && input.count { it == '.' } <= 1) {
                                    amountInput = input
                                }
                            },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                                unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent
                            )
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
                    text = "A beautiful spooky card image with ₹${amountPaise / 100} and your note will be crafted and opened in WhatsApp!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = whatsAppInput,
                    onValueChange = { whatsAppInput = it },
                    label = { Text("WhatsApp Number") },
                    placeholder = { Text("+91 9876543210") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = noteInput,
                    onValueChange = { noteInput = it },
                    label = { Text("Message / Reason") },
                    placeholder = { Text("e.g. Chai, Split dinner, Hostel rent") },
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
                    onSend(whatsAppInput.trim().takeIf { it.isNotBlank() }, noteInput.trim())
                }
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun EditLedgerEntryDialog(
    entry: FriendLedgerEntity,
    onDismiss: () -> Unit,
    onUpdate: (FriendLedgerEntity) -> Unit,
    onDelete: (String) -> Unit
) {
    var amountInput by remember(entry) {
        mutableStateOf(
            if (entry.amountPaise % 100 == 0L) (entry.amountPaise / 100).toString()
            else (entry.amountPaise / 100.0).toString()
        )
    }
    var selectedDirection by remember(entry) { mutableStateOf(entry.direction) }
    var noteInput by remember(entry) { mutableStateOf(entry.note ?: "") }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Edit Debt Spell 📜",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        LedgerDirection.THEY_OWE_YOU to "You Lent 🪙",
                        LedgerDirection.YOU_OWE_THEM to "You Borrowed 💸"
                    ).forEach { (dir, label) ->
                        val isSelected = selectedDirection == dir
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { selectedDirection = dir }
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

                ClayCard(
                    modifier = Modifier.fillMaxWidth(),
                    shapeRadius = 18.dp,
                    elevation = 2.dp
                ) {
                    Text(
                        text = "AMOUNT",
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
                                if (input.all { it.isDigit() || it == '.' } && input.count { it == '.' } <= 1) {
                                    amountInput = input
                                }
                            },
                            placeholder = { Text("0") },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                                unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = noteInput,
                    onValueChange = { noteInput = it },
                    label = { Text("Spell Note") },
                    placeholder = { Text("e.g. Cinema ticket, dinner, metro...") },
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
