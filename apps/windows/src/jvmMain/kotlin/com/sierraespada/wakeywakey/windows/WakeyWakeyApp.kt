package com.sierraespada.wakeywakey.windows

import com.sierraespada.wakeywakey.analytics.AnalyticsProvider
import com.sierraespada.wakeywakey.analytics.Event
import com.sierraespada.wakeywakey.crash.CrashReporter

/**
 * Inicialización global de la app Windows.
 *
 * Sentry DSN y PostHog API key se leen de system properties o variables de entorno
 * (mismo patrón que Android — jamás hardcodeados).
 *
 * En desarrollo (sin valores) ambos SDKs actúan como stubs que logean a consola.
 */
object WakeyWakeyApp {

    fun init() {
        val sentryDsn    = System.getProperty("SENTRY_DSN")    ?: System.getenv("SENTRY_DSN")    ?: ""
        val posthogKey   = System.getProperty("POSTHOG_API_KEY") ?: System.getenv("POSTHOG_API_KEY") ?: ""
        val posthogHost  = System.getProperty("POSTHOG_HOST")  ?: System.getenv("POSTHOG_HOST")  ?: "https://eu.i.posthog.com"
        val env          = System.getProperty("APP_ENV")        ?: System.getenv("APP_ENV")        ?: "development"

        CrashReporter.initialize(dsn = sentryDsn, environment = env)
        AnalyticsProvider.initialize(apiKey = posthogKey, host = posthogHost)
        AnalyticsProvider.instance.track(Event.APP_OPENED)
    }
}
