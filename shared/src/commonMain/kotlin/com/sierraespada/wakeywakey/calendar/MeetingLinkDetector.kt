package com.sierraespada.wakeywakey.calendar

/**
 * Detecta enlaces de videollamada en el texto de un evento de calendario.
 * Soporta 30+ servicios. Devuelve el primer enlace encontrado, priorizando
 * los servicios más comunes.
 */
object MeetingLinkDetector {

    private val patterns: List<Regex> = listOf(
        // Google Meet
        Regex("""https://meet\.google\.com/[a-z]{3}-[a-z]{4}-[a-z]{3}"""),
        Regex("""https://meet\.google\.com/\S+"""),

        // Zoom
        Regex("""https://[\w-]+\.zoom\.us/j/\d+(?:\?pwd=\S+)?"""),
        Regex("""https://zoom\.us/j/\d+(?:\?pwd=\S+)?"""),

        // Microsoft Teams
        Regex("""https://teams\.microsoft\.com/l/meetup-join/\S+"""),
        Regex("""https://teams\.live\.com/meet/\S+"""),

        // Webex
        Regex("""https://[\w-]+\.webex\.com/\S+"""),

        // Whereby
        Regex("""https://whereby\.com/\S+"""),
        Regex("""https://[\w-]+\.whereby\.com/\S+"""),

        // Jitsi Meet
        Regex("""https://meet\.jit\.si/\S+"""),
        Regex("""https://[\w-]+\.jitsi\.net/\S+"""),

        // GoToMeeting / GoTo
        Regex("""https://\w+\.goto\.com/\S+"""),
        Regex("""https://global\.gotomeeting\.com/join/\d+"""),
        Regex("""https://meet\.goto\.com/\S+"""),

        // BlueJeans
        Regex("""https://bluejeans\.com/\d+(?:/\d+)?"""),

        // Cisco Webex (meet subdomain)
        Regex("""https://[\w-]+\.cisco\.com/\S+"""),

        // Around
        Regex("""https://around\.co/r/\S+"""),

        // Gather Town
        Regex("""https://app\.gather\.town/\S+"""),

        // Discord
        Regex("""https://discord\.gg/\S+"""),
        Regex("""https://discord\.com/channels/\S+"""),

        // Slack Huddle / Call
        Regex("""https://[\w-]+\.slack\.com/\S+"""),

        // Loom
        Regex("""https://www\.loom\.com/share/\S+"""),

        // Skype
        Regex("""https://join\.skype\.com/\S+"""),

        // Google Hangouts (legacy)
        Regex("""https://hangouts\.google\.com/\S+"""),

        // Amazon Chime
        Regex("""https://chime\.aws/\S+"""),

        // Zoho Meeting
        Regex("""https://meeting\.zoho\.\w+/\S+"""),

        // 8x8
        Regex("""https://8x8\.vc/\S+"""),

        // Lifesize
        Regex("""https://call\.lifesizecloud\.com/\S+"""),

        // Livestorm
        Regex("""https://app\.livestorm\.co/\S+"""),

        // Generic fallback — cualquier URL que contenga "meet", "zoom" o "call"
        Regex("""https?://\S*(?:meet|zoom|call|conference|video)\S*"""),
    )

    /**
     * Extrae el primer enlace de videollamada encontrado en [text].
     * Devuelve null si no encuentra ninguno.
     */
    fun extract(text: String?): String? {
        if (text.isNullOrBlank()) return null
        for (pattern in patterns) {
            val match = pattern.find(text)?.value
            if (match != null) return cleanUrl(match)
        }
        return null
    }

    /** Extrae de description y location, priorizando description. */
    fun extractFromEvent(description: String?, location: String?): String? =
        extract(description) ?: extract(location)

    private fun cleanUrl(url: String): String =
        url.trimEnd('.', ',', ')', ']', '>')
}
