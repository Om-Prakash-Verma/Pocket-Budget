package com.ghostpocket.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ghostpocket.app.model.FriendBalance
import com.ghostpocket.app.model.MoneyUtils

/**
 * BentoFriendRow displays a friend card in the Ghost Squad list.
 * Highlights the friend's net balance:
 * - Green badge if they owe money ("You will get")
 * - Red badge if the user owes them ("You will give")
 * - Neutral indicator if completely settled.
 *
 * @param balance The FriendBalance data class containing friend info and net balance in paise.
 * @param onClick Invoked when the user taps on this friend card to view their detail ledger.
 * @param modifier Optional modifier for external layout adjustments.
 */
@Composable
fun BentoFriendRow(
    balance: FriendBalance,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val net = balance.netPaise
    val (label, labelColor) = remember(net) {
        when {
            net > 0 -> Pair("${MoneyUtils.formatPaiseToInr(net)} · You will get 🟢", Color(0xFF10B981))
            net < 0 -> Pair("${MoneyUtils.formatPaiseToInr(kotlin.math.abs(net))} · You will give 🔴", Color(0xFFEF4444))
            else -> Pair("Settled ✓ (₹0)", Color.Unspecified)
        }
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
                        fontWeight = if (net != 0L) FontWeight.Bold else FontWeight.Normal,
                        color = if (labelColor != Color.Unspecified) labelColor else MaterialTheme.colorScheme.onSurfaceVariant
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
