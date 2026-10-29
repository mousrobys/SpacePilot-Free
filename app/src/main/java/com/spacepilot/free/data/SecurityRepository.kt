package com.spacepilot.free.data

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import com.spacepilot.free.data.model.ApkRisk
import com.spacepilot.free.data.model.ApkSecurityReport
import com.spacepilot.free.data.model.CloudReputationState
import com.spacepilot.free.data.model.FileEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate

/**
 * Локальный эвристический анализ APK. Не является полноценным антивирусом:
 * проверяет подпись, опасные разрешения, экспортированные компоненты и отладочные флаги.
 */
class ApkSecurityRepository(context: Context) {
    private val appContext = context.applicationContext
    private val resolver = appContext.contentResolver

    suspend fun scan(file: FileEntry, sha256: String?): ApkSecurityReport = withContext(Dispatchers.IO) {
        val cacheDir = File(appContext.cacheDir, "apk_security").apply { mkdirs() }
        val target = File(cacheDir, "scan-${System.currentTimeMillis()}.apk")
        try {
            val input = resolver.openInputStream(Uri.parse(file.uri))
                ?: return@withContext ApkSecurityReport(
                    error = "Cannot read the APK",
                    sha256 = sha256
                )
            input.use { source ->
                target.outputStream().use { output ->
                    source.copyTo(output, DEFAULT_BUFFER_SIZE)
                }
            }
            @Suppress("DEPRECATION")
            val info = appContext.packageManager.getPackageArchiveInfo(
                target.absolutePath,
                PackageManager.GET_PERMISSIONS or
                    PackageManager.GET_ACTIVITIES or
                    PackageManager.GET_RECEIVERS or
                    PackageManager.GET_SERVICES or
                    PackageManager.GET_PROVIDERS or
                    PackageManager.GET_SIGNATURES
            ) ?: return@withContext ApkSecurityReport(
                error = "Package metadata is unavailable",
                sha256 = sha256
            )
            buildReport(info, sha256, target.absolutePath)
        } catch (e: Exception) {
            ApkSecurityReport(error = e.localizedMessage ?: "APK analysis failed", sha256 = sha256)
        } finally {
            target.delete()
        }
    }

    private fun buildReport(info: PackageInfo, sha256: String?, archivePath: String): ApkSecurityReport {
        val appInfo = info.applicationInfo
        val pm = appContext.packageManager

        val signature = info.signatures?.firstOrNull()
        val certBytes = signature?.toByteArray()
        val signatureSha = certBytes?.let { sha256Hex(it) }
        val subject = certBytes?.let { bytes ->
            runCatching {
                val certificate = CertificateFactory.getInstance("X.509")
                    .generateCertificate(ByteArrayInputStream(bytes)) as X509Certificate
                certificate.subjectX500Principal.name
            }.getOrNull()
        }
        val debugCertificate = subject?.let {
            it.contains("Android Debug", ignoreCase = true) || it.contains("O=Android", ignoreCase = true)
        } ?: false
        val debuggable = appInfo?.flags?.and(ApplicationInfo.FLAG_DEBUGGABLE) != 0

        val dangerous = info.requestedPermissions.orEmpty()
            .filter { permissionWeight(it) > 0 }
            .distinct()
            .sortedByDescending { permissionWeight(it) }

        val exported = collectExportedComponents(info, appInfo?.permission)

        var score = 0
        if (signature == null) score += 30
        if (debugCertificate) score += 25
        if (debuggable) score += 25
        score += dangerous.sumOf { permissionWeight(it) }.coerceAtMost(40)
        score += (exported.size * 3).coerceAtMost(15)
        val targetSdk = appInfo?.targetSdkVersion
        if (targetSdk != null && targetSdk < 23) score += 15
        score = score.coerceIn(0, 100)

        val risk = when {
            signature == null -> ApkRisk.UNKNOWN
            score >= 50 -> ApkRisk.HIGH
            score >= 20 -> ApkRisk.MEDIUM
            else -> ApkRisk.LOW
        }

        val label = runCatching {
            appInfo?.sourceDir = archivePath
            appInfo?.publicSourceDir = archivePath
            info.applicationInfo?.loadLabel(pm)?.toString()
        }.getOrNull()

        return ApkSecurityReport(
            packageName = info.packageName,
            label = label,
            targetSdk = targetSdk,
            signatureSha256 = signatureSha,
            signatureSubject = subject,
            debugCertificate = debugCertificate,
            debuggable = debuggable,
            dangerousPermissions = dangerous,
            exportedComponents = exported,
            risk = risk,
            score = score,
            sha256 = sha256
        )
    }

    private fun collectExportedComponents(info: PackageInfo, applicationPermission: String?): List<String> {
        if (applicationPermission != null) return emptyList()
        val components = buildList {
            info.activities.orEmpty().forEach { activity ->
                if (activity.exported && activity.permission == null) {
                    add(activity.name.substringAfterLast('.'))
                }
            }
            info.services.orEmpty().forEach { service ->
                if (service.exported && service.permission == null) {
                    add(service.name.substringAfterLast('.'))
                }
            }
            info.receivers.orEmpty().forEach { receiver ->
                if (receiver.exported && receiver.permission == null) {
                    add(receiver.name.substringAfterLast('.'))
                }
            }
            info.providers.orEmpty().forEach { provider ->
                if (provider.exported && provider.readPermission == null && provider.writePermission == null) {
                    add(provider.name.substringAfterLast('.'))
                }
            }
        }
        return components.distinct().take(20)
    }

    private fun permissionWeight(permission: String): Int = when (permission) {
        "android.permission.SEND_SMS",
        "android.permission.RECEIVE_SMS",
        "android.permission.READ_SMS",
        "android.permission.RECEIVE_MMS",
        "android.permission.READ_CALL_LOG",
        "android.permission.WRITE_CALL_LOG",
        "android.permission.PROCESS_OUTGOING_CALLS" -> 12
        "android.permission.REQUEST_INSTALL_PACKAGES",
        "android.permission.SYSTEM_ALERT_WINDOW",
        "android.permission.BIND_ACCESSIBILITY_SERVICE",
        "android.permission.BIND_DEVICE_ADMIN" -> 10
        "android.permission.READ_CONTACTS",
        "android.permission.WRITE_CONTACTS",
        "android.permission.GET_ACCOUNTS",
        "android.permission.ACCESS_FINE_LOCATION",
        "android.permission.ACCESS_BACKGROUND_LOCATION",
        "android.permission.MANAGE_EXTERNAL_STORAGE",
        "android.permission.QUERY_ALL_PACKAGES" -> 8
        "android.permission.CAMERA",
        "android.permission.RECORD_AUDIO",
        "android.permission.READ_PHONE_STATE",
        "android.permission.CALL_PHONE",
        "android.permission.READ_PHONE_NUMBERS" -> 6
        "android.permission.RECEIVE_BOOT_COMPLETED",
        "android.permission.WRITE_SETTINGS" -> 4
        "android.permission.POST_NOTIFICATIONS" -> 1
        else -> 0
    }

    private fun sha256Hex(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
}

/**
 * Необязательная облачная проверка SHA-256 через VirusTotal API v3.
 * Наружу уходит только хеш, не содержимое файла.
 */
class CloudReputationRepository {

    suspend fun lookup(sha256: String, apiKey: String): CloudReputationState =
        withContext(Dispatchers.IO) {
            val key = apiKey.trim()
            if (key.isEmpty()) {
                return@withContext CloudReputationState.Error("API key is missing", unauthorized = true)
            }
            val connection = try {
                (URL("https://www.virustotal.com/api/v3/files/$sha256").openConnection() as HttpURLConnection)
            } catch (e: Exception) {
                return@withContext CloudReputationState.Error(e.localizedMessage ?: "Network error")
            }
            try {
                connection.requestMethod = "GET"
                connection.connectTimeout = 15_000
                connection.readTimeout = 15_000
                connection.setRequestProperty("x-apikey", key)
                connection.setRequestProperty("accept", "application/json")
                when (val code = connection.responseCode) {
                    200 -> parse(connection.inputStream.bufferedReader().use { it.readText() })
                    401, 403 -> CloudReputationState.Error("The API key was rejected", unauthorized = true)
                    404 -> CloudReputationState.NotFound
                    429 -> CloudReputationState.Error("VirusTotal rate limit reached")
                    else -> CloudReputationState.Error("VirusTotal returned HTTP $code")
                }
            } catch (e: Exception) {
                CloudReputationState.Error(e.localizedMessage ?: "Network error")
            } finally {
                connection.disconnect()
            }
        }

    private fun parse(json: String): CloudReputationState {
        val root = JSONObject(json)
        val attributes = root.optJSONObject("data")?.optJSONObject("attributes")
        val stats = attributes?.optJSONObject("last_analysis_stats")
            ?: return CloudReputationState.Error("Unexpected response format")
        return CloudReputationState.Success(
            malicious = stats.optInt("malicious"),
            suspicious = stats.optInt("suspicious"),
            harmless = stats.optInt("harmless"),
            undetected = stats.optInt("undetected"),
            name = attributes.optString("meaningful_name").takeIf { it.isNotBlank() }
        )
    }
}
