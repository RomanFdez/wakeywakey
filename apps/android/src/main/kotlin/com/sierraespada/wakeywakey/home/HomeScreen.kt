package com.sierraespada.wakeywakey.home

import android.content.Intent
import android.net.Uri
import com.sierraespada.wakeywakey.alert.AlertActivity
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sierraespada.wakeywakey.R
import com.sierraespada.wakeywakey.billing.EntitlementManager
import com.sierraespada.wakeywakey.model.CalendarEvent
import com.sierraespada.wakeywakey.scheduler.AndroidAlarmScheduler
import java.text.SimpleDateFormat
import java.util.*

// ─── Brand colours ────────────────────────────────────────────────────────────
private val Yellow      = Color(0xFFFFE03A)
private val Navy        = Color(0xFF1A1A2E)
private val NavySurface = Color(0xFF16213E)
private val Coral       = Color(0xFFFF6B6B)
private val Green       = Color(0xFF4CAF50)

// ─── Screen ───────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    vm: HomeViewModel = viewModel(),
    onOpenSettings: () -> Unit = {},
    onResetOnboarding: () -> Unit = {},
    onShowPaywall: () -> Unit = {},
    isTablet: Boolean = false,
) {
    val state by vm.uiState.collectAsState()
    var showAddAlert by remember { mutableStateOf(false) }
    var detailEvent  by remember { mutableStateOf<com.sierraespada.wakeywakey.model.CalendarEvent?>(null) }

    if (isTablet) {
        // ── Tablet: two-panel layout ─────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(Navy)
                .systemBarsPadding(),
        ) {
            // Left panel — event list (40% width)
            PullToRefreshBox(
                isRefreshing = state.isLoading,
                onRefresh    = { vm.refresh() },
                modifier     = Modifier
                    .fillMaxHeight()
                    .weight(0.4f),
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp)) {
                        HomeHeader(
                            onOpenSettings    = onOpenSettings,
                            onAddAlert        = { showAddAlert = true },
                            onResetOnboarding = onResetOnboarding,
                            onShowPaywall     = onShowPaywall,
                        )
                    }
                    when {
                        state.error != null -> ErrorState(state.error!!) { vm.refresh() }
                        state.events.isEmpty() && !state.isLoading -> EmptyState()
                        else -> EventList(
                            state      = state,
                            onEventTap = { detailEvent = it },
                        )
                    }
                }
            }

            // Divider
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(1.dp)
                    .background(Color.White.copy(alpha = 0.07f))
            )

            // Right panel — event detail (60% width)
            Box(
                modifier         = Modifier
                    .fillMaxHeight()
                    .weight(0.6f)
                    .background(Color(0xFF16213E)),
                contentAlignment = Alignment.Center,
            ) {
                val selected = detailEvent ?: state.nextEvent
                if (selected != null) {
                    TabletDetailPanel(
                        event     = selected,
                        nowMillis = state.nowMillis,
                        onDelete  = if (selected.calendarId == com.sierraespada.wakeywakey.manualert.ManualAlert.MANUAL_CALENDAR_ID)
                                        vm::removeManualAlert else null,
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text("🎉", fontSize = 48.sp)
                        Text(
                            "Sin reuniones hoy",
                            fontSize   = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color      = Color.White.copy(alpha = 0.4f),
                        )
                    }
                }
            }
        }
    } else {
        // ── Phone: single panel layout ───────────────────────────────────────
        PullToRefreshBox(
            isRefreshing = state.isLoading,
            onRefresh    = { vm.refresh() },
            modifier     = Modifier
                .fillMaxSize()
                .background(Navy)
                .systemBarsPadding(),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp)) {
                    HomeHeader(
                        onOpenSettings    = onOpenSettings,
                        onAddAlert        = { showAddAlert = true },
                        onResetOnboarding = onResetOnboarding,
                        onShowPaywall     = onShowPaywall,
                    )
                }
                when {
                    state.error != null                        -> ErrorState(state.error!!) { vm.refresh() }
                    state.events.isEmpty() && !state.isLoading -> EmptyState()
                    else                                       -> EventList(
                        state       = state,
                        onEventTap  = { detailEvent = it },
                    )
                }
            }
        }

        detailEvent?.let { event ->
            EventDetailSheet(
                event     = event,
                onDismiss = { detailEvent = null },
                onDelete  = if (event.calendarId == com.sierraespada.wakeywakey.manualert.ManualAlert.MANUAL_CALENDAR_ID)
                                vm::removeManualAlert else null,
            )
        }
    }

    if (showAddAlert) {
        AddAlertSheet(
            onDismiss = { showAddAlert = false },
            onConfirm = { title, millis, notes ->
                vm.addManualAlert(title, millis, notes)
                showAddAlert = false
            },
        )
    }
}

// ─── States ───────────────────────────────────────────────────────────────────

@Composable
private fun LoadingState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = Yellow)
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(
        modifier            = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("⚠️", fontSize = 48.sp)
        Spacer(Modifier.height(16.dp))
        Text(message, color = Color.White.copy(alpha = 0.7f), textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        OutlinedButton(
            onClick = onRetry,
            colors  = ButtonDefaults.outlinedButtonColors(contentColor = Yellow),
        ) { Text("Retry") }
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier            = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("🎉", fontSize = 56.sp)
        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(R.string.home_no_events),
            fontSize   = 22.sp,
            fontWeight = FontWeight.Bold,
            color      = Color.White,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Enjoy the day.",
            fontSize = 15.sp,
            color    = Color.White.copy(alpha = 0.5f),
        )
    }
}

@Composable
private fun EventList(state: HomeUiState, onEventTap: (CalendarEvent) -> Unit = {}) {
    LazyColumn(
        modifier            = Modifier.fillMaxSize(),
        contentPadding      = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {

        state.nextEvent?.let { next ->
            item(key = "next_${next.id}") {
                NextMeetingCard(event = next, nowMillis = state.nowMillis, onTap = { onEventTap(next) })
                Spacer(Modifier.height(4.dp))
            }
        }

        if (state.laterEvents.isNotEmpty()) {
            item {
                Text(
                    text       = stringResource(R.string.home_later_today),
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = Color.White.copy(alpha = 0.4f),
                    modifier   = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                )
            }
            items(state.laterEvents, key = { it.id }) { event ->
                EventRow(event = event, nowMillis = state.nowMillis, onTap = { onEventTap(event) })
            }
        }
    }
}

// ─── Debug helpers ────────────────────────────────────────────────────────────

/** Small emoji button used only for debug/test shortcuts. */
@Composable
private fun DebugIconButton(emoji: String, tooltip: String, onClick: () -> Unit) {
    TextButton(
        onClick        = onClick,
        colors         = ButtonDefaults.textButtonColors(contentColor = Yellow.copy(alpha = 0.55f)),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
    ) {
        Text(emoji, fontSize = 20.sp)
    }
}

/** Chip pequeño para simular estados del trial en debug. */
@Composable
private fun DebugTrialChip(
    label:     String,
    chipColor: Color = Color.White.copy(alpha = 0.08f),
    onClick:   () -> Unit,
) {
    Surface(
        shape    = RoundedCornerShape(4.dp),
        color    = chipColor,
        modifier = Modifier.clickable { onClick() },
    ) {
        Text(
            text     = label,
            fontSize = 9.sp,
            color    = Color.White.copy(alpha = 0.6f),
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 3.dp),
        )
    }
}

// ─── Header ───────────────────────────────────────────────────────────────────

@Composable
private fun HomeHeader(
    onOpenSettings: () -> Unit = {},
    onAddAlert: () -> Unit = {},
    onResetOnboarding: () -> Unit = {},
    onShowPaywall: () -> Unit = {},
) {
    val context       = LocalContext.current
    val trialDaysLeft by EntitlementManager.trialDaysLeft.collectAsState()
    val isPro         by EntitlementManager.isPro.collectAsState()
    Column(modifier = Modifier.fillMaxWidth()) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                text       = "WakeyWakey",
                fontSize   = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color      = Yellow,
            )
            Text(
                text     = todayLabel(),
                fontSize = 13.sp,
                color    = Color.White.copy(alpha = 0.45f),
            )
        }

        // Permanent right-side actions
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(0.dp)) {
            TextButton(
                onClick        = onAddAlert,
                colors         = ButtonDefaults.textButtonColors(contentColor = Yellow),
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
            ) {
                Text("+", fontSize = 28.sp, fontWeight = FontWeight.Bold)
            }
            TextButton(
                onClick        = onOpenSettings,
                colors         = ButtonDefaults.textButtonColors(contentColor = Color.White.copy(alpha = 0.6f)),
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
            ) {
                Text("⚙️", fontSize = 20.sp)
            }
        }
    }

    // ── DEBUG buttons — TODO: remove before release ───────────────────────────
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        // 👁  Preview: opens AlertActivity immediately (UI check, no alarm)
        DebugIconButton("👁", "Alert preview") {
            context.startActivity(
                Intent(context, AlertActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    putExtra(AlertActivity.EXTRA_EVENT_ID,    999_998L)
                    putExtra(AlertActivity.EXTRA_TITLE,       "Preview Meeting 👁")
                    putExtra(AlertActivity.EXTRA_START,       System.currentTimeMillis() + 2 * 60_000L)
                    putExtra(AlertActivity.EXTRA_LOCATION,    "Room 42")
                    putExtra(AlertActivity.EXTRA_MEETING_URL, "https://meet.google.com/test")
                }
            )
        }
        // ⚡  Full-stack: schedules alarm to fire in 5 s (meeting in ~65 s)
        DebugIconButton("⚡", "Alarm in 5 s") {
            val now       = System.currentTimeMillis()
            val startTime = now + 65_000L
            val fakeEvent = CalendarEvent(
                id           = 999_999L,
                title        = "Test Meeting ⚡",
                startTime    = startTime,
                endTime      = startTime + 30 * 60_000L,
                location     = null,
                description  = null,
                calendarId   = 0L,
                calendarName = "Debug",
                meetingLink  = "https://meet.google.com/test",
                isAllDay     = false,
            )
            AndroidAlarmScheduler(context).schedule(fakeEvent, minutesBefore = 1)
            Toast.makeText(context, "⚡ Alarm fires in ~5 s", Toast.LENGTH_SHORT).show()
        }
        // 🔄  Reset: clears onboarding flag to re-run the welcome flow
        DebugIconButton("🔄", "Reset onboarding") {
            onResetOnboarding()
            Toast.makeText(context, "🔄 Onboarding reset", Toast.LENGTH_SHORT).show()
        }
    }

    // ── DEBUG — Trial simulator ───────────────────────────────────────────────
    // Simula distintos estados del ciclo de vida del trial sin tocar DataStore.
    // isPro + trialDaysLeft se actualizan en tiempo real → los lock icons y el
    // paywall reaccionan inmediatamente. TODO: eliminar antes del release.
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.End),
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        // Etiqueta de estado actual
        val stateLabel = when {
            isPro && trialDaysLeft > 0 -> "Trial $trialDaysLeft d"
            isPro                      -> "Pro ✓"
            else                       -> "Expired"
        }
        Text(
            text     = stateLabel,
            fontSize = 9.sp,
            color    = if (isPro) Color(0xFF4CAF50).copy(alpha = 0.7f) else Coral.copy(alpha = 0.7f),
            modifier = Modifier.padding(end = 4.dp),
        )

        // D1 → 14 días restantes (día 1 de uso)
        DebugTrialChip("D1") {
            EntitlementManager.debugSetTrialDays(14)
            Toast.makeText(context, "Trial: día 1 — 14 días restantes", Toast.LENGTH_SHORT).show()
        }
        // D7 → 7 días restantes (mitad del trial)
        DebugTrialChip("D7") {
            EntitlementManager.debugSetTrialDays(7)
            Toast.makeText(context, "Trial: día 7 — 7 días restantes", Toast.LENGTH_SHORT).show()
        }
        // D13 → 1 día restante (último día de trial)
        DebugTrialChip("D13") {
            EntitlementManager.debugSetTrialDays(1)
            Toast.makeText(context, "Trial: día 13 — 1 día restante", Toast.LENGTH_SHORT).show()
        }
        // D14+ → 0 días (trial expirado, sin suscripción)
        DebugTrialChip("💀", chipColor = Coral.copy(alpha = 0.25f)) {
            EntitlementManager.debugSetTrialDays(0)
            Toast.makeText(context, "Trial expirado — isPro = false", Toast.LENGTH_SHORT).show()
        }
        // 💳 → abre el paywall directamente
        DebugTrialChip("💳", chipColor = Yellow.copy(alpha = 0.18f)) {
            onShowPaywall()
        }
    }
    // ── END DEBUG ─────────────────────────────────────────────────────────────
    } // end Column
}

// ─── Next meeting card ────────────────────────────────────────────────────────

@Composable
private fun NextMeetingCard(event: CalendarEvent, nowMillis: Long, onTap: () -> Unit = {}) {
    val context        = LocalContext.current
    val minutesLeft    = ((event.startTime - nowMillis) / 60_000L).toInt()
    val isStartingSoon = minutesLeft in 0..5
    val isOngoing      = minutesLeft < 0

    // Subtle scale pulse when ≤ 5 min remaining
    val scale by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue  = 1f,
        targetValue   = if (isStartingSoon) 1.015f else 1f,
        animationSpec = infiniteRepeatable(
            animation  = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "scale",
    )

    val cardColor = when {
        isStartingSoon -> Coral.copy(alpha = 0.15f)
        isOngoing      -> Green.copy(alpha = 0.10f)
        else           -> NavySurface
    }
    val accentColor = when {
        isStartingSoon -> Coral
        isOngoing      -> Green
        else           -> Yellow
    }

    Surface(
        shape    = RoundedCornerShape(20.dp),
        color    = cardColor,
        modifier = Modifier.fillMaxWidth().scale(scale).clickable { onTap() },
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

            // Status badge
            Surface(shape = RoundedCornerShape(6.dp), color = accentColor.copy(alpha = 0.18f)) {
                Text(
                    text = when {
                        isOngoing      -> stringResource(R.string.home_event_ongoing)
                        isStartingSoon -> "● Starting in ${minutesLeft} min"
                        else           -> "Next meeting"
                    },
                    fontSize   = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color      = accentColor,
                    modifier   = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                )
            }

            // Title
            Text(
                text       = event.title,
                fontSize   = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color      = Color.White,
                maxLines   = 2,
                overflow   = TextOverflow.Ellipsis,
            )

            // Time chips + calendar name
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                TimeChip(label = formatTime(event.startTime), color = accentColor)
                if (event.endTime > event.startTime) {
                    TimeChip(label = durationLabel(event.startTime, event.endTime), color = Color.White.copy(alpha = 0.4f))
                }
                Text(
                    text     = event.calendarName,
                    fontSize = 12.sp,
                    color    = Color.White.copy(alpha = 0.35f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }

            // Location (only when there's no video link)
            if (!event.location.isNullOrBlank() && event.meetingLink == null) {
                Text(
                    text     = "📍 ${event.location}",
                    fontSize = 12.sp,
                    color    = Color.White.copy(alpha = 0.5f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // Countdown — plain Text, no animation to avoid flicker
            Text(
                text       = countdownLabel(event.startTime, nowMillis),
                fontSize   = 36.sp,
                fontWeight = FontWeight.ExtraBold,
                color      = accentColor,
            )

            // Join button
            if (event.meetingLink != null) {
                Button(
                    onClick  = {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse(event.meetingLink))
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape    = RoundedCornerShape(12.dp),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor = accentColor,
                        contentColor   = Navy,
                    ),
                ) {
                    Text(stringResource(R.string.home_join_now), fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                }
            }
        }
    }
}

// ─── Later-today event row ────────────────────────────────────────────────────

@Composable
private fun EventRow(event: CalendarEvent, nowMillis: Long = System.currentTimeMillis(), onTap: () -> Unit = {}) {
    val context   = LocalContext.current
    val isOngoing = event.startTime <= nowMillis && event.endTime > nowMillis
    val rowColor  = if (isOngoing) Green.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.05f)

    Surface(
        shape    = RoundedCornerShape(14.dp),
        color    = rowColor,
        modifier = Modifier.fillMaxWidth().clickable { onTap() },
    ) {
        Row(
            modifier              = Modifier.padding(14.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(52.dp)) {
                if (isOngoing) {
                    Text(stringResource(R.string.home_event_ongoing), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Green, softWrap = false)
                } else {
                    val minutesLeft = ((event.startTime - nowMillis) / 60_000).toInt()
                    if (minutesLeft in 1..30) {
                        Text(stringResource(R.string.home_event_in_minutes, minutesLeft), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Yellow, softWrap = false)
                    } else {
                        Text(formatTime(event.startTime), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Yellow, softWrap = false)
                    }
                }
                Text(durationLabel(event.startTime, event.endTime), fontSize = 10.sp, color = Color.White.copy(alpha = 0.35f), softWrap = false)
            }

            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(36.dp)
                    .background(Yellow.copy(alpha = 0.3f), RoundedCornerShape(1.dp))
            )

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    event.title,
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = Color.White,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis,
                )
                Text(
                    event.calendarName,
                    fontSize = 11.sp,
                    color    = Color.White.copy(alpha = 0.35f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (event.meetingLink != null) {
                TextButton(
                    onClick = {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse(event.meetingLink))
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    },
                    colors         = ButtonDefaults.textButtonColors(contentColor = Yellow),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                ) {
                    Text(stringResource(R.string.notif_action_join), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

// ─── Chip ─────────────────────────────────────────────────────────────────────

@Composable
private fun TimeChip(label: String, color: Color) {
    Surface(shape = RoundedCornerShape(6.dp), color = color.copy(alpha = 0.12f)) {
        Text(
            text       = label,
            fontSize   = 12.sp,
            color      = color,
            fontWeight = FontWeight.SemiBold,
            modifier   = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
        )
    }
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
            if (h > 0) "%dh %02dm".format(h, m)
            else       "%02d:%02d".format(m, s)
        }
    }
}
