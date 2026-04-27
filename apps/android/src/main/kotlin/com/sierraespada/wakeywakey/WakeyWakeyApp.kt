package com.sierraespada.wakeywakey

import android.app.Application
import com.sierraespada.wakeywakey.analytics.AnalyticsProvider
import com.sierraespada.wakeywakey.analytics.Event

class WakeyWakeyApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Sentry se auto-inicializa desde AndroidManifest.xml (configurado por el wizard).
        // No hace falta llamar a CrashReporter.initialize() en Android.

        // Analytics — PostHog
        AnalyticsProvider.initialize(
            apiKey = BuildConfig.POSTHOG_API_KEY,
            host   = BuildConfig.POSTHOG_HOST
        )

        // Primer evento — confirma que la pipeline funciona end-to-end
        AnalyticsProvider.instance.track(Event.APP_OPENED)
    }
}
