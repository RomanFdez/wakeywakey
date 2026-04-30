package com.sierraespada.wakeywakey.windows

import androidx.compose.runtime.*
import com.sierraespada.wakeywakey.calendar.StubCalendarRepository
import com.sierraespada.wakeywakey.model.CalendarEvent
import com.sierraespada.wakeywakey.windows.calendar.CalendarAccountManager
import com.sierraespada.wakeywakey.windows.home.HomeViewModel
import com.sierraespada.wakeywakey.windows.scheduler.DesktopScheduler
import com.sierraespada.wakeywakey.windows.settings.DesktopSettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * Estado global de la aplicación Windows.
 *
 * Gestiona:
 *  - Visibilidad de ventanas (home, settings, onboarding)
 *  - Alerta activa en curso
 *  - Pause / resume del scheduler
 *  - Lifecycle del DesktopScheduler
 *  - Repositorio de calendario activo (Google | Microsoft | Stub)
 *
 * Se crea una sola vez en Main.kt con [remember] a nivel de application.
 */
class AppState {

    // ── Scope de la aplicación ─────────────────────────────────────────────────
    private val appScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // ── Ventanas ──────────────────────────────────────────────────────────────
    var showHome        by mutableStateOf(true)
    var showSettings    by mutableStateOf(false)
    var showOnboarding  by mutableStateOf(false)

    // ── Alerta activa ─────────────────────────────────────────────────────────
    var pendingAlert by mutableStateOf<CalendarEvent?>(null)
        private set

    // ── Pause ─────────────────────────────────────────────────────────────────
    val isPaused: Boolean
        get() = DesktopSettingsRepository.settings.value.isPaused

    // ── Repositorio activo ────────────────────────────────────────────────────

    val isCalendarConnected: Boolean
        get() = CalendarAccountManager.isConnected

    // Usa el repo activo si está disponible, si no el stub (evita null-safety en todo el código)
    private val stub = StubCalendarRepository()

    private val activeRepo
        get() = CalendarAccountManager.activeRepo.value ?: stub

    // ── Scheduler + HomeViewModel ──────────────────────────────────────────────

    val scheduler = DesktopScheduler(
        calendarRepo = activeRepo,
        onAlertFired = { event -> pendingAlert = event },
    )

    val homeVm = HomeViewModel(
        calendarRepo = activeRepo,
    )

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    fun start() {
        // Cuando el usuario conecta/desconecta un calendario, refresca el scheduler
        CalendarAccountManager.activeRepo
            .onEach { repo ->
                if (repo != null) {
                    scheduler.updateRepo(repo)
                    homeVm.updateRepo(repo)
                    scheduler.syncNow()
                }
            }
            .launchIn(appScope)

        scheduler.start()
        homeVm.start()
    }

    fun dispose() {
        scheduler.stop()
        homeVm.dispose()
        appScope.cancel()
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

    /** Llama a onboarding si no hay cuenta, refresca si ya hay. */
    fun requestCalendarConnect() {
        if (CalendarAccountManager.isConnected) {
            scheduler.syncNow()
        } else {
            showOnboarding = true
        }
    }
}
