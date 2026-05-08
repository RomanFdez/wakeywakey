package com.sierraespada.wakeywakey.windows.calendar

import com.sierraespada.wakeywakey.windows.BuildConfig
import com.sierraespada.wakeywakey.calendar.CalendarRepository
import com.sierraespada.wakeywakey.calendar.MeetingLinkDetector
import com.sierraespada.wakeywakey.model.CalendarEvent
import com.sierraespada.wakeywakey.model.DeviceCalendar
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.statement.*
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
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

/**
 * Implementación de [CalendarRepository] para Microsoft Graph API v1.0.
 *
 * Flujo OAuth PKCE (RFC 7636) — misma mecánica que Google pero con endpoints de
 * Microsoft Identity Platform (Azure AD):
 *  1. [authorize] abre el browser al consent screen de Microsoft.
 *  2. OAuthCallbackServer (localhost:8766/callback) captura el code.
 *  3. Intercambia code + verifier por access_token + refresh_token.
 *  4. Persiste tokens en [TokenStorage].
 *  5. Auto-refresh transparente antes de cada llamada a la API.
 *
 * Credenciales: registrar en portal.azure.com → App registrations → "Mobile and desktop
 * applications" redirect URI http://localhost:8766/callback.
 * Leer de env vars MICROSOFT_CLIENT_ID o de ~/.wakeywakey/config.properties.
 *
 * Nota: Microsoft no requiere client_secret para aplicaciones de escritorio públicas.
 */
class MicrosoftCalendarRepository : CalendarRepository {

    private val http = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true; isLenient = true })
        }
    }

    // calId string → Long estable (para poder revertir la conversión)
    private val calIdRegistry = mutableMapOf<Long, String>()

    // ── OAuth ─────────────────────────────────────────────────────────────────

    suspend fun authorize(): OAuthTokens {
        val verifier    = PkceHelper.generateCodeVerifier()
        val challenge   = PkceHelper.codeChallenge(verifier)
        val state       = PkceHelper.generateState()
        val callbackSrv = OAuthCallbackServer(CALLBACK_PORT).start()

        val authUrl = URLBuilder(
            "https://login.microsoftonline.com/common/oauth2/v2.0/authorize"
        ).apply {
            parameters.apply {
                append("client_id",             clientId)
                append("redirect_uri",          callbackSrv.redirectUri)
                append("response_type",         "code")
                append("scope",                 SCOPE)
                append("code_challenge",        challenge)
                append("code_challenge_method", "S256")
                append("state",                 state)
                append("response_mode",         "query")
            }
        }.buildString()

        Desktop.getDesktop().browse(URI(authUrl))

        val code = try { callbackSrv.awaitCode() } finally { callbackSrv.stop() }
        return exchangeCode(code, verifier, callbackSrv.redirectUri)
    }

    private suspend fun exchangeCode(code: String, verifier: String, redirectUri: String): OAuthTokens {
        val resp    = http.submitForm(
            url            = "https://login.microsoftonline.com/common/oauth2/v2.0/token",
            formParameters = parameters {
                append("client_id",     clientId)
                append("code",          code)
                append("code_verifier", verifier)
                append("grant_type",    "authorization_code")
                append("redirect_uri",  redirectUri)
                append("scope",         SCOPE)
            }
        )
        val rawJson = resp.bodyAsText()
        System.err.println("MS token response (${resp.status}): $rawJson")

        // Si Microsoft devuelve un error, lanzamos excepción con el mensaje real
        val errResp = runCatching { Json.decodeFromString<MsErrorResponse>(rawJson) }.getOrNull()
        if (errResp?.error != null) {
            throw Exception("Microsoft OAuth error: ${errResp.error} — ${errResp.errorDescription}")
        }

        val body  = Json { ignoreUnknownKeys = true; isLenient = true }.decodeFromString<MsTokenResponse>(rawJson)
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
            url            = "https://login.microsoftonline.com/common/oauth2/v2.0/token",
            formParameters = parameters {
                append("client_id",     clientId)
                append("refresh_token", tokens.refreshToken)
                append("grant_type",    "refresh_token")
                append("scope",         SCOPE)
            }
        )
        val body = resp.body<MsTokenResponse>()
        return tokens.copy(
            accessToken = body.accessToken,
            expiresAt   = System.currentTimeMillis() + body.expiresIn * 1_000L,
        ).also { TokenStorage.save(it) }
    }

    private suspend fun validToken(): String {
        var t = TokenStorage.load(PROVIDER)
            ?: throw IllegalStateException("Not authenticated with Microsoft")
        if (t.isExpired) t = refreshTokens(t)
        return t.accessToken
    }

    private suspend fun fetchEmail(accessToken: String): String = runCatching {
        http.get("https://graph.microsoft.com/v1.0/me") {
            bearerAuth(accessToken)
        }.body<MsUserInfo>().userPrincipalName
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
        val msCalId = calIdRegistry[cal.id] ?: return emptyList()

        val startIso = Instant.ofEpochMilli(fromTime)
            .let { OffsetDateTime.ofInstant(it, java.time.ZoneOffset.UTC) }
            .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        val endIso   = Instant.ofEpochMilli(toTime)
            .let { OffsetDateTime.ofInstant(it, java.time.ZoneOffset.UTC) }
            .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)

        val resp = http.get(
            "https://graph.microsoft.com/v1.0/me/calendars/$msCalId/calendarView"
        ) {
            bearerAuth(token)
            parameter("startDateTime", startIso)
            parameter("endDateTime",   endIso)
            parameter("\$top",         "50")
            parameter("\$orderby",     "start/dateTime")
            parameter("\$select",
                "id,subject,bodyPreview,body,start,end,location,isAllDay," +
                "onlineMeeting,onlineMeetingUrl,attendees,responseStatus")
        }
        val body = resp.body<MsEventsResponse>()

        return body.value
            .filter { if (!includeAllDay) !it.isAllDay else true }
            .mapNotNull { ev ->
                val start = parseMs(ev.start.dateTime)       ?: return@mapNotNull null
                val end   = parseMs(ev.end.dateTime)         ?: start
                val desc  = ev.body?.content ?: ev.bodyPreview
                val loc   = ev.location?.displayName?.takeIf { it.isNotBlank() }
                val link  = ev.onlineMeetingUrl
                    ?: ev.onlineMeeting?.joinUrl
                    ?: MeetingLinkDetector.extractFromEvent(desc, loc)

                CalendarEvent(
                    id                 = ev.id.stableId(),
                    title              = ev.subject?.takeIf { it.isNotBlank() } ?: "(No title)",
                    startTime          = start,
                    endTime            = end,
                    location           = loc,
                    description        = desc,
                    calendarId         = cal.id,
                    calendarName       = cal.name,
                    meetingLink        = link,
                    isAllDay           = ev.isAllDay,
                    selfAttendeeStatus = ev.responseStatus?.response?.toStatusInt() ?: 0,
                )
            }
    }

    override suspend fun getAvailableCalendars(): List<DeviceCalendar> {
        val token = validToken()
        // Resuelve email: primero del StateFlow reactivo, luego token, luego fallback.
        val accountEmail = CalendarAccountManager.connectedEmail(PROVIDER)?.takeIf { it.isNotBlank() } ?: "Microsoft"
        val resp  = http.get("https://graph.microsoft.com/v1.0/me/calendars") {
            bearerAuth(token)
            parameter("\$select", "id,name,color,hexColor,canEdit")
        }
        val body = resp.body<MsCalendarsResponse>()
        return body.value.map { cal ->
            val stableId = cal.id.stableId()
            calIdRegistry[stableId] = cal.id
            DeviceCalendar(
                id          = stableId,
                name        = cal.name.ifBlank { cal.id },
                accountName = accountEmail,
                color       = cal.hexColor?.hexToArgb() ?: 0xFF0078D4.toInt(),
                isVisible   = true,
            )
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Microsoft devuelve "2024-05-21T09:00:00.0000000" sin timezone para
     * calendarView (siempre en UTC cuando se usa $timeZone header, o en local).
     * Intentamos parsear como OffsetDateTime; si falla, asumimos UTC.
     */
    private fun parseMs(s: String?): Long? {
        s ?: return null
        return runCatching { OffsetDateTime.parse(s).toInstant().toEpochMilli() }
            .recover {
                // Formato sin offset → asumimos UTC
                java.time.LocalDateTime.parse(s.take(19), DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                    .toInstant(java.time.ZoneOffset.UTC).toEpochMilli()
            }.getOrNull()
    }

    private fun String.hexToArgb(): Int = runCatching {
        val hex = removePrefix("#")
        (0xFF000000.toInt()) or hex.toInt(16)
    }.getOrElse { 0xFF0078D4.toInt() }

    private fun String.toStatusInt(): Int = when (this) {
        "accepted"          -> 1
        "declined"          -> 2
        "tentativelyAccepted" -> 4
        else                -> 0
    }

    companion object {
        const val PROVIDER      = "microsoft"
        private const val CALLBACK_PORT = 8766
        private const val SCOPE =
            "offline_access Calendars.Read User.Read"

        val clientId: String get() = credential("MICROSOFT_CLIENT_ID") {
            BuildConfig.MICROSOFT_CLIENT_ID.takeIf { it.isNotBlank() }
        }

        val isConfigured: Boolean
            get() = runCatching { clientId; true }.getOrElse { false }
                 || TokenStorage.load(PROVIDER) != null

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

// ── Serialización Microsoft Graph API ────────────────────────────────────────

@Serializable private data class MsTokenResponse(
    @SerialName("access_token")  val accessToken:  String,
    @SerialName("refresh_token") val refreshToken: String? = null,
    @SerialName("expires_in")    val expiresIn:    Long    = 3600L,
)
@Serializable private data class MsUserInfo(
    val userPrincipalName: String = "",
    val mail:              String? = null,
)
@Serializable private data class MsEventsResponse(val value: List<MsEvent> = emptyList())
@Serializable private data class MsEvent(
    val id:                String,
    val subject:           String?          = null,
    val bodyPreview:       String?          = null,
    val body:              MsBody?          = null,
    val start:             MsDateTimeZone,
    val end:               MsDateTimeZone,
    val location:          MsLocation?      = null,
    val isAllDay:          Boolean          = false,
    val onlineMeetingUrl:  String?          = null,
    val onlineMeeting:     MsOnlineMeeting? = null,
    val attendees:         List<MsAttendee>? = null,
    val responseStatus:    MsResponseStatus? = null,
)
@Serializable private data class MsBody(
    val contentType: String = "text",
    val content:     String = "",
)
@Serializable private data class MsDateTimeZone(
    val dateTime: String? = null,
    val timeZone: String? = null,
)
@Serializable private data class MsLocation(val displayName: String = "")
@Serializable private data class MsOnlineMeeting(val joinUrl: String? = null)
@Serializable private data class MsAttendee(
    val emailAddress: MsEmailAddress = MsEmailAddress(),
    val type:         String         = "required",
    val status:       MsResponseStatus? = null,
)
@Serializable private data class MsEmailAddress(
    val address: String = "",
    val name:    String = "",
)
@Serializable private data class MsResponseStatus(val response: String = "none")
@Serializable private data class MsCalendarsResponse(val value: List<MsCalendar> = emptyList())
@Serializable private data class MsCalendar(
    val id:       String,
    val name:     String  = "",
    val hexColor: String? = null,
)

@Serializable private data class MsErrorResponse(
    @SerialName("error")             val error:            String? = null,
    @SerialName("error_description") val errorDescription: String? = null,
)

/** Hash string → Long estable (misma función que GoogleCalendarRepository). */
private fun String.stableId(): Long {
    var h = 7L
    for (c in this) h = h * 31L + c.code
    return h
}
