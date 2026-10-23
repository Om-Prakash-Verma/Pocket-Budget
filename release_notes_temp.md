## [2.0.1] - 2026-10-23

### Added
- **Account Deletion & Data Purge**: Added a destructive "Delete Account & Purge Data âš ï¸" feature in the Settings tab. Securely purges all user records from both the device's local Room SQLite database and the Cloudflare D1 edge database (`DELETE /api/auth/account`), clears session credentials, and returns cleanly to the Welcome screen.
- **Direct GitHub Releases Link**: Added a quick "Releases ðŸŒ" navigation button in the Settings tab to let users easily inspect the official GitHub releases and updates.
- **Production Version Management System**: Standardized Semantic Versioning (`MAJOR.MINOR.PATCH`) with release automation script (`scripts/release.ps1`), GitHub Actions CI release workflow (`.github/workflows/release.yml`), and standardized APK generation/naming (`GhostPocket-v<version>.apk`).
- **Update System Unit Tests**: Added unit test suite (`UpdateSystemTest.kt`) verifying SemVer increment checks and version regex patterns.

### Changed
- Centralized in-app version configuration across Gradle build scripts and UI metadata.

---
