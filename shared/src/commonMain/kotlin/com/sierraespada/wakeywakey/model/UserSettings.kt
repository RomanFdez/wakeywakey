package com.sierraespada.wakeywakey.model

/**
 * Preferencias del usuario. Persisten en DataStore (Android) / preferences (Desktop).
 * Los valores por defecto están pensados para ser seguros y no intrusivos.
 */
data class UserSettings(
    /** Minutos antes del evento para disparar la alerta. */
    val alertMinutesBefore: Int = 1,

    /** IDs de calendarios activos. Vacío = todos activos. */
    val enabledCalendarIds: Set<Long> = emptySet(),

    /** Sonido en la alerta. */
    val soundEnabled: Boolean = true,

    /** Vibración en la alerta. */
    val vibrationEnabled: Boolean = true,

    /** Las alertas están pausadas globalmente hasta [pausedUntil] (epoch millis).
     *  null = no pausadas. */
    val pausedUntil: Long? = null,
) {
    val isPaused: Boolean get() =
        pausedUntil != null && pausedUntil > System.currentTimeMillis()

    fun isCalendarEnabled(calendarId: Long): Boolean =
        enabledCalendarIds.isEmpty() || calendarId in enabledCalendarIds
}
