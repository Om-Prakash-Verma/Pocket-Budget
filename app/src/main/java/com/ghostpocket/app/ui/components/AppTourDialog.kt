package com.ghostpocket.app.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.window.DialogProperties

data class TourStep(
    val icon: String,
    val title: String,
    val subtitle: String,
    val highlights: List<String>,
    val badge: String
)

@Composable
fun AppTourDialog(
    onDismiss: () -> Unit,
    onComplete: () -> Unit
) {
    val steps = remember {
        listOf(
            TourStep(
                icon = "👻",
                title = "Pure Liquid Cash",
                subtitle = "Real Net Balance with Zero Ghost Money",
                highlights = listOf(
                    "Net Balance strictly equals All Income − All Expenses.",
                    "No fake budgets: Add pocket money/allowance directly via '+ Income'.",
                    "Daily Safe-to-Spend calculates automatically from your real cash in hand."
                ),
                badge = "CASHFLOW"
            ),
            TourStep(
                icon = "🧮",
                title = "In-Line Calculator",
                subtitle = "Math Expressions Everywhere",
                highlights = listOf(
                    "Type arithmetic directly into amount fields: '60+100', '250 - 45.50'.",
                    "Supports +, −, ×, ÷, decimals, and BODMAS precedence.",
                    "Quick clay operator pills with real-time '= ₹...' live previews."
                ),
                badge = "SMART INPUT"
            ),
            TourStep(
                icon = "🏷️",
                title = "Month-Locked Budgets",
                subtitle = "Historical Spending Protected",
                highlights = listOf(
                    "Each Realm (Category) budget belongs strictly to that calendar month.",
                    "Increasing your Chai budget in October preserves September's budget.",
                    "Calendar browsing starts from the month you created your account."
                ),
                badge = "BUDGETING"
            ),
            TourStep(
                icon = "🍕",
                title = "Friends: Gave & Got 🤝",
                subtitle = "Crystal-Clear Debt Tracking",
                highlights = listOf(
                    "Only two simple options: You Gave 🔴 (Red) and You Got 🟢 (Green).",
                    "Split bills with friends with accurate per-person division.",
                    "Recording Got immediately balances debts without confusion."
                ),
                badge = "FRIENDS"
            ),
            TourStep(
                icon = "☁️",
                title = "WhatsApp-Style Backup",
                subtitle = "100% Offline with Once-Daily Silent Sync",
                highlights = listOf(
                    "Operates 100% offline — opening, closing, or switching apps never makes background network calls.",
                    "Daily Silent Sync: Automatically backs up all changes in one single atomic batch once every 24 hours.",
                    "Manual 'Sync Now' in Settings lets you trigger an immediate backup anytime you wish."
                ),
                badge = "BACKUP & PRIVACY"
            )
        )
    }

    var currentStepIndex by remember { mutableStateOf(0) }
    val currentStep = steps[currentStepIndex]
    val isLastStep = currentStepIndex == steps.lastIndex

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.72f))
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            ClayCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                shapeRadius = 26.dp,
                elevation = 8.dp
            ) {
                // Header with Step Pill & Skip Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "${currentStep.badge} • ${currentStepIndex + 1}/${steps.size}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    TextButton(
                        onClick = onDismiss,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Skip Tour ✕",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Animated Content Body
                AnimatedContent(
                    targetState = currentStep,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(220)) togetherWith
                                fadeOut(animationSpec = tween(180))
                    },
                    label = "TourSlideTransition"
                ) { step ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Large Tactile Icon Circle
                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = step.icon,
                                fontSize = 38.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = step.title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = step.subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Highlights List
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            step.highlights.forEach { highlight ->
                                Row(
                                    verticalAlignment = Alignment.Top,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "✨",
                                        fontSize = 13.sp,
                                        modifier = Modifier.padding(top = 1.dp, end = 8.dp)
                                    )
                                    Text(
                                        text = highlight,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Progress Dots Indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    steps.indices.forEach { index ->
                        val isSelected = index == currentStepIndex
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .size(if (isSelected) 20.dp else 8.dp, 8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Bottom Action Buttons (Back & Next / Finish)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentStepIndex > 0) {
                        ClayButton(
                            text = "Back",
                            onClick = { currentStepIndex-- },
                            isSecondary = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    ClayButton(
                        text = if (isLastStep) "Start Exploring ✨" else "Next →",
                        onClick = {
                            if (isLastStep) {
                                onComplete()
                            } else {
                                currentStepIndex++
                            }
                        },
                        modifier = Modifier.weight(if (currentStepIndex > 0) 1.6f else 1f)
                    )
                }
            }
        }
    }
}
