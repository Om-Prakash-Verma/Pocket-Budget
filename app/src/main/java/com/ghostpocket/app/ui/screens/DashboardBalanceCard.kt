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
 * - True Liquid Net Balance (All Income - All Expenses).
 * - Share Month Summary button.
 * - Dynamic witty status gauge.
 * - Context-aware Smart Allowance indicator:
 *     - Current Ongoing Month: Real-time "Safe to Spend (₹X / day)".
 *     - Completed Past Month: Historical Net Saved or Overspent indicator.
 *     - Future Month: Upcoming Month placeholder.
 *
 * @param summary Monthly financial totals model.
 * @param selectedMonth Active month descriptor.
 * @param funnyStatus Dynamic witty status message based on remaining cash.
 * @param dailySafePaise Daily allowance in paise for the current month.
 * @param daysRemaining Number of days remaining in the active month.
 * @param modifier Composable modifier.
 */
@Composable
fun DashboardBalanceCard(
    summary: MonthlySummary,
    selectedMonth: SelectedMonth,
    funnyStatus: String,
    dailySafePaise: Long?,
    daysRemaining: Int,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentMonth = SelectedMonth.current()
    val isCurrent = selectedMonth == currentMonth
    val isPast = selectedMonth < currentMonth

    ClayCard(
        modifier = modifier.fillMaxWidth(),
        shapeRadius = 26.dp,
        elevation = 8.dp
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
                                (if (isCurrent && dailySafePaise != null && dailySafePaise > 0L) "⏱️ *Safe to spend:* ${MoneyUtils.formatPaiseToInr(dailySafePaise)}/day ($daysRemaining days left)\n" else "") +
                                (if (isPast) "📊 *Month Net:* ${MoneyUtils.formatPaiseToInr(summary.monthBalancePaise)}\n" else "") +
                                "\n_Tracked with GhostPocket ✨_"
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, shareText)
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Share ${selectedMonth.label} Summary"))
                    }
                    .padding(horizontal = 12.dp, vertical = 7.dp)
            ) {
                Text(
                    text = "Share 📤",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Section: Spooky / Witty Status Gauge Banner
        Text(
            text = funnyStatus,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Section: Smart Allowance Indicator
        when {
            isCurrent -> {
                // Ongoing Month: Safe-to-Spend Allowance
                val hasPositiveBalance = summary.balancePaise > 0L
                val pillBgColor = if (hasPositiveBalance) Color(0xFF10B981).copy(alpha = 0.12f) else Color(0xFFEF4444).copy(alpha = 0.12f)
                val pillTextColor = if (hasPositiveBalance) Color(0xFF10B981) else Color(0xFFEF4444)

                Surface(
                    color = pillBgColor,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = if (hasPositiveBalance) "🎯" else "⚠️", fontSize = 15.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (hasPositiveBalance) "Safe to spend ($daysRemaining days left)" else "Wallet empty / overspent",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = pillTextColor
                            )
                        }
                        Text(
                            text = if (hasPositiveBalance) "${MoneyUtils.formatPaiseToInr(dailySafePaise ?: 0L)} / day" else "₹0 / day",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = pillTextColor
                        )
                    }
                }
            }
            isPast -> {
                // Completed Month: Net Savings / Deficit
                val isSaved = summary.monthBalancePaise >= 0L
                val pillBgColor = if (isSaved) Color(0xFF10B981).copy(alpha = 0.12f) else Color(0xFFEF4444).copy(alpha = 0.12f)
                val pillTextColor = if (isSaved) Color(0xFF10B981) else Color(0xFFEF4444)

                Surface(
                    color = pillBgColor,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = if (isSaved) "✨" else "⚠️", fontSize = 15.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isSaved) "Month Settled • Net Saved" else "Month Settled • Overspent",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = pillTextColor
                            )
                        }
                        Text(
                            text = MoneyUtils.formatPaiseToInr(kotlin.math.abs(summary.monthBalancePaise)),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = pillTextColor
                        )
                    }
                }
            }
            else -> {
                // Future Month Placeholder
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "🗓️", fontSize = 15.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Upcoming Month • No transactions yet",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
