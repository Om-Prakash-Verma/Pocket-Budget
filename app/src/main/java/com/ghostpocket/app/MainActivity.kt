package com.ghostpocket.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.ghostpocket.app.ui.screens.*
import com.ghostpocket.app.ui.theme.GhostPocketTheme
import com.ghostpocket.app.viewmodel.GhostPocketViewModel
import com.ghostpocket.app.viewmodel.GhostPocketViewModelFactory

enum class Screen(val title: String, val icon: ImageVector) {
    HOME("Home 👻", Icons.Default.Home),
    ACTIVITY("Spells 📜", Icons.Default.ReceiptLong),
    FRIENDS("Squad 👥", Icons.Default.Group),
    INSIGHTS("Crystal 🔮", Icons.Default.Analytics)
}

class MainActivity : ComponentActivity() {

    private val viewModel: GhostPocketViewModel by viewModels {
        val app = application as GhostPocketApp
        GhostPocketViewModelFactory(app.repository) {
            SyncWorker.scheduleSync(applicationContext)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GhostPocketTheme {
                MainContent(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainContent(viewModel: GhostPocketViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    var currentScreen by remember { mutableStateOf(Screen.HOME) }

    // Secondary sub-screens
    var selectedFriendBalance by remember { mutableStateOf<FriendBalance?>(null) }
    var isSplitBillOpen by remember { mutableStateOf(false) }
    var isSettingsOpen by remember { mutableStateOf(false) }

    // Bottom sheets and dialogs
    var isAddTransactionOpen by remember { mutableStateOf(false) }
    var showAddFriendDialog by remember { mutableStateOf(false) }
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

    // Google Sign-In Launcher
    val googleSignInLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val task = com.google.android.gms.auth.api.signin.GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
                account.idToken?.let { idToken ->
                    viewModel.signInWithGoogle(idToken)
                }
            } catch (e: Exception) {
                // Handled in viewModel
            }
        }
    }

    if (!uiState.isAuthenticated) {
        AuthScreen(
            isLoading = uiState.authLoading,
            errorMessage = uiState.authError,
            onSignIn = { email, pass -> viewModel.signIn(email, pass) },
            onSignUp = { email, pass -> viewModel.signUp(email, pass) },
            onGoogleSignIn = {
                val gso = com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(
                    com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN
                )
                    .requestIdToken(context.getString(R.string.default_web_client_id))
                    .requestEmail()
                    .build()
                val googleSignInClient = com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(context, gso)
                googleSignInLauncher.launch(googleSignInClient.signInIntent)
            },
            onAnonymousSignIn = { viewModel.continueAsGuest() }
        )
        return
    }

    Scaffold(
        topBar = {
            if (selectedFriendBalance == null && !isSplitBillOpen && !isSettingsOpen) {
                TopAppBar(
                    title = {
                        Text(
                            text = "GhostPocket 👻",
                            style = MaterialTheme.typography.titleMedium
                        )
                    },
                    actions = {
                        IconButton(onClick = { isSettingsOpen = true }) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            }
        },
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
        ) {
            when {
                selectedFriendBalance != null -> {
                    val activeFriend = selectedFriendBalance!!
                    val ledgers by viewModel.getLedgerEntriesForFriend(activeFriend.friend.id).collectAsState(initial = emptyList())
                    val latestBalance = uiState.friends.find { it.friend.id == activeFriend.friend.id } ?: activeFriend

                    FriendDetailScreen(
                        balance = latestBalance,
                        ledgers = ledgers,
                        onBack = { selectedFriendBalance = null },
                        onAddLedger = { paise, direction, note ->
                            viewModel.addLedgerEntry(activeFriend.friend.id, paise, direction, note)
                        },
                        onRecordRepayment = { paise, isRepayingToUser ->
                            viewModel.recordRepayment(activeFriend.friend.id, paise, isRepayingToUser)
                        },
                        onSettleAll = {
                            viewModel.settleFriend(activeFriend.friend.id)
                        },
                        onDeleteFriend = {
                            viewModel.deleteFriend(activeFriend.friend.id)
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

                isSplitBillOpen -> {
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

                isSettingsOpen -> {
                    SettingsScreen(
                        categories = uiState.categories,
                        currentBudgetPaise = uiState.summary.initialBudgetPaise,
                        isSyncing = uiState.isSyncing,
                        onBack = { isSettingsOpen = false },
                        onManualSync = { viewModel.manualSync() },
                        onSetBudget = { showSetBudgetDialog = true },
                        onAddCategory = { name, budgetPaise -> viewModel.addCategory(name, budgetPaise) },
                        onDeleteCategory = { viewModel.deleteCategory(it) },
                        onSignOut = {
                            viewModel.signOut()
                            isSettingsOpen = false
                        }
                    )
                }

                else -> {
                    when (currentScreen) {
                        Screen.HOME -> {
                            DashboardScreen(
                                summary = uiState.summary,
                                categorySpends = uiState.categorySpends,
                                onAddClick = { isAddTransactionOpen = true },
                                onSetBudgetClick = { showSetBudgetDialog = true },
                                onManageCategoriesClick = { isSettingsOpen = true }
                            )
                        }

                        Screen.ACTIVITY -> {
                            ActivityScreen(
                                transactions = uiState.allTransactions,
                                onTransactionClick = { /* Detail action */ }
                            )
                        }

                        Screen.FRIENDS -> {
                            FriendsScreen(
                                friends = uiState.friends,
                                onAddFriendClick = { showAddFriendDialog = true },
                                onSplitBillClick = { isSplitBillOpen = true },
                                onFriendClick = { balance -> selectedFriendBalance = balance }
                            )
                        }

                        Screen.INSIGHTS -> {
                            InsightsScreen(
                                summary = uiState.summary,
                                categorySpends = uiState.categorySpends,
                                recentTransactions = uiState.recentTransactions
                            )
                        }
                    }
                }
            }
        }
    }

    // Add Transaction Bottom Sheet
    if (isAddTransactionOpen) {
        AddTransactionBottomSheet(
            categories = uiState.categories,
            onDismiss = { isAddTransactionOpen = false },
            onAddNewCategoryPrompt = { showAddCategoryDialog = true },
            onSave = { amountPaise, type, category, note ->
                viewModel.addTransaction(amountPaise, type, category, note)
                isAddTransactionOpen = false
            }
        )
    }

    // Set Budget Dialog
    if (showSetBudgetDialog) {
        var budgetInput by remember {
            mutableStateOf(
                if (uiState.summary.initialBudgetPaise > 0L) (uiState.summary.initialBudgetPaise / 100).toString() else ""
            )
        }
        AlertDialog(
            onDismissRequest = { showSetBudgetDialog = false },
            title = {
                Text(
                    text = "Set Your Pocket Budget 💰👻",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "How much gold/allowance do you have for the month? The ghost will watch over your limits.",
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
                            viewModel.setUserBudget(paise)
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
        var friendName by remember { mutableStateOf("") }
        var friendWhatsApp by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddFriendDialog = false },
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
                        text = "Add someone to your squad. You can send them reminder card images directly to WhatsApp!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = friendName,
                        onValueChange = { friendName = it },
                        label = { Text("Friend's Name") },
                        placeholder = { Text("e.g. Aarav, Rohan, Priya") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = friendWhatsApp,
                        onValueChange = { friendWhatsApp = it },
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
                    enabled = friendName.isNotBlank(),
                    onClick = {
                        if (friendName.isNotBlank()) {
                            viewModel.addFriend(friendName.trim(), friendWhatsApp.trim())
                            showAddFriendDialog = false
                        }
                    }
                )
            },
            dismissButton = {
                TextButton(onClick = { showAddFriendDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
