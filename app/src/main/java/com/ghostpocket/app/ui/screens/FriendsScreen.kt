package com.ghostpocket.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ghostpocket.app.model.FriendBalance
import com.ghostpocket.app.ui.components.*

/**
 * FriendsScreen provides the primary hub for managing shared debts and group finances:
 * - Summary Bento card detailing net receivable vs payable balances
 * - Real-time fuzzy search by friend's name or phone number
 * - Filter pills for "All", "Owes You", "You Owe", and "Settled"
 * - One-tap sorting by Highest Debt vs Alphabetical (A-Z)
 * - Quick launch actions for Bill Splitting and Friend registration
 *
 * @param friends List of friends with calculated balances.
 * @param onAddFriendClick Triggered to summon a new friend to the squad.
 * @param onSplitBillClick Triggered to open the bill-splitting flow.
 * @param onFriendClick Triggered when a friend card is tapped.
 * @param onSettingsClick Triggered to open app settings.
 * @param onToggleTheme Triggered to toggle between dark and light themes.
 * @param isDarkTheme Whether dark theme is currently enabled.
 * @param modifier Optional layout modifier.
 */
@Composable
fun FriendsScreen(
    friends: List<FriendBalance>,
    onAddFriendClick: () -> Unit,
    onSplitBillClick: () -> Unit,
    onFriendClick: (FriendBalance) -> Unit,
    onSettingsClick: () -> Unit = {},
    onToggleTheme: () -> Unit = {},
    isDarkTheme: Boolean = true,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") } // "ALL", "OWES_YOU", "YOU_OWE", "SETTLED"
    var sortOption by remember { mutableStateOf("DEBT") } // "DEBT", "NAME"

    // Net balance calculations across entire squad
    val totalOwedToUser = remember(friends) {
        friends.filter { it.netPaise > 0 }.sumOf { it.netPaise }
    }
    val totalUserOwes = remember(friends) {
        friends.filter { it.netPaise < 0 }.sumOf { kotlin.math.abs(it.netPaise) }
    }

    // Filter and sort squad members dynamically
    val filteredFriends = remember(friends, searchQuery, selectedFilter, sortOption) {
        friends.filter { item ->
            val matchesQuery = searchQuery.isBlank() ||
                    item.friend.name.contains(searchQuery, ignoreCase = true) ||
                    (item.friend.phone?.contains(searchQuery, ignoreCase = true) == true)

            val matchesFilter = when (selectedFilter) {
                "OWES_YOU" -> item.netPaise > 0L
                "YOU_OWE" -> item.netPaise < 0L
                "SETTLED" -> item.netPaise == 0L
                else -> true
            }

            matchesQuery && matchesFilter
        }.let { list ->
            when (sortOption) {
                "NAME" -> list.sortedBy { it.friend.name.lowercase() }
                else -> list.sortedByDescending { kotlin.math.abs(it.netPaise) }
            }
        }
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
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ClayButton(
                            text = "Split ⚔️",
                            icon = Icons.Default.CallSplit,
                            isSecondary = true,
                            onClick = onSplitBillClick
                        )

                        ClayIconButton(
                            icon = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                            onClick = onToggleTheme,
                            contentDescription = if (isDarkTheme) "Switch to Light Theme" else "Switch to Dark Theme",
                            size = 46.dp,
                            shapeRadius = 15.dp
                        )

                        ClayIconButton(
                            icon = Icons.Default.Settings,
                            onClick = onSettingsClick,
                            contentDescription = "Settings",
                            size = 46.dp,
                            shapeRadius = 15.dp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
            }

            // Shared Ledger Overview Card
            item {
                FriendsOverviewCard(
                    totalOwedToUser = totalOwedToUser,
                    totalUserOwes = totalUserOwes
                )

                Spacer(modifier = Modifier.height(18.dp))
            }

            // Search Bar & Filter Pills
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search mortal by name or number...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.onBackground,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Filter / Sort Bento Pills
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        ClaySquadFilterPill(
                            label = "All Squad (${friends.size})",
                            isSelected = selectedFilter == "ALL",
                            onClick = { selectedFilter = "ALL" }
                        )
                    }
                    item {
                        ClaySquadFilterPill(
                            label = "Owes You 🪙",
                            isSelected = selectedFilter == "OWES_YOU",
                            onClick = { selectedFilter = "OWES_YOU" }
                        )
                    }
                    item {
                        ClaySquadFilterPill(
                            label = "You Owe 💸",
                            isSelected = selectedFilter == "YOU_OWE",
                            onClick = { selectedFilter = "YOU_OWE" }
                        )
                    }
                    item {
                        ClaySquadFilterPill(
                            label = "Settled ✨",
                            isSelected = selectedFilter == "SETTLED",
                            onClick = { selectedFilter = "SETTLED" }
                        )
                    }
                    item {
                        ClaySquadFilterPill(
                            label = if (sortOption == "NAME") "Sort: A-Z 🔤" else "Sort: Highest Debt 💰",
                            isSelected = true,
                            onClick = {
                                sortOption = if (sortOption == "DEBT") "NAME" else "DEBT"
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
            }

            // Friends Bento List
            if (filteredFriends.isEmpty()) {
                item {
                    ClayCard(
                        modifier = Modifier.fillMaxWidth(),
                        shapeRadius = 22.dp
                    ) {
                        EmptyStateView(
                            message = if (searchQuery.isNotBlank() || selectedFilter != "ALL") {
                                "No mortals match your search incantation! 🧙‍♂️"
                            } else {
                                "No mortals summoned yet! Add a friend to start tracking debts 👻"
                            },
                            mood = GhostMood.WAVING
                        )
                    }
                }
            } else {
                items(
                    items = filteredFriends,
                    key = { it.friend.id },
                    contentType = { "friend_balance" }
                ) { balance ->
                    BentoFriendRow(
                        balance = balance,
                        onClick = { onFriendClick(balance) }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }

        // Add Friend Floating Action Button
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
