package com.sierraespada.wakeywakey.windows

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import com.sierraespada.wakeywakey.windows.alert.AlertWindow
import com.sierraespada.wakeywakey.windows.calendar.OnboardingWindow
import com.sierraespada.wakeywakey.windows.home.HomeScreen
import com.sierraespada.wakeywakey.windows.settings.DesktopSettingsScreen
import java.awt.Font
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.awt.Color as AwtColor

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

// ─── App icon ─────────────────────────────────────────────────────────────────

/**
 * Icono 256×256 generado programáticamente.
 * Reemplazar por un .ico real antes del release.
 */
internal val AppIcon: BitmapPainter by lazy {
    val size = 256
    val img  = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
    val g    = img.createGraphics()
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    // Fondo amarillo
    g.color = AwtColor(0xFF, 0xE0, 0x3A)
    g.fillOval(4, 4, size - 8, size - 8)
    // Texto "WW" en navy
    g.color = AwtColor(0x1A, 0x1A, 0x2E)
    g.font  = Font(Font.SANS_SERIF, Font.BOLD, 88)
    val fm  = g.fontMetrics
    val lbl = "WW"
    g.drawString(lbl, (size - fm.stringWidth(lbl)) / 2, size / 2 + fm.ascent / 3)
    g.dispose()
    BitmapPainter(img.toComposeImageBitmap())
}

// ─── Entry point ─────────────────────────────────────────────────────────────

fun main() = application {

    val appState = remember { AppState() }

    LaunchedEffect(Unit) {
        WakeyWakeyApp.init()
        appState.start()
    }

    DisposableEffect(Unit) {
        onDispose { appState.dispose() }
    }

    // ── System Tray ───────────────────────────────────────────────────────────

    Tray(
        state    = rememberTrayState(),
        icon     = AppIcon,
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

    // ── Home window ───────────────────────────────────────────────────────────

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

    // ── Settings window ───────────────────────────────────────────────────────

    if (appState.showSettings) {
        Window(
            onCloseRequest = { appState.showSettings = false },
            state          = rememberWindowState(size = DpSize(520.dp, 560.dp)),
            title          = "WakeyWakey — Settings",
            icon           = AppIcon,
        ) {
            MaterialTheme(colorScheme = AppColorScheme) {
                DesktopSettingsScreen(
                    onConnectCalendar = {
                        appState.showSettings  = false
                        appState.showOnboarding = true
                    },
                )
            }
        }
    }

    // ── Onboarding window ─────────────────────────────────────────────────────

    if (appState.showOnboarding) {
        OnboardingWindow(
            onConnected = {
                appState.showOnboarding = false
                appState.homeVm.refresh()
            },
            onDismiss = { appState.showOnboarding = false },
        )
    }

    // ── Alert window ──────────────────────────────────────────────────────────

    appState.pendingAlert?.let { event ->
        MaterialTheme(colorScheme = AppColorScheme) {
            AlertWindow(
                event     = event,
                onSnooze  = { delayMs -> appState.snoozeAlert(event, delayMs) },
                onDismiss = { appState.dismissAlert() },
            )
        }
    }
}
