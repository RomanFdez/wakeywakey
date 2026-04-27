package com.sierraespada.wakeywakey

import android.app.Application
import com.sierraespada.wakeywakey.analytics.AnalyticsProvider
import com.sierraespada.wakeywakey.crash.CrashReporter

class WakeyWakeyApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Crash reporting — Sentry
        CrashReporter.initialize(
            dsn         = BuildConfig.SENTRY_DSN,
            environment = if (BuildConfig.DEBUG) "debug" else "production"
        )

        // Analytics — PostHog
        AnalyticsProvider.initialize(
            apiKey = BuildConfig.POSTHOG_API_KEY,
            host   = BuildConfig.POSTHOG_HOST
        )
    }
}
