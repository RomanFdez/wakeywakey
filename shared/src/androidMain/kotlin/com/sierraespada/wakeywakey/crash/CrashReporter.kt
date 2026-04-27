package com.sierraespada.wakeywakey.crash

import io.sentry.Sentry

actual object CrashReporter {

    actual fun initialize(dsn: String, environment: String) {
        // Sentry se auto-inicializa desde AndroidManifest.xml (meta-data io.sentry.dsn).
        // Aquí solo añadimos el tag de entorno para que los errores lleguen bien clasificados.
        if (dsn.isBlank()) return
        Sentry.configureScope { scope ->
            scope.setTag("environment", environment)
        }
    }

    actual fun captureException(throwable: Throwable, context: Map<String, Any>) {
        Sentry.withScope { scope ->
            context.forEach { (key, value) -> scope.setExtra(key, value.toString()) }
            Sentry.captureException(throwable)
        }
    }

    actual fun setUser(id: String) {
        Sentry.setUser(io.sentry.protocol.User().apply { this.id = id })
    }

    actual fun clearUser() {
        Sentry.setUser(null)
    }
}
