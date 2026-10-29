package com.spacepilot.free

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.spacepilot.free.ui.SpacePilotApp
import com.spacepilot.free.ui.theme.SpacePilotTheme
import com.spacepilot.free.util.LocaleUtils
import com.spacepilot.free.viewmodel.AppViewModel
import com.spacepilot.free.widget.SpacePilotWidget
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel: AppViewModel by viewModels()
    private var scanRequestId by mutableIntStateOf(0)

    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(LocaleUtils.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        handleIntent(intent)
        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            val requestId = scanRequestId
            SpacePilotTheme(themeMode = settings.themeMode) {
                SpacePilotApp(
                    viewModel = viewModel,
                    scanRequestId = requestId,
                    onScanRequestConsumed = { scanRequestId = 0 }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshStorage()
        lifecycleScope.launch {
            runCatching { SpacePilotWidget().updateAll(this@MainActivity) }
        }
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == ACTION_SCAN) {
            scanRequestId++
            // Сбрасываем action, чтобы повторное создание Activity не запускало скан ещё раз.
            intent.action = null
        }
    }

    companion object {
        const val ACTION_SCAN = "com.spacepilot.free.action.SCAN"
    }
}
