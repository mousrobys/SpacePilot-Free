package com.spacepilot.free.util

import com.spacepilot.free.data.model.FileCategory

object FileTypes {
    private val imageExtensions = setOf("jpg", "jpeg", "png", "gif", "webp", "bmp", "heic", "heif", "dng", "svg", "tiff")
    private val videoExtensions = setOf("mp4", "mkv", "avi", "mov", "webm", "3gp", "m4v", "ts", "flv", "wmv")
    private val audioExtensions = setOf("mp3", "aac", "flac", "wav", "ogg", "m4a", "opus", "amr", "wma", "mid", "midi")
    private val documentExtensions = setOf("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "rtf", "odt", "ods", "odp", "csv", "epub", "fb2", "json", "xml", "html", "htm")
    private val archiveExtensions = setOf("zip", "rar", "7z", "tar", "gz", "bz2", "xz", "zst", "iso", "cab", "lz", "lzma")

    fun categoryFor(name: String, mimeType: String?, isDirectory: Boolean = false): FileCategory {
        if (isDirectory) return FileCategory.OTHER
        val extension = name.substringAfterLast('.', "").lowercase()
        val mime = mimeType?.lowercase().orEmpty()
        return when {
            extension == "apk" || extension == "apks" || extension == "xapk" || mime.contains("android.package-archive") -> FileCategory.APK
            mime.startsWith("image/") || extension in imageExtensions -> FileCategory.IMAGES
            mime.startsWith("video/") || extension in videoExtensions -> FileCategory.VIDEO
            mime.startsWith("audio/") || extension in audioExtensions -> FileCategory.AUDIO
            extension in archiveExtensions || mime.contains("zip") || mime.contains("compressed") || mime.contains("archive") -> FileCategory.ARCHIVES
            mime.startsWith("text/") || mime.contains("pdf") || mime.contains("word") ||
                mime.contains("sheet") || mime.contains("presentation") || extension in documentExtensions -> FileCategory.DOCUMENTS
            else -> FileCategory.OTHER
        }
    }

    fun matchesFilter(category: FileCategory, filter: FileCategory?): Boolean =
        filter == null || category == filter

    fun isApk(name: String, mimeType: String?): Boolean =
        name.substringAfterLast('.', "").equals("apk", true) ||
            mimeType.equals("application/vnd.android.package-archive", true)
}
