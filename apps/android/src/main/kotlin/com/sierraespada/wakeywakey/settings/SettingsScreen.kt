package com.sierraespada.wakeywakey.settings

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sierraespada.wakeywakey.R
import com.sierraespada.wakeywakey.billing.EntitlementManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sierraespada.wakeywakey.model.UserSettings

private val Yellow      = Color(0xFFFFE03A)
private val Navy        = Color(0xFF1A1A2E)
private val NavySurface = Color(0xFF16213E)

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onShowPaywall: () -> Unit = {},
    isTablet: Boolean = false,
    vm: SettingsViewModel = viewModel(),
) {
    val isPro by EntitlementManager.isPro.collectAsState()
    val context   = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val state by vm.state.collectAsState()
    val s = state.settings

    // Refresh overlay permission state each time user returns from Android Settings
    var overlayGranted by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            overlayGranted = Settings.canDrawOverlays(context)
        }
    }

    // On tablet: centre content with a max width of 600 dp
    val horizontalPadding = if (isTablet) 0.dp else 20.dp
    val contentModifier   = if (isTablet) {
        Modifier
            .widthIn(max = 600.dp)
            .fillMaxHeight()
    } else {
        Modifier.fillMaxSize()
    }

    Box(
        modifier         = Modifier
            .fillMaxSize()
            .background(Navy)
            .systemBarsPadding(),
        contentAlignment = if (isTablet) Alignment.TopCenter else Alignment.TopStart,
    ) {
    Column(modifier = contentModifier) {
        // ── Top bar ───────────────────────────────────────────────────────
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(horizontal = if (isTablet) 24.dp else 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onBack) {
                Text(stringResource(R.string.settings_back), color = Yellow, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.weight(1f))
            Text(
                stringResource(R.string.settings_title),
                fontSize   = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color      = Color.White,
            )
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.width(64.dp)) // balance the Back button
        }

        LazyColumn(
            contentPadding      = PaddingValues(
                start  = if (isTablet) 24.dp else 20.dp,
                end    = if (isTablet) 24.dp else 20.dp,
                bottom = 32.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // ── ALERT DISPLAY ─────────────────────────────────────────────
            item { SectionHeader(stringResource(R.string.settings_section_alert_display)) }
            item { OverlayPermissionCard(granted = overlayGranted, context = context) }

            // ── GENERAL ───────────────────────────────────────────────────
            item { SectionHeader(stringResource(R.string.settings_section_general)) }

            item {
                SettingsCard {
                    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                        // Minutes before (Pro: all options except 1 min)
                        SettingsLabel(stringResource(R.string.settings_alert_time_label))
                        if (!isPro) ProBadgeRow(onShowPaywall)
                        Spacer(Modifier.height(8.dp))
                        MinutesBeforeRow(
                            selected  = s.alertMinutesBefore,
                            onSelect  = { if (it == 1 || isPro) vm.setMinutesBefore(it) else onShowPaywall() },
                            isPro     = isPro,
                        )
                        HorizontalDivider(Modifier.padding(vertical = 12.dp), color = Color.White.copy(alpha = 0.07f))

                        // Sound
                        SettingsToggleRow(stringResource(R.string.settings_sound), s.soundEnabled, vm::setSoundEnabled)

                        if (s.soundEnabled) {
                            HorizontalDivider(Modifier.padding(vertical = 4.dp), color = Color.White.copy(alpha = 0.07f))
                            // Repeat sound — Pro
                            SettingsToggleRow(
                                label     = stringResource(R.string.settings_repeat_sound),
                                checked   = s.repeatSound,
                                onToggle  = { if (isPro) vm.setRepeatSound(it) else onShowPaywall() },
                                proLocked = !isPro,
                            )
                        }

                        HorizontalDivider(Modifier.padding(vertical = 4.dp), color = Color.White.copy(alpha = 0.07f))

                        // Vibration
                        SettingsToggleRow(stringResource(R.string.settings_vibration), s.vibrationEnabled, vm::setVibrationEnabled)
                    }
                }
            }

            // ── EVENTS ────────────────────────────────────────────────────
            item { SectionHeader(stringResource(R.string.settings_section_events)) }

            item {
                SettingsCard {
                    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                        // Filters — todos Pro
                        SettingsToggleRow(
                            label     = stringResource(R.string.settings_filter_video_only),
                            checked   = s.filterVideoOnly,
                            onToggle  = { if (isPro) vm.setFilterVideoOnly(it) else onShowPaywall() },
                            proLocked = !isPro,
                        )
                        Divider()
                        SettingsToggleRow(
                            label     = stringResource(R.string.settings_filter_accepted_only),
                            checked   = s.filterAcceptedOnly,
                            onToggle  = { if (isPro) vm.setFilterAcceptedOnly(it) else onShowPaywall() },
                            proLocked = !isPro,
                        )
                        Divider()
                        SettingsToggleRow(stringResource(R.string.settings_show_all_day), s.showAllDayEvents, vm::setShowAllDayEvents)
                        Divider()

                        // Working hours — Pro
                        SettingsToggleRow(
                            label     = stringResource(R.string.settings_work_hours),
                            checked   = s.workHoursEnabled,
                            onToggle  = { if (isPro) vm.setWorkHoursEnabled(it) else onShowPaywall() },
                            proLocked = !isPro,
                        )
                        if (s.workHoursEnabled) {
                            Divider()
                            WorkHoursRow(
                                startHour     = s.workHoursStart,
                                endHour       = s.workHoursEnd,
                                onStartChange = vm::setWorkHoursStart,
                                onEndChange   = vm::setWorkHoursEnd,
                            )
                            Divider()
                            WorkDaysRow(selected = s.workDays, onToggle = { day ->
                                val updated = if (day in s.workDays) s.workDays - day else s.workDays + day
                                vm.setWorkDays(updated)
                            })
                        }
                    }
                }
            }

            // ── DEVELOPER ─────────────────────────────────────────────────
            item { SectionHeader("Developer") }
            item {
                SettingsCard {
                    SettingsToggleRow(
                        label    = "Show debug bar",
                        checked  = s.showDevBar,
                        onToggle = vm::setShowDevBar,
                    )
                }
            }

            // ── CALENDARS ─────────────────────────────────────────────────
            if (state.availableCalendars.isNotEmpty()) {
                item { SectionHeader(stringResource(R.string.settings_section_calendars)) }
                item {
                    SettingsCard {
                        Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                            Text(
                                stringResource(R.string.settings_calendars_hint),
                                fontSize = 11.sp,
                                color    = Color.White.copy(alpha = 0.35f),
                            )
                            Spacer(Modifier.height(8.dp))
                            state.availableCalendars.forEachIndexed { index, cal ->
                                if (index > 0) Divider()
                                Row(
                                    modifier          = Modifier
                                        .fillMaxWidth()
                                        .clickable { vm.toggleCalendar(cal.id) }
                                        .padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Checkbox(
                                        checked         = s.enabledCalendarIds.isEmpty() ||
                                                          cal.id in s.enabledCalendarIds,
                                        onCheckedChange = { vm.toggleCalendar(cal.id) },
                                        colors          = CheckboxDefaults.colors(
                                            checkedColor   = Yellow,
                                            uncheckedColor = Color.White.copy(alpha = 0.4f),
                                            checkmarkColor = Navy,
                                        ),
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Column {
                                        Text(cal.name, fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                                        Text(cal.accountName, fontSize = 11.sp, color = Color.White.copy(alpha = 0.4f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } // end LazyColumn
    } // end inner Column
    } // end Box
} // end SettingsScreen

// ─── Reusable components ──────────────────────────────────────────────────────

// ─── Overlay permission card ──────────────────────────────────────────────────

private val Green = Color(0xFF4CAF50)
private val Coral = Color(0xFFFF6B6B)

@Composable
private fun OverlayPermissionCard(granted: Boolean, context: android.content.Context) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = NavySurface,
    ) {
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Status icon
            Box(
                modifier         = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (granted) Green.copy(alpha = 0.15f) else Yellow.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text     = if (granted) "✓" else "🖥️",
                    fontSize = if (granted) 20.sp else 22.sp,
                    color    = if (granted) Green else Color.White,
                )
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    stringResource(R.string.settings_overlay_title),
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = Color.White,
                )
                Text(
                    if (granted) stringResource(R.string.settings_overlay_granted)
                    else         stringResource(R.string.settings_overlay_not_granted),
                    fontSize   = 12.sp,
                    color      = Color.White.copy(alpha = 0.5f),
                    lineHeight = 17.sp,
                )
            }

            if (!granted) {
                TextButton(
                    onClick = {
                        context.startActivity(
                            Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}"),
                            ).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
                        )
                    },
                    colors         = ButtonDefaults.textButtonColors(contentColor = Yellow),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text(stringResource(R.string.settings_overlay_allow), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun Divider() {
    HorizontalDivider(Modifier.padding(vertical = 8.dp), color = Color.White.copy(alpha = 0.07f))
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text       = title.uppercase(),
        fontSize   = 11.sp,
        fontWeight = FontWeight.Bold,
        color      = Yellow.copy(alpha = 0.7f),
        letterSpacing = 1.2.sp,
        modifier   = Modifier.padding(top = 12.dp, bottom = 4.dp, start = 4.dp),
    )
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = NavySurface,
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}

@Composable
private fun SettingsLabel(text: String) {
    Text(text, fontSize = 13.sp, color = Color.White.copy(alpha = 0.5f), fontWeight = FontWeight.SemiBold)
}

@Composable
private fun SettingsToggleRow(
    label: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
    proLocked: Boolean = false,
) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .then(if (proLocked) Modifier.clickable { onToggle(!checked) } else Modifier),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            fontSize = 15.sp,
            color    = if (proLocked) Color.White.copy(alpha = 0.45f) else Color.White,
            modifier = Modifier.weight(1f),
        )
        if (proLocked) {
            ProChip()
        } else {
            Switch(
                checked         = checked,
                onCheckedChange = onToggle,
                colors          = SwitchDefaults.colors(
                    checkedThumbColor   = Navy,
                    checkedTrackColor   = Yellow,
                    uncheckedTrackColor = Color.White.copy(alpha = 0.2f),
                ),
            )
        }
    }
}

@Composable
private fun ProChip() {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = Yellow.copy(alpha = 0.15f),
    ) {
        Text(
            stringResource(R.string.paywall_pro_badge),
            fontSize   = 10.sp,
            fontWeight = FontWeight.ExtraBold,
            color      = Yellow,
            modifier   = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
        )
    }
}

@Composable
private fun ProBadgeRow(onShowPaywall: () -> Unit) {
    TextButton(
        onClick        = onShowPaywall,
        contentPadding = PaddingValues(0.dp),
    ) {
        Text(
            "🔒 " + stringResource(R.string.paywall_pro_badge),
            fontSize = 11.sp,
            color    = Yellow.copy(alpha = 0.6f),
        )
    }
}

@Composable
private fun MinutesBeforeRow(selected: Int, onSelect: (Int) -> Unit, isPro: Boolean = true) {
    val options = listOf(0 to "30 s", 1 to "1 min", 2 to "2 min", 5 to "5 min", 10 to "10 min")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (value, label) ->
            val active   = selected == value
            val freeOption = value == 1   // solo 1 min es gratis
            val locked   = !isPro && !freeOption
            FilterChip(
                selected = active,
                onClick  = { onSelect(value) },
                label    = {
                    Text(
                        if (locked) "$label 🔒" else label,
                        fontSize = 12.sp,
                    )
                },
                colors   = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Yellow,
                    selectedLabelColor     = Navy,
                    containerColor         = if (locked) Color.White.copy(alpha = 0.03f)
                                             else        Color.White.copy(alpha = 0.07f),
                    labelColor             = if (locked) Color.White.copy(alpha = 0.3f)
                                             else        Color.White,
                ),
            )
        }
    }
}

@Composable
private fun WorkHoursRow(
    startHour: Int,
    endHour: Int,
    onStartChange: (Int) -> Unit,
    onEndChange: (Int) -> Unit,
) {
    Row(
        modifier          = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.settings_work_hours_from), fontSize = 14.sp, color = Color.White.copy(alpha = 0.6f))
        HourPicker(hour = startHour, onChange = onStartChange, modifier = Modifier.weight(1f))
        Text(stringResource(R.string.settings_work_hours_to), fontSize = 14.sp, color = Color.White.copy(alpha = 0.6f))
        HourPicker(hour = endHour, onChange = onEndChange, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun HourPicker(hour: Int, onChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            shape   = RoundedCornerShape(10.dp),
            colors  = ButtonDefaults.outlinedButtonColors(contentColor = Yellow),
            border  = androidx.compose.foundation.BorderStroke(1.dp, Yellow.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("%02d:00".format(hour), fontWeight = FontWeight.Bold)
        }
        DropdownMenu(
            expanded         = expanded,
            onDismissRequest = { expanded = false },
            containerColor   = NavySurface,
        ) {
            (0..23).forEach { h ->
                DropdownMenuItem(
                    text    = { Text("%02d:00".format(h), color = if (h == hour) Yellow else Color.White) },
                    onClick = { onChange(h); expanded = false },
                )
            }
        }
    }
}

@Composable
private fun WorkDaysRow(selected: Set<Int>, onToggle: (Int) -> Unit) {
    // Calendar constants: 2=Mon … 6=Fri, 7=Sat, 1=Sun
    val days = listOf(2 to "M", 3 to "T", 4 to "W", 5 to "T", 6 to "F", 7 to "S", 1 to "S")
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        days.forEach { (day, label) ->
            val active = day in selected
            Surface(
                shape    = RoundedCornerShape(50),
                color    = if (active) Yellow else Color.White.copy(alpha = 0.08f),
                modifier = Modifier
                    .size(40.dp)
                    .clickable { onToggle(day) },
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        label,
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color      = if (active) Navy else Color.White.copy(alpha = 0.5f),
                    )
                }
            }
        }
    }
}
