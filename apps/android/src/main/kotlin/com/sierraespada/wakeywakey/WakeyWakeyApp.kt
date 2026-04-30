package com.sierraespada.wakeywakey

import android.app.Application
import com.posthog.android.PostHogAndroid
import com.posthog.android.PostHogAndroidConfig
import com.revenuecat.purchases.kmp.Purchases
import com.revenuecat.purchases.kmp.PurchasesConfiguration
import com.sierraespada.wakeywakey.analytics.AnalyticsProvider
import com.sierraespada.wakeywakey.analytics.Event
import com.sierraespada.wakeywakey.billing.EntitlementManager

class WakeyWakeyApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Sentry se auto-inicializa desde AndroidManifest.xml (meta-data io.sentry.dsn).

        // PostHog — necesita Application context, se inicializa aquí antes que cualquier evento.
        if (BuildConfig.POSTHOG_API_KEY.isNotBlank()) {
            PostHogAndroid.setup(
                context = this,
                config  = PostHogAndroidConfig(
                    apiKey = BuildConfig.POSTHOG_API_KEY,
                    host   = BuildConfig.POSTHOG_HOST
                )
            )
        }

        // AnalyticsProvider.initialize() es no-op en Android (PostHog ya está listo arriba).
        AnalyticsProvider.initialize(
            apiKey = BuildConfig.POSTHOG_API_KEY,
            host   = BuildConfig.POSTHOG_HOST
        )

        // Primer evento — confirma que la pipeline funciona end-to-end
        AnalyticsProvider.instance.track(Event.APP_OPENED)

        // RevenueCat — inicializar SDK y cargar estado de entitlement + trial
        if (BuildConfig.REVENUECAT_API_KEY.isNotBlank()) {
            Purchases.configure(
                PurchasesConfiguration(
                    apiKey = BuildConfig.REVENUECAT_API_KEY,
                )
            )
        }
        // init() calcula trial de app Y carga estado RevenueCat
        EntitlementManager.init(this)
    }
}
