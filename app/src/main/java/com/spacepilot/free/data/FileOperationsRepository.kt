package com.spacepilot.free.data

import android.app.PendingIntent
import android.content.Context
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.spacepilot.free.data.model.FileEntry
import com.spacepilot.free.data.model.FileSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class MediaDeleteDirectResult(
    val deleted: List<FileEntry>,
    val failed: List<FileEntry>,
    val systemIntentSender: IntentSender? = null,
    val systemFiles: List<FileEntry> = emptyList()
)

class FileOperationsRepository(context: Context) {
    private val appContext = context.applicationContext
    private val resolver = appContext.contentResolver

    suspend fun deleteSafFiles(files: List<FileEntry>): Pair<List<FileEntry>, List<FileEntry>> =
        withContext(Dispatchers.IO) {
            val deleted = mutableListOf<FileEntry>()
            val failed = mutableListOf<FileEntry>()
            for (file in files) {
                val success = try {
                    android.provider.DocumentsContract.deleteDocument(resolver, Uri.parse(file.uri))
                } catch (_: Exception) {
                    false
                }
                if (success) deleted += file else failed += file
            }
            deleted to failed
        }

    fun createSystemDeleteRequest(files: List<FileEntry>): IntentSender? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R || files.isEmpty()) return null
        return try {
            val pendingIntent: PendingIntent = MediaStore.createDeleteRequest(
                resolver,
                files.map { Uri.parse(it.uri) }
            )
            pendingIntent.intentSender
        } catch (_: Exception) {
            null
        }
    }

    suspend fun deleteMediaDirect(files: List<FileEntry>): MediaDeleteDirectResult =
        withContext(Dispatchers.IO) {
            val deleted = mutableListOf<FileEntry>()
            val failed = mutableListOf<FileEntry>()
            var systemSender: IntentSender? = null
            var pendingFile: FileEntry? = null
            for (file in files) {
                if (systemSender != null) {
                    failed += file
                    continue
                }
                try {
                    val rows = resolver.delete(Uri.parse(file.uri), null, null)
                    if (rows > 0) deleted += file else failed += file
                } catch (e: SecurityException) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val recoverable = e as? android.app.RecoverableSecurityException
                        val sender = recoverable?.userAction?.actionIntent?.intentSender
                        if (sender != null) {
                            systemSender = sender
                            pendingFile = file
                        } else {
                            failed += file
                        }
                    } else {
                        failed += file
                    }
                } catch (_: Exception) {
                    failed += file
                }
            }
            MediaDeleteDirectResult(
                deleted = deleted,
                failed = failed,
                systemIntentSender = systemSender,
                systemFiles = listOfNotNull(pendingFile)
            )
        }

    fun isSaf(entry: FileEntry): Boolean = entry.source == FileSource.SAF
}
