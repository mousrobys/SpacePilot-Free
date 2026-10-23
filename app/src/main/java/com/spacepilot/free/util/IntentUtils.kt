package com.spacepilot.free.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.spacepilot.free.data.model.FileEntry

object IntentUtils {
    fun openFile(context: Context, file: FileEntry): Boolean {
        val uri = Uri.parse(file.uri)
        val mime = file.mimeType
            ?: android.webkit.MimeTypeMap.getSingleton()
                .getMimeTypeFromExtension(file.extension)
            ?: "*/*"
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mime)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return try {
            context.startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, com.spacepilot.free.R.string.error_no_app, Toast.LENGTH_SHORT).show()
            false
        } catch (_: SecurityException) {
            Toast.makeText(context, com.spacepilot.free.R.string.error_access, Toast.LENGTH_SHORT).show()
            false
        }
    }

    fun installApk(context: Context, file: FileEntry): Boolean {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(Uri.parse(file.uri), "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, com.spacepilot.free.R.string.error_no_installer, Toast.LENGTH_SHORT).show()
            false
        } catch (_: SecurityException) {
            Toast.makeText(context, com.spacepilot.free.R.string.error_access, Toast.LENGTH_SHORT).show()
            false
        }
    }

    fun openAppSettings(context: Context) {
        val intent = Intent(
            android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null)
        )
        runCatching { context.startActivity(intent) }
    }
}
