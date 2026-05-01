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
import com.sierraespada.wakeywakey.windows.home.HomeScreen
import com.sierraespada.wakeywakey.windows.settings.DesktopSettingsScreen

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
 * Painter resolution-independent que dibuja el icono de WakeyWakey con
 * Compose Canvas paths — sin texto, sin fuentes, sin CompositionLocals.
 *
 * Funciona en CUALQUIER contexto: Tray, Window, AlertWindow, etc.
 * La forma "WW" se dibuja como dos W geométricas mediante trazados Bézier.
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

        // ── Dos "W" como paths (navy) ─────────────────────────────────────────
        // Cada W tiene 5 puntos: top-left, bottom-left, middle-top, bottom-right, top-right
        val stroke = Stroke(
            width = d * 0.065f,
            cap   = StrokeCap.Round,
            join  = StrokeJoin.Round,
        )
        val navy = Color(0xFF1A1A2E)
        val wH   = d * 0.28f   // alto de la W
        val wW   = d * 0.24f   // ancho de cada W
        val gap  = d * 0.03f   // separación entre las dos W
        val top  = cy - wH * 0.42f
        val bot  = cy + wH * 0.58f
        val mid  = cy + wH * 0.05f  // vértice central (un poco por encima del centro)

        // W izquierda
        val lLeft  = cx - wW - gap / 2f
        val lMidX  = lLeft + wW / 2f
        val lRight = cx - gap / 2f
        val wPath1 = Path().apply {
            moveTo(lLeft,  top); lineTo(lLeft  + wW * 0.25f, bot)
            lineTo(lMidX,  mid); lineTo(lRight - wW * 0.25f, bot)
            lineTo(lRight, top)
        }
        drawPath(wPath1, navy, style = stroke)

        // W derecha
        val rLeft  = cx + gap / 2f
        val rMidX  = rLeft + wW / 2f
        val rRight = cx + wW + gap / 2f
        val wPath2 = Path().apply {
            moveTo(rLeft,  top); lineTo(rLeft  + wW * 0.25f, bot)
            lineTo(rMidX,  mid); lineTo(rRight - wW * 0.25f, bot)
            lineTo(rRight, top)
        }
        drawPath(wPath2, navy, style = stroke)
    }
}

// rememberAppIconPainter() devuelve el singleton vectorial.
// Mantener la función para compatibilidad con OnboardingWindow que la llama.
@Composable
internal fun rememberAppIconPainter(): Painter = AppIcon

// ─── Entry point ─────────────────────────────────────────────────────────────

fun main() {
    // Metal → renderizado GPU nativo en macOS (mejor subpixel AA que OpenGL)
    System.setProperty("skiko.renderApi", "METAL")
    // Integración con el tema del sistema (dark/light mode de macOS)
    System.setProperty("apple.awt.application.appearance", "system")
    // Nombre de la app en la barra de menú de macOS
    System.setProperty("apple.awt.application.name", "WakeyWakey")

    application {

        val appState = remember { AppState() }
        val appIcon  = AppIcon   // singleton vectorial, sin CompositionLocals

        LaunchedEffect(Unit) {
            WakeyWakeyApp.init()
            appState.start()
        }

        DisposableEffect(Unit) {
            onDispose { appState.dispose() }
        }

        // ── System Tray ───────────────────────────────────────────────────────

        Tray(
            state    = rememberTrayState(),
            icon     = appIcon,
            tooltip  = "WakeyWakey",
            onAction = { appState.openHome() },
            menu     = {
                Item("Open WakeyWakey") { appState.openHome() }
                if (appState.isPaused) {
                    Item("▶  Resume alerts") { appState.resume() }
                } else {
                    Item("⏸  Pause 1 hour") { appState.pauseOneHour() }
                }
                Separator()
                Item("Quit") { exitApplication() }
            },
        )

        // ── Home window ───────────────────────────────────────────────────────

        if (appState.showHome) {
            Window(
                onCloseRequest = { appState.showHome = false },
                state          = rememberWindowState(size = DpSize(960.dp, 620.dp)),
                title          = "WakeyWakey",
                icon           = appIcon,
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
                state          = rememberWindowState(size = DpSize(520.dp, 560.dp)),
                title          = "WakeyWakey — Settings",
                icon           = appIcon,
            ) {
                MaterialTheme(colorScheme = AppColorScheme) {
                    DesktopSettingsScreen(
                        onConnectCalendar = {
                            appState.showSettings   = false
                            appState.showOnboarding = true
                        },
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

        // ── Alert window ──────────────────────────────────────────────────────

        appState.pendingAlert?.let { event ->
            MaterialTheme(colorScheme = AppColorScheme) {
                AlertWindow(
                    event     = event,
                    icon      = appIcon,
                    onSnooze  = { delayMs -> appState.snoozeAlert(event, delayMs) },
                    onDismiss = { appState.dismissAlert() },
                )
            }
        }

    } // end application
} // end main
