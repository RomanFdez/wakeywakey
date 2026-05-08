package com.sierraespada.wakeywakey.windows

import kotlinx.coroutines.*
import java.io.File

/**
 * Reproduce los ficheros MP3 incluidos en resources/sounds/ usando `afplay` (macOS).
 * Si el proceso no está disponible (Windows/Linux), cae silenciosamente.
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

    /**
     * Extrae el MP3 de resources a un fichero temporal y lo reproduce con afplay.
     * Si [loop] = true, repite hasta que se cancela el Job devuelto.
     * Devuelve un Job que se puede cancelar para detener la reproducción.
     */
    fun play(soundId: String, volume: Float, loop: Boolean = false): Job {
        return CoroutineScope(Dispatchers.IO).launch {
            val tmpFile = extractToTemp(soundId) ?: return@launch
            try {
                val vol = volume.coerceIn(0f, 1f)
                do {
                    val process = ProcessBuilder(
                        "afplay", "-v", vol.toString(), tmpFile.absolutePath
                    )
                        .redirectErrorStream(true)
                        .start()

                    // Espera a que termine, pero para si se cancela el job
                    val exitCode = withContext(Dispatchers.IO) {
                        while (process.isAlive && isActive) delay(100)
                        if (process.isAlive) process.destroyForcibly()
                        process.waitFor()
                    }
                    if (!isActive) break
                    if (loop && isActive) delay(500)   // pausa entre repeticiones
                } while (loop && isActive)
            } catch (e: Exception) {
                System.err.println("SoundPlayer[$soundId]: ${e.message}")
            } finally {
                tmpFile.delete()
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

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
