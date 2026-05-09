import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
}

val appVersion: String  = (project.findProperty("appVersion") as String?) ?: "0.1.0"
val isRelease:  Boolean = project.hasProperty("release")

// ── Genera AppBuildConfig.kt con IS_RELEASE como const val ───────────────────
// El compilador Kotlin elimina los bloques `if (!IS_RELEASE)` en release builds.
val generateBuildConfig by tasks.registering {
    val outputDir = layout.buildDirectory.dir("generated/buildconfig/kotlin")
    outputs.dir(outputDir)
    doLast {
        val dir = outputDir.get().asFile.also { it.mkdirs() }
        File(dir, "AppBuildConfig.kt").writeText(
            """
            package com.sierraespada.wakeywakey.windows

            object AppBuildConfig {
                /** true en builds de release (-Prelease). Elimina todo el código dev en producción. */
                const val IS_RELEASE: Boolean = $isRelease
            }
            """.trimIndent()
        )
    }
}

kotlin {
    jvm {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    sourceSets {
        val jvmMain by getting {
            kotlin.srcDir(generateBuildConfig)
            dependencies {
                implementation(project(":shared"))
                implementation(compose.desktop.currentOs)
                implementation(compose.material3)
                // Icons core (~864 KB) en vez de Extended (~36 MB). Los 6 iconos Extended están en WakeyWakeyIcons.kt
                implementation("org.jetbrains.compose.material:material-icons-core:1.7.3")
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
        // macOS local dev: point at Temurin 25. On Windows / Linux CI, use the CI's Java (JAVA_HOME).
        val temurin25 = "/Library/Java/JavaVirtualMachines/temurin-25.jdk/Contents/Home"
        if (org.gradle.internal.os.OperatingSystem.current().isMacOsX && file(temurin25).exists()) {
            javaHome = temurin25
        }

        nativeDistributions {
            targetFormats(TargetFormat.Msi, TargetFormat.Exe, TargetFormat.Dmg)
            packageName    = "WakeyWakey"
            packageVersion = appVersion
            description    = "Full-screen meeting alerts you can't miss"
            vendor         = "SierraEspada"
            copyright      = "© 2026 SierraEspada"

            windows {
                menuGroup      = "WakeyWakey"
                upgradeUuid    = "1EF21982-0C82-4462-9C32-E0BD8369C7BE"
                iconFile.set(project.file("src/jvmMain/resources/WakeyWakey.ico"))
                // Aparece en Inicio → Agregar o quitar programas
                shortcut       = true
                dirChooser     = true
                perUserInstall = false   // instala para todos los usuarios (requiere elevación)
            }

            macOS {
                bundleID          = "com.sierraespada.wakeywakey"
                packageVersion    = "1.0.0"   // macOS requires MAJOR > 0
                dmgPackageVersion = "1.0.0"
                iconFile.set(project.file("src/jvmMain/resources/WakeyWakey.icns"))
                // Allows Calendar full-access read via AppleScript / osascript
                entitlementsFile.set(project.file("macos-entitlements.plist"))
                // Firma y notarización se hacen manualmente tras el build
                // (el certificado está en System keychain, no accesible desde jpackage)
                // NSCalendarsFullAccessUsageDescription triggers "Full access" dialog
                // instead of the "add events only" dialog (macOS 14+)
                infoPlist {
                    extraKeysRawXml = """
                        <key>LSUIElement</key>
                        <true/>
                        <key>NSCalendarsFullAccessUsageDescription</key>
                        <string>WakeyWakey needs full calendar access to read your upcoming meetings and alert you before they start.</string>
                        <key>NSCalendarsUsageDescription</key>
                        <string>WakeyWakey needs calendar access to read your upcoming meetings.</string>
                        <key>NSAppleEventsUsageDescription</key>
                        <string>WakeyWakey uses AppleScript to read your calendar events from the macOS Calendar app.</string>
                        <key>CFBundleURLTypes</key>
                        <array>
                            <dict>
                                <key>CFBundleURLSchemes</key>
                                <array>
                                    <string>wakeywakey</string>
                                </array>
                                <key>CFBundleURLName</key>
                                <string>com.sierraespada.wakeywakey</string>
                            </dict>
                        </array>
                    """.trimIndent()
                }
            }
        }

        jvmArgs(
            "-Xdock:name=WakeyWakey",
            "-Dapple.awt.UIElement=true",   // suppress Dock icon before AWT init
        )
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
    description = "Compiles CalendarHelper.swift → arm64 binary (macOS only)"
    onlyIf { org.gradle.internal.os.OperatingSystem.current().isMacOsX }
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
    description = "Copies CalendarHelper into WakeyWakey.app/Contents/MacOS/ (macOS only)"
    onlyIf { org.gradle.internal.os.OperatingSystem.current().isMacOsX }
    dependsOn(compileCalendarHelper)
    from(helperOut)
    into(appMacOS)
    fileMode    = 0b111_101_101   // rwxr-xr-x
}

// ── Codesign deep: firma todo el contenido interno antes de empaquetar el DMG ─
// jpackage no re-firma el JRE embebido ni los helpers que inyectamos. Sin esto,
// la notarización falla con "binary is not signed with a valid Developer ID"
// y "signature does not include a secure timestamp".
//
// Identidad: -PmacosSigningIdentity=<SHA1 o "Developer ID Application: Nombre (TEAMID)">
// o variable de entorno MACOS_SIGNING_IDENTITY. Si falta, la tarea se omite
// (útil en CI de Linux/Windows y en builds de dev).

val macosSigningIdentity: String? =
    (project.findProperty("macosSigningIdentity") as String?)
        ?: System.getenv("MACOS_SIGNING_IDENTITY")

val appBundle      = "${buildDir}/compose/binaries/main/app/WakeyWakey.app"
val entitlements   = "macos-entitlements.plist"

val signMacApp by tasks.registering {
    group       = "build"
    description = "Firma recursivamente WakeyWakey.app (dylibs, helpers, runtime, binario principal) con hardened runtime y timestamp seguro."
    dependsOn(injectCalendarHelper)
    onlyIf {
        val isMac = org.gradle.internal.os.OperatingSystem.current().isMacOsX
        val hasId = !macosSigningIdentity.isNullOrBlank()
        if (!hasId) logger.warn("signMacApp: sin identidad (usa -PmacosSigningIdentity=... o env MACOS_SIGNING_IDENTITY); se omite la firma.")
        isMac && hasId
    }
    doLast {
        val identity = macosSigningIdentity!!
        val app      = file(appBundle)
        val ent      = file(entitlements)
        require(app.exists()) { "No existe $appBundle — ¿se ejecutó createDistributable?" }
        require(ent.exists()) { "No existe $entitlements" }

        fun sign(target: String, withEntitlements: Boolean = false) {
            exec {
                commandLine = buildList {
                    addAll(listOf("codesign", "--force", "--timestamp", "--options", "runtime"))
                    if (withEntitlements) addAll(listOf("--entitlements", ent.absolutePath))
                    addAll(listOf("--sign", identity, target))
                }
            }
        }

        // 1) Todos los .dylib / .jnilib / .so (incluye libs del runtime)
        fileTree(app) { include("**/*.dylib", "**/*.jnilib", "**/*.so") }
            .files
            .sortedByDescending { it.absolutePath.length } // de más profundo a menos
            .forEach { sign(it.absolutePath) }

        // 2) Helpers y binarios auxiliares en Contents/MacOS (todo lo que no sea el principal)
        val mainBin = "$appBundle/Contents/MacOS/WakeyWakey"
        file("$appBundle/Contents/MacOS").listFiles()?.forEach { f ->
            if (f.absolutePath != mainBin && f.canExecute() && f.isFile) {
                sign(f.absolutePath)
            }
        }

        // 3) Runtime (Java embebido) firmado como bundle
        val runtime = "$appBundle/Contents/runtime"
        if (file(runtime).exists()) sign(runtime)

        // 4) Binario principal con entitlements
        sign(mainBin, withEntitlements = true)

        // 5) El propio .app con entitlements
        sign(appBundle, withEntitlements = true)

        // 6) Verificación
        exec { commandLine("codesign", "--verify", "--deep", "--strict", "--verbose=2", appBundle) }
    }
}

afterEvaluate {
    tasks.matching { it.name == "createDistributable" }.configureEach {
        finalizedBy(injectCalendarHelper)
    }
    // El DMG/PKG deben empaquetar el .app YA firmado.
    tasks.matching { it.name in setOf("packageDmg", "packageReleaseDmg", "packageDistributionForCurrentOS") }
        .configureEach { dependsOn(signMacApp) }
}
