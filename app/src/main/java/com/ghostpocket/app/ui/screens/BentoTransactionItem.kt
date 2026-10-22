package com.ghostpocket.app.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ghostpocket.app.model.MoneyUtils
import com.ghostpocket.app.model.TransactionEntity
import com.ghostpocket.app.model.TransactionType
import com.ghostpocket.app.ui.components.CategoryIcon
import com.ghostpocket.app.ui.components.ClayCard
import com.ghostpocket.app.ui.components.SyncStatusChip

/**
 * ============================================================================
 * BentoTransactionItem
 * ============================================================================
 * Compact transaction item representation used in recent activity and dashboard screens.
 *
 * Displays:
 * - Category icon and name.
 * - Transaction note / subtitle.
 * - Formatted transaction amount (+ for income, - for expense).
 * - Cloud synchronization badge ([SyncStatusChip]).
 *
 * @param transaction Transaction entity data.
 * @param onClick Invoked when the transaction item is clicked.
 * @param modifier Composable modifier.
 */
@Composable
fun BentoTransactionItem(
    transaction: TransactionEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isExpense = transaction.type == TransactionType.EXPENSE
    val displayAmount = remember(transaction.amountPaise, isExpense) {
        val formattedAmount = MoneyUtils.formatPaiseToInr(transaction.amountPaise)
        if (isExpense) "-$formattedAmount" else "+$formattedAmount"
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
            // Category Icon & Note
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

            // Amount & Cloud Sync Status Badge
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
