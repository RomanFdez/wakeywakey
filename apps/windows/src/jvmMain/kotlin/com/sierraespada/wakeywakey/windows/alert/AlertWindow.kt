package com.sierraespada.wakeywakey.windows.alert

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import com.sierraespada.wakeywakey.model.CalendarEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.awt.Desktop
import java.awt.GraphicsEnvironment
import java.awt.MouseInfo
import java.net.URI

/**
 * Ventana de alerta full-screen para Windows / macOS.
 *
 *  - Detecta el monitor donde está el ratón (o usa [explicitDevice] en modo all-screens).
 *  - undecorated + alwaysOnTop + tamaño = bounds del monitor → cubre toda la pantalla.
 *  - onCloseRequest vacío: la ventana NO responde a eventos de cierre del SO;
 *    solo se descarta mediante los botones de la UI (Dismiss / Join / Snooze).
 *  - El sonido en bucle se detiene automáticamente a los 30 s.
 */
@Composable
fun AlertWindow(
    event:          CalendarEvent,
    icon:           Painter,
    isPreview:      Boolean = false,
    explicitDevice: java.awt.GraphicsDevice? = null,
    onSnooze:       (Long) -> Unit,
    onDismiss:      () -> Unit,
) {
    // Usa el dispositivo explícito si se pasa (modo all-screens), si no auto-detecta
    val targetDevice = remember(explicitDevice) {
        explicitDevice ?: run {
            val pointer = runCatching { MouseInfo.getPointerInfo() }.getOrNull()
            val ge      = GraphicsEnvironment.getLocalGraphicsEnvironment()
            if (pointer != null) {
                ge.screenDevices.firstOrNull { dev ->
                    dev.defaultConfiguration.bounds.contains(pointer.location)
                } ?: ge.defaultScreenDevice
            } else {
                ge.defaultScreenDevice
            }
        }
    }

    // Bounds del monitor en coordenadas lógicas (puntos en macOS, px en Windows).
    // Los usamos para que Compose renderice el contenido al tamaño completo desde
    // el primer frame, antes de que setFullScreenWindow() tome efecto.
    val screenBounds = remember(targetDevice) { targetDevice.defaultConfiguration.bounds }

    Window(
        // onCloseRequest vacío: la alerta SOLO se cierra por acción explícita del usuario
        // (botón Dismiss, Join o Snooze). Esto evita que macOS la cierre automáticamente
        // durante transiciones de foco o events del sistema.
        onCloseRequest = {},
        state = rememberWindowState(
            placement = WindowPlacement.Floating,
            width     = screenBounds.width.dp,
            height    = screenBounds.height.dp,
            position  = WindowPosition(screenBounds.x.dp, screenBounds.y.dp),
        ),
        undecorated = true,
        alwaysOnTop = true,
        focusable   = true,
        resizable   = false,
        title       = "WakeyWakey",
        icon        = icon,
    ) {
        // Trae la ventana al frente sin usar setFullScreenWindow (API legacy que en
        // macOS moderno puede emitir eventos de cierre durante la transición exclusiva).
        DisposableEffect(Unit) {
            window.toFront()
            window.requestFocus()
            onDispose { }
        }

        // Reproduce sonido de alerta; si está en bucle, lo para a los 30 s máximo.
        // En modo preview no suena.
        DisposableEffect(Unit) {
            val s = com.sierraespada.wakeywakey.windows.settings.DesktopSettingsRepository.settings.value
            var soundJob: Job? = null
            val stopScope = CoroutineScope(Dispatchers.Default)
            if (!isPreview && s.soundEnabled) {
                soundJob = com.sierraespada.wakeywakey.windows.SoundPlayer.play(
                    soundId = s.alertSoundId,
                    volume  = s.alertVolume,
                    loop    = s.repeatSound,
                )
                if (s.repeatSound) {
                    stopScope.launch {
                        delay(30_000L)
                        soundJob?.cancel()
                    }
                }
            }
            onDispose {
                stopScope.cancel()
                soundJob?.cancel()
            }
        }

        DesktopAlertScreen(
            title     = event.title,
            startTime = event.startTime,
            location  = event.location,
            meetingUrl = event.meetingLink,
            isPreview = isPreview,
            onJoin    = {
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
