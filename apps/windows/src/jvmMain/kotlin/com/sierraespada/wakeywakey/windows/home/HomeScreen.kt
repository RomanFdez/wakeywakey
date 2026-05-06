package com.sierraespada.wakeywakey.windows.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sierraespada.wakeywakey.model.CalendarEvent
import com.sierraespada.wakeywakey.windows.calendar.CalendarAccountManager
import com.sierraespada.wakeywakey.windows.settings.DesktopSettingsRepository
import java.awt.Desktop
import java.net.URI
import java.text.SimpleDateFormat
import java.util.*

// ─── Brand colours ────────────────────────────────────────────────────────────
private val Yellow      = Color(0xFFFFE03A)
private val Navy        = Color(0xFF1A1A2E)
private val NavySurface = Color(0xFF16213E)
private val Coral       = Color(0xFFFF6B6B)
private val Green       = Color(0xFF4CAF50)

// ─── Screen ───────────────────────────────────────────────────────────────────

@Composable
fun HomeScreen(
    vm:                HomeViewModel,
    onOpenSettings:    () -> Unit = {},
    onConnectCalendar: () -> Unit = {},
) {
    val state by vm.uiState.collectAsState()
    var selectedEvent by remember { mutableStateOf<CalendarEvent?>(null) }
    val settings by DesktopSettingsRepository.settings.collectAsState()

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(Navy),
    ) {
        // ── Left panel — event list (38% width) ──────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .weight(0.38f),
        ) {
            HomeHeader(
                isPaused       = settings.isPaused,
                onOpenSettings = onOpenSettings,
                onRefresh      = { vm.refresh() },
            )

            when {
                state.error != null ->
                    ErrorState(state.error!!) { vm.refresh() }
                state.events.isEmpty() && !state.isLoading ->
                    if (CalendarAccountManager.isConnected)
                        NoMeetingsTodayState()
                    else
                        EmptyState(onConnectCalendar = onConnectCalendar)
                else ->
                    EventList(
                        state      = state,
                        onEventTap = { selectedEvent = it },
                        selected   = selectedEvent,
                    )
            }

            if (state.isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color    = Yellow,
                    trackColor = Navy,
                )
            }
        }

        // Divider
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(1.dp)
                .background(Color.White.copy(alpha = 0.07f))
        )

        // ── Right panel — detail (62% width) ─────────────────────────────────
        Box(
            modifier         = Modifier
                .fillMaxHeight()
                .weight(0.62f)
                .background(NavySurface),
            contentAlignment = Alignment.Center,
        ) {
            val shown = selectedEvent ?: state.nextEvent
            if (shown != null) {
                DetailPanel(
                    event     = shown,
                    nowMillis = state.nowMillis,
                )
            } else if (!state.isLoading) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("🎉", fontSize = 52.sp)
                    Text(
                        "No meetings today",
                        fontSize   = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color      = Color.White.copy(alpha = 0.35f),
                        textAlign  = TextAlign.Center,
                    )
                    Text(
                        "Enjoy the day.",
                        fontSize = 14.sp,
                        color    = Color.White.copy(alpha = 0.2f),
                    )
                }
            }
        }
    }
}

// ─── Header ───────────────────────────────────────────────────────────────────

@Composable
private fun HomeHeader(
    isPaused:       Boolean,
    onOpenSettings: () -> Unit,
    onRefresh:      () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Navy)
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text       = "WakeyWakey",
                    fontSize   = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color      = Yellow,
                )
                Text(
                    text     = todayLabel(),
                    fontSize = 12.sp,
                    color    = Color.White.copy(alpha = 0.4f),
                )
            }

            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (isPaused) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Coral.copy(alpha = 0.2f),
                    ) {
                        Text(
                            "⏸ Paused",
                            fontSize = 11.sp,
                            color    = Coral,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                }
                HeaderIconButton("↻", "Refresh",  onClick = onRefresh)
                HeaderIconButton("⚙", "Settings", onClick = onOpenSettings)
            }
        }
    }
}

@Composable
private fun HeaderIconButton(icon: String, label: String, onClick: () -> Unit) {
    Surface(
        shape    = RoundedCornerShape(8.dp),
        color    = Color.White.copy(alpha = 0.06f),
        modifier = Modifier
            .size(34.dp)
            .clickable { onClick() },
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(icon, fontSize = 16.sp, color = Color.White.copy(alpha = 0.7f))
        }
    }
}

// ─── Lists ────────────────────────────────────────────────────────────────────

@Composable
private fun EventList(
    state:      HomeUiState,
    onEventTap: (CalendarEvent) -> Unit,
    selected:   CalendarEvent?,
) {
    LazyColumn(
        modifier       = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        state.nextEvent?.let { next ->
            item(key = "next_${next.id}") {
                NextMeetingCard(
                    event     = next,
                    nowMillis = state.nowMillis,
                    isSelected = selected?.id == next.id,
                    onTap     = { onEventTap(next) },
                )
            }
        }

        if (state.laterEvents.isNotEmpty()) {
            item {
                Text(
                    text     = "Later today",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color    = Color.White.copy(alpha = 0.35f),
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                )
            }
            items(state.laterEvents, key = { it.id }) { event ->
                EventRow(
                    event      = event,
                    nowMillis  = state.nowMillis,
                    isSelected = selected?.id == event.id,
                    onTap      = { onEventTap(event) },
                )
            }
        }
    }
}

// ─── Next meeting card ────────────────────────────────────────────────────────

@Composable
private fun NextMeetingCard(
    event:      CalendarEvent,
    nowMillis:  Long,
    isSelected: Boolean,
    onTap:      () -> Unit,
) {
    val minutesLeft    = ((event.startTime - nowMillis) / 60_000L).toInt()
    val isStartingSoon = minutesLeft in 0..5
    val isOngoing      = minutesLeft < 0

    val scale by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue  = 1f,
        targetValue   = if (isStartingSoon) 1.012f else 1f,
        animationSpec = infiniteRepeatable(
            animation  = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "cardScale",
    )

    val accentColor = when {
        isStartingSoon -> Coral
        isOngoing      -> Green
        else           -> Yellow
    }
    val cardColor = when {
        isSelected     -> accentColor.copy(alpha = 0.18f)
        isStartingSoon -> Coral.copy(alpha = 0.10f)
        isOngoing      -> Green.copy(alpha = 0.08f)
        else           -> NavySurface
    }

    Surface(
        shape    = RoundedCornerShape(14.dp),
        color    = cardColor,
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable { onTap() },
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Badge
            Surface(shape = RoundedCornerShape(5.dp), color = accentColor.copy(alpha = 0.16f)) {
                Text(
                    text = when {
                        isOngoing      -> "● Ongoing"
                        isStartingSoon -> "● Starting in ${minutesLeft}m"
                        else           -> "Next meeting"
                    },
                    fontSize   = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color      = accentColor,
                    modifier   = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                )
            }
            Text(
                text       = event.title,
                fontSize   = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color      = Color.White,
                maxLines   = 2,
                overflow   = TextOverflow.Ellipsis,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                TimeChip(formatTime(event.startTime), accentColor)
                TimeChip(durationLabel(event.startTime, event.endTime), Color.White.copy(alpha = 0.4f))
            }
            Text(
                text       = countdownLabel(event.startTime, nowMillis),
                fontSize   = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color      = accentColor,
            )
        }
    }
}

// ─── Later-today event row ────────────────────────────────────────────────────

@Composable
private fun EventRow(
    event:      CalendarEvent,
    nowMillis:  Long,
    isSelected: Boolean,
    onTap:      () -> Unit,
) {
    val isOngoing  = event.startTime <= nowMillis && event.endTime > nowMillis
    val rowColor   = when {
        isSelected -> Yellow.copy(alpha = 0.12f)
        isOngoing  -> Green.copy(alpha = 0.07f)
        else       -> Color.White.copy(alpha = 0.04f)
    }

    Surface(
        shape    = RoundedCornerShape(10.dp),
        color    = rowColor,
        modifier = Modifier.fillMaxWidth().clickable { onTap() },
    ) {
        Row(
            modifier              = Modifier.padding(12.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(48.dp)) {
                if (isOngoing) {
                    Text("● On", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Green, softWrap = false)
                } else {
                    val minsLeft = ((event.startTime - nowMillis) / 60_000).toInt()
                    if (minsLeft in 1..30) {
                        Text("in ${minsLeft}m", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Yellow, softWrap = false)
                    } else {
                        Text(formatTime(event.startTime), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Yellow, softWrap = false)
                    }
                }
                Text(durationLabel(event.startTime, event.endTime), fontSize = 9.sp, color = Color.White.copy(alpha = 0.3f))
            }

            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(32.dp)
                    .background(Yellow.copy(alpha = 0.25f), RoundedCornerShape(1.dp))
            )

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    event.title,
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = Color.White,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis,
                )
                Text(
                    event.calendarName,
                    fontSize = 10.sp,
                    color    = Color.White.copy(alpha = 0.3f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (event.meetingLink != null) {
                Surface(
                    shape    = RoundedCornerShape(6.dp),
                    color    = Yellow.copy(alpha = 0.12f),
                    modifier = Modifier.clickable {
                        runCatching { Desktop.getDesktop().browse(URI(event.meetingLink)) }
                    },
                ) {
                    Text(
                        "Join",
                        fontSize   = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color      = Yellow,
                        modifier   = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    )
                }
            }
        }
    }
}

// ─── Detail panel (right side) ────────────────────────────────────────────────

@Composable
private fun DetailPanel(event: CalendarEvent, nowMillis: Long) {
    val minutesLeft = ((event.startTime - nowMillis) / 60_000L).toInt()
    val isOngoing   = minutesLeft < 0
    val accentColor = when {
        isOngoing          -> Green
        minutesLeft <= 5   -> Coral
        else               -> Yellow
    }
    val dateFmt = remember { SimpleDateFormat("EEE, MMM d · HH:mm", Locale.ENGLISH) }
    val timeFmt = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 40.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        // Calendar badge
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = accentColor.copy(alpha = 0.12f),
        ) {
            Text(
                text     = event.calendarName,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color    = accentColor,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }

        // Title
        Text(
            text       = event.title,
            fontSize   = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            color      = Color.White,
            lineHeight = 34.sp,
        )

        // Countdown
        Text(
            text       = countdownLabel(event.startTime, nowMillis),
            fontSize   = 48.sp,
            fontWeight = FontWeight.ExtraBold,
            color      = accentColor,
        )

        HorizontalDivider(color = Color.White.copy(alpha = 0.07f))

        // Time
        DetailRow("🕐", buildString {
            append(dateFmt.format(Date(event.startTime)))
            if (event.endTime > event.startTime) {
                append(" – ")
                append(timeFmt.format(Date(event.endTime)))
            }
        })

        // Location
        if (!event.location.isNullOrBlank() && event.meetingLink == null) {
            DetailRow("📍", event.location!!)
        }

        // Meeting link
        if (event.meetingLink != null) {
            DetailRow("🔗", event.meetingLink!!)
        }

        // Notes
        if (!event.description.isNullOrBlank()) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                DetailSectionLabel("Notes")
                Text(
                    text       = event.description!!,
                    fontSize   = 14.sp,
                    color      = Color.White.copy(alpha = 0.65f),
                    lineHeight = 21.sp,
                )
            }
        }

        // Join button
        if (event.meetingLink != null) {
            Spacer(Modifier.height(4.dp))
            Button(
                onClick  = {
                    runCatching { Desktop.getDesktop().browse(URI(event.meetingLink)) }
                },
                modifier = Modifier
                    .widthIn(max = 320.dp)
                    .height(52.dp),
                shape  = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentColor,
                    contentColor   = Navy,
                ),
            ) {
                Text("Join now →", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
            }
        }
    }
}

// ─── Empty / Error states ─────────────────────────────────────────────────────

@Composable
private fun NoMeetingsTodayState() {
    Column(
        modifier            = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("🎉", fontSize = 48.sp)
        Spacer(Modifier.height(16.dp))
        Text(
            "No meetings today",
            fontSize   = 18.sp,
            fontWeight = FontWeight.Bold,
            color      = Color.White,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Your calendar is connected.\nEnjoy the free time!",
            fontSize  = 14.sp,
            color     = Color.White.copy(alpha = 0.5f),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun EmptyState(onConnectCalendar: () -> Unit) {
    Column(
        modifier            = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("📅", fontSize = 48.sp)
        Spacer(Modifier.height(16.dp))
        Text(
            "No calendar connected",
            fontSize   = 18.sp,
            fontWeight = FontWeight.Bold,
            color      = Color.White,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Connect Google Calendar or Outlook\nto see your meetings here.",
            fontSize  = 14.sp,
            color     = Color.White.copy(alpha = 0.5f),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onConnectCalendar,
            shape   = RoundedCornerShape(12.dp),
            colors  = ButtonDefaults.buttonColors(containerColor = Yellow, contentColor = Navy),
        ) {
            Text("Connect calendar", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(
        modifier            = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("⚠️", fontSize = 40.sp)
        Spacer(Modifier.height(12.dp))
        Text(message, color = Color.White.copy(alpha = 0.7f), textAlign = TextAlign.Center, fontSize = 14.sp)
        Spacer(Modifier.height(20.dp))
        OutlinedButton(
            onClick = onRetry,
            colors  = ButtonDefaults.outlinedButtonColors(contentColor = Yellow),
        ) { Text("Retry") }
    }
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

@Composable
private fun TimeChip(label: String, color: Color) {
    Surface(shape = RoundedCornerShape(5.dp), color = color.copy(alpha = 0.10f)) {
        Text(
            text       = label,
            fontSize   = 11.sp,
            color      = color,
            fontWeight = FontWeight.SemiBold,
            modifier   = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun DetailRow(emoji: String, text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
        Text(emoji, fontSize = 15.sp)
        Text(text, fontSize = 14.sp, color = Color.White.copy(alpha = 0.7f), modifier = Modifier.weight(1f), lineHeight = 20.sp)
    }
}

@Composable
private fun DetailSectionLabel(text: String) {
    Text(
        text.uppercase(),
        fontSize      = 10.sp,
        fontWeight    = FontWeight.Bold,
        color         = Yellow.copy(alpha = 0.55f),
        letterSpacing = 1.sp,
    )
}

// ─── Formatters ───────────────────────────────────────────────────────────────

private val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())
private val dayFmt  = SimpleDateFormat("EEEE, MMMM d", Locale.ENGLISH)

private fun formatTime(millis: Long): String = timeFmt.format(Date(millis))
private fun todayLabel(): String = dayFmt.format(Date()).replaceFirstChar { it.uppercase() }

private fun durationLabel(start: Long, end: Long): String {
    val mins = ((end - start) / 60_000L).toInt()
    return if (mins >= 60) "${mins / 60}h${if (mins % 60 != 0) "${mins % 60}m" else ""}"
           else "${mins}m"
}

private fun countdownLabel(startTime: Long, nowMillis: Long): String {
    val diff = startTime - nowMillis
    return when {
        diff < -60_000L -> {
            val mins = ((-diff) / 60_000L).toInt()
            "Ongoing · ${mins}m"
        }
        diff < 0        -> "Starting now"
        else            -> {
            val totalSecs = diff / 1_000L
            val h = totalSecs / 3600
            val m = (totalSecs % 3600) / 60
            val s = totalSecs % 60
            if (h > 0) "%dh %02dm".format(h, m) else "%02d:%02d".format(m, s)
        }
    }
}
