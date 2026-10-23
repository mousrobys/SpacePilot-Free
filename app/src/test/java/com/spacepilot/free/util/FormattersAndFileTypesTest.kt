package com.spacepilot.free.util

import com.spacepilot.free.data.model.FileCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class FormattersAndFileTypesTest {

    @Test
    fun bytesFormatsBinaryUnits() {
        assertEquals("0 B", Formatters.bytes(0L, Locale.US))
        assertEquals("1023 B", Formatters.bytes(1023L, Locale.US))
        assertEquals("1 KB", Formatters.bytes(1024L, Locale.US))
        assertEquals("1.5 KB", Formatters.bytes(1536L, Locale.US))
        assertEquals("1 MB", Formatters.bytes(1024L * 1024L, Locale.US))
        assertEquals("2.5 GB", Formatters.bytes((2.5 * 1024 * 1024 * 1024).toLong(), Locale.US))
    }

    @Test
    fun bytesHandlesInvalidValues() {
        assertEquals("—", Formatters.bytes(-1L, Locale.US))
    }

    @Test
    fun humanBytesApproxUsesBinaryUnits() {
        assertEquals("0 B", Formatters.humanBytesApprox(0L))
        assertEquals("1.5 KB", Formatters.humanBytesApprox(1536L))
        assertEquals("1.0 MB", Formatters.humanBytesApprox(1024L * 1024L))
    }

    @Test
    fun percentTruncatesToInteger() {
        assertEquals("0%", Formatters.percent(0f))
        assertEquals("25%", Formatters.percent(0.256f))
        assertEquals("100%", Formatters.percent(1f))
    }

    @Test
    fun categoriesAreDetectedByExtensionAndMime() {
        assertEquals(FileCategory.IMAGES, FileTypes.categoryFor("PHOTO.JPG", null))
        assertEquals(FileCategory.VIDEO, FileTypes.categoryFor("movie.mkv", null))
        assertEquals(FileCategory.AUDIO, FileTypes.categoryFor("song.flac", null))
        assertEquals(FileCategory.DOCUMENTS, FileTypes.categoryFor("report.pdf", null))
        assertEquals(FileCategory.DOCUMENTS, FileTypes.categoryFor("data.bin", "application/pdf"))
        assertEquals(FileCategory.ARCHIVES, FileTypes.categoryFor("backup.zip", null))
        assertEquals(FileCategory.ARCHIVES, FileTypes.categoryFor("data.bin", "application/zip"))
        assertEquals(FileCategory.APK, FileTypes.categoryFor("app.apk", null))
        assertEquals(FileCategory.APK, FileTypes.categoryFor("app.bin", "application/vnd.android.package-archive"))
        assertEquals(FileCategory.OTHER, FileTypes.categoryFor("notes.xyz", "application/octet-stream"))
    }

    @Test
    fun directoriesAreNotClassifiedAsMedia() {
        assertEquals(FileCategory.OTHER, FileTypes.categoryFor("Photos.jpg", null, isDirectory = true))
    }

    @Test
    fun apkDetectionIsCaseInsensitive() {
        assertTrue(FileTypes.isApk("UPDATE.APK", null))
        assertTrue(FileTypes.isApk("app.bin", "application/vnd.android.package-archive"))
        assertFalse(FileTypes.isApk("app.zip", "application/zip"))
    }

    @Test
    fun filterMatchesNullAndExactCategory() {
        assertTrue(FileTypes.matchesFilter(FileCategory.IMAGES, null))
        assertTrue(FileTypes.matchesFilter(FileCategory.IMAGES, FileCategory.IMAGES))
        assertFalse(FileTypes.matchesFilter(FileCategory.IMAGES, FileCategory.VIDEO))
    }
}
