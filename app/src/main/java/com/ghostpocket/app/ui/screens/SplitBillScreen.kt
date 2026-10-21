package com.ghostpocket.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SplitBillScreen(
    friends: List<FriendEntity>,
    onBack: () -> Unit,
    onSplitConfirmed: (title: String, totalPaise: Long, payerFriendId: String?, participantFriendIds: List<String>, customShares: Map<String, Long>?) -> Unit,
    modifier: Modifier = Modifier
) {
    var titleInput by remember { mutableStateOf("") }
    var amountInput by remember { mutableStateOf(TextFieldValue("")) }
    var payerFriendId by remember { mutableStateOf<String?>(null) } // null = You
    val selectedFriendIds = remember { mutableStateListOf<String>() }

    var isCustomSplit by remember { mutableStateOf(false) }
    // customShares: key "user" for currentUser, or friendId for friends
    val customShareInputs = remember { mutableStateMapOf<String, String>() }

    val parsedTotalPaise = MoneyUtils.parseToPaise(amountInput.text) ?: 0L
    val totalParticipants = selectedFriendIds.size + 1 // friends + current user
    val equalShares = remember(parsedTotalPaise, totalParticipants) {
        if (parsedTotalPaise > 0 && totalParticipants > 1) {
            MoneyUtils.splitEqually(parsedTotalPaise, totalParticipants)
        } else {
            emptyList()
        }
    }
    val perPersonPaise = equalShares.firstOrNull() ?: 0L

    // Helper to calculate total allocated paise in custom split mode
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

    // Auto-prefill custom share inputs when switching to custom mode if empty
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Split the Bill ⚔️") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
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

            // Total Amount Clay Bento Box
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

                    // Tactile Calculator Operator Row (+, −, ×, ÷, =, ⌫)
                    CalculatorOperatorRow(
                        textFieldValue = amountInput,
                        onValueChange = { amountInput = it }
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // Who Paid Selector
            item {
                Text(
                    text = "Who Paid the Gold? 🪙",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val isYouSelected = payerFriendId == null
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isYouSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { payerFriendId = null }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "You Paid 👑",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isYouSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (friends.isNotEmpty()) {
                        friends.take(2).forEach { f ->
                            val isFSelected = payerFriendId == f.id
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isFSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { payerFriendId = f.id }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = f.name,
                                    maxLines = 1,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isFSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // Split Mode Selector (Equal vs Custom)
            item {
                Text(
                    text = "Split Mode ⚖️",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (!isCustomSplit) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { isCustomSplit = false }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Equal Split ⚖️",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (!isCustomSplit) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isCustomSplit) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { isCustomSplit = true }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Custom Shares 🍕",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isCustomSplit) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
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
                                    Text(
                                        text = "👑",
                                        style = MaterialTheme.typography.labelLarge
                                    )
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
                    ClayCard(
                        modifier = Modifier.fillMaxWidth(),
                        shapeRadius = 16.dp,
                        elevation = 2.dp,
                        onClick = {
                            if (isChecked) {
                                selectedFriendIds.remove(friend.id)
                                customShareInputs.remove(friend.id)
                            } else {
                                selectedFriendIds.add(friend.id)
                                if (isCustomSplit && !customShareInputs.containsKey(friend.id)) {
                                    customShareInputs[friend.id] = (perPersonPaise / 100).toString()
                                }
                            }
                        }
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
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = friend.name.take(1).uppercase(),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = friend.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            if (isCustomSplit && isChecked) {
                                OutlinedTextField(
                                    value = customShareInputs[friend.id] ?: "",
                                    onValueChange = { str ->
                                        if (str.all { it.isDigit() || it == '.' }) {
                                            customShareInputs[friend.id] = str
                                        }
                                    },
                                    prefix = { Text("₹", fontWeight = FontWeight.Bold) },
                                    placeholder = { Text("0") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier
                                        .width(120.dp)
                                        .padding(end = 6.dp),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }

                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    if (checked) {
                                        selectedFriendIds.add(friend.id)
                                        if (isCustomSplit && !customShareInputs.containsKey(friend.id)) {
                                            customShareInputs[friend.id] = (perPersonPaise / 100).toString()
                                        }
                                    } else {
                                        selectedFriendIds.remove(friend.id)
                                        customShareInputs.remove(friend.id)
                                    }
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // Calculation Preview Bento
            if (parsedTotalPaise > 0 && selectedFriendIds.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(14.dp))
                    ClayCard(
                        modifier = Modifier.fillMaxWidth(),
                        shapeRadius = 18.dp,
                        elevation = 4.dp
                    ) {
                        if (!isCustomSplit) {
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

                                Text(
                                    text = "⚔️✨",
                                    style = MaterialTheme.typography.titleLarge
                                )
                            }
                        } else {
                            val diff = parsedTotalPaise - totalAllocatedPaise
                            val isBalanced = diff == 0L
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "CUSTOM ALLOCATION",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            letterSpacing = 1.1.sp
                                        )
                                        Text(
                                            text = "${MoneyUtils.formatPaiseToInr(totalAllocatedPaise)} of ${MoneyUtils.formatPaiseToInr(parsedTotalPaise)}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Text(
                                        text = if (isBalanced) "✅ Matched" else if (diff > 0) "⚠️ Remaining" else "🛑 Over",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isBalanced) Color(0xFF10B981) else if (diff > 0) Color(0xFFF59E0B) else Color(0xFFEF4444)
                                    )
                                }
                                if (!isBalanced) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (diff > 0) "${MoneyUtils.formatPaiseToInr(diff)} left to assign" else "${MoneyUtils.formatPaiseToInr(-diff)} over total bill",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (diff > 0) Color(0xFFF59E0B) else Color(0xFFEF4444)
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // Confirmation Action
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
