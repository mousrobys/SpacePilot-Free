package com.spacepilot.free.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Radar
import androidx.compose.material.icons.outlined.StopCircle
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.spacepilot.free.R
import com.spacepilot.free.data.model.ScanPhase
import com.spacepilot.free.data.model.ScanSummary
import com.spacepilot.free.data.model.ScanUiState
import com.spacepilot.free.ui.components.SpaceCard
import com.spacepilot.free.ui.components.StatBlock
import com.spacepilot.free.ui.theme.Blue
import com.spacepilot.free.ui.theme.Teal
import com.spacepilot.free.util.Formatters
import java.util.Locale

@Composable
fun ScanScreen(
    scanState: ScanUiState,
    summary: ScanSummary?,
    folderConnected: Boolean,
    onScan: () -> Unit,
    onCancel: () -> Unit,
    onResults: () -> Unit,
    onPickFolder: () -> Unit,
    bottomBar: @Composable () -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.scan_title)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = bottomBar
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                AnimatedContent(
                    targetState = scanState,
                    transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(120)) },
                    label = "scan-state"
                ) { state ->
                    when (state) {
                        is ScanUiState.Idle -> IdleScanCard(
                            summary = summary,
                            folderConnected = folderConnected,
                            onScan = onScan,
                            onPickFolder = onPickFolder
                        )
                        is ScanUiState.Running -> RunningScanCard(state = state, onCancel = onCancel)
                        is ScanUiState.Completed -> CompletedScanCard(
                            summary = state.summary,
                            onResults = onResults,
                            onScan = onScan
                        )
                        is ScanUiState.Failed -> FailedScanCard(
                            message = state.message,
                            onRetry = onScan
                        )
                        is ScanUiState.Cancelled -> CancelledScanCard(onScan = onScan)
                    }
                }
            }
            item {
                ScanIntroCard(folderConnected = folderConnected)
            }
        }
    }
}

@Composable
private fun IdleScanCard(
    summary: ScanSummary?,
    folderConnected: Boolean,
    onScan: () -> Unit,
    onPickFolder: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF0C3B46), Color(0xFF0A2740), Color(0xFF0D2035))
                )
            )
    ) {
        Column(modifier = Modifier.padding(22.dp)) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(RoundedCornerShape(19.dp))
                    .background(Teal.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.Radar,
                    contentDescription = null,
                    tint = Teal,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(Modifier.height(18.dp))
            Text(
                text = stringResource(R.string.scan_start),
                style = MaterialTheme.typography.headlineSmall,
                color = Color(0xFFEFFBFF)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.scan_intro_message),
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFA9C7D8)
            )
            if (summary != null) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.results_scan_time, Formatters.date(summary.timestamp, Locale.getDefault())),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFA9C7D8)
                )
                Text(
                    text = pluralStringResource(
                        R.plurals.files_count,
                        summary.scannedFiles,
                        summary.scannedFiles
                    ),
                    style = MaterialTheme.typography.titleSmall,
                    color = Color(0xFFEFFBFF)
                )
            }
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onScan,
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = Teal,
                        contentColor = Color(0xFF002C2A)
                    )
                ) {
                    Text(stringResource(R.string.scan_start))
                }
                OutlinedButton(onClick = onPickFolder) {
                    Icon(Icons.Outlined.Folder, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        stringResource(
                            if (folderConnected) R.string.settings_change_folder else R.string.scan_choose_folder
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun RunningScanCard(state: ScanUiState.Running, onCancel: () -> Unit) {
    SpaceCard {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(38.dp), strokeWidth = 3.dp)
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = phaseLabel(state.phase),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = if (state.progress != null) {
                            Formatters.percent(state.progress)
                        } else {
                            stringResource(R.string.scan_access_note)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(onClick = onCancel) {
                    Icon(Icons.Outlined.StopCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.scan_cancel))
                }
            }
            Spacer(Modifier.height(16.dp))
            if (state.progress != null) {
                LinearProgressIndicator(
                    progress = { state.progress },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = Teal,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            } else {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = Teal,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(26.dp)) {
                StatBlock(
                    label = stringResource(R.string.scan_files_scanned),
                    value = pluralStringResource(
                        R.plurals.files_count,
                        state.scannedFiles,
                        state.scannedFiles
                    )
                )
                StatBlock(
                    label = stringResource(R.string.scan_data_found),
                    value = Formatters.bytes(state.scannedBytes, Locale.getDefault()),
                    accent = Blue
                )
            }
            AnimatedVisibility(visible = state.currentLocation.isNotBlank()) {
                Column {
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = stringResource(R.string.scan_current, state.currentLocation),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun CompletedScanCard(summary: ScanSummary, onResults: () -> Unit, onScan: () -> Unit) {
    SpaceCard {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = Teal,
                    modifier = Modifier.size(34.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.scan_complete_title),
                    style = MaterialTheme.typography.titleLarge
                )
            }
            Spacer(Modifier.height(14.dp))
            Text(
                text = stringResource(
                    R.string.scan_complete_message,
                    Formatters.count(summary.scannedFiles, Locale.getDefault()),
                    Formatters.bytes(summary.totalBytes, Locale.getDefault())
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (summary.limitedAccess) {
                Spacer(Modifier.height(14.dp))
                NoticeCard(
                    icon = Icons.Outlined.WarningAmber,
                    title = stringResource(R.string.scan_limited_title),
                    message = stringResource(R.string.scan_limited_message),
                    isError = true
                )
            }
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = onResults) {
                    Text(stringResource(R.string.scan_view_results))
                }
                OutlinedButton(onClick = onScan) {
                    Text(stringResource(R.string.scan_restart))
                }
            }
        }
    }
}

@Composable
private fun FailedScanCard(message: String, onRetry: () -> Unit) {
    SpaceCard {
        Column(modifier = Modifier.padding(20.dp)) {
            Icon(
                Icons.Outlined.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(34.dp)
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.scan_failed_title),
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(14.dp))
            Button(onClick = onRetry) {
                Text(stringResource(R.string.action_retry))
            }
        }
    }
}

@Composable
private fun CancelledScanCard(onScan: () -> Unit) {
    SpaceCard {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = stringResource(R.string.scan_cancelled_title),
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.scan_cancelled_message),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(14.dp))
            Button(onClick = onScan) {
                Text(stringResource(R.string.scan_restart))
            }
        }
    }
}

@Composable
private fun ScanIntroCard(folderConnected: Boolean) {
    SpaceCard {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(19.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.scan_intro_title),
                    style = MaterialTheme.typography.titleSmall
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.scan_intro_message),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.FolderOpen,
                    contentDescription = null,
                    tint = if (folderConnected) Teal else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(7.dp))
                Text(
                    text = if (folderConnected) {
                        stringResource(R.string.home_access_connected)
                    } else {
                        stringResource(R.string.scan_permissions_missing)
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun NoticeCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    message: String,
    isError: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isError) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .padding(13.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(10.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun phaseLabel(phase: ScanPhase): String = stringResource(
    when (phase) {
        ScanPhase.PREPARING -> R.string.scan_stage_preparing
        ScanPhase.MEDIA_LIBRARY -> R.string.scan_stage_media
        ScanPhase.FOLDER -> R.string.scan_stage_folder
        ScanPhase.FINISHING -> R.string.scan_stage_finishing
    }
)
