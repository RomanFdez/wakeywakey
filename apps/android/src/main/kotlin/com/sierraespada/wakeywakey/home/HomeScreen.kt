package com.sierraespada.wakeywakey.home

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sierraespada.wakeywakey.model.CalendarEvent
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
fun HomeScreen(vm: HomeViewModel = viewModel()) {
    val state by vm.uiState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Navy)
            .systemBarsPadding(),
    ) {
        PullToRefreshBox(
            isRefreshing = state.isLoading,
            onRefresh    = { vm.refresh() },
            modifier     = Modifier.fillMaxSize(),
        ) {
            when {
                state.error != null    -> ErrorState(state.error!!) { vm.refresh() }
                state.events.isEmpty() && !state.isLoading -> EmptyState()
                else                   -> EventList(state = state)
            }
        }
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
        modifier            = Modifier.fillMaxSize().padding(32.dp),
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
        modifier            = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("🎉", fontSize = 56.sp)
        Spacer(Modifier.height(16.dp))
        Text(
            "No meetings today",
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
private fun EventList(state: HomeUiState) {
    LazyColumn(
        contentPadding      = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            HomeHeader()
            Spacer(Modifier.height(8.dp))
        }

        state.nextEvent?.let { next ->
            item(key = "next_${next.id}") {
                NextMeetingCard(event = next, nowMillis = state.nowMillis)
                Spacer(Modifier.height(4.dp))
            }
        }

        if (state.laterEvents.isNotEmpty()) {
            item {
                Text(
                    text       = "Later today",
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = Color.White.copy(alpha = 0.4f),
                    modifier   = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                )
            }
            items(state.laterEvents, key = { it.id }) { event ->
                EventRow(event = event)
            }
        }
    }
}

// ─── Header ───────────────────────────────────────────────────────────────────

@Composable
private fun HomeHeader() {
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
        Text("⏰", fontSize = 28.sp)
    }
}

// ─── Next meeting card ────────────────────────────────────────────────────────

@Composable
private fun NextMeetingCard(event: CalendarEvent, nowMillis: Long) {
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
        modifier = Modifier.fillMaxWidth().scale(scale),
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

            // Status badge
            Surface(shape = RoundedCornerShape(6.dp), color = accentColor.copy(alpha = 0.18f)) {
                Text(
                    text = when {
                        isOngoing      -> "● Ongoing"
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
                    Text("Join now →", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                }
            }
        }
    }
}

// ─── Later-today event row ────────────────────────────────────────────────────

@Composable
private fun EventRow(event: CalendarEvent) {
    val context = LocalContext.current

    Surface(
        shape    = RoundedCornerShape(14.dp),
        color    = Color.White.copy(alpha = 0.05f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier              = Modifier.padding(14.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(44.dp)) {
                Text(formatTime(event.startTime), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Yellow)
                Text(durationLabel(event.startTime, event.endTime), fontSize = 10.sp, color = Color.White.copy(alpha = 0.35f))
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
                    Text("Join", fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
