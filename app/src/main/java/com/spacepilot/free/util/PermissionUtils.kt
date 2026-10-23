package com.spacepilot.free.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

object PermissionUtils {
    fun mediaReadPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_AUDIO
            )
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    fun scanPermissions(): Array<String> {
        val read = mediaReadPermissions().toMutableList()
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            read += Manifest.permission.WRITE_EXTERNAL_STORAGE
        }
        return read.distinct().toTypedArray()
    }

    fun hasMediaReadAccess(context: Context): Boolean =
        mediaReadPermissions().any { isGranted(context, it) }

    fun hasAllScanPermissions(context: Context): Boolean =
        scanPermissions().all { isGranted(context, it) }

    fun isGranted(context: Context, permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    fun grantedMap(context: Context): Map<String, Boolean> =
        scanPermissions().associateWith { isGranted(context, it) }
}
