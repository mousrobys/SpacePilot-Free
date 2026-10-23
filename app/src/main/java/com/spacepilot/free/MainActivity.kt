package com.spacepilot.free

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spacepilot.free.data.model.ThemeMode
import com.spacepilot.free.ui.SpacePilotApp
import com.spacepilot.free.ui.theme.SpacePilotTheme
import com.spacepilot.free.util.LocaleUtils
import com.spacepilot.free.viewmodel.AppViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: AppViewModel by viewModels()

    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(LocaleUtils.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            SpacePilotTheme(themeMode = settings.themeMode) {
                SpacePilotApp(viewModel = viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshStorage()
    }

}
