package com.spacepilot.free.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.FolderOff
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
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
import com.spacepilot.free.data.model.FileCategory
import com.spacepilot.free.data.model.FileEntry
import com.spacepilot.free.data.model.ScanSummary
import com.spacepilot.free.ui.components.CategoryIcon
import com.spacepilot.free.ui.components.EmptyState
import com.spacepilot.free.ui.components.FileListRow
import com.spacepilot.free.ui.components.SectionHeader
import com.spacepilot.free.ui.components.SpaceCard
import com.spacepilot.free.ui.components.StatBlock
import com.spacepilot.free.ui.theme.Blue
import com.spacepilot.free.ui.theme.Teal
import com.spacepilot.free.util.Formatters
import java.util.Locale

@Composable
fun ResultsScreen(
    summary: ScanSummary?,
    onBack: () -> Unit,
    onScan: () -> Unit,
    onHome: () -> Unit,
    onLargeFiles: () -> Unit,
    onDuplicates: () -> Unit,
    onAnalyzeDuplicates: () -> Unit,
    onOpenFile: (FileEntry) -> Unit,
    onInfo: (FileEntry) -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.results_title)) },
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
        if (summary == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                EmptyState(
                    icon = Icons.Outlined.Storage,
                    title = stringResource(R.string.home_last_scan_empty_title),
                    message = stringResource(R.string.home_last_scan_empty_message),
                    action = {
                        Button(onClick = onScan) {
                            Text(stringResource(R.string.action_scan))
                        }
                    }
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    SummaryHero(summary = summary)
                }
                if (summary.limitedAccess) {
                    item {
                        WarningCard(
                            text = stringResource(R.string.results_limited_access)
                        )
                    }
                }
                item {
                    SectionHeader(title = stringResource(R.string.results_categories))
                }
                item {
                    CategoryBreakdown(summary = summary)
                }
                item {
                    SectionHeader(
                        title = stringResource(R.string.results_large_files),
                        action = if (summary.largeFiles.isNotEmpty()) {
                            {
                                TextButton(onClick = onLargeFiles) {
                                    Text(stringResource(R.string.action_open))
                                }
                            }
                        } else {
                            null
                        }
                    )
                }
                if (summary.largeFiles.isEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.results_no_large),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 2.dp)
                        )
                    }
                } else {
                    items(summary.largeFiles.take(5), key = { it.uri }) { file ->
                        FileListRow(
                            file = file,
                            onClick = { onOpenFile(file) },
                            onInfo = { onInfo(file) }
                        )
                    }
                }
                item {
                    SectionHeader(
                        title = stringResource(R.string.results_duplicates),
                        action = {
                            TextButton(onClick = onDuplicates) {
                                Text(stringResource(R.string.action_open))
                            }
                        }
                    )
                }
                item {
                    DuplicateSummary(
                        summary = summary,
                        onAnalyzeDuplicates = onAnalyzeDuplicates,
                        onDuplicates = onDuplicates
                    )
                }
                item {
                    SectionHeader(title = stringResource(R.string.results_inaccessible))
                }
                item {
                    InaccessibleBlock(summary = summary)
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(onClick = onScan, modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.results_rescan))
                        }
                        OutlinedButton(onClick = onHome, modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.results_home))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryHero(summary: ScanSummary) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF0B3B45), Color(0xFF0A2943), Color(0xFF101F36))
                )
            )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = Teal,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.scan_complete_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFFF0FBFF)
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = if (summary.usedFolderAccess) {
                        stringResource(R.string.results_folder_access)
                    } else {
                        stringResource(R.string.results_media_only)
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFA9C7D8)
                )
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(30.dp)) {
                StatBlock(
                    label = stringResource(R.string.results_scanned_files),
                    value = Formatters.count(summary.scannedFiles, Locale.getDefault()),
                    accent = Color(0xFFF0FBFF)
                )
                StatBlock(
                    label = stringResource(R.string.results_total_size),
                    value = Formatters.bytes(summary.totalBytes, Locale.getDefault()),
                    accent = Teal
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.results_scan_time, Formatters.date(summary.timestamp, Locale.getDefault())),
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFA9C7D8)
            )
        }
    }
}

@Composable
private fun WarningCard(text: String) {
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
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CategoryBreakdown(summary: ScanSummary) {
    SpaceCard {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            FileCategory.entries.forEach { category ->
                val stat = summary.categories[category] ?: return@forEach
                if (stat.count <= 0) return@forEach
                val fraction = if (summary.totalBytes > 0L) {
                    stat.bytes.toFloat() / summary.totalBytes
                } else {
                    0f
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CategoryIcon(category, 34.dp)
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = stringResource(
                                    when (category) {
                                        FileCategory.IMAGES -> R.string.category_images
                                        FileCategory.VIDEO -> R.string.category_video
                                        FileCategory.AUDIO -> R.string.category_audio
                                        FileCategory.DOCUMENTS -> R.string.category_documents
                                        FileCategory.ARCHIVES -> R.string.category_archives
                                        FileCategory.APK -> R.string.category_apk
                                        FileCategory.OTHER -> R.string.category_other
                                    }
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = Formatters.bytes(stat.bytes, Locale.getDefault()),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { fraction.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = Blue,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Text(
                            text = stringResource(R.string.category_count_size, stat.count, Formatters.bytes(stat.bytes, Locale.getDefault())),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Text(
                text = stringResource(R.string.results_access_note),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DuplicateSummary(
    summary: ScanSummary,
    onAnalyzeDuplicates: () -> Unit,
    onDuplicates: () -> Unit
) {
    SpaceCard {
        Column(modifier = Modifier.padding(16.dp)) {
            if (!summary.duplicatesAnalyzed) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.ContentCopy,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(9.dp))
                    Text(
                        text = stringResource(R.string.results_not_analyzed),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    Button(onClick = onAnalyzeDuplicates) {
                        Text(stringResource(R.string.results_analyze_duplicates))
                    }
                }
            } else if (summary.duplicateGroups.isEmpty()) {
                Text(
                    text = stringResource(R.string.results_no_duplicates),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = pluralStringResource(
                        R.plurals.duplicate_groups_count,
                        summary.duplicateGroups.size,
                        summary.duplicateGroups.size
                    ),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(
                        R.string.results_reclaimable,
                        Formatters.bytes(summary.duplicateGroups.sumOf { it.reclaimableBytes }, Locale.getDefault())
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = Teal
                )
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onDuplicates) {
                    Text(stringResource(R.string.action_open))
                }
            }
        }
    }
}

@Composable
private fun InaccessibleBlock(summary: ScanSummary) {
    if (summary.inaccessible.isEmpty()) {
        Text(
            text = stringResource(R.string.results_no_inaccessible),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 2.dp)
        )
    } else {
        SpaceCard {
            Column(modifier = Modifier.padding(16.dp)) {
                summary.inaccessible.take(8).forEach { path ->
                    Row(modifier = Modifier.padding(vertical = 4.dp)) {
                        Icon(
                            Icons.Outlined.FolderOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = path,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (summary.inaccessible.size > 8) {
                    Text(
                        text = "… +${summary.inaccessible.size - 8}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
