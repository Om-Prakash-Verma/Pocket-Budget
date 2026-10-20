package com.ghostpocket.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ghostpocket.app.model.CategoryEntity
import com.ghostpocket.app.model.MoneyUtils
import com.ghostpocket.app.ui.components.CategoryIcon
import com.ghostpocket.app.ui.components.ClayButton
import com.ghostpocket.app.ui.components.ClayCard
import com.ghostpocket.app.util.AppThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    categories: List<CategoryEntity>,
    currentBudgetPaise: Long,
    isSyncing: Boolean,
    currentThemeMode: AppThemeMode = AppThemeMode.DARK,
    onThemeChange: (AppThemeMode) -> Unit = {},
    onBack: () -> Unit,
    onManualSync: () -> Unit,
    onSetBudget: () -> Unit,
    onAddCategory: (name: String, budgetPaise: Long) -> Unit,
    onDeleteCategory: (CategoryEntity) -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddCategoryDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Spooky Settings ⚙️") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 18.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 40.dp)
        ) {
            // BENTO CELL: Budget Configuration
            item {
                ClayCard(
                    modifier = Modifier.fillMaxWidth(),
                    shapeRadius = 24.dp,
                    elevation = 6.dp
                ) {
                    Text(
                        text = "Monthly Allowance / Budget 💰",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Set how much money you have. The ghost will watch over your spending limits.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = MoneyUtils.formatPaiseToInr(currentBudgetPaise),
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.ExtraBold
                        )
                        ClayButton(
                            text = "Change ✏️",
                            onClick = onSetBudget,
                            isSecondary = true
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))
            }

            // BENTO CELL: Custom Categories (Full user control)
            item {
                ClayCard(
                    modifier = Modifier.fillMaxWidth(),
                    shapeRadius = 24.dp,
                    elevation = 6.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Your Categories 🏷️",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${categories.size} custom realms created",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        ClayButton(
                            text = "+ New",
                            onClick = { showAddCategoryDialog = true },
                            isSecondary = true
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (categories.isEmpty()) {
                        Text(
                            text = "No categories exist yet. Tap '+ New' to create your own! (e.g. Chai, Books, Rent)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        categories.forEach { cat ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    CategoryIcon(categoryName = cat.name, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = cat.name,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = if (cat.budgetPaise > 0L) {
                                                "Budget: ${MoneyUtils.formatPaiseToInr(cat.budgetPaise)}"
                                            } else {
                                                "No budget allocated"
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { onDeleteCategory(cat) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))
            }

            // BENTO CELL: Spooky Appearance & Theme
            item {
                ClayCard(
                    modifier = Modifier.fillMaxWidth(),
                    shapeRadius = 24.dp,
                    elevation = 6.dp
                ) {
                    Text(
                        text = "Spooky Appearance 🌓",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Choose your preferred vibe: Crisp Light or Deep Obsidian Dark.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(4.dp)
                    ) {
                        listOf(
                            AppThemeMode.LIGHT to "Light ☀️",
                            AppThemeMode.DARK to "Dark 🌙",
                            AppThemeMode.SYSTEM to "System ⚡"
                        ).forEach { (mode, label) ->
                            val isSelected = currentThemeMode == mode
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .clickable { onThemeChange(mode) }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))
            }

            // BENTO CELL: Cloud Sync
            item {
                ClayCard(
                    modifier = Modifier.fillMaxWidth(),
                    shapeRadius = 24.dp,
                    elevation = 6.dp
                ) {
                    Text(
                        text = "Cloud Spirits & Sync ☁️",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "WorkManager automatically syncs queued spells when internet returns. You can also force a sync now.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    ClayButton(
                        text = if (isSyncing) "Summoning cloud..." else "Sync Now 🔄",
                        icon = Icons.Default.Sync,
                        onClick = onManualSync,
                        enabled = !isSyncing,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))
            }

            // BENTO CELL: Account & Sign Out
            item {
                ClayCard(
                    modifier = Modifier.fillMaxWidth(),
                    shapeRadius = 24.dp,
                    elevation = 6.dp
                ) {
                    Text(
                        text = "Crypt Access & Account 🗝️",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    ClayButton(
                        text = "Sign Out 🚪",
                        isSecondary = true,
                        onClick = onSignOut,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // Spooky Footer
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "GhostPocket v2.0 • Clay Bento Edition",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Made with 👻 for college mortals",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    if (showAddCategoryDialog) {
        var newCatName by remember { mutableStateOf("") }
        var newCatBudget by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddCategoryDialog = false },
            title = { Text("Create Category 🏷️") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newCatName,
                        onValueChange = { newCatName = it },
                        label = { Text("Category Name") },
                        placeholder = { Text("e.g. Chai, Groceries, Games") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = newCatBudget,
                        onValueChange = { input ->
                            if (input.all { it.isDigit() || it == '.' } && input.count { it == '.' } <= 1) {
                                newCatBudget = input
                            }
                        },
                        label = { Text("Allocated Budget ₹ (Optional)") },
                        placeholder = { Text("e.g. 2000") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newCatName.isNotBlank()) {
                            val budgetPaise = MoneyUtils.parseToPaise(newCatBudget) ?: 0L
                            onAddCategory(newCatName.trim(), budgetPaise)
                            showAddCategoryDialog = false
                        }
                    },
                    enabled = newCatName.isNotBlank()
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCategoryDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
