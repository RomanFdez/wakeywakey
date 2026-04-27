package com.sierraespada.wakeywakey.analytics

/**
 * Contrato común de analytics — implementado por cada plataforma.
 * El código compartido solo usa esta interfaz, nunca PostHog directamente.
 */
interface Analytics {
    fun track(event: String, properties: Map<String, Any> = emptyMap())
    fun identify(userId: String, traits: Map<String, Any> = emptyMap())
    fun screen(screenName: String)
    fun flush()
}

// Eventos estándar — úsalos en lugar de strings sueltos para evitar typos
object Event {
    const val APP_OPENED          = "app_opened"
    const val ONBOARDING_STARTED  = "onboarding_started"
    const val ONBOARDING_COMPLETE = "onboarding_completed"
    const val PERMISSION_GRANTED  = "permission_granted"
    const val PERMISSION_DENIED   = "permission_denied"
    const val ALERT_SHOWN         = "alert_shown"
    const val ALERT_DISMISSED     = "alert_dismissed"
    const val ALERT_SNOOZED       = "alert_snoozed"
    const val JOIN_CALL_TAPPED    = "join_call_tapped"
    const val PAYWALL_SHOWN       = "paywall_shown"
    const val SUBSCRIPTION_STARTED = "subscription_started"
    const val SETTINGS_CHANGED    = "settings_changed"
}
