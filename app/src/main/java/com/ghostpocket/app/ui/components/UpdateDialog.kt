package com.ghostpocket.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ghostpocket.app.update.AppUpdateInfo
import com.ghostpocket.app.update.DownloadState
import java.io.File

@Composable
fun UpdateDialog(
    updateInfo: AppUpdateInfo,
    downloadState: DownloadState,
    currentVersionName: String,
    onStartDownload: () -> Unit,
    onInstall: (File) -> Unit,
    onCancelDownload: () -> Unit,
    onDismiss: () -> Unit
) {
    val isMandatory = updateInfo.isMandatory
    val isDownloading = downloadState is DownloadState.Downloading
    val isVerifying = downloadState is DownloadState.Verifying
    val isReady = downloadState is DownloadState.ReadyToInstall
    val isFailed = downloadState is DownloadState.Failed

    Dialog(
        onDismissRequest = {
            if (!isMandatory && !isDownloading) {
                onDismiss()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = !isMandatory && !isDownloading,
            dismissOnClickOutside = !isMandatory && !isDownloading
        )
    ) {
        ClayCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            shapeRadius = 26.dp,
            elevation = 10.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp)
            ) {
                // Header Row with Mascot
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            GhostMascot(size = 36.dp, mood = GhostMood.HAPPY)
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = "New Spell Available! 🚀",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "GhostPocket v${updateInfo.latestVersionName}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    if (!isMandatory && !isDownloading) {
                        IconButton(onClick = onDismiss) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Version Badge Pill
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Current: v$currentVersionName",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "➔",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Latest: v${updateInfo.latestVersionName}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (isMandatory) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.errorContainer)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "⚠️ Mandatory Update Required",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Release Notes Container (Scrollable)
                Text(
                    text = "WHAT'S NEW 📜",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.1.sp
                )
                Spacer(modifier = Modifier.height(6.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 60.dp, max = 150.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .verticalScroll(rememberScrollState())
                        .padding(12.dp)
                ) {
                    Text(
                        text = if (updateInfo.releaseNotes.isNotBlank()) updateInfo.releaseNotes.trim() else "Bug fixes and performance improvements.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Checksum / Verification Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = if (updateInfo.expectedSha256 != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (updateInfo.expectedSha256 != null) {
                            "Verified Release (SHA-256 protected)"
                        } else {
                            "GitHub Official Release (Package signature checked)"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Dynamic Action / Download Progress Section
                when (downloadState) {
                    is DownloadState.Downloading -> {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Downloading update...",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${downloadState.progressPercent}%",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { downloadState.progressPercent / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            val currentMb = downloadState.bytesDownloaded / (1024f * 1024f)
                            val totalMb = downloadState.totalBytes / (1024f * 1024f)
                            Text(
                                text = "%.1f MB / %.1f MB".format(currentMb, totalMb),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            TextButton(
                                onClick = onCancelDownload,
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text("Cancel", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }

                    is DownloadState.Verifying -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Verifying cryptographic checksum...",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    is DownloadState.ReadyToInstall -> {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = if (downloadState.verifiedChecksum) {
                                    "✓ Download verified with SHA-256 checksum!"
                                } else {
                                    "✓ Download complete and package validated."
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            ClayButton(
                                text = "Install Update 📦",
                                icon = Icons.Default.SystemUpdate,
                                onClick = { onInstall(downloadState.apkFile) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    is DownloadState.Failed -> {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Update failed: ${downloadState.error}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (!isMandatory) {
                                    TextButton(
                                        onClick = onDismiss,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Dismiss")
                                    }
                                }
                                ClayButton(
                                    text = "Retry 🔄",
                                    onClick = onStartDownload,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    DownloadState.Idle -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (!isMandatory) {
                                TextButton(
                                    onClick = onDismiss,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Later ⏳")
                                }
                            }
                            ClayButton(
                                text = "Update Now ⚡",
                                icon = Icons.Default.Download,
                                onClick = onStartDownload,
                                modifier = Modifier.weight(1.5f)
                            )
                        }
                    }
                }
            }
        }
    }
}
