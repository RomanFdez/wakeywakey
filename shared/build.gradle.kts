import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.android.library)
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions { jvmTarget = "17" }
        }
    }

    jvm("desktop") // Windows (Compose Desktop)

    // Fase 8 — iOS
    val xcf = XCFramework("Shared")
    listOf(iosX64(), iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "Shared"
            isStatic = false
            xcf.add(this)
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            // materialIconsExtended removed — saves 36 MB on Desktop; icons defined locally in WakeyWakeyIcons.kt
            implementation(compose.components.resources)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.supabase.postgrest)
            implementation(libs.supabase.auth)
            implementation(libs.supabase.realtime)
        }

        androidMain.dependencies {
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.koin.android)
            implementation(libs.revenuecat.kmp)
            implementation(libs.sentry.android)
            implementation(libs.posthog.android) // AnalyticsProvider (androidMain) usa PostHog SDK
        }

        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(libs.kotlinx.coroutines.swing)
                implementation(libs.sentry.jvm)
                // posthog-java añadir en Fase 5 (Windows) cuando confirmemos coordenadas Maven correctas
            }
        }

        // iosMain: sin dependencias externas.
        // IosCalendarRepository → platform.EventKit (Kotlin/Native built-in)
        // IosAlarmScheduler     → platform.UserNotifications (Kotlin/Native built-in)
        // CrashReporter/Analytics → stubs; Sentry + PostHog vía SPM desde apps/ios
    }
}

android {
    namespace  = "com.sierraespada.wakeywakey.shared"
    compileSdk = 35
    defaultConfig { minSdk = 26 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
