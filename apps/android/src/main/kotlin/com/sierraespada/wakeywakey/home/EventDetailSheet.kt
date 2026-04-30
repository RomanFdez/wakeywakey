package com.sierraespada.wakeywakey.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sierraespada.wakeywakey.calendar.AndroidCalendarRepository
import com.sierraespada.wakeywakey.manualert.ManualAlert
import com.sierraespada.wakeywakey.model.CalendarEvent
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

private val Yellow      = Color(0xFFFFE03A)
private val Navy        = Color(0xFF1A1A2E)
private val NavySurface = Color(0xFF16213E)
private val Coral       = Color(0xFFFF6B6B)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailSheet(
    event: CalendarEvent,
    onDismiss: () -> Unit,
    onDelete: ((Long) -> Unit)? = null,   // non-null only for manual alerts
) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()

    val isManual = event.calendarId == ManualAlert.MANUAL_CALENDAR_ID

    var attendees by remember { mutableStateOf<List<String>>(emptyList()) }
    var loading   by remember { mutableStateOf(!isManual) }

    // Load attendees for real calendar events
    LaunchedEffect(event.id) {
        if (!isManual) {
            scope.launch {
                attendees = runCatching {
                    AndroidCalendarRepository(context).getAttendees(event.id)
                }.getOrDefault(emptyList())
                loading = false
            }
        }
    }

    val dateFmt = remember { SimpleDateFormat("EEE, MMM d · HH:mm", Locale.ENGLISH) }
    val timeFmt = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor   = NavySurface,
        dragHandle       = { BottomSheetDefaults.DragHandle(color = Color.White.copy(alpha = 0.3f)) },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Calendar label
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(shape = CircleShape, color = if (isManual) Coral.copy(alpha = 0.2f) else Yellow.copy(alpha = 0.15f)) {
                    Text(
                        if (isManual) "Manual" else event.calendarName,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color    = if (isManual) Coral else Yellow,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }

            // Title
            Text(event.title, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)

            // Time
            DetailRow("🕐", buildString {
                append(dateFmt.format(Date(event.startTime)))
                if (event.endTime > event.startTime) {
                    append(" – ")
                    append(timeFmt.format(Date(event.endTime)))
                }
            })

            // Location
            val loc = event.location
            if (!loc.isNullOrBlank()) {
                DetailRow("📍", loc)
            }

            // Meeting link
            val link = event.meetingLink
            if (link != null) {
                DetailRow("🔗", link)
            }

            // Notes / description
            val desc = event.description
            if (!desc.isNullOrBlank()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SectionLabel("Notes")
                    Text(
                        desc,
                        fontSize  = 14.sp,
                        color     = Color.White.copy(alpha = 0.75f),
                        lineHeight = 20.sp,
                    )
                }
            }

            // Attendees (calendar events only)
            if (!isManual) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionLabel("Attendees")
                    when {
                        loading     -> CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color    = Yellow,
                            strokeWidth = 2.dp,
                        )
                        attendees.isEmpty() -> Text(
                            "No attendee info available.",
                            fontSize = 13.sp,
                            color    = Color.White.copy(alpha = 0.35f),
                        )
                        else -> attendees.forEach { name ->
                            Row(
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Surface(shape = CircleShape, color = Yellow.copy(alpha = 0.15f), modifier = Modifier.size(32.dp)) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            name.first().uppercaseChar().toString(),
                                            fontSize   = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color      = Yellow,
                                        )
                                    }
                                }
                                Text(name, fontSize = 14.sp, color = Color.White.copy(alpha = 0.85f))
                            }
                        }
                    }
                }
            }

            // Delete button (manual alerts only)
            if (isManual && onDelete != null) {
                Spacer(Modifier.height(4.dp))
                OutlinedButton(
                    onClick = { onDelete(event.id); onDismiss() },
                    modifier = Modifier.fillMaxWidth(),
                    shape    = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                    colors   = ButtonDefaults.outlinedButtonColors(contentColor = Coral),
                    border   = androidx.compose.foundation.BorderStroke(1.dp, Coral.copy(alpha = 0.5f)),
                ) {
                    Text("Delete alert", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun DetailRow(emoji: String, text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
        Text(emoji, fontSize = 16.sp)
        Text(text, fontSize = 14.sp, color = Color.White.copy(alpha = 0.75f), modifier = Modifier.weight(1f))
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Bold,
        color = Yellow.copy(alpha = 0.7f), letterSpacing = 1.sp)
}
