package com.sierraespada.wakeywakey.windows.calendar

import com.sierraespada.wakeywakey.windows.BuildConfig
import com.sierraespada.wakeywakey.calendar.CalendarRepository
import com.sierraespada.wakeywakey.calendar.MeetingLinkDetector
import com.sierraespada.wakeywakey.model.CalendarEvent
import com.sierraespada.wakeywakey.model.DeviceCalendar
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.awt.Desktop
import java.net.URI
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Implementación de [CalendarRepository] para Google Calendar API v3.
 *
 * Flujo OAuth PKCE (RFC 7636):
 *  1. [authorize] abre el browser al consent screen de Google.
 *  2. OAuthCallbackServer (localhost:8765/callback) captura el code.
 *  3. Intercambia code + verifier por access_token + refresh_token.
 *  4. Persiste tokens en [TokenStorage].
 *  5. Auto-refresh transparente antes de cada llamada a la API.
 *
 * Credenciales: registrar en console.cloud.google.com como "Desktop app".
 * Leer de env vars GOOGLE_CLIENT_ID / GOOGLE_CLIENT_SECRET o de
 * ~/.wakeywakey/config.properties.
 */
class GoogleCalendarRepository : CalendarRepository {

    private val http = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true; isLenient = true })
        }
    }

    // calId string → Long estable (para poder revertir la conversión en llamadas API)
    private val calIdRegistry = mutableMapOf<Long, String>()

    // ── OAuth ─────────────────────────────────────────────────────────────────

    suspend fun authorize(): OAuthTokens {
        val verifier    = PkceHelper.generateCodeVerifier()
        val challenge   = PkceHelper.codeChallenge(verifier)
        val state       = PkceHelper.generateState()
        val callbackSrv = OAuthCallbackServer(CALLBACK_PORT).start()

        val authUrl = URLBuilder("https://accounts.google.com/o/oauth2/v2/auth").apply {
            parameters.apply {
                append("client_id",             clientId)
                append("redirect_uri",          callbackSrv.redirectUri)
                append("response_type",         "code")
                append("scope",                 SCOPE)
                append("code_challenge",        challenge)
                append("code_challenge_method", "S256")
                append("state",                 state)
                append("access_type",           "offline")
                append("prompt",                "consent")
            }
        }.buildString()

        Desktop.getDesktop().browse(URI(authUrl))

        val code = try { callbackSrv.awaitCode() } finally { callbackSrv.stop() }
        return exchangeCode(code, verifier, callbackSrv.redirectUri)
    }

    private suspend fun exchangeCode(code: String, verifier: String, redirectUri: String): OAuthTokens {
        val resp  = http.submitForm(
            url            = "https://oauth2.googleapis.com/token",
            formParameters = parameters {
                append("client_id",     clientId)
                append("client_secret", clientSecret)
                append("code",          code)
                append("code_verifier", verifier)
                append("grant_type",    "authorization_code")
                append("redirect_uri",  redirectUri)
            }
        )
        val body  = resp.body<GoogleTokenResponse>()
        val email = fetchEmail(body.accessToken)
        return OAuthTokens(
            accessToken  = body.accessToken,
            refreshToken = body.refreshToken ?: "",
            expiresAt    = System.currentTimeMillis() + body.expiresIn * 1_000L,
            provider     = PROVIDER,
            accountEmail = email,
        ).also { TokenStorage.save(it) }
    }

    private suspend fun refreshTokens(tokens: OAuthTokens): OAuthTokens {
        val resp = http.submitForm(
            url            = "https://oauth2.googleapis.com/token",
            formParameters = parameters {
                append("client_id",     clientId)
                append("client_secret", clientSecret)
                append("refresh_token", tokens.refreshToken)
                append("grant_type",    "refresh_token")
            }
        )
        val body = resp.body<GoogleTokenResponse>()
        return tokens.copy(
            accessToken = body.accessToken,
            expiresAt   = System.currentTimeMillis() + body.expiresIn * 1_000L,
        ).also { TokenStorage.save(it) }
    }

    private suspend fun validToken(): String {
        var t = TokenStorage.load(PROVIDER)
            ?: throw IllegalStateException("Not authenticated with Google")
        if (t.isExpired) t = refreshTokens(t)
        return t.accessToken
    }

    private suspend fun fetchEmail(accessToken: String): String = runCatching {
        http.get("https://www.googleapis.com/oauth2/v2/userinfo") {
            bearerAuth(accessToken)
        }.body<GoogleUserInfo>().email
    }.getOrDefault("")

    // ── CalendarRepository ────────────────────────────────────────────────────

    override suspend fun getUpcomingEvents(
        fromTime: Long, toTime: Long, includeAllDay: Boolean,
    ): List<CalendarEvent> {
        val token     = validToken()
        val calendars = getAvailableCalendars()
        return calendars.flatMap { cal ->
            runCatching { fetchEvents(token, cal, fromTime, toTime, includeAllDay) }
                .getOrElse { emptyList() }
        }.sortedBy { it.startTime }
    }

    private suspend fun fetchEvents(
        token: String, cal: DeviceCalendar,
        fromTime: Long, toTime: Long, includeAllDay: Boolean,
    ): List<CalendarEvent> {
        val googleCalId = calIdRegistry[cal.id]
            ?: return emptyList()  // no debería ocurrir si getAvailableCalendars se llamó antes

        val resp = http.get(
            "https://www.googleapis.com/calendar/v3/calendars/${encode(googleCalId)}/events"
        ) {
            bearerAuth(token)
            parameter("timeMin",      Instant.ofEpochMilli(fromTime).toString())
            parameter("timeMax",      Instant.ofEpochMilli(toTime).toString())
            parameter("singleEvents", "true")
            parameter("orderBy",      "startTime")
            parameter("maxResults",   "50")
        }
        val body = resp.body<GoogleEventsResponse>()

        return body.items
            .filter { it.status != "cancelled" }
            .filter { if (!includeAllDay) it.start.dateTime != null else true }
            .mapNotNull { ev ->
                val start = ev.start.dateTime?.let { parseIso(it) }
                    ?: ev.start.date?.let { parseDateOnly(it) } ?: return@mapNotNull null
                val end = ev.end.dateTime?.let { parseIso(it) }
                    ?: ev.end.date?.let { parseDateOnly(it) } ?: start

                val desc = ev.description
                val loc  = ev.location
                CalendarEvent(
                    id                 = ev.id.stableId(),
                    title              = ev.summary?.takeIf { it.isNotBlank() } ?: "(No title)",
                    startTime          = start,
                    endTime            = end,
                    location           = loc,
                    description        = desc,
                    calendarId         = cal.id,
                    calendarName       = cal.name,
                    meetingLink        = MeetingLinkDetector.extractFromEvent(desc, loc)
                        ?: ev.hangoutLink,
                    isAllDay           = ev.start.date != null,
                    selfAttendeeStatus = ev.attendees
                        ?.firstOrNull { it.self }
                        ?.toStatusInt() ?: 0,
                )
            }
    }

    override suspend fun getAvailableCalendars(): List<DeviceCalendar> {
        val token = validToken()
        val resp  = http.get("https://www.googleapis.com/calendar/v3/users/me/calendarList") {
            bearerAuth(token)
            parameter("minAccessRole", "reader")
        }
        val body = resp.body<GoogleCalendarListResponse>()
        return body.items
            .filter { it.selected }
            .map { cal ->
                val stableId = cal.id.stableId()
                calIdRegistry[stableId] = cal.id   // registra para la llamada inversa
                DeviceCalendar(
                    id          = stableId,
                    name        = cal.summary.ifBlank { cal.id },
                    accountName = cal.id,
                    color       = cal.backgroundColor?.hexToArgb() ?: 0xFF4285F4.toInt(),
                    isVisible   = true,
                )
            }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun parseIso(s: String): Long =
        OffsetDateTime.parse(s).toInstant().toEpochMilli()

    private fun parseDateOnly(s: String): Long =
        LocalDate.parse(s, DateTimeFormatter.ISO_LOCAL_DATE)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant().toEpochMilli()

    /** URL-encode para el calendarId en el path (ej: primary, user@gmail.com). */
    private fun encode(s: String): String =
        java.net.URLEncoder.encode(s, "UTF-8").replace("+", "%20")

    /** "#RRGGBB" → ARGB Int. */
    private fun String.hexToArgb(): Int = runCatching {
        val hex = removePrefix("#")
        (0xFF000000.toInt()) or hex.toInt(16)
    }.getOrElse { 0xFF4285F4.toInt() }

    private fun GoogleAttendee.toStatusInt(): Int = when (responseStatus) {
        "accepted"  -> 1
        "declined"  -> 2
        "tentative" -> 4
        else        -> 0
    }

    companion object {
        const val PROVIDER      = "google"
        private const val CALLBACK_PORT = 8765
        private const val SCOPE = "https://www.googleapis.com/auth/calendar.readonly"

        // Prioridad: BuildConfig (compilado en el binario por CI) →
        //            variable de entorno → propiedad de sistema → config file local
        val clientId: String get() = credential("GOOGLE_CLIENT_ID") {
            BuildConfig.GOOGLE_CLIENT_ID.takeIf { it.isNotBlank() }
        }
        val clientSecret: String get() = credential("GOOGLE_CLIENT_SECRET") {
            BuildConfig.GOOGLE_CLIENT_SECRET.takeIf { it.isNotBlank() }
        }

        val isConfigured: Boolean
            get() = runCatching { clientId; clientSecret; true }.getOrElse { false }

        private fun credential(key: String, fromBuildConfig: () -> String?): String =
            fromBuildConfig()
                ?: System.getenv(key)
                ?: System.getProperty(key)
                ?: loadConfigFile(key)
                ?: error("$key not configured.")

        private fun loadConfigFile(key: String): String? = runCatching {
            val file = java.io.File(System.getProperty("user.home"), ".wakeywakey/config.properties")
            java.util.Properties().apply { load(file.inputStream()) }.getProperty(key)
        }.getOrNull()
    }
}

// ── Serialización Google Calendar API ─────────────────────────────────────────

@Serializable private data class GoogleTokenResponse(
    @SerialName("access_token")  val accessToken:  String,
    @SerialName("refresh_token") val refreshToken: String? = null,
    @SerialName("expires_in")    val expiresIn:    Long    = 3600L,
)
@Serializable private data class GoogleUserInfo(val email: String = "")
@Serializable private data class GoogleEventsResponse(val items: List<GoogleEvent> = emptyList())
@Serializable private data class GoogleEvent(
    val id:          String,
    val summary:     String?              = null,
    val description: String?              = null,
    val location:    String?              = null,
    val status:      String?              = null,
    val start:       GoogleDateTime,
    val end:         GoogleDateTime,
    val hangoutLink: String?              = null,
    val attendees:   List<GoogleAttendee>? = null,
)
@Serializable private data class GoogleDateTime(
    val dateTime: String? = null,
    val date:     String? = null,
)
@Serializable private data class GoogleAttendee(
    val email:          String  = "",
    val self:           Boolean = false,
    val responseStatus: String? = null,
)
@Serializable private data class GoogleCalendarListResponse(val items: List<GoogleCalendarItem> = emptyList())
@Serializable private data class GoogleCalendarItem(
    val id:              String,
    val summary:         String  = "",
    val backgroundColor: String? = null,
    val selected:        Boolean = true,
)

/** Hash string → Long estable (sin colisiones para IDs de Google Calendar). */
private fun String.stableId(): Long {
    var h = 7L
    for (c in this) h = h * 31L + c.code
    return h
}
