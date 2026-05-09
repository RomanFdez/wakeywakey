package com.sierraespada.wakeywakey.windows.settings

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import com.sierraespada.wakeywakey.windows.ui.Alarm
import com.sierraespada.wakeywakey.windows.ui.BugReport
import com.sierraespada.wakeywakey.windows.ui.CalendarMonth
import com.sierraespada.wakeywakey.windows.ui.CalendarViewMonth
import com.sierraespada.wakeywakey.windows.ui.Dvr
import com.sierraespada.wakeywakey.windows.ui.ExpandMore
import com.sierraespada.wakeywakey.windows.ui.FilterList
import com.sierraespada.wakeywakey.windows.ui.LinkOff
import com.sierraespada.wakeywakey.windows.ui.Palette
import com.sierraespada.wakeywakey.windows.ui.Schedule
import com.sierraespada.wakeywakey.windows.ui.VolumeDown
import com.sierraespada.wakeywakey.windows.ui.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sierraespada.wakeywakey.model.DeviceCalendar
import com.sierraespada.wakeywakey.model.UserSettings
import com.sierraespada.wakeywakey.windows.PlatformMode
import com.sierraespada.wakeywakey.windows.calendar.CalendarAccountManager

// ─── Paleta ──────────────────────────────────────────────────────────────────

private val Yellow    = Color(0xFFFFE03A)
private val Navy      = Color(0xFF1A1A2E)
private val Surface   = Color(0xFF16213E)
private val Surface2  = Color(0xFF0F3460)
private val White     = Color.White
private val Danger    = Color(0xFFFF6B6B)
private val Subtitle  = Color(0xFF8892AA)

// ─── Tabs ─────────────────────────────────────────────────────────────────────

private enum class SettingsTab(val label: String, val icon: ImageVector) {
    CALENDAR("Calendar", Icons.Filled.CalendarMonth),
    ALERTS  ("Alerts",   Icons.Filled.Alarm),
    MENU_BAR("Menu Bar", Icons.Filled.Dvr),
    APP     ("App",      Icons.Filled.Settings),
}

// ─── Root ─────────────────────────────────────────────────────────────────────

@Composable
fun DesktopSettingsScreen(
    onConnectCalendar: () -> Unit,
    appIcon:           Painter,
    allCalendars:      List<DeviceCalendar> = emptyList(),
    platformMode:      PlatformMode         = PlatformMode.WINDOWS_OAUTH,
    onUpgrade:         () -> Unit           = {},
) {
    val s   by DesktopSettingsRepository.settings.collectAsState()
    var tab by remember { mutableStateOf(SettingsTab.CALENDAR) }

    Column(Modifier.fillMaxSize().background(Navy)) {

        Text(
            "Settings",
            color      = Yellow,
            fontSize   = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier   = Modifier.padding(start = 18.dp, top = 12.dp, bottom = 2.dp),
        )

        TabRow(selectedTabIndex = tab.ordinal, containerColor = Navy, contentColor = Yellow) {
            SettingsTab.entries.forEach { t ->
                Tab(
                    selected               = tab == t,
                    onClick                = { tab = t },
                    selectedContentColor   = Yellow,
                    unselectedContentColor = Subtitle,
                ) {
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        modifier              = Modifier.padding(vertical = 10.dp),
                    ) {
                        Icon(t.icon, contentDescription = null, modifier = Modifier.size(14.dp))
                        Text(t.label, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }

        // Tab content — scrollable para soportar muchos calendarios
        val tabScroll = androidx.compose.foundation.rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(tabScroll)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            when (tab) {
                SettingsTab.CALENDAR -> CalendarTab(s, onConnectCalendar, allCalendars, platformMode, onUpgrade)
                SettingsTab.ALERTS   -> AlertsTab(s, onUpgrade)
                SettingsTab.MENU_BAR -> MenuBarTab(s, appIcon)
                SettingsTab.APP      -> AppTab(s)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "WakeyWakey 0.1.0",
                color    = White.copy(alpha = 0.15f),
                fontSize = 10.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
    }
}

// ─── Tab: Calendar ────────────────────────────────────────────────────────────

@Composable
private fun CalendarTab(
    s:             UserSettings,
    onConnectCalendar: () -> Unit,
    allCalendars:  List<DeviceCalendar>,
    platformMode:  PlatformMode,
    onUpgrade:     () -> Unit = {},
) {
    val isMacMode = platformMode == PlatformMode.MAC_SYSTEM

    // ── Modo macOS: fuente del sistema, sin OAuth ─────────────────────────────
    if (isMacMode) {
        Card {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(Icons.Filled.CalendarMonth, null, tint = Yellow, modifier = Modifier.size(18.dp))
                Column {
                    Text("Using macOS system calendars", color = White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Text("Google, iCloud, Exchange… all calendars from the macOS Calendar app", color = Subtitle, fontSize = 11.sp)
                }
            }
        }
    }

    // ── Cuentas OAuth — Google y Microsoft independientes ─────────────────────
    if (!isMacMode) {
        val activeProviders  by CalendarAccountManager.activeProviders.collectAsState()
        // connectedEmails es reactivo: se actualiza cuando getAvailableCalendars
        // resuelve el email en background (p.ej. primer arranque sin email en token).
        val connectedEmails  by CalendarAccountManager.connectedEmails.collectAsState()
        val googleConnected    = "google"    in activeProviders
        val microsoftConnected = "microsoft" in activeProviders

        Card {
            SectionLabel("Calendar accounts", Icons.Filled.CalendarMonth)
            ItemDivider()

            // ── Google ────────────────────────────────────────────────────────
            if (googleConnected) {
                ConnectedAccountRow(
                    email        = connectedEmails["google"] ?: "",
                    provider     = "google",
                    onDisconnect = { CalendarAccountManager.disconnect("google") },
                    onReconnect  = onConnectCalendar,
                )
            } else {
                ProviderConnectRow(
                    label   = "Google Calendar",
                    color   = Color(0xFF4285F4),
                    enabled = true,
                    onClick = onConnectCalendar,
                )
            }

            ItemDivider()

            // ── Microsoft ─────────────────────────────────────────────────────
            if (microsoftConnected) {
                ConnectedAccountRow(
                    email        = connectedEmails["microsoft"] ?: "",
                    provider     = "microsoft",
                    onDisconnect = { CalendarAccountManager.disconnect("microsoft") },
                    onReconnect  = onConnectCalendar,
                )
            } else {
                ProviderConnectRow(
                    label   = "Microsoft / Outlook",
                    color   = Color(0xFF0078D4),
                    enabled = true,
                    onClick = onConnectCalendar,
                )
            }
        }
    }

    // ── Filtros ───────────────────────────────────────────────────────────────
    Card {
        SectionLabel("Event filters", Icons.Filled.FilterList)
        ItemDivider()
        CompactToggle("Video meetings only", s.filterVideoOnly) {
            DesktopSettingsRepository.save(s.copy(filterVideoOnly = it))
        }
        ItemDivider()
        CompactToggle("Accepted events only", s.filterAcceptedOnly) {
            DesktopSettingsRepository.save(s.copy(filterAcceptedOnly = it))
        }
        ItemDivider()
        CompactToggle("Include all-day events", s.showAllDayEvents) {
            DesktopSettingsRepository.save(s.copy(showAllDayEvents = it))
        }
    }

    // ── Calendarios agrupados por cuenta ──────────────────────────────────────
    if (allCalendars.isNotEmpty()) {
        val isProForCal   by com.sierraespada.wakeywakey.windows.billing.DesktopEntitlementManager.isPro.collectAsState()
        val isTrialForCal = com.sierraespada.wakeywakey.windows.billing.DesktopEntitlementManager.isTrialActive
        val calUnlocked   = isProForCal || isTrialForCal

        val allIds    = allCalendars.map { it.id }.toSet()
        val byAccount = allCalendars
            .sortedWith(compareBy({ it.accountName }, { it.name }))
            .groupBy { it.accountName }

        // Free tier: el usuario elige cual de sus calendarios usar (máx. 1).
        // Si aún no ha elegido, usamos el primero disponible como predeterminado.
        val freeActiveId: Long? = if (!calUnlocked) {
            if (s.enabledCalendarIds.isNotEmpty()) s.enabledCalendarIds.first()
            else allCalendars.firstOrNull()?.id
        } else null

        Card {
            // Cabecera de sección con hint de límite en Free tier
            Row(
                modifier              = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Icon(Icons.Filled.CalendarViewMonth, null, tint = Yellow, modifier = Modifier.size(13.dp))
                    Text("Calendars", color = Yellow, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                }
                if (!calUnlocked) {
                    Row(
                        modifier              = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { onUpgrade() }
                            .background(Yellow.copy(alpha = 0.08f))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Text("🔒", fontSize = 9.sp)
                        Text("Free · max 1", color = Yellow.copy(alpha = 0.7f), fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            byAccount.entries.forEach { (account, cals) ->
                ItemDivider()

                // ── Cabecera de cuenta ────────────────────────────────────────
                Row(
                    modifier              = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(Icons.Filled.AccountCircle, null, tint = Subtitle, modifier = Modifier.size(13.dp))
                    Text(account, color = Subtitle, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }

                // ── Calendarios de esa cuenta ─────────────────────────────────
                cals.forEach { cal ->
                    ItemDivider()
                    if (!calUnlocked) {
                        // Free tier: comportamiento de radio button — el usuario elige cuál usar
                        val selected = cal.id == freeActiveId
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    DesktopSettingsRepository.save(s.copy(enabledCalendarIds = setOf(cal.id)))
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                cal.name,
                                color      = if (selected) White else Subtitle.copy(alpha = 0.55f),
                                fontSize   = 13.sp,
                                fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                                modifier   = Modifier.weight(1f),
                            )
                            RadioButton(
                                selected = selected,
                                onClick  = {
                                    DesktopSettingsRepository.save(s.copy(enabledCalendarIds = setOf(cal.id)))
                                },
                                colors   = RadioButtonDefaults.colors(
                                    selectedColor   = Yellow,
                                    unselectedColor = Subtitle.copy(alpha = 0.4f),
                                ),
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    } else {
                        // Pro / Trial: toggles independientes
                        val enabled = s.enabledCalendarIds.isEmpty() || cal.id in s.enabledCalendarIds
                        CompactToggle(cal.name, enabled) { on ->
                            val current = if (s.enabledCalendarIds.isEmpty()) allIds else s.enabledCalendarIds
                            val newSet  = if (on) current + cal.id else current - cal.id
                            val toSave  = if (newSet == allIds) emptySet() else newSet
                            DesktopSettingsRepository.save(s.copy(enabledCalendarIds = toSave))
                        }
                    }
                }
            }
        }
    }
}

// ─── Tab: Alerts ─────────────────────────────────────────────────────────────

@Composable
private fun AlertsTab(s: UserSettings, onUpgrade: () -> Unit = {}) {

    // ── Timing + Display — un solo bloque ─────────────────────────────────────
    Card {
        SectionLabel("Alert", Icons.Filled.Alarm)
        ItemDivider()
        // Minutos antes
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            listOf(1, 2, 5, 10, 15).forEach { min ->
                val sel = s.alertMinutesBefore == min
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (sel) Yellow else Surface2)
                        .clickable { DesktopSettingsRepository.save(s.copy(alertMinutesBefore = min)) }
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text       = "${min}m",
                        color      = if (sel) Navy else White.copy(alpha = 0.7f),
                        fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                        fontSize   = 12.sp,
                        lineHeight = 12.sp,
                        textAlign  = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            }
        }
        ItemDivider()
        CompactToggle(
            label    = "Alert on active screen only",
            subtitle = "Off = show on all connected monitors",
            checked  = !s.alertAllScreens,
        ) { DesktopSettingsRepository.save(s.copy(alertAllScreens = !it)) }
    }

    // ── Sound ─────────────────────────────────────────────────────────────────
    Card {
        SectionLabel("Sound", Icons.Filled.VolumeUp)
        ItemDivider()
        CompactToggle("Sound alert", s.soundEnabled) {
            DesktopSettingsRepository.save(s.copy(soundEnabled = it))
        }
        if (s.soundEnabled) {
            ItemDivider()
            SoundSelectorRow(s)
            ItemDivider()
            // Volume bar
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.VolumeDown, null, tint = Subtitle, modifier = Modifier.size(13.dp))
                Slider(
                    value         = s.alertVolume,
                    onValueChange = { DesktopSettingsRepository.save(s.copy(alertVolume = it)) },
                    valueRange    = 0f..1f,
                    colors        = SliderDefaults.colors(
                        thumbColor         = Yellow,
                        activeTrackColor   = Yellow,
                        inactiveTrackColor = Surface2,
                    ),
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "${(s.alertVolume * 100).toInt()}%",
                    color    = Subtitle,
                    fontSize = 11.sp,
                    modifier = Modifier.width(30.dp).padding(start = 4.dp),
                )
            }
            ItemDivider()
            CompactToggle("Loop sound", "Repeat until dismissed or snoozed", s.repeatSound) {
                DesktopSettingsRepository.save(s.copy(repeatSound = it))
            }
        }
    }

    // ── Working hours (Pro) ───────────────────────────────────────────────────
    val isProForWorkHours by com.sierraespada.wakeywakey.windows.billing.DesktopEntitlementManager.isPro.collectAsState()
    val isTrialForWorkHours = com.sierraespada.wakeywakey.windows.billing.DesktopEntitlementManager.isTrialActive
    val workHoursUnlocked = isProForWorkHours || isTrialForWorkHours
    Card {
        SectionLabel("Working hours", Icons.Filled.Schedule)
        ItemDivider()
        CompactToggle(
            label    = "Working hours only",
            subtitle = if (workHoursUnlocked) "Silence alerts outside your work schedule"
                       else "🔒 Pro feature — upgrade to enable",
            checked  = s.workHoursEnabled && workHoursUnlocked,
        ) {
            if (workHoursUnlocked) DesktopSettingsRepository.save(s.copy(workHoursEnabled = it))
            else onUpgrade()
        }
        if (s.workHoursEnabled && workHoursUnlocked) {
            ItemDivider()
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("From", color = Subtitle, fontSize = 12.sp)
                HourDropdown(s.workHoursStart) { DesktopSettingsRepository.save(s.copy(workHoursStart = it)) }
                Text("to", color = Subtitle, fontSize = 12.sp)
                HourDropdown(s.workHoursEnd) { DesktopSettingsRepository.save(s.copy(workHoursEnd = it)) }
                Spacer(Modifier.weight(1f))
                listOf(2 to "M", 3 to "T", 4 to "W", 5 to "T", 6 to "F", 7 to "S", 1 to "S")
                    .forEach { (day, label) ->
                        val sel = day in s.workDays
                        Box(
                            modifier = Modifier
                                .size(24.dp).clip(CircleShape)
                                .background(if (sel) Yellow else Surface2)
                                .clickable {
                                    val nd = if (sel) s.workDays - day else s.workDays + day
                                    DesktopSettingsRepository.save(s.copy(workDays = nd))
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text       = label,
                                color      = if (sel) Navy else White.copy(alpha = 0.5f),
                                fontWeight = FontWeight.Bold,
                                fontSize   = 10.sp,
                                lineHeight = 10.sp,   // elimina el padding vertical extra del font metrics
                                textAlign  = androidx.compose.ui.text.style.TextAlign.Center,
                            )
                        }
                    }
            }
        }
    }
}

// ── Sound selector dropdown ────────────────────────────────────────────────────

@Composable
private fun SoundSelectorRow(s: UserSettings) {
    val isPro      by com.sierraespada.wakeywakey.windows.billing.DesktopEntitlementManager.isPro.collectAsState()
    val isTrial    = com.sierraespada.wakeywakey.windows.billing.DesktopEntitlementManager.isTrialActive
    val allSounds  = com.sierraespada.wakeywakey.windows.SoundPlayer.SOUND_DEFS
    // Free tier: solo los primeros 3 sonidos
    val sounds     = if (isPro || isTrial) allSounds else allSounds.take(3)
    val current    = sounds.firstOrNull { it.id == s.alertSoundId } ?: sounds[0]
    var expanded   by remember { mutableStateOf(false) }
    val previewJob = remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    Row(
        modifier  = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text("Sound", color = White, fontSize = 13.sp, fontWeight = FontWeight.Medium)

        Box {
            // Selector button
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Surface2)
                    .clickable { expanded = true }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(current.emoji, fontSize = 14.sp)
                Text(current.label, color = Yellow, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Icon(Icons.Filled.ExpandMore, null, tint = Subtitle, modifier = Modifier.size(14.dp))
            }

            // Dropdown — compact items
            DropdownMenu(
                expanded         = expanded,
                onDismissRequest = { expanded = false },
                containerColor   = Surface2,
            ) {
                sounds.forEach { sound ->
                    val selected = sound.id == s.alertSoundId
                    DropdownMenuItem(
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        text = {
                            Row(
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier              = Modifier.padding(vertical = 2.dp),
                            ) {
                                Text(sound.emoji, fontSize = 13.sp)
                                Text(
                                    sound.label,
                                    color      = if (selected) Yellow else White,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                    fontSize   = 12.sp,
                                )
                            }
                        },
                        onClick = {
                            expanded = false
                            DesktopSettingsRepository.save(s.copy(alertSoundId = sound.id))
                            previewJob.value?.cancel()
                            previewJob.value = com.sierraespada.wakeywakey.windows.SoundPlayer.play(
                                sound.id, s.alertVolume, false,
                            )
                        },
                    )
                }
            }
        }
    }
}

// ─── Tab: Menu Bar ────────────────────────────────────────────────────────────

@Composable
private fun MenuBarTab(s: UserSettings, appIcon: Painter) {

    // Display options
    Card {
        SectionLabel("Display", Icons.Filled.Dvr)
        ItemDivider()
        CompactToggle("Show next meeting name", s.trayShowMeetingName) {
            DesktopSettingsRepository.save(s.copy(trayShowMeetingName = it))
        }
        ItemDivider()
        CompactToggle("Show time remaining", s.trayShowTimeRemaining) {
            DesktopSettingsRepository.save(s.copy(trayShowTimeRemaining = it))
        }
        ItemDivider()
        CompactToggle("Minutes only  (5m vs 5m 30s)", s.countdownMinutesOnly) {
            DesktopSettingsRepository.save(s.copy(countdownMinutesOnly = it))
        }
        ItemDivider()
        CompactToggle("Include tomorrow's meetings", s.trayIncludeTomorrow) {
            DesktopSettingsRepository.save(s.copy(trayIncludeTomorrow = it))
        }
        ItemDivider()
        // Title length inline
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Title length", color = White, fontSize = 13.sp, modifier = Modifier.width(90.dp))
            Slider(
                value         = s.trayTitleMaxLength.toFloat(),
                onValueChange = { DesktopSettingsRepository.save(s.copy(trayTitleMaxLength = it.toInt())) },
                valueRange    = 10f..50f,
                steps         = 39,
                colors        = SliderDefaults.colors(
                    thumbColor         = Yellow,
                    activeTrackColor   = Yellow,
                    inactiveTrackColor = Surface2,
                ),
                modifier = Modifier.weight(1f),
            )
            Text(
                "${s.trayTitleMaxLength}",
                color    = Subtitle,
                fontSize = 11.sp,
                modifier = Modifier.width(26.dp).padding(start = 4.dp),
            )
        }
        ItemDivider()
        CompactToggle("Truncate in the middle", "e.g. \"Standup…sprint\" instead of \"Standup…\"", s.trayTitleTruncateMiddle) {
            DesktopSettingsRepository.save(s.copy(trayTitleTruncateMiddle = it))
        }
    }

    // Appearance + preview
    Card {
        SectionLabel("Appearance", Icons.Filled.Palette)
        ItemDivider()
        CompactToggle(
            label    = "Monochrome icon",
            subtitle = if (s.trayMonochromeIcon)
                           "Color applies to icon + text"
                       else
                           "Icon is always yellow — color applies to text only",
            checked  = s.trayMonochromeIcon,
        ) {
            DesktopSettingsRepository.save(s.copy(trayMonochromeIcon = it))
        }
        ItemDivider()
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                if (s.trayMonochromeIcon) "Color" else "Text",
                color    = Subtitle,
                fontSize = 12.sp,
                modifier = Modifier.width(40.dp),
            )
            TrayColorPicker(current = s.trayAccentColor) {
                DesktopSettingsRepository.save(s.copy(trayAccentColor = it))
            }
        }
        ItemDivider()
        // Preview
        val accentColor  = accentPreviewColor(s.trayAccentColor)
        // When NOT monochrome: icon is always yellow brand circle; text uses accentColor
        // When monochrome: both icon circle and text use accentColor
        val iconTintColor = if (s.trayMonochromeIcon) accentColor else Color(0xFFFFE03A)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF0A0A1A))
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                // Icon: tinted to accentColor when monochrome, normal when not
                Box(
                    modifier         = Modifier.size(22.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    androidx.compose.foundation.Image(
                        painter            = appIcon,
                        contentDescription = null,
                        modifier           = Modifier.size(22.dp),
                        colorFilter        = if (s.trayMonochromeIcon)
                            androidx.compose.ui.graphics.ColorFilter.tint(iconTintColor)
                        else null,
                    )
                }
                if (s.trayShowMeetingName) {
                    Text("Design Review", color = accentColor, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
                if (s.trayShowTimeRemaining) {
                    if (s.trayShowMeetingName) Text("·", color = accentColor.copy(alpha = 0.5f), fontSize = 13.sp)
                    Text(
                        if (s.countdownMinutesOnly) "12m" else "12m 30s",
                        color      = accentColor,
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                if (!s.trayShowMeetingName && !s.trayShowTimeRemaining) {
                    Text("Icon only", color = Subtitle, fontSize = 12.sp)
                }
            }
        }
    }
}

private fun accentPreviewColor(id: String) = when (id) {
    "red"    -> Color(0xFFFF3B30)
    "yellow" -> Color(0xFFFFCC00)
    "blue"   -> Color(0xFF007AFF)
    "purple" -> Color(0xFFAF52DE)
    "green"  -> Color(0xFF34C759)
    "orange" -> Color(0xFFFF9500)
    else     -> Color.White.copy(alpha = 0.85f)
}

// ─── Tab: App ─────────────────────────────────────────────────────────────────

@Composable
private fun AppTab(s: UserSettings) {

    // ── Plan / License status ─────────────────────────────────────────────────
    val isPro         by com.sierraespada.wakeywakey.windows.billing.DesktopEntitlementManager.isPro.collectAsState()
    val trialDaysLeft by com.sierraespada.wakeywakey.windows.billing.DesktopEntitlementManager.trialDaysLeft.collectAsState()
    val licenseKey    by com.sierraespada.wakeywakey.windows.billing.DesktopEntitlementManager.licenseKey.collectAsState()

    Card {
        SectionLabel("Plan", Icons.Filled.Star)
        ItemDivider()
        Row(
            modifier              = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                when {
                    isPro -> {
                        Text("✅ WakeyWakey Pro", color = Yellow, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        if (!licenseKey.isNullOrBlank()) {
                            val masked = licenseKey!!.take(8) + "••••••••••••••••••••"
                            Text(masked, color = Subtitle, fontSize = 10.sp)
                        }
                    }
                    trialDaysLeft > 0 -> {
                        Text("🔄 Free trial", color = White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text("$trialDaysLeft days remaining", color = Subtitle, fontSize = 11.sp)
                    }
                    else -> {
                        Text("⛔ Trial expired", color = Danger, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text("Upgrade to keep using WakeyWakey", color = Subtitle, fontSize = 11.sp)
                    }
                }
            }
        }

        // ── Desactivar este dispositivo (libera una plaza) ────────────────────
        if (isPro) {
            val scope = rememberCoroutineScope()
            var isDeactivating  by remember { mutableStateOf(false) }
            var deactivateError by remember { mutableStateOf<String?>(null) }
            ItemDivider()
            Row(
                modifier              = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Deactivate this device", color = White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Text("Frees up one of your 5 activations", color = Subtitle, fontSize = 10.sp)
                }
                Button(
                    onClick = {
                        isDeactivating = true; deactivateError = null
                        scope.launch {
                            val error = com.sierraespada.wakeywakey.windows.billing.DesktopEntitlementManager
                                .deactivateLicense()
                            isDeactivating = false
                            if (error != null) deactivateError = error
                        }
                    },
                    enabled = !isDeactivating,
                    colors  = ButtonDefaults.buttonColors(containerColor = Danger, contentColor = White),
                ) {
                    if (isDeactivating) CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = White)
                    else Text("Deactivate", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
            if (deactivateError != null) Text(deactivateError!!, color = Danger, fontSize = 10.sp)
        }

        // ── Activación manual por si falla el botón automático ────────────────
        if (!isPro) {
            val scope = rememberCoroutineScope()
            ItemDivider()
            var manualKey   by remember { mutableStateOf("") }
            var keyError    by remember { mutableStateOf<String?>(null) }
            var keySuccess  by remember { mutableStateOf(false) }
            var isValidating by remember { mutableStateOf(false) }

            Text("Have a license key?", color = Subtitle, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
            Row(
                modifier              = Modifier.fillMaxWidth().padding(top = 4.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                OutlinedTextField(
                    value         = manualKey,
                    onValueChange = { manualKey = it.trim(); keyError = null; keySuccess = false },
                    placeholder   = { Text("License key", color = Subtitle, fontSize = 11.sp) },
                    singleLine    = true,
                    enabled       = !isValidating,
                    modifier      = Modifier.weight(1f),
                    colors        = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = Yellow,
                        unfocusedBorderColor = Subtitle.copy(alpha = 0.3f),
                        focusedTextColor     = White,
                        unfocusedTextColor   = White,
                    ),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp),
                )
                Button(
                    onClick = {
                        if (manualKey.length < 10) { keyError = "Invalid key"; return@Button }
                        isValidating = true
                        scope.launch {
                            val error = com.sierraespada.wakeywakey.windows.billing.DesktopEntitlementManager
                                .activateLicenseOnline(manualKey)
                            isValidating = false
                            if (error == null) { keySuccess = true; manualKey = "" }
                            else keyError = error
                        }
                    },
                    enabled = !isValidating,
                    colors  = ButtonDefaults.buttonColors(containerColor = Yellow, contentColor = Color(0xFF1A1A2E)),
                ) {
                    if (isValidating) CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = Color(0xFF1A1A2E))
                    else Text("Activate", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
            if (keyError != null)  Text(keyError!!, color = Danger, fontSize = 10.sp)
            if (keySuccess)        Text("✅ License activated!", color = Color(0xFF4CAF50), fontSize = 10.sp)
        }
    }

    if (AutostartManager.isSupported) {
        Card {
            var autostartOn by remember { mutableStateOf(AutostartManager.isEnabled) }
            CompactToggle("Launch at login", "Start WakeyWakey when you log in", autostartOn) { en ->
                val result = if (en) AutostartManager.enable() else AutostartManager.disable()
                if (result.isSuccess) autostartOn = en
            }
        }
    } else {
        Card {
            Text("No application settings available on this platform.", color = Subtitle, fontSize = 13.sp)
        }
    }

    // ── Developer — solo en builds de desarrollo ──────────────────────────────
    if (!com.sierraespada.wakeywakey.windows.AppBuildConfig.IS_RELEASE) {
        Card {
            SectionLabel("Developer", Icons.Filled.BugReport)
            ItemDivider()
            CompactToggle(
                label    = "Modo DEV",
                subtitle = "Muestra la barra de desarrollo en el popup del tray",
                checked  = s.showDevBar,
            ) { DesktopSettingsRepository.save(s.copy(showDevBar = it)) }
        }
    }
}

// ─── Shared building blocks ───────────────────────────────────────────────────

/**
 * Card contenedora de sección — fondo oscuro, esquinas redondeadas.
 */
@Composable
private fun Card(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Surface)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
        content = content,
    )
}

@Composable
private fun SectionLabel(text: String, icon: ImageVector) {
    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        modifier              = Modifier.padding(vertical = 2.dp),
    ) {
        Icon(icon, contentDescription = null, tint = Yellow, modifier = Modifier.size(13.dp))
        Text(text, color = Yellow, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
    }
}

@Composable
private fun ItemDivider() {
    HorizontalDivider(color = White.copy(alpha = 0.05f), modifier = Modifier.padding(vertical = 2.dp))
}

/**
 * Toggle row compacto — sin subtítulo (overload simple).
 */
@Composable
private fun CompactToggle(label: String, checked: Boolean, onToggle: (Boolean) -> Unit) {
    CompactToggle(label = label, subtitle = null, checked = checked, onToggle = onToggle)
}

/**
 * Toggle row compacto — con subtítulo opcional.
 */
@Composable
private fun CompactToggle(
    label:    String,
    subtitle: String? = null,
    checked:  Boolean,
    onToggle: (Boolean) -> Unit,
) {
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .clickable { onToggle(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            if (subtitle != null) {
                Text(subtitle, color = Subtitle, fontSize = 11.sp, lineHeight = 14.sp)
            }
        }
        Spacer(Modifier.width(10.dp))
        Box(modifier = Modifier.size(34.dp, 20.dp), contentAlignment = Alignment.Center) {
            Switch(
                checked         = checked,
                onCheckedChange = onToggle,
                modifier        = Modifier.scale(0.65f),
                colors          = SwitchDefaults.colors(
                    checkedThumbColor    = Navy,
                    checkedTrackColor    = Yellow,
                    uncheckedThumbColor  = Subtitle,
                    uncheckedTrackColor  = Surface2,
                    uncheckedBorderColor = Surface2,
                ),
            )
        }
    }
}

@Composable
private fun SmallArrowButton(icon: ImageVector, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Surface2)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = White.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
    }
}

// ─── ConnectedAccountRow ──────────────────────────────────────────────────────

@Composable
private fun ConnectedAccountRow(
    email:        String,
    provider:     String,
    onDisconnect: () -> Unit,
    onReconnect:  () -> Unit,
) {
    var showConfirm by remember { mutableStateOf(false) }

    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        val (badgeColor, badgeLabel) = when (provider) {
            "google"    -> Color(0xFF4285F4) to "G"
            "microsoft" -> Color(0xFF0078D4) to "M"
            else        -> Subtitle          to "?"
        }
        Box(
            modifier         = Modifier
                .size(32.dp).clip(CircleShape)
                .background(badgeColor.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(badgeLabel, color = badgeColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                email.ifBlank { provider.replaceFirstChar { it.uppercase() } },
                color      = White,
                fontSize   = 13.sp,
                fontWeight = FontWeight.Medium,
            )
            Text(
                when (provider) { "google" -> "Google Calendar"; "microsoft" -> "Outlook"; else -> provider },
                color    = Subtitle,
                fontSize = 11.sp,
            )
        }
        IconButton(onClick = { showConfirm = true }, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Filled.LinkOff, contentDescription = "Disconnect", tint = Danger.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
        }
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            containerColor   = Surface,
            title   = { Text("Disconnect calendar?", color = White) },
            text    = { Text("WakeyWakey will stop alerting until you reconnect.", color = Subtitle) },
            confirmButton = {
                TextButton(onClick = { showConfirm = false; onDisconnect() }) {
                    Text("Disconnect", color = Danger, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) { Text("Cancel", color = Yellow) }
            },
        )
    }
}

// ─── ProviderConnectRow ───────────────────────────────────────────────────────

@Composable
private fun ProviderConnectRow(
    label:   String,
    color:   Color,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier         = Modifier.size(32.dp).clip(CircleShape).background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label.first().toString(),
                    color      = color.copy(alpha = if (enabled) 1f else 0.4f),
                    fontWeight = FontWeight.Bold,
                    fontSize   = 14.sp,
                )
            }
            Text(label, color = if (enabled) Subtitle else Subtitle.copy(alpha = 0.5f), fontSize = 13.sp)
        }
        Button(
            onClick        = onClick,
            enabled        = enabled,
            colors         = ButtonDefaults.buttonColors(containerColor = color.copy(alpha = 0.15f), contentColor = color),
            shape          = RoundedCornerShape(8.dp),
            modifier       = Modifier.height(30.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
            elevation      = ButtonDefaults.buttonElevation(0.dp),
        ) {
            Text("Connect", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
        }
    }
}

// ─── HourDropdown ─────────────────────────────────────────────────────────────

@Composable
private fun HourDropdown(hour: Int, onChange: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(Surface2)
                .clickable { expanded = true }
                .padding(horizontal = 10.dp, vertical = 5.dp),
        ) {
            Text("%02d:00".format(hour), color = White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, containerColor = Surface2) {
            (0..23).forEach { h ->
                DropdownMenuItem(
                    text    = { Text("%02d:00".format(h), color = if (h == hour) Yellow else White, fontSize = 12.sp) },
                    onClick = { onChange(h); expanded = false },
                )
            }
        }
    }
}

// ─── TrayColorPicker ──────────────────────────────────────────────────────────

@Composable
private fun TrayColorPicker(current: String, onChange: (String) -> Unit) {
    val options = listOf(
        "system" to Color.White,
        "yellow" to Color(0xFFFFCC00),
        "red"    to Color(0xFFFF3B30),
        "blue"   to Color(0xFF007AFF),
        "green"  to Color(0xFF34C759),
        "orange" to Color(0xFFFF9500),
        "purple" to Color(0xFFAF52DE),
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (id, color) ->
            val selected = current == id
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(
                        width = if (selected) 2.dp else 0.dp,
                        color = Yellow,
                        shape = CircleShape,
                    )
                    .clickable { onChange(id) },
            )
        }
    }
}
