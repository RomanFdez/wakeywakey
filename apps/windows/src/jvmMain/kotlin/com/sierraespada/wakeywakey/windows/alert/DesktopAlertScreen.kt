package com.sierraespada.wakeywakey.windows.alert

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

private val Yellow = Color(0xFFFFE03A)
private val Navy   = Color(0xFF1A1A2E)
private val Coral  = Color(0xFFFF6B6B)

/**
 * Pantalla de alerta full-screen para Desktop.
 *
 * Versión independiente de la de Android — no usa R.string ni APIs Android.
 * Las cadenas están en inglés (i18n en Fase 6 via Compose Multiplatform Resources).
 *
 * Idéntica en UX: countdown, snooze, slide-to-dismiss.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesktopAlertScreen(
    title:               String,
    startTime:           Long,
    location:            String?,
    meetingUrl:          String?,
    isPreview:           Boolean = false,
    countdownMinutesOnly: Boolean = false,
    onJoin:              () -> Unit,
    onSnooze:            (delayMillis: Long) -> Unit,
    onDismiss:           () -> Unit,
    onUpgrade:           () -> Unit = {},
) {
    val isPro     by com.sierraespada.wakeywakey.windows.billing.DesktopEntitlementManager.isPro.collectAsState()
    val isTrial   = com.sierraespada.wakeywakey.windows.billing.DesktopEntitlementManager.isTrialActive
    val allowCustomSnooze = isPro || isTrial
    var secondsLeft by remember { mutableLongStateOf((startTime - System.currentTimeMillis()) / 1000L) }
    LaunchedEffect(startTime) {
        while (secondsLeft > -60) {
            secondsLeft = (startTime - System.currentTimeMillis()) / 1000L
            delay(1000L)
        }
    }

    val pulse = secondsLeft in 0L..30L
    val scale by animateFloatAsState(
        targetValue   = if (pulse) 1.15f else 1f,
        animationSpec = if (pulse) infiniteRepeatable(
            animation  = tween(500), repeatMode = RepeatMode.Reverse
        ) else snap(),
        label = "pulse",
    )

    var showSnoozeDialog by remember { mutableStateOf(false) }

    Box(
        modifier         = Modifier.fillMaxSize().background(Navy),
        contentAlignment = Alignment.Center,
    ) {
        // ── Banner PREVIEW ────────────────────────────────────────────────────
        if (isPreview) {
            Box(
                modifier         = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 20.dp)
                    .background(Coral.copy(alpha = 0.18f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 18.dp, vertical = 6.dp),
            ) {
                Text(
                    "PREVIEW — not a real alert",
                    fontSize   = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color      = Coral,
                    letterSpacing = 1.sp,
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier            = Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .padding(horizontal = 40.dp),
        ) {
            Text("⏰", fontSize = 64.sp, modifier = Modifier.scale(scale))

            // Title — tamaño adaptativo según longitud
            val titleFontSize = when {
                title.length > 60 -> 22.sp
                title.length > 35 -> 28.sp
                else              -> 34.sp
            }
            Text(
                text       = title,
                fontSize   = titleFontSize,
                fontWeight = FontWeight.ExtraBold,
                color      = Color.White,
                textAlign  = TextAlign.Center,
                lineHeight = titleFontSize * 1.25f,
                maxLines   = 4,
                overflow   = TextOverflow.Ellipsis,
            )

            // Time
            val timeStr = remember(startTime) {
                SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(startTime))
            }
            Text(timeStr, fontSize = 18.sp, color = Color.White.copy(alpha = 0.6f))

            // Location
            if (!location.isNullOrBlank() && !location.startsWith("http")) {
                Text(
                    "📍 $location",
                    fontSize  = 14.sp,
                    color     = Color.White.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(Modifier.height(8.dp))
            CountdownText(secondsLeft, countdownMinutesOnly)
            Spacer(Modifier.height(16.dp))

            // Join button
            if (meetingUrl != null) {
                Button(
                    onClick  = onJoin,
                    modifier = Modifier.fillMaxWidth().height(60.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = Yellow),
                    shape    = RoundedCornerShape(18.dp),
                ) {
                    Icon(
                        imageVector        = Icons.Filled.Videocam,
                        contentDescription = null,
                        tint               = Navy,
                        modifier           = Modifier.size(24.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text("Join now", color = Navy, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                }
            }

            // Snooze row
            DesktopSnoozeRow(
                onSnooze          = onSnooze,
                showCustom        = allowCustomSnooze,
                onCustomRequested = { showSnoozeDialog = true },
                onUpgrade         = onUpgrade,
            )

            // Dismiss
            OutlinedButton(
                onClick  = onDismiss,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White.copy(alpha = 0.55f),
                ),
            ) {
                Text("✕  Dismiss", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }

    if (showSnoozeDialog) {
        DesktopCustomSnoozeDialog(
            onConfirm = { mins ->
                showSnoozeDialog = false
                onSnooze(mins * 60_000L)
            },
            onDismiss = { showSnoozeDialog = false },
        )
    }
}

// ─── Countdown ────────────────────────────────────────────────────────────────

@Composable
private fun CountdownText(secondsLeft: Long, minutesOnly: Boolean = false) {
    val text = when {
        secondsLeft < -60  -> "Started ${(-secondsLeft / 60).toInt()}m ago"
        secondsLeft < 0    -> "Starting NOW"
        secondsLeft < 60   -> if (minutesOnly) "< 1m" else "Starts in ${secondsLeft}s"
        else               -> {
            val m = (secondsLeft / 60).toInt()
            val s = (secondsLeft % 60).toInt()
            if (minutesOnly) "Starts in ${m}m"
            else "Starts in %dm %02ds".format(m, s)
        }
    }
    val color = if (secondsLeft in 0L..30L) Coral else Yellow
    Text(text, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = color, textAlign = TextAlign.Center)
}

// ─── Snooze row ───────────────────────────────────────────────────────────────

@Composable
private fun DesktopSnoozeRow(
    onSnooze:          (Long) -> Unit,
    showCustom:        Boolean = true,
    onCustomRequested: () -> Unit,
    onUpgrade:         () -> Unit = {},
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        Text("Snooze:", fontSize = 13.sp, color = Color.White.copy(alpha = 0.5f))
        SnoozeChip("1 min") { onSnooze(60_000L) }
        SnoozeChip("5 min") { onSnooze(5 * 60_000L) }
        if (showCustom) {
            SnoozeChip("Custom…") { onCustomRequested() }
        }
    }
}

@Composable
private fun SnoozeChip(label: String, onClick: () -> Unit) {
    Surface(
        shape    = RoundedCornerShape(20.dp),
        color    = Color.White.copy(alpha = 0.08f),
    ) {
        TextButton(
            onClick        = onClick,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
        ) {
            Text(label, color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
        }
    }
}

// ─── Custom snooze dialog ─────────────────────────────────────────────────────

@Composable
private fun DesktopCustomSnoozeDialog(
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf("") }
    val minutes = text.toIntOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = Color(0xFF16213E),
        title = {
            Text("Snooze for…", color = Color.White, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                ) {
                    listOf(5, 10, 15, 30).forEach { m ->
                        OutlinedButton(
                            onClick = { onConfirm(m) },
                            shape   = RoundedCornerShape(8.dp),
                            colors  = ButtonDefaults.outlinedButtonColors(contentColor = Yellow),
                        ) { Text("${m}m") }
                    }
                }
                OutlinedTextField(
                    value         = text,
                    onValueChange = { text = it.filter { c -> c.isDigit() }.take(3) },
                    placeholder   = { Text("or type minutes…", color = Color.White.copy(alpha = 0.3f)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine    = true,
                    colors        = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = Yellow,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedTextColor     = Color.White,
                        unfocusedTextColor   = Color.White,
                    ),
                )
            }
        },
        confirmButton = {
            Button(
                onClick  = { minutes?.let { if (it > 0) onConfirm(it) } },
                enabled  = minutes != null && minutes > 0,
                colors   = ButtonDefaults.buttonColors(containerColor = Yellow, contentColor = Navy),
            ) { Text("Snooze ${minutes ?: "?"}m", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.White.copy(alpha = 0.5f))
            }
        },
    )
}

