# Changelog

All notable changes to the GhostPocket application are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [2.0.1] - 2026-10-23

### Added
- **Account Deletion & Data Purge**: Added a destructive "Delete Account & Purge Data ⚠️" feature in the Settings tab. Securely purges all user records from both the device's local Room SQLite database and the Cloudflare D1 edge database (`DELETE /api/auth/account`), clears session credentials, and returns cleanly to the Welcome screen.
- **Direct GitHub Releases Link**: Added a quick "Releases 🌐" navigation button in the Settings tab to let users easily inspect the official GitHub releases and updates.
- **Production Version Management System**: Standardized Semantic Versioning (`MAJOR.MINOR.PATCH`) with release automation script (`scripts/release.ps1`), GitHub Actions CI release workflow (`.github/workflows/release.yml`), and standardized APK generation/naming (`GhostPocket-v<version>.apk`).
- **Update System Unit Tests**: Added unit test suite (`UpdateSystemTest.kt`) verifying SemVer increment checks and version regex patterns.

### Changed
- Centralized in-app version configuration across Gradle build scripts and UI metadata.

---

## [2.0.0] - 2026-10-23

### Added
- **Clean Slate Baseline**: Official major milestone release establishing GhostPocket v2.0.0.
- **Pure Liquid Cashflow Engine**: Net Balance strictly calculates actual liquidity ($\text{All-Time Income} - \text{All-Time Expenses}$) with zero artificial budget additions.
- **In-Line Math Calculator**: Type arithmetic expressions directly into transaction amount fields (`50 + 20 * 2 = 90`) with live evaluation previews and operator pills (`+`, `−`, `×`, `÷`, `=`).
- **Quick Preset Amount Chips**: Add common increments (`+₹10`, `+₹20`, `+₹50`, `+₹100`, `+₹500`) with a single tap.
- **Ghost Squad Debt Tracking**: Simplified peer debt accounting using unambiguous "You Gave 🔴" vs "You Got 🟢" actions, equal & custom bill splitting, and 1-tap debt settlement.
- **WhatsApp-Style Silent Cloud Backup**: 100% offline-first operation. WorkManager batches all records and syncs to Cloudflare D1 edge database once every 24 hours under idle/battery-safe conditions.
- **Interactive App Tour**: 5-step modal guide explaining cashflow, in-line calculations, month-scoped budgets, friend debt mechanics, and offline-first privacy.
- **Direct In-App APK Updater**: Seamless background check against GitHub Releases API with progress tracking, SHA-256 verification, and PackageInstaller integration.

### Changed
- Standardized release versioning to Semantic Versioning (`MAJOR.MINOR.PATCH`) with centralized version management in `app/build.gradle.kts`.
- Standardized APK artifact naming to `GhostPocket-v${versionName}.apk`.
- Optimized Cloudflare sync protocol to a single atomic roundtrip (`POST /api/sync`) using `db.batch()`.
- Modernized theme system with strict obsidian dark, light, and system-adaptive modes.

### Fixed
- **Historical Month Time Drift**: Transactions added while inspecting past or future months automatically anchor to that month (`noon on 1st of month`) with an indicator chip.
- **Account Creation Month Lockout**: Cloud-synced historical transactions dynamically expand calendar navigation bounds backwards.
- **Category Rename Budget Orphan**: Renaming a spending realm re-keys and migrates all month-scoped budgets (`cat_<old>_*` to `cat_<new>_*`).
- **Friend Cascade Deletion**: Deleting a squad member cascades to delete or flag for deletion all associated ledger entries.
- **Category Outflow Reconciliation**: Expenses with blank or unselected categories fall back to `"Other"` so category totals match monthly expense totals exactly.

### Removed
- Completely removed legacy Google Play Services and Firebase dependencies (`firebase-bom`, `firebase-auth`, `firebase-firestore`, `google-services`), reducing APK download size by ~4.7 MB (~18.5 MB total).
- Removed the aggressive 3-second `ON_STOP` exit sync to ensure zero unnecessary network calls on app minimization.
