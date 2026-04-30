package com.sierraespada.wakeywakey.windows.alert

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

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
    title:      String,
    startTime:  Long,
    location:   String?,
    meetingUrl: String?,
    onJoin:     () -> Unit,
    onSnooze:   (delayMillis: Long) -> Unit,
    onDismiss:  () -> Unit,
) {
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
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier            = Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .padding(horizontal = 40.dp),
        ) {
            Text("⏰", fontSize = 64.sp, modifier = Modifier.scale(scale))

            // Title — auto-size
            Text(
                text       = title,
                fontSize   = 36.sp,
                fontWeight = FontWeight.ExtraBold,
                color      = Color.White,
                textAlign  = TextAlign.Center,
                maxLines   = 3,
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
            CountdownText(secondsLeft)
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
                startTime         = startTime,
                secondsLeft       = secondsLeft,
                onSnooze          = onSnooze,
                onCustomRequested = { showSnoozeDialog = true },
            )

            // Dismiss
            DesktopSwipeToDismiss(onDismiss = onDismiss)
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
private fun CountdownText(secondsLeft: Long) {
    val text = when {
        secondsLeft < -60  -> "Started ${(-secondsLeft / 60).toInt()}m ago"
        secondsLeft < 0    -> "Starting NOW"
        secondsLeft < 60   -> "Starts in ${secondsLeft}s"
        else               -> {
            val m = (secondsLeft / 60).toInt()
            val s = (secondsLeft % 60).toInt()
            "Starts in %dm %02ds".format(m, s)
        }
    }
    val color = if (secondsLeft in 0L..30L) Coral else Yellow
    Text(
        text       = text,
        fontSize   = 22.sp,
        fontWeight = FontWeight.ExtraBold,
        color      = color,
        textAlign  = TextAlign.Center,
    )
}

// ─── Snooze row ───────────────────────────────────────────────────────────────

@Composable
private fun DesktopSnoozeRow(
    startTime:         Long,
    secondsLeft:       Long,
    onSnooze:          (Long) -> Unit,
    onCustomRequested: () -> Unit,
) {
    val minsUntilStart = (secondsLeft / 60L).toInt()

    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        Text(
            "Snooze:",
            fontSize = 13.sp,
            color    = Color.White.copy(alpha = 0.5f),
        )

        // 1 min
        SnoozeChip("1 min") { onSnooze(60_000L) }

        // At start (only if event hasn't started yet)
        if (minsUntilStart > 1) {
            SnoozeChip("At start (+${minsUntilStart}m)") {
                onSnooze((startTime - System.currentTimeMillis()).coerceAtLeast(60_000L))
            }
        }

        // Custom
        SnoozeChip("Custom…") { onCustomRequested() }
    }
}

@Composable
private fun SnoozeChip(label: String, onClick: () -> Unit) {
    Surface(
        shape    = RoundedCornerShape(20.dp),
        color    = Color.White.copy(alpha = 0.08f),
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .pointerInput(Unit) { detectHorizontalDragGestures { _, _ -> } }
            .then(Modifier),
    ) {
        TextButton(
            onClick        = onClick,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
        ) {
            Text(label, color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
        }
    }
}

// ─── Slide to dismiss ─────────────────────────────────────────────────────────

@Composable
private fun DesktopSwipeToDismiss(onDismiss: () -> Unit) {
    var trackWidth  by remember { mutableIntStateOf(0) }
    var offsetX     by remember { mutableFloatStateOf(0f) }
    val scope       = rememberCoroutineScope()
    val threshold   = 0.6f

    val animOffset by animateFloatAsState(
        targetValue   = offsetX,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label         = "swipe",
    )

    Box(
        modifier = Modifier
            .widthIn(min = 220.dp, max = 320.dp)
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(24.dp))
            .onSizeChanged { trackWidth = it.width }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (offsetX / trackWidth >= threshold) onDismiss()
                        else scope.launch { offsetX = 0f }
                    },
                    onHorizontalDrag = { _, delta ->
                        offsetX = (offsetX + delta).coerceIn(0f, trackWidth.toFloat())
                    },
                )
            },
    ) {
        // Track fill
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width((animOffset).dp.coerceAtLeast(0.dp))
                .background(Yellow.copy(alpha = 0.12f)),
        )

        // Thumb
        Box(
            modifier = Modifier
                .offset { IntOffset(animOffset.roundToInt(), 0) }
                .size(48.dp)
                .clip(CircleShape)
                .background(Yellow.copy(alpha = 0.9f)),
            contentAlignment = Alignment.Center,
        ) {
            Text("›", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Navy)
        }

        // Label
        Box(
            modifier         = Modifier.fillMaxSize().padding(start = 60.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                "slide to dismiss  ›",
                fontSize = 13.sp,
                color    = Color.White.copy(alpha = 0.35f),
            )
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

// ─── Helpers ──────────────────────────────────────────────────────────────────

private val CircleShape = RoundedCornerShape(50)
