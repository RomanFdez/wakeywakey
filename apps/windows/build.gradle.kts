import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    jvm {
        compilations.all { kotlinOptions.jvmTarget = "17" }
        withJava()
    }

    sourceSets {
        val jvmMain by getting {
            dependencies {
                implementation(project(":shared"))
                implementation(compose.desktop.currentOs)
                implementation(libs.kotlinx.coroutines.swing)
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
            packageVersion = "0.1.0"
            description    = "Full-screen meeting alerts you can't miss"
            vendor         = "SierraEspada"
            copyright      = "© 2026 SierraEspada"

            windows {
                menuGroup   = "WakeyWakey"
                upgradeUuid = "1EF21982-0C82-4462-9C32-E0BD8369C7BE"
                // iconFile.set(project.file("src/main/resources/icon.ico"))  // TODO: añadir icon.ico antes del release
            }
        }
    }
}
