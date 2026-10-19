package com.ghostpocket.app.ui.screens

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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghostpocket.app.model.FriendEntity
import com.ghostpocket.app.model.MoneyUtils
import com.ghostpocket.app.ui.components.ClayButton
import com.ghostpocket.app.ui.components.ClayCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SplitBillScreen(
    friends: List<FriendEntity>,
    onBack: () -> Unit,
    onSplitConfirmed: (title: String, totalPaise: Long, payerFriendId: String?, participantFriendIds: List<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    var titleInput by remember { mutableStateOf("") }
    var amountInput by remember { mutableStateOf("") }
    var payerFriendId by remember { mutableStateOf<String?>(null) } // null = You
    val selectedFriendIds = remember { mutableStateListOf<String>() }

    val parsedTotalPaise = MoneyUtils.parseToPaise(amountInput) ?: 0L
    val totalParticipants = selectedFriendIds.size + 1 // friends + current user
    val shares = remember(parsedTotalPaise, totalParticipants) {
        if (parsedTotalPaise > 0 && totalParticipants > 1) {
            MoneyUtils.splitEqually(parsedTotalPaise, totalParticipants)
        } else {
            emptyList()
        }
    }
    val perPersonPaise = shares.firstOrNull() ?: 0L

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
                Spacer(modifier = Modifier.height(22.dp))
            }

            // Split with Whom
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
                items(friends) { friend ->
                    val isChecked = selectedFriendIds.contains(friend.id)
                    ClayCard(
                        modifier = Modifier.fillMaxWidth(),
                        shapeRadius = 16.dp,
                        elevation = 2.dp,
                        onClick = {
                            if (isChecked) selectedFriendIds.remove(friend.id)
                            else selectedFriendIds.add(friend.id)
                        }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
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

                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    if (checked) selectedFriendIds.add(friend.id)
                                    else selectedFriendIds.remove(friend.id)
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
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // Confirmation Action
            item {
                val canConfirm = titleInput.isNotBlank() &&
                        parsedTotalPaise > 0 &&
                        selectedFriendIds.isNotEmpty()

                ClayButton(
                    text = "Cast Split Bill ⚔️",
                    enabled = canConfirm,
                    onClick = {
                        onSplitConfirmed(
                            titleInput.trim(),
                            parsedTotalPaise,
                            payerFriendId,
                            selectedFriendIds.toList()
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
