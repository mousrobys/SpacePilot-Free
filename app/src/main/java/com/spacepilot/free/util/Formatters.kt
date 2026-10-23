package com.spacepilot.free.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.ln
import kotlin.math.pow

object Formatters {
    fun bytes(value: Long, locale: Locale = Locale.getDefault()): String {
        if (value < 0) return "—"
        if (value < 1024) return "$value B"
        val units = arrayOf("KB", "MB", "GB", "TB")
        var size = value.toDouble()
        var unit = -1
        do {
            size /= 1024.0
            unit++
        } while (size >= 1024.0 && unit < units.lastIndex)
        val pattern = if (size >= 100 || size % 1.0 == 0.0) "%.0f %s" else "%.1f %s"
        return String.format(locale, pattern, size, units[unit])
    }

    fun count(value: Int, locale: Locale = Locale.getDefault()): String =
        String.format(locale, "%,d", value)

    fun percent(fraction: Float): String = "${(fraction * 100f).toInt()}%"

    fun date(timestamp: Long, locale: Locale = Locale.getDefault()): String {
        if (timestamp <= 0L) return "—"
        return DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, locale)
            .format(Date(timestamp))
    }

    fun shortDate(timestamp: Long, locale: Locale = Locale.getDefault()): String {
        if (timestamp <= 0L) return "—"
        return DateFormat.getDateInstance(DateFormat.MEDIUM, locale).format(Date(timestamp))
    }

    fun humanBytesApprox(value: Long): String {
        if (value <= 0L) return "0 B"
        val exp = (ln(value.toDouble()) / ln(1024.0)).toInt()
        val unit = arrayOf("B", "KB", "MB", "GB", "TB")[exp.coerceIn(0, 4)]
        val pre = value / 1024.0.pow(exp.toDouble())
        return String.format(Locale.US, "%.1f %s", pre, unit)
    }
}

fun Uri.displayName(context: Context): String? {
    if (scheme == "content") {
        runCatching {
            context.contentResolver.query(this, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) return cursor.getString(0)
            }
        }
    }
    return lastPathSegment?.substringAfterLast('/')
}
