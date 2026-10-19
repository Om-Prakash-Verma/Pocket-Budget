# Architecture & Synchronization Guide 🏛️⚡

This document outlines the architectural patterns, data flow, and synchronization mechanisms implemented in **GhostPocket**.

---

## 1. Core Principles

- **Offline-First & Local SSOT**: The local Room database is the **single source of truth** for all screens and ViewModels. The user can perform any action with zero network latency.
- **Strict Integer Currency**: All financial values are stored in **integer paise** (`Long`) to prevent binary floating-point rounding inaccuracies.
- **Spark-Tier Quota Optimization**:
  - Unnecessary reads/writes are eliminated.
  - Analytics and balances are calculated locally.
  - Updates are batched and coalesced before uploading.

---

## 2. Unidirectional Data Flow (UDF)

```
       User Action (Click, Input, Dismiss)
                     │
                     ▼
             GhostPocketViewModel
                     │
                     ▼
           GhostPocketRepository
                     │
                     ├───> 1. Persist to Room (Instant UI flow emission)
                     └───> 2. Enqueue WorkManager SyncWorker (Debounced 3-5s)
                                       │
                                       ▼
                             Cloud Firestore Batch
```

---

## 3. Synchronization Pipeline (`SyncWorker`)

1. **Network Constraint & Backoff**: WorkManager executes only when `NetworkType.CONNECTED` is present and retries with exponential backoff on failure.
2. **Operation Coalescing**:
   - If a transaction, friend, or category is created and deleted prior to a cloud sync, it is removed locally without emitting cloud writes.
3. **Idempotence**:
   - Documents use client-generated deterministic UUIDs with `SetOptions.merge()`. Duplicate uploads cannot produce duplicate debts or transactions.
4. **Relational Dependency Order**:
   - `Friends` $\rightarrow$ `Friend Ledgers` $\rightarrow$ `Transactions` $\rightarrow$ `Categories` $\rightarrow$ `Settings`.
5. **Local Cloud Confirmation**:
   - Room entities transition to `SyncStatus.SYNCED` only after Firestore commits the batch.
6. **Account Isolation**:
   - Calling `signOut()` immediately invokes `repository.clearLocalCache()`, purging local tables to prevent data crossover. On next sign-in, remote documents are pulled cleanly into Room.

---

## 4. WhatsApp Image Generator

[`WhatsAppReminderHelper`](file:///app/src/main/java/com/ghostpocket/app/util/WhatsAppReminderHelper.kt) renders a 1080x1350 bitmap directly to disk with Android `FileProvider`, avoiding cloud storage fees and delivering an instant reminder card to WhatsApp.
