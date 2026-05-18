package com.sierraespada.wakeywakey.windows

/**
 * Credenciales OAuth compiladas en el binario para distribución.
 *
 * Las credenciales se generan en AppBuildConfig.kt (build/generated/) a partir de:
 *   - Release local : ~/.gradle/gradle.properties  (googleClientId / googleClientSecret / microsoftClientId)
 *   - CI            : -PgoogleClientId=... pasado por el workflow desde GitHub Secrets
 *   - Dev           : cadenas vacías → fallback a ~/.wakeywakey/config.properties en runtime
 */
internal object BuildConfig {
    const val VERSION = "1.0.0"

    // Delega en AppBuildConfig (generado en build time con las credenciales reales)
    val GOOGLE_CLIENT_ID:     String get() = AppBuildConfig.GOOGLE_CLIENT_ID
    val GOOGLE_CLIENT_SECRET: String get() = AppBuildConfig.GOOGLE_CLIENT_SECRET
    val MICROSOFT_CLIENT_ID:  String get() = AppBuildConfig.MICROSOFT_CLIENT_ID
}
