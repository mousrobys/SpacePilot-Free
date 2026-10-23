package com.spacepilot.free.data

import android.content.Context
import com.spacepilot.free.data.model.CategoryStat
import com.spacepilot.free.data.model.DuplicateGroup
import com.spacepilot.free.data.model.DuplicateVerification
import com.spacepilot.free.data.model.FileCategory
import com.spacepilot.free.data.model.FileEntry
import com.spacepilot.free.data.model.FileSource
import com.spacepilot.free.data.model.ScanSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

data class CachedScan(
    val summary: ScanSummary,
    val files: List<FileEntry>
)

class ScanCacheRepository(context: Context) {
    private val appContext = context.applicationContext
    private val cacheFile = File(appContext.filesDir, SettingsRepository.SCAN_CACHE_FILE)
    private val tempFile = File(appContext.filesDir, "${SettingsRepository.SCAN_CACHE_FILE}.tmp")

    suspend fun save(summary: ScanSummary, files: List<FileEntry>) = withContext(Dispatchers.IO) {
        runCatching {
            val root = JSONObject()
                .put("version", 1)
                .put("timestamp", summary.timestamp)
                .put("inaccessible", JSONArray(summary.inaccessible))
                .put("duplicatesAnalyzed", summary.duplicatesAnalyzed)
                .put("usedFolderAccess", summary.usedFolderAccess)
                .put("limitedAccess", summary.limitedAccess)
                .put("files", JSONArray().apply {
                    prioritized(files).take(MAX_CACHED_FILES).forEach { put(it.toJson()) }
                })
                .put("duplicates", JSONArray().apply {
                    summary.duplicateGroups.forEach { group ->
                        put(
                            JSONObject()
                                .put("size", group.size)
                                .put("verification", group.verification.name)
                                .put("files", JSONArray().apply {
                                    group.files.forEach { put(it.toJson()) }
                                })
                        )
                    }
                })
            tempFile.outputStream().use { output ->
                GZIPOutputStream(output).bufferedWriter().use { writer ->
                    writer.write(root.toString())
                }
            }
            if (cacheFile.exists()) cacheFile.delete()
            tempFile.renameTo(cacheFile)
        }
    }

    suspend fun load(): CachedScan? = withContext(Dispatchers.IO) {
        if (!cacheFile.exists()) return@withContext null
        runCatching {
            val text = GZIPInputStream(cacheFile.inputStream()).bufferedReader().use { it.readText() }
            val root = JSONObject(text)
            val files = root.optJSONArray("files")?.let { parseFiles(it) }.orEmpty()
            val duplicateGroups = root.optJSONArray("duplicates")?.let { array ->
                buildList {
                    for (i in 0 until array.length()) {
                        val group = array.optJSONObject(i) ?: continue
                        val groupFiles = group.optJSONArray("files")?.let(::parseFiles).orEmpty()
                        if (groupFiles.size < 2) continue
                        val verification = runCatching {
                            DuplicateVerification.valueOf(group.optString("verification"))
                        }.getOrDefault(DuplicateVerification.FULL_SHA256)
                        add(
                            DuplicateGroup(
                                size = group.optLong("size"),
                                files = groupFiles,
                                verification = verification
                            )
                        )
                    }
                }
            }.orEmpty()
            val categories = files.groupBy { it.category }.mapValues { (_, entries) ->
                CategoryStat(entries.size, entries.sumOf { it.size })
            }
            val inaccessible = root.optJSONArray("inaccessible")?.let { array ->
                buildList {
                    for (i in 0 until array.length()) add(array.optString(i))
                }
            }.orEmpty()
            val summary = ScanSummary(
                timestamp = root.optLong("timestamp"),
                scannedFiles = files.size,
                totalBytes = files.sumOf { it.size },
                categories = categories,
                largeFiles = files.filter { it.size >= LARGE_FILE_THRESHOLD }
                    .sortedByDescending { it.size },
                duplicateGroups = duplicateGroups,
                inaccessible = inaccessible,
                duplicatesAnalyzed = root.optBoolean("duplicatesAnalyzed"),
                usedFolderAccess = root.optBoolean("usedFolderAccess"),
                limitedAccess = root.optBoolean("limitedAccess")
            )
            if (summary.timestamp <= 0L) null else CachedScan(summary, files)
        }.getOrNull()
    }

    fun delete() {
        runCatching { cacheFile.delete() }
        runCatching { tempFile.delete() }
    }

    private fun prioritized(files: List<FileEntry>): List<FileEntry> {
        if (files.size <= MAX_CACHED_FILES) return files
        val important = files.filter {
            it.size >= LARGE_FILE_THRESHOLD || it.category == FileCategory.APK
        }
        val rest = files.filterNot { it in important }
        return (important + rest).distinctBy { it.uri }
    }

    private fun parseFiles(array: JSONArray): List<FileEntry> {
        val result = ArrayList<FileEntry>(array.length())
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            val category = runCatching { FileCategory.valueOf(obj.optString("category")) }
                .getOrDefault(FileCategory.OTHER)
            val source = runCatching { FileSource.valueOf(obj.optString("source")) }
                .getOrDefault(FileSource.SAF)
            val entry = FileEntry(
                uri = obj.optString("uri"),
                name = obj.optString("name"),
                size = obj.optLong("size"),
                modified = obj.optLong("modified"),
                mimeType = obj.optString("mime").takeIf { it.isNotBlank() && it != "null" },
                path = obj.optString("path"),
                category = category,
                source = source
            )
            if (entry.uri.isNotBlank() && entry.name.isNotBlank()) result += entry
        }
        return result
    }

    private fun FileEntry.toJson(): JSONObject = JSONObject()
        .put("uri", uri)
        .put("name", name)
        .put("size", size)
        .put("modified", modified)
        .put("mime", mimeType ?: "")
        .put("path", path)
        .put("category", category.name)
        .put("source", source.name)

    companion object {
        const val LARGE_FILE_THRESHOLD = 100L * 1024L * 1024L
        private const val MAX_CACHED_FILES = 40_000
    }
}
