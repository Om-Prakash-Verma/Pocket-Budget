package com.ghostpocket.app.update

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

class UpdateManager(private val context: Context) {

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(
        UpdateConfig.PREFS_NAME,
        Context.MODE_PRIVATE
    )

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    private var activeDownloadCall: okhttp3.Call? = null

    /**
     * Gets current installed application version info.
     */
    fun getCurrentVersionInfo(): Pair<String, Int> {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val versionName = pInfo.versionName ?: "1.0.0"
            val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pInfo.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                pInfo.versionCode
            }
            Pair(versionName, versionCode)
        } catch (e: Exception) {
            Pair("1.0.0", 1)
        }
    }

    /**
     * Checks GitHub for the latest release.
     * @param isManual True if initiated by the user (bypasses throttle).
     */
    suspend fun checkForUpdates(isManual: Boolean = false): UpdateCheckResult = withContext(Dispatchers.IO) {
        val lastCheck = prefs.getLong(UpdateConfig.KEY_LAST_CHECK_TIMESTAMP, 0L)
        val now = System.currentTimeMillis()

        if (!isManual && (now - lastCheck) < UpdateConfig.AUTO_CHECK_THROTTLE_MS) {
            return@withContext UpdateCheckResult.UpToDate
        }

        try {
            val request = Request.Builder()
                .url(UpdateConfig.LATEST_RELEASE_API_URL)
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "GhostPocket-App")
                .build()

            val response = httpClient.newCall(request).execute()
            prefs.edit().putLong(UpdateConfig.KEY_LAST_CHECK_TIMESTAMP, now).apply()

            if (response.code == 404) {
                return@withContext UpdateCheckResult.UpToDate
            }

            if (response.code == 403 || response.code == 429) {
                return@withContext UpdateCheckResult.Error("GitHub API rate limit reached. Please try again later.")
            }

            if (!response.isSuccessful) {
                return@withContext UpdateCheckResult.Error("Failed to check for updates (HTTP ${response.code})")
            }

            val bodyString = response.body?.string()
                ?: return@withContext UpdateCheckResult.Error("Empty response from GitHub.")

            val release = json.decodeFromString<GitHubReleaseDto>(bodyString)

            // Ignore drafts or prereleases
            if (release.draft || release.prerelease) {
                return@withContext UpdateCheckResult.UpToDate
            }

            // Find APK asset
            val apkAsset = release.assets.firstOrNull { asset ->
                asset.name.endsWith(".apk", ignoreCase = true)
            } ?: return@withContext UpdateCheckResult.UpToDate

            val (currentVersionName, currentVersionCode) = getCurrentVersionInfo()

            val releaseNotes = release.body.orEmpty()
            val releaseTag = release.tagName
            val remoteVersionName = releaseTag.trim().removePrefix("v").removePrefix("V")

            // Parse remote versionCode from release notes if explicitly specified
            val explicitRemoteVersionCode = UpdateUtils.extractVersionCode(releaseNotes)

            // Determine if update is available:
            // 1. If remoteVersionName is higher than current (e.g. 1.4.1 > 1.4.0), check versionCode
            // 2. If remoteVersionName is identical (1.4.0 == 1.4.0), the user already has this release, so do not prompt.
            val isSemanticHigher = UpdateUtils.isSemanticVersionNewer(remoteVersionName, currentVersionName)
            val isNewer = if (isSemanticHigher) {
                if (explicitRemoteVersionCode != null) {
                    explicitRemoteVersionCode > currentVersionCode
                } else {
                    true
                }
            } else {
                false
            }

            if (!isNewer) {
                return@withContext UpdateCheckResult.UpToDate
            }

            // Extract SHA-256 if available
            val expectedSha256 = UpdateUtils.extractSha256(releaseNotes, release.assets, httpClient)

            // Check if mandatory
            val isMandatory = releaseNotes.contains("[mandatory]", ignoreCase = true) ||
                    releaseNotes.contains("mandatory: true", ignoreCase = true) ||
                    releaseNotes.contains("mandatory:true", ignoreCase = true)

            val updateInfo = AppUpdateInfo(
                latestVersionName = remoteVersionName,
                latestVersionCode = explicitRemoteVersionCode ?: (currentVersionCode + 1),
                releaseTag = releaseTag,
                releaseNotes = releaseNotes,
                apkDownloadUrl = apkAsset.downloadUrl,
                apkFileName = apkAsset.name,
                apkSize = apkAsset.size,
                expectedSha256 = expectedSha256,
                isMandatory = isMandatory
            )

            UpdateCheckResult.UpdateAvailable(updateInfo)
        } catch (e: Exception) {
            UpdateCheckResult.Error(e.message ?: "Network error while checking for updates.")
        }
    }

    /**
     * Downloads and verifies the APK.
     */
    suspend fun downloadApk(updateInfo: AppUpdateInfo) = withContext(Dispatchers.IO) {
        _downloadState.value = DownloadState.Downloading(0, 0, updateInfo.apkSize)

        val updateDir = getUpdateDirectory()
        // Clean old update files
        updateDir.listFiles()?.forEach { it.delete() }

        val tempFile = File(updateDir, "${updateInfo.apkFileName}.tmp")
        val finalFile = File(updateDir, updateInfo.apkFileName)

        try {
            val request = Request.Builder()
                .url(updateInfo.apkDownloadUrl)
                .header("User-Agent", "GhostPocket-App")
                .build()

            val call = httpClient.newCall(request)
            activeDownloadCall = call
            val response = call.execute()

            if (!response.isSuccessful) {
                _downloadState.value = DownloadState.Failed("Download failed: HTTP ${response.code}")
                return@withContext
            }

            val body = response.body ?: run {
                _downloadState.value = DownloadState.Failed("Download body was empty.")
                return@withContext
            }

            val totalBytes = if (body.contentLength() > 0) body.contentLength() else updateInfo.apkSize
            var bytesRead = 0L

            body.byteStream().use { input: InputStream ->
                FileOutputStream(tempFile).use { output: FileOutputStream ->
                    val buffer = ByteArray(8 * 1024)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        bytesRead += read
                        val progress = if (totalBytes > 0) ((bytesRead * 100) / totalBytes).toInt() else 0
                        _downloadState.value = DownloadState.Downloading(
                            progressPercent = progress.coerceIn(0, 100),
                            bytesDownloaded = bytesRead,
                            totalBytes = totalBytes
                        )
                    }
                    output.flush()
                }
            }

            activeDownloadCall = null
            _downloadState.value = DownloadState.Verifying

            // 1. Verify Checksum if provided
            var checksumVerified = false
            if (!updateInfo.expectedSha256.isNullOrBlank()) {
                val calculatedSha256 = UpdateUtils.calculateSha256(tempFile)
                if (!calculatedSha256.equals(updateInfo.expectedSha256, ignoreCase = true)) {
                    tempFile.delete()
                    _downloadState.value = DownloadState.Failed(
                        "Security verification failed: SHA-256 checksum mismatch."
                    )
                    return@withContext
                }
                checksumVerified = true
            }

            // 2. Validate APK integrity & Package Name
            val archiveInfo = context.packageManager.getPackageArchiveInfo(
                tempFile.absolutePath,
                PackageManager.GET_ACTIVITIES
            )

            if (archiveInfo == null) {
                tempFile.delete()
                _downloadState.value = DownloadState.Failed("Corrupted APK file. Cannot be installed.")
                return@withContext
            }

            if (archiveInfo.packageName != context.packageName) {
                tempFile.delete()
                _downloadState.value = DownloadState.Failed(
                    "Invalid APK: Package '${archiveInfo.packageName}' does not match '${context.packageName}'."
                )
                return@withContext
            }

            // Rename temp to final
            if (finalFile.exists()) finalFile.delete()
            if (!tempFile.renameTo(finalFile)) {
                // Fallback copy if rename fails across partitions
                tempFile.copyTo(finalFile, overwrite = true)
                tempFile.delete()
            }

            _downloadState.value = DownloadState.ReadyToInstall(finalFile, checksumVerified)
        } catch (e: Exception) {
            tempFile.delete()
            _downloadState.value = DownloadState.Failed(e.message ?: "Download interrupted.")
        }
    }

    /**
     * Cancels active download if running.
     */
    fun cancelDownload() {
        try {
            activeDownloadCall?.cancel()
            activeDownloadCall = null
            val updateDir = getUpdateDirectory()
            updateDir.listFiles()?.forEach { if (it.name.endsWith(".tmp")) it.delete() }
        } catch (ignored: Exception) {}
        _downloadState.value = DownloadState.Idle
    }

    /**
     * Clears download state back to Idle.
     */
    fun resetDownloadState() {
        _downloadState.value = DownloadState.Idle
    }

    /**
     * Initiates the APK installation via Android Package Installer.
     * Handles unknown sources permission on Android 8.0+.
     */
    fun installApk(apkFile: File): Boolean {
        if (!apkFile.exists()) {
            _downloadState.value = DownloadState.Failed("APK file not found.")
            return false
        }

        // On Android 8.0+ (Oreo+), check if app is allowed to request package installs
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!context.packageManager.canRequestPackageInstalls()) {
                val settingsIntent = Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${context.packageName}")
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(settingsIntent)
                return false
            }
        }

        return try {
            val apkUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }

            // Explicitly grant read permission to resolving package installer handlers
            val resolveInfoList = context.packageManager.queryIntentActivities(
                installIntent,
                PackageManager.MATCH_DEFAULT_ONLY
            )
            for (resolveInfo in resolveInfoList) {
                val packageName = resolveInfo.activityInfo.packageName
                context.grantUriPermission(
                    packageName,
                    apkUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }

            context.startActivity(installIntent)
            true
        } catch (e: Exception) {
            _downloadState.value = DownloadState.Failed("Could not launch package installer: ${e.message}")
            false
        }
    }

    private fun getUpdateDirectory(): File {
        val dir = File(context.cacheDir, "updates")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }
}
