# 🚀 GhostPocket Release Guide

This document outlines the standard procedure for creating and publishing new production releases of **GhostPocket**.

GhostPocket includes an integrated **In-App Update System** that monitors [GitHub Releases](https://github.com/Om-Prakash-Verma/Pocket-Budget/releases). Every release published following these steps will automatically notify existing users and allow them to download and install updates directly within the application.

---

## 📋 Prerequisites

Before publishing a release, ensure you have:
1. **JDK 21** installed and configured (e.g. `C:\Users\ompra\jdk-21.0.6+7`).
2. **GitHub CLI (`gh`)** installed and authenticated:
   ```cmd
   gh auth status
   ```
3. A clean Git working tree.

---

## 🏷️ Versioning Rules

GhostPocket uses two version numbers located in [`app/build.gradle.kts`](file:///d:/Github/Pocket-Budget/app/build.gradle.kts):

```kotlin
defaultConfig {
    versionCode = 3          // Monotonically increasing integer (e.g., 2 -> 3 -> 4)
    versionName = "1.4.0"    // Semantic version string (e.g., "1.4.0" -> "1.4.1")
}
```

> [!IMPORTANT]
> - **`versionCode`** MUST always be incremented by at least `1` with every release. The Android OS and GhostPocket's in-app update engine use this number to determine if an installed APK is older than the release.
> - **`versionName`** follows Semantic Versioning (`MAJOR.MINOR.PATCH`).

---

## ⚡ Method 1: Direct Release via Command Line (Recommended)

This is the fastest and most reliable method to build and publish a release in under 2 minutes.

### Step 1: Increment Version Numbers
Edit [`app/build.gradle.kts`](file:///d:/Github/Pocket-Budget/app/build.gradle.kts):
- Set `versionCode` to the next integer (e.g., `4`).
- Set `versionName` to your new version (e.g., `"1.4.1"`).

### Step 2: Build the APK
Open PowerShell or CMD in the project root:
```powershell
$env:JAVA_HOME = "C:\Users\ompra\jdk-21.0.6+7"
.\gradlew.bat assembleDebug
```
The output APK will be generated at:
`app\build\outputs\apk\debug\GhostPocket-v<versionName>.apk`

### Step 3: Compute the SHA-256 Checksum
Run this command in PowerShell to compute the cryptographic checksum of your APK:
```powershell
(Get-FileHash app\build\outputs\apk\debug\GhostPocket-v1.4.1.apk -Algorithm SHA256).Hash
```
*(Copy the resulting 64-character hash string).*

### Step 4: Commit and Push Changes
```bash
git add app/build.gradle.kts
git commit -m "chore(release): bump version to v1.4.1 (code 4)"
git push origin main
```

### Step 5: Publish the Release via GitHub CLI
Execute the `gh release create` command:

```cmd
gh release create v1.4.1 app\build\outputs\apk\debug\GhostPocket-v1.4.1.apk --title "GhostPocket v1.4.1 🚀👻" --notes "### GhostPocket v1.4.1 Release Notes`n`n- **versionCode**: 4`n- **SHA-256**: <PASTE_YOUR_SHA256_HASH_HERE>`n`n#### What's New:`n- Describe your new feature here`n- Describe bug fixes here`n`nSHA256: <PASTE_YOUR_SHA256_HASH_HERE>"
```

---

## 🤖 Method 2: Automated Release via GitHub Actions

The repository includes an automated workflow at [`.github/workflows/release.yml`](file:///d:/Github/Pocket-Budget/.github/workflows/release.yml).

### Steps:
1. Update `versionCode` and `versionName` in [`app/build.gradle.kts`](file:///d:/Github/Pocket-Budget/app/build.gradle.kts).
2. Commit and push your changes to `main`:
   ```bash
   git add app/build.gradle.kts
   git commit -m "chore(release): prepare v1.4.1"
   git push origin main
   ```
3. Create and push a Git version tag:
   ```bash
   git tag v1.4.1
   git push origin v1.4.1
   ```
4. GitHub Actions will automatically:
   - Check out code and set up JDK 21.
   - Compile the APK with Gradle.
   - Calculate the SHA-256 checksum.
   - Publish the GitHub Release with the APK and `.sha256` asset attached.

---

## 🌐 Method 3: Manual Release via GitHub Web Interface

If you prefer using the browser:
1. Build the APK locally using Step 2 above and calculate its SHA-256 hash.
2. Go to **[GitHub Releases](https://github.com/Om-Prakash-Verma/Pocket-Budget/releases)**.
3. Click **"Draft a new release"**.
4. Fill in the release details:
   - **Choose a tag**: Type `v1.4.1` (or your version) and select *Create new tag*.
   - **Release title**: `GhostPocket v1.4.1 🚀👻`
   - **Description**: Ensure it contains the `versionCode` and `SHA256` hash:
     ```markdown
     ### GhostPocket v1.4.1
     - **versionCode**: 4
     - **SHA-256**: <your_sha256_hash>
     
     #### What's New:
     - Bug fixes and optimizations.
     
     SHA256: <your_sha256_hash>
     ```
   - **Attach binaries**: Drag and drop `GhostPocket-v1.4.1.apk` into the file upload box.
5. Click **"Publish release"**.

---

## 🔒 Mandatory Updates Flag

If a release contains critical bug fixes or security changes that users **must** install before continuing, include `[mandatory]` or `mandatory: true` in the release notes body:

```markdown
### Critical Update [mandatory]
- Database schema migration.
```

When GhostPocket detects this flag:
- The update dialog displays a **"⚠️ Mandatory Update Required"** banner.
- The "Later" button is hidden.
- The user must update to continue using the application.

---

## 🧪 Verifying In-App Updates

Once a release is published:
1. Open an existing build of GhostPocket on your device.
2. Navigate to **⚙️ Settings -> Ghost Updates 🚀**.
3. Tap **"Check for Updates 🔄"**.
4. GhostPocket will query the GitHub API, present the update modal with your changelog, download the APK, verify its checksum, and launch the installer.
