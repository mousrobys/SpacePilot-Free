package com.spacepilot.free.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.spacepilot.free.data.model.FileCategory
import com.spacepilot.free.data.model.FileEntry
import com.spacepilot.free.data.model.FileSource
import com.spacepilot.free.util.FileTypes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

data class MediaScanResult(
    val files: List<FileEntry>,
    val deniedCollections: List<String>,
    val folderNames: List<String> = emptyList()
)

class MediaStoreRepository(context: Context) {
    private val appContext = context.applicationContext

    private data class CollectionQuery(
        val uri: Uri,
        val kind: String,
        val defaultCategory: FileCategory? = null
    )

    suspend fun scan(
        onProgress: suspend (files: Int, bytes: Long, fraction: Float?, current: String) -> Unit
    ): MediaScanResult = withContext(Dispatchers.IO) {
        val queries = buildQueries()
        val counts = mutableMapOf<CollectionQuery, Int>()
        val denied = mutableListOf<String>()

        for (query in queries) {
            try {
                appContext.contentResolver.query(
                    query.uri,
                    arrayOf(MediaStore.MediaColumns._ID),
                    null,
                    null,
                    null
                )?.use { cursor ->
                    counts[query] = cursor.count
                }
            } catch (_: SecurityException) {
                denied += query.kind
            } catch (_: Exception) {
                // A broken provider should not stop analysis of the remaining collections.
            }
        }

        val projectedTotal = counts.values.sum()
        var processed = 0
        var bytes = 0L
        val files = LinkedHashMap<String, FileEntry>()
        var lastReport = 0L

        for (query in queries) {
            if (!counts.containsKey(query)) continue
            try {
                val projection = projectionFor(query)
                appContext.contentResolver.query(
                    query.uri,
                    projection,
                    null,
                    null,
                    "${MediaStore.MediaColumns.DATE_MODIFIED} DESC"
                )?.use { cursor ->
                    val idIndex = cursor.getColumnIndex(MediaStore.MediaColumns._ID)
                    val nameIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(MediaStore.MediaColumns.SIZE)
                    val dateIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_MODIFIED)
                    val mimeIndex = cursor.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE)
                    val relativeIndex = cursor.getColumnIndex(MediaStore.MediaColumns.RELATIVE_PATH)
                    val dataIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)

                    while (cursor.moveToNext()) {
                        coroutineContext.ensureActive()
                        val id = cursor.getLong(idIndex)
                        val name = cursor.getString(nameIndex) ?: continue
                        val size = cursor.getLong(sizeIndex).coerceAtLeast(0L)
                        val modified = cursor.getLong(dateIndex) * 1000L
                        val mime = cursor.getString(mimeIndex)
                        val relative = if (relativeIndex >= 0) cursor.getString(relativeIndex) else null
                        val data = if (dataIndex >= 0) cursor.getString(dataIndex) else null
                        val uri = ContentUris.withAppendedId(query.uri, id)
                        val path = buildPath(relative, data, name)
                        val category = query.defaultCategory
                            ?: FileTypes.categoryFor(name, mime)
                        val entry = FileEntry(
                            uri = uri.toString(),
                            name = name,
                            size = size,
                            modified = modified,
                            mimeType = mime,
                            path = path,
                            category = category,
                            source = FileSource.MEDIA_STORE
                        )
                        files[entry.uri] = entry
                        processed++
                        bytes += size

                        val now = System.currentTimeMillis()
                        if (now - lastReport > 120L) {
                            lastReport = now
                            onProgress(
                                files.size,
                                bytes,
                                if (projectedTotal > 0) processed.toFloat() / projectedTotal else null,
                                path
                            )
                        }
                    }
                }
            } catch (_: SecurityException) {
                denied += query.kind
            } catch (_: Exception) {
                // Ignore isolated provider failures; the scan summary reports limited access.
            }
        }

        onProgress(files.size, bytes, if (projectedTotal > 0) 1f else null, "")
        MediaScanResult(
            files = files.values.toList(),
            deniedCollections = denied.distinct()
        )
    }

    private fun buildQueries(): List<CollectionQuery> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            listOf(
                CollectionQuery(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "images", FileCategory.IMAGES),
                CollectionQuery(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, "video", FileCategory.VIDEO),
                CollectionQuery(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, "audio", FileCategory.AUDIO),
                CollectionQuery(MediaStore.Downloads.EXTERNAL_CONTENT_URI, "downloads")
            )
        } else {
            listOf(
                CollectionQuery(MediaStore.Files.getContentUri("external"), "files")
            )
        }
    }

    private fun projectionFor(query: CollectionQuery): Array<String> {
        val columns = mutableListOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.DATE_MODIFIED,
            MediaStore.MediaColumns.MIME_TYPE
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            columns += MediaStore.MediaColumns.RELATIVE_PATH
        } else {
            columns += MediaStore.MediaColumns.DATA
        }
        return columns.toTypedArray()
    }

    private fun buildPath(relative: String?, data: String?, name: String): String {
        return when {
            !relative.isNullOrBlank() -> {
                val folder = relative.trimEnd('/')
                if (folder.isBlank()) name else "$folder/$name"
            }
            !data.isNullOrBlank() -> data
            else -> name
        }
    }
}
