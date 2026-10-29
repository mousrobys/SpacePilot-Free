package com.spacepilot.free.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RenamePatternTest {

    @Test
    fun appliesNameAndIndexAndKeepsExtension() {
        assertEquals("photo_01.jpg", RenamePattern.apply("{name}_{n}", "photo.jpg", 1, 4))
        assertEquals("photo_04.jpg", RenamePattern.apply("{name}_{n}", "photo.jpg", 4, 4))
    }

    @Test
    fun padsIndexToTotalWidth() {
        assertEquals("clip_001.mp4", RenamePattern.apply("clip_{n}", "a.mp4", 1, 120))
        assertEquals("clip_120.mp4", RenamePattern.apply("clip_{n}", "a.mp4", 120, 120))
    }

    @Test
    fun usesMinimumTwoDigitIndex() {
        assertEquals("a_01.txt", RenamePattern.apply("{name}_{n}", "a.txt", 1, 3))
    }

    @Test
    fun appendsIndexWhenPatternHasNoNameOrIndex() {
        assertEquals("report_01.pdf", RenamePattern.apply("report", "old.pdf", 1, 2))
    }

    @Test
    fun explicitExtensionTokenReplacesOriginal() {
        assertEquals("a_01.png", RenamePattern.apply("{name}_{n}.{ext}", "a.png", 1, 2))
    }

    @Test
    fun doesNotDuplicateExtensionWhenPatternAlreadyEndsWithIt() {
        assertEquals("a.bak.txt", RenamePattern.apply("{name}.bak.txt", "a.txt", 1, 2))
    }

    @Test
    fun sanitizesPathSeparators() {
        val result = RenamePattern.apply("a/b\\c", "x.txt", 1, 1)
        assertTrue(result.none { it == '/' || it == '\\' })
        assertTrue(result.endsWith(".txt"))
    }

    @Test
    fun blankPatternKeepsOriginalName() {
        assertEquals("photo.jpg", RenamePattern.apply("   ", "photo.jpg", 1, 1))
    }

    @Test
    fun directoriesDoNotGetExtension() {
        assertEquals("Photos_01", RenamePattern.apply("{name}_{n}", "Photos", 1, 2))
    }

    @Test
    fun previewStopsAtLimit() {
        val names = listOf("a.txt", "b.txt", "c.txt", "d.txt")
        assertEquals(
            listOf("img_01.txt", "img_02.txt", "img_03.txt"),
            RenamePattern.preview("img_{n}", names, limit = 3)
        )
        assertTrue(RenamePattern.preview("img_{n}", emptyList()).isEmpty())
    }
}
