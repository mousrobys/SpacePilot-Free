package com.spacepilot.free.viewmodel

import android.app.Application
import android.content.IntentSender
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.spacepilot.free.R
import com.spacepilot.free.data.CachedScan
import com.spacepilot.free.data.FileAnalysisRepository
import com.spacepilot.free.data.FileOperationsRepository
import com.spacepilot.free.data.HashRepository
import com.spacepilot.free.data.MediaScanResult
import com.spacepilot.free.data.MediaStoreRepository
import com.spacepilot.free.data.SafRepository
import com.spacepilot.free.data.SafScanResult
import com.spacepilot.free.data.ScanCacheRepository
import com.spacepilot.free.data.SettingsRepository
import com.spacepilot.free.data.StorageRepository
import com.spacepilot.free.data.model.AppSettings
import com.spacepilot.free.data.model.BrowserNode
import com.spacepilot.free.data.model.BrowserState
import com.spacepilot.free.data.model.CategoryStat
import com.spacepilot.free.data.model.CopyOperation
import com.spacepilot.free.data.model.DuplicateGroup
import com.spacepilot.free.data.model.DuplicateUiState
import com.spacepilot.free.data.model.FileAnalysisState
import com.spacepilot.free.data.model.FileCategory
import com.spacepilot.free.data.model.FileEntry
import com.spacepilot.free.data.model.LanguageMode
import com.spacepilot.free.data.model.OperationRecord
import com.spacepilot.free.data.model.OperationType
import com.spacepilot.free.data.model.PendingSystemDelete
import com.spacepilot.free.data.model.ScanPhase
import com.spacepilot.free.data.model.ScanSummary
import com.spacepilot.free.data.model.ScanUiState
import com.spacepilot.free.data.model.StorageInfo
import com.spacepilot.free.data.model.ThemeMode
import com.spacepilot.free.util.LocaleUtils
import com.spacepilot.free.util.PermissionUtils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

sealed interface UiEvent {
    data class Deleted(val count: Int, val failed: Int) : UiEvent
    data class DeletePending(val count: Int) : UiEvent
    data object DeleteCancelled : UiEvent
    data class Copied(val name: String, val moved: Boolean) : UiEvent
    data class CopyFailed(val reason: String?) : UiEvent
    data object HistoryCleared : UiEvent
    data class Message(val text: String) : UiEvent
}

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application.applicationContext
    private val settingsRepository = SettingsRepository(app)
    private val storageRepository = StorageRepository(app)
    private val mediaStoreRepository = MediaStoreRepository(app)
    private val safRepository = SafRepository(app)
    private val hashRepository = HashRepository(app)
    private val analysisRepository = FileAnalysisRepository(app)
    private val operationsRepository = FileOperationsRepository(app)
    private val cacheRepository = ScanCacheRepository(app)

    val settings: StateFlow<AppSettings> = settingsRepository.settings
    val folderUri: StateFlow<String?> = settingsRepository.folderUri
    val recentOperations: StateFlow<List<OperationRecord>> = settingsRepository.operations

    private val _storageInfo = MutableStateFlow(StorageInfo())
    val storageInfo: StateFlow<StorageInfo> = _storageInfo.asStateFlow()

    private val _scanState = MutableStateFlow<ScanUiState>(ScanUiState.Idle)
    val scanState: StateFlow<ScanUiState> = _scanState.asStateFlow()

    private val _summary = MutableStateFlow<ScanSummary?>(null)
    val summary: StateFlow<ScanSummary?> = _summary.asStateFlow()

    private val _allFiles = MutableStateFlow<List<FileEntry>>(emptyList())
    val allFiles: StateFlow<List<FileEntry>> = _allFiles.asStateFlow()

    private val _duplicates = MutableStateFlow<DuplicateUiState>(DuplicateUiState.NotAnalyzed)
    val duplicates: StateFlow<DuplicateUiState> = _duplicates.asStateFlow()

    private val _browser = MutableStateFlow(BrowserState())
    val browser: StateFlow<BrowserState> = _browser.asStateFlow()

    private val _folderAccessError = MutableStateFlow<String?>(null)
    val folderAccessError: StateFlow<String?> = _folderAccessError.asStateFlow()

    private val _analysis = MutableStateFlow<FileAnalysisState?>(null)
    val analysis: StateFlow<FileAnalysisState?> = _analysis.asStateFlow()

    private val _copyPending = MutableStateFlow<CopyOperation?>(null)
    val copyPending: StateFlow<CopyOperation?> = _copyPending.asStateFlow()

    private val _events = Channel<UiEvent>(Channel.BUFFERED)
    val events: Flow<UiEvent> = _events.receiveAsFlow()

    private val _deleteRequests = Channel<IntentSender>(Channel.BUFFERED)
    val deleteRequests: Flow<IntentSender> = _deleteRequests.receiveAsFlow()

    private var scanJob: Job? = null
    private var duplicateJob: Job? = null
    private var analysisJob: Job? = null
    private var pendingSystemDelete: PendingSystemDelete? = null
    private var cacheLoaded = false

    init {
        refreshStorage()
        viewModelScope.launch {
            val cached = cacheRepository.load()
            if (cached != null && scanJob == null && _summary.value == null) {
                applyCachedScan(cached)
            }
            cacheLoaded = true
        }
    }

    fun refreshStorage() {
        viewModelScope.launch { _storageInfo.value = storageRepository.readStorageInfo() }
    }

    fun startScan() {
        if (scanJob?.isActive == true) return
        scanJob = viewModelScope.launch {
            _scanState.value = ScanUiState.Running(ScanPhase.PREPARING)
            _duplicates.value = DuplicateUiState.NotAnalyzed
            try {
                val hasMediaAccess = PermissionUtils.hasMediaReadAccess(app)
                var lastReport = 0L
                suspend fun report(
                    phase: ScanPhase,
                    files: Int,
                    bytes: Long,
                    fraction: Float?,
                    location: String
                ) {
                    val now = System.currentTimeMillis()
                    if (now - lastReport > 90L || fraction == 1f || phase != ScanPhase.MEDIA_LIBRARY) {
                        lastReport = now
                        _scanState.value = ScanUiState.Running(
                            phase = phase,
                            scannedFiles = files,
                            scannedBytes = bytes,
                            progress = fraction,
                            currentLocation = location
                        )
                    }
                }

                val mediaResult = if (hasMediaAccess) {
                    _scanState.value = ScanUiState.Running(ScanPhase.MEDIA_LIBRARY)
                    try {
                        mediaStoreRepository.scan { files, bytes, fraction, current ->
                            report(ScanPhase.MEDIA_LIBRARY, files, bytes, fraction, current)
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        MediaScanResult(emptyList(), listOf("media"), listOf(e.localizedMessage.orEmpty()))
                    }
                } else {
                    MediaScanResult(emptyList(), listOf("media"))
                }

                val folderString = folderUri.value
                val safResult = if (folderString != null) {
                    _scanState.value = ScanUiState.Running(
                        ScanPhase.FOLDER,
                        mediaResult.files.size,
                        mediaResult.files.sumOf { it.size }
                    )
                    try {
                        safRepository.scanTree(Uri.parse(folderString)) { files, bytes, current ->
                            report(
                                ScanPhase.FOLDER,
                                mediaResult.files.size + files,
                                mediaResult.files.sumOf { it.size } + bytes,
                                null,
                                current
                            )
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        SafScanResult(emptyList(), listOf(e.localizedMessage.orEmpty()), "")
                    }
                } else {
                    null
                }

                _scanState.value = ScanUiState.Running(
                    ScanPhase.FINISHING,
                    mediaResult.files.size + (safResult?.files?.size ?: 0),
                    mediaResult.files.sumOf { it.size } + (safResult?.files?.sumOf { it.size } ?: 0L)
                )

                val merged = mergeSources(mediaResult.files, safResult?.files.orEmpty())
                val categories = merged.groupBy { it.category }.mapValues { (_, entries) ->
                    CategoryStat(entries.size, entries.sumOf { it.size })
                }
                val summary = ScanSummary(
                    timestamp = System.currentTimeMillis(),
                    scannedFiles = merged.size,
                    totalBytes = merged.sumOf { it.size },
                    categories = categories,
                    largeFiles = merged.filter { it.size >= ScanCacheRepository.LARGE_FILE_THRESHOLD }
                        .sortedByDescending { it.size },
                    duplicateGroups = emptyList(),
                    inaccessible = (safResult?.inaccessible.orEmpty() + mediaResult.folderNames)
                        .filter { it.isNotBlank() }
                        .distinct(),
                    duplicatesAnalyzed = false,
                    usedFolderAccess = folderString != null,
                    limitedAccess = !PermissionUtils.hasAllScanPermissions(app) ||
                        folderString == null ||
                        mediaResult.deniedCollections.isNotEmpty() ||
                        !hasMediaAccess
                )
                _allFiles.value = merged
                _summary.value = summary
                _duplicates.value = DuplicateUiState.NotAnalyzed
                settingsRepository.addOperation(
                    OperationRecord(
                        timestamp = summary.timestamp,
                        type = OperationType.SCAN,
                        description = localizedString(R.string.operation_scan, merged.size),
                        bytes = summary.totalBytes
                    )
                )
                cacheRepository.save(summary, merged)
                _scanState.value = ScanUiState.Completed(summary)
                refreshStorage()
            } catch (e: CancellationException) {
                _scanState.value = ScanUiState.Cancelled
                throw e
            } catch (e: Exception) {
                _scanState.value = ScanUiState.Failed(
                    message = e.localizedMessage ?: localizedString(R.string.scan_failed_generic),
                    details = e::class.java.simpleName
                )
            }
        }
    }

    fun cancelScan() {
        scanJob?.cancel()
        scanJob = null
    }

    fun analyzeDuplicates() {
        if (duplicateJob?.isActive == true) return
        val files = _allFiles.value
        if (files.size < 2) {
            _duplicates.value = DuplicateUiState.NoCandidates
            return
        }
        duplicateJob = viewModelScope.launch {
            _duplicates.value = DuplicateUiState.Running(0, 0, null)
            try {
                val result = hashRepository.findDuplicates(files) { hashed, total ->
                    _duplicates.value = DuplicateUiState.Running(
                        hashedFiles = hashed,
                        totalCandidates = total,
                        progress = if (total > 0) hashed.toFloat() / total else null
                    )
                }
                val current = _summary.value
                if (current != null) {
                    val updated = current.copy(
                        duplicateGroups = result.groups,
                        duplicatesAnalyzed = true
                    )
                    _summary.value = updated
                    cacheRepository.save(updated, _allFiles.value)
                }
                _duplicates.value = DuplicateUiState.Completed(result.groups)
            } catch (e: CancellationException) {
                _duplicates.value = DuplicateUiState.NotAnalyzed
                throw e
            } catch (e: Exception) {
                _duplicates.value = DuplicateUiState.Failed(
                    e.localizedMessage ?: localizedString(R.string.duplicates_failed)
                )
            } finally {
                duplicateJob = null
            }
        }
    }

    fun cancelDuplicateAnalysis() {
        duplicateJob?.cancel()
        duplicateJob = null
        _duplicates.value = DuplicateUiState.NotAnalyzed
    }

    suspend fun setFolderAccess(uri: Uri) {
        // Сначала забираем persistable-доступ, потом любые обращения к провайдеру:
        // на Android 11+ доступ к дереву документов нельзя использовать до фиксации разрешения.
        val flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
            android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        val persisted = runCatching {
            app.contentResolver.takePersistableUriPermission(uri, flags)
            true
        }.getOrElse {
            runCatching {
                app.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                true
            }.getOrDefault(false)
        }
        _folderAccessError.value = if (persisted) null else app.getString(R.string.msg_folder_access_denied)

        settingsRepository.setFolderUri(uri.toString())
        val name = safRepository.rootName(uri).ifBlank { uri.lastPathSegment ?: "Storage" }
        _browser.value = BrowserState(rootUri = uri.toString(), rootName = name)
        loadBrowserRoot()
    }

    fun consumeFolderAccessError() {
        _folderAccessError.value = null
    }

    fun clearFolderAccess() {
        folderUri.value?.let { value ->
            runCatching {
                app.contentResolver.releasePersistableUriPermission(
                    Uri.parse(value),
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            }
        }
        settingsRepository.setFolderUri(null)
        _browser.value = BrowserState()
    }

    fun loadBrowserRoot() {
        val uriString = folderUri.value ?: return
        val treeUri = Uri.parse(uriString)
        if (_browser.value.rootUri != uriString) {
            _browser.value = BrowserState(
                rootUri = uriString,
                rootName = safRepository.rootName(treeUri)
            )
        }
        val current = _browser.value
        loadBrowserDirectory(current.currentUri?.let(Uri::parse), current)
    }

    fun openBrowserDirectory(entry: FileEntry) {
        if (!entry.isDirectory) return
        val current = _browser.value
        val node = BrowserNode(entry.uri, entry.name)
        val updated = current.copy(stack = current.stack + node)
        _browser.value = updated
        loadBrowserDirectory(Uri.parse(entry.uri), updated)
    }

    fun browserGoUp() {
        val current = _browser.value
        if (current.stack.isEmpty()) return
        val updated = current.copy(stack = current.stack.dropLast(1))
        _browser.value = updated
        loadBrowserDirectory(updated.currentUri?.let(Uri::parse), updated)
    }

    fun refreshBrowser() {
        val current = _browser.value
        val root = current.rootUri ?: return
        loadBrowserDirectory(
            current.currentUri?.let(Uri::parse),
            if (current.rootUri == null) current.copy(rootUri = root) else current
        )
    }

    private fun loadBrowserDirectory(uri: Uri?, state: BrowserState) {
        val treeUriString = state.rootUri ?: folderUri.value ?: return
        val treeUri = Uri.parse(treeUriString)
        _browser.value = state.copy(
            rootUri = treeUriString,
            loading = true,
            error = null
        )
        viewModelScope.launch {
            val result = safRepository.loadChildren(treeUri, uri)
            _browser.value = _browser.value.copy(
                rootUri = treeUriString,
                rootName = result.rootName.ifBlank { _browser.value.rootName },
                loading = false,
                items = result.items,
                error = result.error
            )
            if (result.error != null && !hasPersistedAccess(treeUri)) {
                _folderAccessError.value = app.getString(R.string.msg_folder_access_lost)
            }
        }
    }

    private fun hasPersistedAccess(treeUri: Uri): Boolean = runCatching {
        app.contentResolver.persistedUriPermissions.any { it.uri == treeUri && it.isReadPermission }
    }.getOrDefault(false)

    fun requestCopy(file: FileEntry, move: Boolean) {
        if (file.isDirectory) return
        _copyPending.value = CopyOperation.Pending(file, move)
    }

    fun onCopyDestinationSelected(destination: Uri) {
        val pending = _copyPending.value as? CopyOperation.Pending ?: return
        _copyPending.value = null
        viewModelScope.launch {
            val result = safRepository.copyTo(pending.source, destination, pending.move)
            if (result.success) {
                if (pending.move) {
                    removedEntries(listOf(pending.source))
                }
                settingsRepository.addOperation(
                    OperationRecord(
                        timestamp = System.currentTimeMillis(),
                        type = if (pending.move) OperationType.MOVE else OperationType.COPY,
                        description = localizedString(
                            if (pending.move) R.string.operation_move else R.string.operation_copy,
                            pending.source.name
                        ),
                        bytes = pending.source.size
                    )
                )
                if (pending.move) refreshBrowser()
                _events.send(UiEvent.Copied(pending.source.name, pending.move))
                if (result.message != null) _events.send(UiEvent.Message(result.message))
            } else {
                _events.send(UiEvent.CopyFailed(result.message))
            }
        }
    }

    fun cancelCopy() {
        _copyPending.value = null
    }

    fun deleteFiles(files: List<FileEntry>) {
        if (files.isEmpty()) return
        viewModelScope.launch {
            val safFiles = files.filter { operationsRepository.isSaf(it) }
            val mediaFiles = files.filterNot { operationsRepository.isSaf(it) }
            val deleted = mutableListOf<FileEntry>()
            val failed = mutableListOf<FileEntry>()

            if (safFiles.isNotEmpty()) {
                val (ok, bad) = operationsRepository.deleteSafFiles(safFiles)
                deleted += ok
                failed += bad
            }

            if (mediaFiles.isNotEmpty()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val sender = operationsRepository.createSystemDeleteRequest(mediaFiles)
                    if (sender != null) {
                        pendingSystemDelete = PendingSystemDelete(mediaFiles, true)
                        if (deleted.isNotEmpty()) {
                            removedEntries(deleted)
                        }
                        _events.send(UiEvent.DeletePending(mediaFiles.size))
                        _deleteRequests.send(sender)
                    } else {
                        failed += mediaFiles
                    }
                } else {
                    val result = operationsRepository.deleteMediaDirect(mediaFiles)
                    deleted += result.deleted
                    failed += result.failed
                    if (result.systemIntentSender != null && result.systemFiles.isNotEmpty()) {
                        pendingSystemDelete = PendingSystemDelete(result.systemFiles, true)
                        _deleteRequests.send(result.systemIntentSender)
                    }
                }
            }

            if (deleted.isNotEmpty()) {
                removedEntries(deleted)
                _events.send(UiEvent.Deleted(deleted.size, failed.size))
            } else if (failed.isNotEmpty() && pendingSystemDelete == null) {
                _events.send(UiEvent.Deleted(0, failed.size))
            }
        }
    }

    fun onSystemDeleteResult(success: Boolean) {
        val pending = pendingSystemDelete
        pendingSystemDelete = null
        if (success && pending != null) {
            removedEntries(pending.files)
            _events.trySend(UiEvent.Deleted(pending.files.size, 0))
        } else {
            _events.trySend(UiEvent.DeleteCancelled)
        }
    }

    private fun removedEntries(entries: List<FileEntry>) {
        val removedUris = entries.map { it.uri }.toSet()
        if (removedUris.isEmpty()) return
        val remaining = _allFiles.value.filterNot { it.uri in removedUris }
        _allFiles.value = remaining
        val current = _summary.value
        if (current != null) {
            val categories = remaining.groupBy { it.category }.mapValues { (_, values) ->
                CategoryStat(values.size, values.sumOf { it.size })
            }
            val duplicateGroups = current.duplicateGroups.mapNotNull { group ->
                val files = group.files.filterNot { it.uri in removedUris }
                if (files.size > 1) group.copy(files = files) else null
            }
            val updated = current.copy(
                scannedFiles = remaining.size,
                totalBytes = remaining.sumOf { it.size },
                categories = categories,
                largeFiles = remaining.filter { it.size >= ScanCacheRepository.LARGE_FILE_THRESHOLD }
                    .sortedByDescending { it.size },
                duplicateGroups = duplicateGroups
            )
            _summary.value = updated
            val duplicateState = _duplicates.value
            if (duplicateState is DuplicateUiState.Completed) {
                _duplicates.value = DuplicateUiState.Completed(duplicateGroups)
            }
            viewModelScope.launch { cacheRepository.save(updated, remaining) }
        }
        val deletedBytes = entries.sumOf { it.size }
        settingsRepository.addOperation(
            OperationRecord(
                timestamp = System.currentTimeMillis(),
                type = OperationType.DELETE,
                description = localizedString(R.string.operation_delete, entries.size),
                bytes = deletedBytes
            )
        )
        if (_browser.value.items.any { it.uri in removedUris }) {
            refreshBrowser()
        }
    }

    fun analyzeFile(file: FileEntry) {
        analysisJob?.cancel()
        _analysis.value = FileAnalysisState(file = file, loading = true)
        analysisJob = viewModelScope.launch {
            try {
                val checksum = analysisRepository.sha256(file)
                val apkInfo = if (file.category == FileCategory.APK) {
                    analysisRepository.inspectApk(file)
                } else {
                    null
                }
                _analysis.value = FileAnalysisState(
                    file = file,
                    loading = false,
                    sha256 = checksum,
                    apkInfo = apkInfo
                )
                settingsRepository.addOperation(
                    OperationRecord(
                        timestamp = System.currentTimeMillis(),
                        type = OperationType.CHECKSUM,
                        description = localizedString(R.string.operation_checksum, file.name),
                        bytes = file.size
                    )
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _analysis.value = FileAnalysisState(
                    file = file,
                    loading = false,
                    error = e.localizedMessage ?: localizedString(R.string.analysis_failed)
                )
            }
        }
    }

    fun dismissAnalysis() {
        analysisJob?.cancel()
        analysisJob = null
        _analysis.value = null
    }

    fun recordApkInstall(file: FileEntry) {
        settingsRepository.addOperation(
            OperationRecord(
                timestamp = System.currentTimeMillis(),
                type = OperationType.APK_INSTALL,
                description = localizedString(R.string.operation_apk_install, file.name),
                bytes = file.size
            )
        )
    }

    fun setTheme(mode: ThemeMode) = settingsRepository.setTheme(mode)

    fun setLanguage(mode: LanguageMode) = settingsRepository.setLanguage(mode)

    fun resetSettings() {
        settingsRepository.resetSettings()
    }

    fun clearHistory() {
        settingsRepository.clearAllLocalHistory()
        cacheRepository.delete()
        scanJob?.cancel()
        duplicateJob?.cancel()
        _summary.value = null
        _allFiles.value = emptyList()
        _duplicates.value = DuplicateUiState.NotAnalyzed
        _scanState.value = ScanUiState.Idle
        _analysis.value = null
        viewModelScope.launch { _events.send(UiEvent.HistoryCleared) }
    }

    override fun onCleared() {
        scanJob?.cancel()
        duplicateJob?.cancel()
        analysisJob?.cancel()
        super.onCleared()
    }

    private fun applyCachedScan(cached: CachedScan) {
        _summary.value = cached.summary
        _allFiles.value = cached.files
        _duplicates.value = if (cached.summary.duplicatesAnalyzed) {
            DuplicateUiState.Completed(cached.summary.duplicateGroups)
        } else {
            DuplicateUiState.NotAnalyzed
        }
    }

    private fun mergeSources(mediaFiles: List<FileEntry>, safFiles: List<FileEntry>): List<FileEntry> {
        val merged = LinkedHashMap<String, FileEntry>()
        mediaFiles.forEach { merged[it.uri] = it }
        safFiles.forEach { safFile ->
            val samePhysicalFile = mediaFiles.any { media ->
                media.name == safFile.name &&
                    media.size == safFile.size &&
                    media.modified > 0L &&
                    safFile.modified > 0L &&
                    kotlin.math.abs(media.modified - safFile.modified) < 2_000L
            }
            if (!samePhysicalFile) merged[safFile.uri] = safFile
        }
        return merged.values.toList()
    }

    private fun localizedString(@StringRes id: Int, vararg args: Any): String {
        val locale = LocaleUtils.resolveLanguage(settingsRepository.settings.value.languageMode)
        if (locale.language == Locale.getDefault().language && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return app.getString(id, *args)
        }
        val config = Configuration(app.resources.configuration).apply {
            setLocale(locale)
            setLayoutDirection(locale)
        }
        val localized = app.createConfigurationContext(config)
        return localized.getString(id, *args)
    }
}
