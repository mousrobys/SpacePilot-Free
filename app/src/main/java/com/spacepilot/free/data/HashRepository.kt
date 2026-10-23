package com.spacepilot.free.data

import android.content.Context
import android.content.pm.PackageManager
import com.spacepilot.free.data.model.ApkPackageInfo
import com.spacepilot.free.data.model.DuplicateGroup
import com.spacepilot.free.data.model.DuplicateVerification
import com.spacepilot.free.data.model.FileEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.security.MessageDigest
import kotlin.coroutines.coroutineContext

data class DuplicateAnalysisResult(
    val groups: List<DuplicateGroup>,
    val failedFiles: Int
)

class HashRepository(context: Context) {
    private val appContext = context.applicationContext
    private val resolver = appContext.contentResolver

    suspend fun findDuplicates(
        files: List<FileEntry>,
        onProgress: suspend (hashed: Int, total: Int) -> Unit
    ): DuplicateAnalysisResult = withContext(Dispatchers.IO) {
        val candidates = files.filter { !it.isDirectory && it.size > 0L }
            .groupBy { it.size }
            .filterValues { it.size > 1 }
            .values
            .flatten()
        val total = candidates.size
        var hashed = 0
        var failed = 0
        val groups = mutableListOf<DuplicateGroup>()

        val sizeGroups = files.asSequence()
            .filter { !it.isDirectory && it.size > 0L }
            .groupBy { it.size }
            .filterValues { it.size > 1 }

        for ((size, sameSize) in sizeGroups) {
            coroutineContext.ensureActive()
            val verification = if (size > SAMPLED_THRESHOLD) {
                DuplicateVerification.SAMPLED_SHA256
            } else {
                DuplicateVerification.FULL_SHA256
            }
            val byHash = LinkedHashMap<String, MutableList<FileEntry>>()
            for (file in sameSize) {
                coroutineContext.ensureActive()
                val hash = try {
                    hashFile(file.uri, file.size, verification)
                } catch (_: Exception) {
                    null
                }
                hashed++
                if (hash == null) {
                    failed++
                } else {
                    byHash.getOrPut(hash) { mutableListOf() }.add(file)
                }
                if (hashed % 3 == 0 || hashed == total) {
                    onProgress(hashed, total)
                }
            }
            byHash.values.filter { it.size > 1 }.forEach { matching ->
                groups += DuplicateGroup(
                    size = size,
                    files = matching.sortedBy { it.path },
                    verification = verification
                )
            }
            onProgress(hashed, total)
        }
        DuplicateAnalysisResult(
            groups = groups.sortedByDescending { it.reclaimableBytes },
            failedFiles = failed
        )
    }

    suspend fun sha256(file: FileEntry): String = withContext(Dispatchers.IO) {
        val input = resolver.openInputStream(android.net.Uri.parse(file.uri))
            ?: throw IllegalStateException("Cannot open file")
        input.use { stream ->
            val digest = MessageDigest.getInstance("SHA-256")
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                coroutineContext.ensureActive()
                val read = stream.read(buffer)
                if (read < 0) break
                if (read > 0) digest.update(buffer, 0, read)
            }
            digest.digest().toHex()
        }
    }

    private fun hashFile(uri: String, size: Long, verification: DuplicateVerification): String? {
        val input = resolver.openInputStream(android.net.Uri.parse(uri)) ?: return null
        input.use { stream ->
            val digest = MessageDigest.getInstance("SHA-256")
            if (verification == DuplicateVerification.FULL_SHA256) {
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    val read = stream.read(buffer)
                    if (read < 0) break
                    if (read > 0) digest.update(buffer, 0, read)
                }
            } else {
                digest.update("size:$size".toByteArray())
                val segments = sampledSegments(size)
                var position = 0L
                val buffer = ByteArray(SEGMENT_SIZE)
                for ((offset, length) in segments) {
                    skipFully(stream, offset - position)
                    position = offset
                    var remaining = length
                    while (remaining > 0) {
                        val read = stream.read(buffer, 0, minOf(buffer.size, remaining))
                        if (read < 0) break
                        if (read > 0) {
                            digest.update(buffer, 0, read)
                            remaining -= read
                            position += read
                        }
                    }
                    digest.update("offset:$offset".toByteArray())
                }
            }
            return digest.digest().toHex()
        }
    }

    private fun sampledSegments(size: Long): List<Pair<Long, Int>> {
        val head = SEGMENT_SIZE.toLong().coerceAtMost(size)
        val tailOffset = (size - SEGMENT_SIZE).coerceAtLeast(0L)
        val tail = (size - tailOffset).toInt()
        val middleOffset = ((size / 2) - (SEGMENT_SIZE / 2)).coerceIn(head, (tailOffset - SEGMENT_SIZE).coerceAtLeast(head))
        val middle = if (middleOffset in head until tailOffset) {
            minOf(SEGMENT_SIZE.toLong(), tailOffset - middleOffset).toInt()
        } else {
            0
        }
        return buildList {
            add(0L to head.toInt())
            if (middle > 0) add(middleOffset to middle)
            if (tail > 0) add(tailOffset to tail)
        }.distinctBy { it.first }
    }

    private fun skipFully(input: InputStream, count: Long) {
        var remaining = count
        while (remaining > 0) {
            val skipped = input.skip(remaining)
            if (skipped > 0) {
                remaining -= skipped
            } else {
                if (input.read() < 0) return
                remaining--
            }
        }
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    companion object {
        private const val SAMPLED_THRESHOLD = 32L * 1024L * 1024L
        private const val SEGMENT_SIZE = 128 * 1024
    }
}

class FileAnalysisRepository(context: Context) {
    private val appContext = context.applicationContext
    private val resolver = appContext.contentResolver
    private val hashRepository = HashRepository(context)

    suspend fun sha256(file: FileEntry): String = hashRepository.sha256(file)

    suspend fun inspectApk(file: FileEntry): ApkPackageInfo = withContext(Dispatchers.IO) {
        val cacheDir = File(appContext.cacheDir, "apk_inspect").apply { mkdirs() }
        val target = File(cacheDir, "inspect-${System.currentTimeMillis()}.apk")
        try {
            val input = resolver.openInputStream(android.net.Uri.parse(file.uri))
                ?: return@withContext ApkPackageInfo(null, null, null, null, null, null)
            input.use { source ->
                target.outputStream().use { output ->
                    source.copyTo(output, DEFAULT_BUFFER_SIZE)
                }
            }
            val pm = appContext.packageManager
            val info = pm.getPackageArchiveInfo(target.absolutePath, 0)
                ?: return@withContext ApkPackageInfo(null, null, null, null, null, null)
            val appInfo = info.applicationInfo
            appInfo?.sourceDir = target.absolutePath
            appInfo?.publicSourceDir = target.absolutePath
            val label = runCatching { appInfo?.loadLabel(pm)?.toString() }.getOrNull()
            val versionCode = if (android.os.Build.VERSION.SDK_INT >= 28) {
                info.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                info.versionCode.toLong()
            }
            ApkPackageInfo(
                packageName = info.packageName,
                versionName = info.versionName,
                versionCode = versionCode,
                label = label,
                minSdk = appInfo?.minSdkVersion,
                targetSdk = appInfo?.targetSdkVersion
            )
        } catch (_: PackageManager.NameNotFoundException) {
            ApkPackageInfo(null, null, null, null, null, null)
        } finally {
            target.delete()
        }
    }
}
