package com.sierraespada.wakeywakey.analytics

import com.posthog.android.PostHog
import com.posthog.android.PostHogAndroidConfig

actual object AnalyticsProvider {

    private lateinit var analytics: Analytics

    actual fun initialize(apiKey: String, host: String) {
        val config = PostHogAndroidConfig(apiKey, host)
        PostHog.setup(config)
        analytics = PostHogAnalytics
    }

    actual val instance: Analytics get() = analytics
}

private object PostHogAnalytics : Analytics {
    override fun track(event: String, properties: Map<String, Any>) {
        PostHog.capture(event, properties = properties)
    }

    override fun identify(userId: String, traits: Map<String, Any>) {
        PostHog.identify(userId, userProperties = traits)
    }

    override fun screen(screenName: String) {
        PostHog.screen(screenName)
    }

    override fun flush() {
        PostHog.flush()
    }
}
