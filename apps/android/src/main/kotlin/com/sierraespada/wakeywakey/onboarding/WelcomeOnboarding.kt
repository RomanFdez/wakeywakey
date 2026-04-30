package com.sierraespada.wakeywakey.onboarding

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sierraespada.wakeywakey.calendar.AndroidCalendarRepository
import com.sierraespada.wakeywakey.model.CalendarEvent
import com.sierraespada.wakeywakey.model.DeviceCalendar
import com.sierraespada.wakeywakey.scheduler.AndroidAlarmScheduler
import com.sierraespada.wakeywakey.settings.SettingsRepository
import com.sierraespada.wakeywakey.ui.theme.WakeyWakeyTheme
import kotlinx.coroutines.launch

private val Yellow      = Color(0xFFFFE03A)
private val Navy        = Color(0xFF1A1A2E)
private val NavySurface = Color(0xFF16213E)
private val Green       = Color(0xFF4CAF50)

private const val PAGE_WELCOME     = 0
private const val PAGE_PERMISSIONS = 1
private const val PAGE_CALENDARS   = 2
private const val PAGE_PRO_TOUR    = 3
private const val PAGE_READY       = 4
private const val TOTAL_PAGES      = 5

// ─── Entry point ─────────────────────────────────────────────────────────────

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun WelcomeOnboarding(
    permVm: PermissionsViewModel = viewModel(),
    onComplete: () -> Unit,
) {
    val context   = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val scope     = rememberCoroutineScope()
    val pagerState = rememberPagerState { TOTAL_PAGES }

    val permState by permVm.state.collectAsState()

    // Refresh permission state every time the user comes back from Android Settings
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            permVm.refresh(context)
        }
    }

    // Calendar data for page 2
    var calendars    by remember { mutableStateOf<List<DeviceCalendar>>(emptyList()) }
    var selectedIds  by remember { mutableStateOf<Set<Long>>(emptySet()) }  // empty = all
    var calsLoading  by remember { mutableStateOf(false) }

    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage == PAGE_CALENDARS && calendars.isEmpty() && !calsLoading) {
            calsLoading = true
            calendars   = runCatching {
                AndroidCalendarRepository(context).getAvailableCalendars()
            }.getOrDefault(emptyList())
            calsLoading = false
        }
    }

    fun goNext() = scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }

    WakeyWakeyTheme(darkTheme = true) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Navy)
                .systemBarsPadding(),
        ) {
            HorizontalPager(
                state             = pagerState,
                userScrollEnabled = false,
                modifier          = Modifier.fillMaxSize(),
            ) { page ->
                when (page) {
                    PAGE_WELCOME -> WelcomePage(onNext = ::goNext)

                    PAGE_PERMISSIONS -> PermissionsPageContent(
                        state     = permState,
                        onRefresh = { permVm.refresh(context) },
                        onNext    = ::goNext,
                    )

                    PAGE_CALENDARS -> CalendarPickerPage(
                        calendars   = calendars,
                        loading     = calsLoading,
                        selectedIds = selectedIds,
                        onToggle    = { id ->
                            val allIds  = calendars.map { it.id }.toSet()
                            val current = if (selectedIds.isEmpty()) allIds else selectedIds
                            val newSet  = if (id in current) current - id else current + id
                            // Si están todos marcados volvemos a empty (= todos activos)
                            selectedIds = if (newSet == allIds) emptySet() else newSet
                        },
                        onNext = {
                            scope.launch {
                                SettingsRepository.getInstance(context).setEnabledCalendarIds(selectedIds)
                                goNext()
                            }
                        },
                    )

                    PAGE_PRO_TOUR -> ProTourPage(onNext = ::goNext)

                    PAGE_READY -> ReadyPage(
                        onComplete = {
                            scope.launch {
                                SettingsRepository.getInstance(context).completeOnboarding()
                                onComplete()
                            }
                        },
                    )
                }
            }

            // ── Page indicator dots ──────────────────────────────────────────
            Row(
                modifier              = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                repeat(TOTAL_PAGES) { i ->
                    val active = i == pagerState.currentPage
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (active) Yellow else Color.White.copy(alpha = 0.25f))
                            .size(if (active) 8.dp else 6.dp),
                    )
                }
            }
        }
    }
}

// ─── Page 0: Welcome ─────────────────────────────────────────────────────────

@Composable
private fun WelcomePage(onNext: () -> Unit) {
    Column(
        modifier            = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp)
            .padding(bottom = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // Illustration area (emoji art)
        Box(
            modifier         = Modifier
                .size(180.dp)
                .clip(CircleShape)
                .background(Yellow.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center,
        ) {
            Text("🗓️", fontSize = 72.sp)
        }

        Spacer(Modifier.height(40.dp))

        Text(
            text       = "Nunca más llegues\ntarde a una reunión",
            fontSize   = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            color      = Color.White,
            textAlign  = TextAlign.Center,
            lineHeight = 36.sp,
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text       = "WakeyWakey te avisa justo\nantes de que empiece tu próximo evento.",
            fontSize   = 16.sp,
            color      = Color.White.copy(alpha = 0.55f),
            textAlign  = TextAlign.Center,
            lineHeight = 24.sp,
        )

        Spacer(Modifier.height(56.dp))

        Button(
            onClick  = onNext,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape    = RoundedCornerShape(16.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = Yellow, contentColor = Navy),
        ) {
            Text("Empezar →", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
        }
    }
}

// ─── Page 1: Permissions ─────────────────────────────────────────────────────

@Composable
private fun PermissionsPageContent(
    state: PermissionsState,
    onRefresh: () -> Unit,
    onNext: () -> Unit,
) {
    val context = LocalContext.current

    val runtimeLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { onRefresh() }

    Column(
        modifier            = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .padding(bottom = 72.dp, top = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        // Header
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text("Permisos", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            Text(
                "WakeyWakey necesita estos permisos para avisarte a tiempo.",
                fontSize   = 13.sp,
                color      = Color.White.copy(alpha = 0.45f),
                textAlign  = TextAlign.Center,
                lineHeight = 18.sp,
            )
        }

        // Permission rows — compact, no scroll
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            PERM_ITEMS.forEach { item ->
                OnboardingPermRow(
                    item    = item,
                    granted = state.isGranted(item),
                    onAllow = {
                        requestPermission(
                            context         = context,
                            item            = item,
                            runtimeLauncher = { runtimeLauncher.launch(it) },
                        )
                    },
                )
            }
        }

        // Continue button
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick  = onNext,
                enabled  = state.requiredGranted,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape    = RoundedCornerShape(16.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor         = Yellow,
                    contentColor           = Navy,
                    disabledContainerColor = Yellow.copy(alpha = 0.25f),
                    disabledContentColor   = Navy.copy(alpha = 0.4f),
                ),
            ) {
                Text(
                    if (state.requiredGranted) "Continuar →" else "Concede los permisos obligatorios",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize   = 14.sp,
                )
            }
            Text(
                "Tus datos nunca salen de tu dispositivo.",
                fontSize  = 11.sp,
                color     = Color.White.copy(alpha = 0.28f),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun OnboardingPermRow(item: PermItem, granted: Boolean, onAllow: () -> Unit) {
    val alpha by animateFloatAsState(if (granted) 0.45f else 1f, label = "alpha")

    Surface(
        shape    = RoundedCornerShape(12.dp),
        color    = Color.White.copy(alpha = 0.06f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier              = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Icon
            Box(
                modifier         = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (granted) Green.copy(alpha = 0.15f) else Yellow.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text     = if (granted) "✓" else item.emoji,
                    fontSize = if (granted) 16.sp else 17.sp,
                    color    = if (granted) Green else Color.White,
                )
            }

            // Title + rationale
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                ) {
                    Text(
                        item.title,
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = Color.White.copy(alpha = alpha),
                    )
                    if (!item.required) {
                        Text(
                            "opcional",
                            fontSize = 10.sp,
                            color    = Color.White.copy(alpha = 0.35f),
                        )
                    }
                }
                Text(
                    item.rationale,
                    fontSize   = 11.sp,
                    color      = Color.White.copy(alpha = alpha * 0.55f),
                    lineHeight = 15.sp,
                )
            }

            // Allow button
            if (!granted) {
                TextButton(
                    onClick        = onAllow,
                    colors         = ButtonDefaults.textButtonColors(contentColor = Yellow),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Text("Permitir", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

// ─── Page 2: Calendar picker ─────────────────────────────────────────────────

@Composable
private fun CalendarPickerPage(
    calendars: List<DeviceCalendar>,
    loading: Boolean,
    selectedIds: Set<Long>,
    onToggle: (Long) -> Unit,
    onNext: () -> Unit,
) {
    Column(
        modifier            = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .padding(bottom = 80.dp, top = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("📅", fontSize = 44.sp)
        Spacer(Modifier.height(12.dp))
        Text(
            "Elige tus calendarios",
            fontSize   = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            color      = Color.White,
            textAlign  = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "WakeyWakey monitorizará estos calendarios\npara avisarte antes de cada evento.",
            fontSize   = 14.sp,
            color      = Color.White.copy(alpha = 0.5f),
            textAlign  = TextAlign.Center,
            lineHeight = 21.sp,
        )
        Spacer(Modifier.height(20.dp))

        // Calendar list (scrollable)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            when {
                loading -> CircularProgressIndicator(
                    modifier    = Modifier.align(Alignment.CenterHorizontally).padding(32.dp),
                    color       = Yellow,
                    strokeWidth = 2.dp,
                )
                calendars.isEmpty() -> Text(
                    "No se encontraron calendarios.",
                    color    = Color.White.copy(alpha = 0.4f),
                    fontSize = 14.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(32.dp),
                )
                else -> {
                    // Group by account
                    val grouped = calendars.groupBy { it.accountName }
                    grouped.forEach { (account, cals) ->
                        // Account header
                        Text(
                            account,
                            fontSize      = 11.sp,
                            fontWeight    = FontWeight.Bold,
                            color         = Yellow.copy(alpha = 0.7f),
                            letterSpacing = 1.sp,
                            modifier      = Modifier.padding(start = 4.dp, top = 4.dp),
                        )
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = NavySurface,
                        ) {
                            Column {
                                cals.forEachIndexed { idx, cal ->
                                    if (idx > 0) HorizontalDivider(color = Color.White.copy(alpha = 0.06f))
                                    CalendarRow(
                                        calendar   = cal,
                                        checked    = selectedIds.isEmpty() || cal.id in selectedIds,
                                        onChecked  = { onToggle(cal.id) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Text(
            if (selectedIds.isEmpty()) "Todos los calendarios activos"
            else "${selectedIds.size} calendario${if (selectedIds.size != 1) "s" else ""} seleccionado${if (selectedIds.size != 1) "s" else ""}",
            fontSize = 12.sp,
            color    = Color.White.copy(alpha = 0.35f),
        )
        Spacer(Modifier.height(8.dp))

        Button(
            onClick  = onNext,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape    = RoundedCornerShape(16.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = Yellow, contentColor = Navy),
        ) {
            Text("Continuar →", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
        }
    }
}

@Composable
private fun CalendarRow(calendar: DeviceCalendar, checked: Boolean, onChecked: () -> Unit) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Calendar colour dot
        Box(
            modifier = Modifier
                .size(14.dp)
                .clip(CircleShape)
                .background(calendarColor(calendar.color)),
        )

        Text(
            calendar.name,
            fontSize   = 14.sp,
            fontWeight = FontWeight.Medium,
            color      = Color.White,
            modifier   = Modifier.weight(1f),
        )

        Checkbox(
            checked         = checked,
            onCheckedChange = { onChecked() },
            colors          = CheckboxDefaults.colors(
                checkedColor   = Yellow,
                uncheckedColor = Color.White.copy(alpha = 0.35f),
                checkmarkColor = Navy,
            ),
        )
    }
}

/** Converts Android calendar color int (may be negative ARGB) to Compose Color. */
private fun calendarColor(androidColor: Int): Color =
    Color(androidColor or (0xFF shl 24).toInt())  // ensure full alpha

// ─── Page 3: Pro tour ────────────────────────────────────────────────────────

@Composable
private fun ProTourPage(onNext: () -> Unit) {
    val Coral  = Color(0xFFFF6B6B)

    val features = listOf(
        "⏱" to "Tiempo de aviso personalizable",
        "📅" to "Filtrado de calendarios",
        "🕐" to "Solo horario laboral",
        "🔲" to "Widget en pantalla de inicio",
        "⚡" to "Acceso rápido (Quick Settings)",
        "💤" to "Opciones de posponer personalizadas",
    )

    Column(
        modifier            = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp)
            .padding(bottom = 80.dp, top = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Hero
        Text("⭐", fontSize = 52.sp)
        Spacer(Modifier.height(12.dp))
        Text(
            "Tu prueba Pro ya está activa",
            fontSize   = 26.sp,
            fontWeight = FontWeight.ExtraBold,
            color      = Yellow,
            textAlign  = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Tienes 14 días con acceso completo\na todas las funciones. Sin tarjeta.",
            fontSize   = 14.sp,
            color      = Color.White.copy(alpha = 0.55f),
            textAlign  = TextAlign.Center,
            lineHeight = 21.sp,
        )

        Spacer(Modifier.height(24.dp))

        // Features card
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = NavySurface,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                features.forEach { (emoji, label) ->
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(
                            modifier         = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Yellow.copy(alpha = 0.10f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(emoji, fontSize = 15.sp)
                        }
                        Text(
                            label,
                            fontSize  = 14.sp,
                            color     = Color.White.copy(alpha = 0.85f),
                            modifier  = Modifier.weight(1f),
                        )
                        Text("✓", fontSize = 13.sp, color = Green, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))

        Button(
            onClick  = onNext,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape    = RoundedCornerShape(16.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = Yellow, contentColor = Navy),
        ) {
            Text("¡Genial, vamos! →", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
        }
    }
}

// ─── Page 4: Ready ───────────────────────────────────────────────────────────

@Composable
private fun ReadyPage(onComplete: () -> Unit) {
    val context = LocalContext.current

    // Auto-schedule a demo alert 20 s from now when the user reaches this page
    LaunchedEffect(Unit) {
        val now = System.currentTimeMillis()
        AndroidAlarmScheduler(context).schedule(
            CalendarEvent(
                id           = -99L,
                title        = "Reunión de prueba 🎉",
                startTime    = now + 30_000L,   // 30 s from now
                endTime      = now + 30 * 60_000L,
                location     = null,
                description  = null,
                calendarId   = -1L,
                calendarName = "Demo",
                meetingLink  = null,
                isAllDay     = false,
            ),
            minutesBefore = 0,
        )
    }

    Column(
        modifier            = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp)
            .padding(bottom = 80.dp, top = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("🎉", fontSize = 72.sp)

        Spacer(Modifier.height(24.dp))

        Text(
            "¡Todo listo!",
            fontSize   = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            color      = Yellow,
        )

        Spacer(Modifier.height(12.dp))

        Text(
            "Ahora recibirás alertas a tiempo\npara tus eventos del calendario.",
            fontSize   = 16.sp,
            color      = Color.White.copy(alpha = 0.6f),
            textAlign  = TextAlign.Center,
            lineHeight = 24.sp,
        )

        Spacer(Modifier.height(40.dp))

        // Info card — demo already scheduled automatically
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = NavySurface,
        ) {
            Row(
                modifier              = Modifier.padding(20.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment     = Alignment.Top,
            ) {
                Text("🔔", fontSize = 24.sp)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "Alerta de prueba lista",
                        fontSize   = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color      = Color.White,
                    )
                    Text(
                        "Hemos preparado una alerta de prueba que llegará en unos segundos. Bloquea la pantalla o cambia de app para verla en distintas situaciones.",
                        fontSize   = 13.sp,
                        color      = Color.White.copy(alpha = 0.55f),
                        lineHeight = 19.sp,
                    )
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        Button(
            onClick  = onComplete,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape    = RoundedCornerShape(16.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = Yellow, contentColor = Navy),
        ) {
            Text("¡Vamos allá! →", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
        }
    }
}
