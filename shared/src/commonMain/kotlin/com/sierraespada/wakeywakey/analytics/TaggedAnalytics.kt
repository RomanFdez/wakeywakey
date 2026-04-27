package com.sierraespada.wakeywakey.analytics

/**
 * Decorator que inyecta propiedades base en todos los eventos.
 *
 * Usado para añadir `app: "wakeywakey"` a cada track/identify,
 * ya que el proyecto PostHog es compartido con otras apps de SierraEspada
 * y necesitamos poder filtrar por app en el dashboard.
 *
 * Uso:
 *   val analytics = TaggedAnalytics(realAnalytics, baseProperties = mapOf("app" to "wakeywakey"))
 */
class TaggedAnalytics(
    private val delegate: Analytics,
    private val baseProperties: Map<String, Any>
) : Analytics {

    override fun track(event: String, properties: Map<String, Any>) {
        delegate.track(event, baseProperties + properties) // las propiedades del caller tienen prioridad
    }

    override fun identify(userId: String, traits: Map<String, Any>) {
        delegate.identify(userId, baseProperties + traits)
    }

    override fun screen(screenName: String) {
        delegate.screen(screenName)
    }

    override fun flush() {
        delegate.flush()
    }
}
