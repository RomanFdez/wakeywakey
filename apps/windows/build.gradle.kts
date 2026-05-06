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
                // JNA removed — using Swift CalendarHelper binary instead
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.sierraespada.wakeywakey.windows.MainKt"

        // Temurin 25 has jpackage; Android Studio's JBR does not.
        // Used only for createDistributable / packageDmg tasks, not for compilation.
        javaHome = "/Library/Java/JavaVirtualMachines/temurin-25.jdk/Contents/Home"

        nativeDistributions {
            targetFormats(TargetFormat.Msi, TargetFormat.Exe, TargetFormat.Dmg)
            packageName    = "WakeyWakey"
            packageVersion = appVersion
            description    = "Full-screen meeting alerts you can't miss"
            vendor         = "SierraEspada"
            copyright      = "© 2026 SierraEspada"

            windows {
                menuGroup   = "WakeyWakey"
                upgradeUuid = "1EF21982-0C82-4462-9C32-E0BD8369C7BE"
            }

            macOS {
                bundleID          = "com.sierraespada.wakeywakey"
                packageVersion    = "1.0.0"   // macOS requires MAJOR > 0
                dmgPackageVersion = "1.0.0"
                iconFile.set(project.file("src/jvmMain/resources/WakeyWakey.icns"))
                // Allows Calendar full-access read via AppleScript / osascript
                entitlementsFile.set(project.file("macos-entitlements.plist"))
                // NSCalendarsFullAccessUsageDescription triggers "Full access" dialog
                // instead of the "add events only" dialog (macOS 14+)
                infoPlist {
                    extraKeysRawXml = """
                        <key>NSCalendarsFullAccessUsageDescription</key>
                        <string>WakeyWakey needs full calendar access to read your upcoming meetings and alert you before they start.</string>
                        <key>NSCalendarsUsageDescription</key>
                        <string>WakeyWakey needs calendar access to read your upcoming meetings.</string>
                        <key>NSAppleEventsUsageDescription</key>
                        <string>WakeyWakey uses AppleScript to read your calendar events from the macOS Calendar app.</string>
                    """.trimIndent()
                }
            }
        }

        jvmArgs("-Xdock:name=WakeyWakey")
    }
}

// ── Compile Swift CalendarHelper and inject into .app bundle ─────────────────
// Runs only on macOS; produces a native arm64 EventKit binary bundled inside the .app.
// EventKit TCC checks use "responsible process" → WakeyWakey.app's Calendar permission applies.

val swiftSrc  = "src/jvmMain/swift/CalendarHelper.swift"
val helperOut = "${buildDir}/swift/CalendarHelper"
val appMacOS  = "${buildDir}/compose/binaries/main/app/WakeyWakey.app/Contents/MacOS"

val compileCalendarHelper by tasks.registering(Exec::class) {
    group       = "build"
    description = "Compiles CalendarHelper.swift → arm64 binary"
    inputs.file(swiftSrc)
    outputs.file(helperOut)
    executable  = "swiftc"
    args(
        swiftSrc,
        "-framework", "EventKit",
        "-framework", "Foundation",
        "-target", "arm64-apple-macosx13.0",
        "-O", "-o", helperOut,
    )
    doFirst { file(helperOut).parentFile.mkdirs() }
}

val injectCalendarHelper by tasks.registering(Copy::class) {
    group       = "build"
    description = "Copies CalendarHelper into WakeyWakey.app/Contents/MacOS/"
    dependsOn(compileCalendarHelper)
    from(helperOut)
    into(appMacOS)
    fileMode    = 0b111_101_101   // rwxr-xr-x
}

afterEvaluate {
    tasks.matching { it.name == "createDistributable" }.configureEach {
        finalizedBy(injectCalendarHelper)
    }
}
