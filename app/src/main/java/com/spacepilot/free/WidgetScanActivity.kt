package com.spacepilot.free

import android.app.Activity
import android.content.Intent
import android.os.Bundle

/**
 * Прозрачная активность-мост для виджета: виджет может стартовать только компонент,
 * поэтому он открывает этот экран, а тот сразу передаёт ACTION_SCAN в MainActivity.
 */
class WidgetScanActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(
            Intent(this, MainActivity::class.java).apply {
                action = MainActivity.ACTION_SCAN
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
        )
        finish()
    }
}
