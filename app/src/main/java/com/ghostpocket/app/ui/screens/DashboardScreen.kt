package com.ghostpocket.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ghostpocket.app.model.CategorySpend
import com.ghostpocket.app.model.MonthlySummary
import com.ghostpocket.app.model.SelectedMonth
import com.ghostpocket.app.ui.components.*

/**
 * ============================================================================
 * DashboardScreen
 * ============================================================================
 * The primary home dashboard screen of GhostPocket.
 *
 * Screen Components:
 * 1. Spooky Greeting header with dynamic ghost mascot mood & theme/settings quick toggles.
 * 2. [MonthSelectorRow] with account-boundary aware back/forward navigation and quick picker.
 * 3. [DashboardBalanceCard] displaying net balance, budget progress bar, daily "Safe to Spend"
 *    allowance, and 1-tap summary sharing to WhatsApp/ShareSheet.
 * 4. [DashboardInflowOutflowRow] displaying total income (Fed 🪙) and expenses (Vanished 💸).
 * 5. Category Realms list ([BentoCategoryBudgetCard]) with 1-tap budget adjustment ([QuickCategoryBudgetDialog]).
 * 6. Floating clay action button (+ Add) for logging new transactions.
 *
 * @param summary Aggregated financial totals for the selected month.
 * @param categorySpends Breakdown of spending by realm/category.
 * @param selectedMonth The active month being displayed.
 * @param creationMonth The earliest month the user has account history for.
 * @param canSelectPreviousMonth Flag indicating if earlier months exist.
 * @param onPreviousMonth Steps to the previous calendar month.
 * @param onNextMonth Steps to the next calendar month.
 * @param onCurrentMonth Jumps to the current real-world month.
 * @param onSelectMonth Jumps directly to a chosen month from the picker dialog.
 * @param onAddClick Opens the Add Transaction bottom sheet.
 * @param onSetBudgetClick Opens the main budget editor.
 * @param onSetCategoryBudget Updates a specific realm's monthly budget.
 * @param onManageCategoriesClick Navigates to the realm management screen.
 * @param onSettingsClick Opens app settings.
 * @param onToggleTheme Toggles dark/light theme mode.
 * @param isDarkTheme True if dark theme is currently active.
 * @param modifier Composable modifier.
 */
@Composable
fun DashboardScreen(
    summary: MonthlySummary,
    categorySpends: List<CategorySpend>,
    selectedMonth: SelectedMonth = SelectedMonth.current(),
    creationMonth: SelectedMonth = SelectedMonth.current(),
    canSelectPreviousMonth: Boolean = true,
    onPreviousMonth: () -> Unit = {},
    onNextMonth: () -> Unit = {},
    onCurrentMonth: () -> Unit = {},
    onSelectMonth: (SelectedMonth) -> Unit = {},
    onAddClick: () -> Unit,
    onSetBudgetClick: () -> Unit,
    onSetCategoryBudget: (category: String, budgetPaise: Long) -> Unit = { _, _ -> },
    onManageCategoriesClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onToggleTheme: () -> Unit = {},
    isDarkTheme: Boolean = true,
    modifier: Modifier = Modifier
) {
    // ------------------------------------------------------------------------
    // 1. Dialog & State Setup
    // ------------------------------------------------------------------------
    var showMonthPicker by remember { mutableStateOf(false) }
    var categoryForBudgetEdit by remember { mutableStateOf<CategorySpend?>(null) }

    // Calculate days remaining in the active month
    val daysRemaining = remember(selectedMonth) {
        val cal = java.util.Calendar.getInstance()
        if (selectedMonth.isCurrentMonth) {
            val totalDays = cal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
            val today = cal.get(java.util.Calendar.DAY_OF_MONTH)
            (totalDays - today + 1).coerceAtLeast(1)
        } else {
            val calMonth = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.YEAR, selectedMonth.year)
                set(java.util.Calendar.MONTH, selectedMonth.month - 1)
            }
            calMonth.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
        }
    }

    // Daily safe allowance calculation
    val dailySafePaise = remember(summary.remainingBudgetPaise, daysRemaining) {
        if (summary.initialBudgetPaise > 0L) {
            (summary.remainingBudgetPaise / daysRemaining).coerceAtLeast(0L)
        } else null
    }

    // Dynamic ghost mood based on financial status
    val ghostMood = remember(summary) {
        when {
            summary.initialBudgetPaise > 0 && summary.remainingBudgetPaise < 0 -> GhostMood.SPOOKY_SPOOKED
            summary.balancePaise > 500000 -> GhostMood.RICH_GHOST
            summary.totalExpensePaise > summary.totalIncomePaise && summary.totalIncomePaise > 0 -> GhostMood.THINKING
            else -> GhostMood.HAPPY
        }
    }

    // Witty/funny status gauge text
    val funnyStatus = remember(summary) {
        when {
            summary.initialBudgetPaise == 0L -> "Set a budget before the ghosts eat it all! 👻"
            summary.remainingBudgetPaise < 0 -> "You're haunting your bank account! 😱 Over budget!"
            summary.remainingBudgetPaise < (summary.initialBudgetPaise * 0.2) -> "Danger zone! Your pocket is weeping 📉"
            else -> "Looking spooky rich! Keep it safe 🪙✨"
        }
    }

    // ------------------------------------------------------------------------
    // 2. Active Dialogs
    // ------------------------------------------------------------------------
    if (showMonthPicker) {
        MonthPickerDialog(
            currentSelectedMonth = selectedMonth,
            creationMonth = creationMonth,
            onMonthSelected = onSelectMonth,
            onDismiss = { showMonthPicker = false }
        )
    }

    categoryForBudgetEdit?.let { spend ->
        QuickCategoryBudgetDialog(
            spend = spend,
            monthLabel = selectedMonth.label,
            onDismiss = { categoryForBudgetEdit = null },
            onSave = { newBudget ->
                onSetCategoryBudget(spend.category, newBudget)
            }
        )
    }

    // ------------------------------------------------------------------------
    // 3. Screen Layout
    // ------------------------------------------------------------------------
    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 100.dp)
        ) {
            // Header: Spooky Greeting + Bento Actions (Settings, Theme, Mascot)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Boo! 👋",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "GhostPocket",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ClayIconButton(
                            icon = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                            onClick = onToggleTheme,
                            contentDescription = if (isDarkTheme) "Switch to Light Theme" else "Switch to Dark Theme",
                            size = 46.dp,
                            shapeRadius = 15.dp
                        )

                        ClayIconButton(
                            icon = Icons.Default.Settings,
                            onClick = onSettingsClick,
                            contentDescription = "Settings",
                            size = 46.dp,
                            shapeRadius = 15.dp
                        )

                        ClayCard(
                            shapeRadius = 16.dp,
                            elevation = 4.dp,
                            modifier = Modifier.size(46.dp),
                            onClick = onSetBudgetClick
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                GhostMascot(size = 34.dp, mood = ghostMood)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Month Selector Pill Bar
            item {
                MonthSelectorRow(
                    selectedMonth = selectedMonth,
                    canSelectPreviousMonth = canSelectPreviousMonth,
                    onPreviousMonth = onPreviousMonth,
                    onNextMonth = onNextMonth,
                    onPickMonth = { showMonthPicker = true }
                )
            }

            // Main Balance Card & Daily Safe-to-Spend Allowance
            item {
                DashboardBalanceCard(
                    summary = summary,
                    selectedMonth = selectedMonth,
                    funnyStatus = funnyStatus,
                    dailySafePaise = dailySafePaise,
                    daysRemaining = daysRemaining,
                    onSetBudgetClick = onSetBudgetClick
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Income (Fed 🪙) & Expense (Vanished 💸) Bento Row
            item {
                DashboardInflowOutflowRow(
                    totalIncomePaise = summary.totalIncomePaise,
                    totalExpensePaise = summary.totalExpensePaise
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Category Realms Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Category Realms 🏷️",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Allocated, spent & remaining gold",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { onManageCategoriesClick() }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Manage ⚙️",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Category Realm Progress List
            if (categorySpends.isEmpty()) {
                item {
                    ClayCard(
                        modifier = Modifier.fillMaxWidth(),
                        shapeRadius = 22.dp
                    ) {
                        EmptyStateView(
                            message = "No categories or spells logged yet! Tap '+ Add' or create a category in settings 🏷️",
                            mood = GhostMood.THINKING
                        )
                    }
                }
            } else {
                items(
                    items = categorySpends,
                    key = { it.category },
                    contentType = { "category_spend" }
                ) { spend ->
                    BentoCategoryBudgetCard(
                        spend = spend,
                        onClick = { categoryForBudgetEdit = spend }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }

        // Tactile Clay Floating Action Button (+ Add)
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(22.dp)
        ) {
            ClayButton(
                text = "+ Add",
                icon = Icons.Default.Add,
                onClick = onAddClick
            )
        }
    }
}
