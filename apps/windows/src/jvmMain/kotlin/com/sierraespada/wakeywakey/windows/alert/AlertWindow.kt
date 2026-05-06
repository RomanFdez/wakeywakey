package com.sierraespada.wakeywakey.windows.alert

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import com.sierraespada.wakeywakey.model.CalendarEvent
import java.awt.Desktop
import java.awt.GraphicsEnvironment
import java.awt.MouseInfo
import java.net.URI
import javax.swing.SwingUtilities

/**
 * Ventana de alerta full-screen para Windows / macOS.
 *
 * Estrategia de fullscreen:
 *  1. Detecta el monitor donde está el ratón.
 *  2. Crea una ventana undecorated + alwaysOnTop en ese monitor.
 *  3. Llama a GraphicsDevice.setFullScreenWindow() para pasar a modo exclusivo
 *     → en macOS cubre la menu bar y el dock (igual que IYF).
 *     → en Windows cubre la taskbar.
 *  4. Al cerrar, restaura setFullScreenWindow(null) para que el sistema
 *     vuelva al estado normal.
 *
 * El [icon] se pasa desde Main.kt para reutilizar el painter vectorial
 * creado en el contexto @Composable de application {}.
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
        onCloseRequest = onDismiss,
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
        // Entra en fullscreen exclusivo sobre el monitor correcto (EDT obligatorio).
        // Esto cubre la menu bar en macOS y la taskbar en Windows.
        DisposableEffect(targetDevice) {
            SwingUtilities.invokeLater {
                targetDevice.setFullScreenWindow(window)
                window.toFront()
                window.requestFocus()
            }
            onDispose {
                SwingUtilities.invokeLater {
                    if (targetDevice.fullScreenWindow === window) {
                        targetDevice.setFullScreenWindow(null)
                    }
                }
            }
        }

        // Reproduce sonido de alerta
        DisposableEffect(Unit) {
            val s = com.sierraespada.wakeywakey.windows.settings.DesktopSettingsRepository.settings.value
            val soundJob = if (s.soundEnabled) {
                com.sierraespada.wakeywakey.windows.SoundPlayer.play(
                    soundId = s.alertSoundId,
                    volume  = s.alertVolume,
                    loop    = s.repeatSound,
                )
            } else null
            onDispose { soundJob?.cancel() }
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
