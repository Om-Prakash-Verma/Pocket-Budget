package com.ghostpocket.app.update

/**
 * Centralized configuration for GhostPocket GitHub Releases In-App Update System.
 */
object UpdateConfig {
    /** GitHub repository owner */
    const val GITHUB_OWNER = "Om-Prakash-Verma"

    /** GitHub repository name */
    const val GITHUB_REPO = "Pocket-Budget"

    /** API endpoint for the latest release */
    const val LATEST_RELEASE_API_URL = "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"

    /** Throttle automatic background update checks (6 hours) */
    const val AUTO_CHECK_THROTTLE_MS = 6 * 60 * 60 * 1000L

    /** SharedPreferences file name for update state */
    const val PREFS_NAME = "ghost_pocket_update_prefs"

    /** Key for storing the last check timestamp */
    const val KEY_LAST_CHECK_TIMESTAMP = "last_check_timestamp"

    /** Key for storing an ignored version code by the user */
    const val KEY_IGNORED_VERSION_CODE = "ignored_version_code"
}
