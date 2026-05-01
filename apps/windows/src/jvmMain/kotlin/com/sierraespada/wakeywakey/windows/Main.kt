package com.sierraespada.wakeywakey.windows

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

// ─── App icon — painter vectorial (resolución independiente) ──────────────────

/**
 * Crea un [Painter] que dibuja el icono de WakeyWakey usando Compose Canvas.
 * Al ser vectorial se renderiza nítido a cualquier escala y densidad de píxeles
 * (Retina 2×, 3×, monitores 4K, tray de macOS, barra de título de Windows, etc.)
 * sin ningún upscaling ni downscaling de bitmap.
 *
 * Debe llamarse dentro de un contexto @Composable porque necesita [rememberTextMeasurer].
 */
@Composable
internal fun rememberAppIconPainter(): Painter {
    val textMeasurer = rememberTextMeasurer()
    return remember(textMeasurer) {
        object : Painter() {
            // Sin tamaño intrínseco → el caller decide el tamaño (tray, ventana, etc.)
            override val intrinsicSize = Size.Unspecified

            override fun DrawScope.onDraw() {
                val d   = size.minDimension
                val pad = d * 0.03f

                // ── Círculo amarillo ──────────────────────────────────────────
                drawCircle(
                    color  = Color(0xFFFFE03A),
                    radius = d / 2f - pad,
                    center = Offset(size.width / 2f, size.height / 2f),
                )

                // ── "WW" en navy — tamaño proporcional al icono ───────────────
                // toSp() usa la densidad del DrawScope (que implementa Density)
                val fontSizeSp = (d * 0.33f).toSp()
                val style = TextStyle(
                    color         = Color(0xFF1A1A2E),
                    fontWeight    = FontWeight.ExtraBold,
                    fontSize      = fontSizeSp,
                    letterSpacing = 0.sp,
                )
                val measured = textMeasurer.measure("WW", style)
                drawText(
                    textMeasurer = textMeasurer,
                    text         = "WW",
                    style        = style,
                    topLeft      = Offset(
                        x = (size.width  - measured.size.width)  / 2f,
                        y = (size.height - measured.size.height) / 2f,
                    ),
                )
            }
        }
    }
}

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
        // Icono vectorial creado aquí (contexto @Composable) y compartido por todas las ventanas
        val appIcon  = rememberAppIconPainter()

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
