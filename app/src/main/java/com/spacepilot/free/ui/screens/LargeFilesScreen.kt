package com.spacepilot.free.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Sort
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.spacepilot.free.R
import com.spacepilot.free.data.model.FileEntry
import com.spacepilot.free.data.model.ScanSummary
import com.spacepilot.free.ui.components.DeleteConfirmationDialog
import com.spacepilot.free.ui.components.EmptyState
import com.spacepilot.free.ui.components.FileListRow
import com.spacepilot.free.ui.components.SpaceCard
import com.spacepilot.free.util.Formatters
import java.util.Locale

private enum class LargeSort {
    SIZE,
    NAME,
    DATE
}

@Composable
fun LargeFilesScreen(
    summary: ScanSummary?,
    onBack: () -> Unit,
    onScan: () -> Unit,
    onOpenFile: (FileEntry) -> Unit,
    onInfo: (FileEntry) -> Unit,
    onDelete: (List<FileEntry>) -> Unit
) {
    var threshold by rememberSaveable { mutableLongStateOf(100L) }
    var sort by remember { mutableStateOf(LargeSort.SIZE) }
    var sortMenuOpen by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<Set<String>>(emptySet()) }
    var confirmDelete by remember { mutableStateOf(false) }

    val allLarge = summary?.largeFiles.orEmpty()
    val thresholdBytes = threshold * 1024L * 1024L
    val filtered = allLarge
        .filter { it.size >= thresholdBytes }
        .let { list ->
            when (sort) {
                LargeSort.SIZE -> list.sortedByDescending { it.size }
                LargeSort.NAME -> list.sortedBy { it.name.lowercase() }
                LargeSort.DATE -> list.sortedByDescending { it.modified }
            }
        }
    val selectedFiles = filtered.filter { it.uri in selected }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.large_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { sortMenuOpen = true }) {
                            Icon(
                                Icons.Outlined.Sort,
                                contentDescription = stringResource(R.string.large_sort_label)
                            )
                        }
                        DropdownMenu(
                            expanded = sortMenuOpen,
                            onDismissRequest = { sortMenuOpen = false }
                        ) {
                            listOf(
                                LargeSort.SIZE to R.string.large_sort_size,
                                LargeSort.NAME to R.string.large_sort_name,
                                LargeSort.DATE to R.string.large_sort_date
                            ).forEach { (mode, label) ->
                                DropdownMenuItem(
                                    text = { Text(stringResource(label)) },
                                    onClick = {
                                        sort = mode
                                        sortMenuOpen = false
                                    }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
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
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.large_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 2.dp)
                )
            }
            if (summary != null) {
                item {
                    Column {
                        Text(
                            text = stringResource(R.string.large_filter_label),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 2.dp, bottom = 6.dp)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = threshold == 100L,
                                onClick = { threshold = 100L; selected = emptySet() },
                                label = { Text(stringResource(R.string.large_filter_100)) }
                            )
                            FilterChip(
                                selected = threshold == 500L,
                                onClick = { threshold = 500L; selected = emptySet() },
                                label = { Text(stringResource(R.string.large_filter_500)) }
                            )
                            FilterChip(
                                selected = threshold == 1000L,
                                onClick = { threshold = 1000L; selected = emptySet() },
                                label = { Text(stringResource(R.string.large_filter_1000)) }
                            )
                        }
                    }
                }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(
                                R.string.large_found,
                                pluralStringResource(
                                    R.plurals.files_count,
                                    filtered.size,
                                    filtered.size
                                ),
                                Formatters.bytes(filtered.sumOf { it.size }, Locale.getDefault())
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.weight(1f))
                        if (selectedFiles.isNotEmpty()) {
                            Button(onClick = { selected = emptySet() }) {
                                Text(stringResource(R.string.action_clear_selection))
                            }
                        }
                    }
                }
                item {
                    NoticeCard(
                        icon = Icons.Outlined.WarningAmber,
                        title = stringResource(R.string.large_file_hint),
                        text = stringResource(R.string.dup_reclaimable_hint),
                        error = false
                    )
                }
            }
            if (filtered.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Outlined.Storage,
                        title = stringResource(
                            if (summary == null) R.string.large_need_scan else R.string.large_empty_title
                        ),
                        message = stringResource(R.string.large_empty_message),
                        action = if (summary == null) {
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
            } else {
                itemsIndexed(filtered, key = { _, file -> file.uri }) { _, file ->
                    FileListRow(
                        file = file,
                        onClick = { onOpenFile(file) },
                        selected = file.uri in selected,
                        onSelectedChange = { checked ->
                            selected = if (checked) selected + file.uri else selected - file.uri
                        },
                        onInfo = { onInfo(file) }
                    )
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
            onDismiss = { confirmDelete = false }
        )
    }
}

@Composable
private fun NoticeCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    text: String,
    error: Boolean
) {
    SpaceCard {
        Row(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
