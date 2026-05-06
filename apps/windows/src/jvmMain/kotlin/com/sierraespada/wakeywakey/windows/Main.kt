package com.sierraespada.wakeywakey.windows

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import com.sierraespada.wakeywakey.windows.alert.AlertWindow
import com.sierraespada.wakeywakey.windows.calendar.OnboardingWindow
import com.sierraespada.wakeywakey.windows.calendar.SetupWizardWindow
import com.sierraespada.wakeywakey.windows.home.HomeScreen
import com.sierraespada.wakeywakey.windows.settings.DesktopSettingsRepository
import com.sierraespada.wakeywakey.windows.settings.DesktopSettingsScreen
import com.sierraespada.wakeywakey.windows.tray.TrayMenuWindow

// ─── Brand palette ────────────────────────────────────────────────────────────

val AppColorScheme = darkColorScheme(
    primary      = Color(0xFFFFE03A),
    onPrimary    = Color(0xFF1A1A2E),
    surface      = Color(0xFF16213E),
    onSurface    = Color.White,
    background   = Color(0xFF1A1A2E),
    onBackground = Color.White,
    error        = Color(0xFFFF6B6B),
)

// ─── App icon — painter vectorial puro (sin TextMeasurer) ────────────────────

/**
 * Painter resolution-independent con las letras WW más compactas y anchas,
 * proporción más cercana a una W tipográfica real.
 * Sin texto, sin fuentes, sin CompositionLocals → válido en cualquier contexto.
 */
internal val AppIcon: Painter = object : Painter() {
    override val intrinsicSize = Size.Unspecified

    override fun DrawScope.onDraw() {
        val d   = size.minDimension
        val cx  = size.width  / 2f
        val cy  = size.height / 2f
        val pad = d * 0.035f

        // ── Círculo amarillo ──────────────────────────────────────────────────
        drawCircle(
            color  = Color(0xFFFFE03A),
            radius = d / 2f - pad,
            center = Offset(cx, cy),
        )

        // ── Dos "W" — proporciones tipográficas (ancha, no alargada) ─────────
        val stroke = Stroke(
            width = d * 0.07f,          // trazo ligeramente más grueso
            cap   = StrokeCap.Round,
            join  = StrokeJoin.Round,
        )
        val navy = Color(0xFF1A1A2E)
        val wH   = d * 0.20f           // alto reducido → menos alargada
        val wW   = d * 0.26f           // ancho aumentado → más tipográfica
        val gap  = d * 0.025f          // separación mínima entre W y W

        // Anclas verticales: la W ocupa desde top hasta bot, valle en mid
        val top  = cy - wH * 0.50f
        val bot  = cy + wH * 0.50f
        val mid  = cy - wH * 0.12f    // valle bastante arriba → W menos "puntiaguda"

        // W izquierda
        val lLeft  = cx - wW - gap / 2f
        val lMidX  = lLeft + wW / 2f
        val lRight = cx - gap / 2f
        val wPath1 = Path().apply {
            moveTo(lLeft,  top)
            lineTo(lLeft  + wW * 0.28f, bot)
            lineTo(lMidX,  mid)
            lineTo(lRight - wW * 0.28f, bot)
            lineTo(lRight, top)
        }
        drawPath(wPath1, navy, style = stroke)

        // W derecha
        val rLeft  = cx + gap / 2f
        val rMidX  = rLeft + wW / 2f
        val rRight = cx + wW + gap / 2f
        val wPath2 = Path().apply {
            moveTo(rLeft,  top)
            lineTo(rLeft  + wW * 0.28f, bot)
            lineTo(rMidX,  mid)
            lineTo(rRight - wW * 0.28f, bot)
            lineTo(rRight, top)
        }
        drawPath(wPath2, navy, style = stroke)
    }
}

// rememberAppIconPainter() — mantener para compatibilidad con OnboardingWindow.
@Composable
internal fun rememberAppIconPainter(): Painter = AppIcon

// ─── Entry point ─────────────────────────────────────────────────────────────

fun main() {
    System.setProperty("skiko.renderApi", "METAL")
    System.setProperty("apple.awt.application.appearance", "system")
    System.setProperty("apple.awt.application.name", "WakeyWakey")

    application {

        val appState = remember { AppState() }

        LaunchedEffect(Unit) {
            WakeyWakeyApp.init()
            appState.start()
        }

        DisposableEffect(Unit) {
            onDispose { appState.dispose() }
        }

        // ── Tray nativo via AWT (isImageAutoSize = false → texto sin recortar) ─

        val settings    by DesktopSettingsRepository.settings.collectAsState()
        val homeUiState by appState.homeVm.uiState.collectAsState()

        // Instala el tray una sola vez y lo elimina al cerrar la app
        DisposableEffect(Unit) {
            AwtTrayManager.install(
                onTrayClicked = { clickX ->
                    appState.trayClickX = clickX
                    appState.showTrayPopup = true
                },
                onQuit = { exitApplication() },
            )
            onDispose { AwtTrayManager.remove() }
        }

        // Actualiza el contenido del tray cuando cambia cualquier estado relevante.
        // Usar homeUiState completo como clave garantiza que el rebuild se dispara
        // siempre que los eventos cambien (no sólo cuando nextEvent cambia).
        LaunchedEffect(homeUiState, settings) {
            AwtTrayManager.updateContent(settings, homeUiState)
        }

        // Actualiza el menú (Pause / Resume) cuando cambia el estado de pausa
        LaunchedEffect(appState.isPaused) {
            AwtTrayManager.updatePaused(appState.isPaused)
        }

        // ── Tray popup: popup rico con lista de reuniones ─────────────────────

        if (appState.showTrayPopup) {
            MaterialTheme(colorScheme = AppColorScheme) {
                TrayMenuWindow(
                    homeState        = homeUiState,
                    clickX           = appState.trayClickX,
                    isPaused         = appState.isPaused,
                    onPreviewEvent   = { event -> appState.previewAlert(event) },
                    onOpenSettings   = { appState.showSettings = true },
                    onPauseOneHour   = { appState.pauseOneHour() },
                    onResume         = { appState.resume() },
                    onQuit           = { exitApplication() },
                    onDismiss        = { appState.showTrayPopup = false },
                    showDevBar       = settings.showDevBar,
                    onDebugPreview   = { appState.debugPreviewAlert() },
                    onDebugAlarm5s   = { appState.debugAlarmIn5s() },
                    onResetWizard    = { appState.resetWizard() },
                    platformMode     = appState.platformMode,
                    onTogglePlatform = {
                        appState.platformMode = when (appState.platformMode) {
                            PlatformMode.WINDOWS_OAUTH -> PlatformMode.MAC_SYSTEM
                            PlatformMode.MAC_SYSTEM    -> PlatformMode.WINDOWS_OAUTH
                        }
                    },
                )
            }
        }

        // ── First-run wizard ──────────────────────────────────────────────────

        if (appState.showWizard) {
            SetupWizardWindow(
                icon                 = AppIcon,
                platformMode         = appState.platformMode,
                onPlatformModeChange = { appState.platformMode = it },
                onComplete           = { appState.completeWizard() },
            )
        }

        // ── Home window ───────────────────────────────────────────────────────

        if (appState.showHome) {
            Window(
                onCloseRequest = { appState.showHome = false },
                state          = rememberWindowState(size = DpSize(960.dp, 620.dp)),
                title          = "WakeyWakey",
                icon           = AppIcon,
            ) {
                MaterialTheme(colorScheme = AppColorScheme) {
                    HomeScreen(
                        vm                = appState.homeVm,
                        onOpenSettings    = { appState.showSettings = true },
                        onConnectCalendar = { appState.requestCalendarConnect() },
                    )
                }
            }
        }

        // ── Settings window ───────────────────────────────────────────────────

        if (appState.showSettings) {
            Window(
                onCloseRequest = { appState.showSettings = false },
                state          = rememberWindowState(size = DpSize(560.dp, 680.dp)),
                title          = "WakeyWakey — Settings",
                icon           = AppIcon,
            ) {
                MaterialTheme(colorScheme = AppColorScheme) {
                    DesktopSettingsScreen(
                        onConnectCalendar  = {
                            appState.showSettings   = false
                            appState.showOnboarding = true
                        },
                        appIcon            = AppIcon,
                        availableCalendars = homeUiState.availableCalendars,
                        platformMode       = appState.platformMode,
                    )
                }
            }
        }

        // ── Onboarding window ─────────────────────────────────────────────────

        if (appState.showOnboarding) {
            OnboardingWindow(
                onConnected = {
                    appState.showOnboarding = false
                    appState.homeVm.refresh()
                },
                onDismiss = { appState.showOnboarding = false },
            )
        }

        // ── Alert window (real o preview) ─────────────────────────────────────

        appState.pendingAlert?.let { event ->
            if (settings.alertAllScreens) {
                // Mostrar en todas las pantallas
                val devices = remember {
                    java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().screenDevices.toList()
                }
                devices.forEach { device ->
                    MaterialTheme(colorScheme = AppColorScheme) {
                        AlertWindow(
                            event          = event,
                            icon           = AppIcon,
                            isPreview      = appState.isPendingAlertPreview,
                            explicitDevice = device,
                            onSnooze       = { delayMs -> appState.snoozeAlert(event, delayMs) },
                            onDismiss      = { appState.dismissAlert() },
                        )
                    }
                }
            } else {
                MaterialTheme(colorScheme = AppColorScheme) {
                    AlertWindow(
                        event      = event,
                        icon       = AppIcon,
                        isPreview  = appState.isPendingAlertPreview,
                        onSnooze   = { delayMs -> appState.snoozeAlert(event, delayMs) },
                        onDismiss  = { appState.dismissAlert() },
                    )
                }
            }
        }

    } // end application
} // end main
