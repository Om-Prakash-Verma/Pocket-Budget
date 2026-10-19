# Architecture & Synchronization Guide 🏛️⚡

This document outlines the architectural patterns, data flow, synchronization mechanisms, and design systems implemented in **GhostPocket**.

---

## 1. Core Architectural Principles

- **Offline-First & Local SSOT**: The local Room database is the **single source of truth** for all screens and ViewModels. All user actions emit instantly to UI flows with 0ms network latency.
- **Strict Integer Currency**: All financial figures are stored in **integer paise** (`Long`) to prevent binary floating-point rounding inaccuracies.
- **Cost-Optimized Cloud Sync**:
  - **Single-Roundtrip 2-Way Sync**: Both local mutations (push) and remote updates (pull) are merged in a single HTTP `POST /api/sync` request.
  - **Only-When-Dirty Gating**: Network requests are suppressed unless local records have dirty flags (`isDirty == true`) or periodic thresholds elapse.
  - **Atomic Batching**: D1 SQLite statements are executed via `db.batch()` to guarantee all-or-nothing consistency.
- **Bento Grid + Claymorphism**: Tactile 3D soft clay elements, custom `ClayIconButton`, specular bevels, and fluid elevations without redundant system top bars.

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
                      └───> 2. Mark isDirty = true & Enqueue Sync
                                        │
                                        ▼
                         Cloudflare D1 / WorkManager
                         (POST /api/sync atomic payload)
```

---

## 3. Synchronization Pipeline

```
┌────────────────────────────────┐                 ┌─────────────────────────────┐
│          Android Client        │                 │     Cloudflare Worker       │
│                                │                 │      (Hono v4 + D1)         │
│  1. Check dirty entity count   │                 │                             │
│     (If 0 & recent, SKIP)      │                 │                             │
│  2. Build SyncRequest:         │  POST /api/sync │                             │
│     - Dirty Transactions       ├────────────────►│  3. Begin atomic batch:     │
│     - Dirty Friends & Ledgers  │                 │     - Upsert dirty entities │
│     - Dirty Categories/Budget  │                 │  4. Query remote updates:   │
│     - lastSyncTimestamp        │                 │     - WHERE updated_at > ts │
│                                │◄────────────────┤  5. Return SyncResponse     │
│  6. Clear dirty flags locally  │   JSON Payload  │                             │
│  7. Merge remote updates       │                 │                             │
└────────────────────────────────┘                 └─────────────────────────────┘
```

### Key Optimizations
1. **Network Constraint & Backoff**: WorkManager executes only when `NetworkType.CONNECTED` is present and retries with exponential backoff on failure.
2. **Operation Coalescing**:
   - If an entity is created and deleted prior to cloud sync, it is pruned locally without emitting cloud writes.
3. **Idempotence**:
   - Entities use client-generated deterministic UUIDs with `INSERT ... ON CONFLICT DO UPDATE`. Duplicate uploads cannot produce duplicate debts or transactions.
4. **Account Isolation**:
   - Calling `signOut()` immediately invokes `repository.clearLocalCache()`, purging local tables to prevent data crossover.
5. **Initial Crypt Restoration**:
   - When logging in with an existing account on a fresh device, `isInitialDataFetching` displays an animated Thinking Ghost with progress spinner until the crypt data is fully restored to Room.

---

## 4. WhatsApp Image Generator

[`WhatsAppReminderHelper`](file:///app/src/main/java/com/ghostpocket/app/util/WhatsAppReminderHelper.kt) renders a 1080x1350 card directly to local app storage using Android's native Canvas API:
- Hand-drawn cartoon ghost mascot with begging anime eyes.
- Speech bubble requesting repayment.
- Clear amount breakdown in INR (₹).
- Playful meme friendship disclaimer.
- Shared via Android `FileProvider` directly to WhatsApp with zero cloud storage fees.

---

## 5. UI & Design System

- **Monochrome Claymorphism**: Soft background gradients (`#FFFFFF` to `#F1F1F6` in light, `#24242B` to `#19191E` in dark) with dual-layer shadows.
- **Unified Bento Headers**:
  - `Boo! 👋` greeting + bold `GhostPocket` display title.
  - Compact `ClayIconButton` (48x48dp, 16dp radius) for Settings access on every screen.
  - Interactive Mascot Avatar card (48x48dp) reflecting real-time budget mood.
- **Window Insets Protection**:
  - `statusBarsPadding()` ensures top-of-screen content never overlaps with camera cutouts or status bars across Android 8 through 15+.
