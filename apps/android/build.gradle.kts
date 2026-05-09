import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)

    id("io.sentry.android.gradle") version "6.5.0"
}

// Lee secrets.properties (local, gitignoreado) o variables de entorno (CI)
val secrets = rootProject.file("secrets.properties")
    .takeIf { it.exists() }
    ?.let { Properties().apply { load(it.inputStream()) } }
    ?: Properties()

fun secret(key: String) = secrets.getProperty(key) ?: System.getenv(key) ?: ""

android {
    namespace   = "com.sierraespada.wakeywakey"
    compileSdk  = 35

    defaultConfig {
        applicationId = "com.sierraespada.wakeywakey"
        minSdk        = 26          // Android 8.0 — cubre ~95 % de dispositivos activos
        targetSdk     = 35
        versionCode   = 3
        versionName   = "0.1.1"

        // Secrets inyectados como BuildConfig — nunca hardcodeados en el código
        buildConfigField("String", "SENTRY_DSN",           "\"${secret("SENTRY_DSN")}\"")
        buildConfigField("String", "POSTHOG_API_KEY",      "\"${secret("POSTHOG_API_KEY")}\"")
        buildConfigField("String", "POSTHOG_HOST",         "\"${secret("POSTHOG_HOST").ifEmpty { "https://eu.i.posthog.com" }}\"")
        buildConfigField("String", "REVENUECAT_API_KEY",   "\"${secret("REVENUECAT_API_KEY")}\"")
    }

    signingConfigs {
        create("release") {
            val keystorePath = secret("KEYSTORE_PATH")
            if (keystorePath.isNotEmpty()) {
                storeFile = rootProject.file(keystorePath)
            }
            storePassword = secret("KEYSTORE_PASSWORD")
            keyAlias      = secret("KEY_ALIAS")
            keyPassword   = secret("KEY_PASSWORD")
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix   = "-debug"
        }
        release {
            isMinifyEnabled   = true
            isShrinkResources = true
            signingConfig     = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions { jvmTarget = "17" }

    buildFeatures {
        compose      = true
        buildConfig  = true   // necesario para acceder a BuildConfig.SENTRY_DSN etc.
    }
}

sentry {
    // Sentry sigue funcionando en runtime (crash reporting).
    // Solo desactivamos los uploads de artefactos — no tienen sentido sin auth token.
    autoUploadProguardMapping = false
    uploadNativeSymbols       = false
    autoUploadNativeSymbols   = false
    includeNativeSources      = false
    ignoredBuildTypes         = setOf("debug")
}

// Desactiva cualquier tarea de Sentry que intente subir algo a sus servidores.
// Se reactivarán en CI añadiendo SENTRY_AUTH_TOKEN a los secrets del repo.
afterEvaluate {
    val hasToken = secret("SENTRY_AUTH_TOKEN").isNotEmpty()
    if (!hasToken) {
        tasks.matching { task ->
            task.name.startsWith("sentry", ignoreCase = true) &&
            (task.name.contains("Upload", ignoreCase = true) ||
             task.name.contains("Bundle", ignoreCase = true) ||
             task.name.contains("Source", ignoreCase = true))
        }.configureEach { enabled = false }
    }
}

dependencies {
    implementation(project(":shared"))

    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material3:material3-window-size-class")
    implementation("androidx.compose.material:material-icons-extended")
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Crash + Analytics
    implementation(libs.sentry.android)
    implementation(libs.posthog.android)

    // Notificaciones + alarmas
    implementation("androidx.core:core-ktx:1.13.1")

    // Serialization (for ManualAlert JSON storage)
    implementation(libs.kotlinx.serialization.json)

    // Settings persistence
    implementation(libs.datastore.preferences)

    // Background sync
    implementation(libs.work.runtime.ktx)

    // Home widget (Glance)
    implementation(libs.glance.appwidget)
    implementation(libs.glance.material3)

    // Monetización — RevenueCat KMP
    implementation(libs.revenuecat.kmp)
}


sentry {
    org.set("sierra-espada")
    projectName.set("wakeywakeyandroid")

    // this will upload your source code to Sentry to show it as part of the stack traces
    // disable if you don't want to expose your sources
    includeSourceContext.set(true)
}
