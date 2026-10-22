package com.ghostpocket.app.ui.screens

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ghostpocket.app.model.FriendBalance
import com.ghostpocket.app.model.FriendEntity
import com.ghostpocket.app.model.FriendLedgerEntity
import com.ghostpocket.app.model.LedgerDirection
import com.ghostpocket.app.model.MoneyUtils

/**
 * ============================================================================
 * FriendDetailScreen
 * ============================================================================
 * Primary screen for managing an individual friend's debt balance and transaction history.
 *
 * Key Capabilities:
 * - View at-a-glance net balance, total gave (🔴), and total got (🟢).
 * - Log new Gave/Got transactions via [AddLedgerEntryDialog].
 * - Edit or banish individual past entries via [EditLedgerEntryDialog].
 * - Send custom WhatsApp payment reminders with payment breakdown cards.
 * - Settle all debts cleanly with one tap.
 * - Safely edit friend info or delete a friend with debt preservation safeguards.
 */
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
    onUpdateFriend: (FriendEntity) -> Unit = {},
    onDeleteFriend: (settleRemaining: Boolean) -> Unit,
    onSendWhatsAppReminder: (whatsAppNumber: String?, amountPaise: Long, note: String?) -> Unit,
    modifier: Modifier = Modifier
) {
    // ------------------------------------------------------------------------
    // 1. Dialog & Screen States
    // ------------------------------------------------------------------------
    var showAddLedgerDialog by remember { mutableStateOf(false) }
    var initialDialogDirection by remember { mutableStateOf(LedgerDirection.THEY_OWE_YOU) }
    var showWhatsAppReminderDialog by remember { mutableStateOf(false) }
    var showEditFriendDialog by remember { mutableStateOf(false) }
    var showDeleteFriendConfirm by remember { mutableStateOf(false) }
    var selectedLedgerForEdit by remember { mutableStateOf<FriendLedgerEntity?>(null) }

    val context = LocalContext.current
    var editName by remember(balance.friend) { mutableStateOf(balance.friend.name) }
    var editPhone by remember(balance.friend) { mutableStateOf(balance.friend.phone ?: "") }

    // ------------------------------------------------------------------------
    // 2. System Contact Picker Launcher
    // ------------------------------------------------------------------------
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
                                if (!name.isNullOrBlank()) editName = name
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

    // ------------------------------------------------------------------------
    // 3. Screen Scaffold & Content
    // ------------------------------------------------------------------------
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
            // Net Balance Bento Clay Card Header
            item {
                FriendDetailHeader(
                    balance = balance,
                    onGaveClick = {
                        initialDialogDirection = LedgerDirection.THEY_OWE_YOU
                        showAddLedgerDialog = true
                    },
                    onGotClick = {
                        initialDialogDirection = LedgerDirection.YOU_OWE_THEM
                        showAddLedgerDialog = true
                    },
                    onWhatsAppReminderClick = { showWhatsAppReminderDialog = true },
                    onSettleAllClick = onSettleAll
                )

                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Debt History 📜",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Ledger Entries History List
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

    // ------------------------------------------------------------------------
    // 4. Sub-Dialogs Orchestration
    // ------------------------------------------------------------------------

    // Add Gave/Got Transaction Dialog
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

    // WhatsApp Reminder Generator Dialog
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

    // Edit Friend Details Dialog
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
                Text(text = if (hasUnsettledDebt) "Unsettled Gold Alert! 🪙⚠️" else "Remove Mortal from Squad? 👥")
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

    // Edit / Delete Individual Ledger Entry Dialog
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
