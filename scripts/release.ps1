param (
    [Parameter(Mandatory = $true, Position = 0)]
    [string]$TargetVersion,

    [switch]$Publish
)

$ErrorActionPreference = "Stop"

$repoRoot = (git rev-parse --show-toplevel).Trim()
Set-Location $repoRoot

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "GhostPocket Release Automation - Target: v$TargetVersion" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

# 1. Validate Semantic Versioning Format (MAJOR.MINOR.PATCH)
$semverPattern = '^\d+\.\d+\.\d+$'
if ($TargetVersion -notmatch $semverPattern) {
    Write-Error "Invalid semantic version '$TargetVersion'. Must follow format MAJOR.MINOR.PATCH (e.g. 2.0.1)."
    exit 1
}

$targetParts = $TargetVersion.Split('.')
$targetMajor = [int]$targetParts[0]
$targetMinor = [int]$targetParts[1]
$targetPatch = [int]$targetParts[2]

# 2. Extract Current Version from app/build.gradle.kts
$gradleFile = Join-Path $repoRoot "app/build.gradle.kts"
$gradleContent = Get-Content $gradleFile -Raw

if ($gradleContent -match 'versionCode\s*=\s*(\d+)') {
    $currentVersionCode = [int]$Matches[1]
} else {
    Write-Error "Could not parse versionCode from $gradleFile"
    exit 1
}

if ($gradleContent -match 'versionName\s*=\s*"(\d+\.\d+\.\d+)"') {
    $currentVersionName = $Matches[1]
} else {
    Write-Error "Could not parse versionName from $gradleFile"
    exit 1
}

Write-Host "Current Version: $currentVersionName (versionCode: $currentVersionCode)" -ForegroundColor Yellow

$currentParts = $currentVersionName.Split('.')
$currentMajor = [int]$currentParts[0]
$currentMinor = [int]$currentParts[1]
$currentPatch = [int]$currentParts[2]

# 3. Ensure Target Version is strictly newer
$isGreater = $false
if ($targetMajor -gt $currentMajor) {
    $isGreater = $true
} elseif ($targetMajor -eq $currentMajor) {
    if ($targetMinor -gt $currentMinor) {
        $isGreater = $true
    } elseif ($targetMinor -eq $currentMinor) {
        if ($targetPatch -gt $currentPatch) {
            $isGreater = $true
        }
    }
}

if (-not $isGreater) {
    Write-Error "Target version '$TargetVersion' must be strictly newer than current version '$currentVersionName'."
    exit 1
}

# 4. Check if Git Tag already exists locally or remotely
$targetTag = "v$TargetVersion"
$existingLocalTag = git tag -l $targetTag
if ($existingLocalTag) {
    Write-Error "Git tag '$targetTag' already exists locally! Aborting."
    exit 1
}

$remoteTagCheck = git ls-remote --tags origin "refs/tags/$targetTag"
if ($remoteTagCheck) {
    Write-Error "Git tag '$targetTag' already exists on remote origin! Aborting."
    exit 1
}

# 5. Check if GitHub release already exists
$existingRelease = gh release view $targetTag 2>$null
if ($LASTEXITCODE -eq 0 -and $existingRelease) {
    Write-Error "GitHub release '$targetTag' already exists! Aborting."
    exit 1
}

Write-Host "Version validation passed: $currentVersionName -> $TargetVersion" -ForegroundColor Green

# 6. Compute Next versionCode
$nextVersionCode = $currentVersionCode + 1
Write-Host "Next versionCode: $nextVersionCode" -ForegroundColor Green

# 7. Update Android build.gradle.kts
$newGradleContent = $gradleContent -replace 'versionCode\s*=\s*\d+', "versionCode = $nextVersionCode"
$newGradleContent = $newGradleContent -replace 'versionName\s*=\s*"\d+\.\d+\.\d+"', "versionName = `"$TargetVersion`""
Set-Content -Path $gradleFile -Value $newGradleContent -NoNewline

# 8. Update SettingsScreen.kt and GhostPocketViewModel.kt defaults
$settingsFile = Join-Path $repoRoot "app/src/main/java/com/ghostpocket/app/ui/screens/SettingsScreen.kt"
if (Test-Path $settingsFile) {
    (Get-Content $settingsFile -Raw) `
        -replace 'currentVersionName:\s*String\s*=\s*"\d+\.\d+\.\d+"', "currentVersionName: String = `"$TargetVersion`"" `
        -replace 'currentVersionCode:\s*Int\s*=\s*\d+', "currentVersionCode: Int = $nextVersionCode" |
        Set-Content -Path $settingsFile -NoNewline
}

$vmFile = Join-Path $repoRoot "app/src/main/java/com/ghostpocket/app/viewmodel/GhostPocketViewModel.kt"
if (Test-Path $vmFile) {
    (Get-Content $vmFile -Raw) `
        -replace 'Pair\("\d+\.\d+\.\d+",\s*\d+\)', "Pair(`"$TargetVersion`", $nextVersionCode)" |
        Set-Content -Path $vmFile -NoNewline
}

Write-Host "Updated build configuration files to $TargetVersion ($nextVersionCode)." -ForegroundColor Green

# 9. Ensure JDK 21 Environment
if (-not $env:JAVA_HOME -and (Test-Path "C:\Users\ompra\jdk-21.0.6+7")) {
    $env:JAVA_HOME = "C:\Users\ompra\jdk-21.0.6+7"
}

# 10. Run Unit Tests
Write-Host "Running unit tests..." -ForegroundColor Yellow
& .\gradlew.bat testDebugUnitTest
if ($LASTEXITCODE -ne 0) {
    Write-Error "Unit tests failed!"
    exit 1
}

# 11. Assemble Debug APK
Write-Host "Building APK..." -ForegroundColor Yellow
& .\gradlew.bat assembleDebug
if ($LASTEXITCODE -ne 0) {
    Write-Error "Gradle build failed!"
    exit 1
}

# 12. Verify APK Existence and Standardized Name
$apkExpectedPath = Join-Path $repoRoot "app/build/outputs/apk/debug/GhostPocket-v$TargetVersion.apk"
if (-not (Test-Path $apkExpectedPath)) {
    $fallbackApk = Join-Path $repoRoot "app/build/outputs/apk/debug/app-debug.apk"
    if (Test-Path $fallbackApk) {
        Copy-Item $fallbackApk $apkExpectedPath
    } else {
        Write-Error "APK file not found at $apkExpectedPath"
        exit 1
    }
}

$apkSizeMB = [math]::Round(((Get-Item $apkExpectedPath).Length / 1MB), 2)
Write-Host "Generated APK: GhostPocket-v$TargetVersion.apk ($apkSizeMB MB)" -ForegroundColor Green

# 13. Extract Release Notes from CHANGELOG.md
$changelogFile = Join-Path $repoRoot "CHANGELOG.md"
$releaseNotes = ""
if (Test-Path $changelogFile) {
    $clContent = Get-Content $changelogFile -Raw
    $escapedVer = [regex]::Escape($TargetVersion)
    if ($clContent -match "(?s)## \[$escapedVer\].*?(?=(## \[\d|\Z))") {
        $releaseNotes = $Matches[0].Trim()
    }
}

if (-not $releaseNotes) {
    $releaseNotes = "### GhostPocket v$TargetVersion`n`n- **versionCode**: $nextVersionCode`n- Built on $(Get-Date -Format 'yyyy-MM-dd')"
}

$tempNotesFile = Join-Path $repoRoot "release_notes_temp.md"
Set-Content -Path $tempNotesFile -Value $releaseNotes -Encoding UTF8

Write-Host "Release Notes prepared from CHANGELOG.md" -ForegroundColor Cyan

# 14. Git Commit, Tag, Push, and Release (Only if -Publish is passed)
if ($Publish) {
    Write-Host "Publishing release $targetTag to GitHub..." -ForegroundColor Yellow
    
    git add app/build.gradle.kts app/src CHANGELOG.md
    git commit -m "feat: release $targetTag (versionCode $nextVersionCode)"
    git push origin main
    
    git tag -a $targetTag -m "GhostPocket $targetTag"
    git push origin $targetTag
    
    gh release create $targetTag $apkExpectedPath `
        --title "GhostPocket $targetTag" `
        --notes-file $tempNotesFile
        
    Remove-Item $tempNotesFile -ErrorAction SilentlyContinue
    Write-Host "==========================================================" -ForegroundColor Green
    Write-Host "GhostPocket $targetTag successfully published on GitHub!" -ForegroundColor Green
    Write-Host "==========================================================" -ForegroundColor Green
} else {
    Remove-Item $tempNotesFile -ErrorAction SilentlyContinue
    Write-Host "==========================================================" -ForegroundColor Yellow
    Write-Host "Local build and version bump to $targetTag completed successfully!" -ForegroundColor Yellow
    Write-Host "DRY RUN MODE: No Git commit, tag, or GitHub release was published." -ForegroundColor Yellow
    Write-Host "To publish, execute: .\scripts\release.ps1 -TargetVersion `"$TargetVersion`" -Publish" -ForegroundColor Yellow
    Write-Host "==========================================================" -ForegroundColor Yellow
}
