package com.sierraespada.wakeywakey.analytics

import com.posthog.PostHog

actual object AnalyticsProvider {

    // TaggedAnalytics inyecta app:"wakeywakey" en todos los eventos automáticamente.
    // PostHog.setup() ya fue llamado desde WakeyWakeyApp (necesita Context).
    private val analytics: Analytics = TaggedAnalytics(
        delegate       = PostHogAnalytics,
        baseProperties = mapOf("app" to "wakeywakey")
    )

    actual fun initialize(apiKey: String, host: String) {
        // No-op: PostHogAndroid.setup() se llama desde WakeyWakeyApp con Application context.
        // Esta función existe para satisfacer el contrato expect/actual del módulo shared.
    }

    actual val instance: Analytics get() = analytics
}

private object PostHogAnalytics : Analytics {

    override fun track(event: String, properties: Map<String, Any>) {
        PostHog.capture(
            event      = event,
            properties = properties.takeIf { it.isNotEmpty() }
        )
    }

    override fun identify(userId: String, traits: Map<String, Any>) {
        PostHog.identify(
            distinctId     = userId,
            userProperties = traits.takeIf { it.isNotEmpty() }
        )
    }

    override fun screen(screenName: String) {
        PostHog.screen(screenTitle = screenName)
    }

    override fun flush() {
        PostHog.flush()
    }
}
