package com.sierraespada.wakeywakey.windows.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.rememberWindowState
import com.sierraespada.wakeywakey.windows.rememberAppIconPainter
import kotlinx.coroutines.launch

// ─── Colores locales ──────────────────────────────────────────────────────────

private val Yellow  = Color(0xFFFFE03A)
private val Navy    = Color(0xFF1A1A2E)
private val Surface = Color(0xFF16213E)
private val White   = Color.White
private val GoogleBlue = Color(0xFF4285F4)
private val MsBlue     = Color(0xFF0078D4)

/**
 * Ventana de onboarding: el usuario elige con qué proveedor de calendario
 * quiere conectarse (Google Calendar o Microsoft Outlook/365).
 *
 * Se muestra la primera vez que se abre la app o cuando el usuario pulsa
 * "Connect calendar" desde HomeScreen.
 *
 * @param onConnected   Llamado cuando el proceso OAuth termina con éxito.
 * @param onDismiss     Llamado cuando el usuario cierra la ventana sin conectar.
 */
@Composable
fun OnboardingWindow(
    onConnected: () -> Unit,
    onDismiss:   () -> Unit,
) {
    val appIcon = rememberAppIconPainter()
    Window(
        onCloseRequest = onDismiss,
        state          = rememberWindowState(size = DpSize(460.dp, 540.dp)),
        title          = "WakeyWakey — Connect Calendar",
        icon           = appIcon,
        resizable      = false,
    ) {
        OnboardingContent(
            onConnected = onConnected,
            onDismiss   = onDismiss,
        )
    }
}

// ─── Contenido ────────────────────────────────────────────────────────────────

private enum class Step { CHOOSE, CONNECTING, SUCCESS, ERROR }

@Composable
private fun OnboardingContent(
    onConnected: () -> Unit,
    onDismiss:   () -> Unit,
) {
    val scope   = rememberCoroutineScope()
    var step    by remember { mutableStateOf(Step.CHOOSE) }
    var errorMsg by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Navy),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier            = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {

            // ── Close button ──────────────────────────────────────────────────
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close", tint = White.copy(alpha = 0.5f))
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── Logo + título ─────────────────────────────────────────────────
            Text("📅", fontSize = 48.sp)
            Spacer(Modifier.height(12.dp))
            Text(
                "Connect your calendar",
                color      = Yellow,
                fontSize   = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign  = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "WakeyWakey reads your upcoming meetings so it can\nalert you before they start.",
                color     = White.copy(alpha = 0.65f),
                fontSize  = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
            )

            Spacer(Modifier.height(32.dp))

            // ── Steps ─────────────────────────────────────────────────────────
            when (step) {

                Step.CHOOSE -> {
                    ProviderButton(
                        label    = "Connect Google Calendar",
                        subLabel = "Gmail · Google Workspace",
                        color    = GoogleBlue,
                        enabled  = CalendarAccountManager.canConnectGoogle,
                        onClick  = {
                            step = Step.CONNECTING
                            scope.launch {
                                runCatching { CalendarAccountManager.connectGoogle() }
                                    .onSuccess { step = Step.SUCCESS }
                                    .onFailure { e ->
                                        errorMsg = e.message ?: "Unknown error"
                                        step = Step.ERROR
                                    }
                            }
                        },
                    )

                    if (!CalendarAccountManager.canConnectGoogle) {
                        Text(
                            "GOOGLE_CLIENT_ID / GOOGLE_CLIENT_SECRET not configured",
                            color    = Color(0xFFFF6B6B),
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            modifier  = Modifier.padding(top = 4.dp),
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    ProviderButton(
                        label    = "Connect Microsoft Calendar",
                        subLabel = "Outlook · Microsoft 365",
                        color    = MsBlue,
                        enabled  = CalendarAccountManager.canConnectMicrosoft,
                        onClick  = {
                            step = Step.CONNECTING
                            scope.launch {
                                runCatching { CalendarAccountManager.connectMicrosoft() }
                                    .onSuccess { step = Step.SUCCESS }
                                    .onFailure { e ->
                                        errorMsg = e.message ?: "Unknown error"
                                        step = Step.ERROR
                                    }
                            }
                        },
                    )

                    if (!CalendarAccountManager.canConnectMicrosoft) {
                        Text(
                            "MICROSOFT_CLIENT_ID not configured",
                            color    = Color(0xFFFF6B6B),
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            modifier  = Modifier.padding(top = 4.dp),
                        )
                    }

                    Spacer(Modifier.height(24.dp))

                    Text(
                        "Only calendar data is read — no changes are made.\nTokens are stored locally on this device.",
                        color     = White.copy(alpha = 0.4f),
                        fontSize  = 12.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp,
                    )
                }

                Step.CONNECTING -> {
                    Spacer(Modifier.height(32.dp))
                    CircularProgressIndicator(color = Yellow, strokeWidth = 3.dp)
                    Spacer(Modifier.height(24.dp))
                    Text(
                        "A browser window has opened.\nSign in and grant calendar access,\nthen come back here.",
                        color     = White.copy(alpha = 0.7f),
                        fontSize  = 15.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp,
                    )
                }

                Step.SUCCESS -> {
                    Spacer(Modifier.height(24.dp))
                    Icon(
                        imageVector        = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint               = Color(0xFF4CAF50),
                        modifier           = Modifier.size(56.dp),
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Calendar connected!",
                        color      = White,
                        fontSize   = 20.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    CalendarAccountManager.connectedEmail?.let { email ->
                        Spacer(Modifier.height(6.dp))
                        Text(email, color = White.copy(alpha = 0.55f), fontSize = 13.sp)
                    }
                    Spacer(Modifier.weight(1f))
                    Button(
                        onClick = onConnected,
                        colors  = ButtonDefaults.buttonColors(
                            containerColor = Yellow,
                            contentColor   = Navy,
                        ),
                        shape   = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                    ) {
                        Text("Let's go!", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }

                Step.ERROR -> {
                    Spacer(Modifier.height(24.dp))
                    Icon(
                        imageVector        = Icons.Filled.Warning,
                        contentDescription = null,
                        tint               = Color(0xFFFF6B6B),
                        modifier           = Modifier.size(48.dp),
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Connection failed",
                        color      = Color(0xFFFF6B6B),
                        fontSize   = 18.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        errorMsg,
                        color     = White.copy(alpha = 0.6f),
                        fontSize  = 12.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp,
                    )
                    Spacer(Modifier.height(24.dp))
                    OutlinedButton(
                        onClick = { step = Step.CHOOSE },
                        border  = androidx.compose.foundation.BorderStroke(1.dp, Yellow.copy(alpha = 0.5f)),
                        shape   = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                    ) {
                        Text("Try again", color = Yellow, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

// ─── Provider button ──────────────────────────────────────────────────────────

@Composable
private fun ProviderButton(
    label:    String,
    subLabel: String,
    color:    Color,
    enabled:  Boolean,
    onClick:  () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(RoundedCornerShape(14.dp))
            .border(1.5.dp, color.copy(alpha = if (enabled) 0.7f else 0.25f), RoundedCornerShape(14.dp))
            .background(color.copy(alpha = if (enabled) 0.12f else 0.05f)),
    ) {
        Button(
            onClick  = onClick,
            enabled  = enabled,
            colors   = ButtonDefaults.buttonColors(
                containerColor         = Color.Transparent,
                contentColor           = White,
                disabledContainerColor = Color.Transparent,
                disabledContentColor   = White.copy(alpha = 0.3f),
            ),
            shape    = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxSize(),
            elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(label, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text(
                    subLabel,
                    fontSize = 12.sp,
                    color    = if (enabled) White.copy(alpha = 0.55f) else White.copy(alpha = 0.25f),
                )
            }
        }
    }
}
