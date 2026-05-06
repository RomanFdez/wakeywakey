package com.sierraespada.wakeywakey.windows.tray

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sierraespada.wakeywakey.model.CalendarEvent
import com.sierraespada.wakeywakey.windows.calendar.CustomEventsRepository
import java.text.SimpleDateFormat
import java.util.*

private val Yellow   = Color(0xFFFFE03A)
private val Navy     = Color(0xFF1A1A2E)
private val NavySurf = Color(0xFF16213E)
private val Coral    = Color(0xFFFF6B6B)
private val White    = Color.White

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEventDialog(onDismiss: () -> Unit) {
    var title    by remember { mutableStateOf("") }
    var dateText by remember { mutableStateOf(todayString()) }
    var timeText by remember { mutableStateOf(nextHourString()) }
    var error    by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = NavySurf,
        shape            = RoundedCornerShape(16.dp),
        title = {
            Text("Add event", color = Yellow, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Title
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Title", color = White.copy(alpha = 0.45f), fontSize = 11.sp)
                    OutlinedTextField(
                        value         = title,
                        onValueChange = { title = it },
                        placeholder   = { Text("Event name", color = White.copy(alpha = 0.3f)) },
                        singleLine    = true,
                        colors        = fieldColors(),
                        modifier      = Modifier.fillMaxWidth(),
                    )
                }

                // Date + Time
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Date (DD/MM/YYYY)", color = White.copy(alpha = 0.45f), fontSize = 11.sp)
                        OutlinedTextField(
                            value         = dateText,
                            onValueChange = { dateText = it.take(10) },
                            singleLine    = true,
                            colors        = fieldColors(),
                            modifier      = Modifier.fillMaxWidth(),
                        )
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Time (HH:MM)", color = White.copy(alpha = 0.45f), fontSize = 11.sp)
                        OutlinedTextField(
                            value         = timeText,
                            onValueChange = { timeText = it.take(5) },
                            singleLine    = true,
                            colors        = fieldColors(),
                            modifier      = Modifier.fillMaxWidth(),
                        )
                    }
                }

                error?.let { Text(it, color = Coral, fontSize = 12.sp) }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val event = buildEvent(title, dateText, timeText)
                    if (event == null) {
                        error = "Check title, date (DD/MM/YYYY) and time (HH:MM)"
                    } else {
                        CustomEventsRepository.add(event)
                        onDismiss()
                    }
                },
                colors  = ButtonDefaults.buttonColors(containerColor = Yellow, contentColor = Navy),
                shape   = RoundedCornerShape(10.dp),
            ) { Text("Save", fontWeight = FontWeight.ExtraBold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = White.copy(alpha = 0.45f))
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor   = Yellow,
    unfocusedBorderColor = White.copy(alpha = 0.18f),
    focusedTextColor     = White,
    unfocusedTextColor   = White,
    cursorColor          = Yellow,
)

private fun buildEvent(title: String, dateText: String, timeText: String): CalendarEvent? {
    if (title.isBlank()) return null
    val fmt  = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    val date = runCatching { fmt.parse("$dateText $timeText") }.getOrNull() ?: return null
    val startMs = date.time
    val endMs   = startMs + 60 * 60_000L  // 1 hora de duración por defecto
    return CalendarEvent(
        id           = CustomEventsRepository.newId(),
        title        = title.trim(),
        startTime    = startMs,
        endTime      = endMs,
        meetingLink  = null,
        location     = null,
        description  = null,
        calendarId   = CustomEventsRepository.CUSTOM_CALENDAR_ID,
        calendarName = CustomEventsRepository.CUSTOM_CALENDAR_NAME,
        isAllDay     = false,
    )
}

private fun todayString(): String =
    SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())

private fun nextHourString(): String {
    val cal = Calendar.getInstance().apply { add(Calendar.HOUR_OF_DAY, 1); set(Calendar.MINUTE, 0) }
    return SimpleDateFormat("HH:mm", Locale.getDefault()).format(cal.time)
}
