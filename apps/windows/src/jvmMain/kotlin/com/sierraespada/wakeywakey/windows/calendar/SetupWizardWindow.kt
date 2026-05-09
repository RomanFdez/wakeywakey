package com.sierraespada.wakeywakey.windows.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import com.sierraespada.wakeywakey.windows.ui.SwapHoriz
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.rememberWindowState
import com.sierraespada.wakeywakey.windows.PlatformMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

// ─── Colores ──────────────────────────────────────────────────────────────────

private val Yellow     = Color(0xFFFFE03A)
private val Navy       = Color(0xFF1A1A2E)
private val NavySurf   = Color(0xFF16213E)
private val White      = Color.White
private val GoogleBlue = Color(0xFF4285F4)
private val MsBlue     = Color(0xFF0078D4)
private val MacGray    = Color(0xFF8E8E93)
private val Green      = Color(0xFF4CAF50)
private val Coral      = Color(0xFFFF6B6B)
private val Subtitle   = Color(0xFF8892AA)

// ─── Pasos del wizard ─────────────────────────────────────────────────────────

private enum class WizardStep  { WELCOME, CONNECT, DONE }
private enum class ConnectState { IDLE, CONNECTING, SUCCESS, ERROR }

/**
 * Ventana de configuración inicial (primer arranque).
 *
 * Pasos:
 *  1. WELCOME  — presenta la app
 *  2. CONNECT  — macOS: solicita permisos de Calendar;
 *                Windows: conecta Google o Microsoft vía OAuth
 *  3. DONE     — confirmación
 *
 * El botón DEV en el paso CONNECT permite alternar entre modos para pruebas.
 */
@Composable
fun SetupWizardWindow(
    icon:                 Painter,
    platformMode:         PlatformMode,
    onPlatformModeChange: (PlatformMode) -> Unit,
    onComplete:           () -> Unit,
) {
    Window(
        onCloseRequest = onComplete,
        state          = rememberWindowState(size = DpSize(520.dp, 580.dp)),
        title          = "WakeyWakey — Setup",
        icon           = icon,
        resizable      = false,
    ) {
        WizardContent(
            platformMode         = platformMode,
            onPlatformModeChange = onPlatformModeChange,
            onComplete           = onComplete,
        )
    }
}

// ─── Contenido ────────────────────────────────────────────────────────────────

@Composable
private fun WizardContent(
    platformMode:         PlatformMode,
    onPlatformModeChange: (PlatformMode) -> Unit,
    onComplete:           () -> Unit,
) {
    var step         by remember { mutableStateOf(WizardStep.WELCOME) }
    var connectState by remember { mutableStateOf(ConnectState.IDLE) }
    var errorMsg     by remember { mutableStateOf("") }
    var macGranted   by remember { mutableStateOf(false) }
    val scope        = rememberCoroutineScope()

    // Resetear estado de conexión al cambiar modo (toggle DEV)
    LaunchedEffect(platformMode) {
        connectState = ConnectState.IDLE
        errorMsg     = ""
        macGranted   = false
    }

    Box(
        modifier         = Modifier.fillMaxSize().background(Navy),
        contentAlignment = Alignment.TopCenter,
    ) {
        when (step) {
            WizardStep.WELCOME -> WelcomeStep(onNext = { step = WizardStep.CONNECT })

            WizardStep.CONNECT -> ConnectStep(
                platformMode  = platformMode,
                connectState  = connectState,
                errorMsg      = errorMsg,
                macGranted    = macGranted,
                onDevToggle   = {
                    onPlatformModeChange(
                        if (platformMode == PlatformMode.MAC_SYSTEM) PlatformMode.WINDOWS_OAUTH
                        else PlatformMode.MAC_SYSTEM
                    )
                },
                onConnectOAuth = { provider ->
                    connectState = ConnectState.CONNECTING
                    scope.launch {
                        runCatching {
                            when (provider) {
                                "google"    -> CalendarAccountManager.connectGoogle()
                                "microsoft" -> CalendarAccountManager.connectMicrosoft()
                            }
                        }.onSuccess {
                            connectState = ConnectState.SUCCESS
                            step = WizardStep.DONE
                        }.onFailure { e ->
                            errorMsg     = e.message ?: "Unknown error"
                            connectState = ConnectState.ERROR
                        }
                    }
                },
                onRequestMacPermission = {
                    connectState = ConnectState.CONNECTING
                    scope.launch {
                        val granted = requestMacCalendarPermission()
                        if (granted) {
                            macGranted   = true
                            connectState = ConnectState.SUCCESS
                            step = WizardStep.DONE
                        } else {
                            errorMsg     = "Calendar access was denied. Open System Settings → Privacy & Security → Calendars and enable WakeyWakey."
                            connectState = ConnectState.ERROR
                        }
                    }
                },
                onRetry = { connectState = ConnectState.IDLE },
                onSkip  = { connectState = ConnectState.IDLE; step = WizardStep.DONE },
            )

            WizardStep.DONE -> DoneStep(
                platformMode = platformMode,
                connected    = if (platformMode == PlatformMode.MAC_SYSTEM)
                                   macGranted
                               else
                                   CalendarAccountManager.isConnected,
                onFinish     = onComplete,
            )
        }
    }
}

// ─── Paso 1: Bienvenida ───────────────────────────────────────────────────────

@Composable
private fun WelcomeStep(onNext: () -> Unit) {
    Column(
        modifier            = Modifier.fillMaxSize().padding(horizontal = 40.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(0.5f))
        Text("⏰", fontSize = 72.sp)
        Spacer(Modifier.height(20.dp))
        Text("WakeyWakey", color = Yellow, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(6.dp))
        Text("Never miss a meeting again", color = White.copy(alpha = 0.65f), fontSize = 16.sp)
        Spacer(Modifier.height(36.dp))
        FeatureBullet("🔔", "Full-screen alert before each meeting starts")
        Spacer(Modifier.height(12.dp))
        FeatureBullet("📋", "Next meeting always visible in your menu bar")
        Spacer(Modifier.height(12.dp))
        FeatureBullet("🔗", "One tap to join Google Meet, Teams or Zoom")
        Spacer(Modifier.height(12.dp))
        FeatureBullet("⏸", "Pause or snooze alerts whenever you need")
        Spacer(Modifier.weight(1f))
        Button(
            onClick  = onNext,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape    = RoundedCornerShape(14.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = Yellow, contentColor = Navy),
        ) {
            Text("Get Started  →", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
        }
    }
}

@Composable
private fun FeatureBullet(emoji: String, text: String) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(emoji, fontSize = 22.sp, modifier = Modifier.width(32.dp))
        Text(text, color = White.copy(alpha = 0.75f), fontSize = 14.sp, lineHeight = 20.sp, modifier = Modifier.weight(1f))
    }
}

// ─── Paso 2: Conectar calendario ──────────────────────────────────────────────

@Composable
private fun ConnectStep(
    platformMode:          PlatformMode,
    connectState:          ConnectState,
    errorMsg:              String,
    macGranted:            Boolean,
    onDevToggle:           () -> Unit,
    onConnectOAuth:        (provider: String) -> Unit,
    onRequestMacPermission: () -> Unit,
    onRetry:               () -> Unit,
    onSkip:                () -> Unit,
) {
    Column(
        modifier            = Modifier.fillMaxSize().padding(horizontal = 36.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // ── DEV toggle ────────────────────────────────────────────────────────
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF0F3460))
                    .clickable { onDevToggle() }
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Icon(Icons.Filled.SwapHoriz, null, tint = Yellow, modifier = Modifier.size(14.dp))
                Text(
                    "DEV: ${if (platformMode == PlatformMode.MAC_SYSTEM) "macOS" else "Windows"}",
                    color      = Yellow,
                    fontSize   = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        if (platformMode == PlatformMode.MAC_SYSTEM) {
            MacConnectStep(
                connectState           = connectState,
                errorMsg               = errorMsg,
                onRequestMacPermission = onRequestMacPermission,
                onRetry                = onRetry,
                onSkip                 = onSkip,
            )
        } else {
            WindowsConnectStep(
                connectState   = connectState,
                errorMsg       = errorMsg,
                onConnectOAuth = onConnectOAuth,
                onRetry        = onRetry,
                onSkip         = onSkip,
            )
        }
    }
}

// ── macOS: solicitar permiso de Calendar ──────────────────────────────────────

@Composable
private fun ColumnScope.MacConnectStep(
    connectState:           ConnectState,
    errorMsg:               String,
    onRequestMacPermission: () -> Unit,
    onRetry:                () -> Unit,
    onSkip:                 () -> Unit,
) {
    Text("🍎", fontSize = 48.sp)
    Spacer(Modifier.height(10.dp))
    Text(
        "Allow calendar access",
        color      = Yellow,
        fontSize   = 22.sp,
        fontWeight = FontWeight.Bold,
        textAlign  = TextAlign.Center,
    )
    Spacer(Modifier.height(8.dp))
    Text(
        "WakeyWakey reads events from the macOS Calendar app — Google, iCloud, Exchange and any other calendar you have synced.",
        color      = White.copy(alpha = 0.55f),
        fontSize   = 13.sp,
        textAlign  = TextAlign.Center,
        lineHeight = 19.sp,
    )

    Spacer(Modifier.height(28.dp))

    when (connectState) {
        ConnectState.IDLE, ConnectState.ERROR -> {
            // Permission button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.5.dp, MacGray.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                    .background(MacGray.copy(alpha = 0.10f)),
            ) {
                Button(
                    onClick   = onRequestMacPermission,
                    colors    = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor   = White,
                    ),
                    shape     = RoundedCornerShape(14.dp),
                    modifier  = Modifier.fillMaxSize(),
                    elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp),
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Grant Calendar Access", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        Text("macOS will ask for permission", fontSize = 12.sp, color = White.copy(alpha = 0.5f))
                    }
                }
            }

            // Hint: macOS defaults to "add events only" — remind user to pick Full Access
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E3A5F), RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("☝️", fontSize = 16.sp)
                Text(
                    "When macOS asks, select \"Acceso total al calendario\" — not \"solo para añadir eventos\".",
                    color      = White.copy(alpha = 0.85f),
                    fontSize   = 12.sp,
                    lineHeight = 17.sp,
                )
            }

            if (connectState == ConnectState.ERROR) {
                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Coral.copy(alpha = 0.10f), RoundedCornerShape(10.dp))
                        .padding(12.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(Icons.Filled.Warning, null, tint = Coral, modifier = Modifier.size(18.dp))
                    Text(errorMsg, color = Coral, fontSize = 12.sp, modifier = Modifier.weight(1f), lineHeight = 17.sp)
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onRetry,
                    shape   = RoundedCornerShape(10.dp),
                    colors  = ButtonDefaults.outlinedButtonColors(contentColor = Yellow),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Try again", fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(Modifier.weight(1f))
            TextButton(onClick = onSkip) {
                Text("Skip for now  →", color = White.copy(alpha = 0.35f), fontSize = 13.sp)
            }
        }

        ConnectState.CONNECTING -> {
            Spacer(Modifier.height(32.dp))
            CircularProgressIndicator(color = Yellow, strokeWidth = 3.dp, modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(24.dp))
            Text(
                "Requesting calendar access…\nmacOS may show a permission dialog.",
                color      = White.copy(alpha = 0.7f),
                fontSize   = 14.sp,
                textAlign  = TextAlign.Center,
                lineHeight = 21.sp,
            )
        }

        ConnectState.SUCCESS -> { /* parent moves to DONE */ }
    }
}

// ── Windows: conectar OAuth ────────────────────────────────────────────────────

@Composable
private fun ColumnScope.WindowsConnectStep(
    connectState:   ConnectState,
    errorMsg:       String,
    onConnectOAuth: (provider: String) -> Unit,
    onRetry:        () -> Unit,
    onSkip:         () -> Unit,
) {
    Text("📅", fontSize = 48.sp)
    Spacer(Modifier.height(10.dp))
    Text(
        "Connect your calendar",
        color      = Yellow,
        fontSize   = 22.sp,
        fontWeight = FontWeight.Bold,
        textAlign  = TextAlign.Center,
    )
    Spacer(Modifier.height(8.dp))
    Text(
        "WakeyWakey reads your upcoming meetings to alert you before they start.\nNo changes are ever made.",
        color      = White.copy(alpha = 0.55f),
        fontSize   = 13.sp,
        textAlign  = TextAlign.Center,
        lineHeight = 19.sp,
    )

    Spacer(Modifier.height(28.dp))

    when (connectState) {
        ConnectState.IDLE, ConnectState.ERROR -> {
            ProviderButton(
                label    = "Connect Google Calendar",
                subLabel = "Gmail · Google Workspace",
                color    = GoogleBlue,
                enabled  = CalendarAccountManager.canConnectGoogle,
                onClick  = { onConnectOAuth("google") },
            )
            if (!CalendarAccountManager.canConnectGoogle) {
                Text("GOOGLE_CLIENT_ID not configured", color = Coral, fontSize = 10.sp, modifier = Modifier.padding(top = 3.dp))
            }

            Spacer(Modifier.height(14.dp))

            ProviderButton(
                label    = "Connect Microsoft / Outlook",
                subLabel = "Outlook · Microsoft 365",
                color    = MsBlue,
                enabled  = CalendarAccountManager.canConnectMicrosoft,
                onClick  = { onConnectOAuth("microsoft") },
            )
            if (!CalendarAccountManager.canConnectMicrosoft) {
                Text("MICROSOFT_CLIENT_ID not configured", color = Coral, fontSize = 10.sp, modifier = Modifier.padding(top = 3.dp))
            }

            if (connectState == ConnectState.ERROR) {
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Coral.copy(alpha = 0.10f), RoundedCornerShape(10.dp))
                        .padding(12.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(Icons.Filled.Warning, null, tint = Coral, modifier = Modifier.size(18.dp))
                    Text(errorMsg, color = Coral, fontSize = 12.sp, modifier = Modifier.weight(1f))
                }
            }

            Spacer(Modifier.weight(1f))
            TextButton(onClick = onSkip) {
                Text("Skip for now  →", color = White.copy(alpha = 0.35f), fontSize = 13.sp)
            }
        }

        ConnectState.CONNECTING -> {
            Spacer(Modifier.height(32.dp))
            CircularProgressIndicator(color = Yellow, strokeWidth = 3.dp, modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(24.dp))
            Text(
                "A browser window has opened.\nSign in and grant calendar access,\nthen return here.",
                color      = White.copy(alpha = 0.7f),
                fontSize   = 14.sp,
                textAlign  = TextAlign.Center,
                lineHeight = 21.sp,
            )
        }

        ConnectState.SUCCESS -> { /* parent moves to DONE */ }
    }
}

// ─── Paso 3: Listo ────────────────────────────────────────────────────────────

@Composable
private fun DoneStep(
    platformMode: PlatformMode,
    connected:    Boolean,
    onFinish:     () -> Unit,
) {
    Column(
        modifier            = Modifier.fillMaxSize().padding(horizontal = 40.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))

        if (connected) {
            Icon(Icons.Filled.CheckCircle, null, tint = Green, modifier = Modifier.size(64.dp))
            Spacer(Modifier.height(20.dp))
            Text("You're all set!", color = White, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(10.dp))
            if (platformMode == PlatformMode.MAC_SYSTEM) {
                Text(
                    "Calendar access granted.\nAll your macOS calendars will be used.",
                    color      = Green.copy(alpha = 0.85f),
                    fontSize   = 13.sp,
                    textAlign  = TextAlign.Center,
                    lineHeight = 19.sp,
                )
            } else {
                CalendarAccountManager.connectedEmail?.let { email ->
                    Text(email, color = Green.copy(alpha = 0.8f), fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "WakeyWakey is now running in your menu bar.\nClick the WW icon anytime to see your meetings.",
                color      = White.copy(alpha = 0.6f),
                fontSize   = 14.sp,
                textAlign  = TextAlign.Center,
                lineHeight = 21.sp,
            )
        } else {
            Text("🔔", fontSize = 64.sp)
            Spacer(Modifier.height(20.dp))
            Text("WakeyWakey is running", color = White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
            Spacer(Modifier.height(10.dp))
            Text(
                "You can connect a calendar later from the\nmenu bar icon → Settings.",
                color      = White.copy(alpha = 0.55f),
                fontSize   = 14.sp,
                textAlign  = TextAlign.Center,
                lineHeight = 21.sp,
            )
        }

        Spacer(Modifier.weight(1f))

        Button(
            onClick  = onFinish,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape    = RoundedCornerShape(14.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = Yellow, contentColor = Navy),
        ) {
            Text(
                if (connected) "Start using WakeyWakey  →" else "Close",
                fontWeight = FontWeight.ExtraBold,
                fontSize   = 16.sp,
            )
        }
    }
}

// ─── Provider button ──────────────────────────────────────────────────────────

@Composable
private fun ProviderButton(label: String, subLabel: String, color: Color, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(RoundedCornerShape(14.dp))
            .border(1.5.dp, color.copy(alpha = if (enabled) 0.7f else 0.25f), RoundedCornerShape(14.dp))
            .background(color.copy(alpha = if (enabled) 0.12f else 0.05f)),
    ) {
        Button(
            onClick   = onClick,
            enabled   = enabled,
            colors    = ButtonDefaults.buttonColors(
                containerColor         = Color.Transparent,
                contentColor           = White,
                disabledContainerColor = Color.Transparent,
                disabledContentColor   = White.copy(alpha = 0.3f),
            ),
            shape     = RoundedCornerShape(14.dp),
            modifier  = Modifier.fillMaxSize(),
            elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(label, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text(subLabel, fontSize = 12.sp, color = if (enabled) White.copy(alpha = 0.55f) else White.copy(alpha = 0.25f))
            }
        }
    }
}

// ─── macOS permission helper ──────────────────────────────────────────────────

/**
 * Ejecuta un osascript mínimo que accede al calendario.
 * macOS mostrará el diálogo de permisos si aún no se han concedido.
 * Devuelve true si el acceso fue concedido (script completó sin error).
 */
private suspend fun requestMacCalendarPermission(): Boolean = withContext(Dispatchers.IO) {
    try {
        val script  = "tell application \"Calendar\" to return name of first calendar"
        val process = ProcessBuilder("osascript", "-e", script)
            .redirectErrorStream(false)
            .start()
        val output  = process.inputStream.bufferedReader().readText()
        process.errorStream.bufferedReader().readText()  // consume stderr
        process.waitFor(15, TimeUnit.SECONDS)
        val exitCode = process.exitValue()
        exitCode == 0 && output.trim().isNotEmpty()
    } catch (e: Exception) {
        System.err.println("MacCalendarPermission: ${e.message}")
        false
    }
}
