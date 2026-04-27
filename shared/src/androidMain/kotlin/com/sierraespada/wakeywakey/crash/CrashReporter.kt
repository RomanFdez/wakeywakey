package com.sierraespada.wakeywakey.crash

import io.sentry.Sentry
import io.sentry.SentryLevel
import io.sentry.android.core.SentryAndroid

actual object CrashReporter {

    actual fun initialize(dsn: String, environment: String) {
        if (dsn.isBlank()) return  // sin DSN en debug local → no inicializar
        SentryAndroid.init { options ->
            options.dsn         = dsn
            options.environment = environment
            options.isEnableUserInteractionTracing = true
            options.tracesSampleRate = if (environment == "production") 0.2 else 1.0
        }
    }

    actual fun captureException(throwable: Throwable, context: Map<String, Any>) {
        Sentry.captureException(throwable) { scope ->
            context.forEach { (key, value) -> scope.setExtra(key, value.toString()) }
        }
    }

    actual fun setUser(id: String) {
        Sentry.setUser(io.sentry.protocol.User().apply { this.id = id })
    }

    actual fun clearUser() {
        Sentry.setUser(null)
    }
}
