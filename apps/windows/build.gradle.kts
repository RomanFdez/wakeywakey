import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
}

val appVersion: String = (project.findProperty("appVersion") as String?) ?: "0.1.0"

kotlin {
    jvm {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    sourceSets {
        val jvmMain by getting {
            dependencies {
                implementation(project(":shared"))
                implementation(compose.desktop.currentOs)
                implementation(compose.material3)
                implementation(compose.materialIconsExtended)
                implementation(libs.kotlinx.coroutines.swing)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.ktor.client.cio)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.serialization.json)
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.sierraespada.wakeywakey.windows.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Msi, TargetFormat.Exe)
            packageName    = "WakeyWakey"
            packageVersion = appVersion
            description    = "Full-screen meeting alerts you can't miss"
            vendor         = "SierraEspada"
            copyright      = "© 2026 SierraEspada"

            windows {
                menuGroup   = "WakeyWakey"
                upgradeUuid = "1EF21982-0C82-4462-9C32-E0BD8369C7BE"
            }
        }
    }
}
