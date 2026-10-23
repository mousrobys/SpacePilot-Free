package com.spacepilot.free.data

import android.content.Context
import android.os.Environment
import android.os.StatFs
import com.spacepilot.free.data.model.StorageInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class StorageRepository(context: Context) {
    private val appContext = context.applicationContext

    @Suppress("DEPRECATION")
    suspend fun readStorageInfo(): StorageInfo = withContext(Dispatchers.IO) {
        runCatching {
            val path = Environment.getExternalStorageDirectory()
            val stat = StatFs(path.absolutePath)
            val total = stat.totalBytes
            val free = stat.availableBytes
            StorageInfo(
                totalBytes = total.coerceAtLeast(0L),
                usedBytes = (total - free).coerceAtLeast(0L),
                freeBytes = free.coerceAtLeast(0L)
            )
        }.getOrElse {
            runCatching {
                val stat = StatFs(appContext.filesDir.absolutePath)
                val total = stat.totalBytes
                val free = stat.availableBytes
                StorageInfo(
                    totalBytes = total.coerceAtLeast(0L),
                    usedBytes = (total - free).coerceAtLeast(0L),
                    freeBytes = free.coerceAtLeast(0L)
                )
            }.getOrDefault(StorageInfo())
        }
    }
}
