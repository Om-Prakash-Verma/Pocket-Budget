package com.ghostpocket.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * ============================================================================
 * QuickAmountChipsRow
 * ============================================================================
 * Horizontal row of quick amount increment pills (+₹10, +₹20, +₹50, +₹100, +₹500).
 *
 * Behavior:
 * - Tapping a chip parses the current amount, adds the increment in Rupees,
 *   and places the cursor at the end of the new number.
 *
 * @param onPresetClick Invoked with the integer rupee increment (e.g. 10, 20, 50).
 * @param modifier Composable modifier.
 */
@Composable
fun QuickAmountChipsRow(
    onPresetClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        listOf(10, 20, 50, 100, 500).forEach { preset ->
            val chipShape = RoundedCornerShape(10.dp)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(chipShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
                    .clickable { onPresetClick(preset) }
                    .padding(vertical = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+₹$preset",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
