package com.ghostpocket.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghostpocket.app.model.FriendBalance
import com.ghostpocket.app.model.MoneyUtils
import com.ghostpocket.app.ui.components.*

@Composable
fun FriendsScreen(
    friends: List<FriendBalance>,
    onAddFriendClick: () -> Unit,
    onSplitBillClick: () -> Unit,
    onFriendClick: (FriendBalance) -> Unit,
    onSettingsClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val totalOwedToUser = remember(friends) {
        friends.filter { it.netPaise > 0 }.sumOf { it.netPaise }
    }
    val totalUserOwes = remember(friends) {
        friends.filter { it.netPaise < 0 }.sumOf { kotlin.math.abs(it.netPaise) }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 100.dp)
        ) {
            // Header with Split Bill Action and Settings
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Split & Settle 👥",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Ghost Squad",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ClayButton(
                            text = "Split ⚔️",
                            icon = Icons.Default.CallSplit,
                            isSecondary = true,
                            onClick = onSplitBillClick
                        )

                        ClayIconButton(
                            icon = Icons.Default.Settings,
                            onClick = onSettingsClick,
                            contentDescription = "Settings",
                            size = 48.dp,
                            shapeRadius = 16.dp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
            }

            // BENTO CELL: Shared Ledger Overview
            item {
                ClayCard(
                    modifier = Modifier.fillMaxWidth(),
                    shapeRadius = 24.dp,
                    elevation = 6.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "THEY OWE YOU 📜",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 1.1.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = MoneyUtils.formatPaiseToInr(totalOwedToUser),
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "YOU OWE THEM 💸",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 1.1.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = MoneyUtils.formatPaiseToInr(totalUserOwes),
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // Friends Bento List
            if (friends.isEmpty()) {
                item {
                    ClayCard(
                        modifier = Modifier.fillMaxWidth(),
                        shapeRadius = 22.dp
                    ) {
                        EmptyStateView(
                            message = "No mortals summoned yet! Add a friend to start tracking debts 👻",
                            mood = GhostMood.WAVING
                        )
                    }
                }
            } else {
                items(friends) { balance ->
                    BentoFriendRow(
                        balance = balance,
                        onClick = { onFriendClick(balance) }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }

        // Add Friend Floating Clay Button
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(22.dp)
        ) {
            ClayButton(
                text = "+ Friend",
                icon = Icons.Default.Add,
                onClick = onAddFriendClick
            )
        }
    }
}

@Composable
fun BentoFriendRow(
    balance: FriendBalance,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val net = balance.netPaise
    val label = when {
        net > 0 -> "${MoneyUtils.formatPaiseToInr(net)} · Owes you 📜"
        net < 0 -> "${MoneyUtils.formatPaiseToInr(kotlin.math.abs(net))} · You owe 🪙"
        else -> "Settled ✓ (Peace restored)"
    }

    ClayCard(
        modifier = modifier.fillMaxWidth(),
        shapeRadius = 18.dp,
        elevation = 3.dp,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = balance.friend.name.take(1).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = balance.friend.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = "→",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
