package com.spacepilot.free.widget

import android.content.ComponentName
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.spacepilot.free.R
import com.spacepilot.free.WidgetScanActivity
import com.spacepilot.free.data.StorageRepository
import com.spacepilot.free.data.model.StorageInfo
import com.spacepilot.free.util.Formatters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * Домашний виджет: свободное место и кнопка запуска сканирования.
 * Данные читаются из StatFs при каждом обновлении, сеть не используется.
 */
class SpacePilotWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val storage = withContext(Dispatchers.IO) {
            runCatching { StorageRepository(context).readStorageInfo() }
                .getOrDefault(StorageInfo())
        }
        provideContent {
            WidgetContent(storage = storage, context = context)
        }
    }
}

@Composable
private fun WidgetContent(storage: StorageInfo, context: Context) {
    val background = ColorProvider(Color(0xFF0B1E2A))
    val muted = ColorProvider(Color(0xFF9FB6C4))
    val accent = ColorProvider(Color(0xFF4FD1C5))
    val white = ColorProvider(Color.White)

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(background)
            .cornerRadius(20.dp)
            .padding(14.dp),
        verticalAlignment = Alignment.Top,
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = context.getString(R.string.widget_name),
            style = TextStyle(fontSize = 12.sp, color = muted, fontWeight = FontWeight.Medium)
        )
        Spacer(GlanceModifier.height(6.dp))
        Text(
            text = Formatters.bytes(storage.freeBytes, Locale.getDefault()),
            style = TextStyle(fontSize = 24.sp, color = white, fontWeight = FontWeight.Bold)
        )
        Text(
            text = context.getString(R.string.widget_free_label),
            style = TextStyle(fontSize = 12.sp, color = muted)
        )
        Spacer(GlanceModifier.height(4.dp))
        Text(
            text = context.getString(R.string.widget_used, storage.usedPercent),
            style = TextStyle(fontSize = 12.sp, color = muted)
        )
        Spacer(GlanceModifier.height(10.dp))
        Box(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(ColorProvider(Color(0xFF14313F)))
                .cornerRadius(12.dp)
                .clickable(
                    actionStartActivity(
                        ComponentName(context, WidgetScanActivity::class.java)
                    )
                )
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = context.getString(R.string.widget_scan),
                style = TextStyle(fontSize = 13.sp, color = accent, fontWeight = FontWeight.Medium)
            )
        }
    }
}
