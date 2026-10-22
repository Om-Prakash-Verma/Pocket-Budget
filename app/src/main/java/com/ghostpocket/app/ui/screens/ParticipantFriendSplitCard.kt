package com.ghostpocket.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ghostpocket.app.model.FriendEntity
import com.ghostpocket.app.ui.components.ClayCard

/**
 * ============================================================================
 * ParticipantFriendSplitCard
 * ============================================================================
 * Card representing a squad member in the Bill Splitting squad list.
 *
 * Behavior:
 * - Equal Split Mode: Displays friend name, avatar, and toggle checkbox.
 * - Custom Shares Mode: Displays friend name, avatar, toggle checkbox, and a dedicated
 *   share input field in currency (₹).
 *
 * @param friend The friend data model.
 * @param isChecked Whether this friend is included in the split.
 * @param isCustomSplit Whether custom shares mode is active.
 * @param customShareValue Current custom amount text entered for this friend.
 * @param onToggleCheck Invoked when the friend's inclusion is toggled.
 * @param onCustomShareChange Invoked when custom share amount changes.
 * @param modifier Composable modifier.
 */
@Composable
fun ParticipantFriendSplitCard(
    friend: FriendEntity,
    isChecked: Boolean,
    isCustomSplit: Boolean,
    customShareValue: String,
    onToggleCheck: (Boolean) -> Unit,
    onCustomShareChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    ClayCard(
        modifier = modifier.fillMaxWidth(),
        shapeRadius = 16.dp,
        elevation = 2.dp,
        onClick = { onToggleCheck(!isChecked) }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Friend Avatar & Name
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = friend.name.take(1).uppercase(),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = friend.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Custom Share Input (Only when included in custom split mode)
            if (isCustomSplit && isChecked) {
                OutlinedTextField(
                    value = customShareValue,
                    onValueChange = { str ->
                        if (str.all { it.isDigit() || it == '.' }) {
                            onCustomShareChange(str)
                        }
                    },
                    prefix = { Text("₹", fontWeight = FontWeight.Bold) },
                    placeholder = { Text("0") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .width(120.dp)
                        .padding(end = 6.dp),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // Inclusion Checkbox
            Checkbox(
                checked = isChecked,
                onCheckedChange = onToggleCheck
            )
        }
    }
}
