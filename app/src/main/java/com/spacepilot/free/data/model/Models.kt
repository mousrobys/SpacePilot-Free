package com.spacepilot.free.data.model

enum class FileCategory {
    IMAGES,
    VIDEO,
    AUDIO,
    DOCUMENTS,
    ARCHIVES,
    APK,
    OTHER
}

enum class FileSource {
    MEDIA_STORE,
    SAF
}

data class FileEntry(
    val uri: String,
    val name: String,
    val size: Long,
    val modified: Long,
    val mimeType: String?,
    val path: String,
    val category: FileCategory,
    val source: FileSource,
    val isDirectory: Boolean = false
) {
    val extension: String
        get() = name.substringAfterLast('.', "").lowercase()
}

data class CategoryStat(
    val count: Int = 0,
    val bytes: Long = 0
)

data class StorageInfo(
    val totalBytes: Long = 0L,
    val usedBytes: Long = 0L,
    val freeBytes: Long = 0L
) {
    val usedFraction: Float
        get() = if (totalBytes <= 0L) 0f else (usedBytes.toDouble() / totalBytes.toDouble()).toFloat().coerceIn(0f, 1f)

    val usedPercent: Int
        get() = (usedFraction * 100f).toInt()
}

enum class ScanPhase {
    PREPARING,
    MEDIA_LIBRARY,
    FOLDER,
    FINISHING
}

sealed interface ScanUiState {
    data object Idle : ScanUiState
    data class Running(
        val phase: ScanPhase,
        val scannedFiles: Int = 0,
        val scannedBytes: Long = 0L,
        val progress: Float? = null,
        val currentLocation: String = ""
    ) : ScanUiState

    data class Completed(val summary: ScanSummary) : ScanUiState
    data class Failed(val message: String, val details: String? = null) : ScanUiState
    data object Cancelled : ScanUiState
}

enum class DuplicateVerification {
    FULL_SHA256,
    SAMPLED_SHA256
}

data class DuplicateGroup(
    val size: Long,
    val files: List<FileEntry>,
    val verification: DuplicateVerification
) {
    val totalBytes: Long
        get() = size * files.size

    val reclaimableBytes: Long
        get() = size * (files.size - 1)
}

data class ScanSummary(
    val timestamp: Long,
    val scannedFiles: Int,
    val totalBytes: Long,
    val categories: Map<FileCategory, CategoryStat>,
    val largeFiles: List<FileEntry>,
    val duplicateGroups: List<DuplicateGroup>,
    val inaccessible: List<String>,
    val duplicatesAnalyzed: Boolean = false,
    val usedFolderAccess: Boolean = false,
    val limitedAccess: Boolean = false,
    val sourceLabel: String = ""
)

sealed interface DuplicateUiState {
    data object NotAnalyzed : DuplicateUiState
    data object NoCandidates : DuplicateUiState
    data class Running(
        val hashedFiles: Int,
        val totalCandidates: Int,
        val progress: Float?
    ) : DuplicateUiState

    data class Completed(val groups: List<DuplicateGroup>) : DuplicateUiState
    data class Failed(val message: String) : DuplicateUiState
}

enum class ThemeMode {
    DARK,
    LIGHT,
    SYSTEM
}

enum class LanguageMode {
    SYSTEM,
    RU,
    EN
}

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.DARK,
    val languageMode: LanguageMode = LanguageMode.SYSTEM
)

enum class OperationType {
    DELETE,
    MOVE,
    COPY,
    SCAN,
    CHECKSUM,
    APK_INSTALL
}

data class OperationRecord(
    val timestamp: Long,
    val type: OperationType,
    val description: String,
    val bytes: Long = 0L
)

data class ApkPackageInfo(
    val packageName: String?,
    val versionName: String?,
    val versionCode: Long?,
    val label: String?,
    val minSdk: Int?,
    val targetSdk: Int?
)

data class FileAnalysisState(
    val file: FileEntry,
    val loading: Boolean = true,
    val sha256: String? = null,
    val apkInfo: ApkPackageInfo? = null,
    val error: String? = null
)

enum class BrowserSort {
    NAME,
    SIZE,
    DATE
}

data class BrowserNode(
    val uri: String,
    val name: String
)

data class BrowserState(
    val rootUri: String? = null,
    val rootName: String = "",
    val stack: List<BrowserNode> = emptyList(),
    val items: List<FileEntry> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null
) {
    val currentUri: String?
        get() = stack.lastOrNull()?.uri ?: rootUri

    val currentName: String
        get() = stack.lastOrNull()?.name ?: rootName

    val canGoUp: Boolean
        get() = stack.isNotEmpty()

    val breadcrumb: String
        get() = buildString {
            append(rootName.ifBlank { "Storage" })
            stack.forEach {
                append(" / ")
                append(it.name)
            }
        }
}

data class PendingSystemDelete(
    val files: List<FileEntry>,
    val systemRequest: Boolean
)

sealed interface CopyOperation {
    data class Pending(val source: FileEntry, val move: Boolean) : CopyOperation
}
