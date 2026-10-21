package com.ghostpocket.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.ghostpocket.app.model.SelectedMonth

@Composable
fun MonthPickerDialog(
    currentSelectedMonth: SelectedMonth,
    creationMonth: SelectedMonth,
    onMonthSelected: (SelectedMonth) -> Unit,
    onDismiss: () -> Unit
) {
    var displayYear by remember { mutableStateOf(currentSelectedMonth.year) }
    val monthNames = remember {
        listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    }

    val canGoPrevYear = displayYear > creationMonth.year

    Dialog(onDismissRequest = onDismiss) {
        ClayCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shapeRadius = 24.dp,
            elevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Text(
                    text = "Select Realm Month 📅",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Historical records start from ${creationMonth.label}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Year Switcher
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { if (canGoPrevYear) displayYear-- },
                        enabled = canGoPrevYear
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = "Previous Year",
                            tint = if (canGoPrevYear) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                        )
                    }

                    Text(
                        text = displayYear.toString(),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    IconButton(
                        onClick = { displayYear++ }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Next Year",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 12 Months Grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    itemsIndexed(monthNames) { index, name ->
                        val monthNum = index + 1
                        val candidateMonth = SelectedMonth(displayYear, monthNum)
                        val isEnabled = candidateMonth >= creationMonth
                        val isSelected = candidateMonth.year == currentSelectedMonth.year && candidateMonth.month == currentSelectedMonth.month
                        val isCurrent = candidateMonth.isCurrentMonth

                        val shape = RoundedCornerShape(12.dp)
                        val bgColor = when {
                            isSelected -> MaterialTheme.colorScheme.primary
                            isCurrent -> MaterialTheme.colorScheme.primaryContainer
                            isEnabled -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                        }

                        val textColor = when {
                            isSelected -> MaterialTheme.colorScheme.onPrimary
                            isCurrent -> MaterialTheme.colorScheme.onPrimaryContainer
                            isEnabled -> MaterialTheme.colorScheme.onSurface
                            else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                        }

                        Box(
                            modifier = Modifier
                                .height(44.dp)
                                .clip(shape)
                                .background(bgColor)
                                .then(
                                    if (isCurrent && !isSelected) {
                                        Modifier.border(1.dp, MaterialTheme.colorScheme.primary, shape)
                                    } else Modifier
                                )
                                .then(
                                    if (isEnabled) {
                                        Modifier.clickable {
                                            onMonthSelected(candidateMonth)
                                            onDismiss()
                                        }
                                    } else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected || isCurrent) FontWeight.Bold else FontWeight.Medium,
                                color = textColor,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Quick Action: Current Month & Cancel
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }

                    ClayButton(
                        text = "Current Month ⏱️",
                        onClick = {
                            onMonthSelected(SelectedMonth.current())
                            onDismiss()
                        },
                        isSecondary = true,
                        modifier = Modifier.weight(1.5f)
                    )
                }
            }
        }
    }
}
