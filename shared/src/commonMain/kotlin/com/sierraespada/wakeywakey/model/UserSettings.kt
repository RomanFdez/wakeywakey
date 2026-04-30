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

    // ── Pause ──────────────────────────────────────────────────────────────
    val pausedUntil: Long? = null,
) {
    val isPaused: Boolean get() =
        pausedUntil != null && pausedUntil > System.currentTimeMillis()

    fun isCalendarEnabled(calendarId: Long): Boolean =
        enabledCalendarIds.isEmpty() || calendarId in enabledCalendarIds
}
