package com.sierraespada.wakeywakey.windows.calendar

import com.sierraespada.wakeywakey.model.CalendarEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

/**
 * Almacén de eventos creados manualmente por el usuario (no del calendario).
 *
 * Persiste en ~/.wakeywakey/custom_events.tsv
 * Formato de cada línea: id\ttitle\tstartTime\tendTime\tmeetingLink
 * El campo meetingLink puede estar vacío.
 */
object CustomEventsRepository {

    private val dataFile: File by lazy {
        val dir = File(System.getProperty("user.home"), ".wakeywakey")
        dir.mkdirs()
        File(dir, "custom_events.tsv")
    }

    // ID especial para identificar eventos custom
    const val CUSTOM_CALENDAR_ID = -1L
    const val CUSTOM_CALENDAR_NAME = "My Events"

    private val _events = MutableStateFlow(load())
    val events: StateFlow<List<CalendarEvent>> = _events.asStateFlow()

    // ── CRUD ──────────────────────────────────────────────────────────────────

    fun add(event: CalendarEvent) {
        val next = _events.value.toMutableList().apply { add(event) }
        persist(next)
        _events.value = next
    }

    fun remove(id: Long) {
        val next = _events.value.filter { it.id != id }
        persist(next)
        _events.value = next
    }

    /** Devuelve los eventos futuros (fin > now), ordenados por inicio. */
    fun upcomingEvents(fromMillis: Long, toMillis: Long): List<CalendarEvent> =
        _events.value.filter { it.endTime > fromMillis && it.startTime <= toMillis }
            .sortedBy { it.startTime }

    // ── Generación de ID ──────────────────────────────────────────────────────

    fun newId(): Long {
        val existing = _events.value.map { it.id }.toSet()
        var id = System.currentTimeMillis()
        while (id in existing) id++
        return id
    }

    // ── Serialización ─────────────────────────────────────────────────────────

    private fun persist(events: List<CalendarEvent>) {
        runCatching {
            dataFile.writeText(
                events.joinToString("\n") { e ->
                    val safeTitle = e.title.replace("\t", " ").replace("\n", " ")
                    "${e.id}\t$safeTitle\t${e.startTime}\t${e.endTime}\t${e.meetingLink ?: ""}"
                }
            )
        }
    }

    private fun load(): List<CalendarEvent> {
        if (!dataFile.exists()) return emptyList()
        return runCatching {
            dataFile.readLines()
                .filter { it.isNotBlank() }
                .mapNotNull { line ->
                    val p = line.split("\t")
                    if (p.size < 4) return@mapNotNull null
                    CalendarEvent(
                        id           = p[0].toLongOrNull() ?: return@mapNotNull null,
                        title        = p[1],
                        startTime    = p[2].toLongOrNull() ?: return@mapNotNull null,
                        endTime      = p[3].toLongOrNull() ?: return@mapNotNull null,
                        meetingLink  = p.getOrNull(4)?.takeIf { it.isNotEmpty() },
                        location     = null,
                        description  = null,
                        calendarId   = CUSTOM_CALENDAR_ID,
                        calendarName = CUSTOM_CALENDAR_NAME,
                        isAllDay     = false,
                    )
                }
        }.getOrElse { emptyList() }
    }
}
