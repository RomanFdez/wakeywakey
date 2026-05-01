package com.sierraespada.wakeywakey.windows.settings

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sierraespada.wakeywakey.model.UserSettings
import com.sierraespada.wakeywakey.windows.calendar.CalendarAccountManager

// ─── Colores ──────────────────────────────────────────────────────────────────

private val Yellow    = Color(0xFFFFE03A)
private val Navy      = Color(0xFF1A1A2E)
private val Surface   = Color(0xFF16213E)
private val Surface2  = Color(0xFF0F3460)
private val White     = Color.White
private val Danger    = Color(0xFFFF6B6B)
private val Subtitle  = Color(0xFF8892AA)

/**
 * Pantalla de ajustes del escritorio.
 *
 * Secciones:
 *  1. Cuenta de calendario — email conectado, cambiar/desconectar
 *  2. Alertas              — minutos antes
 *  3. Filtros              — solo video, solo aceptados, eventos de todo el día
 *  4. Sonido               — activar/desactivar, repetir
 *  5. Horario laboral      — habilitar, hora inicio/fin, días de la semana
 *  6. App                  — autostart al iniciar sesión
 */
@Composable
fun DesktopSettingsScreen(
    onConnectCalendar: () -> Unit,
) {
    val s        by DesktopSettingsRepository.settings.collectAsState()
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Navy),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // ── Header ────────────────────────────────────────────────────────
            Text(
                "Settings",
                color      = Yellow,
                fontSize   = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier   = Modifier.padding(bottom = 4.dp),
            )

            // ── 1. Calendar account ───────────────────────────────────────────
            SettingsSection(title = "Calendar account", icon = Icons.Filled.CalendarMonth) {
                val email    by CalendarAccountManager.activeProvider.collectAsState()
                val provider by CalendarAccountManager.activeProvider.collectAsState()

                if (email != null) {
                    ConnectedAccountRow(
                        email    = CalendarAccountManager.connectedEmail ?: "",
                        provider = provider ?: "",
                        onDisconnect = {
                            CalendarAccountManager.disconnect()
                        },
                        onReconnect = onConnectCalendar,
                    )
                } else {
                    Row(
                        modifier            = Modifier.fillMaxWidth(),
                        verticalAlignment   = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            "No calendar connected",
                            color    = Subtitle,
                            fontSize = 14.sp,
                        )
                        Button(
                            onClick = onConnectCalendar,
                            colors  = ButtonDefaults.buttonColors(
                                containerColor = Yellow,
                                contentColor   = Navy,
                            ),
                            shape    = RoundedCornerShape(10.dp),
                            modifier = Modifier.height(36.dp),
                        ) {
                            Text("Connect", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                    }
                }
            }

            // ── 2. Alerts ─────────────────────────────────────────────────────
            SettingsSection(title = "Alert timing", icon = Icons.Filled.Alarm) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Alert me this many minutes before each meeting:",
                        color    = Subtitle,
                        fontSize = 13.sp,
                    )
                    AlertMinutesSelector(
                        current = s.alertMinutesBefore,
                        onChange = { mins ->
                            DesktopSettingsRepository.save(s.copy(alertMinutesBefore = mins))
                        },
                    )
                }
            }

            // ── 3. Filters ────────────────────────────────────────────────────
            SettingsSection(title = "Event filters", icon = Icons.Filled.FilterList) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    ToggleRow(
                        label    = "Video meetings only",
                        subtitle = "Skip events without a video link",
                        checked  = s.filterVideoOnly,
                        onToggle = { DesktopSettingsRepository.save(s.copy(filterVideoOnly = it)) },
                    )
                    HorizontalDivider(color = White.copy(alpha = 0.06f))
                    ToggleRow(
                        label    = "Accepted events only",
                        subtitle = "Skip events you declined or haven't responded to",
                        checked  = s.filterAcceptedOnly,
                        onToggle = { DesktopSettingsRepository.save(s.copy(filterAcceptedOnly = it)) },
                    )
                    HorizontalDivider(color = White.copy(alpha = 0.06f))
                    ToggleRow(
                        label    = "Include all-day events",
                        subtitle = "Show and alert for full-day events",
                        checked  = s.showAllDayEvents,
                        onToggle = { DesktopSettingsRepository.save(s.copy(showAllDayEvents = it)) },
                    )
                }
            }

            // ── 4. Sound ──────────────────────────────────────────────────────
            SettingsSection(title = "Sound", icon = Icons.Filled.VolumeUp) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    ToggleRow(
                        label    = "Sound alerts",
                        subtitle = "Play a sound when an alert fires",
                        checked  = s.soundEnabled,
                        onToggle = { DesktopSettingsRepository.save(s.copy(soundEnabled = it)) },
                    )
                    if (s.soundEnabled) {
                        HorizontalDivider(color = White.copy(alpha = 0.06f))
                        ToggleRow(
                            label    = "Repeat sound",
                            subtitle = "Loop until you dismiss or snooze",
                            checked  = s.repeatSound,
                            onToggle = { DesktopSettingsRepository.save(s.copy(repeatSound = it)) },
                        )
                    }
                }
            }

            // ── 5. Working hours ──────────────────────────────────────────────
            SettingsSection(title = "Working hours", icon = Icons.Filled.Schedule) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ToggleRow(
                        label    = "Working hours only",
                        subtitle = "Silence alerts outside your work schedule",
                        checked  = s.workHoursEnabled,
                        onToggle = { DesktopSettingsRepository.save(s.copy(workHoursEnabled = it)) },
                    )

                    if (s.workHoursEnabled) {
                        Spacer(Modifier.height(4.dp))
                        WorkHoursEditor(s = s)
                    }
                }
            }

            // ── 6. App ────────────────────────────────────────────────────────
            if (AutostartManager.isSupported) {
                SettingsSection(title = "Application", icon = Icons.Filled.Settings) {
                    var autostartOn by remember { mutableStateOf(AutostartManager.isEnabled) }

                    ToggleRow(
                        label    = "Launch at login",
                        subtitle = "Start WakeyWakey automatically when you log in",
                        checked  = autostartOn,
                        onToggle = { enabled ->
                            val result = if (enabled) AutostartManager.enable()
                                         else         AutostartManager.disable()
                            if (result.isSuccess) autostartOn = enabled
                        },
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Version footer ────────────────────────────────────────────────
            Text(
                "WakeyWakey 0.1.0",
                color    = White.copy(alpha = 0.2f),
                fontSize = 11.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
    }
}

// ─── Sub-composables ──────────────────────────────────────────────────────────

@Composable
private fun SettingsSection(
    title:   String,
    icon:    ImageVector,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Surface)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = Yellow, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(title, color = Yellow, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        }
        content()
    }
}

@Composable
private fun ToggleRow(
    label:    String,
    subtitle: String,
    checked:  Boolean,
    onToggle: (Boolean) -> Unit,
) {
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .clickable { onToggle(!checked) }
            .padding(vertical = 6.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = Subtitle, fontSize = 12.sp, lineHeight = 16.sp)
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked         = checked,
            onCheckedChange = onToggle,
            colors          = SwitchDefaults.colors(
                checkedThumbColor       = Navy,
                checkedTrackColor       = Yellow,
                uncheckedThumbColor     = Subtitle,
                uncheckedTrackColor     = Surface2,
                uncheckedBorderColor    = Surface2,
            ),
        )
    }
}

@Composable
private fun AlertMinutesSelector(current: Int, onChange: (Int) -> Unit) {
    val options = listOf(1, 2, 5, 10, 15)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { min ->
            val selected = current == min
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (selected) Yellow else Surface2)
                    .clickable { onChange(min) }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "${min}m",
                    color      = if (selected) Navy else White.copy(alpha = 0.7f),
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    fontSize   = 14.sp,
                )
            }
        }
    }
}

@Composable
private fun ConnectedAccountRow(
    email:       String,
    provider:    String,
    onDisconnect: () -> Unit,
    onReconnect:  () -> Unit,
) {
    var showConfirm by remember { mutableStateOf(false) }

    Row(
        modifier          = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Provider badge
        val (badgeColor, badgeLabel) = when (provider) {
            "google"    -> Color(0xFF4285F4) to "G"
            "microsoft" -> Color(0xFF0078D4) to "M"
            else        -> Subtitle          to "?"
        }
        Box(
            modifier         = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(badgeColor.copy(alpha = 0.2f))
                .border(1.dp, badgeColor.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(badgeLabel, color = badgeColor, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                email.ifBlank { provider.replaceFirstChar { it.uppercase() } },
                color      = White,
                fontSize   = 14.sp,
                fontWeight = FontWeight.Medium,
            )
            Text(
                when (provider) {
                    "google"    -> "Google Calendar"
                    "microsoft" -> "Microsoft Outlook"
                    else        -> provider
                },
                color    = Subtitle,
                fontSize = 12.sp,
            )
        }
        // Disconnect button
        IconButton(
            onClick = { showConfirm = true },
        ) {
            Icon(
                imageVector        = Icons.Filled.LinkOff,
                contentDescription = "Disconnect",
                tint               = Danger.copy(alpha = 0.7f),
            )
        }
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            containerColor   = Surface,
            title   = { Text("Disconnect calendar?", color = White) },
            text    = {
                Text(
                    "WakeyWakey will stop receiving meeting alerts until you connect again.",
                    color = Subtitle,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showConfirm = false
                    onDisconnect()
                }) {
                    Text("Disconnect", color = Danger, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) {
                    Text("Cancel", color = Yellow)
                }
            },
        )
    }
}

@Composable
private fun WorkHoursEditor(s: UserSettings) {
    // ── Time range ────────────────────────────────────────────────────────────
    Row(
        modifier          = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("From", color = Subtitle, fontSize = 13.sp)
        HourDropdown(
            hour     = s.workHoursStart,
            onChange = { DesktopSettingsRepository.save(s.copy(workHoursStart = it)) },
        )
        Text("to", color = Subtitle, fontSize = 13.sp)
        HourDropdown(
            hour     = s.workHoursEnd,
            onChange = { DesktopSettingsRepository.save(s.copy(workHoursEnd = it)) },
        )
    }

    Spacer(Modifier.height(4.dp))

    // ── Day picker ────────────────────────────────────────────────────────────
    Text("Active days:", color = Subtitle, fontSize = 13.sp)
    Spacer(Modifier.height(4.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        // Calendar constants: 2=Mon 3=Tue 4=Wed 5=Thu 6=Fri 7=Sat 1=Sun
        listOf(2 to "M", 3 to "T", 4 to "W", 5 to "T", 6 to "F", 7 to "S", 1 to "S")
            .forEach { (day, label) ->
                val selected = day in s.workDays
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (selected) Yellow else Surface2)
                        .border(1.dp, if (selected) Yellow else Subtitle.copy(0.3f), CircleShape)
                        .clickable {
                            val newDays = if (selected) s.workDays - day else s.workDays + day
                            DesktopSettingsRepository.save(s.copy(workDays = newDays))
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        label,
                        color      = if (selected) Navy else White.copy(alpha = 0.5f),
                        fontWeight = FontWeight.Bold,
                        fontSize   = 13.sp,
                    )
                }
            }
    }
}

@Composable
private fun HourDropdown(hour: Int, onChange: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Surface2)
                .clickable { expanded = true }
                .padding(horizontal = 14.dp, vertical = 8.dp),
        ) {
            Text(
                "%02d:00".format(hour),
                color      = White,
                fontSize   = 14.sp,
                fontWeight = FontWeight.Medium,
            )
        }
        DropdownMenu(
            expanded         = expanded,
            onDismissRequest = { expanded = false },
            containerColor   = Surface2,
        ) {
            (0..23).forEach { h ->
                DropdownMenuItem(
                    text    = { Text("%02d:00".format(h), color = if (h == hour) Yellow else White) },
                    onClick = { onChange(h); expanded = false },
                )
            }
        }
    }
}
