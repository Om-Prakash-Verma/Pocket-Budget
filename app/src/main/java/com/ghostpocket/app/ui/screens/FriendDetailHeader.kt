package com.ghostpocket.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghostpocket.app.model.FriendBalance
import com.ghostpocket.app.model.MoneyUtils
import com.ghostpocket.app.ui.components.ClayButton
import com.ghostpocket.app.ui.components.ClayCard

/**
 * ============================================================================
 * FriendDetailHeader
 * ============================================================================
 * Top summary card on the Friend Detail screen displaying:
 * - Net balance amount with positive (green) / negative (red) color coding.
 * - Breakdown summary: "You Gave 🔴" total vs "You Got 🟢" total.
 * - Primary action buttons for logging "You Gave 🔴" or "You Got 🟢".
 * - Secondary actions for WhatsApp reminder and settling all debts.
 *
 * @param balance The calculated friend balance summary.
 * @param onGaveClick Triggered when user taps "You Gave 🔴".
 * @param onGotClick Triggered when user taps "You Got 🟢".
 * @param onWhatsAppReminderClick Triggered when user requests a WhatsApp reminder.
 * @param onSettleAllClick Triggered when user wants to settle all debts.
 * @param modifier Composable modifier.
 */
@Composable
fun FriendDetailHeader(
    balance: FriendBalance,
    onGaveClick: () -> Unit,
    onGotClick: () -> Unit,
    onWhatsAppReminderClick: () -> Unit,
    onSettleAllClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // ------------------------------------------------------------------------
    // 1. Balance Computations & Text Styles
    // ------------------------------------------------------------------------
    val net = balance.netPaise
    val (label, amount, netColor) = when {
        net > 0 -> Triple("You will get (They owe you) 🪙", MoneyUtils.formatPaiseToInr(net), Color(0xFF10B981))
        net < 0 -> Triple("You will give (You owe them) 💸", MoneyUtils.formatPaiseToInr(kotlin.math.abs(net)), Color(0xFFEF4444))
        else -> Triple("Settled! No pending balance ✨", "₹0", MaterialTheme.colorScheme.onSurface)
    }

    // ------------------------------------------------------------------------
    // 2. Bento Clay Card Container
    // ------------------------------------------------------------------------
    ClayCard(
        modifier = modifier.fillMaxWidth(),
        shapeRadius = 24.dp,
        elevation = 6.dp
    ) {
        // Section: Header Label
        Text(
            text = "BALANCE WITH ${balance.friend.name.uppercase()} 📜",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 1.1.sp
        )
        Spacer(modifier = Modifier.height(4.dp))

        // Section: Big Balance Display
        Text(
            text = amount,
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
            color = netColor
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Section: Mini Summary Breakdown (Total Gave 🔴 vs Total Got 🟢)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "YOU GAVE 🔴",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFEF4444)
                )
                Text(
                    text = MoneyUtils.formatPaiseToInr(balance.theyOweYouPaise),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFEF4444)
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "YOU GOT 🟢",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF10B981)
                )
                Text(
                    text = MoneyUtils.formatPaiseToInr(balance.youOweThemPaise),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF10B981)
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Section: Primary Action Buttons (You Gave 🔴 & You Got 🟢)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ClayButton(
                text = "You Gave 🔴",
                onClick = onGaveClick,
                customColor = Color(0xFFEF4444),
                modifier = Modifier.weight(1f)
            )
            ClayButton(
                text = "You Got 🟢",
                onClick = onGotClick,
                customColor = Color(0xFF10B981),
                modifier = Modifier.weight(1f)
            )
        }

        // Section: WhatsApp Reminder (Only when friend owes user)
        if (net > 0L) {
            Spacer(modifier = Modifier.height(10.dp))
            ClayButton(
                text = "WhatsApp Reminder 💬🖼️",
                onClick = onWhatsAppReminderClick,
                isSecondary = false,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Section: Exorcise / Settle All Debts
        if (net != 0L) {
            Spacer(modifier = Modifier.height(10.dp))
            ClayButton(
                text = "Exorcise All Debts (Settle) ✓",
                onClick = onSettleAllClick,
                isSecondary = true,
                icon = Icons.Default.Check,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
