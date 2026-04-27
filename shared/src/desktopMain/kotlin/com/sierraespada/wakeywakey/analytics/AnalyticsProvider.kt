package com.sierraespada.wakeywakey.analytics

// TODO Fase 5 (Windows): integrar PostHog JVM SDK cuando confirmemos coordenadas Maven.
// Por ahora implementación stub que loguea a consola en desarrollo.
actual object AnalyticsProvider {

    // TaggedAnalytics añade app:"wakeywakey" a todos los eventos automáticamente.
    // El proyecto PostHog es compartido entre apps de SierraEspada.
    private val analytics: Analytics = TaggedAnalytics(
        delegate       = StubAnalytics,
        baseProperties = mapOf("app" to "wakeywakey")
    )

    actual fun initialize(apiKey: String, host: String) {
        println("[Analytics] Initialized (stub) — apiKey=${apiKey.take(8)}…")
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
