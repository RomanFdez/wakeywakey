package com.sierraespada.wakeywakey.windows

import kotlinx.coroutines.*
import javax.sound.sampled.*
import kotlin.math.*

/**
 * Genera y reproduce sonidos de alerta de forma programática (sin ficheros de audio).
 * Cada sonido se sintetiza con PCM a 44100 Hz, 16 bits, mono.
 */
object SoundPlayer {

    data class SoundDef(val id: String, val label: String, val emoji: String)

    val SOUND_DEFS = listOf(
        SoundDef("bell",    "Bell",    "🔔"),
        SoundDef("ring",    "Phone",   "📞"),
        SoundDef("chime",   "Chimes",  "🎵"),
        SoundDef("ping",    "Ping",    "✨"),
        SoundDef("horn",    "Horn",    "📯"),
        SoundDef("duck",    "Duck",    "🦆"),
        SoundDef("alarm",   "Alarm",   "🚨"),
        SoundDef("coin",    "Coin",    "🪙"),
        SoundDef("plop",    "Plop",    "💧"),
        SoundDef("whistle", "Whistle", "🌬️"),
        SoundDef("fanfare", "Fanfare", "🎺"),
        SoundDef("boing",   "Boing",   "🌀"),
    )

    private val SR = 44100   // sample rate Hz

    /**
     * Reproduce el sonido [soundId] con el [volume] dado (0.0–1.0).
     * Si [loop] = true, repite hasta que se cancela el Job devuelto.
     * Devuelve un Job que se puede cancelar para detener la reproducción.
     */
    fun play(soundId: String, volume: Float, loop: Boolean = false): Job {
        return CoroutineScope(Dispatchers.IO).launch {
            try {
                val samples = generateSamples(soundId)
                val bytes   = samplesToBytes(samples, volume.coerceIn(0f, 1f))
                val format  = AudioFormat(SR.toFloat(), 16, 1, true, false)
                // SourceDataLine is more reliable than Clip on macOS — write() blocks
                // until audio is actually played, which prevents silent failures.
                val info = DataLine.Info(SourceDataLine::class.java, format)
                val line = (AudioSystem.getLine(info) as SourceDataLine)
                line.open(format)
                line.start()
                try {
                    do {
                        line.write(bytes, 0, bytes.size)
                        line.drain()
                        if (loop && isActive) delay(2000)
                    } while (loop && isActive)
                } finally {
                    line.stop()
                    line.close()
                }
            } catch (e: Exception) {
                System.err.println("SoundPlayer[$soundId]: ${e.message}")
            }
        }
    }

    // ── Sample generators ──────────────────────────────────────────────────────

    private fun generateSamples(id: String): FloatArray = when (id) {
        "bell"    -> bellSamples()
        "ring"    -> ringSamples()
        "chime"   -> chimeSamples()
        "ping"    -> pingSamples()
        "horn"    -> hornSamples()
        "duck"    -> duckSamples()
        "alarm"   -> alarmSamples()
        "coin"    -> coinSamples()
        "plop"    -> plopSamples()
        "whistle" -> whistleSamples()
        "fanfare" -> fanfareSamples()
        "boing"   -> boingSamples()
        else      -> bellSamples()
    }

    // 🔔 Bell — 880 Hz sine with exponential decay, 1.2 s
    private fun bellSamples(): FloatArray {
        val n = (SR * 1.2).toInt()
        return FloatArray(n) { i ->
            val t = i.toFloat() / SR
            sin(2 * PI * 880 * t).toFloat() * exp(-4.0 * t).toFloat()
        }
    }

    // 📞 Phone — two-tone 440+480 Hz, 0.6 s on / 0.4 s off × 2 rings
    private fun ringSamples(): FloatArray {
        val onMs = (SR * 0.6).toInt()
        val offMs = (SR * 0.4).toInt()
        val ring = FloatArray(onMs) { i ->
            val t = i.toFloat() / SR
            ((sin(2 * PI * 440 * t) + sin(2 * PI * 480 * t)) / 2).toFloat()
        }
        val silence = FloatArray(offMs) { 0f }
        return ring + silence + ring
    }

    // 🎵 Chimes — ascending C5→E5→G5, each 0.35 s with decay
    private fun chimeSamples(): FloatArray {
        fun note(freq: Double) = FloatArray((SR * 0.35).toInt()) { i ->
            val t = i.toFloat() / SR
            sin(2 * PI * freq * t).toFloat() * exp(-5.0 * t).toFloat()
        }
        return note(523.25) + note(659.25) + note(783.99)
    }

    // ✨ Ping — short 1200 Hz, 0.18 s, fast decay
    private fun pingSamples(): FloatArray {
        val n = (SR * 0.18).toInt()
        return FloatArray(n) { i ->
            val t = i.toFloat() / SR
            sin(2 * PI * 1200 * t).toFloat() * exp(-14.0 * t).toFloat()
        }
    }

    // 📯 Horn — 220 Hz triangle wave with harmonics, 0.6 s
    private fun hornSamples(): FloatArray {
        val n = (SR * 0.6).toInt()
        return FloatArray(n) { i ->
            val t = i.toFloat() / SR
            val env = if (t < 0.05) t / 0.05f else exp(-2.0 * (t - 0.05)).toFloat()
            val tri = (2 * abs(2 * (220 * t - floor(220 * t + 0.5))) - 1).toFloat()
            tri * env.toFloat()
        }
    }

    // 🦆 Duck — "quack": two-lobe shape, nasal 1400→600 Hz + AM modulation
    private fun duckSamples(): FloatArray {
        val n = (SR * 0.28).toInt()
        var phase = 0.0
        return FloatArray(n) { i ->
            val t  = i.toFloat() / SR
            val tn = t / 0.28f                          // 0..1 normalised
            // Frequency drops from 1400→600 Hz (nasal quack sweep)
            val freq = 1400.0 - 800.0 * tn
            phase += 2 * PI * freq / SR
            // Envelope: two-lobe "w-ACK" shape
            val lobe1 = if (tn < 0.18f) (tn / 0.18f) else 0f
            val lobe2 = if (tn in 0.22f..1.0f)
                sin(PI * ((tn - 0.22f) / 0.78f)).toFloat()
            else 0f
            val env = maxOf(lobe1, lobe2)
            // Add slight nasal AM (120 Hz tremolo)
            val am = (0.7f + 0.3f * sin(2 * PI * 120 * t)).toFloat()
            sin(phase).toFloat() * env * am
        }
    }

    // 🚨 Alarm — fast alternating 800/1000 Hz every 80 ms, 1.0 s
    private fun alarmSamples(): FloatArray {
        val n = (SR * 1.0).toInt()
        val period = (SR * 0.08).toInt()
        return FloatArray(n) { i ->
            val t = i.toFloat() / SR
            val freq = if ((i / period) % 2 == 0) 800.0 else 1000.0
            sin(2 * PI * freq * t).toFloat()
        }
    }

    // 🪙 Coin — ascending glide 800→1600 Hz, 0.28 s
    private fun coinSamples(): FloatArray {
        val n = (SR * 0.28).toInt()
        var phase = 0.0
        return FloatArray(n) { i ->
            val t = i.toFloat() / SR
            val freq = 800.0 + 800.0 * t / 0.28
            phase += 2 * PI * freq / SR
            val env = exp(-6.0 * t).toFloat()
            sin(phase).toFloat() * env
        }
    }

    // 💧 Plop — descending 400→80 Hz, 0.22 s
    private fun plopSamples(): FloatArray {
        val n = (SR * 0.22).toInt()
        var phase = 0.0
        return FloatArray(n) { i ->
            val t = i.toFloat() / SR
            val freq = 400.0 * exp(-4.5 * t)
            phase += 2 * PI * freq / SR
            val env = exp(-8.0 * t).toFloat()
            sin(phase).toFloat() * env
        }
    }

    // 🌬️ Whistle — 1800 Hz with 7 Hz tremolo, 0.55 s
    private fun whistleSamples(): FloatArray {
        val n = (SR * 0.55).toInt()
        return FloatArray(n) { i ->
            val t = i.toFloat() / SR
            val am  = (0.5 + 0.5 * sin(2 * PI * 7 * t)).toFloat()
            val env = if (t < 0.05) t / 0.05f else exp(-2.0 * (t - 0.05)).toFloat()
            sin(2 * PI * 1800 * t).toFloat() * am * env.toFloat()
        }
    }

    // 🎺 Fanfare — G4→C5→E5, 0.15 s each, instant rise
    private fun fanfareSamples(): FloatArray {
        fun note(freq: Double, ms: Double) = FloatArray((SR * ms).toInt()) { i ->
            val t = i.toFloat() / SR
            sin(2 * PI * freq * t).toFloat() * exp(-3.0 * t).toFloat()
        }
        return note(392.0, 0.15) + note(523.25, 0.15) + note(659.25, 0.25)
    }

    // 🌀 Boing — spring: 200→500→200 Hz sweep, 0.5 s
    private fun boingSamples(): FloatArray {
        val n = (SR * 0.5).toInt()
        var phase = 0.0
        return FloatArray(n) { i ->
            val t = i.toFloat() / SR
            // freq follows a triangle-ish sweep: rises then falls
            val norm = t / 0.5
            val freq = 200.0 + 300.0 * (1 - abs(2 * norm - 1))
            phase += 2 * PI * freq / SR
            val env = exp(-2.5 * t).toFloat()
            sin(phase).toFloat() * env
        }
    }

    // ── PCM conversion ─────────────────────────────────────────────────────────

    private fun samplesToBytes(samples: FloatArray, volume: Float): ByteArray {
        val bytes = ByteArray(samples.size * 2)
        samples.forEachIndexed { i, sample ->
            val s = (sample * volume * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            bytes[i * 2]     = (s.toInt() and 0xFF).toByte()
            bytes[i * 2 + 1] = ((s.toInt() shr 8) and 0xFF).toByte()
        }
        return bytes
    }
}

private operator fun FloatArray.plus(other: FloatArray): FloatArray {
    val result = FloatArray(size + other.size)
    copyInto(result)
    other.copyInto(result, size)
    return result
}
