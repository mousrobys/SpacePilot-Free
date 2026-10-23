package com.spacepilot.free.data

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import com.spacepilot.free.data.model.FileEntry
import com.spacepilot.free.data.model.FileSource
import com.spacepilot.free.util.FileTypes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.IOException
import kotlin.coroutines.coroutineContext

data class SafScanResult(
    val files: List<FileEntry>,
    val inaccessible: List<String>,
    val rootName: String
)

data class BrowserLoadResult(
    val rootName: String,
    val items: List<FileEntry>,
    val error: String? = null
)

data class CopyResult(
    val success: Boolean,
    val message: String? = null,
    val newUri: String? = null
)

class SafRepository(context: Context) {
    private val appContext = context.applicationContext
    private val resolver: ContentResolver = appContext.contentResolver

    fun documentFor(uri: Uri, treeUri: Uri? = null): DocumentFile? {
        return try {
            if (treeUri != null && uri == treeUri) {
                DocumentFile.fromTreeUri(appContext, uri)
            } else {
                DocumentFile.fromSingleUri(appContext, uri)
            }
        } catch (_: Exception) {
            null
        }
    }

    fun rootName(treeUri: Uri): String {
        return runCatching { DocumentFile.fromTreeUri(appContext, treeUri)?.name }.getOrNull()
            ?: treeUri.lastPathSegment
            ?: "Storage"
    }

    suspend fun loadChildren(treeUri: Uri, directoryUri: Uri?): BrowserLoadResult =
        withContext(Dispatchers.IO) {
            val target = if (directoryUri == null || directoryUri == treeUri) {
                DocumentFile.fromTreeUri(appContext, treeUri)
            } else {
                DocumentFile.fromSingleUri(appContext, directoryUri)
            } ?: return@withContext BrowserLoadResult(rootName(treeUri), emptyList(), "Cannot open folder")
            val name = target.name ?: rootName(treeUri)
            try {
                val items = target.listFiles()
                    .mapNotNull { child ->
                        val childName = child.name ?: return@mapNotNull null
                        val directory = child.isDirectory
                        FileEntry(
                            uri = child.uri.toString(),
                            name = childName,
                            size = if (directory) 0L else child.length().coerceAtLeast(0L),
                            modified = child.lastModified(),
                            mimeType = child.type,
                            path = childName,
                            category = FileTypes.categoryFor(childName, child.type, directory),
                            source = FileSource.SAF,
                            isDirectory = directory
                        )
                    }
                    .sortedWith(compareByDescending<FileEntry> { it.isDirectory }.thenBy { it.name.lowercase() })
                BrowserLoadResult(name, items)
            } catch (e: Exception) {
                BrowserLoadResult(name, emptyList(), e.localizedMessage ?: "Folder access error")
            }
        }

    suspend fun scanTree(
        treeUri: Uri,
        onProgress: suspend (files: Int, bytes: Long, current: String) -> Unit
    ): SafScanResult = withContext(Dispatchers.IO) {
        val root = DocumentFile.fromTreeUri(appContext, treeUri)
            ?: return@withContext SafScanResult(emptyList(), listOf("Selected folder is unavailable"), "")
        val rootName = root.name ?: root.uri.lastPathSegment ?: "Storage"
        val files = LinkedHashMap<String, FileEntry>()
        val inaccessible = mutableListOf<String>()
        val stack = ArrayDeque<Pair<DocumentFile, String>>()
        val visited = HashSet<String>()
        stack.addLast(root to rootName)
        var bytes = 0L

        while (stack.isNotEmpty()) {
            coroutineContext.ensureActive()
            val (directory, path) = stack.removeLast()
            if (!visited.add(directory.uri.toString())) continue
            val children = try {
                directory.listFiles()
            } catch (e: SecurityException) {
                inaccessible += path
                continue
            } catch (_: Exception) {
                inaccessible += path
                continue
            }
            for (child in children) {
                coroutineContext.ensureActive()
                val name = child.name
                if (name.isNullOrBlank()) continue
                val childPath = "$path/$name"
                if (child.isDirectory) {
                    stack.addLast(child to childPath)
                    continue
                }
                if (!child.isFile) continue
                val size = runCatching { child.length().coerceAtLeast(0L) }.getOrDefault(0L).coerceAtLeast(0L)
                val entry = FileEntry(
                    uri = child.uri.toString(),
                    name = name,
                    size = size,
                    modified = runCatching { child.lastModified() }.getOrDefault(0L),
                    mimeType = child.type,
                    path = childPath,
                    category = FileTypes.categoryFor(name, child.type),
                    source = FileSource.SAF
                )
                files[entry.uri] = entry
                bytes += size
                onProgress(files.size, bytes, childPath)
            }
        }
        onProgress(files.size, bytes, "")
        SafScanResult(files.values.toList(), inaccessible.distinct(), rootName)
    }

    suspend fun delete(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            DocumentsContract.deleteDocument(resolver, uri)
        } catch (_: Exception) {
            false
        }
    }

    suspend fun copyTo(
        source: FileEntry,
        destinationTreeUri: Uri,
        move: Boolean
    ): CopyResult = withContext(Dispatchers.IO) {
        val destinationRoot = DocumentFile.fromTreeUri(appContext, destinationTreeUri)
            ?: return@withContext CopyResult(false, "Destination folder is unavailable")
        val sourceUri = Uri.parse(source.uri)
        val mime = source.mimeType ?: "application/octet-stream"
        val safeName = source.name.ifBlank { "file" }
        val targetName = uniqueName(destinationRoot, safeName)
        val target = try {
            destinationRoot.createFile(mime, targetName)
        } catch (e: Exception) {
            return@withContext CopyResult(false, e.localizedMessage)
        } ?: return@withContext CopyResult(false, "The destination does not allow file creation")

        try {
            val input = resolver.openInputStream(sourceUri)
                ?: return@withContext CopyResult(false, "Cannot read the source file")
            input.use { sourceStream ->
                val output = resolver.openOutputStream(target.uri)
                    ?: return@withContext CopyResult(false, "Cannot write to the destination")
                output.use { targetStream ->
                    sourceStream.copyTo(targetStream, DEFAULT_BUFFER_SIZE)
                }
            }
            if (move) {
                val deleted = delete(sourceUri)
                if (!deleted) {
                    return@withContext CopyResult(
                        true,
                        "The copy was created, but the original could not be deleted",
                        target.uri.toString()
                    )
                }
            }
            CopyResult(true, null, target.uri.toString())
        } catch (e: Exception) {
            runCatching { target.delete() }
            CopyResult(false, e.localizedMessage)
        }
    }

    private fun uniqueName(directory: DocumentFile, name: String): String {
        if (directory.findFile(name) == null) return name
        val dot = name.lastIndexOf('.')
        val base = if (dot > 0) name.substring(0, dot) else name
        val extension = if (dot > 0) name.substring(dot) else ""
        var index = 1
        while (index < 1000) {
            val candidate = "$base ($index)$extension"
            if (directory.findFile(candidate) == null) return candidate
            index++
        }
        return "$base-${System.currentTimeMillis()}$extension"
    }

    @Throws(IOException::class)
    fun openInput(uri: Uri) = resolver.openInputStream(uri)
}
