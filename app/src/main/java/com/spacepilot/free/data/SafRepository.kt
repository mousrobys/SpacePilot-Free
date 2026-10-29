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
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
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

data class SafOperationResult(
    val success: Boolean,
    val message: String? = null,
    val newUri: String? = null,
    val bytes: Long = 0L,
    val processed: Int = 0,
    val failed: Int = 0
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

    suspend fun rename(uri: Uri, newName: String): SafOperationResult = withContext(Dispatchers.IO) {
        val clean = newName.trim()
        if (clean.isEmpty()) {
            return@withContext SafOperationResult(false, "The name must not be empty")
        }
        if (clean.contains('/') || clean.contains('\\')) {
            return@withContext SafOperationResult(false, "The name must not contain slashes")
        }
        try {
            val renamed = DocumentsContract.renameDocument(resolver, uri, clean)
            if (renamed == null) {
                SafOperationResult(false, "The provider refused to rename the item")
            } else {
                SafOperationResult(true, newUri = renamed.toString())
            }
        } catch (e: Exception) {
            SafOperationResult(false, e.localizedMessage)
        }
    }

    suspend fun createDirectory(
        treeUri: Uri,
        parentUri: Uri?,
        name: String
    ): SafOperationResult = withContext(Dispatchers.IO) {
        val clean = name.trim()
        if (clean.isEmpty()) {
            return@withContext SafOperationResult(false, "The folder name must not be empty")
        }
        if (clean.contains('/') || clean.contains('\\')) {
            return@withContext SafOperationResult(false, "The folder name must not contain slashes")
        }
        try {
            val documentUri = buildParentDocumentUri(treeUri, parentUri)
            val created = DocumentsContract.createDocument(
                resolver,
                documentUri,
                DocumentsContract.Document.MIME_TYPE_DIR,
                clean
            )
            if (created == null) {
                SafOperationResult(false, "The provider refused to create the folder")
            } else {
                SafOperationResult(true, newUri = created.toString())
            }
        } catch (e: Exception) {
            SafOperationResult(false, e.localizedMessage)
        }
    }

    suspend fun zipFiles(
        files: List<FileEntry>,
        destinationTreeUri: Uri,
        archiveName: String,
        onProgress: suspend (processed: Int, total: Int) -> Unit = { _, _ -> }
    ): SafOperationResult = withContext(Dispatchers.IO) {
        val sources = files.filter { !it.isDirectory }
        if (sources.isEmpty()) {
            return@withContext SafOperationResult(false, "There are no files to compress")
        }
        val destinationRoot = DocumentFile.fromTreeUri(appContext, destinationTreeUri)
            ?: return@withContext SafOperationResult(false, "Destination folder is unavailable")
        val safeName = archiveName
            .trim()
            .ifBlank { "SpacePilot-${System.currentTimeMillis()}.zip" }
            .let { if (it.endsWith(".zip", ignoreCase = true)) it else "$it.zip" }
        val target = try {
            destinationRoot.createFile("application/zip", uniqueName(destinationRoot, safeName))
        } catch (e: Exception) {
            return@withContext SafOperationResult(false, e.localizedMessage)
        } ?: return@withContext SafOperationResult(false, "The destination does not allow file creation")

        var processed = 0
        var failed = 0
        var bytes = 0L
        val entryNames = HashSet<String>()
        try {
            val output = resolver.openOutputStream(target.uri)
                ?: return@withContext SafOperationResult(false, "Cannot write to the destination")
            output.use { raw ->
                ZipOutputStream(raw.buffered()).use { zip ->
                    for (file in sources) {
                        coroutineContext.ensureActive()
                        val input = try {
                            resolver.openInputStream(Uri.parse(file.uri))
                        } catch (_: Exception) {
                            null
                        }
                        if (input == null) {
                            failed++
                            processed++
                            onProgress(processed, sources.size)
                            continue
                        }
                        val entryName = uniqueEntryName(file.name, entryNames)
                        try {
                            zip.putNextEntry(ZipEntry(entryName))
                            input.use { source ->
                                val copied = source.copyTo(zip, DEFAULT_BUFFER_SIZE)
                                bytes += copied
                            }
                            zip.closeEntry()
                            processed++
                        } catch (_: Exception) {
                            failed++
                            processed++
                            runCatching { zip.closeEntry() }
                        }
                        onProgress(processed, sources.size)
                    }
                    zip.finish()
                }
            }
            if (processed == 0) {
                runCatching { target.delete() }
                SafOperationResult(false, "No file could be compressed")
            } else {
                SafOperationResult(
                    success = true,
                    message = if (failed > 0) "Some files could not be added" else null,
                    newUri = target.uri.toString(),
                    bytes = bytes,
                    processed = processed,
                    failed = failed
                )
            }
        } catch (e: Exception) {
            runCatching { target.delete() }
            SafOperationResult(false, e.localizedMessage)
        }
    }

    private fun buildParentDocumentUri(treeUri: Uri, parentUri: Uri?): Uri {
        val parent = parentUri ?: treeUri
        return when {
            DocumentsContract.isTreeUri(parent) -> DocumentsContract.buildDocumentUriUsingTree(
                parent,
                DocumentsContract.getTreeDocumentId(parent)
            )
            DocumentsContract.isDocumentUri(appContext, parent) -> DocumentsContract.buildDocumentUriUsingTree(
                treeUri,
                DocumentsContract.getDocumentId(parent)
            )
            else -> parent
        }
    }

    private fun uniqueEntryName(name: String, used: MutableSet<String>): String {
        val safe = name.ifBlank { "file" }.replace('/', '_').replace('\\', '_')
        if (used.add(safe)) return safe
        val dot = safe.lastIndexOf('.')
        val base = if (dot > 0) safe.substring(0, dot) else safe
        val extension = if (dot > 0) safe.substring(dot) else ""
        var index = 1
        while (index < 10000) {
            val candidate = "$base ($index)$extension"
            if (used.add(candidate)) return candidate
            index++
        }
        val fallback = "$base-${System.currentTimeMillis()}$extension"
        used.add(fallback)
        return fallback
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
