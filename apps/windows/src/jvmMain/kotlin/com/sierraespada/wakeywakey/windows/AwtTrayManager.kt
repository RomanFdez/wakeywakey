package com.sierraespada.wakeywakey.windows

import com.sierraespada.wakeywakey.model.UserSettings
import com.sierraespada.wakeywakey.windows.home.HomeUiState
import java.awt.AlphaComposite
import java.awt.BasicStroke
import java.awt.Font as AwtFont
import java.awt.RenderingHints
import java.awt.SystemTray
import java.awt.TrayIcon
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.geom.GeneralPath
import java.awt.image.BufferedImage
import java.util.Calendar as JCal
import java.awt.Color as AwtColor
import javax.swing.SwingUtilities

/**
 * Gestiona el icono del system tray usando java.awt.SystemTray directamente,
 * evitando las limitaciones del Tray composable de Compose Desktop
 * (que fuerza isImageAutoSize = true y escala el bitmap al tamaño de icono estándar).
 *
 * Con isImageAutoSize = false el bitmap ancho (icono + texto) se muestra
 * en su tamaño nativo, igual que hace IYF con NSStatusItem.
 */
object AwtTrayManager {

    private var trayIcon: TrayIcon? = null

    // Callbacks inyectados desde Main
    // onTrayClicked recibe la posición X del clic para posicionar el popup debajo del icono
    private var onTrayClicked: (clickX: Int) -> Unit = {}
    private var onQuit:        () -> Unit             = {}
    private var isPaused:      Boolean                = false

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    fun install(
        onTrayClicked: (clickX: Int) -> Unit,
        onQuit:        () -> Unit,
    ) {
        if (!SystemTray.isSupported()) return

        this.onTrayClicked = onTrayClicked
        this.onQuit        = onQuit

        val ti = TrayIcon(buildImage(null), "WakeyWakey")
        ti.isImageAutoSize = false   // ← clave: sin esto macOS escala a 22×22

        // Usamos MouseListener en lugar de ActionListener para:
        //  • Detectar UN SOLO clic (ActionListener requiere doble clic en algunas plataformas)
        //  • Obtener la posición exacta del clic para posicionar el popup justo debajo del icono
        // Sin popupMenu de AWT: si está asignado, macOS lo muestra en el primer clic
        // izquierdo e intercepta el MouseEvent antes de llegar a nuestro listener.
        // Todo queda en el TrayMenuWindow de Compose (incluyendo el botón Quit).
        ti.addMouseListener(object : MouseAdapter() {
            // mousePressed (not mouseClicked) fires on the first physical press,
            // before the button is released. This prevents the "two-click" issue on
            // macOS where the click event sometimes arrives only on the second press.
            override fun mousePressed(e: MouseEvent) {
                if (e.button == MouseEvent.BUTTON1) {
                    SwingUtilities.invokeLater { onTrayClicked(e.locationOnScreen.x) }
                }
            }
        })

        try {
            SystemTray.getSystemTray().add(ti)
            trayIcon = ti
        } catch (_: Exception) {}
    }

    fun remove() {
        trayIcon?.let {
            try { SystemTray.getSystemTray().remove(it) } catch (_: Exception) {}
        }
        trayIcon = null
    }

    // ── Actualizaciones reactivas ──────────────────────────────────────────────

    fun updateContent(settings: UserSettings, homeState: HomeUiState) {
        val label = runCatching { computeLabel(settings, homeState) }.getOrNull()
        // Siempre actualizar en el EDT para que AWT procese el cambio correctamente
        SwingUtilities.invokeLater { trayIcon?.image = buildImage(label, settings.trayMonochromeIcon, settings.trayAccentColor) }
    }

    fun updatePaused(paused: Boolean) {
        isPaused = paused
        // Ya no hay popupMenu de AWT; el estado de pausa se refleja en el TrayMenuWindow
    }

    // ── Texto del tray ────────────────────────────────────────────────────────

    private fun computeLabel(settings: UserSettings, homeState: HomeUiState): String? {
        if (!settings.trayShowMeetingName && !settings.trayShowTimeRemaining) return null

        val now = System.currentTimeMillis()

        // Siguiente reunión que aún no ha empezado (cualquier día).
        val next = homeState.events.firstOrNull { ev -> ev.startTime > now } ?: return null

        val title: String? = if (settings.trayShowMeetingName) {
            val raw = next.title
            val max = settings.trayTitleMaxLength
            if (raw.length <= max) raw
            else if (settings.trayTitleTruncateMiddle) {
                val half = max / 2
                raw.take(half) + "…" + raw.takeLast(max - half - 1)
            } else {
                raw.take(max - 1) + "…"
            }
        } else null

        val time: String? = if (settings.trayShowTimeRemaining) {
            val daysAway = calendarDaysUntil(next.startTime)
            when {
                daysAway == 0 -> {
                    // Hoy: muestra cuenta regresiva en minutos/segundos
                    val minsLeft = ((next.startTime - now) / 60_000L).toInt().coerceAtLeast(0)
                    when {
                        minsLeft <= 0 -> "now"
                        minsLeft < 60 -> if (settings.countdownMinutesOnly) "${minsLeft}m"
                                         else {
                                             val s = (((next.startTime - now) / 1000L) % 60L)
                                                         .toInt().coerceAtLeast(0)
                                             "${minsLeft}m %02ds".format(s)
                                         }
                        else          -> "${minsLeft / 60}h ${minsLeft % 60}m"
                    }
                }
                daysAway == 1 -> if (isSpanish()) "Mañana" else "Tomorrow"
                else          -> "+${daysAway}d"
            }
        } else null

        return listOfNotNull(title, time).joinToString(" · ").ifEmpty { null }
    }

    /** Días de calendario entre ahora y [targetMillis] (0 = hoy, 1 = mañana, …). */
    private fun calendarDaysUntil(targetMillis: Long): Int {
        val now = JCal.getInstance()
        val target = JCal.getInstance().apply { timeInMillis = targetMillis }
        // Normaliza a medianoche para comparar días completos
        now.set(JCal.HOUR_OF_DAY, 0); now.set(JCal.MINUTE, 0); now.set(JCal.SECOND, 0); now.set(JCal.MILLISECOND, 0)
        target.set(JCal.HOUR_OF_DAY, 0); target.set(JCal.MINUTE, 0); target.set(JCal.SECOND, 0); target.set(JCal.MILLISECOND, 0)
        val diffMs = target.timeInMillis - now.timeInMillis
        return (diffMs / (24 * 60 * 60 * 1000L)).toInt().coerceAtLeast(0)
    }

    private fun isToday(millis: Long): Boolean = calendarDaysUntil(millis) == 0

    private fun isSpanish(): Boolean =
        java.util.Locale.getDefault().language.equals("es", ignoreCase = true)

    // ── Renderizado AWT ───────────────────────────────────────────────────────

    /**
     * Construye el BufferedImage del tray.
     *  • @2x para Retina: iconSize = 44px → se muestra como 22pt
     *  • Con isImageAutoSize=false macOS respeta el ancho natural del bitmap
     *
     * [monochrome] = true → WW blanco sobre transparente (estilo macOS nativo)
     * [monochrome] = false → círculo amarillo con WW navy (estilo marca)
     */
    private fun buildImage(label: String?, monochrome: Boolean = false, accentColor: String = "system"): java.awt.Image {
        val scale    = 2
        val iconSize = 22 * scale          // 44px
        val fontSize = 13 * scale          // 26px → 13pt en pantalla

        val font     = AwtFont(AwtFont.SANS_SERIF, AwtFont.BOLD, fontSize)

        val textWidth: Int = if (label != null) {
            val tmp = BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
            val tg  = tmp.createGraphics().also {
                it.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
                                    RenderingHints.VALUE_FRACTIONALMETRICS_ON)
                it.font = font
            }
            val tw = tg.fontMetrics.stringWidth(label)
            tg.dispose()
            tw
        } else 0

        val textGap    = 6 * scale
        val rightPad   = 4 * scale
        val totalWidth = if (label != null) iconSize + textGap + textWidth + rightPad else iconSize

        val img = BufferedImage(totalWidth, iconSize, BufferedImage.TYPE_INT_ARGB)
        val g   = img.createGraphics().apply {
            setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON)
            setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
            setRenderingHint(RenderingHints.KEY_RENDERING,         RenderingHints.VALUE_RENDER_QUALITY)
            setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON)
        }

        val cx = iconSize / 2f
        val cy = iconSize / 2f
        val r  = iconSize / 2f - scale

        // Resolve accent color (null = system default)
        val accentAwtColor: AwtColor? = when (accentColor) {
            "red"    -> AwtColor(255,  59,  48)
            "yellow" -> AwtColor(255, 204,   0)
            "blue"   -> AwtColor(  0, 122, 255)
            "purple" -> AwtColor(175,  82, 222)
            "green"  -> AwtColor( 52, 199,  89)
            "orange" -> AwtColor(255, 149,   0)
            else     -> null
        }
        val textAwtColor: AwtColor = accentAwtColor ?: AwtColor(1f, 1f, 1f, 0.85f)

        // ── Geometría WW compartida: IDÉNTICA en ambos estilos ────────────────
        // Solo cambian el color / compositing, no las proporciones.
        val wH   = iconSize * 0.20f
        val wW   = iconSize * 0.26f
        val wGap = iconSize * 0.025f
        val wTop = cy - wH * 0.50f
        val wBot = cy + wH * 0.50f
        val wMid = cy - wH * 0.12f

        fun makeWPath(lx: Float, mX: Float, rx: Float) = GeneralPath().apply {
            moveTo(lx.toDouble(),                 wTop.toDouble())
            lineTo((lx + wW * 0.28f).toDouble(),  wBot.toDouble())
            lineTo(mX.toDouble(),                 wMid.toDouble())
            lineTo((rx - wW * 0.28f).toDouble(),  wBot.toDouble())
            lineTo(rx.toDouble(),                 wTop.toDouble())
        }
        val wwStroke = BasicStroke(iconSize * 0.07f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
        val wPath1 = makeWPath(cx - wW - wGap/2f, cx - wGap/2f - wW/2f, cx - wGap/2f)
        val wPath2 = makeWPath(cx + wGap/2f,       cx + wGap/2f + wW/2f, cx + wW + wGap/2f)

        if (monochrome) {
            // ── Estilo macOS: círculo BLANCO + WW recortadas (transparentes) ───
            // 1) Rellena el círculo en blanco semiopaco (o con acento si se configuró)
            g.color = accentAwtColor ?: AwtColor(1f, 1f, 1f, 0.90f)
            g.fillOval((cx - r).toInt(), (cy - r).toInt(), (2 * r).toInt(), (2 * r).toInt())

            // 2) DST_OUT recorta las WW → dejan ver el fondo transparente
            g.composite = AlphaComposite.getInstance(AlphaComposite.DST_OUT)
            g.color     = AwtColor(0f, 0f, 0f, 1f)   // el color no importa en DST_OUT
            g.stroke    = wwStroke
            g.draw(wPath1)
            g.draw(wPath2)

            // 3) Restaura SRC_OVER para el texto
            g.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER)
            if (label != null) {
                g.font  = font
                g.color = textAwtColor
                val fm    = g.fontMetrics
                val textY = (iconSize + fm.ascent - fm.descent) / 2
                g.drawString(label, iconSize + textGap, textY)
            }
        } else {
            // ── Estilo marca: círculo SIEMPRE amarillo + WW navy ─────────────
            // El color de acento solo afecta al texto, no al icono.
            g.color = AwtColor(0xFF, 0xE0, 0x3A)
            g.fillOval((cx - r).toInt(), (cy - r).toInt(), (2 * r).toInt(), (2 * r).toInt())

            g.color  = AwtColor(0x1A, 0x1A, 0x2E)
            g.stroke = wwStroke
            g.draw(wPath1)
            g.draw(wPath2)

            if (label != null) {
                g.font  = font
                g.color = textAwtColor
                val fm    = g.fontMetrics
                val textY = (iconSize + fm.ascent - fm.descent) / 2
                g.drawString(label, iconSize + textGap, textY)
            }
        }

        g.dispose()
        return img
    }
}
