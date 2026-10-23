package com.ghostpocket.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ghostpocket.app.ui.components.ClayButton
import com.ghostpocket.app.ui.components.ClayCard
import com.ghostpocket.app.util.AppThemeMode

/**
 * ============================================================================
 * ThemeSettingsCard
 * ============================================================================
 * Card for toggling between Light ☀️, Dark 🌙, and System ⚡ theme modes.
 */
@Composable
fun ThemeSettingsCard(
    currentThemeMode: AppThemeMode,
    onThemeChange: (AppThemeMode) -> Unit,
    modifier: Modifier = Modifier
) {
    ClayCard(
        modifier = modifier.fillMaxWidth(),
        shapeRadius = 24.dp,
        elevation = 6.dp
    ) {
        Text(
            text = "Spooky Appearance 🌓",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Choose your preferred vibe: Crisp Light or Deep Obsidian Dark.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(4.dp)
        ) {
            listOf(
                AppThemeMode.LIGHT to "Light ☀️",
                AppThemeMode.DARK to "Dark 🌙",
                AppThemeMode.SYSTEM to "System ⚡"
            ).forEach { (mode, label) ->
                val isSelected = currentThemeMode == mode
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .clickable { onThemeChange(mode) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * ============================================================================
 * AppTourSettingsCard
 * ============================================================================
 * Card for replaying the app tour guide modal.
 */
@Composable
fun AppTourSettingsCard(
    onReplayTour: () -> Unit,
    modifier: Modifier = Modifier
) {
    ClayCard(
        modifier = modifier.fillMaxWidth(),
        shapeRadius = 24.dp,
        elevation = 6.dp
    ) {
        Text(
            text = "App Tour & Feature Guide 🪄",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Revisit the enchanted guide to explore all features, calculator tricks, split bills, and privacy magic.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(14.dp))
        ClayButton(
            text = "Replay App Tour 🪄",
            onClick = onReplayTour,
            isSecondary = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * ============================================================================
 * CloudSyncSettingsCard
 * ============================================================================
 * Card managing cloud synchronization with Cloudflare D1.
 */
@Composable
fun CloudSyncSettingsCard(
    isSyncing: Boolean,
    onManualSync: () -> Unit,
    modifier: Modifier = Modifier
) {
    ClayCard(
        modifier = modifier.fillMaxWidth(),
        shapeRadius = 24.dp,
        elevation = 6.dp
    ) {
        Text(
            text = "Cloud Spirits & Sync ☁️",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "WhatsApp-Style Silent Backup: Runs automatically once every 24 hours to back up all changes in a single batch. You can also force a backup now.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(14.dp))

        ClayButton(
            text = if (isSyncing) "Summoning cloud..." else "Sync Now 🔄",
            icon = Icons.Default.Sync,
            onClick = onManualSync,
            enabled = !isSyncing,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * ============================================================================
 * AppUpdateSettingsCard
 * ============================================================================
 * Card for checking GitHub Releases for newer APK versions.
 */
@Composable
fun AppUpdateSettingsCard(
    currentVersionName: String,
    currentVersionCode: Int,
    isCheckingForUpdate: Boolean,
    onCheckForUpdates: () -> Unit,
    modifier: Modifier = Modifier
) {
    ClayCard(
        modifier = modifier.fillMaxWidth(),
        shapeRadius = 24.dp,
        elevation = 6.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Ghost Updates 🚀",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "v$currentVersionName ($currentVersionCode)",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "GhostPocket checks GitHub Releases for new spells, bug banishments, and optimizations.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(14.dp))

        val context = androidx.compose.ui.platform.LocalContext.current
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ClayButton(
                text = if (isCheckingForUpdate) "Checking..." else "Check Updates 🔄",
                icon = Icons.Default.SystemUpdate,
                onClick = onCheckForUpdates,
                enabled = !isCheckingForUpdate,
                modifier = Modifier.weight(1f)
            )
            ClayButton(
                text = "Releases 🌐",
                isSecondary = true,
                onClick = {
                    val intent = android.content.Intent(
                        android.content.Intent.ACTION_VIEW,
                        android.net.Uri.parse("https://github.com/Om-Prakash-Verma/Pocket-Budget/releases")
                    ).apply {
                        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    try {
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        // ignore
                    }
                },
                modifier = Modifier.weight(0.9f)
            )
        }
    }
}
