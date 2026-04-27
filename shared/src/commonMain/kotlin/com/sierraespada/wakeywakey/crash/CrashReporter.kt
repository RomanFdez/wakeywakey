package com.sierraespada.wakeywakey.crash

/**
 * expect/actual: cada plataforma provee su implementación real (Sentry).
 */
expect object CrashReporter {
    fun initialize(dsn: String, environment: String)
    fun captureException(throwable: Throwable, context: Map<String, Any> = emptyMap())
    fun setUser(id: String)
    fun clearUser()
}
