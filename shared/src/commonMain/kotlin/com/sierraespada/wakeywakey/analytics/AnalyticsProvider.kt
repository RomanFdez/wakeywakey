package com.sierraespada.wakeywakey.analytics

/**
 * expect/actual: cada plataforma provee su implementación real.
 * En commonMain solo se declara el contrato.
 */
expect object AnalyticsProvider {
    fun initialize(apiKey: String, host: String)
    val instance: Analytics
}
