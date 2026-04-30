package com.sierraespada.wakeywakey.model

/**
 * Evento de calendario normalizado. Agnóstico de plataforma.
 * Los datos crudos vienen de Calendar Provider (Android) o de las APIs de
 * calendario correspondientes en Windows / iOS.
 */
data class CalendarEvent(
    val id: Long,
    val title: String,
    val startTime: Long,        // epoch millis UTC
    val endTime: Long,          // epoch millis UTC
    val location: String?,
    val description: String?,
    val calendarId: Long,
    val calendarName: String,
    val meetingLink: String?,   // extraído de description/location por MeetingLinkDetector
    val isAllDay: Boolean,
    /**
     * Estado de asistencia del propio usuario.
     * Valores: 0=NONE, 1=ACCEPTED, 2=DECLINED, 3=INVITED, 4=TENTATIVE
     * (CalendarContract.Attendees.ATTENDEE_STATUS_*)
     */
    val selfAttendeeStatus: Int = 0,
) {
    /** Minutos que faltan para que empiece el evento. Negativo si ya empezó. */
    fun minutesUntilStart(now: Long = currentTimeMillis()): Long =
        (startTime - now) / 60_000L
}

/** Expect para obtener el tiempo actual — permite mockear en tests. */
expect fun currentTimeMillis(): Long
