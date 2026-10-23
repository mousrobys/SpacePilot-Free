package com.spacepilot.free.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.spacepilot.free.R
import com.spacepilot.free.data.model.FileAnalysisState
import com.spacepilot.free.data.model.FileCategory
import com.spacepilot.free.data.model.FileEntry
import com.spacepilot.free.data.model.FileSource
import com.spacepilot.free.util.Formatters
import java.util.Locale

@Composable
fun categoryLabel(category: FileCategory): String = androidx.compose.ui.res.stringResource(
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
fun ConfirmActionDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = false,
    details: (@Composable () -> Unit)? = null
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                if (destructive) Icons.Outlined.DeleteOutline else Icons.Outlined.Info,
                contentDescription = null,
                tint = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
        },
        title = { Text(title) },
        text = {
            Column {
                Text(message, style = MaterialTheme.typography.bodyMedium)
                if (details != null) {
                    Spacer(Modifier.height(12.dp))
                    details()
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = if (destructive) {
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                } else {
                    ButtonDefaults.buttonColors()
                }
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(androidx.compose.ui.res.stringResource(R.string.action_cancel))
            }
        }
    )
}

@Composable
fun DeleteConfirmationDialog(
    files: List<FileEntry>,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    extraWarning: String? = null
) {
    val count = files.size
    val total = files.sumOf { it.size }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Outlined.DeleteOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = { Text(androidx.compose.ui.res.stringResource(R.string.delete_title)) },
        text = {
            Column {
                Text(
                    text = androidx.compose.ui.res.pluralStringResource(
                        R.plurals.files_selected,
                        count,
                        count
                    ),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = Formatters.bytes(total, Locale.getDefault()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                Column(
                    modifier = Modifier
                        .heightIn(max = 150.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    files.take(40).forEach { file ->
                        Text(
                            text = file.name,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (files.size > 40) {
                        Text("… +${files.size - 40}", style = MaterialTheme.typography.labelSmall)
                    }
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    text = extraWarning
                        ?: androidx.compose.ui.res.stringResource(R.string.delete_warning),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = androidx.compose.ui.res.stringResource(R.string.delete_system_note),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Text(androidx.compose.ui.res.stringResource(R.string.delete_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(androidx.compose.ui.res.stringResource(R.string.action_cancel))
            }
        }
    )
}

@Composable
fun PermissionRationaleDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Outlined.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = { Text(androidx.compose.ui.res.stringResource(R.string.permission_title)) },
        text = {
            Text(
                text = androidx.compose.ui.res.stringResource(R.string.permission_message),
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(androidx.compose.ui.res.stringResource(R.string.permission_allow))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(androidx.compose.ui.res.stringResource(R.string.action_cancel))
            }
        }
    )
}

@Composable
fun FileAnalysisDialog(
    state: FileAnalysisState,
    onDismiss: () -> Unit,
    onOpen: () -> Unit,
    onInstall: (() -> Unit)? = null
) {
    val file = state.file
    val clipboard = LocalClipboardManager.current
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .heightIn(max = 720.dp),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.padding(22.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FileTypeIcon(file.category, file.isDirectory, 46.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = file.name,
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = categoryLabel(file.category),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                ) {
                    InfoRow(
                        androidx.compose.ui.res.stringResource(R.string.info_size),
                        Formatters.bytes(file.size, Locale.getDefault())
                    )
                    InfoRow(
                        androidx.compose.ui.res.stringResource(R.string.info_modified),
                        Formatters.date(file.modified, Locale.getDefault())
                    )
                    InfoRow(
                        androidx.compose.ui.res.stringResource(R.string.info_path),
                        file.path
                    )
                    InfoRow(
                        androidx.compose.ui.res.stringResource(R.string.info_mime),
                        file.mimeType ?: androidx.compose.ui.res.stringResource(R.string.common_unknown)
                    )
                    InfoRow(
                        androidx.compose.ui.res.stringResource(R.string.info_source),
                        androidx.compose.ui.res.stringResource(
                            if (file.source == FileSource.SAF) R.string.info_source_saf else R.string.info_source_media
                        )
                    )

                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = androidx.compose.ui.res.stringResource(R.string.info_checksum),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (state.loading) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(10.dp))
                            Text(
                                androidx.compose.ui.res.stringResource(R.string.info_calculating),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    } else if (state.sha256 != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = state.sha256,
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = {
                                    clipboard.setText(AnnotatedString(state.sha256))
                                }
                            ) {
                                Icon(
                                    Icons.Outlined.ContentCopy,
                                    contentDescription = androidx.compose.ui.res.stringResource(R.string.info_copy_checksum),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                    if (state.error != null) {
                        Text(
                            text = state.error,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    if (file.category == FileCategory.APK) {
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = androidx.compose.ui.res.stringResource(R.string.info_package),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        val apk = state.apkInfo
                        if (state.loading) {
                            Text(
                                text = androidx.compose.ui.res.stringResource(R.string.common_loading),
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        } else if (apk?.packageName == null) {
                            Text(
                                text = androidx.compose.ui.res.stringResource(R.string.info_no_metadata),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            InfoRow(
                                androidx.compose.ui.res.stringResource(R.string.info_label),
                                apk.label ?: "—"
                            )
                            InfoRow(
                                androidx.compose.ui.res.stringResource(R.string.info_package_name),
                                apk.packageName
                            )
                            InfoRow(
                                androidx.compose.ui.res.stringResource(R.string.info_version),
                                listOfNotNull(apk.versionName, apk.versionCode?.toString()).joinToString(" · ")
                            )
                            InfoRow(
                                androidx.compose.ui.res.stringResource(R.string.info_min_sdk),
                                apk.minSdk?.toString() ?: "—"
                            )
                            InfoRow(
                                androidx.compose.ui.res.stringResource(R.string.info_target_sdk),
                                apk.targetSdk?.toString() ?: "—"
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = androidx.compose.ui.res.stringResource(R.string.info_apk_warning),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
                Spacer(Modifier.height(18.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(onClick = onOpen, modifier = Modifier.weight(1f)) {
                        Text(androidx.compose.ui.res.stringResource(R.string.action_open))
                    }
                    if (onInstall != null) {
                        OutlinedButton(onClick = onInstall, modifier = Modifier.weight(1f)) {
                            Text(androidx.compose.ui.res.stringResource(R.string.apk_install))
                        }
                    }
                    TextButton(onClick = onDismiss) {
                        Text(androidx.compose.ui.res.stringResource(R.string.action_close))
                    }
                }
            }
        }
    }
}

@Composable
fun TextInfoDialog(title: String, text: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(text = text, style = MaterialTheme.typography.bodyMedium)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(androidx.compose.ui.res.stringResource(R.string.action_close))
            }
        }
    )
}

@Composable
fun RadioOptionRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal
        )
    }
}
