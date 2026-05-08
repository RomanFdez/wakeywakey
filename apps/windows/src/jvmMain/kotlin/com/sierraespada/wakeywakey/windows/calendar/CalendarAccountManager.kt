package com.sierraespada.wakeywakey.windows.calendar

import com.sierraespada.wakeywakey.calendar.CalendarRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Gestiona las cuentas de calendario activas (Google y/o Microsoft).
 *
 * Soporte multi-cuenta: Google y Microsoft pueden estar conectados simultáneamente.
 * Los eventos se combinan en [CombinedCalendarRepository].
 *
 * Estado:
 *  - [activeProviders] — conjunto de providers conectados ("google", "microsoft")
 *  - [activeRepos]     — lista de repositorios listos para usar
 *  - [connectedEmails] — emails reactivos por provider; se actualiza en background
 *                        cuando los repositorios obtienen los calendarios.
 */
object CalendarAccountManager {

    // ── Estado observable ──────────────────────────────────────────────────────

    private val _activeProviders = MutableStateFlow<Set<String>>(emptySet())
    val activeProviders: StateFlow<Set<String>> = _activeProviders.asStateFlow()

    private val _activeRepos = MutableStateFlow<List<CalendarRepository>>(emptyList())
    val activeRepos: StateFlow<List<CalendarRepository>> = _activeRepos.asStateFlow()

    /**
     * Emails por provider. Reactivo: se actualiza cuando el repositorio resuelve
     * el email (p.ej. después de [getAvailableCalendars] o al conectar).
     * Usar en UI con [collectAsState] para que se recomponga automáticamente.
     */
    private val _connectedEmails = MutableStateFlow<Map<String, String>>(emptyMap())
    val connectedEmails: StateFlow<Map<String, String>> = _connectedEmails.asStateFlow()

    /** Repo combinado listo para usar; null si ninguna cuenta conectada. */
    val combinedRepo: CalendarRepository?
        get() = _activeRepos.value.let { repos ->
            when {
                repos.isEmpty() -> null
                repos.size == 1 -> repos.first()
                else            -> CombinedCalendarRepository(repos)
            }
        }

    val isConnected: Boolean get() = _activeProviders.value.isNotEmpty()

    fun isProviderConnected(provider: String): Boolean = provider in _activeProviders.value

    // ── Init ───────────────────────────────────────────────────────────────────

    init {
        restoreFromStorage()
    }

    private fun restoreFromStorage() {
        val providers = mutableSetOf<String>()
        val repos     = mutableListOf<CalendarRepository>()
        val emails    = mutableMapOf<String, String>()

        TokenStorage.load(GoogleCalendarRepository.PROVIDER)?.let { token ->
            providers += GoogleCalendarRepository.PROVIDER
            repos     += GoogleCalendarRepository()
            if (token.accountEmail.isNotBlank()) emails[GoogleCalendarRepository.PROVIDER] = token.accountEmail
        }
        TokenStorage.load(MicrosoftCalendarRepository.PROVIDER)?.let { token ->
            providers += MicrosoftCalendarRepository.PROVIDER
            repos     += MicrosoftCalendarRepository()
            if (token.accountEmail.isNotBlank()) emails[MicrosoftCalendarRepository.PROVIDER] = token.accountEmail
        }

        _activeProviders.value  = providers
        _activeRepos.value      = repos
        _connectedEmails.value  = emails
    }

    // ── Conexión de cuentas ────────────────────────────────────────────────────

    /** Inicia OAuth para Google Calendar. Añade al conjunto de providers activos. */
    suspend fun connectGoogle(): OAuthTokens {
        val repo   = GoogleCalendarRepository()
        val tokens = repo.authorize()
        addProvider(GoogleCalendarRepository.PROVIDER, repo)
        // El email viene en el token recién guardado
        if (tokens.accountEmail.isNotBlank()) {
            _connectedEmails.value = _connectedEmails.value + (GoogleCalendarRepository.PROVIDER to tokens.accountEmail)
        }
        return tokens
    }

    /** Inicia OAuth para Microsoft / Outlook. Añade al conjunto de providers activos. */
    suspend fun connectMicrosoft(): OAuthTokens {
        val repo   = MicrosoftCalendarRepository()
        val tokens = repo.authorize()
        addProvider(MicrosoftCalendarRepository.PROVIDER, repo)
        if (tokens.accountEmail.isNotBlank()) {
            _connectedEmails.value = _connectedEmails.value + (MicrosoftCalendarRepository.PROVIDER to tokens.accountEmail)
        }
        return tokens
    }

    /** Desconecta un proveedor específico y borra sus tokens. */
    fun disconnect(provider: String) {
        TokenStorage.clear(provider)
        val newProviders = _activeProviders.value - provider
        val newRepos     = _activeRepos.value.filter {
            when (provider) {
                GoogleCalendarRepository.PROVIDER    -> it !is GoogleCalendarRepository
                MicrosoftCalendarRepository.PROVIDER -> it !is MicrosoftCalendarRepository
                else -> true
            }
        }
        _activeProviders.value  = newProviders
        _activeRepos.value      = newRepos
        _connectedEmails.value  = _connectedEmails.value - provider
    }

    /** Desconecta TODOS los providers (retrocompatibilidad). */
    fun disconnect() {
        _activeProviders.value.forEach { TokenStorage.clear(it) }
        _activeProviders.value  = emptySet()
        _activeRepos.value      = emptyList()
        _connectedEmails.value  = emptyMap()
    }

    /**
     * Llamado por los repositorios cuando resuelven el email en background
     * (p.ej. durante [getAvailableCalendars]). Actualiza el StateFlow reactivo
     * y persiste el email en el token si aún no estaba guardado.
     */
    fun updateEmail(provider: String, email: String) {
        if (email.isBlank()) return
        // Actualiza el StateFlow → la UI se recompone automáticamente
        _connectedEmails.value = _connectedEmails.value + (provider to email)
        // Persiste en el token si falta
        val stored = TokenStorage.load(provider) ?: return
        if (stored.accountEmail.isBlank()) {
            TokenStorage.save(stored.copy(accountEmail = email))
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private fun addProvider(provider: String, repo: CalendarRepository) {
        val currentRepos = _activeRepos.value.filter {
            when (provider) {
                GoogleCalendarRepository.PROVIDER    -> it !is GoogleCalendarRepository
                MicrosoftCalendarRepository.PROVIDER -> it !is MicrosoftCalendarRepository
                else -> true
            }
        }
        _activeProviders.value = _activeProviders.value + provider
        _activeRepos.value     = currentRepos + repo
    }

    // ── Info de cuenta ─────────────────────────────────────────────────────────

    /** Email de un provider (snapshot no reactivo — preferir [connectedEmails] en UI). */
    fun connectedEmail(provider: String): String? =
        _connectedEmails.value[provider]?.takeIf { it.isNotBlank() }
            ?: TokenStorage.load(provider)?.accountEmail?.takeIf { it.isNotBlank() }

    /** Email de cualquier cuenta conectada (retrocompatibilidad). */
    val connectedEmail: String?
        get() = _activeProviders.value.firstOrNull()?.let { connectedEmail(it) }

    val canConnectGoogle: Boolean
        get() = GoogleCalendarRepository.isConfigured

    val canConnectMicrosoft: Boolean
        get() = MicrosoftCalendarRepository.isConfigured
}
