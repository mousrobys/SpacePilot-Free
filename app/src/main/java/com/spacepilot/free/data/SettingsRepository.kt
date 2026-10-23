package com.spacepilot.free.data

import android.content.Context
import com.spacepilot.free.data.model.AppSettings
import com.spacepilot.free.data.model.LanguageMode
import com.spacepilot.free.data.model.OperationRecord
import com.spacepilot.free.data.model.OperationType
import com.spacepilot.free.data.model.ThemeMode
import com.spacepilot.free.util.LocaleUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

class SettingsRepository(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(LocaleUtils.PREFS_NAME, Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private val _folderUri = MutableStateFlow(prefs.getString(KEY_FOLDER_URI, null))
    val folderUri: StateFlow<String?> = _folderUri.asStateFlow()

    private val _operations = MutableStateFlow(loadOperations())
    val operations: StateFlow<List<OperationRecord>> = _operations.asStateFlow()

    fun setTheme(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME, mode.name).apply()
        _settings.value = _settings.value.copy(themeMode = mode)
    }

    fun setLanguage(mode: LanguageMode) {
        prefs.edit().putString(LocaleUtils.KEY_LANGUAGE, mode.name).apply()
        _settings.value = _settings.value.copy(languageMode = mode)
    }

    fun resetSettings() {
        prefs.edit()
            .remove(KEY_THEME)
            .remove(LocaleUtils.KEY_LANGUAGE)
            .apply()
        _settings.value = AppSettings()
    }

    fun setFolderUri(uri: String?) {
        if (uri == null) {
            prefs.edit().remove(KEY_FOLDER_URI).apply()
        } else {
            prefs.edit().putString(KEY_FOLDER_URI, uri).apply()
        }
        _folderUri.value = uri
    }

    fun addOperation(record: OperationRecord) {
        val updated = (listOf(record) + _operations.value).take(MAX_OPERATIONS)
        _operations.value = updated
        saveOperations(updated)
    }

    fun clearOperations() {
        _operations.value = emptyList()
        prefs.edit().remove(KEY_OPERATIONS).apply()
    }

    fun clearAllLocalHistory() {
        clearOperations()
        appContext.deleteFile(SCAN_CACHE_FILE)
    }

    private fun loadSettings(): AppSettings {
        val theme = runCatching {
            ThemeMode.valueOf(prefs.getString(KEY_THEME, ThemeMode.DARK.name) ?: ThemeMode.DARK.name)
        }.getOrDefault(ThemeMode.DARK)
        val language = LocaleUtils.languageModeFromString(
            prefs.getString(LocaleUtils.KEY_LANGUAGE, LanguageMode.SYSTEM.name)
        )
        return AppSettings(themeMode = theme, languageMode = language)
    }

    private fun loadOperations(): List<OperationRecord> {
        val raw = prefs.getString(KEY_OPERATIONS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    val type = runCatching { OperationType.valueOf(obj.optString("type")) }
                        .getOrDefault(OperationType.SCAN)
                    add(
                        OperationRecord(
                            timestamp = obj.optLong("timestamp"),
                            type = type,
                            description = obj.optString("description"),
                            bytes = obj.optLong("bytes")
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun saveOperations(records: List<OperationRecord>) {
        val array = JSONArray()
        records.forEach {
            array.put(
                JSONObject()
                    .put("timestamp", it.timestamp)
                    .put("type", it.type.name)
                    .put("description", it.description)
                    .put("bytes", it.bytes)
            )
        }
        prefs.edit().putString(KEY_OPERATIONS, array.toString()).apply()
    }

    companion object {
        const val KEY_THEME = "theme_mode"
        const val KEY_FOLDER_URI = "folder_uri"
        private const val KEY_OPERATIONS = "recent_operations"
        private const val MAX_OPERATIONS = 25
        const val SCAN_CACHE_FILE = "last_scan_cache.json.gz"
    }
}
