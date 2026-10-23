package com.spacepilot.free.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.spacepilot.free.data.model.FileEntry
import com.spacepilot.free.util.Formatters
import java.util.Locale

@Composable
fun FileListRow(
    file: FileEntry,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean? = null,
    onSelectedChange: ((Boolean) -> Unit)? = null,
    onInfo: (() -> Unit)? = null,
    onMore: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    showPath: Boolean = true
) {
    SpaceCard(
        modifier = modifier,
        onClick = onClick,
        containerColor = if (selected == true) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f)
        } else {
            MaterialTheme.colorScheme.surface
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = if (selected != null) 4.dp else 14.dp, end = 6.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selected != null && onSelectedChange != null) {
                Checkbox(
                    checked = selected,
                    onCheckedChange = onSelectedChange,
                    modifier = Modifier.size(44.dp)
                )
            }
            FileTypeIcon(file.category, file.isDirectory)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = file.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (file.isDirectory) {
                        Icon(
                            Icons.Outlined.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    text = if (file.isDirectory) {
                        file.path.takeIf { it != file.name } ?: ""
                    } else {
                        buildString {
                            append(Formatters.bytes(file.size, Locale.getDefault()))
                            if (file.modified > 0L) {
                                append("  ·  ")
                                append(Formatters.shortDate(file.modified, Locale.getDefault()))
                            }
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (showPath && !file.isDirectory && file.path.isNotBlank() && file.path != file.name) {
                    Text(
                        text = file.path,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.82f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            when {
                trailing != null -> trailing()
                onMore != null -> IconButton(onClick = onMore) {
                    Icon(
                        Icons.Outlined.MoreVert,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                onInfo != null -> IconButton(onClick = onInfo) {
                    Icon(
                        Icons.Outlined.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                else -> Spacer(Modifier.width(8.dp))
            }
        }
    }
}

@Composable
fun FileNameMetaRow(name: String, meta: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = name,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = meta,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun SimpleFileRow(
    file: FileEntry,
    meta: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null
) {
    SpaceCard(modifier = modifier, onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FileTypeIcon(file.category, file.isDirectory, 38.dp)
            FileNameMetaRow(file.name, meta, Modifier.weight(1f))
            trailing?.invoke()
        }
    }
}
