package com.sierraespada.wakeywakey.windows.tray

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import com.sierraespada.wakeywakey.windows.ui.Videocam
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.rememberWindowState
import com.sierraespada.wakeywakey.model.CalendarEvent
import com.sierraespada.wakeywakey.windows.PlatformMode
import com.sierraespada.wakeywakey.windows.calendar.CustomEventsRepository
import com.sierraespada.wakeywakey.windows.home.HomeUiState
import com.sierraespada.wakeywakey.windows.settings.DesktopSettingsRepository
import java.awt.Desktop
import java.awt.GraphicsEnvironment
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.awt.event.WindowFocusListener
import java.net.URI
import java.text.SimpleDateFormat
import java.util.*

private val Yellow      = Color(0xFFFFE03A)
private val Navy        = Color(0xFF1A1A2E)
private val NavySurface = Color(0xFF16213E)
private val Coral       = Color(0xFFFF6B6B)
private val Green       = Color(0xFF4CAF50)

private val timeFmt = SimpleDateFormat("HH:mm", Locale.ENGLISH)

/**
 * Popup del tray al estilo IYF:
 *  - Se posiciona justo debajo del icono de la barra de menú (macOS) / encima del
 *    tray (Windows)
 *  - Lista de reuniones: si están en curso + tienen enlace → icono verde, abre URL
 *  - Si la reunión aún no ha empezado → al pulsar abre AlertWindow en modo PREVIEW
 *  - Botones Settings y Quit al pie
 *  - Se cierra al perder el foco
 */
@Composable
fun TrayMenuWindow(
    homeState:        HomeUiState,
    clickX:           Int,
    isPaused:         Boolean,
    onPreviewEvent:   (CalendarEvent) -> Unit,
    onOpenSettings:   () -> Unit,
    onPauseOneHour:   () -> Unit,
    onResume:         () -> Unit,
    onQuit:           () -> Unit,
    onDismiss:        () -> Unit,
    showDevBar:       Boolean          = true,
    isPro:            Boolean          = false,
    trialDaysLeft:    Int              = 30,
    onUpgrade:        () -> Unit       = {},
    // Debug
    onDebugPreview:   () -> Unit      = {},
    onDebugAlarm5s:   () -> Unit      = {},
    onResetWizard:    () -> Unit      = {},
    platformMode:     PlatformMode    = PlatformMode.WINDOWS_OAUTH,
    onTogglePlatform: () -> Unit      = {},
) {
    val isMac = remember {
        System.getProperty("os.name").orEmpty().contains("Mac", ignoreCase = true)
    }

    // Pantalla que contiene el punto de clic
    val screenBounds = remember(clickX) {
        val ge = GraphicsEnvironment.getLocalGraphicsEnvironment()
        ge.screenDevices.firstOrNull { dev ->
            dev.defaultConfiguration.bounds.let { b ->
                clickX >= b.x && clickX <= b.x + b.width
            }
        }?.defaultConfiguration?.bounds ?: ge.defaultScreenDevice.defaultConfiguration.bounds
    }

    val popupW = 380
    val popupH = 520

    // Centra el popup bajo el icono; lo mantiene dentro de los límites de la pantalla
    val popupX = (clickX - popupW / 2)
        .coerceIn(screenBounds.x + 4, screenBounds.x + screenBounds.width - popupW - 4)
    // macOS: justo bajo la barra de menú (~24pt); Windows: sobre la taskbar (~44px)
    val popupY = if (isMac) screenBounds.y + 24
                 else screenBounds.y + screenBounds.height - popupH - 44

    var showAll       by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }

    val now      = homeState.nowMillis
    val isFree   = !isPro && trialDaysLeft <= 0
    val settings by DesktopSettingsRepository.settings.collectAsState()

    // Agrupación por fecha: hoy, mañana, día específico
    data class EventGroup(val label: String, val events: List<CalendarEvent>)

    val groupedEvents = remember(homeState.events, showAll, now) {
        val todayCal    = Calendar.getInstance()
        val tomorrowCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
        val weekLimitMs = todayCal.timeInMillis + 7L * 24 * 60 * 60 * 1000
        val dayFmt      = SimpleDateFormat("EEEE, d MMM", Locale.ENGLISH)

        fun dayKey(millis: Long): String {
            val c = Calendar.getInstance().apply { timeInMillis = millis }
            return when {
                c.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR) &&
                c.get(Calendar.DAY_OF_YEAR) == todayCal.get(Calendar.DAY_OF_YEAR) -> "Today"
                c.get(Calendar.YEAR) == tomorrowCal.get(Calendar.YEAR) &&
                c.get(Calendar.DAY_OF_YEAR) == tomorrowCal.get(Calendar.DAY_OF_YEAR) -> "Tomorrow"
                else -> dayFmt.format(Date(millis))
                    .replaceFirstChar { it.uppercase() }
            }
        }

        val filtered = homeState.events
            .distinctBy { it.id }           // evitar duplicados antes de agrupar
            .filter { it.endTime > now }
            .let { all ->
                if (showAll) all.filter { it.startTime < weekLimitMs }.take(30)
                else all.filter { dayKey(it.startTime) == "Today" }
            }

        filtered
            .groupBy { dayKey(it.startTime) }
            .map { (label, evs) -> EventGroup(label, evs) }
    }

    Window(
        onCloseRequest = onDismiss,
        state          = rememberWindowState(
            placement = WindowPlacement.Floating,
            width     = popupW.dp,
            height    = popupH.dp,
            position  = WindowPosition(popupX.dp, popupY.dp),
        ),
        undecorated    = true,
        alwaysOnTop    = true,
        focusable      = true,
        resizable      = false,
        title          = "",
    ) {
        // ── Cierre automático al perder foco ──────────────────────────────────
        //
        // Problema macOS: al hacer clic en el tray desde otra app el proceso JVM
        // NO es la app activa → windowDeactivated/windowLostFocus nunca se disparan.
        //
        // Solución en 3 pasos:
        //  1. Activar el proceso JVM con `/usr/bin/open <bundle>` (sin permisos).
        //     Una vez activo, macOS envía eventos de foco normalmente.
        //  2. Grace period amplio (1 s) para que `open` complete la activación.
        //  3. Polling de KeyboardFocusManager.activeWindow: cuando deja de ser
        //     nuestra ventana → cerrar.
        DisposableEffect(Unit) {
            // Paso 1 — activar la app JVM en macOS usando /usr/bin/open
            if (isMac) {
                try {
                    val cmd = ProcessHandle.current().info().command().orElse("")
                    val dotApp = ".app"
                    val idx = cmd.indexOf("$dotApp/Contents/")
                    val bundlePath = if (idx >= 0) cmd.substring(0, idx + dotApp.length) else null
                    if (bundlePath != null) {
                        ProcessBuilder("/usr/bin/open", bundlePath).start()
                    } else {
                        // Dev: sin bundle, intentamos requestForeground vía reflexión
                        try {
                            val cls = Class.forName("com.apple.eawt.Application")
                            val app = cls.getMethod("getApplication").invoke(null)
                            cls.getMethod("requestForeground", Boolean::class.javaPrimitiveType)
                               .invoke(app, true)
                        } catch (_: Exception) {}
                    }
                } catch (_: Exception) {}
            }
            window.toFront()
            window.requestFocus()

            var dismissed    = false
            var ready        = false
            var hadFocusOnce = false

            fun dismissOnce() { if (!dismissed) { dismissed = true; onDismiss() } }

            // Paso 2 — grace period de 1 s (open tarda ~200–400 ms en activar)
            val graceTimer = javax.swing.Timer(1000) {
                ready = true
            }.also { it.isRepeats = false; it.start() }

            // Paso 3 — polling cada 100 ms sobre activeWindow
            val pollTimer = javax.swing.Timer(100) {
                if (!ready) return@Timer
                val active = java.awt.KeyboardFocusManager
                    .getCurrentKeyboardFocusManager().activeWindow
                if (active == window) {
                    hadFocusOnce = true
                } else if (hadFocusOnce) {
                    dismissOnce()
                }
            }
            pollTimer.start()

            // Listeners de foco como respaldo inmediato (funciona si ya éramos activos)
            val adapter = object : WindowAdapter() {
                override fun windowDeactivated(e: WindowEvent) { if (ready && hadFocusOnce) dismissOnce() }
            }
            val focusListener = object : WindowFocusListener {
                override fun windowGainedFocus(e: WindowEvent) { hadFocusOnce = true }
                override fun windowLostFocus(e: WindowEvent)   { if (ready && hadFocusOnce) dismissOnce() }
            }
            window.addWindowListener(adapter)
            window.addWindowFocusListener(focusListener)

            onDispose {
                graceTimer.stop()
                pollTimer.stop()
                window.removeWindowListener(adapter)
                window.removeWindowFocusListener(focusListener)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Navy)
        ) {
            // ── Trial banner / Upgrade ────────────────────────────────────────
            if (!isPro) {
                val bannerBg    = if (trialDaysLeft <= 0) Color(0xFFFF6B6B).copy(alpha = 0.15f)
                                  else Color(0xFFFFE03A).copy(alpha = 0.10f)
                val bannerColor = if (trialDaysLeft <= 0) Color(0xFFFF6B6B) else Color(0xFFFFE03A)
                val bannerText  = if (trialDaysLeft <= 0) "Trial expired — Upgrade to Pro"
                                  else "$trialDaysLeft days left in trial"

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(bannerBg)
                        .clickable { onUpgrade(); onDismiss() }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically,
                ) {
                    Text(bannerText, color = bannerColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    Text("Upgrade →", color = bannerColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                HorizontalDivider(color = Color.White.copy(alpha = 0.06f))
            }

            // ── Pause · Settings · Quit — lo primero de todo ──────────────────
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .background(NavySurface)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                if (isPaused) {
                    FooterButton("▶  Resume", modifier = Modifier.weight(1f), color = Green) {
                        onResume(); onDismiss()
                    }
                } else {
                    FooterButton("⏸  Pause 1h", modifier = Modifier.weight(1f)) {
                        onPauseOneHour(); onDismiss()
                    }
                }
                Box(Modifier.width(1.dp).height(28.dp).align(Alignment.CenterVertically)
                    .background(Color.White.copy(alpha = 0.08f)))
                FooterButton("⚙  Settings", modifier = Modifier.weight(1f)) {
                    onOpenSettings(); onDismiss()
                }
                Box(Modifier.width(1.dp).height(28.dp).align(Alignment.CenterVertically)
                    .background(Color.White.copy(alpha = 0.08f)))
                FooterButton("✕  Quit", modifier = Modifier.weight(1f)) { onQuit() }
            }
            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

            // ── Cabecera (nombre de la app + filtros) ─────────────────────────
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .background(NavySurface)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                Text("WakeyWakey", fontWeight = FontWeight.ExtraBold, color = Yellow, fontSize = 15.sp)

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                ) {
                    // Filtro Today / All
                    FilterChip("Today", !showAll) { showAll = false }
                    FilterChip("All",    showAll) { showAll = true  }

                    // Botón añadir evento
                    Surface(
                        shape   = RoundedCornerShape(5.dp),
                        color   = Yellow.copy(alpha = 0.15f),
                        onClick = { showAddDialog = true },
                    ) {
                        Text(
                            "+",
                            fontSize   = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color      = Yellow,
                            modifier   = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                        )
                    }
                }
            }
            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

            // ── Lista de reuniones agrupada por fecha ──────────────────────────
            LazyColumn(
                modifier            = Modifier.weight(1f),
                contentPadding      = PaddingValues(vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                if (groupedEvents.isEmpty()) {
                    item {
                        Box(
                            modifier         = Modifier.fillMaxWidth().padding(32.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "No upcoming meetings",
                                color    = Color.White.copy(alpha = 0.35f),
                                fontSize = 13.sp,
                            )
                        }
                    }
                } else {
                    // En Free tier: el calendario activo lo elige el usuario en Settings.
                    // Si enabledCalendarIds está vacío (todos activos), usamos el mismo
                    // fallback que la pantalla de Settings: primer cal. ordenado por cuenta/nombre.
                    val freeCalId = if (isFree) {
                        if (settings.enabledCalendarIds.isNotEmpty()) {
                            settings.enabledCalendarIds.first()
                        } else {
                            homeState.allCalendars
                                .sortedWith(compareBy({ it.accountName }, { it.name }))
                                .firstOrNull()?.id
                                ?: homeState.events.firstOrNull()?.calendarId
                        }
                    } else null
                    val freeEvents = if (isFree)
                        homeState.events
                            .filter { ev -> freeCalId == null || ev.calendarId == freeCalId }
                            .take(com.sierraespada.wakeywakey.windows.billing.DesktopEntitlementManager.FREE_TIER_MAX_DAILY_ALERTS)
                            .map { it.id }.toSet()
                    else null  // null = todos disponibles

                    groupedEvents.forEach { group ->
                        item(key = "header_${group.label}") {
                            Text(
                                text     = group.label,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color    = Color.White.copy(alpha = 0.35f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 14.dp, top = 10.dp, bottom = 2.dp),
                                letterSpacing = 0.5.sp,
                            )
                        }
                        // Separa disponibles vs bloqueados dentro del grupo
                        val (available, locked) = group.events.partition { ev ->
                            freeEvents == null || ev.id in freeEvents
                        }

                        items(available, key = { "${group.label}_${it.id}" }) { event ->
                            TrayEventRow(
                                event     = event,
                                nowMillis = now,
                                isCustom  = event.calendarId == CustomEventsRepository.CUSTOM_CALENDAR_ID,
                                onClick   = {
                                    val isOngoing = event.startTime <= now && event.endTime > now
                                    if (isOngoing && event.meetingLink != null) {
                                        runCatching { Desktop.getDesktop().browse(URI(event.meetingLink)) }
                                        onDismiss()
                                    } else {
                                        onPreviewEvent(event)
                                    }
                                },
                                onDelete  = { CustomEventsRepository.remove(event.id) },
                            )
                        }

                        // Banner de upgrade + eventos bloqueados
                        if (locked.isNotEmpty()) {
                            item(key = "upgrade_banner_${group.label}") {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onUpgrade(); onDismiss() }
                                        .background(Color(0xFFFFE03A).copy(alpha = 0.07f))
                                        .padding(horizontal = 14.dp, vertical = 6.dp),
                                    verticalAlignment     = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(
                                        "🔒 ${locked.size} more event${if (locked.size > 1) "s" else ""} — Upgrade to Pro",
                                        color    = Color(0xFFFFE03A).copy(alpha = 0.7f),
                                        fontSize = 11.sp,
                                    )
                                    Text(
                                        "Upgrade →",
                                        color      = Color(0xFFFFE03A),
                                        fontSize   = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                            items(locked, key = { "locked_${group.label}_${it.id}" }) { event ->
                                TrayEventRow(
                                    event     = event,
                                    nowMillis = now,
                                    isCustom  = false,
                                    isLocked  = true,
                                    onClick   = { onUpgrade(); onDismiss() },
                                    onDelete  = {},
                                )
                            }
                        }
                    }
                }
            }

            // ── Barra DEV — solo en builds de desarrollo (IS_RELEASE = false) ──
            if (!com.sierraespada.wakeywakey.windows.AppBuildConfig.IS_RELEASE && showDevBar) Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.025f))
                    .padding(horizontal = 10.dp, vertical = 2.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                // Grupo izquierdo: label + botones debug + simuladores de tier
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "DEV",
                        fontSize      = 8.sp,
                        fontWeight    = FontWeight.Bold,
                        color         = Yellow.copy(alpha = 0.35f),
                        letterSpacing = 1.sp,
                        modifier      = Modifier.padding(end = 4.dp),
                    )
                    // 👁  Preview alerta inmediata
                    DebugTrayButton("👁") { onDebugPreview(); onDismiss() }
                    // ⚡  Alarma en 5 s
                    DebugTrayButton("⚡") { onDebugAlarm5s(); onDismiss() }
                    // 🔄  Reset wizard
                    DebugTrayButton("🔄") { onResetWizard(); onDismiss() }

                    Spacer(Modifier.width(4.dp))
                    Box(Modifier.width(1.dp).height(16.dp).background(Color.White.copy(alpha = 0.12f)))
                    Spacer(Modifier.width(4.dp))

                    // Simuladores de tier — revocar licencia antes de cambiar a Trial/Free
                    val em = com.sierraespada.wakeywakey.windows.billing.DesktopEntitlementManager
                    DebugTierButton("⏳ Trial") {
                        em.debugRevokeLicense(); em.debugSetElapsedDays(0)
                    }
                    DebugTierButton("🔒 Free") {
                        em.debugRevokeLicense(); em.debugSetElapsedDays(31)
                    }
                    DebugTierButton("⭐ Pro") {
                        em.activateLicense("DEBUG-LICENSE-KEY-PRO")
                    }
                }

                // Toggle Mac ↔ Win — siempre visible en el extremo derecho
                val isMacMode = platformMode == PlatformMode.MAC_SYSTEM
                Surface(
                    shape    = RoundedCornerShape(4.dp),
                    color    = if (isMacMode) Color(0xFF0A84FF).copy(alpha = 0.15f)
                               else Yellow.copy(alpha = 0.10f),
                    modifier = Modifier.clickable { onTogglePlatform() },
                ) {
                    Text(
                        if (isMacMode) "🍎 macOS" else "🪟 Win",
                        fontSize   = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color      = if (isMacMode) Color(0xFF0A84FF) else Yellow,
                        modifier   = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
            }
            if (!com.sierraespada.wakeywakey.windows.AppBuildConfig.IS_RELEASE && showDevBar) HorizontalDivider(color = Color.White.copy(alpha = 0.06f))
            // ── END DEV ────────────────────────────────────────────────────────

            // ── Dialog añadir evento ──────────────────────────────────────────
            if (showAddDialog) {
                AddEventDialog(onDismiss = { showAddDialog = false })
            }
        }
    }
}

// ── Fila de evento ────────────────────────────────────────────────────────────

@Composable
private fun TrayEventRow(
    event:    CalendarEvent,
    nowMillis: Long,
    isCustom:  Boolean = false,
    isLocked:  Boolean = false,
    onClick:   () -> Unit,
    onDelete:  () -> Unit = {},
) {
    val isOngoing   = event.startTime <= nowMillis && event.endTime > nowMillis
    val minsLeft    = ((event.startTime - nowMillis) / 60_000L).toInt()
    val hasVideo    = event.meetingLink != null

    // Locked rows are dimmed — override all colors
    val dimAlpha = if (isLocked) 0.3f else 1f

    // Color de estado (usado en texto de hora y fondo de fila)
    val statusColor = when {
        isLocked              -> Color.White.copy(alpha = 0.25f)
        isOngoing && hasVideo -> Green
        isOngoing             -> Yellow.copy(alpha = 0.6f)
        minsLeft in 0..10     -> Coral
        else                  -> Yellow.copy(alpha = 0.45f)
    }
    // La franja lateral usa el color del calendario; si no hay, usa el de estado
    val barColor = if (isLocked) {
        Color.White.copy(alpha = 0.12f)
    } else {
        event.calendarColor?.let { argb ->
            Color(
                red   = ((argb shr 16) and 0xFF) / 255f,
                green = ((argb shr  8) and 0xFF) / 255f,
                blue  = ( argb        and 0xFF) / 255f,
                alpha = 1f,
            )
        } ?: statusColor
    }
    val accentColor = statusColor

    val rowBg = when {
        isLocked              -> Color.Transparent
        isOngoing && hasVideo -> Green.copy(alpha = 0.07f)
        isOngoing             -> Yellow.copy(alpha = 0.05f)
        else                  -> Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(rowBg)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Franja de color lateral — color del calendario
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(36.dp)
                .background(barColor, RoundedCornerShape(2.dp))
        )

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            // Línea 1: indicador de recurrencia + título
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (event.isRecurring && !isLocked) {
                    Text(
                        "↻",
                        fontSize = 11.sp,
                        color    = Color.White.copy(alpha = 0.55f),
                    )
                }
                if (isLocked) {
                    Text(
                        "🔒",
                        fontSize = 11.sp,
                        modifier = Modifier.padding(end = 2.dp),
                    )
                }
                Text(
                    text       = event.title,
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = Color.White.copy(alpha = dimAlpha),
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis,
                    modifier   = Modifier.weight(1f, fill = false),
                )
            }
            // Línea 2: hora inicio–fin
            Text(
                text     = buildTimeLabel(event, nowMillis, isOngoing, minsLeft),
                fontSize = 11.sp,
                color    = accentColor,
            )
        }

        // Icono cámara alineado al margen derecho
        if (hasVideo && !isLocked) {
            Icon(
                imageVector        = Icons.Filled.Videocam,
                contentDescription = "Video meeting",
                tint               = if (isOngoing) Green else Yellow.copy(alpha = 0.6f),
                modifier           = Modifier.size(20.dp),
            )
        }

        // Botón eliminar solo para eventos propios
        if (isCustom && !isLocked) {
            Box(
                modifier         = Modifier
                    .size(22.dp)
                    .clickable { onDelete() },
                contentAlignment = Alignment.Center,
            ) {
                Text("✕", fontSize = 11.sp, color = Color.White.copy(alpha = 0.35f))
            }
        }
    }
}

private fun buildTimeLabel(
    event:     CalendarEvent,
    nowMillis: Long,
    isOngoing: Boolean,
    minsLeft:  Int,
): String {
    val startStr = timeFmt.format(Date(event.startTime))
    val endStr   = timeFmt.format(Date(event.endTime))
    val range    = "$startStr–$endStr"
    return when {
        isOngoing     -> "● En curso · $range"
        minsLeft <= 0 -> "Ahora · $range"
        minsLeft < 60 -> "En ${minsLeft}m · $range"
        else          -> range
    }
}

// ── Componentes pequeños ──────────────────────────────────────────────────────

@Composable
private fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    androidx.compose.material3.Surface(
        shape    = RoundedCornerShape(5.dp),
        color    = if (selected) Yellow.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.05f),
        modifier = Modifier.clickable { onClick() },
    ) {
        Text(
            text     = label,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color    = if (selected) Yellow else Color.White.copy(alpha = 0.45f),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun FooterButton(
    label:   String,
    modifier: Modifier = Modifier,
    color:   Color = Color.White.copy(alpha = 0.55f),
    onClick: () -> Unit,
) {
    Box(
        modifier         = modifier
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = label, fontSize = 12.sp, color = color)
    }
}

@Composable
private fun DebugTrayButton(emoji: String, onClick: () -> Unit) {
    Box(
        modifier         = Modifier
            .height(28.dp)
            .clickable { onClick() }
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(emoji, fontSize = 15.sp)
    }
}

@Composable
private fun DebugTierButton(label: String, onClick: () -> Unit) {
    Box(
        modifier         = Modifier
            .height(24.dp)
            .clickable { onClick() }
            .padding(horizontal = 5.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, fontSize = 9.sp, color = Color.White.copy(alpha = 0.5f), fontWeight = FontWeight.SemiBold)
    }
}

