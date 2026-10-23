package com.spacepilot.free.util

import android.content.Context
import android.content.res.Configuration
import com.spacepilot.free.data.model.LanguageMode
import java.util.Locale

object LocaleUtils {
    const val PREFS_NAME = "spacepilot_settings"
    const val KEY_LANGUAGE = "language_mode"
    private val supported = setOf("ru", "en")

    fun resolveLanguage(mode: LanguageMode): Locale = when (mode) {
        LanguageMode.RU -> Locale("ru")
        LanguageMode.EN -> Locale("en")
        LanguageMode.SYSTEM -> {
            val system = Locale.getDefault().language
            if (system in supported) Locale(system) else Locale("ru")
        }
    }

    fun languageModeFromString(value: String?): LanguageMode =
        runCatching { LanguageMode.valueOf(value ?: "") }.getOrDefault(LanguageMode.SYSTEM)

    fun wrap(context: Context): Context {
        val mode = languageModeFromString(
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_LANGUAGE, LanguageMode.SYSTEM.name)
        )
        val locale = resolveLanguage(mode)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        return context.createConfigurationContext(config)
    }
}
