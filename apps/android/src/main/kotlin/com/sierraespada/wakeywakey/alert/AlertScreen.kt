package com.sierraespada.wakeywakey.alert

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.sierraespada.wakeywakey.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

private val Yellow = Color(0xFFFFE03A)
private val Navy   = Color(0xFF1A1A2E)
private val Coral  = Color(0xFFFF6B6B)

/**
 * Full-screen alert displayed over the lock screen when a meeting is imminent.
 *
 * Snooze options:
 *  • 1 min      — re-alert in 60 seconds
 *  • At start   — re-alert exactly at the event's start time (only shown if start > now)
 *  • Custom     — opens a picker so the user can choose any number of minutes
 *
 * onSnooze receives the delay in milliseconds from now.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertScreen(
    title: String,
    startTime: Long,
    location: String?,
    meetingUrl: String?,
    onJoin: () -> Unit,
    onSnooze: (delayMillis: Long) -> Unit,
    onDismiss: () -> Unit,
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

    // Custom snooze dialog state
    var showCustomDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Navy)
            .systemBarsPadding(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier            = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
        ) {
            Text("⏰", fontSize = 61.sp, modifier = Modifier.scale(scale))

            AutoSizeText(
                text       = title,
                maxFontSize = 36.sp,
                minFontSize = 16.sp,
                fontWeight  = FontWeight.ExtraBold,
                color       = Color.White,
                textAlign   = TextAlign.Center,
                maxLines    = 3,
                modifier    = Modifier.fillMaxWidth(),
            )

            val timeStr = remember(startTime) {
                SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(startTime))
            }
            Text(text = timeStr, fontSize = 18.sp, color = Color.White.copy(alpha = 0.6f))

            if (!location.isNullOrBlank() && !location.startsWith("http")) {
                Text(
                    text      = "📍 $location",
                    fontSize  = 14.sp,
                    color     = Color.White.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(Modifier.height(8.dp))
            CountdownText(secondsLeft)
            Spacer(Modifier.height(16.dp))

            // ── Join ─────────────────────────────────────────────────────────
            if (meetingUrl != null) {
                Button(
                    onClick  = onJoin,
                    modifier = Modifier.fillMaxWidth().height(60.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = Yellow),
                    shape    = RoundedCornerShape(18.dp),
                ) {
                    Text(stringResource(R.string.alert_join_now), color = Navy, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                }
            }

            // ── Snooze options ────────────────────────────────────────────────
            SnoozeRow(
                startTime         = startTime,
                secondsLeft       = secondsLeft,
                onSnooze          = onSnooze,
                onCustomRequested = { showCustomDialog = true },
            )

            // ── Dismiss ───────────────────────────────────────────────────────
            SwipeToDismiss(onDismiss = onDismiss)
        }
    }

    // ── Custom snooze dialog ──────────────────────────────────────────────────
    if (showCustomDialog) {
        CustomSnoozeDialog(
            onConfirm = { minutes ->
                showCustomDialog = false
                onSnooze(minutes * 60_000L)
            },
            onDismiss = { showCustomDialog = false },
        )
    }
}

// ─── Snooze row ───────────────────────────────────────────────────────────────

@Composable
private fun SnoozeRow(
    startTime: Long,
    secondsLeft: Long,
    onSnooze: (Long) -> Unit,
    onCustomRequested: () -> Unit,
) {
    val now             = System.currentTimeMillis()
    val millisToStart   = startTime - now
    val showAtStart     = millisToStart > 90_000L   // only useful if > 90s away

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text      = stringResource(R.string.alert_snooze_label),
            fontSize  = 13.sp,
            color     = Color.White.copy(alpha = 0.4f),
            textAlign = TextAlign.Center,
            modifier  = Modifier.fillMaxWidth(),
        )
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // 1 min
            SnoozeChip(
                label    = stringResource(R.string.alert_snooze_1min),
                modifier = Modifier.weight(1f),
                onClick  = { onSnooze(60_000L) },
            )
            // At start (conditional)
            if (showAtStart) {
                val minsToStart = (millisToStart / 60_000L).toInt()
                SnoozeChip(
                    label    = stringResource(R.string.alert_snooze_at_start, minsToStart),
                    modifier = Modifier.weight(1.4f),
                    onClick  = { onSnooze(millisToStart) },
                )
            }
            // Custom
            SnoozeChip(
                label    = stringResource(R.string.alert_snooze_custom),
                modifier = Modifier.weight(1f),
                onClick  = onCustomRequested,
            )
        }
    }
}

@Composable
private fun SnoozeChip(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    OutlinedButton(
        onClick  = onClick,
        modifier = modifier.height(48.dp),
        shape    = RoundedCornerShape(14.dp),
        colors   = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
        border   = androidx.compose.foundation.BorderStroke(1.5.dp, Color.White.copy(alpha = 0.22f)),
        contentPadding = PaddingValues(horizontal = 6.dp),
    ) {
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

// ─── Custom snooze dialog ─────────────────────────────────────────────────────

@Composable
private fun CustomSnoozeDialog(
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val presets = listOf(2, 5, 10, 15, 30)
    var customText  by remember { mutableStateOf("") }
    var selected    by remember { mutableIntStateOf(5) }
    var isCustom    by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = Color(0xFF16213E),
        title = {
            Text(stringResource(R.string.alert_snooze_dialog_title), color = Color.White, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Preset chips
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier              = Modifier.fillMaxWidth(),
                ) {
                    presets.forEach { min ->
                        val active = !isCustom && selected == min
                        FilterChip(
                            selected = active,
                            onClick  = { selected = min; isCustom = false },
                            label    = { Text("${min}m", fontSize = 13.sp) },
                            colors   = FilterChipDefaults.filterChipColors(
                                selectedContainerColor     = Yellow,
                                selectedLabelColor         = Navy,
                                containerColor             = Color.White.copy(alpha = 0.07f),
                                labelColor                 = Color.White,
                            ),
                        )
                    }
                }

                // Custom input
                OutlinedTextField(
                    value         = customText,
                    onValueChange = { v ->
                        customText = v.filter { it.isDigit() }.take(3)
                        isCustom   = customText.isNotEmpty()
                        selected   = customText.toIntOrNull() ?: selected
                    },
                    placeholder   = { Text(stringResource(R.string.alert_snooze_type_minutes), color = Color.White.copy(alpha = 0.3f)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine    = true,
                    colors        = OutlinedTextFieldDefaults.colors(
                        focusedTextColor    = Color.White,
                        unfocusedTextColor  = Color.White,
                        focusedBorderColor  = Yellow,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.25f),
                        cursorColor         = Yellow,
                    ),
                )
            }
        },
        confirmButton = {
            val mins = if (isCustom) customText.toIntOrNull() ?: selected else selected
            Button(
                onClick  = { if (mins > 0) onConfirm(mins) },
                enabled  = (if (isCustom) customText.toIntOrNull() ?: 0 else selected) > 0,
                colors   = ButtonDefaults.buttonColors(containerColor = Yellow, contentColor = Navy),
            ) {
                val m = if (isCustom) customText.toIntOrNull() ?: selected else selected
                Text(stringResource(R.string.alert_snooze_confirm, m), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.alert_cancel), color = Color.White.copy(alpha = 0.5f))
            }
        },
    )
}

// ─── Swipe-to-dismiss ─────────────────────────────────────────────────────────

/**
 * iOS-style "slide to dismiss" track. The user must drag the thumb at least
 * [DISMISS_THRESHOLD] of the track width to trigger [onDismiss].
 * Releasing early snaps the thumb back with a spring animation.
 *
 * Key fix: maxOffset is stored as MutableState so the pointerInput lambda always
 * reads the live value. pointerInput key = trackWidthPx so the gesture handler
 * is reinstalled once the real width is known (onSizeChanged fires after first draw).
 */
@Composable
private fun SwipeToDismiss(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope       = rememberCoroutineScope()
    val density     = androidx.compose.ui.platform.LocalDensity.current
    val thumbSizeDp = 52.dp
    val trackPadDp  = 6.dp
    val thumbSizePx = with(density) { thumbSizeDp.toPx() }
    val trackPadPx  = with(density) { trackPadDp.toPx() }

    // Store as State so the gesture lambda always reads the latest value
    val maxOffset = remember { mutableFloatStateOf(0f) }
    val offsetX   = remember { Animatable(0f) }

    val progress = if (maxOffset.floatValue > 0f)
        (offsetX.value / maxOffset.floatValue).coerceIn(0f, 1f)
    else 0f

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(68.dp)
            .onSizeChanged { size ->
                maxOffset.floatValue = (size.width - thumbSizePx - trackPadPx * 2f)
                    .coerceAtLeast(0f)
            }
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.07f))
            .border(1.5.dp, Color.White.copy(alpha = 0.13f), CircleShape),
        contentAlignment = Alignment.CenterStart,
    ) {
        // Filled track grows with drag
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress)
                .background(Yellow.copy(alpha = (0.18f * progress)), CircleShape)
        )

        // Hint label fades as the user drags
        Text(
            text      = stringResource(R.string.alert_slide_to_dismiss),
            fontSize  = 13.sp,
            color     = Color.White.copy(alpha = (0.38f - progress * 0.5f).coerceAtLeast(0f)),
            modifier  = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )

        // Draggable thumb — key = maxOffset.floatValue so the gesture block
        // is reinstalled after onSizeChanged sets the real maxOffset
        Box(
            modifier = Modifier
                .padding(start = trackPadDp)
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .size(thumbSizeDp)
                .clip(CircleShape)
                .background(Yellow)
                .pointerInput(maxOffset.floatValue) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            val current = offsetX.value
                            val max     = maxOffset.floatValue
                            val prog    = if (max > 0f) current / max else 0f
                            scope.launch {
                                if (prog >= DISMISS_THRESHOLD) {
                                    offsetX.animateTo(max, spring(stiffness = Spring.StiffnessMediumLow))
                                    onDismiss()
                                } else {
                                    offsetX.animateTo(0f, spring(stiffness = Spring.StiffnessMedium))
                                }
                            }
                        },
                        onDragCancel = {
                            scope.launch {
                                offsetX.animateTo(0f, spring(stiffness = Spring.StiffnessMedium))
                            }
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            scope.launch {
                                offsetX.snapTo(
                                    (offsetX.value + dragAmount).coerceIn(0f, maxOffset.floatValue)
                                )
                            }
                        },
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Text("›", fontSize = 26.sp, color = Navy, fontWeight = FontWeight.ExtraBold)
        }
    }
}

private const val DISMISS_THRESHOLD = 0.75f

// ─── Auto-size text ───────────────────────────────────────────────────────────

/**
 * Text que reduce su fontSize automáticamente hasta [minFontSize] para que
 * quepa dentro de su espacio. Si aun así hay overflow, trunca con "…".
 * Solo dibuja cuando ha encontrado el tamaño estable (evita flash visual).
 */
@Composable
private fun AutoSizeText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    maxFontSize: TextUnit = 36.sp,
    minFontSize: TextUnit = 16.sp,
    fontWeight: FontWeight = FontWeight.Normal,
    textAlign: TextAlign = TextAlign.Start,
    maxLines: Int = Int.MAX_VALUE,
) {
    // Resetear cuando cambia el texto (no ocurre en AlertScreen pero es correcto)
    var fontSize    by remember(text) { mutableStateOf(maxFontSize) }
    var readyToDraw by remember(text) { mutableStateOf(false) }

    Text(
        text       = text,
        modifier   = modifier.drawWithContent { if (readyToDraw) drawContent() },
        color      = color,
        fontSize   = fontSize,
        fontWeight = fontWeight,
        textAlign  = textAlign,
        maxLines   = maxLines,
        overflow   = TextOverflow.Ellipsis,
        lineHeight = (fontSize.value * 1.22f).sp,
        onTextLayout = { result ->
            if (result.hasVisualOverflow) {
                val next = (fontSize.value * 0.9f).sp
                if (next > minFontSize) {
                    fontSize = next         // sigue reduciendo
                } else {
                    fontSize    = minFontSize
                    readyToDraw = true      // llegamos al mínimo → pintar con ellipsis
                }
            } else {
                readyToDraw = true          // cabe perfectamente
            }
        },
    )
}

// ─── Countdown ────────────────────────────────────────────────────────────────

@Composable
private fun CountdownText(secondsLeft: Long) {
    when {
        secondsLeft > 0 -> {
            val m    = secondsLeft / 60
            val s    = secondsLeft % 60
            val text = if (m > 0) stringResource(R.string.alert_starts_in_ms, m, s)
                       else       stringResource(R.string.alert_starts_in_s, s)
            Text(text = text, fontSize = 17.sp, color = Yellow, fontWeight = FontWeight.SemiBold)
        }
        secondsLeft >= -30 -> {
            Text(text = stringResource(R.string.alert_starting_now), fontSize = 20.sp, color = Coral, fontWeight = FontWeight.Bold)
        }
        else -> {
            val m = (-secondsLeft / 60).toInt()
            Text(text = stringResource(R.string.alert_started_ago, m), fontSize = 16.sp, color = Color.White.copy(alpha = 0.5f))
        }
    }
}
