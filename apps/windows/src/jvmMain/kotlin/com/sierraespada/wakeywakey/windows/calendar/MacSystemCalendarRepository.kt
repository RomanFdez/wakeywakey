package com.sierraespada.wakeywakey.windows.calendar

import com.sierraespada.wakeywakey.calendar.CalendarRepository
import com.sierraespada.wakeywakey.model.CalendarEvent
import com.sierraespada.wakeywakey.model.DeviceCalendar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Implementación de [CalendarRepository] para macOS.
 *
 * Usa un binario Swift nativo (CalendarHelper) que accede a EventKit directamente.
 * EventKit usa "responsible process" para el TCC check → los permisos concedidos
 * a WakeyWakey.app se aplican aunque CalendarHelper sea un subprocess.
 *
 * Esto resuelve el problema de osascript donde macOS comprobaba la identidad
 * del proceso hijo (osascript) y no la del app padre, devolviendo -10000.
 */
class MacSystemCalendarRepository : CalendarRepository {

    /**
     * Path al helper binario. Cuando corre como .app bundle está en
     * Contents/MacOS/CalendarHelper. En dev (./gradlew run) usamos el binario
     * precompilado en build/swift/ si existe.
     */
    private val helperPath: String by lazy {
        // 1. Junto al ejecutable principal (dentro del .app bundle)
        val exe = File(System.getProperty("compose.application.resources.dir") ?: "")
        val bundled = File(exe.parentFile?.parentFile, "MacOS/CalendarHelper")
        if (bundled.canExecute()) return@lazy bundled.absolutePath

        // 2. Ejecutable actual (Contents/MacOS/WakeyWakey → CalendarHelper)
        val self = File(ProcessHandle.current().info().command().orElse(""))
        val sibling = File(self.parentFile, "CalendarHelper")
        if (sibling.canExecute()) return@lazy sibling.absolutePath

        // 3. Build output (dev mode)
        val devBuild = File("apps/windows/build/swift/CalendarHelper")
        if (devBuild.canExecute()) return@lazy devBuild.absolutePath

        val devBuild2 = File("build/swift/CalendarHelper")
        if (devBuild2.canExecute()) return@lazy devBuild2.absolutePath

        System.err.println("MacCalendar: CalendarHelper not found, tried $bundled, $sibling, $devBuild")
        ""
    }

    override suspend fun getUpcomingEvents(
        fromTime:      Long,
        toTime:        Long,
        includeAllDay: Boolean,
    ): List<CalendarEvent> = withContext(Dispatchers.IO) {
        if (helperPath.isEmpty()) return@withContext emptyList()
        runCatching {
            val allDay = if (includeAllDay) "1" else "0"
            val output = runHelper("events", fromTime.toString(), toTime.toString(), allDay)
            parseEvents(output, includeAllDay)
        }.getOrElse { e ->
            System.err.println("MacCalendar.getUpcomingEvents: ${e.message}")
            emptyList()
        }
    }

    override suspend fun getAvailableCalendars(): List<DeviceCalendar> =
        withContext(Dispatchers.IO) {
            if (helperPath.isEmpty()) return@withContext emptyList()
            runCatching {
                val output = runHelper("calendars")
                System.err.println("MacCalendar.getAvailableCalendars: outLen=${output.length}")
                output.trim().lines()
                    .filter { it.contains("|") }
                    .map { line ->
                        val parts = line.split("|", limit = 2)
                        DeviceCalendar(
                            id          = parts.getOrNull(1)?.trim()?.hashCode()?.toLong() ?: 0L,
                            name        = parts.getOrNull(0)?.trim() ?: "Unknown",
                            accountName = "macOS Calendar",
                            color       = 0xFF4285F4.toInt(),
                        )
                    }
            }.getOrElse { e ->
                System.err.println("MacCalendar.getAvailableCalendars: ${e.message}")
                emptyList()
            }
        }

    // ── Helper runner ─────────────────────────────────────────────────────────

    private fun runHelper(vararg args: String): String {
        val cmd = listOf(helperPath) + args.toList()
        System.err.println("MacCalendar: running ${cmd.joinToString(" ")}")
        val process = ProcessBuilder(cmd)
            .redirectErrorStream(false)
            .start()
        val output = process.inputStream.bufferedReader().readText()
        val error  = process.errorStream.bufferedReader().readText()
        val exited = process.waitFor(15, TimeUnit.SECONDS)
        if (error.isNotBlank()) System.err.println("CalendarHelper stderr: $error")
        System.err.println("MacCalendar: exit=${if (exited) process.exitValue() else "timeout"} outLen=${output.length}")
        return output
    }

    // ── Event parser ──────────────────────────────────────────────────────────

    private fun parseEvents(output: String, includeAllDay: Boolean): List<CalendarEvent> {
        return output.trim().lines()
            .filter { it.contains("|") }
            .mapNotNull { line ->
                runCatching {
                    val p       = line.split("|")
                    val title   = p[0].trim()
                    val startMs = p[1].trim().toLong()
                    val endMs   = p[2].trim().toLong()
                    val loc     = p.getOrNull(3)?.trim()?.takeIf { it.isNotBlank() }
                    val url     = p.getOrNull(4)?.trim()?.takeIf { it.isNotBlank() }
                    val calName = p.getOrNull(5)?.trim() ?: "Calendar"
                    val calId   = p.getOrNull(6)?.trim() ?: calName
                    val allDay  = p.getOrNull(7)?.trim()?.equals("true", ignoreCase = true) ?: false

                    if (allDay && !includeAllDay) return@runCatching null
                    if (title.isBlank()) return@runCatching null

                    CalendarEvent(
                        id           = (calId + startMs).hashCode().toLong() and 0x7FFF_FFFFL,
                        title        = title,
                        startTime    = startMs,
                        endTime      = endMs,
                        location     = loc,
                        meetingLink  = url,
                        description  = null,
                        calendarId   = calId.hashCode().toLong() and 0x7FFF_FFFFL,
                        calendarName = calName,
                        isAllDay     = allDay,
                    )
                }.getOrNull()
            }
    }
}
