package com.spacepilot.free.util

/**
 * Шаблон пакетного переименования.
 *
 * Поддерживаемые подстановки:
 *  - `{name}` — исходное имя без расширения;
 *  - `{n}`    — порядковый номер (с ведущими нулями);
 *  - `{ext}`  — исходное расширение без точки.
 *
 * Если в шаблоне нет ни `{name}`, ни `{n}`, номер добавляется автоматически,
 * чтобы файлы не получили одинаковые имена.
 */
object RenamePattern {

    fun apply(pattern: String, originalName: String, index: Int, total: Int): String {
        val trimmed = pattern.trim()
        if (trimmed.isEmpty()) return originalName

        val isDirectory = !originalName.contains('.')
        val dot = originalName.lastIndexOf('.')
        val baseName = if (!isDirectory && dot > 0) originalName.substring(0, dot) else originalName
        val extension = if (!isDirectory && dot > 0) originalName.substring(dot + 1) else ""

        val width = total.toString().length.coerceAtLeast(2)
        val number = index.toString().padStart(width, '0')

        var effective = trimmed
        val hasIndex = effective.contains("{n}")
        val hasName = effective.contains("{name}")
        if (!hasIndex && !hasName) {
            effective += "_{n}"
        }

        var result = effective
            .replace("{name}", baseName)
            .replace("{n}", number)
            .replace("{ext}", extension)

        if (!trimmed.contains("{ext}") && extension.isNotEmpty()) {
            val alreadyHasExtension = result.endsWith(".$extension", ignoreCase = true)
            if (!alreadyHasExtension) result = "$result.$extension"
        }

        result = result
            .replace('/', '_')
            .replace('\\', '_')
            .replace("\u0000", "")
            .trim()

        return result.ifBlank { originalName }
    }

    fun preview(pattern: String, names: List<String>, limit: Int = 3): List<String> {
        if (names.isEmpty()) return emptyList()
        return names.take(limit).mapIndexed { i, name ->
            apply(pattern, name, i + 1, names.size)
        }
    }
}
