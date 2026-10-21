package com.ghostpocket.app.ui.screens

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
    var initialDialogDirection by remember { mutableStateOf(LedgerDirection.THEY_OWE_YOU) }
    var showWhatsAppReminderDialog by remember { mutableStateOf(false) }
    var showEditFriendDialog by remember { mutableStateOf(false) }
    var showDeleteFriendConfirm by remember { mutableStateOf(false) }
    var selectedLedgerForEdit by remember { mutableStateOf<FriendLedgerEntity?>(null) }

    val context = LocalContext.current
    var editName by remember(balance.friend) { mutableStateOf(balance.friend.name) }
    var editPhone by remember(balance.friend) { mutableStateOf(balance.friend.phone ?: "") }

    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val contactUri: Uri? = result.data?.data
            if (contactUri != null) {
                try {
                    val projection = arrayOf(
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                        ContactsContract.CommonDataKinds.Phone.NUMBER
                    )
                    context.contentResolver.query(contactUri, projection, null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                            val numberIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                            if (nameIdx != -1) {
                                val name = cursor.getString(nameIdx)
                                if (!name.isNullOrBlank()) {
                                    editName = name
                                }
                            }
                            if (numberIdx != -1) {
                                val rawNumber = cursor.getString(numberIdx)
                                if (!rawNumber.isNullOrBlank()) {
                                    editPhone = rawNumber.replace("[\\s\\-\\(\\)]".toRegex(), "")
                                }
                            }
                        }
                    }
                    showEditFriendDialog = true
                } catch (e: Exception) {
                    Toast.makeText(context, "Could not load contact: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

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
                        text = "BALANCE WITH ${balance.friend.name.uppercase()} 📜",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    val net = balance.netPaise
                    val (label, amount, netColor) = when {
                        net > 0 -> Triple("You will get (They owe you) 🪙", MoneyUtils.formatPaiseToInr(net), Color(0xFF10B981))
                        net < 0 -> Triple("You will give (You owe them) 💸", MoneyUtils.formatPaiseToInr(kotlin.math.abs(net)), Color(0xFFEF4444))
                        else -> Triple("Settled! No pending balance ✨", "₹0", MaterialTheme.colorScheme.onSurface)
                    }

                    Text(
                        text = amount,
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = netColor
                    )
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Mini summary breakdown: Total Gave (Red) vs Total Got (Green)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "YOU GAVE 🔴",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFEF4444)
                            )
                            Text(
                                text = MoneyUtils.formatPaiseToInr(balance.theyOweYouPaise),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFEF4444)
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "YOU GOT 🟢",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF10B981)
                            )
                            Text(
                                text = MoneyUtils.formatPaiseToInr(balance.youOweThemPaise),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF10B981)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Only two options: You Gave (Red) and You Got (Green)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ClayButton(
                            text = "You Gave 🔴",
                            onClick = {
                                initialDialogDirection = LedgerDirection.THEY_OWE_YOU
                                showAddLedgerDialog = true
                            },
                            customColor = Color(0xFFEF4444),
                            modifier = Modifier.weight(1f)
                        )
                        ClayButton(
                            text = "You Got 🟢",
                            onClick = {
                                initialDialogDirection = LedgerDirection.YOU_OWE_THEM
                                showAddLedgerDialog = true
                            },
                            customColor = Color(0xFF10B981),
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
                items(
                    items = ledgers,
                    key = { it.id },
                    contentType = { "ledger_entry" }
                ) { entry ->
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
            initialDirection = initialDialogDirection,
            friendName = balance.friend.name,
            onDismiss = { showAddLedgerDialog = false },
            onConfirm = { paise, direction, note ->
                onAddLedger(paise, direction, note)
                showAddLedgerDialog = false
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
        AlertDialog(
            onDismissRequest = { showEditFriendDialog = false },
            title = { Text("Edit Mortal's Scroll 👥") },
            text = {
                Column {
                    OutlinedButton(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
                                contactPickerLauncher.launch(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "No contacts app available on device", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Contacts",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Update from Contacts 📱",
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

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
            friendName = balance.friend.name,
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
    val isGave = entry.direction == LedgerDirection.THEY_OWE_YOU
    val directionText = if (isGave) "You Gave" else "You Got"
    val accentColor = if (isGave) Color(0xFFEF4444) else Color(0xFF10B981)
    val badgeBg = if (isGave) Color(0xFFEF4444).copy(alpha = 0.14f) else Color(0xFF10B981).copy(alpha = 0.14f)
    val sign = if (isGave) "− " else "+ "

    ClayCard(
        modifier = modifier.fillMaxWidth(),
        shapeRadius = 18.dp,
        elevation = 2.dp,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f, fill = false)
            ) {
                // Direction indicator bubble
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(badgeBg),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isGave) "🔴" else "🟢",
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = entry.note?.takeIf { it.isNotBlank() } ?: directionText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = badgeBg
                        ) {
                            Text(
                                text = directionText,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = accentColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "· $dateStr",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Amount: RED for Gave, GREEN for Got!
            Text(
                text = "$sign${MoneyUtils.formatPaiseToInr(entry.amountPaise)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = accentColor
            )
        }
    }
}

@Composable
fun AddLedgerEntryDialog(
    initialDirection: LedgerDirection = LedgerDirection.THEY_OWE_YOU,
    friendName: String = "Friend",
    onDismiss: () -> Unit,
    onConfirm: (amountPaise: Long, direction: LedgerDirection, note: String?) -> Unit
) {
    var amountInput by remember { mutableStateOf("") }
    var selectedDirection by remember(initialDirection) { mutableStateOf(initialDirection) }
    var noteInput by remember { mutableStateOf("") }

    val isGave = selectedDirection == LedgerDirection.THEY_OWE_YOU
    val themeColor = if (isGave) Color(0xFFEF4444) else Color(0xFF10B981)

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
                                focusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                                unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent
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
                                if (input.all { MoneyUtils.isValidAmountInputChar(it) }) {
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
    friendName: String = "Friend",
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

    val isGave = selectedDirection == LedgerDirection.THEY_OWE_YOU
    val themeColor = if (isGave) Color(0xFFEF4444) else Color(0xFF10B981)

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
                                focusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                                unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent
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
