package com.sierraespada.wakeywakey.windows

import androidx.compose.runtime.*
import com.sierraespada.wakeywakey.calendar.StubCalendarRepository
import com.sierraespada.wakeywakey.model.CalendarEvent
import com.sierraespada.wakeywakey.windows.home.HomeViewModel
import com.sierraespada.wakeywakey.windows.scheduler.DesktopScheduler
import com.sierraespada.wakeywakey.windows.settings.DesktopSettingsRepository

/**
 * Estado global de la aplicación Windows.
 *
 * Gestiona:
 *  - Visibilidad de ventanas (home, settings, onboarding)
 *  - Alerta activa en curso
 *  - Pause / resume del scheduler
 *  - Lifecycle del DesktopScheduler
 *
 * Se crea una sola vez en Main.kt con [remember] a nivel de application.
 */
class AppState {

    // ── Ventanas ──────────────────────────────────────────────────────────────
    var showHome     by mutableStateOf(true)
    var showSettings by mutableStateOf(false)

    // ── Alerta activa ─────────────────────────────────────────────────────────
    var pendingAlert by mutableStateOf<CalendarEvent?>(null)
        private set

    // ── Pause ─────────────────────────────────────────────────────────────────
    val isPaused: Boolean
        get() = DesktopSettingsRepository.settings.value.isPaused

    // ── Dependencias ──────────────────────────────────────────────────────────

    // Slice 5.2: reemplazar StubCalendarRepository por Google/MicrosoftCalendarRepository
    val calendarRepo = StubCalendarRepository()

    val scheduler = DesktopScheduler(
        calendarRepo = calendarRepo,
        onAlertFired = { event ->
            // Corre en Dispatchers.Default — pendingAlert debe setearse en el hilo principal
            pendingAlert = event
        },
    )

    val homeVm = HomeViewModel(
        calendarRepo = calendarRepo,
    )

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    fun start() {
        scheduler.start()
        homeVm.start()
    }

    fun dispose() {
        scheduler.stop()
        homeVm.dispose()
    }

    // ── Acciones ──────────────────────────────────────────────────────────────

    fun dismissAlert() {
        pendingAlert = null
    }

    fun snoozeAlert(event: CalendarEvent, delayMs: Long) {
        pendingAlert = null
        scheduler.snooze(event, delayMs)
    }

    fun pauseOneHour() {
        val until = System.currentTimeMillis() + 60 * 60_000L
        DesktopSettingsRepository.setPausedUntil(until)
    }

    fun resume() {
        DesktopSettingsRepository.setPausedUntil(null)
        scheduler.syncNow()
    }

    fun openHome() {
        showHome = true
    }
}
