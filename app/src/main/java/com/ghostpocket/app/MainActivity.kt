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
        GhostPocketViewModelFactory(app.repository, app.d1ApiClient, app.updateManager, applicationContext) {
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
    var showAppTour by remember { mutableStateOf(!com.ghostpocket.app.util.AppPreferences.hasCompletedTour(context)) }

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
            showAppTour -> {
                com.ghostpocket.app.util.AppPreferences.setTourCompleted(context, true)
                showAppTour = false
            }
            availableUpdate != null && !availableUpdate!!.isMandatory -> viewModel.dismissUpdateDialog()
            transactionToEdit != null -> transactionToEdit = null
            isAddTransactionOpen -> isAddTransactionOpen = false
            showAddCategoryDialog -> showAddCategoryDialog = false
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
        com.ghostpocket.app.ui.components.InitialDataFetchingScreen()
        return
    }


    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (selectedFriendBalance == null && !isSplitBillOpen && !isSettingsOpen) {
                MainBottomBar(
                    currentScreen = currentScreen,
                    onSelectScreen = { currentScreen = it }
                )
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
                            categories = uiState.categories,
                            onBack = { isSplitBillOpen = false },
                            onSplitConfirmed = { title, totalPaise, payerFriendId, participantFriendIds, category, customShares ->
                                viewModel.splitBill(title, totalPaise, payerFriendId, participantFriendIds, category, customShares)
                                isSplitBillOpen = false
                            }
                        )
                    }

                    ActiveSubScreen.SETTINGS -> {
                        SettingsScreen(
                            categories = uiState.categories,
                            isSyncing = uiState.isSyncing,
                            currentThemeMode = currentThemeMode,
                            onThemeChange = onThemeChange,
                            isCheckingForUpdate = isCheckingForUpdate,
                            currentVersionName = currentVersionName,
                            currentVersionCode = currentVersionCode,
                            onCheckForUpdates = { viewModel.checkForUpdates(isManual = true) },
                            onReplayTour = {
                                isSettingsOpen = false
                                showAppTour = true
                            },
                            onBack = { isSettingsOpen = false },
                            onManualSync = { viewModel.manualSync() },
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
                                        creationMonth = uiState.creationMonth,
                                        canSelectPreviousMonth = uiState.canSelectPreviousMonth,
                                        onPreviousMonth = { viewModel.selectPreviousMonth() },
                                        onNextMonth = { viewModel.selectNextMonth() },
                                        onCurrentMonth = { viewModel.selectCurrentMonth() },
                                        onSelectMonth = { viewModel.selectMonth(it) },
                                        onAddClick = { isAddTransactionOpen = true },
                                        onSetCategoryBudget = { cat, budgetPaise ->
                                            viewModel.editCategory(cat, cat, budgetPaise, uiState.selectedMonth.monthKey)
                                        },
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
            targetMonth = uiState.selectedMonth,
            transactionToEdit = transactionToEdit,
            onDismiss = {
                isAddTransactionOpen = false
                transactionToEdit = null
            },
            onAddNewCategoryPrompt = { showAddCategoryDialog = true },
            onSave = { amountPaise, type, category, note, timestamp ->
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
                    viewModel.addTransaction(amountPaise, type, category, note, timestamp)
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

    // Add Category Dialog
    if (showAddCategoryDialog) {
        AddCategoryDialog(
            onDismiss = { showAddCategoryDialog = false },
            onConfirm = { name, budgetPaise -> viewModel.addCategory(name, budgetPaise) }
        )
    }

    // Add Friend Dialog
    if (showAddFriendDialog) {
        AddFriendDialog(
            context = context,
            contactPickerLauncher = contactPickerLauncher,
            initialName = friendNameInput,
            initialWhatsApp = friendWhatsAppInput,
            onDismiss = {
                showAddFriendDialog = false
                friendNameInput = ""
                friendWhatsAppInput = ""
            },
            onConfirm = { name, whatsApp ->
                viewModel.addFriend(name, whatsApp)
                showAddFriendDialog = false
                friendNameInput = ""
                friendWhatsAppInput = ""
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

    // First-Time App Tour Dialog
    if (showAppTour) {
        com.ghostpocket.app.ui.components.AppTourDialog(
            onDismiss = {
                com.ghostpocket.app.util.AppPreferences.setTourCompleted(context, true)
                showAppTour = false
            },
            onComplete = {
                com.ghostpocket.app.util.AppPreferences.setTourCompleted(context, true)
                showAppTour = false
            }
        )
    }
}
