package com.ghostpocket.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghostpocket.app.model.CategorySpend
import com.ghostpocket.app.model.MonthlySummary
import com.ghostpocket.app.model.MoneyUtils
import com.ghostpocket.app.model.TransactionType
import com.ghostpocket.app.ui.components.*

@Composable
fun DashboardScreen(
    summary: MonthlySummary,
    categorySpends: List<CategorySpend>,
    onAddClick: () -> Unit,
    onSetBudgetClick: () -> Unit,
    onManageCategoriesClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Dynamic mood based on budget health!
    val ghostMood = remember(summary) {
        when {
            summary.initialBudgetPaise > 0 && summary.remainingBudgetPaise < 0 -> GhostMood.SPOOKY_SPOOKED
            summary.balancePaise > 500000 -> GhostMood.RICH_GHOST
            summary.totalExpensePaise > summary.totalIncomePaise && summary.totalIncomePaise > 0 -> GhostMood.THINKING
            else -> GhostMood.HAPPY
        }
    }

    val funnyStatus = remember(summary) {
        when {
            summary.initialBudgetPaise == 0L -> "Set a budget before the ghosts eat it all! 👻"
            summary.remainingBudgetPaise < 0 -> "You're haunting your bank account! 😱 Over budget!"
            summary.remainingBudgetPaise < (summary.initialBudgetPaise * 0.2) -> "Danger zone! Your pocket is weeping 📉"
            else -> "Looking spooky rich! Keep it safe 🪙✨"
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp),
            contentPadding = PaddingValues(top = 20.dp, bottom = 100.dp)
        ) {
            // Header: Spooky Greeting + Bento Mascot Pill
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Boo! 👋",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "GhostPocket",
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Mascot Bento Avatar
                    ClayCard(
                        shapeRadius = 24.dp,
                        elevation = 4.dp,
                        modifier = Modifier.size(60.dp),
                        onClick = onSetBudgetClick
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            GhostMascot(size = 46.dp, mood = ghostMood)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))
            }

            // BENTO CELL 1: Main Balance & Funny Spooky Gauge
            item {
                ClayCard(
                    modifier = Modifier.fillMaxWidth(),
                    shapeRadius = 26.dp,
                    elevation = 8.dp,
                    onClick = onSetBudgetClick
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column {
                            Text(
                                text = "NET BALANCE",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 1.2.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = MoneyUtils.formatPaiseToInr(summary.balancePaise),
                                style = MaterialTheme.typography.displayLarge,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        // Budget quick pill
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { onSetBudgetClick() }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = if (summary.initialBudgetPaise > 0L) "Edit Budget ✏️" else "+ Set Budget",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Funny status banner
                    Text(
                        text = funnyStatus,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (summary.initialBudgetPaise > 0L) {
                        Spacer(modifier = Modifier.height(12.dp))
                        // Clay Progress Bar
                        val fraction = (summary.totalExpensePaise.toFloat() / summary.initialBudgetPaise.toFloat()).coerceIn(0f, 1f)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fraction)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Budget: ${MoneyUtils.formatPaiseToInr(summary.initialBudgetPaise)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Left: ${MoneyUtils.formatPaiseToInr(summary.remainingBudgetPaise)}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // BENTO GRID ROW: Income & Spent Clay Cubes
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Income Bento
                    ClayCard(
                        modifier = Modifier.weight(1f),
                        shapeRadius = 22.dp,
                        elevation = 6.dp
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowDownward,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Fed 🪙",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = MoneyUtils.formatPaiseToInr(summary.totalIncomePaise),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Spent Bento
                    ClayCard(
                        modifier = Modifier.weight(1f),
                        shapeRadius = 22.dp,
                        elevation = 6.dp
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowUpward,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Vanished 💸",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = MoneyUtils.formatPaiseToInr(summary.totalExpensePaise),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // Category Budget Breakdown Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Category Realms 🏷️",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Allocated, spent & remaining gold",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { onManageCategoriesClick() }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Manage ⚙️",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
            }

            // Category Budget Cards or empty state
            if (categorySpends.isEmpty()) {
                item {
                    ClayCard(
                        modifier = Modifier.fillMaxWidth(),
                        shapeRadius = 22.dp
                    ) {
                        EmptyStateView(
                            message = "No categories or spells logged yet! Tap '+ Add' or create a category in settings 🏷️",
                            mood = GhostMood.THINKING
                        )
                    }
                }
            } else {
                items(categorySpends) { spend ->
                    BentoCategoryBudgetCard(spend = spend)
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }

        // Tactile Clay Floating Action Button
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(22.dp)
        ) {
            ClayButton(
                text = "+ Add",
                icon = Icons.Default.Add,
                onClick = onAddClick
            )
        }
    }
}

@Composable
fun BentoCategoryBudgetCard(
    spend: CategorySpend,
    modifier: Modifier = Modifier
) {
    val allocated = spend.allocatedBudgetPaise
    val spent = spend.totalPaise
    val remaining = allocated - spent
    val hasBudget = allocated > 0L
    val isOverBudget = hasBudget && remaining < 0L

    val fraction = if (hasBudget) {
        (spent.toFloat() / allocated.toFloat()).coerceIn(0f, 1f)
    } else {
        spend.percentage.coerceIn(0f, 1f)
    }

    ClayCard(
        modifier = modifier.fillMaxWidth(),
        shapeRadius = 20.dp,
        elevation = 3.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        CategoryIcon(
                            categoryName = spend.category,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = spend.category,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (hasBudget) {
                                if (isOverBudget) "Over budget! 😱" else "On track 👍"
                            } else {
                                "No budget set"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Spent display
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = MoneyUtils.formatPaiseToInr(spent),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Spent",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = fraction.coerceIn(0.02f, 1f))
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.primary)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Budget and Left details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (hasBudget) "Budget: ${MoneyUtils.formatPaiseToInr(allocated)}" else "Uncapped",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = if (hasBudget) {
                        if (remaining >= 0L) "Left: ${MoneyUtils.formatPaiseToInr(remaining)}" else "-${MoneyUtils.formatPaiseToInr(kotlin.math.abs(remaining))} ⚠️"
                    } else {
                        "${(spend.percentage * 100).toInt()}% of total expenses"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun BentoTransactionItem(
    transaction: com.ghostpocket.app.model.TransactionEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isExpense = transaction.type == TransactionType.EXPENSE
    val formattedAmount = MoneyUtils.formatPaiseToInr(transaction.amountPaise)
    val displayAmount = if (isExpense) "-$formattedAmount" else "+$formattedAmount"

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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    CategoryIcon(
                        categoryName = transaction.category,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = transaction.category,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (!transaction.note.isNullOrBlank()) transaction.note else if (isExpense) "Vanished" else "Summoned",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = displayAmount,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                SyncStatusChip(status = transaction.syncStatus)
            }
        }
    }
}
