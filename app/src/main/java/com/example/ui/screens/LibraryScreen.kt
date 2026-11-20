package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DownloadForOffline
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.AppListing
import com.example.data.InstalledApp
import com.example.data.UiState
import com.example.ui.components.AppBadgeIcon
import com.example.ui.components.EmptyStatePanel
import com.example.ui.components.SectionHeaderRow
import com.example.ui.components.formatSizeMb

@Composable
fun LibraryScreen(
    libraryState: UiState<List<InstalledApp>>,
    catalogById: Map<String, AppListing>,
    downloadProgress: Map<String, Float>,
    onOpenDetail: (String) -> Unit,
    onUpdateApp: (AppListing) -> Unit,
    onLaunchApp: (InstalledApp) -> Unit,
    onUninstall: (InstalledApp) -> Unit,
    onBrowseStore: () -> Unit
) {
    var pendingUninstall by remember { mutableStateOf<InstalledApp?>(null) }

    pendingUninstall?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingUninstall = null },
            title = { Text("Uninstall ${target.title}?") },
            text = {
                Text("This removes ${target.title} from your cloud-synced library. You can re-install it anytime from PulseStore.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUninstall(target)
                        pendingUninstall = null
                    },
                    modifier = Modifier.testTag("confirm_uninstall_button")
                ) {
                    Text("Uninstall")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingUninstall = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    when (libraryState) {
        is UiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        is UiState.Error -> {
            Box(modifier = Modifier.padding(16.dp)) {
                EmptyStatePanel(
                    icon = Icons.Filled.DownloadForOffline,
                    title = "Could not load library",
                    subtitle = libraryState.message
                )
            }
        }

        is UiState.Success -> {
            val installed = libraryState.data
            if (installed.isEmpty()) {
                Box(modifier = Modifier.padding(16.dp)) {
                    EmptyStatePanel(
                        icon = Icons.Filled.DownloadForOffline,
                        title = "Your Library is Empty",
                        subtitle = "Games and apps you download appear here with cloud sync and automatic version update alerts.",
                        actionLabel = "Explore Storefront",
                        onAction = onBrowseStore
                    )
                }
            } else {
                val totalSizeMb = installed.sumOf { it.sizeMb }
                val updatesAvailable = installed.mapNotNull { item ->
                    val latest = catalogById[item.appId]
                    if (latest != null && latest.versionName != item.installedVersion) item to latest
                    else null
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("library_screen"),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Summary Card
                    item(key = "library_summary") {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.CloudDone,
                                        contentDescription = "Synced library",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "${installed.size} Installed",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Text(
                                            text = "${formatSizeMb(totalSizeMb)} library footprint • Synced",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                                if (updatesAvailable.isNotEmpty()) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.tertiary,
                                        shape = RoundedCornerShape(50)
                                    ) {
                                        Text(
                                            text = "${updatesAvailable.size} update${if (updatesAvailable.size > 1) "s" else ""}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onTertiary,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (updatesAvailable.isNotEmpty()) {
                        item(key = "updates_header") {
                            SectionHeaderRow(title = "Updates Available")
                        }
                        items(updatesAvailable, key = { "upd_${it.first.appId}" }) { (item, latest) ->
                            val progress = downloadProgress[item.appId]
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.45f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AppBadgeIcon(
                                        accentHex = latest.accentColorHex,
                                        iconPreset = latest.iconPreset,
                                        iconUrl = latest.iconUrl,
                                        title = latest.title,
                                        size = 48.dp,
                                        cornerRadius = 14.dp
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = latest.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "v${item.installedVersion} → v${latest.versionName}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.tertiary
                                        )
                                    }
                                    FilledTonalButton(
                                        onClick = { onUpdateApp(latest) },
                                        enabled = progress == null,
                                        modifier = Modifier.testTag("lib_update_${latest.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.SystemUpdate,
                                            contentDescription = "Update",
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(if (progress != null) "${(progress * 100).toInt()}%" else "Update")
                                    }
                                }
                            }
                        }
                    }

                    item(key = "installed_header") {
                        SectionHeaderRow(title = "Downloaded Software")
                    }

                    items(installed, key = { "lib_${it.appId}" }) { item ->
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .clickable { onOpenDetail(item.appId) }
                                .testTag("library_item_${item.appId}")
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AppBadgeIcon(
                                        accentHex = item.accentColorHex,
                                        iconPreset = item.iconPreset,
                                        iconUrl = item.iconUrl,
                                        title = item.title,
                                        size = 52.dp,
                                        cornerRadius = 15.dp
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${item.developerName} • ${item.category}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "v${item.installedVersion}  •  ${formatSizeMb(item.sizeMb)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(
                                        onClick = { pendingUninstall = item },
                                        modifier = Modifier.testTag("uninstall_btn_${item.appId}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.DeleteOutline,
                                            contentDescription = "Uninstall ${item.title}",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { onOpenDetail(item.appId) },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Store Page")
                                    }
                                    Button(
                                        onClick = { onLaunchApp(item) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("launch_btn_${item.appId}"),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        if (!item.downloadUrl.isNullOrBlank()) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                                contentDescription = "Open link",
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Open Release")
                                        } else {
                                            Text("Launch")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(12.dp)) }
                }
            }
        }
    }
}
