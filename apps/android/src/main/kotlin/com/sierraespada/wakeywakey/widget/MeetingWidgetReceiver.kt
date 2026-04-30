package com.sierraespada.wakeywakey.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.updateAll

class MeetingWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MeetingWidget()

    companion object {
        /** Llama esto desde WorkManager / SchedulerService para refrescar el widget. */
        suspend fun update(context: Context) {
            MeetingWidget().updateAll(context)
        }
    }
}
