package com.spacepilot.free.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Radar
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.spacepilot.free.R
import com.spacepilot.free.data.model.FileCategory
import com.spacepilot.free.data.model.OperationRecord
import com.spacepilot.free.data.model.OperationType
import com.spacepilot.free.data.model.ScanSummary
import com.spacepilot.free.data.model.ScanUiState
import com.spacepilot.free.data.model.StorageInfo
import com.spacepilot.free.ui.components.AppLogo
import com.spacepilot.free.ui.components.CategoryIcon
import com.spacepilot.free.ui.components.SectionHeader
import com.spacepilot.free.ui.components.SpaceCard
import com.spacepilot.free.ui.components.StatBlock
import com.spacepilot.free.ui.components.StorageDonut
import com.spacepilot.free.ui.theme.Blue
import com.spacepilot.free.ui.theme.Green
import com.spacepilot.free.ui.theme.Teal
import com.spacepilot.free.util.Formatters
import java.util.Locale

@Composable
fun HomeScreen(
    storageInfo: StorageInfo,
    summary: ScanSummary?,
    scanState: ScanUiState,
    folderUri: String?,
    operations: List<OperationRecord>,
    onScan: () -> Unit,
    onLargeFiles: () -> Unit,
    onDuplicates: () -> Unit,
    onFiles: () -> Unit,
    onApks: () -> Unit,
    onSettings: () -> Unit,
    onResults: () -> Unit,
    onPickFolder: () -> Unit,
    onRefreshStorage: () -> Unit,
    onViewScan: () -> Unit,
    bottomBar: @Composable () -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
        bottomBar = bottomBar
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 0.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                HomeHeader(onSettings = onSettings)
            }
            item {
                StorageCard(
                    storageInfo = storageInfo,
                    onRefresh = onRefreshStorage
                )
            }
            if (scanState is ScanUiState.Running) {
                item {
                    ScanInProgressCard(scanState = scanState, onClick = onViewScan)
                }
            }
            item {
                FolderAccessCard(
                    connected = folderUri != null,
                    onPickFolder = onPickFolder
                )
            }
            item {
                ScanActionCard(onScan = onScan)
            }
            item {
                SectionHeader(title = stringResource(R.string.home_quick_title))
            }
            item {
                QuickActionsGrid(
                    onLargeFiles = onLargeFiles,
                    onDuplicates = onDuplicates,
                    onFiles = onFiles,
                    onApks = onApks
                )
            }
            item {
                SectionHeader(title = stringResource(R.string.home_categories_title))
            }
            item {
                CategoriesGrid(summary = summary)
            }
            item {
                SectionHeader(
                    title = stringResource(R.string.home_last_scan_title),
                    action = if (summary != null) {
                        {
                            TextButton(onClick = onResults) {
                                Text(stringResource(R.string.home_view_results))
                            }
                        }
                    } else {
                        null
                    }
                )
            }
            item {
                LastScanCard(
                    summary = summary,
                    onResults = onResults,
                    onScan = onScan
                )
            }
            item {
                SectionHeader(title = stringResource(R.string.home_recent_operations))
            }
            if (operations.isEmpty()) {
                item {
                    SpaceCard {
                        Text(
                            text = stringResource(R.string.home_no_operations),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(18.dp)
                        )
                    }
                }
            } else {
                items(operations.take(6), key = { "${it.timestamp}-${it.type}" }) { operation ->
                    OperationRow(operation)
                }
            }
        }
    }
}

@Composable
private fun HomeHeader(onSettings: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = 14.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppLogo(54.dp)
        Spacer(Modifier.width(13.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = stringResource(R.string.app_tagline),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onSettings) {
            Icon(
                Icons.Outlined.Settings,
                contentDescription = stringResource(R.string.action_settings),
                tint = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

@Composable
private fun StorageCard(storageInfo: StorageInfo, onRefresh: () -> Unit) {
    SpaceCard {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.storage_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onRefresh, modifier = Modifier.size(34.dp)) {
                    Icon(
                        Icons.Outlined.Refresh,
                        contentDescription = stringResource(R.string.action_refresh),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
            if (storageInfo.totalBytes <= 0L) {
                Text(
                    text = stringResource(R.string.storage_unavailable),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StorageDonut(
                        usedFraction = storageInfo.usedFraction,
                        label = stringResource(R.string.storage_used)
                    )
                    Spacer(Modifier.width(22.dp))
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatBlock(
                            label = stringResource(R.string.storage_free),
                            value = Formatters.bytes(storageInfo.freeBytes, Locale.getDefault()),
                            accent = Teal
                        )
                        StatBlock(
                            label = stringResource(R.string.storage_used),
                            value = Formatters.bytes(storageInfo.usedBytes, Locale.getDefault()),
                            accent = Blue
                        )
                        StatBlock(
                            label = stringResource(R.string.storage_total),
                            value = Formatters.bytes(storageInfo.totalBytes, Locale.getDefault()),
                            accent = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                HorizontalGradientDivider()
                Spacer(Modifier.height(10.dp))
                Text(
                    text = stringResource(R.string.storage_note),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun HorizontalGradientDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(
                Brush.horizontalGradient(
                    listOf(Color.Transparent, Teal.copy(alpha = 0.3f), Color.Transparent)
                )
            )
    )
}

@Composable
private fun ScanInProgressCard(scanState: ScanUiState.Running, onClick: () -> Unit) {
    SpaceCard(
        onClick = onClick,
        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.5.dp)
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.home_scan_in_progress),
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = pluralStringResource(
                        R.plurals.files_count,
                        scanState.scannedFiles,
                        scanState.scannedFiles
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(Icons.Outlined.ChevronRight, contentDescription = null)
        }
    }
}

@Composable
private fun FolderAccessCard(connected: Boolean, onPickFolder: () -> Unit) {
    SpaceCard(
        containerColor = if (connected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.24f)
        } else {
            MaterialTheme.colorScheme.surface
        }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Teal.copy(alpha = 0.13f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (connected) Icons.Outlined.VerifiedUser else Icons.Outlined.FolderOpen,
                    contentDescription = null,
                    tint = Teal,
                    modifier = Modifier.size(23.dp)
                )
            }
            Spacer(Modifier.width(13.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (connected) {
                        stringResource(R.string.home_access_connected)
                    } else {
                        stringResource(R.string.home_access_title)
                    },
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = if (connected) {
                        stringResource(R.string.home_access_connected_message)
                    } else {
                        stringResource(R.string.home_access_message)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
            TextButton(onClick = onPickFolder) {
                Text(
                    stringResource(
                        if (connected) R.string.home_access_change else R.string.home_access_connect
                    )
                )
            }
        }
    }
}

@Composable
private fun ScanActionCard(onScan: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF0B3E49), Color(0xFF0B2B44), Color(0xFF102A48))
                )
            )
            .border(1.dp, Teal.copy(alpha = 0.25f), MaterialTheme.shapes.extraLarge)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Teal.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.Radar,
                    contentDescription = null,
                    tint = Teal,
                    modifier = Modifier.size(30.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.quick_scan),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFFF0FBFF)
                )
                Text(
                    text = stringResource(R.string.quick_scan_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFA9C6D7)
                )
            }
            Button(
                onClick = onScan,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Teal,
                    contentColor = Color(0xFF002C2A)
                ),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(stringResource(R.string.action_scan))
            }
        }
    }
}

@Composable
private fun QuickActionsGrid(
    onLargeFiles: () -> Unit,
    onDuplicates: () -> Unit,
    onFiles: () -> Unit,
    onApks: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            QuickActionCard(
                icon = Icons.Outlined.Storage,
                title = stringResource(R.string.quick_large),
                subtitle = stringResource(R.string.quick_large_desc),
                tint = Blue,
                onClick = onLargeFiles,
                modifier = Modifier.weight(1f)
            )
            QuickActionCard(
                icon = Icons.Outlined.ContentCopy,
                title = stringResource(R.string.quick_duplicates),
                subtitle = stringResource(R.string.quick_duplicates_desc),
                tint = Teal,
                onClick = onDuplicates,
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            QuickActionCard(
                icon = Icons.Outlined.Folder,
                title = stringResource(R.string.quick_files),
                subtitle = stringResource(R.string.quick_files_desc),
                tint = Green,
                onClick = onFiles,
                modifier = Modifier.weight(1f)
            )
            QuickActionCard(
                icon = Icons.Outlined.Smartphone,
                title = stringResource(R.string.quick_apk),
                subtitle = stringResource(R.string.quick_apk_desc),
                tint = Color(0xFFFF7CB2),
                onClick = onApks,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun QuickActionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    SpaceCard(modifier = modifier.animateContentSize(), onClick = onClick) {
        Column(modifier = Modifier.padding(15.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(tint.copy(alpha = 0.13f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(21.dp))
                }
                Spacer(Modifier.weight(1f))
                Icon(
                    Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.height(13.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CategoriesGrid(summary: ScanSummary?) {
    if (summary == null) {
        SpaceCard {
            Text(
                text = stringResource(R.string.categories_empty),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(18.dp)
            )
        }
        return
    }
    val categories = FileCategory.entries
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        categories.chunked(2).forEach { rowCategories ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                rowCategories.forEach { category ->
                    val stat = summary.categories[category]
                    CategoryCard(
                        category = category,
                        count = stat?.count ?: 0,
                        bytes = stat?.bytes ?: 0L,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (rowCategories.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun CategoryCard(category: FileCategory, count: Int, bytes: Long, modifier: Modifier = Modifier) {
    SpaceCard(modifier = modifier) {
        Row(
            modifier = Modifier.padding(13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CategoryIcon(category, 38.dp)
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = categoryTitle(category),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(
                        R.string.category_count_size,
                        count,
                        Formatters.bytes(bytes, Locale.getDefault())
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun categoryTitle(category: FileCategory): String = stringResource(
    when (category) {
        FileCategory.IMAGES -> R.string.category_images
        FileCategory.VIDEO -> R.string.category_video
        FileCategory.AUDIO -> R.string.category_audio
        FileCategory.DOCUMENTS -> R.string.category_documents
        FileCategory.ARCHIVES -> R.string.category_archives
        FileCategory.APK -> R.string.category_apk
        FileCategory.OTHER -> R.string.category_other
    }
)

@Composable
private fun LastScanCard(summary: ScanSummary?, onResults: () -> Unit, onScan: () -> Unit) {
    SpaceCard {
        Column(modifier = Modifier.padding(18.dp)) {
            if (summary == null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.AutoAwesome,
                        contentDescription = null,
                        tint = Teal,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.home_last_scan_empty_title),
                        style = MaterialTheme.typography.titleSmall
                    )
                }
                Spacer(Modifier.height(7.dp))
                Text(
                    text = stringResource(R.string.home_last_scan_empty_message),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                Button(onClick = onScan) {
                    Text(stringResource(R.string.action_scan))
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = Formatters.date(summary.timestamp, Locale.getDefault()),
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f)
                    )
                    if (summary.limitedAccess) {
                        Text(
                            text = stringResource(R.string.scan_limited_title),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    StatBlock(
                        label = stringResource(R.string.scan_files_scanned),
                        value = pluralStringResource(
                            R.plurals.files_count,
                            summary.scannedFiles,
                            summary.scannedFiles
                        )
                    )
                    StatBlock(
                        label = stringResource(R.string.scan_data_found),
                        value = Formatters.bytes(summary.totalBytes, Locale.getDefault()),
                        accent = Blue
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onResults) {
                        Text(stringResource(R.string.home_view_results))
                    }
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = if (summary.usedFolderAccess) {
                            stringResource(R.string.results_folder_access)
                        } else {
                            stringResource(R.string.results_media_only)
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun OperationRow(operation: OperationRecord) {
    SpaceCard {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val icon = when (operation.type) {
                OperationType.DELETE -> Icons.Outlined.DeleteOutline
                OperationType.SCAN -> Icons.Outlined.Radar
                OperationType.COPY -> Icons.Outlined.ContentCopy
                OperationType.MOVE -> Icons.Outlined.FolderOpen
                OperationType.CHECKSUM -> Icons.Outlined.Description
                OperationType.APK_INSTALL -> Icons.Outlined.Smartphone
            }
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = operation.description,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = Formatters.date(operation.timestamp, Locale.getDefault()),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (operation.bytes > 0) {
                Text(
                    text = Formatters.bytes(operation.bytes, Locale.getDefault()),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
