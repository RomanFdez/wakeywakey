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
    const val PAYWALL_SHOWN        = "paywall_shown"
    const val PAYWALL_DISMISSED    = "paywall_dismissed"
    const val PLAN_SELECTED        = "plan_selected"        // properties: plan (monthly|annual|lifetime)
    const val PURCHASE_STARTED     = "purchase_started"     // properties: plan
    const val PURCHASE_COMPLETED   = "purchase_completed"   // properties: plan
    const val PURCHASE_FAILED      = "purchase_failed"      // properties: plan
    const val PURCHASE_RESTORED    = "purchase_restored"
    const val TRIAL_STARTED        = "trial_started"        // first install, trial begins
    const val TRIAL_EXPIRED        = "trial_expired"        // trialDaysLeft reaches 0
    const val SUBSCRIPTION_STARTED = "subscription_started"
    const val SETTINGS_CHANGED     = "settings_changed"
    const val FREE_TIER_HIT        = "free_tier_hit"        // user hits 1-cal/3-event limit
}
