package com.ghostpocket.app.update

import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.security.MessageDigest
import java.util.regex.Pattern

/**
 * UpdateUtils provides helper methods for update parsing, checksum calculation,
 * and semantic version comparison.
 */
object UpdateUtils {

    /**
     * Extracts an optional versionCode integer from GitHub release markdown body.
     */
    fun extractVersionCode(releaseNotes: String): Int? {
        val cleanNotes = releaseNotes.replace("*", "").replace("`", "")
        val pattern = Pattern.compile("(?i)(?:version[_\\s-]?code|build[_\\s-]?number|vc)\\s*[:=]\\s*(\\d+)")
        val matcher = pattern.matcher(cleanNotes)
        if (matcher.find()) {
            return matcher.group(1)?.toIntOrNull()
        }
        return null
    }

    /**
     * Extracts expected SHA-256 hash from release notes or downloads it from a checksum asset.
     */
    fun extractSha256(
        releaseNotes: String,
        assets: List<GitHubAssetDto>,
        httpClient: OkHttpClient
    ): String? {
        // 1. Look in release notes for SHA256: <64 hex>
        val pattern = Pattern.compile("(?i)(?:sha-?256|checksum)[:=\\s]+([a-fA-F0-9]{64})")
        val matcher = pattern.matcher(releaseNotes)
        if (matcher.find()) {
            return matcher.group(1)?.lowercase()
        }

        // 2. Look for .sha256 or checksums asset
        val shaAsset = assets.firstOrNull {
            it.name.endsWith(".sha256", ignoreCase = true) ||
            it.name.equals("checksums.txt", ignoreCase = true) ||
            it.name.equals("sha256.txt", ignoreCase = true)
        }
        if (shaAsset != null) {
            try {
                val req = Request.Builder().url(shaAsset.downloadUrl).build()
                val resp = httpClient.newCall(req).execute()
                val content = resp.body?.string().orEmpty()
                val assetMatcher = Pattern.compile("([a-fA-F0-9]{64})").matcher(content)
                if (assetMatcher.find()) {
                    return assetMatcher.group(1)?.lowercase()
                }
            } catch (ignored: Exception) {}
        }

        return null
    }

    /**
     * Computes the SHA-256 hex string of a downloaded file.
     */
    fun calculateSha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(8 * 1024)
            var read: Int
            while (input.read(buffer).also { read = it } != -1) {
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    /**
     * Compares two semantic version strings (e.g. "1.5.1" vs "1.5.0").
     */
    fun isSemanticVersionNewer(remote: String, current: String): Boolean {
        val remoteParts = remote.split(".").mapNotNull { it.toIntOrNull() }
        val currentParts = current.split(".").mapNotNull { it.toIntOrNull() }
        val maxLen = maxOf(remoteParts.size, currentParts.size)
        for (i in 0 until maxLen) {
            val r = remoteParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (r > c) return true
            if (r < c) return false
        }
        return false
    }
}
