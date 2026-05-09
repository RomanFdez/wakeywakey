package com.sierraespada.wakeywakey.windows

import kotlinx.coroutines.*
import java.io.File

/**
 * Reproductor de MP3 cross-platform.
 *
 *  - macOS   → `afplay -v <vol> <path>`
 *  - Windows → PowerShell MediaPlayer (WPF assemblies, incluidos en Win 10/11)
 *  - Linux   → `mpg123 -q <path>` (fallback; no incluido en distribución)
 *
 * Si el player no está disponible, falla silenciosamente.
 */
object SoundPlayer {

    data class SoundDef(val id: String, val label: String, val emoji: String)

    val SOUND_DEFS = listOf(
        SoundDef("notification-1",    "Notification 1",    "🔔"),
        SoundDef("notification-2",    "Notification 2",    "🔔"),
        SoundDef("notification-3",    "Notification 3",    "🔔"),
        SoundDef("notification-4",    "Notification 4",    "🔔"),
        SoundDef("notification-5",    "Notification 5",    "🔔"),
        SoundDef("service-bell",      "Service Bell",      "🛎️"),
        SoundDef("clock-alarm",       "Clock Alarm",       "⏰"),
        SoundDef("call-to-attention", "Call to Attention", "📣"),
        SoundDef("boxing-ring",       "Boxing Ring",       "🥊"),
        SoundDef("coin",              "Coin",              "🪙"),
        SoundDef("level-up",          "Level Up",          "⬆️"),
        SoundDef("metal-spring",      "Metal Spring",      "🌀"),
        SoundDef("punch",             "Punch",             "👊"),
        SoundDef("referee-whistle",   "Referee Whistle",   "🏁"),
        SoundDef("whistle",           "Whistle",           "🌬️"),
    )

    private val isMac     = System.getProperty("os.name", "").startsWith("Mac")
    private val isWindows = System.getProperty("os.name", "").startsWith("Windows")

    /**
     * Extrae el MP3 de resources a un fichero temporal y lo reproduce.
     * Si [loop] = true, repite hasta que se cancela el Job devuelto.
     */
    fun play(soundId: String, volume: Float, loop: Boolean = false): Job {
        return CoroutineScope(Dispatchers.IO).launch {
            val tmpFile = extractToTemp(soundId) ?: return@launch
            try {
                val vol = volume.coerceIn(0f, 1f)
                do {
                    val cmd = buildCommand(tmpFile.absolutePath, vol)
                    val process = ProcessBuilder(cmd)
                        .redirectErrorStream(true)
                        .start()

                    withContext(Dispatchers.IO) {
                        while (process.isAlive && isActive) delay(100)
                        if (process.isAlive) process.destroyForcibly()
                        process.waitFor()
                    }
                    if (!isActive) break
                    if (loop && isActive) delay(500)
                } while (loop && isActive)
            } catch (e: Exception) {
                System.err.println("SoundPlayer[$soundId]: ${e.message}")
            } finally {
                tmpFile.delete()
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun buildCommand(path: String, volume: Float): List<String> = when {
        isMac     -> listOf("afplay", "-v", volume.toString(), path)
        isWindows -> listOf(
            "powershell.exe", "-NoProfile", "-NonInteractive", "-Command",
            // WPF MediaPlayer incluido en Windows 10/11 sin dependencias extra
            """
            Add-Type -AssemblyName PresentationCore
            ${'$'}mp = New-Object System.Windows.Media.MediaPlayer
            ${'$'}mp.Open([Uri]"$path")
            ${'$'}mp.Volume = $volume
            ${'$'}mp.Play()
            Start-Sleep -Milliseconds 30000
            """.trimIndent()
        )
        else      -> listOf("mpg123", "-q", path)   // Linux fallback
    }

    private fun extractToTemp(soundId: String): File? {
        val resourcePath = "/sounds/$soundId.mp3"
        val stream = SoundPlayer::class.java.getResourceAsStream(resourcePath)
            ?: run {
                System.err.println("SoundPlayer: resource not found: $resourcePath")
                return null
            }
        return runCatching {
            val tmp = File.createTempFile("ww_sound_$soundId", ".mp3")
            tmp.deleteOnExit()
            stream.use { input -> tmp.outputStream().use { output -> input.copyTo(output) } }
            tmp
        }.getOrElse { e ->
            System.err.println("SoundPlayer: failed to extract $soundId: ${e.message}")
            null
        }
    }
}
