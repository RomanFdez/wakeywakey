plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
}

// Lee secrets.properties (local, gitignoreado) o variables de entorno (CI)
val secrets = rootProject.file("secrets.properties")
    .takeIf { it.exists() }
    ?.let { java.util.Properties().apply { load(it.inputStream()) } }
    ?: java.util.Properties()

fun secret(key: String) = secrets.getProperty(key) ?: System.getenv(key) ?: ""

android {
    namespace   = "com.sierraespada.wakeywakey"
    compileSdk  = 35

    defaultConfig {
        applicationId = "com.sierraespada.wakeywakey"
        minSdk        = 26          // Android 8.0 — cubre ~95 % de dispositivos activos
        targetSdk     = 35
        versionCode   = 1
        versionName   = "0.1.0"

        // Secrets inyectados como BuildConfig — nunca hardcodeados en el código
        buildConfigField("String", "SENTRY_DSN",       "\"${secret("SENTRY_DSN")}\"")
        buildConfigField("String", "POSTHOG_API_KEY",  "\"${secret("POSTHOG_API_KEY")}\"")
        buildConfigField("String", "POSTHOG_HOST",     "\"${secret("POSTHOG_HOST").ifEmpty { "https://eu.i.posthog.com" }}\"")
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix   = "-debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
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

dependencies {
    implementation(project(":shared"))

    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Crash + Analytics
    implementation(libs.sentry.android)
    implementation(libs.posthog.android)
}
