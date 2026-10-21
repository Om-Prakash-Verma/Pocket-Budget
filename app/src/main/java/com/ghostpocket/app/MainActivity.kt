package com.ghostpocket.app

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghostpocket.app.model.FriendBalance
import com.ghostpocket.app.model.MoneyUtils
import com.ghostpocket.app.model.TransactionEntity
import com.ghostpocket.app.sync.SyncWorker
import com.ghostpocket.app.ui.components.ClayButton
import com.ghostpocket.app.ui.components.ClayCard
import com.ghostpocket.app.ui.components.GhostMascot
import com.ghostpocket.app.ui.components.GhostMood
import com.ghostpocket.app.ui.components.UpdateDialog
import com.ghostpocket.app.ui.screens.*

import com.ghostpocket.app.ui.theme.GhostPocketTheme
import com.ghostpocket.app.util.AppThemeMode
import com.ghostpocket.app.util.ThemePreferences
import com.ghostpocket.app.viewmodel.GhostPocketViewModel
import com.ghostpocket.app.viewmodel.GhostPocketViewModelFactory

enum class Screen(val title: String, val icon: ImageVector) {
    HOME("Home 👻", Icons.Default.Home),
    ACTIVITY("Spells 📜", Icons.Default.ReceiptLong),
    FRIENDS("Squad 👥", Icons.Default.Group),
    INSIGHTS("Crystal 🔮", Icons.Default.Analytics)
}

private enum class ActiveSubScreen {
    NONE,
    FRIEND_DETAIL,
    SPLIT_BILL,
    SETTINGS
}

class MainActivity : ComponentActivity() {

    private val viewModel: GhostPocketViewModel by viewModels {
        val app = application as GhostPocketApp
        GhostPocketViewModelFactory(app.repository, app.d1ApiClient, app.updateManager) {
            SyncWorker.scheduleSync(applicationContext)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as GhostPocketApp
        val userId = app.d1ApiClient?.currentUserId ?: "guest_user"
        SyncWorker.scheduleDailyPeriodicSync(applicationContext, userId)

        setContent {
            val context = androidx.compose.ui.platform.LocalContext.current
            val systemInDark = androidx.compose.foundation.isSystemInDarkTheme()
            var themeMode by remember { mutableStateOf(ThemePreferences.getThemeMode(context)) }
            val isDarkTheme = remember(themeMode, systemInDark) {
                when (themeMode) {
                    AppThemeMode.LIGHT -> false
                    AppThemeMode.DARK -> true
                    AppThemeMode.SYSTEM -> systemInDark
                }
            }

            GhostPocketTheme(darkTheme = isDarkTheme) {
                MainContent(
                    viewModel = viewModel,
                    currentThemeMode = themeMode,
                    isDarkTheme = isDarkTheme,
                    onThemeChange = { mode ->
                        ThemePreferences.setThemeMode(context, mode)
                        themeMode = mode
                    },
                    onToggleTheme = {
                        val next = if (isDarkTheme) AppThemeMode.LIGHT else AppThemeMode.DARK
                        ThemePreferences.setThemeMode(context, next)
                        themeMode = next
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainContent(
    viewModel: GhostPocketViewModel,
    currentThemeMode: AppThemeMode = AppThemeMode.DARK,
    isDarkTheme: Boolean = true,
    onThemeChange: (AppThemeMode) -> Unit = {},
    onToggleTheme: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var currentScreen by remember { mutableStateOf(Screen.HOME) }

    // In-App Update Observation
    val availableUpdate by viewModel.availableUpdate.collectAsState()
    val downloadState by viewModel.downloadState.collectAsState()
    val isCheckingForUpdate by viewModel.isCheckingForUpdate.collectAsState()
    val (currentVersionName, currentVersionCode) = remember { viewModel.getCurrentVersionInfo() }

    // Secondary sub-screens
    var selectedFriendBalance by remember { mutableStateOf<FriendBalance?>(null) }
    var isSplitBillOpen by remember { mutableStateOf(false) }
    var isSettingsOpen by remember { mutableStateOf(false) }

    // Bottom sheets and dialogs
    var isAddTransactionOpen by remember { mutableStateOf(false) }
    var transactionToEdit by remember { mutableStateOf<TransactionEntity?>(null) }
    var showAddFriendDialog by remember { mutableStateOf(false) }
    var friendNameInput by rememberSaveable { mutableStateOf("") }
    var friendWhatsAppInput by rememberSaveable { mutableStateOf("") }
    var showSetBudgetDialog by remember { mutableStateOf(false) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessage()
        }
    }

    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = context as? ComponentActivity

    // Contact Picker Launcher for directly adding friends from device contacts
    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val contactUri: Uri? = result.data?.data
            if (contactUri != null) {
                try {
                    val projection = arrayOf(
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                        ContactsContract.CommonDataKinds.Phone.NUMBER
                    )
                    context.contentResolver.query(contactUri, projection, null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                            val numberIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                            if (nameIdx != -1) {
                                val name = cursor.getString(nameIdx)
                                if (!name.isNullOrBlank()) {
                                    friendNameInput = name
                                }
                            }
                            if (numberIdx != -1) {
                                val rawNumber = cursor.getString(numberIdx)
                                if (!rawNumber.isNullOrBlank()) {
                                    // Strip whitespace, hyphens, and parenthesis for clean phone format
                                    val cleaned = rawNumber.replace("[\\s\\-\\(\\)]".toRegex(), "")
                                    friendWhatsAppInput = cleaned
                                }
                            }
                        }
                    }
                    showAddFriendDialog = true
                } catch (e: Exception) {
                    Toast.makeText(context, "Could not load contact: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // System Back Handler: Prevents accidental app closing and cleanly navigates back
    BackHandler(enabled = true) {
        when {
            availableUpdate != null && !availableUpdate!!.isMandatory -> viewModel.dismissUpdateDialog()
            transactionToEdit != null -> transactionToEdit = null
            isAddTransactionOpen -> isAddTransactionOpen = false
            showAddCategoryDialog -> showAddCategoryDialog = false
            showSetBudgetDialog -> showSetBudgetDialog = false
            showAddFriendDialog -> {
                showAddFriendDialog = false
                friendNameInput = ""
                friendWhatsAppInput = ""
            }
            selectedFriendBalance != null -> selectedFriendBalance = null
            isSplitBillOpen -> isSplitBillOpen = false
            isSettingsOpen -> isSettingsOpen = false
            currentScreen != Screen.HOME -> currentScreen = Screen.HOME
            else -> activity?.finish()
        }
    }

    // App Background Sync Observer: Syncs pending changes cleanly when user leaves/minimizes app
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_STOP) {
                SyncWorker.scheduleOnAppBackground(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    if (!uiState.isAuthenticated) {
        AuthScreen(
            isLoading = uiState.authLoading,
            errorMessage = uiState.authError,
            onSignIn = { email, pass -> viewModel.signIn(email, pass) },
            onSignUp = { email, pass -> viewModel.signUp(email, pass) }
        )
        return
    }

    // Initial Data Fetching Screen (when logging in with existing ID)
    if (uiState.isInitialDataFetching) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(24.dp)
            ) {
                ClayCard(
                    shapeRadius = 32.dp,
                    elevation = 10.dp,
                    modifier = Modifier.size(110.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        GhostMascot(
                            size = 80.dp,
                            mood = GhostMood.THINKING
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onBackground,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Conjuring your data from the crypt... 📜✨",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Restoring spells, gold ledger & categories",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }


    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (selectedFriendBalance == null && !isSplitBillOpen && !isSettingsOpen) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.background,
                    tonalElevation = 0.dp
                ) {
                    Screen.entries.forEach { screen ->
                        val selected = currentScreen == screen
                        NavigationBarItem(
                            selected = selected,
                            onClick = { currentScreen = screen },
                            icon = {
                                Icon(
                                    imageVector = screen.icon,
                                    contentDescription = screen.title
                                )
                            },
                            label = {
                                Text(
                                    text = screen.title,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onBackground,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                selectedTextColor = MaterialTheme.colorScheme.onBackground,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                indicatorColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding()
        ) {
            val activeSubScreen = when {
                selectedFriendBalance != null -> ActiveSubScreen.FRIEND_DETAIL
                isSplitBillOpen -> ActiveSubScreen.SPLIT_BILL
                isSettingsOpen -> ActiveSubScreen.SETTINGS
                else -> ActiveSubScreen.NONE
            }

            AnimatedContent(
                targetState = activeSubScreen,
                transitionSpec = {
                    if (targetState != ActiveSubScreen.NONE && initialState == ActiveSubScreen.NONE) {
                        (slideInVertically(animationSpec = tween(240, easing = FastOutSlowInEasing)) { it / 8 } +
                                fadeIn(animationSpec = tween(200)))
                            .togetherWith(
                                fadeOut(animationSpec = tween(160)) +
                                        scaleOut(targetScale = 0.96f, animationSpec = tween(160))
                            )
                    } else if (targetState == ActiveSubScreen.NONE && initialState != ActiveSubScreen.NONE) {
                        (fadeIn(animationSpec = tween(200)) +
                                scaleIn(initialScale = 0.96f, animationSpec = tween(200)))
                            .togetherWith(
                                slideOutVertically(animationSpec = tween(200, easing = FastOutLinearInEasing)) { it / 8 } +
                                        fadeOut(animationSpec = tween(160))
                            )
                    } else {
                        fadeIn(animationSpec = tween(180))
                            .togetherWith(fadeOut(animationSpec = tween(180)))
                    }
                },
                label = "SubScreenAnimatedTransition"
            ) { subScreen ->
                when (subScreen) {
                    ActiveSubScreen.FRIEND_DETAIL -> {
                        val activeFriend = selectedFriendBalance
                        if (activeFriend != null) {
                            val ledgers by viewModel.getLedgerEntriesForFriend(activeFriend.friend.id).collectAsState(initial = emptyList())
                            val latestBalance = uiState.friends.find { it.friend.id == activeFriend.friend.id } ?: activeFriend

                            FriendDetailScreen(
                                balance = latestBalance,
                                ledgers = ledgers,
                                onBack = { selectedFriendBalance = null },
                                onAddLedger = { paise, direction, note ->
                                    viewModel.addLedgerEntry(activeFriend.friend.id, paise, direction, note)
                                },
                                onUpdateLedger = { updatedLedger ->
                                    viewModel.updateLedgerEntry(updatedLedger)
                                },
                                onDeleteLedger = { ledgerId ->
                                    viewModel.deleteLedgerEntry(ledgerId)
                                },
                                onRecordRepayment = { paise, isRepayingToUser ->
                                    viewModel.recordRepayment(activeFriend.friend.id, paise, isRepayingToUser)
                                },
                                onSettleAll = {
                                    viewModel.settleFriend(activeFriend.friend.id)
                                },
                                onUpdateFriend = { updatedFriend ->
                                    viewModel.updateFriend(updatedFriend)
                                },
                                onDeleteFriend = { settleRemaining ->
                                    viewModel.deleteFriendSafe(activeFriend.friend.id, settleRemaining)
                                    selectedFriendBalance = null
                                },
                                onSendWhatsAppReminder = { whatsAppNumber, amountPaise, note ->
                                    com.ghostpocket.app.util.WhatsAppReminderHelper.sendWhatsAppReminder(
                                        context = context,
                                        whatsAppNumber = whatsAppNumber,
                                        friendName = activeFriend.friend.name,
                                        amountPaise = amountPaise,
                                        note = note
                                    )
                                }
                            )
                        }
                    }

                    ActiveSubScreen.SPLIT_BILL -> {
                        SplitBillScreen(
                            friends = uiState.rawFriends,
                            onBack = { isSplitBillOpen = false },
                            onSplitConfirmed = { title, totalPaise, payerFriendId, participantFriendIds ->
                                val defaultCat = uiState.categories.firstOrNull()?.name ?: "Split"
                                viewModel.splitBill(title, totalPaise, payerFriendId, participantFriendIds, defaultCat)
                                isSplitBillOpen = false
                            }
                        )
                    }

                    ActiveSubScreen.SETTINGS -> {
                        SettingsScreen(
                            categories = uiState.categories,
                            currentBudgetPaise = uiState.summary.initialBudgetPaise,
                            isSyncing = uiState.isSyncing,
                            currentThemeMode = currentThemeMode,
                            onThemeChange = onThemeChange,
                            isCheckingForUpdate = isCheckingForUpdate,
                            currentVersionName = currentVersionName,
                            currentVersionCode = currentVersionCode,
                            onCheckForUpdates = { viewModel.checkForUpdates(isManual = true) },
                            onBack = { isSettingsOpen = false },
                            onManualSync = { viewModel.manualSync() },
                            onSetBudget = { showSetBudgetDialog = true },
                            onAddCategory = { name, budgetPaise -> viewModel.addCategory(name, budgetPaise) },
                            onEditCategory = { oldName, newName, budgetPaise -> viewModel.editCategory(oldName, newName, budgetPaise, uiState.selectedMonth.monthKey) },
                            onDeleteCategory = { viewModel.deleteCategorySafe(it) },
                            onSignOut = {
                                viewModel.signOut()
                                isSettingsOpen = false
                            }
                        )
                    }

                    ActiveSubScreen.NONE -> {
                        AnimatedContent(
                            targetState = currentScreen,
                            transitionSpec = {
                                val forward = targetState.ordinal > initialState.ordinal
                                if (forward) {
                                    (slideInHorizontally(animationSpec = tween(220, easing = FastOutSlowInEasing)) { it / 4 } +
                                            fadeIn(animationSpec = tween(200)))
                                        .togetherWith(
                                            slideOutHorizontally(animationSpec = tween(180, easing = FastOutLinearInEasing)) { -it / 4 } +
                                                    fadeOut(animationSpec = tween(160))
                                        )
                                } else {
                                    (slideInHorizontally(animationSpec = tween(220, easing = FastOutSlowInEasing)) { -it / 4 } +
                                            fadeIn(animationSpec = tween(200)))
                                        .togetherWith(
                                            slideOutHorizontally(animationSpec = tween(180, easing = FastOutLinearInEasing)) { it / 4 } +
                                                    fadeOut(animationSpec = tween(160))
                                        )
                                }
                            },
                            label = "TabContentAnimatedTransition"
                        ) { screen ->
                            when (screen) {
                                Screen.HOME -> {
                                    DashboardScreen(
                                        summary = uiState.summary,
                                        categorySpends = uiState.categorySpends,
                                        selectedMonth = uiState.selectedMonth,
                                        onPreviousMonth = { viewModel.selectPreviousMonth() },
                                        onNextMonth = { viewModel.selectNextMonth() },
                                        onCurrentMonth = { viewModel.selectCurrentMonth() },
                                        onAddClick = { isAddTransactionOpen = true },
                                        onSetBudgetClick = { showSetBudgetDialog = true },
                                        onManageCategoriesClick = { isSettingsOpen = true },
                                        onSettingsClick = { isSettingsOpen = true },
                                        onToggleTheme = onToggleTheme,
                                        isDarkTheme = isDarkTheme
                                    )
                                }

                                Screen.ACTIVITY -> {
                                    ActivityScreen(
                                        transactions = uiState.allTransactions,
                                        onTransactionClick = { tx -> transactionToEdit = tx },
                                        onSettingsClick = { isSettingsOpen = true },
                                        onToggleTheme = onToggleTheme,
                                        isDarkTheme = isDarkTheme
                                    )
                                }

                                Screen.FRIENDS -> {
                                    FriendsScreen(
                                        friends = uiState.friends,
                                        onAddFriendClick = {
                                            friendNameInput = ""
                                            friendWhatsAppInput = ""
                                            showAddFriendDialog = true
                                        },
                                        onSplitBillClick = { isSplitBillOpen = true },
                                        onFriendClick = { balance -> selectedFriendBalance = balance },
                                        onSettingsClick = { isSettingsOpen = true },
                                        onToggleTheme = onToggleTheme,
                                        isDarkTheme = isDarkTheme
                                    )
                                }

                                Screen.INSIGHTS -> {
                                    InsightsScreen(
                                        summary = uiState.summary,
                                        categorySpends = uiState.categorySpends,
                                        recentTransactions = uiState.recentTransactions,
                                        onSettingsClick = { isSettingsOpen = true },
                                        onToggleTheme = onToggleTheme,
                                        isDarkTheme = isDarkTheme
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add / Edit Transaction Bottom Sheet
    if (isAddTransactionOpen || transactionToEdit != null) {
        AddTransactionBottomSheet(
            categories = uiState.categories,
            transactionToEdit = transactionToEdit,
            onDismiss = {
                isAddTransactionOpen = false
                transactionToEdit = null
            },
            onAddNewCategoryPrompt = { showAddCategoryDialog = true },
            onSave = { amountPaise, type, category, note ->
                val currentEdit = transactionToEdit
                if (currentEdit != null) {
                    viewModel.editTransaction(
                        currentEdit.copy(
                            amountPaise = amountPaise,
                            type = type,
                            category = category,
                            note = note
                        )
                    )
                } else {
                    viewModel.addTransaction(amountPaise, type, category, note)
                }
                isAddTransactionOpen = false
                transactionToEdit = null
            },
            onDelete = { id ->
                viewModel.deleteTransaction(id)
                isAddTransactionOpen = false
                transactionToEdit = null
            }
        )
    }

    // Set Budget Dialog
    if (showSetBudgetDialog) {
        val selectedMonth = uiState.selectedMonth
        val currentMonthBudgetPaise = uiState.summary.initialBudgetPaise
        var budgetInput by remember(selectedMonth, currentMonthBudgetPaise) {
            mutableStateOf(
                if (currentMonthBudgetPaise > 0L) (currentMonthBudgetPaise / 100).toString() else ""
            )
        }
        AlertDialog(
            onDismissRequest = { showSetBudgetDialog = false },
            title = {
                Text(
                    text = "Budget for ${selectedMonth.label} 💰👻",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "Set your gold allowance specifically for ${selectedMonth.label}. Other months will preserve their own budgets intact.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    ClayCard(
                        modifier = Modifier.fillMaxWidth(),
                        shapeRadius = 18.dp,
                        elevation = 2.dp
                    ) {
                        Text(
                            text = "MONTHLY ALLOWANCE",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "₹",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            OutlinedTextField(
                                value = budgetInput,
                                onValueChange = { input ->
                                    if (input.all { it.isDigit() || it == '.' } && input.count { it == '.' } <= 1) {
                                        budgetInput = input
                                    }
                                },
                                placeholder = { Text("10000") },
                                singleLine = true,
                                textStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                                    unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent
                                )
                            )
                        }
                    }
                }
            },
            confirmButton = {
                val paise = MoneyUtils.parseToPaise(budgetInput)
                ClayButton(
                    text = "Lock In 🔒",
                    enabled = paise != null,
                    onClick = {
                        if (paise != null) {
                            viewModel.setUserBudget(paise, selectedMonth.monthKey)
                            showSetBudgetDialog = false
                        }
                    }
                )
            },
            dismissButton = {
                TextButton(onClick = { showSetBudgetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Add Category Dialog
    if (showAddCategoryDialog) {
        var newCatName by remember { mutableStateOf("") }
        var newCatBudget by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddCategoryDialog = false },
            title = {
                Text(
                    text = "Create New Category 🏷️",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "Name your custom realm and assign a budget limit if desired.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = newCatName,
                        onValueChange = { newCatName = it },
                        label = { Text("Realm Name") },
                        placeholder = { Text("e.g. Chai, Books, Gym, Anime") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
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
                        placeholder = { Text("e.g. 1500") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                ClayButton(
                    text = "Create Realm ✨",
                    enabled = newCatName.isNotBlank(),
                    onClick = {
                        if (newCatName.isNotBlank()) {
                            val budgetPaise = MoneyUtils.parseToPaise(newCatBudget) ?: 0L
                            viewModel.addCategory(newCatName.trim(), budgetPaise)
                            showAddCategoryDialog = false
                        }
                    }
                )
            },
            dismissButton = {
                TextButton(onClick = { showAddCategoryDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Add Friend Dialog
    if (showAddFriendDialog) {
        AlertDialog(
            onDismissRequest = {
                showAddFriendDialog = false
                friendNameInput = ""
                friendWhatsAppInput = ""
            },
            title = {
                Text(
                    text = "Summon a Friend 👥👻",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "Add someone to your squad. Pick directly from your phone contacts or enter details manually!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    // Quick-Pick from Contacts Button
                    OutlinedButton(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
                                contactPickerLauncher.launch(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "No contacts app available on device", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Contacts",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Pick from Contacts 📱",
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f))
                        Text(
                            text = "  or enter manually  ",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        HorizontalDivider(modifier = Modifier.weight(1f))
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = friendNameInput,
                        onValueChange = { friendNameInput = it },
                        label = { Text("Friend's Name") },
                        placeholder = { Text("e.g. Aarav, Rohan, Priya") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = friendWhatsAppInput,
                        onValueChange = { friendWhatsAppInput = it },
                        label = { Text("WhatsApp Number 💬") },
                        placeholder = { Text("+91 9876543210") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                ClayButton(
                    text = "Summon 🪄",
                    enabled = friendNameInput.isNotBlank(),
                    onClick = {
                        if (friendNameInput.isNotBlank()) {
                            viewModel.addFriend(friendNameInput.trim(), friendWhatsAppInput.trim())
                            showAddFriendDialog = false
                            friendNameInput = ""
                            friendWhatsAppInput = ""
                        }
                    }
                )
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddFriendDialog = false
                    friendNameInput = ""
                    friendWhatsAppInput = ""
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    // In-App Update Dialog
    availableUpdate?.let { info ->
        UpdateDialog(
            updateInfo = info,
            downloadState = downloadState,
            currentVersionName = currentVersionName,
            onStartDownload = { viewModel.startUpdateDownload(info) },
            onInstall = { apkFile -> viewModel.installApk(apkFile) },
            onCancelDownload = { viewModel.cancelUpdateDownload() },
            onDismiss = { viewModel.dismissUpdateDialog() }
        )
    }
}
