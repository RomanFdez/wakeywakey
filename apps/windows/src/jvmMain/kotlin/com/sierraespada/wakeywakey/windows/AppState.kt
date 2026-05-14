package com.sierraespada.wakeywakey.windows

import androidx.compose.runtime.*
import com.sierraespada.wakeywakey.calendar.StubCalendarRepository
import com.sierraespada.wakeywakey.model.CalendarEvent
import com.sierraespada.wakeywakey.windows.calendar.CalendarAccountManager
import com.sierraespada.wakeywakey.windows.calendar.CombinedCalendarRepository
import com.sierraespada.wakeywakey.windows.calendar.MacSystemCalendarRepository
import com.sierraespada.wakeywakey.windows.home.HomeViewModel
import com.sierraespada.wakeywakey.windows.scheduler.DesktopScheduler
import com.sierraespada.wakeywakey.windows.settings.DesktopSettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ── Modo de plataforma ────────────────────────────────────────────────────────

/**
 * Controla qué backend de calendario se usa:
 *  - WINDOWS_OAUTH → el usuario conecta Google/Microsoft (OAuth)
 *  - MAC_SYSTEM    → se usa EventKit del sistema (calendarios nativos de macOS)
 *
 * En la barra de debug puede cambiarse para probar ambos modos en cualquier SO.
 */
enum class PlatformMode { WINDOWS_OAUTH, MAC_SYSTEM }

/**
 * Estado global de la aplicación Windows.
 *
 * Gestiona:
 *  - Visibilidad de ventanas (home, settings, onboarding, trayPopup)
 *  - Alerta activa en curso (real o preview)
 *  - Pause / resume del scheduler
 *  - Lifecycle del DesktopScheduler
 *  - Repositorio de calendario activo (Google | Microsoft | Stub)
 *  - Modo de plataforma (Windows OAuth / macOS sistema) — conmutable en debug
 *
 * Se crea una sola vez en Main.kt con [remember] a nivel de application.
 */
class AppState {

    // ── Scope de la aplicación ─────────────────────────────────────────────────
    internal val appScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // ── Ventanas ──────────────────────────────────────────────────────────────
    /** Wizard de primer arranque: se muestra si nunca se ha completado el setup. */
    var showWizard      by mutableStateOf(!DesktopSettingsRepository.wizardCompleted)
    var showHome        by mutableStateOf(false)
    var showSettings    by mutableStateOf(false)
    /** Reconexión de calendario (desde Settings, no el wizard inicial). */
    var showOnboarding  by mutableStateOf(false)
    var showTrayPopup   by mutableStateOf(false)
    var trayClickX      by mutableStateOf(0)
    /** true cuando el onboarding se abrió desde Settings → al cerrar vuelve a Settings. */
    var onboardingFromSettings by mutableStateOf(false)
    /** Muestra el paywall (trial expirado o usuario lo abre manualmente). */
    var showPaywall     by mutableStateOf(false)

    // ── Cola de alertas ───────────────────────────────────────────────────────
    /**
     * Cada entrada guarda el evento y si fue abierta manualmente (preview).
     * La primera entrada es la alerta actualmente visible en pantalla.
     * Al descartar/unirse/snooze se elimina la primera y aparece la siguiente.
     * Esto permite acumular alertas perdidas mientras el equipo estaba apagado.
     */
    private data class AlertEntry(val event: CalendarEvent, val isPreview: Boolean)
    private var alertQueue by mutableStateOf(listOf<AlertEntry>())

    /** Evento de la alerta actualmente visible (nulo si no hay ninguna). */
    val pendingAlert: CalendarEvent?
        get() = alertQueue.firstOrNull()?.event

    /** true cuando la alerta activa fue abierta manualmente (preview). */
    val isPendingAlertPreview: Boolean
        get() = alertQueue.firstOrNull()?.isPreview ?: false

    /** Número de alertas acumuladas pendientes de ver (incluye la activa). */
    val pendingAlertCount: Int
        get() = alertQueue.size

    // ── Pause ─────────────────────────────────────────────────────────────────
    val isPaused: Boolean
        get() = DesktopSettingsRepository.settings.value.isPaused

    // ── Modo de plataforma ────────────────────────────────────────────────────
    var platformMode by mutableStateOf(
        if (System.getProperty("os.name").orEmpty().contains("Mac", ignoreCase = true))
            PlatformMode.MAC_SYSTEM
        else
            PlatformMode.WINDOWS_OAUTH
    )

    // ── Repositorio activo ────────────────────────────────────────────────────

    val isCalendarConnected: Boolean
        get() = CalendarAccountManager.isConnected

    private val stub          = StubCalendarRepository()
    private val macSystemRepo = MacSystemCalendarRepository()

    private val activeRepo
        get() = when (platformMode) {
            PlatformMode.MAC_SYSTEM    -> macSystemRepo
            PlatformMode.WINDOWS_OAUTH -> CalendarAccountManager.combinedRepo ?: stub
        }

    // ── Scheduler + HomeViewModel ──────────────────────────────────────────────

    val scheduler = DesktopScheduler(
        calendarRepo = activeRepo,
        onAlertFired = { event ->
            // El scheduler corre en Dispatchers.Default; las mutaciones de estado
            // Compose deben hacerse en el hilo principal (EDT en Desktop).
            appScope.launch {
                withContext(Dispatchers.Main) {
                    if (alertQueue.none { it.event.id == event.id }) {
                        // Inserta al frente: la alerta más reciente se muestra primero.
                        alertQueue = listOf(AlertEntry(event, isPreview = false)) + alertQueue
                    }
                }
            }
        },
    )

    val homeVm = HomeViewModel(
        calendarRepo = activeRepo,
    )

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    fun start() {
        // WINDOWS_OAUTH: reacciona cuando el usuario conecta/desconecta cualquier cuenta OAuth
        CalendarAccountManager.activeRepos
            .onEach { repos ->
                if (platformMode == PlatformMode.WINDOWS_OAUTH) {
                    val repo = when {
                        repos.isEmpty() -> stub
                        repos.size == 1 -> repos.first()
                        else            -> CombinedCalendarRepository(repos)
                    }
                    scheduler.updateRepo(repo)
                    homeVm.updateRepo(repo)
                    if (repos.isNotEmpty()) scheduler.syncNow()
                }
            }
            .launchIn(appScope)

        // Cambia de repo cuando el usuario alterna el modo de plataforma en debug.
        // El primer valor emitido es el modo inicial — lo saltamos para no borrar
        // los filtros de calendario guardados en cada arranque normal.
        appScope.launch {
            var isFirstEmission = true
            snapshotFlow { platformMode }
                .distinctUntilChanged()
                .collect { mode ->
                    if (isFirstEmission) { isFirstEmission = false; return@collect }
                    val repo = when (mode) {
                        PlatformMode.MAC_SYSTEM    -> macSystemRepo
                        PlatformMode.WINDOWS_OAUTH -> CalendarAccountManager.combinedRepo ?: stub
                    }
                    scheduler.updateRepo(repo)
                    homeVm.switchRepo(repo)   // limpia filtros al cambiar de modo
                    scheduler.syncNow()
                }
        }

        scheduler.start()
        homeVm.start()
    }

    fun dispose() {
        scheduler.stop()
        homeVm.dispose()
        appScope.cancel()
    }

    // ── Acciones de alerta ────────────────────────────────────────────────────

    fun dismissAlert() {
        alertQueue = alertQueue.drop(1)
    }

    fun snoozeAlert(event: CalendarEvent, delayMs: Long) {
        alertQueue = alertQueue.drop(1)
        scheduler.snooze(event, delayMs)
    }

    /**
     * Abre la ventana de alerta en modo preview para un evento concreto
     * (normalmente llamado desde el popup del tray o desde el debug bar).
     */
    fun previewAlert(event: CalendarEvent) {
        showTrayPopup = false
        // El preview se inserta al frente y no bloquea alertas reales que pueda haber
        if (alertQueue.none { it.event.id == event.id }) {
            alertQueue = listOf(AlertEntry(event, isPreview = true)) + alertQueue
        }
    }

    // ── Debug helpers ─────────────────────────────────────────────────────────

    /** Abre inmediatamente la pantalla de alerta con un evento falso. */
    fun debugPreviewAlert() {
        val now = System.currentTimeMillis()
        previewAlert(
            CalendarEvent(
                id           = 999_998L,
                title        = "Preview Meeting 👁",
                startTime    = now + 2 * 60_000L,
                endTime      = now + 32 * 60_000L,
                location     = "Room 42",
                description  = null,
                calendarId   = 0L,
                calendarName = "Debug",
                meetingLink  = "https://meet.google.com/test",
                isAllDay     = false,
            )
        )
    }

    /** Dispara la alerta con un evento falso en 5 segundos. */
    fun debugAlarmIn5s() {
        val now = System.currentTimeMillis()
        val fakeEvent = CalendarEvent(
            id           = 999_999L,
            title        = "Test Meeting ⚡",
            startTime    = now + 10_000L,          // "empieza" en 10 s
            endTime      = now + 40 * 60_000L,
            location     = null,
            description  = null,
            calendarId   = 0L,
            calendarName = "Debug",
            meetingLink  = "https://meet.google.com/test",
            isAllDay     = false,
        )
        // El state de Compose debe mutarse en el hilo principal (EDT en Desktop)
        appScope.launch {
            delay(5_000L)
            withContext(Dispatchers.Main) {
                if (alertQueue.none { it.event.id == fakeEvent.id }) {
                    alertQueue = listOf(AlertEntry(fakeEvent, isPreview = false)) + alertQueue
                }
            }
        }
    }

    // ── Acciones de UI ────────────────────────────────────────────────────────

    fun pauseOneHour() {
        val until = System.currentTimeMillis() + 60 * 60_000L
        DesktopSettingsRepository.setPausedUntil(until)
    }

    fun resume() {
        DesktopSettingsRepository.setPausedUntil(null)
        scheduler.syncNow()
    }

    /** Marca el wizard como completado, lo cierra y abre el popup del tray. */
    fun completeWizard() {
        DesktopSettingsRepository.setWizardCompleted()
        showWizard = false
        homeVm.refresh()
        // Abre el popup directamente para que el usuario sepa que la app está en el tray
        showTrayPopup = true
    }

    /** Reabre el wizard (para desarrollo/debug). */
    fun resetWizard() {
        // Solo borramos la flag en memoria; el usuario puede persistirlo desde debug
        showWizard = true
    }

    fun openHome() {
        showHome = true
    }

    fun requestCalendarConnect() {
        if (CalendarAccountManager.isConnected) {
            scheduler.syncNow()
        } else {
            showOnboarding = true
        }
    }
}
