package com.spacepilot.free.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.StopCircle
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.spacepilot.free.data.model.DuplicateGroup
import com.spacepilot.free.data.model.DuplicateUiState
import com.spacepilot.free.data.model.DuplicateVerification
import com.spacepilot.free.data.model.FileEntry
import com.spacepilot.free.ui.components.DeleteConfirmationDialog
import com.spacepilot.free.ui.components.EmptyState
import com.spacepilot.free.ui.components.FileListRow
import com.spacepilot.free.ui.components.SpaceCard
import com.spacepilot.free.ui.theme.Blue
import com.spacepilot.free.ui.theme.Teal
import com.spacepilot.free.util.Formatters
import java.util.Locale

@Composable
fun DuplicatesScreen(
    duplicates: DuplicateUiState,
    scannedFileCount: Int,
    onBack: () -> Unit,
    onAnalyze: () -> Unit,
    onCancelAnalysis: () -> Unit,
    onScan: () -> Unit,
    onOpenFile: (FileEntry) -> Unit,
    onInfo: (FileEntry) -> Unit,
    onDelete: (List<FileEntry>) -> Unit,
    showBottomBar: Boolean,
    bottomBar: @Composable () -> Unit = {}
) {
    var selected by remember { mutableStateOf<Set<String>>(emptySet()) }
    var confirmDelete by remember { mutableStateOf(false) }
    val selectedFiles = when (duplicates) {
        is DuplicateUiState.Completed -> duplicates.groups
            .flatMap { it.files }
            .filter { it.uri in selected }
        else -> emptyList()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.dup_title)) },
                navigationIcon = {
                    if (!showBottomBar) {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.Outlined.ArrowBack,
                                contentDescription = stringResource(R.string.action_back)
                            )
                        }
                    }
                },
                actions = {
                    if (duplicates is DuplicateUiState.Completed) {
                        IconButton(onClick = onAnalyze) {
                            Icon(
                                Icons.Outlined.PlayArrow,
                                contentDescription = stringResource(R.string.dup_analyze)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            Column {
                AnimatedVisibility(
                    visible = selectedFiles.isNotEmpty(),
                    enter = slideInVertically { it },
                    exit = slideOutVertically { it }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = pluralStringResource(
                                    R.plurals.files_selected,
                                    selectedFiles.size,
                                    selectedFiles.size
                                ),
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = Formatters.bytes(selectedFiles.sumOf { it.size }, Locale.getDefault()),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Button(
                            onClick = { confirmDelete = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            )
                        ) {
                            Icon(Icons.Outlined.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.action_delete))
                        }
                    }
                }
                bottomBar()
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.dup_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 2.dp)
                )
            }
            when (duplicates) {
                is DuplicateUiState.NotAnalyzed -> {
                    item {
                        AnalyzeCard(
                            scannedFileCount = scannedFileCount,
                            onAnalyze = onAnalyze,
                            onScan = onScan
                        )
                    }
                }
                is DuplicateUiState.Running -> {
                    item {
                        RunningCard(state = duplicates, onCancel = onCancelAnalysis)
                    }
                }
                is DuplicateUiState.NoCandidates -> {
                    item {
                        EmptyState(
                            icon = Icons.Outlined.ContentCopy,
                            title = stringResource(R.string.dup_empty_title),
                            message = stringResource(R.string.dup_no_candidates),
                            action = {
                                Button(onClick = onScan) {
                                    Text(stringResource(R.string.action_scan))
                                }
                            }
                        )
                    }
                }
                is DuplicateUiState.Failed -> {
                    item {
                        SpaceCard {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Text(
                                    text = stringResource(R.string.duplicates_failed),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = duplicates.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(12.dp))
                                Button(onClick = onAnalyze) {
                                    Text(stringResource(R.string.action_retry))
                                }
                            }
                        }
                    }
                }
                is DuplicateUiState.Completed -> {
                    if (duplicates.groups.isEmpty()) {
                        item {
                            EmptyState(
                                icon = Icons.Outlined.ContentCopy,
                                title = stringResource(R.string.dup_empty_title),
                                message = stringResource(R.string.dup_empty_message),
                                action = {
                                    OutlinedButton(onClick = onAnalyze) {
                                        Text(stringResource(R.string.dup_analyze))
                                    }
                                }
                            )
                        }
                    } else {
                        item {
                            DuplicateSummaryCard(groups = duplicates.groups)
                        }
                        item {
                            Text(
                                text = stringResource(R.string.dup_select_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 2.dp)
                            )
                        }
                        items(duplicates.groups, key = { it.files.firstOrNull()?.uri ?: it.size.toString() }) { group ->
                            DuplicateGroupCard(
                                group = group,
                                selected = selected,
                                onSelectedChange = { uri, checked ->
                                    selected = if (checked) selected + uri else selected - uri
                                },
                                onOpenFile = onOpenFile,
                                onInfo = onInfo
                            )
                        }
                        item {
                            Text(
                                text = stringResource(R.string.dup_sampled_note),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 2.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (confirmDelete) {
        DeleteConfirmationDialog(
            files = selectedFiles,
            onConfirm = {
                onDelete(selectedFiles)
                selected = emptySet()
                confirmDelete = false
            },
            onDismiss = { confirmDelete = false },
            extraWarning = stringResource(R.string.dup_delete_warning)
        )
    }
}

@Composable
private fun AnalyzeCard(scannedFileCount: Int, onAnalyze: () -> Unit, onScan: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF0B3B45), Color(0xFF0B2942), Color(0xFF0D2035))
                )
            )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Icon(
                Icons.Outlined.ContentCopy,
                contentDescription = null,
                tint = Teal,
                modifier = Modifier.size(34.dp)
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = stringResource(R.string.dup_analyze),
                style = MaterialTheme.typography.titleLarge,
                color = androidx.compose.ui.graphics.Color(0xFFF0FBFF)
            )
            Spacer(Modifier.height(5.dp))
            Text(
                text = if (scannedFileCount > 1) {
                    pluralStringResource(R.plurals.files_count, scannedFileCount, scannedFileCount)
                } else {
                    stringResource(R.string.dup_need_scan)
                },
                style = MaterialTheme.typography.bodySmall,
                color = androidx.compose.ui.graphics.Color(0xFFA9C7D8)
            )
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onAnalyze,
                    enabled = scannedFileCount > 1,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Teal,
                        contentColor = androidx.compose.ui.graphics.Color(0xFF002C2A)
                    )
                ) {
                    Text(stringResource(R.string.action_continue))
                }
                OutlinedButton(onClick = onScan) {
                    Text(stringResource(R.string.action_scan))
                }
            }
        }
    }
}

@Composable
private fun RunningCard(state: DuplicateUiState.Running, onCancel: () -> Unit) {
    SpaceCard {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(38.dp), strokeWidth = 3.dp)
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.dup_analyzing),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = if (state.totalCandidates > 0) {
                            stringResource(
                                R.string.dup_progress,
                                state.hashedFiles,
                                state.totalCandidates
                            )
                        } else {
                            stringResource(R.string.common_loading)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(onClick = onCancel) {
                    Icon(Icons.Outlined.StopCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.dup_cancel))
                }
            }
            Spacer(Modifier.height(16.dp))
            if (state.progress != null) {
                LinearProgressIndicator(
                    progress = { state.progress ?: 0f },
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
            Spacer(Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.scan_access_note),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DuplicateSummaryCard(groups: List<DuplicateGroup>) {
    val reclaimable = groups.sumOf { it.reclaimableBytes }
    SpaceCard {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = pluralStringResource(
                        R.plurals.duplicate_groups_count,
                        groups.size,
                        groups.size
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = Formatters.bytes(reclaimable, Locale.getDefault()),
                    style = MaterialTheme.typography.titleMedium,
                    color = Teal
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.dup_reclaimable, Formatters.bytes(reclaimable, Locale.getDefault())),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.dup_reclaimable_hint),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DuplicateGroupCard(
    group: DuplicateGroup,
    selected: Set<String>,
    onSelectedChange: (String, Boolean) -> Unit,
    onOpenFile: (FileEntry) -> Unit,
    onInfo: (FileEntry) -> Unit
) {
    var expanded by rememberSaveable(group.files.firstOrNull()?.uri) { mutableStateOf(true) }
    SpaceCard {
        Column(modifier = Modifier.animateContentSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(Blue.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.ContentCopy,
                        contentDescription = null,
                        tint = Blue,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.width(11.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = group.files.firstOrNull()?.name ?: stringResource(R.string.dup_group),
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = stringResource(
                            R.string.dup_files_in_group,
                            group.files.size
                        ) + " · " + Formatters.bytes(group.size, Locale.getDefault()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            AnimatedVisibility(visible = expanded) {
                Column {
                    HorizontalDivider()
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (group.verification == DuplicateVerification.FULL_SHA256) {
                                stringResource(R.string.dup_verification_full)
                            } else {
                                stringResource(R.string.dup_verification_sampled)
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = Teal,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = stringResource(
                                R.string.dup_reclaimable,
                                Formatters.bytes(group.reclaimableBytes, Locale.getDefault())
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    group.files.forEach { file ->
                        FileListRow(
                            file = file,
                            onClick = { onOpenFile(file) },
                            selected = file.uri in selected,
                            onSelectedChange = { checked -> onSelectedChange(file.uri, checked) },
                            onInfo = { onInfo(file) },
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(7.dp))
                        Text(
                            text = stringResource(R.string.dup_delete_warning),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HorizontalDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant)
    )
}
