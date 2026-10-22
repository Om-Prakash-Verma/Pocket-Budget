package com.ghostpocket.app.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghostpocket.app.model.MoneyUtils
import com.ghostpocket.app.model.MonthlySummary
import com.ghostpocket.app.model.SelectedMonth
import com.ghostpocket.app.ui.components.ClayCard

/**
 * ============================================================================
 * DashboardBalanceCard
 * ============================================================================
 * Prominent Bento card on the Dashboard displaying:
 * - Net balance for the selected month.
 * - Share Month Summary button (formats WhatsApp / ShareSheet summary).
 * - Edit / Set Monthly Budget pill.
 * - Spooky status gauge description.
 * - Monthly budget progress bar and remaining budget tracker.
 * - Daily "Safe to Spend" allowance indicator with real-time green/red state.
 *
 * @param summary Monthly financial totals model.
 * @param selectedMonth Active month descriptor.
 * @param funnyStatus Dynamic witty status message based on remaining budget.
 * @param dailySafePaise Daily allowance in paise.
 * @param daysRemaining Number of days remaining in the month.
 * @param onSetBudgetClick Invoked when the budget button is tapped.
 * @param modifier Composable modifier.
 */
@Composable
fun DashboardBalanceCard(
    summary: MonthlySummary,
    selectedMonth: SelectedMonth,
    funnyStatus: String,
    dailySafePaise: Long?,
    daysRemaining: Int,
    onSetBudgetClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    ClayCard(
        modifier = modifier.fillMaxWidth(),
        shapeRadius = 26.dp,
        elevation = 8.dp,
        onClick = onSetBudgetClick
    ) {
        // Section: Top Row - Net Balance & Action Pills
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

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Share Month Summary Button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable {
                            val shareText = "👻 *GhostPocket • ${selectedMonth.label} Summary*\n" +
                                    "💰 *Net Balance:* ${MoneyUtils.formatPaiseToInr(summary.balancePaise)}\n" +
                                    "📈 *Income:* ${MoneyUtils.formatPaiseToInr(summary.totalIncomePaise)}\n" +
                                    "📉 *Spent:* ${MoneyUtils.formatPaiseToInr(summary.totalExpensePaise)}\n" +
                                    (if (summary.initialBudgetPaise > 0L) "🎯 *Budget Left:* ${MoneyUtils.formatPaiseToInr(summary.remainingBudgetPaise)}\n" else "") +
                                    (if (dailySafePaise != null && dailySafePaise > 0L) "⏱️ *Safe to spend:* ${MoneyUtils.formatPaiseToInr(dailySafePaise)}/day ($daysRemaining days left)\n" else "") +
                                    "\n_Tracked with GhostPocket ✨_"
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, shareText)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share ${selectedMonth.label} Summary"))
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Share 📤",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Budget Quick Action Pill
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
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Section: Spooky / Witty Status Gauge Banner
        Text(
            text = funnyStatus,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Section: Budget Progress Bar & Daily Safe-to-Spend Pill
        if (summary.initialBudgetPaise > 0L) {
            Spacer(modifier = Modifier.height(12.dp))
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

            // Daily Safe-to-Spend Status Pill
            Spacer(modifier = Modifier.height(10.dp))
            val isOverBudget = summary.remainingBudgetPaise < 0
            Surface(
                color = if (!isOverBudget) Color(0xFF10B981).copy(alpha = 0.12f) else Color(0xFFEF4444).copy(alpha = 0.12f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = if (!isOverBudget) "🎯" else "⚠️", fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (!isOverBudget) "Safe to spend ($daysRemaining days left)" else "Monthly allowance exceeded!",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (!isOverBudget) Color(0xFF10B981) else Color(0xFFEF4444)
                        )
                    }
                    Text(
                        text = if (!isOverBudget) "${MoneyUtils.formatPaiseToInr(dailySafePaise ?: 0L)} / day" else "₹0 / day",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (!isOverBudget) Color(0xFF10B981) else Color(0xFFEF4444)
                    )
                }
            }
        }
    }
}
