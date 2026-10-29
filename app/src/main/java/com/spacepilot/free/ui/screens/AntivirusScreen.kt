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
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.spacepilot.free.R
import com.spacepilot.free.data.model.ApkRisk
import com.spacepilot.free.data.model.ApkScanState
import com.spacepilot.free.data.model.ApkSecurityReport
import com.spacepilot.free.data.model.CloudReputationState
import com.spacepilot.free.data.model.FileEntry
import com.spacepilot.free.ui.components.EmptyState
import com.spacepilot.free.ui.components.SpaceCard
import com.spacepilot.free.util.Formatters
import java.util.Locale

@Composable
fun AntivirusScreen(
    apkFiles: List<FileEntry>,
    scans: Map<String, ApkScanState>,
    cloudEnabled: Boolean,
    hasScanResults: Boolean,
    onBack: () -> Unit,
    onScanAll: (List<FileEntry>) -> Unit,
    onScanFile: (FileEntry) -> Unit,
    onCancel: () -> Unit,
    onScanStorage: () -> Unit,
    onOpenFile: (FileEntry) -> Unit
) {
    val busy = scans.values.any { it.loading }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.security_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                },
                actions = {
                    if (busy) {
                        TextButton(onClick = onCancel) {
                            Text(stringResource(R.string.security_cancel))
                        }
                    } else if (apkFiles.isNotEmpty()) {
                        TextButton(onClick = { onScanAll(apkFiles) }) {
                            Text(stringResource(R.string.security_scan_all))
                        }
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
                Column {
                    Text(
                        text = stringResource(R.string.security_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.security_found, apkFiles.size),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.Security,
                            contentDescription = null,
                            tint = if (cloudEnabled) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = stringResource(
                                if (cloudEnabled) R.string.security_cloud_enabled
                                else R.string.security_cloud_off
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.security_local_note),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (apkFiles.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Outlined.Security,
                        title = stringResource(
                            if (hasScanResults) R.string.security_empty_title
                            else R.string.security_need_scan
                        ),
                        message = stringResource(R.string.security_empty_message),
                        action = {
                            Button(onClick = onScanStorage) {
                                Text(stringResource(R.string.action_scan))
                            }
                        }
                    )
                }
            } else {
                items(apkFiles, key = { it.uri }) { file ->
                    val state = scans[file.uri]
                    ApkScanCard(
                        file = file,
                        state = state,
                        onScan = { onScanFile(file) },
                        onOpen = { onOpenFile(file) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ApkScanCard(
    file: FileEntry,
    state: ApkScanState?,
    onScan: () -> Unit,
    onOpen: () -> Unit
) {
    SpaceCard {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = file.name,
                        style = MaterialTheme.typography.titleSmall,
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
                }
                val report = state?.report
                RiskBadge(report?.risk ?: ApkRisk.UNKNOWN)
            }

            if (state?.loading == true) {
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.security_scanning),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            state?.report?.let { report ->
                Spacer(Modifier.height(10.dp))
                ReportBody(report = report, cloud = state.cloud)
            }

            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onScan, enabled = state?.loading != true) {
                    Text(
                        stringResource(
                            if (state?.report != null) R.string.security_rescan
                            else R.string.security_scan
                        )
                    )
                }
                TextButton(onClick = onOpen) {
                    Text(stringResource(R.string.action_open))
                }
            }
        }
    }
}

@Composable
private fun ReportBody(report: ApkSecurityReport, cloud: CloudReputationState) {
    if (report.error != null) {
        Text(
            text = report.error,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
        )
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        report.packageName?.let { packageName ->
            KeyValue(stringResource(R.string.security_package), packageName)
        }
        report.targetSdk?.let { sdk ->
            KeyValue(stringResource(R.string.security_target_sdk), sdk.toString())
        }
        KeyValue(
            stringResource(R.string.security_risk),
            "${riskLabel(report.risk)} · ${stringResource(R.string.security_score, report.score)}"
        )
        if (report.signatureSha256 == null) {
            Text(
                text = stringResource(R.string.security_no_signature),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        } else {
            Text(
                text = stringResource(
                    R.string.security_signature,
                    report.signatureSha256.take(24) + "…"
                ),
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (report.debugCertificate) {
            WarningLine(stringResource(R.string.security_debug_cert))
        }
        if (report.debuggable) {
            WarningLine(stringResource(R.string.security_debuggable))
        }
        if (report.dangerousPermissions.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.security_permissions, report.dangerousPermissions.size),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
            report.dangerousPermissions.take(8).forEach { permission ->
                Text(
                    text = permission.substringAfterLast('.'),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (report.exportedComponents.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.security_components, report.exportedComponents.size),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
            report.exportedComponents.take(8).forEach { component ->
                Text(
                    text = component,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = cloudText(cloud),
            style = MaterialTheme.typography.bodySmall,
            color = if (cloud is CloudReputationState.Error) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}

@Composable
private fun cloudText(state: CloudReputationState): String = when (state) {
    CloudReputationState.Idle -> stringResource(R.string.security_cloud_off)
    CloudReputationState.Loading -> stringResource(R.string.security_cloud_loading)
    is CloudReputationState.Success -> if (state.malicious + state.suspicious > 0) {
        stringResource(R.string.security_cloud_found, state.malicious, state.suspicious)
    } else {
        stringResource(
            R.string.security_cloud_clean,
            state.harmless + state.undetected
        )
    }
    CloudReputationState.NotFound -> stringResource(R.string.security_cloud_not_found)
    is CloudReputationState.Error -> if (state.unauthorized) {
        stringResource(R.string.security_cloud_no_key)
    } else {
        stringResource(R.string.security_cloud_error, state.message)
    }
}

@Composable
private fun KeyValue(label: String, value: String) {
    Text(
        text = "$label $value",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun WarningLine(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            Icons.Outlined.WarningAmber,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(14.dp)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.error
        )
    }
}

@Composable
private fun RiskBadge(risk: ApkRisk) {
    val color = when (risk) {
        ApkRisk.LOW -> Color(0xFF2BB673)
        ApkRisk.MEDIUM -> Color(0xFFE0A106)
        ApkRisk.HIGH -> MaterialTheme.colorScheme.error
        ApkRisk.UNKNOWN -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.16f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = riskLabel(risk),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
    }
}

@Composable
private fun riskLabel(risk: ApkRisk): String = stringResource(
    when (risk) {
        ApkRisk.LOW -> R.string.security_risk_low
        ApkRisk.MEDIUM -> R.string.security_risk_medium
        ApkRisk.HIGH -> R.string.security_risk_high
        ApkRisk.UNKNOWN -> R.string.security_risk_unknown
    }
)
