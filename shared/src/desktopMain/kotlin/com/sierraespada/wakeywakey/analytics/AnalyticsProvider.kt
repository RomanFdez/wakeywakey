package com.sierraespada.wakeywakey.analytics

import com.posthog.java.PostHog

actual object AnalyticsProvider {

    private lateinit var analytics: Analytics

    actual fun initialize(apiKey: String, host: String) {
        val client = PostHog.Builder(apiKey).host(host).build()
        analytics = PostHogDesktopAnalytics(client)
    }

    actual val instance: Analytics get() = analytics
}

private class PostHogDesktopAnalytics(private val client: PostHog) : Analytics {

    override fun track(event: String, properties: Map<String, Any>) {
        client.capture("desktop_user", event, properties)
    }

    override fun identify(userId: String, traits: Map<String, Any>) {
        client.identify(userId, traits, emptyMap())
    }

    override fun screen(screenName: String) {
        client.capture("desktop_user", "screen_viewed", mapOf("screen" to screenName))
    }

    override fun flush() {
        client.shutdown()
    }
}
