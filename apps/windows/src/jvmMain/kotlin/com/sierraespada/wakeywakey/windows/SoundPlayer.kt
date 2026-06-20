package com.sierraespada.wakeywakey.windows

import kotlinx.coroutines.*
import java.io.File
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.FloatControl
import javax.sound.sampled.SourceDataLine

/**
 * Reproductor de MP3 cross-platform usando javax.sound.sampled + mp3spi.
 *
 * mp3spi registra un AudioFileReader para MP3 que AudioSystem detecta automáticamente.
 * Sin procesos externos, sin dependencias de STA/WPF — funciona en cualquier hilo.
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
     * Extrae el MP3 de resources y lo reproduce en un coroutine de IO.
     * Si [loop] = true, repite hasta que se cancela el Job devuelto.
     */
    fun play(soundId: String, volume: Float, loop: Boolean = false): Job {
        return CoroutineScope(Dispatchers.IO).launch {
            val tmpFile = extractToTemp(soundId) ?: return@launch
            try {
                do {
                    playFile(tmpFile, volume.coerceIn(0f, 1f))
                    if (loop && isActive) delay(300)
                } while (loop && isActive)
            } catch (_: CancellationException) {
                // cancelled normally — no-op
            } catch (e: Exception) {
                System.err.println("SoundPlayer[$soundId]: ${e.message}")
            } finally {
                tmpFile.delete()
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun playFile(file: File, volume: Float) {
        val audioStream = AudioSystem.getAudioInputStream(file)
        val baseFormat  = audioStream.format
        // mp3spi decodifica a PCM; necesitamos un formato PCM que la línea de salida acepte
        val pcmFormat   = javax.sound.sampled.AudioFormat(
            javax.sound.sampled.AudioFormat.Encoding.PCM_SIGNED,
            baseFormat.sampleRate,
            16,
            baseFormat.channels,
            baseFormat.channels * 2,
            baseFormat.sampleRate,
            false,
        )
        val pcmStream = AudioSystem.getAudioInputStream(pcmFormat, audioStream)
        val info      = javax.sound.sampled.DataLine.Info(SourceDataLine::class.java, pcmFormat)
        val line      = AudioSystem.getLine(info) as SourceDataLine

        line.open(pcmFormat)

        // Control de volumen (dB) — convierte 0..1 a rango del control
        if (line.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            val gain    = line.getControl(FloatControl.Type.MASTER_GAIN) as FloatControl
            val minDb   = gain.minimum
            val maxDb   = gain.maximum
            // volumen 0 → mínimo; volumen 1 → máximo; escala logarítmica aproximada
            val targetDb = minDb + (maxDb - minDb) * volume
            gain.value  = targetDb.coerceIn(minDb, maxDb)
        }

        line.start()

        val buf  = ByteArray(4096)
        var read: Int
        while (pcmStream.read(buf).also { read = it } != -1) {
            line.write(buf, 0, read)
            if (Thread.currentThread().isInterrupted) break
        }

        line.drain()
        line.close()
        pcmStream.close()
        audioStream.close()
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
