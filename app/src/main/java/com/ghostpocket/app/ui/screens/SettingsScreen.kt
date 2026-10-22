package com.ghostpocket.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ghostpocket.app.model.CategoryEntity
import com.ghostpocket.app.model.MoneyUtils
import com.ghostpocket.app.ui.components.ClayButton
import com.ghostpocket.app.ui.components.ClayCard
import com.ghostpocket.app.util.AppThemeMode

/**
 * ============================================================================
 * SettingsScreen
 * ============================================================================
 * Settings and customization panel for GhostPocket.
 *
 * Screen Sections:
 * 1. Monthly Allowance / Overall Budget configuration.
 * 2. Category Realm Management: [CategorySettingsCard] (List, Edit, Delete, Add new realms).
 * 3. Appearance & Theme: [ThemeSettingsCard] (Light, Dark, System modes).
 * 4. App Tour Replay: [AppTourSettingsCard].
 * 5. Cloud Spirits & Sync: [CloudSyncSettingsCard].
 * 6. App Updates: [AppUpdateSettingsCard] (GitHub release check).
 * 7. Account & Sign Out.
 *
 * @param categories List of categories in the database.
 * @param currentBudgetPaise Active monthly budget in paise.
 * @param isSyncing Whether cloud sync is running.
 * @param currentThemeMode Active theme mode (Light/Dark/System).
 * @param currentVersionName Human-readable version string (e.g. "1.5.0").
 * @param currentVersionCode Build version code (e.g. 13).
 * @param isCheckingForUpdate Whether update check network request is running.
 * @param onBack Back navigation callback.
 * @param onSetBudget Invoked to change monthly budget.
 * @param onAddCategory Invoked to insert a new category realm.
 * @param onEditCategory Invoked to rename or update a category's budget.
 * @param onDeleteCategory Invoked to remove a category realm.
 * @param onManualSync Triggers manual cloud sync.
 * @param onCheckForUpdates Triggers manual update check against GitHub.
 * @param onThemeChange Updates user theme preference.
 * @param onReplayTour Triggers interactive app tour.
 * @param onSignOut Clears user session.
 * @param modifier Composable modifier.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    categories: List<CategoryEntity>,
    isSyncing: Boolean,
    currentThemeMode: AppThemeMode = AppThemeMode.DARK,
    currentVersionName: String = "2.0.0",
    currentVersionCode: Int = 2,
    isCheckingForUpdate: Boolean = false,
    onBack: () -> Unit,
    onAddCategory: (String, Long) -> Unit,
    onEditCategory: (oldName: String, newName: String, budgetPaise: Long) -> Unit = { _, _, _ -> },
    onDeleteCategory: (CategoryEntity) -> Unit,
    onManualSync: () -> Unit,
    onCheckForUpdates: () -> Unit = {},
    onThemeChange: (AppThemeMode) -> Unit = {},
    onReplayTour: () -> Unit = {},
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    // ------------------------------------------------------------------------
    // 1. Dialog State
    // ------------------------------------------------------------------------
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var categoryToEdit by remember { mutableStateOf<CategoryEntity?>(null) }
    var categoryToDelete by remember { mutableStateOf<CategoryEntity?>(null) }

    // ------------------------------------------------------------------------
    // 2. Scaffold & List Layout
    // ------------------------------------------------------------------------
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Spooky Settings ⚙️") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            // Custom Realm / Categories Management Card
            item {
                CategorySettingsCard(
                    categories = categories,
                    onAddClick = { showAddCategoryDialog = true },
                    onEditClick = { categoryToEdit = it },
                    onDeleteClick = { categoryToDelete = it }
                )
                Spacer(modifier = Modifier.height(18.dp))
            }

            // Theme & Appearance
            item {
                ThemeSettingsCard(
                    currentThemeMode = currentThemeMode,
                    onThemeChange = onThemeChange
                )
                Spacer(modifier = Modifier.height(18.dp))
            }

            // App Tour & Magic Feature Guide
            item {
                AppTourSettingsCard(onReplayTour = onReplayTour)
                Spacer(modifier = Modifier.height(18.dp))
            }

            // Cloud Sync
            item {
                CloudSyncSettingsCard(
                    isSyncing = isSyncing,
                    onManualSync = onManualSync
                )
                Spacer(modifier = Modifier.height(18.dp))
            }

            // App Updates
            item {
                AppUpdateSettingsCard(
                    currentVersionName = currentVersionName,
                    currentVersionCode = currentVersionCode,
                    isCheckingForUpdate = isCheckingForUpdate,
                    onCheckForUpdates = onCheckForUpdates
                )
                Spacer(modifier = Modifier.height(18.dp))
            }

            // Account & Sign Out
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
                        text = "GhostPocket v$currentVersionName • Clay Bento Edition",
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

    // ------------------------------------------------------------------------
    // 3. Category Dialogs
    // ------------------------------------------------------------------------
    if (showAddCategoryDialog) {
        AddCategoryDialog(
            onDismiss = { showAddCategoryDialog = false },
            onConfirm = onAddCategory
        )
    }

    val editingCat = categoryToEdit
    if (editingCat != null) {
        EditCategoryDialog(
            category = editingCat,
            onDismiss = { categoryToEdit = null },
            onConfirm = { newName, newBudget ->
                onEditCategory(editingCat.name, newName, newBudget)
            }
        )
    }

    val deletingCat = categoryToDelete
    if (deletingCat != null) {
        DeleteCategoryConfirmDialog(
            category = deletingCat,
            onDismiss = { categoryToDelete = null },
            onConfirm = { onDeleteCategory(deletingCat) }
        )
    }
}
