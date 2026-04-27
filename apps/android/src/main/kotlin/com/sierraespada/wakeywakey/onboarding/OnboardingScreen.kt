package com.sierraespada.wakeywakey.onboarding

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sierraespada.wakeywakey.ui.theme.WakeyWakeyTheme

// ─── Brand colours (inline para no depender de un objeto Colors externo) ──────
private val Yellow = Color(0xFFFFE03A)
private val Navy   = Color(0xFF1A1A2E)
private val Green  = Color(0xFF4CAF50)

// ─── Modelo de ítem de permiso ────────────────────────────────────────────────

private enum class PermAction { CALENDAR, NOTIFICATIONS, EXACT_ALARM, BATTERY, FULL_SCREEN }

private data class PermItem(
    val emoji: String,
    val title: String,
    val rationale: String,
    val action: PermAction,
    val required: Boolean = true,
)

private val PERM_ITEMS = listOf(
    PermItem(
        emoji     = "📅",
        title     = "Acceso al calendario",
        rationale = "Lee tus reuniones para programar alertas. Los datos nunca salen de tu dispositivo.",
        action    = PermAction.CALENDAR,
    ),
    PermItem(
        emoji     = "🔔",
        title     = "Notificaciones",
        rationale = "Envía la alerta de reunión como notificación de alta prioridad.",
        action    = PermAction.NOTIFICATIONS,
    ),
    PermItem(
        emoji     = "⏰",
        title     = "Alarmas exactas",
        rationale = "Android 12+ requiere permiso especial para activar alarmas a la hora exacta.",
        action    = PermAction.EXACT_ALARM,
    ),
    PermItem(
        emoji     = "🔋",
        title     = "Sin optimización de batería",
        rationale = "Evita que Android duerma la app y pierda alertas con la pantalla apagada.",
        action    = PermAction.BATTERY,
        required  = false,
    ),
    PermItem(
        emoji     = "📱",
        title     = "Pantalla completa al alertar",
        rationale = "Muestra la alerta sobre la pantalla de bloqueo (Android 14+).",
        action    = PermAction.FULL_SCREEN,
        required  = false,
    ),
)

// ─── Mapeo PermItem → estado actual ───────────────────────────────────────────

private fun PermissionsState.isGranted(item: PermItem): Boolean = when (item.action) {
    PermAction.CALENDAR       -> calendar
    PermAction.NOTIFICATIONS  -> notifications
    PermAction.EXACT_ALARM    -> exactAlarm
    PermAction.BATTERY        -> batteryOptimization
    PermAction.FULL_SCREEN    -> fullScreenIntent
}

// ─── Screen ───────────────────────────────────────────────────────────────────

@Composable
fun OnboardingScreen(
    vm: PermissionsViewModel = viewModel(),
    onAllRequiredGranted: () -> Unit,
) {
    val context   = LocalContext.current
    val state     by vm.state.collectAsState()
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    // Refresca el estado al volver de Settings (usuario concedió permiso especial)
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            vm.refresh(context)
        }
    }

    // Avance automático cuando los permisos requeridos están OK
    LaunchedEffect(state.requiredGranted) {
        if (state.requiredGranted) onAllRequiredGranted()
    }

    // Launcher para runtime permissions (CALENDAR + POST_NOTIFICATIONS)
    val runtimeLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { vm.refresh(context) }

    WakeyWakeyTheme(darkTheme = true) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Navy)
                .systemBarsPadding(),
        ) {
            Column(
                modifier              = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalAlignment   = Alignment.CenterHorizontally,
                verticalArrangement   = Arrangement.spacedBy(16.dp),
            ) {
                OnboardingHeader()

                PERM_ITEMS.forEach { item ->
                    AnimatedVisibility(
                        visible = true,
                        enter   = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
                    ) {
                        PermissionRow(
                            item    = item,
                            granted = state.isGranted(item),
                            onAllow = {
                                requestPermission(
                                    context         = context,
                                    item            = item,
                                    runtimeLauncher = { runtimeLauncher.launch(it) },
                                )
                            },
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                Button(
                    onClick  = onAllRequiredGranted,
                    enabled  = state.requiredGranted,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape    = RoundedCornerShape(14.dp),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor         = Yellow,
                        contentColor           = Navy,
                        disabledContainerColor = Yellow.copy(alpha = 0.25f),
                        disabledContentColor   = Navy.copy(alpha = 0.4f),
                    ),
                ) {
                    Text(
                        text       = if (state.requiredGranted) "¡Empezar!" else "Acepta los permisos requeridos",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize   = 15.sp,
                    )
                }

                Text(
                    text       = "Los datos del calendario nunca salen de tu dispositivo.\nConsulta nuestra Política de Privacidad en sierraespada.com/privacy",
                    fontSize   = 11.sp,
                    color      = Color.White.copy(alpha = 0.35f),
                    textAlign  = TextAlign.Center,
                    lineHeight = 16.sp,
                )
            }
        }
    }
}

// ─── Sub-composables ──────────────────────────────────────────────────────────

@Composable
private fun OnboardingHeader() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier            = Modifier.padding(bottom = 8.dp),
    ) {
        Text("⏰", fontSize = 56.sp)
        Text(
            text       = "WakeyWakey",
            fontSize   = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            color      = Yellow,
        )
        Text(
            text       = "Para alertarte antes de tus reuniones\nnecesitamos un par de permisos.",
            fontSize   = 15.sp,
            color      = Color.White.copy(alpha = 0.7f),
            textAlign  = TextAlign.Center,
            lineHeight = 22.sp,
        )
    }
}

@Composable
private fun PermissionRow(
    item: PermItem,
    granted: Boolean,
    onAllow: () -> Unit,
) {
    val alpha by animateFloatAsState(
        targetValue = if (granted) 0.55f else 1f,
        label       = "rowAlpha",
    )

    Surface(
        shape    = RoundedCornerShape(16.dp),
        color    = Color.White.copy(alpha = 0.06f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier              = Modifier.padding(14.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Icono / check
            Box(
                modifier         = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        if (granted) Green.copy(alpha = 0.15f)
                        else Yellow.copy(alpha = 0.10f)
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text     = if (granted) "✓" else item.emoji,
                    fontSize = if (granted) 20.sp else 22.sp,
                    color    = if (granted) Green else Color.White,
                )
            }

            // Texto
            Column(
                modifier            = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                ) {
                    Text(
                        text       = item.title,
                        fontSize   = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = Color.White.copy(alpha = alpha),
                    )
                    if (!item.required) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color.White.copy(alpha = 0.07f),
                        ) {
                            Text(
                                text     = "opcional",
                                fontSize = 10.sp,
                                color    = Color.White.copy(alpha = 0.45f),
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                            )
                        }
                    }
                }
                Text(
                    text       = item.rationale,
                    fontSize   = 12.sp,
                    color      = Color.White.copy(alpha = alpha * 0.65f),
                    lineHeight = 17.sp,
                )
            }

            // Botón "Permitir" sólo cuando aún no está concedido
            if (!granted) {
                TextButton(
                    onClick = onAllow,
                    colors  = ButtonDefaults.textButtonColors(contentColor = Yellow),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text("Permitir", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

// ─── Solicitar permiso / abrir Settings ───────────────────────────────────────

private fun requestPermission(
    context: android.content.Context,
    item: PermItem,
    runtimeLauncher: (Array<String>) -> Unit,
) {
    when (item.action) {
        PermAction.CALENDAR -> runtimeLauncher(arrayOf(Manifest.permission.READ_CALENDAR))

        PermAction.NOTIFICATIONS -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                runtimeLauncher(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
            }
        }

        PermAction.EXACT_ALARM -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.startActivity(
                    Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                        data  = Uri.parse("package:${context.packageName}")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                )
            }
        }

        PermAction.BATTERY -> {
            context.startActivity(
                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data  = Uri.parse("package:${context.packageName}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            )
        }

        PermAction.FULL_SCREEN -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                context.startActivity(
                    Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENTS).apply {
                        data  = Uri.parse("package:${context.packageName}")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                )
            }
        }
    }
}
