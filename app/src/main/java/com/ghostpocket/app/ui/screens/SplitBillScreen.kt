package com.ghostpocket.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghostpocket.app.model.FriendEntity
import com.ghostpocket.app.model.MoneyUtils
import com.ghostpocket.app.ui.components.CalculatorOperatorRow
import com.ghostpocket.app.ui.components.ClayButton
import com.ghostpocket.app.ui.components.ClayCard

/**
 * ============================================================================
 * SplitBillScreen
 * ============================================================================
 * Bill splitting interface allowing users to divide group expenses with friends.
 *
 * Core Features:
 * - Bill Title & Total Amount input with tactile mathematical calculator row.
 * - Payer selector: [BillPayerSelector] ("You Paid 👑" vs Squad member).
 * - Split mode switch: [BillSplitModeSelector] (Equal Split ⚖️ vs Custom Shares 🍕).
 * - Live Allocation status verification: [CustomShareAllocationCard] (Matched ✅ / Remaining ⚠️ / Over 🛑).
 * - Per-person customized split shares: [ParticipantFriendSplitCard].
 *
 * @param friends List of friends available to participate in the split.
 * @param onBack Callback when navigating back.
 * @param onSplitConfirmed Callback invoked with confirmed split title, total, payer, and shares.
 * @param modifier Composable modifier.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SplitBillScreen(
    friends: List<FriendEntity>,
    onBack: () -> Unit,
    onSplitConfirmed: (title: String, totalPaise: Long, payerFriendId: String?, participantFriendIds: List<String>, customShares: Map<String, Long>?) -> Unit,
    modifier: Modifier = Modifier
) {
    // ------------------------------------------------------------------------
    // 1. Screen State
    // ------------------------------------------------------------------------
    var titleInput by remember { mutableStateOf("") }
    var amountInput by remember { mutableStateOf(TextFieldValue("")) }
    var payerFriendId by remember { mutableStateOf<String?>(null) } // null = You
    val selectedFriendIds = remember { mutableStateListOf<String>() }

    var isCustomSplit by remember { mutableStateOf(false) }
    val customShareInputs = remember { mutableStateMapOf<String, String>() }

    val parsedTotalPaise = MoneyUtils.parseToPaise(amountInput.text) ?: 0L
    val totalParticipants = selectedFriendIds.size + 1 // squad friends + current user
    val equalShares = remember(parsedTotalPaise, totalParticipants) {
        if (parsedTotalPaise > 0 && totalParticipants > 1) {
            MoneyUtils.splitEqually(parsedTotalPaise, totalParticipants)
        } else {
            emptyList()
        }
    }
    val perPersonPaise = equalShares.firstOrNull() ?: 0L

    // Live calculation of total custom shares allocated
    val totalAllocatedPaise = remember(customShareInputs.toMap(), isCustomSplit, selectedFriendIds.toList()) {
        if (!isCustomSplit) {
            parsedTotalPaise
        } else {
            val userPaise = MoneyUtils.parseToPaise(customShareInputs["user"] ?: "") ?: 0L
            val friendsPaise = selectedFriendIds.sumOf { fId ->
                MoneyUtils.parseToPaise(customShareInputs[fId] ?: "") ?: 0L
            }
            userPaise + friendsPaise
        }
    }

    // Auto-prefill custom share inputs when switching to custom mode if inputs are empty
    LaunchedEffect(isCustomSplit) {
        if (isCustomSplit && parsedTotalPaise > 0 && selectedFriendIds.isNotEmpty()) {
            if (customShareInputs.isEmpty()) {
                val userAmt = (perPersonPaise / 100).toString()
                customShareInputs["user"] = userAmt
                selectedFriendIds.forEach { fId ->
                    customShareInputs[fId] = userAmt
                }
            }
        }
    }

    // ------------------------------------------------------------------------
    // 2. Scaffold & Form Structure
    // ------------------------------------------------------------------------
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Split the Bill ⚔️") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            // Bill Title Input
            item {
                OutlinedTextField(
                    value = titleInput,
                    onValueChange = { titleInput = it },
                    label = { Text("What was the spell for? (Title)") },
                    placeholder = { Text("e.g. Midnight Shawarma, Hostel WiFi, Pizza") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Total Amount Clay Bento Box with Live Calculator
            item {
                ClayCard(
                    modifier = Modifier.fillMaxWidth(),
                    shapeRadius = 22.dp,
                    elevation = 4.dp
                ) {
                    Text(
                        text = "TOTAL BILL AMOUNT",
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

                    // Calculator Operator Row (+, −, ×, ÷, =, ⌫)
                    CalculatorOperatorRow(
                        textFieldValue = amountInput,
                        onValueChange = { amountInput = it }
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // Who Paid Selector
            item {
                BillPayerSelector(
                    friends = friends,
                    payerFriendId = payerFriendId,
                    onSelectPayer = { payerFriendId = it }
                )
                Spacer(modifier = Modifier.height(20.dp))
            }

            // Split Mode Selector (Equal vs Custom)
            item {
                BillSplitModeSelector(
                    isCustomSplit = isCustomSplit,
                    onSelectMode = { isCustomSplit = it }
                )
                Spacer(modifier = Modifier.height(22.dp))
            }

            // Split with Whom Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Split Among Squad 👥",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "You are included",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Custom Split: "You" Share Field
            if (isCustomSplit) {
                item {
                    ClayCard(
                        modifier = Modifier.fillMaxWidth(),
                        shapeRadius = 16.dp,
                        elevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = "👑", style = MaterialTheme.typography.labelLarge)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Your Share",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Included automatically",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = customShareInputs["user"] ?: "",
                                onValueChange = { str ->
                                    if (str.all { it.isDigit() || it == '.' }) {
                                        customShareInputs["user"] = str
                                    }
                                },
                                prefix = { Text("₹", fontWeight = FontWeight.Bold) },
                                placeholder = { Text("0") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.width(120.dp),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }

            // Friends Checklist / Custom Shares Items
            if (friends.isEmpty()) {
                item {
                    ClayCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "No friends summoned yet! Add friends from the Squad tab to split bills.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(
                    items = friends,
                    key = { it.id },
                    contentType = { "split_friend" }
                ) { friend ->
                    val isChecked = selectedFriendIds.contains(friend.id)
                    ParticipantFriendSplitCard(
                        friend = friend,
                        isChecked = isChecked,
                        isCustomSplit = isCustomSplit,
                        customShareValue = customShareInputs[friend.id] ?: "",
                        onToggleCheck = { checked ->
                            if (checked) {
                                selectedFriendIds.add(friend.id)
                                if (isCustomSplit && !customShareInputs.containsKey(friend.id)) {
                                    customShareInputs[friend.id] = (perPersonPaise / 100).toString()
                                }
                            } else {
                                selectedFriendIds.remove(friend.id)
                                customShareInputs.remove(friend.id)
                            }
                        },
                        onCustomShareChange = { str -> customShareInputs[friend.id] = str }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // Calculation Preview Bento (Equal vs Custom)
            if (parsedTotalPaise > 0 && selectedFriendIds.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(14.dp))
                    if (!isCustomSplit) {
                        ClayCard(
                            modifier = Modifier.fillMaxWidth(),
                            shapeRadius = 18.dp,
                            elevation = 4.dp
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "FAIR SHARE ($totalParticipants participants)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        letterSpacing = 1.1.sp
                                    )
                                    Text(
                                        text = "${MoneyUtils.formatPaiseToInr(perPersonPaise)} / person",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(text = "⚔️✨", style = MaterialTheme.typography.titleLarge)
                            }
                        }
                    } else {
                        CustomShareAllocationCard(
                            parsedTotalPaise = parsedTotalPaise,
                            totalAllocatedPaise = totalAllocatedPaise
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // Split Confirmation Action Button
            item {
                val isCustomValid = !isCustomSplit || (totalAllocatedPaise == parsedTotalPaise)
                val canConfirm = titleInput.isNotBlank() &&
                        parsedTotalPaise > 0 &&
                        selectedFriendIds.isNotEmpty() &&
                        isCustomValid

                ClayButton(
                    text = if (isCustomSplit && totalAllocatedPaise != parsedTotalPaise) "Match Total to Split ⚔️" else "Cast Split Bill ⚔️",
                    enabled = canConfirm,
                    onClick = {
                        val customSharesMap: Map<String, Long>? = if (isCustomSplit) {
                            val map = mutableMapOf<String, Long>()
                            map["user"] = MoneyUtils.parseToPaise(customShareInputs["user"] ?: "") ?: 0L
                            selectedFriendIds.forEach { fId ->
                                map[fId] = MoneyUtils.parseToPaise(customShareInputs[fId] ?: "") ?: 0L
                            }
                            map
                        } else null

                        onSplitConfirmed(
                            titleInput.trim(),
                            parsedTotalPaise,
                            payerFriendId,
                            selectedFriendIds.toList(),
                            customSharesMap
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
