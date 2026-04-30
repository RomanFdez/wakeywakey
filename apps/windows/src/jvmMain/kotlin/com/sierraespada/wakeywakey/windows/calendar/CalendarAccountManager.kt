package com.sierraespada.wakeywakey.windows.calendar

import com.sierraespada.wakeywakey.calendar.CalendarRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Gestiona la cuenta de calendario activa (Google o Microsoft).
 *
 * Estado:
 *  - [activeProvider] — "google" | "microsoft" | null (sin conectar)
 *  - [activeRepo]     — implementación de [CalendarRepository] lista para usar
 *
 * Al arrancar detecta automáticamente si hay tokens guardados.
 */
object CalendarAccountManager {

    // ── Estado observable ──────────────────────────────────────────────────────

    private val _activeProvider = MutableStateFlow<String?>(null)
    val activeProvider: StateFlow<String?> = _activeProvider.asStateFlow()

    /** Repositorio listo para usar; null si no hay cuenta conectada. */
    private val _activeRepo = MutableStateFlow<CalendarRepository?>(null)
    val activeRepo: StateFlow<CalendarRepository?> = _activeRepo.asStateFlow()

    val isConnected: Boolean get() = _activeProvider.value != null

    // ── Init ───────────────────────────────────────────────────────────────────

    init {
        // Detecta tokens guardados al arrancar la app
        restoreFromStorage()
    }

    private fun restoreFromStorage() {
        val tokens = TokenStorage.loadAny() ?: return
        setProvider(tokens.provider)
    }

    // ── Conexión de cuentas ────────────────────────────────────────────────────

    /**
     * Inicia el flujo OAuth para Google Calendar.
     * Suspende hasta que el usuario completa el consentimiento.
     * @throws IllegalStateException si GOOGLE_CLIENT_ID / GOOGLE_CLIENT_SECRET no están configurados.
     */
    suspend fun connectGoogle(): OAuthTokens {
        val repo   = GoogleCalendarRepository()
        val tokens = repo.authorize()           // abre browser + espera redirect
        setProvider(GoogleCalendarRepository.PROVIDER, repo)
        return tokens
    }

    /**
     * Inicia el flujo OAuth para Microsoft / Outlook.
     * Suspende hasta que el usuario completa el consentimiento.
     * @throws IllegalStateException si MICROSOFT_CLIENT_ID no está configurado.
     */
    suspend fun connectMicrosoft(): OAuthTokens {
        val repo   = MicrosoftCalendarRepository()
        val tokens = repo.authorize()
        setProvider(MicrosoftCalendarRepository.PROVIDER, repo)
        return tokens
    }

    /** Desconecta la cuenta activa y borra los tokens guardados. */
    fun disconnect() {
        _activeProvider.value?.let { TokenStorage.clear(it) }
        _activeProvider.value = null
        _activeRepo.value     = null
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private fun setProvider(provider: String, repo: CalendarRepository? = null) {
        _activeProvider.value = provider
        _activeRepo.value     = repo ?: when (provider) {
            GoogleCalendarRepository.PROVIDER    -> GoogleCalendarRepository()
            MicrosoftCalendarRepository.PROVIDER -> MicrosoftCalendarRepository()
            else -> null
        }
    }

    // ── Info de cuenta ─────────────────────────────────────────────────────────

    /** Email de la cuenta conectada, o null si no hay. */
    val connectedEmail: String?
        get() = _activeProvider.value?.let { TokenStorage.load(it)?.accountEmail }

    /** true si Google Calendar puede iniciar OAuth (credenciales configuradas). */
    val canConnectGoogle: Boolean
        get() = GoogleCalendarRepository.isConfigured

    /** true si Microsoft Calendar puede iniciar OAuth (credenciales configuradas). */
    val canConnectMicrosoft: Boolean
        get() = MicrosoftCalendarRepository.isConfigured
}
