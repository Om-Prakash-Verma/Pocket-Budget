package com.ghostpocket.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghostpocket.app.model.FriendLedgerEntity
import com.ghostpocket.app.model.LedgerDirection
import com.ghostpocket.app.model.MoneyUtils
import com.ghostpocket.app.ui.components.ClayCard
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * ============================================================================
 * BentoLedgerItem
 * ============================================================================
 * Displays a single transaction entry in a friend's ledger history.
 *
 * Visual Indicators:
 * - Red accent (🔴 / − ₹...) indicates money given to the friend ("You Gave").
 * - Green accent (🟢 / + ₹...) indicates money received from the friend ("You Got").
 *
 * @param entry The ledger item data model.
 * @param onClick Optional click handler to open the edit/delete dialog.
 * @param modifier Composable modifier for styling and layout constraints.
 */
@Composable
fun BentoLedgerItem(
    entry: FriendLedgerEntity,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // ------------------------------------------------------------------------
    // 1. Data & Formatting Precomputations
    // ------------------------------------------------------------------------
    val dateStr = remember(entry.timestamp) {
        SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(entry.timestamp))
    }
    val isGave = entry.direction == LedgerDirection.THEY_OWE_YOU
    val directionText = if (isGave) "You Gave" else "You Got"
    val accentColor = if (isGave) Color(0xFFEF4444) else Color(0xFF10B981)
    val badgeBg = if (isGave) Color(0xFFEF4444).copy(alpha = 0.14f) else Color(0xFF10B981).copy(alpha = 0.14f)
    val sign = if (isGave) "− " else "+ "

    // ------------------------------------------------------------------------
    // 2. Bento Clay Card Container
    // ------------------------------------------------------------------------
    ClayCard(
        modifier = modifier.fillMaxWidth(),
        shapeRadius = 18.dp,
        elevation = 2.dp,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Group: Direction Emoji, Title/Note, Direction Tag & Timestamp
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f, fill = false)
            ) {
                // Direction indicator bubble
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(badgeBg),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isGave) "🔴" else "🟢",
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = entry.note?.takeIf { it.isNotBlank() } ?: directionText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = badgeBg
                        ) {
                            Text(
                                text = directionText,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = accentColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "· $dateStr",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Right Group: Signed & Formatted Amount in Currency
            Text(
                text = "$sign${MoneyUtils.formatPaiseToInr(entry.amountPaise)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = accentColor
            )
        }
    }
}
