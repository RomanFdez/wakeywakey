package com.sierraespada.wakeywakey.widget

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.glance.*
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.*
import androidx.glance.text.*
import androidx.glance.unit.ColorProvider
import com.sierraespada.wakeywakey.MainActivity
import com.sierraespada.wakeywakey.calendar.AndroidCalendarRepository
import com.sierraespada.wakeywakey.model.CalendarEvent
import com.sierraespada.wakeywakey.settings.SettingsRepository
import com.sierraespada.wakeywakey.util.applySettings
import kotlinx.coroutines.flow.firstOrNull
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MeetingWidget : GlanceAppWidget() {

    // Tres tamaños: fila compacta (2x1), fila normal (3x1), expandido (3x2)
    override val sizeMode = SizeMode.Responsive(
        setOf(COMPACT, STANDARD, EXPANDED)
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val nextEvent = loadNextEvent(context)
        provideContent {
            val size = LocalSize.current
            when {
                size.height >= EXPANDED.height -> ExpandedContent(event = nextEvent)
                else                           -> CompactContent(event = nextEvent)
            }
        }
    }

    private suspend fun loadNextEvent(context: Context): CalendarEvent? {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR)
            != PackageManager.PERMISSION_GRANTED) return null
        return try {
            val settings = SettingsRepository.getInstance(context).settings.firstOrNull()
            val repo     = AndroidCalendarRepository(context)
            val now      = System.currentTimeMillis()
            repo.getUpcomingEvents(
                fromTime      = now,
                toTime        = now + 24 * 60 * 60_000L,
                includeAllDay = settings?.showAllDayEvents ?: false,
            ).let { if (settings != null) it.applySettings(settings) else it }
                .firstOrNull { it.endTime > now }
        } catch (e: Exception) { null }
    }

    companion object {
        val COMPACT  = DpSize(140.dp,  50.dp)  // 2x1
        val STANDARD = DpSize(280.dp,  50.dp)  // 4x1
        val EXPANDED = DpSize(210.dp, 130.dp)  // 3x2 (130dp = umbral seguro para 2 filas reales)

        // WakeyWakey palette
        val Yellow  = Color(0xFFFFE03A)
        val Navy    = Color(0xFF1A1A2E)
        val White   = Color.White
        val Green   = Color(0xFF4CAF50)
    }
}

// ─── Compact (1 fila): ⏰  en 5m  Weekly Sync  [Unirse] ─────────────────────

@Composable
private fun CompactContent(event: CalendarEvent?) {
    Row(
        modifier            = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(MeetingWidget.Navy))
            .cornerRadius(14.dp)
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clickable(actionStartActivity<MainActivity>()),
        verticalAlignment   = Alignment.CenterVertically,
    ) {
        if (event == null) {
            Text(
                text  = "⏰  Sin reuniones hoy",
                style = TextStyle(
                    color    = ColorProvider(MeetingWidget.White.copy(alpha = 0.5f)),
                    fontSize = 12.sp,
                ),
            )
        } else {
            val timeLabel = eventTimeLabel(event)
            // Time badge
            Text(
                text  = "⏰  $timeLabel  ",
                style = TextStyle(
                    color      = ColorProvider(if (isOngoing(event)) MeetingWidget.Green else MeetingWidget.Yellow),
                    fontSize   = 12.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
            // Title (takes remaining space, shrinks font before truncating)
            Text(
                text     = event.title,
                style    = TextStyle(
                    color    = ColorProvider(MeetingWidget.White),
                    fontSize = titleFontSize(event.title, compact = true),
                ),
                maxLines = 1,
                modifier = GlanceModifier.defaultWeight(),
            )
            // Join button
            event.meetingLink?.let { url ->
                Spacer(GlanceModifier.width(6.dp))
                JoinButton(url = url)
            }
        }
    }
}

// ─── Expanded (2 filas): tiempo + título en fila 1, botón Unirse en fila 2 ──

@Composable
private fun ExpandedContent(event: CalendarEvent?) {
    Column(
        modifier          = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(MeetingWidget.Navy))
            .cornerRadius(14.dp)
            .padding(12.dp)
            .clickable(actionStartActivity<MainActivity>()),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (event == null) {
            Text(
                text  = "⏰  WakeyWakey",
                style = TextStyle(
                    color      = ColorProvider(MeetingWidget.Yellow),
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
            Spacer(GlanceModifier.height(4.dp))
            Text(
                text  = "No hay reuniones próximas",
                style = TextStyle(
                    color    = ColorProvider(MeetingWidget.White.copy(alpha = 0.45f)),
                    fontSize = 12.sp,
                ),
            )
        } else {
            val timeLabel = eventTimeLabel(event)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text  = "⏰  ",
                    style = TextStyle(fontSize = 12.sp),
                )
                Text(
                    text  = timeLabel,
                    style = TextStyle(
                        color      = ColorProvider(if (isOngoing(event)) MeetingWidget.Green else MeetingWidget.Yellow),
                        fontSize   = 12.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }
            Spacer(GlanceModifier.height(3.dp))
            Text(
                text     = event.title,
                style    = TextStyle(
                    color      = ColorProvider(MeetingWidget.White),
                    fontSize   = titleFontSize(event.title, compact = false),
                    fontWeight = FontWeight.Bold,
                ),
                maxLines = 2,
            )
            event.meetingLink?.let { url ->
                Spacer(GlanceModifier.height(6.dp))
                JoinButton(url = url)
            }
        }
    }
}

// ─── Shared helpers ───────────────────────────────────────────────────────────

@Composable
private fun JoinButton(url: String) {
    Box(
        modifier = GlanceModifier
            .background(ColorProvider(MeetingWidget.Yellow))
            .cornerRadius(8.dp)
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .clickable(
                actionStartActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            ),
    ) {
        Text(
            text  = "Unirse",
            style = TextStyle(
                color      = ColorProvider(MeetingWidget.Navy),
                fontSize   = 12.sp,
                fontWeight = FontWeight.Bold,
            ),
        )
    }
}

/**
 * Devuelve el tamaño de fuente apropiado para el título según su longitud.
 * [compact] = fila única (máx 13sp → mín 11sp)
 * [expanded] = dos filas (máx 15sp → mín 11sp)
 * Por debajo de 11sp el texto ya no es legible en un widget; a partir de ahí
 * maxLines + ellipsis acortan el texto.
 */
private fun titleFontSize(title: String, compact: Boolean): TextUnit {
    val len = title.length
    return if (compact) {
        when {
            len <= 22 -> 13.sp
            len <= 32 -> 12.sp
            else      -> 11.sp
        }
    } else {
        when {
            len <= 18 -> 15.sp
            len <= 28 -> 14.sp
            len <= 40 -> 12.sp
            else      -> 11.sp
        }
    }
}

private fun isOngoing(event: CalendarEvent): Boolean {
    val now = System.currentTimeMillis()
    return event.startTime <= now && event.endTime > now
}

private fun eventTimeLabel(event: CalendarEvent): String {
    val now         = System.currentTimeMillis()
    val minutesLeft = ((event.startTime - now) / 60_000L).toInt()
    return when {
        isOngoing(event)  -> "En curso"
        minutesLeft <= 0  -> "Ahora"
        minutesLeft < 60  -> "en ${minutesLeft}m"
        else              -> SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(event.startTime))
    }
}
