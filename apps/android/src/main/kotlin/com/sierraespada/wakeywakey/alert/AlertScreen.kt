package com.sierraespada.wakeywakey.alert

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

private val Yellow = Color(0xFFFFE03A)
private val Navy   = Color(0xFF1A1A2E)
private val Coral  = Color(0xFFFF6B6B)

/**
 * Pantalla de alerta full-screen.
 * Se muestra sobre la pantalla de bloqueo cuando llega una reunión.
 */
@Composable
fun AlertScreen(
    title: String,
    startTime: Long,
    location: String?,
    meetingUrl: String?,
    onJoin: () -> Unit,
    onSnooze: () -> Unit,
    onDismiss: () -> Unit,
) {
    // Countdown actualizado cada segundo
    var secondsLeft by remember { mutableLongStateOf((startTime - System.currentTimeMillis()) / 1000L) }
    LaunchedEffect(startTime) {
        while (secondsLeft > -60) {
            secondsLeft = (startTime - System.currentTimeMillis()) / 1000L
            delay(1000L)
        }
    }

    // Pulso en el emoji de alarma cuando quedan <30s
    val pulse = secondsLeft in 0L..30L
    val scale by animateFloatAsState(
        targetValue = if (pulse) 1.15f else 1f,
        animationSpec = if (pulse) infiniteRepeatable(
            animation = tween(500), repeatMode = RepeatMode.Reverse
        ) else snap(),
        label = "pulse"
    )

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
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
        ) {

            // Icono
            Text(
                text = "⏰",
                fontSize = 72.sp,
                modifier = Modifier.scale(scale),
            )

            // Título del evento
            Text(
                text = title,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                textAlign = TextAlign.Center,
                lineHeight = 34.sp,
            )

            // Hora de inicio
            val timeStr = remember(startTime) {
                SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(startTime))
            }
            Text(
                text = timeStr,
                fontSize = 18.sp,
                color = Color.White.copy(alpha = 0.6f),
            )

            // Localización (si existe y no es una URL)
            if (!location.isNullOrBlank() && !location.startsWith("http")) {
                Text(
                    text = "📍 $location",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center,
                )
            }

            // Countdown
            Spacer(Modifier.height(8.dp))
            CountdownText(secondsLeft)

            Spacer(Modifier.height(24.dp))

            // Botón "Join" — solo si hay enlace de videollamada
            if (meetingUrl != null) {
                Button(
                    onClick = onJoin,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Yellow),
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Text(
                        text = "Join now",
                        color = Navy,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
            }

            // Snooze
            OutlinedButton(
                onClick = onSnooze,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(
                    1.5.dp, Color.White.copy(alpha = 0.25f)
                ),
            ) {
                Text("Snooze 5 min", fontSize = 16.sp)
            }

            // Dismiss
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Dismiss",
                    color = Color.White.copy(alpha = 0.45f),
                    fontSize = 14.sp,
                )
            }
        }
    }
}

@Composable
private fun CountdownText(secondsLeft: Long) {
    when {
        secondsLeft > 0 -> {
            val m = secondsLeft / 60
            val s = secondsLeft % 60
            val text = if (m > 0) "Starts in ${m}m ${s}s" else "Starts in ${s}s"
            Text(text = text, fontSize = 17.sp, color = Yellow, fontWeight = FontWeight.SemiBold)
        }
        secondsLeft >= -30 -> {
            Text(text = "Starting NOW", fontSize = 20.sp, color = Coral, fontWeight = FontWeight.Bold)
        }
        else -> {
            val m = (-secondsLeft / 60).toInt()
            Text(
                text = "Started ${m}m ago",
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.5f),
            )
        }
    }
}
