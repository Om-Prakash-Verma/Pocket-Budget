# GhostPocket (Pocket Budget) - Complete User POV Flowcharts

Visual walkthrough of the application architecture, user journeys, decision nodes, and financial data flows from a user's point of view.

---

## 1. Master User Navigation & App Lifecycle

```mermaid
flowchart TD
    Start(["Launch GhostPocket App"]) --> AuthCheck{"Authenticated?"}

    AuthCheck -- No --> AuthScreen["Auth Screen"]
    AuthScreen -->|Email & Password| LoginChoice{"Action"}
    LoginChoice -->|Sign In / Sign Up| AuthProcess["Cloudflare D1 / Firebase"]
    AuthScreen -->|Google One-Tap| AuthProcess
    AuthScreen -->|Guest Mode| GuestWalk["Ghost Walk (Local Offline)"]
    AuthProcess --> CheckFirstLaunch{"First Time User?"}
    GuestWalk --> CheckFirstLaunch

    AuthCheck -- Yes --> CheckFirstLaunch

    CheckFirstLaunch -- Yes --> AppTour["Interactive 5-Step App Tour"]
    AppTour -->|Finish or Skip| HomeDashboard
    CheckFirstLaunch -- No --> HomeDashboard["🏠 Home (Dashboard)"]

    HomeDashboard --> NavTabs{"Bottom Navigation"}
    NavTabs -->|Home Tab| HomeDashboard
    NavTabs -->|Spells Tab| SpellsScreen["📜 Spells (Activity List)"]
    NavTabs -->|Squad Tab| SquadScreen["👥 Squad (Friends Ledger)"]
    NavTabs -->|Crystal Tab| CrystalScreen["🔮 Crystal (Insights & Analytics)"]

    HomeDashboard --> TopActions{"Top Bar Actions"}
    TopActions -->|Theme Toggle| ThemeMode["Toggle Obsidian Dark / Light"]
    TopActions -->|Settings Gear| SettingsScreen["⚙️ Spooky Settings"]
    TopActions -->|Mascot Status| MascotReaction["Dynamic Mascot Mood Reaction"]
```

---

## 2. Daily Financial Flow: Income, Expenses & Safe-to-Spend

```mermaid
flowchart TD
    subgraph DashboardView["Dashboard Overview"]
        MonthNav["Month Selector Carousel<br/>(Account Creation Month ➔ Current)"]
        NetCard["Net Balance Bento Card<br/>(All-Time Income − All-Time Expenses)"]
        MonthContext{"Active Month Check"}
        
        MonthNav --> MonthContext
        MonthContext -- Current Month --> SafePill["🎯 Live Safe-to-Spend Pill<br/>(Net Balance ÷ Remaining Days)"]
        MonthContext -- Past Month --> SettledPill["✨ Historical Status Pill<br/>(Net Saved vs Overspent)"]
        MonthContext -- Future Month --> FuturePill["🗓️ Upcoming Month Placeholder"]

        InflowOutflow["Inflow / Outflow Summary<br/>(Total Income vs Total Spent)"]
        CatBudgets["Category Realms<br/>(Spending progress bar & monthly caps)"]
    end

    subgraph AddFlow["Record Transaction (+ FAB)"]
        TapFAB(["Tap + FAB"]) --> OpenSheet["Add Transaction Bottom Sheet"]
        OpenSheet --> TypeToggle{"Choose Type"}
        
        TypeToggle -- Expense 💸 --> EnterExp["Enter Expense Amount"]
        TypeToggle -- Income 🪙 --> EnterInc["Enter Income / Pocket Money"]

        EnterExp --> MathRow["In-Line Arithmetic Calculator<br/>(+, −, ×, ÷, Decimals, BODMAS)"]
        EnterInc --> MathRow
        MathRow --> QuickChips["Quick Amount Chips<br/>(₹50, ₹100, ₹200, ₹500)"]
        
        QuickChips --> PickCategory["Select Realm / Category<br/>(Food, Travel, Outings, or + New)"]
        PickCategory --> EnterNote["Optional Note / Description"]
        EnterNote --> SaveTx(["Save Spell"])
        
        SaveTx --> LiveRecalc["Live Balance & Safe-to-Spend Recalculated"]
        LiveRecalc --> DashboardView
    end
```

---

## 3. Ghost Squad (Friend Debt & Udhaar Khata)

```mermaid
flowchart TD
    SquadHome["👥 Ghost Squad Hub"] --> SquadOverview["Squad Overview Bento<br/>(THEY OWE YOU 📜 vs YOU OWE THEM 💸)"]
    SquadHome --> FriendSearch["Live Search & Filter Pills<br/>(All, Owes You, You Owe, Settled, A-Z)"]
    
    SquadHome --> AddFriendAction(["+ Friend Button"])
    AddFriendAction --> FriendDialog["Add Friend Dialog"]
    FriendDialog -->|Device Contacts| ContactPicker["Pick from Phone Contacts"]
    FriendDialog -->|Manual Input| ManualFriend["Enter Name & WhatsApp Number"]
    ContactPicker --> SaveFriend["Summon Friend to Squad"]
    ManualFriend --> SaveFriend

    SquadHome --> FriendCard["Tap Friend Card"]
    FriendCard --> FriendDetail["Friend Detail Ledger Screen"]

    subgraph FriendLedgerActions["Friend Detail Actions"]
        FriendDetail --> LedgerHistory["Chronological Ledger Items<br/>(Gave 🔴 vs Got 🟢)"]
        
        FriendDetail --> ActionGave["Tap '+ Gave 🔴'<br/>(You lent money to friend)"]
        FriendDetail --> ActionGot["Tap '+ Got 🟢'<br/>(Friend gave money to you)"]
        FriendDetail --> ActionRepay["Tap 'Record Repayment'<br/>(Partial / Full payoff)"]
        FriendDetail --> ActionSettleAll["Tap 'Settle All'<br/>(Instant zero balance)"]
        FriendDetail --> ActionWhatsApp["Tap 'WhatsApp Reminder'<br/>(Auto-generates funny comic card with ghost mascot)"]
    end

    ActionGave --> UpdateDebt["Update Friend Net Balance"]
    ActionGot --> UpdateDebt
    ActionRepay --> UpdateDebt
    ActionSettleAll --> UpdateDebt
    UpdateDebt --> IsolatedLedger["Protected Udhaar Khata<br/>(Does NOT pollute personal income/expense totals)"]
```

---

## 4. Bill Splitting Flow (Equal & Unequal Shares)

```mermaid
flowchart TD
    StartSplit(["Tap 'Split ⚔️' Button"]) --> SplitScreen["SplitBillScreen"]

    SplitScreen --> BillInputs["1. Enter Bill Title & Total Amount<br/>(In-line Calculator Supported)"]
    BillInputs --> SelectCategory["2. Select Category<br/>(Food 🍔, Outings 🎬, Travel 🚌, etc.)"]
    SelectCategory --> WhoPaid{"3. Who Paid Upfront?"}

    WhoPaid -- You Paid 👑 --> UserPaid["User Paid Total Bill Upfront"]
    WhoPaid -- Friend Paid 👤 --> FriendPaid["Selected Friend Paid Upfront"]

    UserPaid --> ModeSelect{"4. Split Mode"}
    FriendPaid --> ModeSelect

    ModeSelect -- Equal Split ⚖️ --> AutoDivide["Fair Share Calculated Evenly<br/>(Total ÷ Number of Participants)"]
    ModeSelect -- Custom Shares 🍕 --> CustomAllocation["Custom Allocation Matrix<br/>(Enter custom amounts per person)"]
    CustomAllocation --> LiveValidation{"Allocated == Total?"}
    LiveValidation -- Over / Remaining --> ShowWarning["Live Warning Pill: Adjust shares"]
    LiveValidation -- Matched ✅ --> ReadyToConfirm

    AutoDivide --> SelectSquad["Select Participating Squad Members"]
    SelectSquad --> ReadyToConfirm["Confirmation Button Active"]
    
    ReadyToConfirm --> CastSplit(["Tap 'Cast Split Bill ⚔️'"])
    
    CastSplit --> SplitExecution{"Execution Engine"}
    SplitExecution -- If User Paid --> DeductWallet["1. Deduct Full Bill from Wallet (Personal Expense)<br/>2. Add 'They Owe You' in each friend's ledger"]
    SplitExecution -- If Friend Paid --> RecordDebt["1. Record 'You Owe Friend' in Payer's Ledger<br/>2. Wallet unaffected until repayment"]
```

---

## 5. Spells (Activity), Insights (Crystal) & Settings

```mermaid
flowchart LR
    subgraph SpellsTab["📜 Spells (Activity)"]
        SearchActivity["Fuzzy Search by Note or Realm"]
        FilterActivity["Type Filter: All / Expense Only / Income Only"]
        EditTx["Tap Transaction ➔ Edit or Banish (Delete)"]
    end

    subgraph CrystalTab["🔮 Crystal (Analytics)"]
        DonutChart["Bento Expense Donut & Bar Charts"]
        CategoryBreakdown["Realm Breakdown by % Share"]
        CashflowTrend["Monthly Inflow vs Outflow Comparison"]
    end

    subgraph SettingsTab["⚙️ Settings"]
        RealmManager["Manage Categories (Add, Rename, Safe Delete with Fallback)"]
        ThemeSelector["Theme Preferences (Obsidian Dark / Light / System)"]
        ReplayTour["Replay App Tour"]
        SyncSpirits["Manual Cloud Backup & Restore"]
        AppUpdater["Check for Updates ➔ Background Download ➔ In-App Installer"]
        SignOutSession["Sign Out ➔ Clean Secure Local Cache"]
    end
```
