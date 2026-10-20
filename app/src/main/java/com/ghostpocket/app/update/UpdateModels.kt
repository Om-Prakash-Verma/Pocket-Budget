package com.ghostpocket.app.update

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.io.File

@Serializable
data class GitHubReleaseDto(
    @SerialName("tag_name") val tagName: String = "",
    @SerialName("name") val name: String? = null,
    @SerialName("body") val body: String? = null,
    @SerialName("draft") val draft: Boolean = false,
    @SerialName("prerelease") val prerelease: Boolean = false,
    @SerialName("published_at") val publishedAt: String? = null,
    @SerialName("assets") val assets: List<GitHubAssetDto> = emptyList()
)

@Serializable
data class GitHubAssetDto(
    @SerialName("name") val name: String = "",
    @SerialName("browser_download_url") val downloadUrl: String = "",
    @SerialName("size") val size: Long = 0L,
    @SerialName("content_type") val contentType: String? = null
)

data class AppUpdateInfo(
    val latestVersionName: String,
    val latestVersionCode: Int,
    val releaseTag: String,
    val releaseNotes: String,
    val apkDownloadUrl: String,
    val apkFileName: String,
    val apkSize: Long,
    val expectedSha256: String?, // Null when not supplied by release
    val isMandatory: Boolean
)

sealed class UpdateCheckResult {
    data class UpdateAvailable(val info: AppUpdateInfo) : UpdateCheckResult()
    data object UpToDate : UpdateCheckResult()
    data class Error(val message: String) : UpdateCheckResult()
}

sealed class DownloadState {
    data object Idle : DownloadState()
    data class Downloading(
        val progressPercent: Int,
        val bytesDownloaded: Long,
        val totalBytes: Long
    ) : DownloadState()
    data object Verifying : DownloadState()
    data class ReadyToInstall(
        val apkFile: File,
        val verifiedChecksum: Boolean
    ) : DownloadState()
    data class Failed(val error: String) : DownloadState()
}
