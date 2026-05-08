package com.sierraespada.wakeywakey.windows

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import java.awt.Desktop
import java.net.InetAddress
import java.net.ServerSocket
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.*
import com.sierraespada.wakeywakey.windows.alert.AlertWindow
import com.sierraespada.wakeywakey.windows.billing.DesktopEntitlementManager
import com.sierraespada.wakeywakey.windows.billing.DesktopPaywallWindow
import com.sierraespada.wakeywakey.windows.calendar.OnboardingWindow
import com.sierraespada.wakeywakey.windows.calendar.SetupWizardWindow
import com.sierraespada.wakeywakey.windows.home.HomeScreen
import com.sierraespada.wakeywakey.windows.settings.DesktopSettingsRepository
import com.sierraespada.wakeywakey.windows.settings.DesktopSettingsScreen
import com.sierraespada.wakeywakey.windows.tray.TrayMenuWindow

// ─── Brand palette ────────────────────────────────────────────────────────────

val AppColorScheme = darkColorScheme(
    primary      = Color(0xFFFFE03A),
    onPrimary    = Color(0xFF1A1A2E),
    surface      = Color(0xFF16213E),
    onSurface    = Color.White,
    background   = Color(0xFF1A1A2E),
    onBackground = Color.White,
    error        = Color(0xFFFF6B6B),
)

// ─── App icon — painter vectorial puro (sin TextMeasurer) ────────────────────

/**
 * Painter resolution-independent con las letras WW más compactas y anchas,
 * proporción más cercana a una W tipográfica real.
 * Sin texto, sin fuentes, sin CompositionLocals → válido en cualquier contexto.
 */
internal val AppIcon: Painter = object : Painter() {
    override val intrinsicSize = Size.Unspecified

    override fun DrawScope.onDraw() {
        val d   = size.minDimension
        val cx  = size.width  / 2f
        val cy  = size.height / 2f
        val pad = d * 0.035f

        // ── Círculo amarillo ──────────────────────────────────────────────────
        drawCircle(
            color  = Color(0xFFFFE03A),
            radius = d / 2f - pad,
            center = Offset(cx, cy),
        )

        // ── Dos "W" — proporciones tipográficas (ancha, no alargada) ─────────
        val stroke = Stroke(
            width = d * 0.07f,          // trazo ligeramente más grueso
            cap   = StrokeCap.Round,
            join  = StrokeJoin.Round,
        )
        val navy = Color(0xFF1A1A2E)
        val wH   = d * 0.20f           // alto reducido → menos alargada
        val wW   = d * 0.26f           // ancho aumentado → más tipográfica
        val gap  = d * 0.025f          // separación mínima entre W y W

        // Anclas verticales: la W ocupa desde top hasta bot, valle en mid
        val top  = cy - wH * 0.50f
        val bot  = cy + wH * 0.50f
        val mid  = cy - wH * 0.12f    // valle bastante arriba → W menos "puntiaguda"

        // W izquierda
        val lLeft  = cx - wW - gap / 2f
        val lMidX  = lLeft + wW / 2f
        val lRight = cx - gap / 2f
        val wPath1 = Path().apply {
            moveTo(lLeft,  top)
            lineTo(lLeft  + wW * 0.28f, bot)
            lineTo(lMidX,  mid)
            lineTo(lRight - wW * 0.28f, bot)
            lineTo(lRight, top)
        }
        drawPath(wPath1, navy, style = stroke)

        // W derecha
        val rLeft  = cx + gap / 2f
        val rMidX  = rLeft + wW / 2f
        val rRight = cx + wW + gap / 2f
        val wPath2 = Path().apply {
            moveTo(rLeft,  top)
            lineTo(rLeft  + wW * 0.28f, bot)
            lineTo(rMidX,  mid)
            lineTo(rRight - wW * 0.28f, bot)
            lineTo(rRight, top)
        }
        drawPath(wPath2, navy, style = stroke)
    }
}

// rememberAppIconPainter() — mantener para compatibilidad con OnboardingWindow.
@Composable
internal fun rememberAppIconPainter(): Painter = AppIcon

// ─── Entry point ─────────────────────────────────────────────────────────────

// ── Single-instance lock via socket ──────────────────────────────────────────
//
// Intentamos hacer bind en un puerto localhost fijo.
// Si el bind falla (BindException) → otra instancia ya está corriendo.
// El OS libera el puerto automáticamente cuando el proceso muere — sin ficheros residuales.

private const val SINGLE_INSTANCE_PORT = 47291   // puerto arbitrario, específico de WakeyWakey
private var _instanceSocket: ServerSocket? = null

// ── Custom URL scheme: wakeywakey://activate?key=XXXX ─────────────────────────
// Recibe la license key tras la compra en LemonSqueezy y activa la licencia
// sin que el usuario tenga que copiar/pegar nada.

private val _pendingActivationKey = MutableStateFlow<String?>(null)

private fun setupUriHandler() {
    if (!Desktop.isDesktopSupported()) return
    val desktop = Desktop.getDesktop()
    if (!desktop.isSupported(Desktop.Action.APP_OPEN_URI)) return
    desktop.setOpenURIHandler { event ->
        val uri = event.uri
        if (uri.scheme == "wakeywakey" && uri.host == "activate") {
            val key = uri.query
                ?.split("&")
                ?.firstOrNull { it.startsWith("key=") }
                ?.removePrefix("key=")
                ?.trim()
            if (!key.isNullOrBlank()) {
                _pendingActivationKey.value = key
            }
        }
    }
}

private fun acquireSingleInstanceLock(): Boolean {
    return try {
        _instanceSocket = ServerSocket(SINGLE_INSTANCE_PORT, 0, InetAddress.getByName("127.0.0.1"))
        true
    } catch (e: java.net.BindException) {
        false  // puerto ocupado → otra instancia activa
    } catch (e: Exception) {
        System.err.println("WakeyWakey: single-instance check failed (${e.message}) — allowing start")
        true
    }
}

// ─────────────────────────────────────────────────────────────────────────────

fun main() {
    // Instancia única: si ya hay una corriendo, salir silenciosamente.
    if (!acquireSingleInstanceLock()) {
        System.err.println("WakeyWakey: otra instancia ya está en ejecución — saliendo.")
        return
    }

    // Registra el handler de wakeywakey:// ANTES de inicializar AWT/Compose.
    setupUriHandler()

    // Oculta el icono del Dock en macOS — la app vive solo en la barra de menú.
    // Debe establecerse ANTES de cualquier inicialización de AWT.
    System.setProperty("apple.awt.UIElement", "true")
    System.setProperty("skiko.renderApi", "METAL")
    System.setProperty("apple.awt.application.appearance", "system")
    System.setProperty("apple.awt.application.name", "WakeyWakey")

    application {

        val appState = remember { AppState() }

        LaunchedEffect(Unit) {
            WakeyWakeyApp.init()
            appState.start()
        }

        // Activa la licencia automáticamente cuando llega un wakeywakey://activate?key=... URL
        var showActivationSuccess by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            _pendingActivationKey
                .filterNotNull()
                .collect { key ->
                    DesktopEntitlementManager.activateLicense(key)
                    _pendingActivationKey.value = null
                    appState.showPaywall = false
                    showActivationSuccess = true
                }
        }

        if (showActivationSuccess) {
            androidx.compose.ui.window.DialogWindow(
                onCloseRequest = { showActivationSuccess = false },
                title          = "WakeyWakey",
                state          = androidx.compose.ui.window.rememberDialogState(
                    width  = 340.dp,
                    height = 240.dp,
                ),
            ) {
                MaterialTheme(colorScheme = AppColorScheme) {
                    androidx.compose.foundation.layout.Column(
                        modifier              = androidx.compose.ui.Modifier
                            .fillMaxSize()
                            .background(Color(0xFF1A1A2E))
                            .padding(24.dp),
                        verticalArrangement   = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
                        horizontalAlignment   = androidx.compose.ui.Alignment.CenterHorizontally,
                    ) {
                        androidx.compose.material3.Text(
                            "🎉 License activated!",
                            color      = Color(0xFFFFE03A),
                            fontSize   = 18.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        )
                        androidx.compose.material3.Text(
                            "Welcome to WakeyWakey Pro.\nYou now have full access to all features.",
                            color    = Color.White,
                            fontSize = 13.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                        androidx.compose.material3.Button(
                            onClick = { showActivationSuccess = false },
                            colors  = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFFE03A),
                                contentColor   = Color(0xFF1A1A2E),
                            ),
                        ) {
                            androidx.compose.material3.Text("Get started", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                        }
                    }
                }
            }
        }

        DisposableEffect(Unit) {
            onDispose { appState.dispose() }
        }

        // ── Tray nativo via AWT (isImageAutoSize = false → texto sin recortar) ─

        val settings       by DesktopSettingsRepository.settings.collectAsState()
        val homeUiState    by appState.homeVm.uiState.collectAsState()
        val isPro          by DesktopEntitlementManager.isPro.collectAsState()
        val trialDaysLeft  by DesktopEntitlementManager.trialDaysLeft.collectAsState()

        // Si el trial expira, mostrar el paywall automáticamente (una sola vez)
        LaunchedEffect(trialDaysLeft, isPro) {
            if (!isPro && trialDaysLeft <= 0 && !appState.showPaywall) {
                appState.showPaywall = true
            }
        }

        // Al cambiar de tier (DEV o activación de licencia) cerrar Settings si está abierta,
        // ya que algunos elementos del tier no se recomponen hasta reabrirla.
        LaunchedEffect(Unit) {
            var initialPro   = isPro
            var initialDays  = trialDaysLeft
            snapshotFlow { DesktopEntitlementManager.isPro.value to DesktopEntitlementManager.trialDaysLeft.value }
                .collect { (pro, days) ->
                    if (pro != initialPro || days != initialDays) {
                        initialPro  = pro
                        initialDays = days
                        if (appState.showSettings) appState.showSettings = false
                    }
                }
        }

        // Instala el tray una sola vez y lo elimina al cerrar la app
        DisposableEffect(Unit) {
            AwtTrayManager.install(
                onTrayClicked = { clickX ->
                    appState.trayClickX = clickX
                    appState.showTrayPopup = true
                },
                onQuit = { exitApplication() },
            )
            onDispose { AwtTrayManager.remove() }
        }

        // Actualiza el contenido del tray cuando cambia cualquier estado relevante.
        // Usar homeUiState completo como clave garantiza que el rebuild se dispara
        // siempre que los eventos cambien (no sólo cuando nextEvent cambia).
        LaunchedEffect(homeUiState, settings) {
            AwtTrayManager.updateContent(settings, homeUiState)
        }

        // Actualiza el menú (Pause / Resume) cuando cambia el estado de pausa
        LaunchedEffect(appState.isPaused) {
            AwtTrayManager.updatePaused(appState.isPaused)
        }

        // ── Tray popup: popup rico con lista de reuniones ─────────────────────

        if (appState.showTrayPopup) {
            MaterialTheme(colorScheme = AppColorScheme) {
                TrayMenuWindow(
                    homeState        = homeUiState,
                    clickX           = appState.trayClickX,
                    isPaused         = appState.isPaused,
                    onPreviewEvent   = { event -> appState.previewAlert(event) },
                    onOpenSettings   = { appState.showSettings = true },
                    onPauseOneHour   = { appState.pauseOneHour() },
                    onResume         = { appState.resume() },
                    onQuit           = { exitApplication() },
                    onDismiss        = { appState.showTrayPopup = false },
                    showDevBar       = settings.showDevBar,
                    isPro            = isPro,
                    trialDaysLeft    = trialDaysLeft,
                    onUpgrade        = { appState.showTrayPopup = false; appState.showPaywall = true },
                    onDebugPreview   = { appState.debugPreviewAlert() },
                    onDebugAlarm5s   = { appState.debugAlarmIn5s() },
                    onResetWizard    = { appState.resetWizard() },
                    platformMode     = appState.platformMode,
                    onTogglePlatform = {
                        appState.platformMode = when (appState.platformMode) {
                            PlatformMode.WINDOWS_OAUTH -> PlatformMode.MAC_SYSTEM
                            PlatformMode.MAC_SYSTEM    -> PlatformMode.WINDOWS_OAUTH
                        }
                    },
                )
            }
        }

        // ── First-run wizard ──────────────────────────────────────────────────

        if (appState.showWizard) {
            SetupWizardWindow(
                icon                 = AppIcon,
                platformMode         = appState.platformMode,
                onPlatformModeChange = { appState.platformMode = it },
                onComplete           = { appState.completeWizard() },
            )
        }

        // ── Home window ───────────────────────────────────────────────────────

        if (appState.showHome) {
            Window(
                onCloseRequest = { appState.showHome = false },
                state          = rememberWindowState(size = DpSize(960.dp, 620.dp)),
                title          = "WakeyWakey",
                icon           = AppIcon,
            ) {
                MaterialTheme(colorScheme = AppColorScheme) {
                    HomeScreen(
                        vm                = appState.homeVm,
                        onOpenSettings    = { appState.showSettings = true },
                        onConnectCalendar = { appState.requestCalendarConnect() },
                    )
                }
            }
        }

        // ── Settings window ───────────────────────────────────────────────────

        if (appState.showSettings) {
            Window(
                onCloseRequest = { appState.showSettings = false },
                state          = rememberWindowState(size = DpSize(560.dp, 680.dp)),
                title          = "WakeyWakey — Settings",
                icon           = AppIcon,
            ) {
                MaterialTheme(colorScheme = AppColorScheme) {
                    DesktopSettingsScreen(
                        onConnectCalendar  = {
                            appState.showSettings          = false
                            appState.onboardingFromSettings = true
                            appState.showOnboarding        = true
                        },
                        appIcon      = AppIcon,
                        allCalendars = homeUiState.allCalendars,
                        platformMode = appState.platformMode,
                        onUpgrade    = { appState.showPaywall = true },
                    )
                }
            }
        }

        // ── Onboarding window ─────────────────────────────────────────────────

        if (appState.showOnboarding) {
            OnboardingWindow(
                onConnected = {
                    appState.showOnboarding = false
                    appState.homeVm.refresh()
                    // Si vino desde Settings, volver a Settings para ver las cuentas
                    if (appState.onboardingFromSettings) {
                        appState.onboardingFromSettings = false
                        appState.showSettings = true
                    }
                },
                onDismiss = {
                    appState.showOnboarding = false
                    // Si vino desde Settings y cancela, también volver
                    if (appState.onboardingFromSettings) {
                        appState.onboardingFromSettings = false
                        appState.showSettings = true
                    }
                },
            )
        }

        // ── Paywall ───────────────────────────────────────────────────────────

        if (appState.showPaywall) {
            DesktopPaywallWindow(
                trialDaysLeft = trialDaysLeft,
                onDismiss     = { appState.showPaywall = false },
            )
        }

        // ── Alert window (real o preview) ─────────────────────────────────────

        appState.pendingAlert?.let { event ->
            if (settings.alertAllScreens) {
                // Mostrar en todas las pantallas
                val devices = remember {
                    java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().screenDevices.toList()
                }
                devices.forEach { device ->
                    MaterialTheme(colorScheme = AppColorScheme) {
                        AlertWindow(
                            event          = event,
                            icon           = AppIcon,
                            isPreview      = appState.isPendingAlertPreview,
                            explicitDevice = device,
                            onSnooze       = { delayMs -> appState.snoozeAlert(event, delayMs) },
                            onDismiss      = { appState.dismissAlert() },
                        )
                    }
                }
            } else {
                MaterialTheme(colorScheme = AppColorScheme) {
                    AlertWindow(
                        event      = event,
                        icon       = AppIcon,
                        isPreview  = appState.isPendingAlertPreview,
                        onSnooze   = { delayMs -> appState.snoozeAlert(event, delayMs) },
                        onDismiss  = { appState.dismissAlert() },
                    )
                }
            }
        }

    } // end application
} // end main
