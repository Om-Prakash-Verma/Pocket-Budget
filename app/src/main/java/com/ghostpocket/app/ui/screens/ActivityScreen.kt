package com.ghostpocket.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ghostpocket.app.model.TransactionEntity
import com.ghostpocket.app.model.TransactionType
import com.ghostpocket.app.ui.components.ClayCard
import com.ghostpocket.app.ui.components.EmptyStateView
import com.ghostpocket.app.ui.components.GhostMood

@Composable
fun ActivityScreen(
    transactions: List<TransactionEntity>,
    onTransactionClick: (TransactionEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilterType by remember { mutableStateOf<TransactionType?>(null) }
    var sortByAmount by remember { mutableStateOf(false) }

    val filteredList = remember(transactions, searchQuery, selectedFilterType, sortByAmount) {
        transactions.filter { tx ->
            val matchesQuery = searchQuery.isBlank() ||
                    tx.category.contains(searchQuery, ignoreCase = true) ||
                    (tx.note?.contains(searchQuery, ignoreCase = true) == true)
            val matchesType = selectedFilterType == null || tx.type == selectedFilterType
            matchesQuery && matchesType
        }.let { list ->
            if (sortByAmount) {
                list.sortedByDescending { it.amountPaise }
            } else {
                list.sortedByDescending { it.timestamp }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp)
            .padding(top = 20.dp)
    ) {
        // Screen Title
        Text(
            text = "Activity Log 📜",
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Search Bar with Clay feel
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search spells, chai, snacks...") },
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
                ClayFilterPill(
                    label = "All Spells",
                    isSelected = selectedFilterType == null,
                    onClick = { selectedFilterType = null }
                )
            }
            item {
                ClayFilterPill(
                    label = "Vanished 💸",
                    isSelected = selectedFilterType == TransactionType.EXPENSE,
                    onClick = { selectedFilterType = TransactionType.EXPENSE }
                )
            }
            item {
                ClayFilterPill(
                    label = "Summoned 🪙",
                    isSelected = selectedFilterType == TransactionType.INCOME,
                    onClick = { selectedFilterType = TransactionType.INCOME }
                )
            }
            item {
                ClayFilterPill(
                    label = if (sortByAmount) "Heaviest Loot 💰" else "Latest First ⏱️",
                    isSelected = sortByAmount,
                    onClick = { sortByAmount = !sortByAmount }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Transaction list
        if (filteredList.isEmpty()) {
            ClayCard(
                modifier = Modifier.fillMaxWidth(),
                shapeRadius = 22.dp
            ) {
                EmptyStateView(
                    message = "No spells match your incantation! 🧙‍♂️",
                    mood = GhostMood.SPOOKY_SPOOKED
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredList) { tx ->
                    BentoTransactionItem(
                        transaction = tx,
                        onClick = { onTransactionClick(tx) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ClayFilterPill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
