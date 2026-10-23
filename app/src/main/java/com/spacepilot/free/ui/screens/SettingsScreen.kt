package com.spacepilot.free.ui.screens

import android.Manifest
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.SettingsBackupRestore
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.spacepilot.free.R
import com.spacepilot.free.data.model.AppSettings
import com.spacepilot.free.data.model.LanguageMode
import com.spacepilot.free.data.model.ThemeMode
import com.spacepilot.free.ui.components.AppLogo
import com.spacepilot.free.ui.components.ConfirmActionDialog
import com.spacepilot.free.ui.components.RadioOptionRow
import com.spacepilot.free.ui.components.SettingsSectionCard
import com.spacepilot.free.ui.components.SpaceCard
import com.spacepilot.free.ui.components.TextInfoDialog
import com.spacepilot.free.ui.theme.Teal
import com.spacepilot.free.util.PermissionUtils

@Composable
fun SettingsScreen(
    settings: AppSettings,
    folderUri: String?,
    folderName: String,
    versionName: String,
    onThemeChange: (ThemeMode) -> Unit,
    onLanguageChange: (LanguageMode) -> Unit,
    onPickFolder: () -> Unit,
    onDisconnectFolder: () -> Unit,
    onClearHistory: () -> Unit,
    onResetSettings: () -> Unit,
    onOpenSystemSettings: () -> Unit,
    bottomBar: @Composable () -> Unit
) {
    var showReset by remember { mutableStateOf(false) }
    var showClear by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    var showPrivacy by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val permissionMap = remember(context) { PermissionUtils.grantedMap(context) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
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
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                SettingsSectionCard(title = stringResource(R.string.settings_appearance)) {
                    Row(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.Palette,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.settings_theme),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    ThemeMode.entries.forEach { mode ->
                        RadioOptionRow(
                            label = stringResource(
                                when (mode) {
                                    ThemeMode.DARK -> R.string.theme_dark
                                    ThemeMode.LIGHT -> R.string.theme_light
                                    ThemeMode.SYSTEM -> R.string.theme_system
                                }
                            ),
                            selected = settings.themeMode == mode,
                            onClick = { onThemeChange(mode) }
                        )
                    }
                }
            }
            item {
                SettingsSectionCard(title = stringResource(R.string.settings_language)) {
                    Row(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.Language,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.settings_language),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    LanguageMode.entries.forEach { mode ->
                        RadioOptionRow(
                            label = stringResource(
                                when (mode) {
                                    LanguageMode.SYSTEM -> R.string.lang_system
                                    LanguageMode.RU -> R.string.lang_ru
                                    LanguageMode.EN -> R.string.lang_en
                                }
                            ),
                            selected = settings.languageMode == mode,
                            onClick = { onLanguageChange(mode) }
                        )
                    }
                }
            }
            item {
                SettingsSectionCard(title = stringResource(R.string.settings_access)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.Folder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(34.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (folderUri == null) {
                                    stringResource(R.string.settings_folder_none)
                                } else {
                                    folderName.ifBlank { stringResource(R.string.settings_folder) }
                                },
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = stringResource(R.string.settings_folder_access_note),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        TextButton(onClick = onPickFolder) {
                            Text(
                                stringResource(
                                    if (folderUri == null) R.string.settings_pick_folder
                                    else R.string.settings_change_folder
                                )
                            )
                        }
                        if (folderUri != null) {
                            TextButton(onClick = onDisconnectFolder) {
                                Text(stringResource(R.string.settings_disconnect_folder))
                            }
                        }
                    }
                }
            }
            item {
                SettingsSectionCard(title = stringResource(R.string.settings_permissions)) {
                    Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp)) {
                        permissionMap.forEach { (permission, granted) ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    if (granted) Icons.Outlined.CheckCircle else Icons.Outlined.Lock,
                                    contentDescription = null,
                                    tint = if (granted) Teal else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(17.dp)
                                )
                                Spacer(Modifier.width(9.dp))
                                Text(
                                    text = permissionLabel(permission),
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = stringResource(
                                        if (granted) R.string.common_permission_granted
                                        else R.string.common_permission_denied
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (granted) Teal else MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = stringResource(R.string.settings_permissions_desc),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextButton(onClick = onOpenSystemSettings) {
                            Text(stringResource(R.string.settings_open_system))
                        }
                    }
                }
            }
            item {
                SettingsSectionCard(title = stringResource(R.string.settings_history)) {
                    SettingActionRow(
                        icon = Icons.Outlined.DeleteSweep,
                        title = stringResource(R.string.settings_clear_history),
                        subtitle = stringResource(R.string.settings_clear_history_desc),
                        onClick = { showClear = true }
                    )
                    SettingActionRow(
                        icon = Icons.Outlined.SettingsBackupRestore,
                        title = stringResource(R.string.settings_reset),
                        subtitle = stringResource(R.string.settings_reset_desc),
                        onClick = { showReset = true }
                    )
                }
            }
            item {
                SettingsSectionCard(title = stringResource(R.string.settings_about)) {
                    SettingActionRow(
                        icon = Icons.Outlined.Info,
                        title = stringResource(R.string.settings_about),
                        subtitle = stringResource(R.string.settings_version, versionName),
                        onClick = { showAbout = true }
                    )
                    SettingActionRow(
                        icon = Icons.Outlined.PrivacyTip,
                        title = stringResource(R.string.settings_privacy),
                        subtitle = null,
                        onClick = { showPrivacy = true }
                    )
                }
            }
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AppLogo(46.dp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = stringResource(R.string.settings_version, versionName),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    if (showReset) {
        ConfirmActionDialog(
            title = stringResource(R.string.settings_reset_confirm_title),
            message = stringResource(R.string.settings_reset_confirm_message),
            confirmLabel = stringResource(R.string.settings_reset),
            onConfirm = {
                showReset = false
                onResetSettings()
            },
            onDismiss = { showReset = false }
        )
    }
    if (showClear) {
        ConfirmActionDialog(
            title = stringResource(R.string.settings_history_confirm_title),
            message = stringResource(R.string.settings_history_confirm_message),
            confirmLabel = stringResource(R.string.settings_clear_history),
            destructive = true,
            onConfirm = {
                showClear = false
                onClearHistory()
            },
            onDismiss = { showClear = false }
        )
    }
    if (showAbout) {
        TextInfoDialog(
            title = stringResource(R.string.settings_about_title),
            text = stringResource(R.string.settings_about_text),
            onDismiss = { showAbout = false }
        )
    }
    if (showPrivacy) {
        TextInfoDialog(
            title = stringResource(R.string.settings_privacy_title),
            text = stringResource(R.string.settings_privacy_text),
            onDismiss = { showPrivacy = false }
        )
    }
}

@Composable
private fun SettingActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
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
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        TextButton(onClick = onClick) {
            Text(stringResource(R.string.action_open))
        }
    }
}

@Composable
private fun permissionLabel(permission: String): String = stringResource(
    when (permission) {
        Manifest.permission.READ_EXTERNAL_STORAGE -> R.string.permission_read_storage
        Manifest.permission.WRITE_EXTERNAL_STORAGE -> R.string.permission_write_storage
        Manifest.permission.READ_MEDIA_IMAGES -> R.string.permission_media_images
        Manifest.permission.READ_MEDIA_VIDEO -> R.string.permission_media_video
        Manifest.permission.READ_MEDIA_AUDIO -> R.string.permission_media_audio
        else -> R.string.permission_unknown
    }
)
