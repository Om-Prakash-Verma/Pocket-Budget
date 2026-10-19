package com.ghostpocket.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp),
        contentPadding = PaddingValues(top = 20.dp, bottom = 100.dp)
    ) {
        item {
            Text(
                text = "Spooky Insights 🔮",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Where did all the coins vanish?",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(18.dp))
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
