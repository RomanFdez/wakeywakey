package com.sierraespada.wakeywakey.windows.alert

import androidx.compose.runtime.Composable
import androidx.compose.ui.window.*
import com.sierraespada.wakeywakey.model.CalendarEvent
import java.awt.Desktop
import java.net.URI

/**
 * Ventana de alerta full-screen para Windows.
 *
 * Características:
 *  - `alwaysOnTop = true`  — aparece sobre cualquier otra ventana
 *  - `undecorated = true`  — sin barra de título del SO
 *  - `WindowPlacement.Fullscreen` — cubre toda la pantalla (igual que "In Your Face")
 *  - `focusable = true`    — captura el foco al aparecer
 *
 * Se lanza desde AppState.pendingAlert y se cierra al hacer dismiss/snooze.
 */
@Composable
fun AlertWindow(
    event:    CalendarEvent,
    onSnooze: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    Window(
        onCloseRequest = onDismiss,
        state          = rememberWindowState(placement = WindowPlacement.Fullscreen),
        undecorated    = true,
        alwaysOnTop    = true,
        focusable      = true,
        resizable      = false,
        title          = "WakeyWakey",
        icon           = AppIcon,
    ) {
        DesktopAlertScreen(
            title      = event.title,
            startTime  = event.startTime,
            location   = event.location,
            meetingUrl = event.meetingLink,
            onJoin     = {
                event.meetingLink?.let {
                    runCatching { Desktop.getDesktop().browse(URI(it)) }
                }
                onDismiss()
            },
            onSnooze   = onSnooze,
            onDismiss  = onDismiss,
        )
    }
}
