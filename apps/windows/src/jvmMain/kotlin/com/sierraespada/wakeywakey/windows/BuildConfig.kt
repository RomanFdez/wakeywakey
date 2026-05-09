package com.sierraespada.wakeywakey.windows

/**
 * Credenciales OAuth compiladas en el binario para distribución.
 *
 * En DESARROLLO: dejar vacío — el código cae al fichero
 *   ~/.wakeywakey/config.properties como fallback.
 *
 * En PRODUCCIÓN (CI/CD): el workflow de release sobreescribe este fichero
 *   con los valores reales antes de compilar el MSI/EXE:
 *
 *   sed -i "s|GOOGLE_CLIENT_ID_PLACEHOLDER|$GOOGLE_CLIENT_ID|g" BuildConfig.kt
 *
 *   O usando el script de CI incluido en .github/workflows/release.yml.
 */
internal object BuildConfig {
    const val VERSION              = "1.0.0"
    const val GOOGLE_CLIENT_ID     = ""   // Sobreescrito por CI en producción
    const val GOOGLE_CLIENT_SECRET = ""   // Sobreescrito por CI en producción
    const val MICROSOFT_CLIENT_ID  = ""   // Sobreescrito por CI en producción
}
