package com.sierraespada.wakeywakey.windows.settings

/**
 * Gestiona el autostart de la app al iniciar sesión.
 *
 * Implementaciones por SO:
 *  - Windows  → registro HKCU\Software\Microsoft\Windows\CurrentVersion\Run
 *  - macOS    → LaunchAgent en ~/Library/LaunchAgents/
 *  - Linux    → XDG autostart en ~/.config/autostart/
 *
 * Usa el ejecutable real de la JVM en ejecución para que funcione tanto
 * en desarrollo (jar) como en el paquete nativo (WakeyWakey.exe / .app).
 */
object AutostartManager {

    private val os = System.getProperty("os.name", "").lowercase()
    val isWindows get() = os.contains("windows")
    val isMac     get() = os.contains("mac")
    val isLinux   get() = os.contains("linux") || os.contains("nux")

    val isSupported get() = isWindows || isMac || isLinux

    // ── Estado ────────────────────────────────────────────────────────────────

    val isEnabled: Boolean
        get() = runCatching {
            when {
                isWindows -> windowsIsEnabled()
                isMac     -> macIsEnabled()
                isLinux   -> linuxIsEnabled()
                else      -> false
            }
        }.getOrElse { false }

    // ── Enable / Disable ──────────────────────────────────────────────────────

    fun enable(): Result<Unit> = runCatching {
        val exe = executablePath()
        when {
            isWindows -> windowsEnable(exe)
            isMac     -> macEnable(exe)
            isLinux   -> linuxEnable(exe)
        }
    }

    fun disable(): Result<Unit> = runCatching {
        when {
            isWindows -> windowsDisable()
            isMac     -> macDisable()
            isLinux   -> linuxDisable()
        }
    }

    // ── Windows registry ──────────────────────────────────────────────────────

    private val REG_KEY  = "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Run"
    private val APP_NAME = "WakeyWakey"

    private fun windowsIsEnabled(): Boolean {
        val result = exec("reg", "query", REG_KEY, "/v", APP_NAME)
        return result.contains(APP_NAME, ignoreCase = true)
    }

    private fun windowsEnable(exe: String) {
        exec("reg", "add", REG_KEY, "/v", APP_NAME, "/t", "REG_SZ", "/d", "\"$exe\"", "/f")
    }

    private fun windowsDisable() {
        exec("reg", "delete", REG_KEY, "/v", APP_NAME, "/f")
    }

    // ── macOS LaunchAgent ─────────────────────────────────────────────────────

    private val MAC_PLIST_DIR  = "${System.getProperty("user.home")}/Library/LaunchAgents"
    private val MAC_PLIST_FILE = "$MAC_PLIST_DIR/com.sierraespada.wakeywakey.plist"
    private val MAC_LABEL      = "com.sierraespada.wakeywakey"

    private fun macIsEnabled(): Boolean =
        java.io.File(MAC_PLIST_FILE).exists()

    private fun macEnable(exe: String) {
        java.io.File(MAC_PLIST_DIR).mkdirs()
        java.io.File(MAC_PLIST_FILE).writeText("""
            <?xml version="1.0" encoding="UTF-8"?>
            <!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN"
              "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
            <plist version="1.0">
            <dict>
                <key>Label</key>
                <string>$MAC_LABEL</string>
                <key>ProgramArguments</key>
                <array>
                    <string>$exe</string>
                </array>
                <key>RunAtLoad</key>
                <true/>
                <key>KeepAlive</key>
                <false/>
            </dict>
            </plist>
        """.trimIndent())
        exec("launchctl", "load", "-w", MAC_PLIST_FILE)
    }

    private fun macDisable() {
        runCatching { exec("launchctl", "unload", "-w", MAC_PLIST_FILE) }
        java.io.File(MAC_PLIST_FILE).delete()
    }

    // ── Linux XDG autostart ───────────────────────────────────────────────────

    private val LINUX_AUTOSTART_DIR  = "${System.getProperty("user.home")}/.config/autostart"
    private val LINUX_AUTOSTART_FILE = "$LINUX_AUTOSTART_DIR/wakeywakey.desktop"

    private fun linuxIsEnabled(): Boolean =
        java.io.File(LINUX_AUTOSTART_FILE).exists()

    private fun linuxEnable(exe: String) {
        java.io.File(LINUX_AUTOSTART_DIR).mkdirs()
        java.io.File(LINUX_AUTOSTART_FILE).writeText("""
            [Desktop Entry]
            Type=Application
            Name=WakeyWakey
            Exec=$exe
            Hidden=false
            NoDisplay=false
            X-GNOME-Autostart-enabled=true
        """.trimIndent())
    }

    private fun linuxDisable() {
        java.io.File(LINUX_AUTOSTART_FILE).delete()
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Ruta al ejecutable actual.
     * - Paquete nativo: devuelve el .exe / .app / bin real.
     * - Desarrollo con jar: devuelve el ejecutable de Java.
     */
    private fun executablePath(): String {
        // 1. Intenta leer el ejecutable del proceso actual (JDK 9+)
        val cmdPath = ProcessHandle.current().info().command().orElse(null)
        if (cmdPath != null) return cmdPath

        // 2. Fallback: java.home/bin/java
        val javaHome = System.getProperty("java.home", "")
        return if (isWindows) "$javaHome\\bin\\javaw.exe" else "$javaHome/bin/java"
    }

    private fun exec(vararg cmd: String): String = runCatching {
        ProcessBuilder(*cmd)
            .redirectErrorStream(true)
            .start()
            .inputStream.bufferedReader().readText()
    }.getOrElse { "" }
}
