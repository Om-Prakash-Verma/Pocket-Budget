# GhostPocket Logical Decision Flows & State Diagrams

Deep-dive logical flowcharts showing how internal algorithms, budget thresholds, UI indicators, and math engines function under the hood from a user's perspective.

---

## 1. Category Budgeting: Inheritance, Tracking & Visual States

Shows how monthly category budgets inherit values across months, calculate usage, and switch UI visual states (Green vs Red warning).

```mermaid
flowchart TD
    StartCat(["User Views Dashboard in Month M (e.g. 2026-10)"]) --> ResolveBudget{"How is Category Budget resolved?"}

    ResolveBudget -->|1. Exact Month Key Exists| UseCurrent["Use budget for cat_Food_2026-10"]
    ResolveBudget -->|2. Not set for current month| CheckPrevious{"Did earlier month have budget?"}
    CheckPrevious -->|Yes| InheritLatest["Inherit latest previous budget<br/>(e.g. cat_Food_2026-09)"]
    CheckPrevious -->|No| CheckBaseline{"Baseline Category Entity budget > 0?"}
    CheckBaseline -->|Yes| UseBaseline["Use CategoryEntity.budgetPaise"]
    CheckBaseline -->|No| ZeroBudget["Allocated Budget = ₹0 (No limit)"]

    UseCurrent --> CalcSpend["Query all EXPENSE transactions in Month M for this Category"]
    InheritLatest --> CalcSpend
    UseBaseline --> CalcSpend
    ZeroBudget --> CalcSpend

    CalcSpend --> MathCheck{"Is Budget > ₹0?"}

    MathCheck -- "Budget == ₹0" --> RenderNoLimit["Render Card Without Budget Progress Bar<br/>• Shows total spent: '₹1,450'<br/>• Shows % of total monthly expenses: '35%'<br/>• Badge: 'No budget set'"]

    MathCheck -- "Budget > ₹0" --> CalcUsage["Calculate usage fraction:<br/>fraction = totalSpent / allocatedBudget"]
    
    CalcUsage --> ThresholdCheck{"Is totalSpent > allocatedBudget?"}

    ThresholdCheck -- "Under Budget (<= 100%)" --> RenderGreen["🟢 Safe / Normal UI State:<br/>• Progress Bar: Primary Green (0% to 100%)<br/>• Text: 'Left: ₹X'<br/>• Card Accent: Normal Clay"]

    ThresholdCheck -- "Over Budget (> 100%)" --> RenderRed["🔴 Overspent / Warning UI State:<br/>• Progress Bar: Clamped at 100% Crimson Red<br/>• Text: '⚠️ Over by ₹X'<br/>• Alert Badge: 'Budget Exceeded!'"]

    RenderNoLimit --> TapCard["User taps Category Card"]
    RenderGreen --> TapCard
    RenderRed --> TapCard

    TapCard --> OpenQuickDialog["Opens 1-Tap Category Budget Dialog"]
    OpenQuickDialog --> UserInputsNewBudget["User enters new budget (e.g. ₹2,500)"]
    UserInputsNewBudget --> SaveMonthBudget["Saves strictly to cat_Food_2026-10<br/>(Past months remain completely unaffected!)"]
    SaveMonthBudget --> RealtimeRefresh["Live UI Updates Instantly"]
```

---

## 2. Dynamic Daily "Safe-to-Spend" & Month Context Logic

Shows how the main Dashboard Balance card switches behavior between Current, Past, and Future months.

```mermaid
flowchart TD
    StartBalance(["User Selects Month M in Carousel"]) --> CompareMonth{"Compare Month M with Current Month"}

    CompareMonth -- "M == Current Month (Active)" --> ActiveFlow["Active Month Engine"]
    CompareMonth -- "M < Current Month (Past)" --> PastFlow["Historical Month Engine"]
    CompareMonth -- "M > Current Month (Future)" --> FutureFlow["Upcoming Month Engine"]

    subgraph ActiveMonth["Current Ongoing Month"]
        ActiveFlow --> GetDays["Calculate remaining days in current month D<br/>(Total days − Today + 1)"]
        GetDays --> GetCash["True Net Balance = All-Time Income − All-Time Expense"]
        GetCash --> CheckCash{"Is Net Balance > 0?"}

        CheckCash -- Yes --> CalcDaily["Daily Safe Allowance = Net Balance ÷ D"]
        CalcDaily --> GreenPill["🟢 Emerald Green Allowance Pill:<br/>'🎯 Safe to spend (D days left): ₹X / day'"]

        CheckCash -- No --> RedPill["🔴 Crimson Warning Pill:<br/>'⚠️ Wallet empty / overspent (₹0 / day)'"]
    end

    subgraph PastMonth["Past Historical Month"]
        PastFlow --> GetMonthNet["Month Net = Month Income − Month Expense"]
        GetMonthNet --> CheckPastSavings{"Is Month Net >= 0?"}

        CheckPastSavings -- "Income >= Expense" --> PastSaved["✨ Emerald Settled Pill:<br/>'✨ Month Settled • Net Saved: ₹X'"]
        CheckPastSavings -- "Expense > Income" --> PastDeficit["⚠️ Deficit Pill:<br/>'⚠️ Month Settled • Overspent: ₹X'"]
    end

    subgraph FutureMonth["Future Month"]
        FutureFlow --> ShowFuture["🗓️ Placeholder Pill:<br/>'Upcoming Month • No transactions yet'"]
    end
```

---

## 3. In-Line Arithmetic Calculator Flow

Shows how mathematical expressions (`150 + 200 * 2`) are processed live in any amount textfield.

```mermaid
flowchart TD
    UserInput["User types in Amount Field<br/>(e.g., '120 + 80 * 2')"] --> LiveParser["In-Line Math Parser Engine (MoneyUtils)"]

    LiveParser --> ValidateExpr{"Is expression valid & complete?"}

    ValidateExpr -- Valid --> EvalBODMAS["Evaluate using standard BODMAS precedence:<br/>1. Parentheses<br/>2. Multiplication & Division (*, /)<br/>3. Addition & Subtraction (+, -)"]
    EvalBODMAS --> LivePreview["Display Live Result Chip:<br/>'= ₹280' in bold accent"]

    ValidateExpr -- Trailing Operator / Incomplete --> WaitNext["Hide live preview chip gracefully<br/>(Wait for next digit)"]

    LiveParser --> OperatorButton["User taps Operator Pill (+, −, ×, ÷)"]
    OperatorButton --> AppendOp["Appends operator with clean spacing<br/>(Keeps cursor at end of expression)"]

    LivePreview --> SubmitTx["User taps 'Save' or 'Confirm'"]
    SubmitTx --> FinalEval["Evaluate expression to final Paise value: 28000L"]
    FinalEval --> PersistDB["Persist to SQLite Room Database"]
```

---

## 4. Friend Squad: "Gave" vs "Got" & Udhaar Debt Resolution

Shows how the two-way friend debt ledger works without distorting personal monthly wallet cashflow.

```mermaid
flowchart TD
    UserOpensFriend["User Opens Friend Ledger for 'Rahul'"] --> CurrentBalance["Read Unsettled Ledgers for Rahul"]
    CurrentBalance --> NetCalc["theyOweYou = Sum of 'Gave 🔴'<br/>youOweThem = Sum of 'Got 🟢'<br/>netPaise = theyOweYou − youOweThem"]

    NetCalc --> NetStatus{"Net Balance Status"}

    NetStatus -- "netPaise > 0" --> CardGreen["🟢 Green Status Badge:<br/>'₹X · You will get'"]
    NetStatus -- "netPaise < 0" --> CardRed["🔴 Red Status Badge:<br/>'₹X · You will give'"]
    NetStatus -- "netPaise == 0" --> CardSettled["✓ Settled Badge:<br/>'Settled ✓ (₹0)'"]

    UserOpensFriend --> UserAction{"User Action"}

    UserAction -- "User Lends Money" --> TapGave["Tap '+ Gave 🔴'"]
    TapGave --> AddLend["Insert FriendLedgerEntity:<br/>direction = THEY_OWE_YOU"]
    AddLend --> UpdateOwed["Increases 'They Owe You'<br/>(Personal income/expense untouched)"]

    UserAction -- "Friend Borrows / User Gets Money" --> TapGot["Tap '+ Got 🟢'"]
    TapGot --> AddBorrow["Insert FriendLedgerEntity:<br/>direction = YOU_OWE_THEM"]
    AddBorrow --> UpdateDebt["Increases 'You Owe Them'<br/>(Personal income/expense untouched)"]

    UserAction -- "Repayment Made" --> TapRepay["Tap 'Record Repayment'"]
    TapRepay --> RepayDir{"Who is repaying whom?"}
    RepayDir -- "Friend repays User" --> AddRepayGot["Adds offset YOU_OWE_THEM entry"]
    RepayDir -- "User repays Friend" --> AddRepayGave["Adds offset THEY_OWE_YOU entry"]
    AddRepayGot --> Rebalance["Net Balance moves toward ₹0"]
    AddRepayGave --> Rebalance

    UserAction -- "Full Settlement" --> TapSettleAll["Tap 'Settle All'"]
    TapSettleAll --> MarkAllSettled["Mark all entries isSettled = true<br/>(Clean slate, ₹0 balance)"]

    UserAction -- "Nudge Debt" --> TapWhatsApp["Tap 'WhatsApp Reminder'"]
    TapWhatsApp --> DrawCard["Procedurally draws humorous comic reminder card bitmap:<br/>• Pleading cartoon ghost mascot<br/>• Bold formatted amount badge<br/>• Custom witty note"]
    DrawCard --> LaunchWhatsApp["Opens WhatsApp targeted to friend's phone number"]
```

---

## 5. Bill Splitting: Payer & Custom Share Allocation Matrix

Shows how a group bill is divided and logged across both the wallet and squad ledgers.

```mermaid
flowchart TD
    StartBill(["Enter Bill Details:<br/>Title, Total Amount (₹), Category 🏷️"]) --> SelectPayer{"Who Paid Upfront?"}

    SelectPayer -- "You Paid 👑" --> PayerUser["User is Payer"]
    SelectPayer -- "Friend Paid 👤" --> PayerFriend["Friend is Payer (e.g. Aman)"]

    PayerUser --> ChooseMode{"Split Mode Selector"}
    PayerFriend --> ChooseMode

    ChooseMode -- "Equal Split ⚖️" --> CalcEqual["totalMembers = Friends Selected + 1 (User)<br/>shares = splitEqually(totalPaise, totalMembers)<br/>Remainder distributed to first members"]

    ChooseMode -- "Custom Shares 🍕" --> InputCustom["Display Individual Share Inputs for User and Friends"]
    InputCustom --> LiveSum["totalAllocated = userShare + sum(friendShares)"]
    LiveSum --> ValidateCustom{"totalAllocated == totalPaise?"}

    ValidateCustom -- "Allocated < Total" --> PillRemaining["⚠️ Warning: '₹X Remaining to allocate'"]
    ValidateCustom -- "Allocated > Total" --> PillExceeded["🛑 Error: 'Allocated exceeds bill by ₹X'"]
    ValidateCustom -- "Exact Match" --> PillMatched["✅ Pill: 'Exact Match • Ready to split'"]

    CalcEqual --> ConfirmSplit(["User Confirms Split"])
    PillMatched --> ConfirmSplit

    ConfirmSplit --> RouteExecution{"Who Paid?"}

    RouteExecution -- "User Paid" --> ExecUser["1. Record EXPENSE transaction in User Wallet:<br/>Amount = Full Total Bill<br/>Category = Selected Category 🏷️<br/>2. Add 'THEY_OWE_YOU' entry in each friend's ledger for their share"]

    RouteExecution -- "Friend Paid" --> ExecFriend["1. Add 'YOU_OWE_THEM' entry in Payer's Ledger for user's share<br/>2. Wallet cashflow untouched until user repays friend"]
```

---

## 6. Month History Boundary & Creation Month Guard

Shows how history navigation is protected against scrolling earlier than account inception.

```mermaid
flowchart TD
    Init(["App Launch"]) --> CheckStoredKey{"Is Account Creation Month stored in AppPreferences?"}

    CheckStoredKey -- Yes --> UseStored["creationMonth = SelectedMonth.fromMonthKey(stored)"]
    CheckStoredKey -- No --> ScanTxs["Scan earliest transaction timestamp in DB"]
    
    ScanTxs --> FoundTx{"Any transactions found?"}
    FoundTx -- Yes --> SetFromTx["creationMonth = Month of earliest transaction"]
    FoundTx -- No --> SetCurrent["creationMonth = Current Month"]
    SetFromTx --> SavePref["Store in AppPreferences permanently"]
    SetCurrent --> SavePref
    SavePref --> UseStored

    UseStored --> MonthCarousel["Month Selector Row UI"]
    MonthCarousel --> CheckPreviousNav{"selectedMonth > creationMonth?"}

    CheckPreviousNav -- Yes --> EnablePrev["'◀ Previous Month' button ENABLED<br/>User can browse previous month"]
    CheckPreviousNav -- No --> DisablePrev["'◀ Previous Month' button DISABLED & DIMMED<br/>Prevents navigating into non-existent empty history"]
```
