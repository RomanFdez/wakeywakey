package com.sierraespada.wakeywakey.model

/**
 * Preferencias del usuario. Persisten en DataStore (Android) / preferences (Desktop).
 * Los valores por defecto están pensados para ser seguros y no intrusivos.
 *
 * workDays usa constantes de Calendar: 1=Dom 2=Lun 3=Mar 4=Mié 5=Jue 6=Vie 7=Sáb
 */
data class UserSettings(
    /** Minutos antes del evento para disparar la alerta. */
    val alertMinutesBefore: Int = 1,

    // ── Sound ──────────────────────────────────────────────────────────────
    val soundEnabled: Boolean = true,
    val repeatSound: Boolean  = false,
    val vibrationEnabled: Boolean = true,

    // ── Event filters ──────────────────────────────────────────────────────
    /** Show only events that have a video conference link. */
    val filterVideoOnly: Boolean    = false,
    /** Show only events the user has accepted. */
    val filterAcceptedOnly: Boolean = false,
    /** Include all-day events in the list and alerts. */
    val showAllDayEvents: Boolean   = false,

    // ── Calendars ──────────────────────────────────────────────────────────
    /** IDs de calendarios activos. Vacío = todos activos. */
    val enabledCalendarIds: Set<Long> = emptySet(),

    // ── Working hours ──────────────────────────────────────────────────────
    val workHoursEnabled: Boolean = false,
    val workHoursStart: Int = 8,
    val workHoursEnd: Int   = 20,
    val workDays: Set<Int>  = setOf(2, 3, 4, 5, 6),

    // ── Menu bar / Tray (desktop only) ────────────────────────────────────
    /** Muestra el nombre de la próxima reunión junto al icono del tray. */
    val trayShowMeetingName: Boolean = true,
    /** Muestra el tiempo restante hasta la próxima reunión junto al icono. */
    val trayShowTimeRemaining: Boolean = true,
    /** Incluye reuniones del día siguiente en el indicador de la barra. */
    val trayIncludeTomorrow: Boolean = false,
    /**
     * macOS: usa icono monocromo blanco (WW sobre fondo transparente)
     * en lugar del círculo amarillo, para encajar con los iconos del sistema.
     */
    val trayMonochromeIcon: Boolean = false,

    // ── Alert display ──────────────────────────────────────────────────────
    /** true = alerta en todas las pantallas; false = solo la pantalla activa (por defecto). */
    val alertAllScreens: Boolean = false,

    // ── Sound ──────────────────────────────────────────────────────────────
    /** ID del sonido. Ver SoundPlayer.SOUND_DEFS. */
    val alertSoundId: String = "bell",
    /** Volumen de la alerta: 0.0 (silencio) → 1.0 (máximo). */
    val alertVolume: Float = 0.8f,

    // ── Countdown display ──────────────────────────────────────────────────
    /** true = solo minutos ("5m"), false = "5m 30s" (normal por defecto). */
    val countdownMinutesOnly: Boolean = false,

    // ── Tray title ─────────────────────────────────────────────────────────
    /** Longitud máxima del título en la barra (10–50, defecto 20). */
    val trayTitleMaxLength: Int = 20,
    /** true = truncar por el medio; false = por el final (defecto true). */
    val trayTitleTruncateMiddle: Boolean = true,
    /** Color de acento del icono/texto. "system"/"red"/"yellow"/"blue"/"purple"/"green"/"orange". */
    val trayAccentColor: String = "system",

    // ── Pause ──────────────────────────────────────────────────────────────
    val pausedUntil: Long? = null,

    // ── Developer / debug ──────────────────────────────────────────────────
    /** Muestra la barra DEV en el popup del tray. Solo para desarrollo. */
    val showDevBar: Boolean = true,
) {
    val isPaused: Boolean get() =
        pausedUntil != null && pausedUntil > System.currentTimeMillis()

    fun isCalendarEnabled(calendarId: Long): Boolean =
        enabledCalendarIds.isEmpty() || calendarId in enabledCalendarIds
}
