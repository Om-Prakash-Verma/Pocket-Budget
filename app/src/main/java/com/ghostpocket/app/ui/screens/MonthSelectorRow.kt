package com.ghostpocket.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghostpocket.app.model.SelectedMonth
import com.ghostpocket.app.ui.components.ClayCard

/**
 * ============================================================================
 * MonthSelectorRow
 * ============================================================================
 * Navigation bar allowing the user to step backwards and forwards through months,
 * or tap the center pill to trigger the [MonthPickerDialog].
 *
 * Boundary Rules:
 * - Users can only navigate back as far as their account creation month.
 * - [canSelectPreviousMonth] disables the back arrow when reaching the account boundary.
 *
 * @param selectedMonth Currently active month model.
 * @param canSelectPreviousMonth Flag controlling whether user can navigate further back.
 * @param onPreviousMonth Invoked when the user taps previous month.
 * @param onNextMonth Invoked when the user taps next month.
 * @param onPickMonth Invoked when the user taps the center month label.
 * @param modifier Composable modifier.
 */
@Composable
fun MonthSelectorRow(
    selectedMonth: SelectedMonth,
    canSelectPreviousMonth: Boolean,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onPickMonth: () -> Unit,
    modifier: Modifier = Modifier
) {
    ClayCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        shapeRadius = 18.dp,
        elevation = 2.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Previous Month Button
            IconButton(
                onClick = onPreviousMonth,
                enabled = canSelectPreviousMonth,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Previous Month",
                    tint = if (canSelectPreviousMonth) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                )
            }

            // Center Month Pill (Tappable for Quick Picker Dialog)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onPickMonth)
                    .padding(horizontal = 12.dp, vertical = 2.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = selectedMonth.label,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "📅", fontSize = 14.sp)
                }
                if (selectedMonth.isCurrentMonth) {
                    Text(
                        text = "Current Month • Tap to change",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = "Tap to pick month / jump",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Next Month Button
            IconButton(
                onClick = onNextMonth,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Next Month",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
