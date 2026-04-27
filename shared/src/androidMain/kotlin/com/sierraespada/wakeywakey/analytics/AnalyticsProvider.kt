package com.sierraespada.wakeywakey.analytics

// TODO Fase 1: integrar PostHog Android SDK con Context.
// PostHog Android v3 requiere Application context en setup() — refactorizar
// cuando tengamos el Application class completamente inicializado.
actual object AnalyticsProvider {

    // TaggedAnalytics añade app:"wakeywakey" a todos los eventos automáticamente.
    // El proyecto PostHog es compartido entre apps de SierraEspada.
    private val analytics: Analytics = TaggedAnalytics(
        delegate       = StubAnalytics,
        baseProperties = mapOf("app" to "wakeywakey")
    )

    actual fun initialize(apiKey: String, host: String) {
        println("[Analytics] Initialized (Android stub) — apiKey=${apiKey.take(8)}…")
        // TODO: PostHog.setup(context, PostHogAndroidConfig(apiKey, host))
    }

    actual val instance: Analytics get() = analytics
}

private object StubAnalytics : Analytics {
    override fun track(event: String, properties: Map<String, Any>) {
        println("[Analytics] track: $event $properties")
    }

    override fun identify(userId: String, traits: Map<String, Any>) {
        println("[Analytics] identify: $userId $traits")
    }

    override fun screen(screenName: String) {
        println("[Analytics] screen: $screenName")
    }

    override fun flush() {}
}
