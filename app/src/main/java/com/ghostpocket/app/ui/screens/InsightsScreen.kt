package com.ghostpocket.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ghostpocket.app.model.CategorySpend
import com.ghostpocket.app.model.MonthlySummary
import com.ghostpocket.app.model.MoneyUtils
import com.ghostpocket.app.model.TransactionEntity
import com.ghostpocket.app.model.TransactionType
import com.ghostpocket.app.ui.components.*

@Composable
fun InsightsScreen(
    summary: MonthlySummary,
    categorySpends: List<CategorySpend>,
    recentTransactions: List<TransactionEntity>,
    onSettingsClick: () -> Unit = {},
    onToggleTheme: () -> Unit = {},
    isDarkTheme: Boolean = true,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 100.dp)
    ) {
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
                        text = "Financial Alchemy 🔮",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Spooky Insights",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
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

        // BENTO CELL: Category Breakdown
        item {
            ClayCard(
                modifier = Modifier.fillMaxWidth(),
                shapeRadius = 24.dp,
                elevation = 6.dp
            ) {
                Text(
                    text = "Spending Realms 🏷️",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(14.dp))

                if (categorySpends.isEmpty()) {
                    EmptyStateView(
                        message = "The crystal ball is cloudy... Log transactions to see insights! 🔮",
                        mood = GhostMood.CALM
                    )
                } else {
                    MonochromeCategoryBars(spends = categorySpends)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // BENTO CELL: Spending Trend Sparkline
        item {
            ClayCard(
                modifier = Modifier.fillMaxWidth(),
                shapeRadius = 24.dp,
                elevation = 6.dp
            ) {
                Text(
                    text = "Vanishing Speed (Trend) 📉",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))

                val trendPoints = remember(recentTransactions) {
                    val expenses = recentTransactions.filter { it.type == TransactionType.EXPENSE }
                    if (expenses.isEmpty()) {
                        emptyList()
                    } else {
                        expenses.take(7).reversed().map { (it.amountPaise / 100).toFloat() }
                    }
                }

                if (trendPoints.size < 2) {
                    EmptyStateView(
                        message = "Need at least 2 expenses to plot a ghostly trendline 👻",
                        mood = GhostMood.THINKING
                    )
                } else {
                    MonochromeTrendChart(points = trendPoints)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // BENTO CELL: Heaviest Gold Drains (Largest Expenses)
        item {
            val largestExpenses = remember(recentTransactions) {
                recentTransactions
                    .filter { it.type == TransactionType.EXPENSE }
                    .sortedByDescending { it.amountPaise }
                    .take(3)
            }

            ClayCard(
                modifier = Modifier.fillMaxWidth(),
                shapeRadius = 24.dp,
                elevation = 6.dp
            ) {
                Text(
                    text = "Heaviest Gold Drains 🕳️",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(14.dp))

                if (largestExpenses.isEmpty()) {
                    Text(
                        text = "No gold drained yet! You're saving like a wise wizard 🧙‍♂️",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    largestExpenses.forEachIndexed { idx, tx ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${idx + 1}. ${tx.category}${if (!tx.note.isNullOrBlank()) " (${tx.note})" else ""}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = MoneyUtils.formatPaiseToInr(tx.amountPaise),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (idx < largestExpenses.size - 1) {
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                    }
                }
            }
        }
    }
}
