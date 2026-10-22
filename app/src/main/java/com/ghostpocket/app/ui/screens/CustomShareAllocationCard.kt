package com.ghostpocket.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghostpocket.app.model.MoneyUtils
import com.ghostpocket.app.ui.components.ClayCard

/**
 * ============================================================================
 * CustomShareAllocationCard
 * ============================================================================
 * Visual verification indicator displaying whether total individual participant shares
 * match the total bill amount.
 *
 * Statuses:
 * - "✅ Matched": Sum of all shares equals total bill.
 * - "⚠️ Remaining": Gold still unallocated (indicates amount remaining).
 * - "🛑 Over": Sum of shares exceeds total bill.
 *
 * @param parsedTotalPaise Total bill amount in paise.
 * @param totalAllocatedPaise Sum of all allocated member shares in paise.
 * @param modifier Composable modifier.
 */
@Composable
fun CustomShareAllocationCard(
    parsedTotalPaise: Long,
    totalAllocatedPaise: Long,
    modifier: Modifier = Modifier
) {
    val diff = parsedTotalPaise - totalAllocatedPaise
    val isBalanced = diff == 0L

    ClayCard(
        modifier = modifier.fillMaxWidth(),
        shapeRadius = 18.dp,
        elevation = 4.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "CUSTOM ALLOCATION",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.1.sp
                    )
                    Text(
                        text = "${MoneyUtils.formatPaiseToInr(totalAllocatedPaise)} of ${MoneyUtils.formatPaiseToInr(parsedTotalPaise)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = if (isBalanced) "✅ Matched" else if (diff > 0) "⚠️ Remaining" else "🛑 Over",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isBalanced) Color(0xFF10B981) else if (diff > 0) Color(0xFFF59E0B) else Color(0xFFEF4444)
                )
            }
            if (!isBalanced) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (diff > 0) "${MoneyUtils.formatPaiseToInr(diff)} left to assign" else "${MoneyUtils.formatPaiseToInr(-diff)} over total bill",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (diff > 0) Color(0xFFF59E0B) else Color(0xFFEF4444)
                )
            }
        }
    }
}
