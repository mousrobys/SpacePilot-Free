package com.spacepilot.free.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.InstallMobile
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.spacepilot.free.R
import com.spacepilot.free.data.model.FileEntry
import com.spacepilot.free.ui.components.EmptyState
import com.spacepilot.free.ui.components.FileTypeIcon
import com.spacepilot.free.ui.components.SpaceCard
import com.spacepilot.free.util.Formatters
import java.util.Locale

@Composable
fun ApkScreen(
    apkFiles: List<FileEntry>,
    hasScanResults: Boolean,
    onBack: () -> Unit,
    onScan: () -> Unit,
    onInfo: (FileEntry) -> Unit,
    onInstall: (FileEntry) -> Unit
) {
    val sorted = apkFiles.sortedByDescending { it.modified }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.apk_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.apk_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 2.dp)
                )
            }
            item {
                SpaceCard {
                    Row(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                        Icon(
                            Icons.Outlined.WarningAmber,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.apk_safety_note),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            if (sorted.isNotEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.apk_count, sorted.size),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 2.dp)
                    )
                }
                items(sorted, key = { it.uri }) { file ->
                    ApkRow(
                        file = file,
                        onInfo = { onInfo(file) },
                        onInstall = { onInstall(file) }
                    )
                }
            } else {
                item {
                    EmptyState(
                        icon = Icons.Outlined.Smartphone,
                        title = stringResource(R.string.apk_empty_title),
                        message = stringResource(R.string.apk_empty_message),
                        action = if (!hasScanResults) {
                            {
                                Button(onClick = onScan) {
                                    Text(stringResource(R.string.action_scan))
                                }
                            }
                        } else {
                            null
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ApkRow(file: FileEntry, onInfo: () -> Unit, onInstall: () -> Unit) {
    SpaceCard(onClick = onInfo) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 4.dp, top = 11.dp, bottom = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FileTypeIcon(file.category, false, 42.dp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = buildString {
                        append(Formatters.bytes(file.size, Locale.getDefault()))
                        if (file.modified > 0L) {
                            append("  ·  ")
                            append(Formatters.shortDate(file.modified, Locale.getDefault()))
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = file.path,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onInfo) {
                Icon(
                    Icons.Outlined.Info,
                    contentDescription = stringResource(R.string.action_info),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onInstall) {
                Icon(
                    Icons.Outlined.InstallMobile,
                    contentDescription = stringResource(R.string.apk_install),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
