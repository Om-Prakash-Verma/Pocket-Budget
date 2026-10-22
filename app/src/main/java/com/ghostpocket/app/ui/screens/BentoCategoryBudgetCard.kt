package com.ghostpocket.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ghostpocket.app.model.CategorySpend
import com.ghostpocket.app.model.MoneyUtils
import com.ghostpocket.app.ui.components.CategoryIcon
import com.ghostpocket.app.ui.components.ClayCard

/**
 * ============================================================================
 * BentoCategoryBudgetCard
 * ============================================================================
 * Card displaying spending and budget progress for an individual realm (category).
 *
 * Visual Features:
 * - Realm icon in a circular clay container.
 * - Current spent amount vs allocated budget.
 * - Dynamic progress bar tracking spending percentage against budget.
 * - Status tags: "On track 👍", "Over budget! 😱", or "No budget set".
 * - Clicking triggers [onClick] (used for 1-tap budget editing).
 *
 * @param spend Category spending data model.
 * @param onClick Optional click handler to open the quick budget editor.
 * @param modifier Composable modifier.
 */
@Composable
fun BentoCategoryBudgetCard(
    spend: CategorySpend,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // ------------------------------------------------------------------------
    // 1. Spending & Budget Computations
    // ------------------------------------------------------------------------
    val allocated = spend.allocatedBudgetPaise
    val spent = spend.totalPaise
    val remaining = allocated - spent
    val hasBudget = allocated > 0L
    val isOverBudget = hasBudget && remaining < 0L

    val fraction = remember(hasBudget, spent, allocated, spend.percentage) {
        if (hasBudget) {
            (spent.toFloat() / allocated.toFloat()).coerceIn(0f, 1f)
        } else {
            spend.percentage.coerceIn(0f, 1f)
        }
    }

    val progressShape = remember { RoundedCornerShape(4.dp) }
    val formattedSpent = remember(spent) { MoneyUtils.formatPaiseToInr(spent) }
    val formattedBudget = remember(allocated) { "Budget: ${MoneyUtils.formatPaiseToInr(allocated)}" }
    val remainingText = remember(hasBudget, remaining, spend.percentage) {
        if (hasBudget) {
            if (remaining >= 0L) "Left: ${MoneyUtils.formatPaiseToInr(remaining)}" else "-${MoneyUtils.formatPaiseToInr(kotlin.math.abs(remaining))} ⚠️"
        } else {
            "${(spend.percentage * 100).toInt()}% of total expenses"
        }
    }

    // ------------------------------------------------------------------------
    // 2. Bento Card Layout
    // ------------------------------------------------------------------------
    ClayCard(
        modifier = modifier.fillMaxWidth(),
        shapeRadius = 20.dp,
        elevation = 3.dp,
        onClick = onClick
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Section: Category Title, Icon & Spent Details
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

                // Spent amount display
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formattedSpent,
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

            // Section: Visual Spending Progress Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(progressShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = fraction.coerceIn(0.02f, 1f))
                        .fillMaxHeight()
                        .clip(progressShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Section: Budget Allowance & Remaining Indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (hasBudget) formattedBudget else "Uncapped",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = remainingText,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
