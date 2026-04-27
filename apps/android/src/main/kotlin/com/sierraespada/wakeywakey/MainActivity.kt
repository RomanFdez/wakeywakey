package com.sierraespada.wakeywakey

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sierraespada.wakeywakey.scheduler.SchedulerService
import com.sierraespada.wakeywakey.ui.theme.WakeyWakeyTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Arranca el SchedulerService para programar alarmas al abrir la app
        startService(Intent(this, SchedulerService::class.java))

        setContent {
            WakeyWakeyTheme {
                MainScreen()
            }
        }
    }
}

/**
 * Pantalla principal temporal — placeholder hasta el Slice 3 (HomeScreen completa).
 * Confirma que la app arranca, el tema carga y el scheduler se inicia.
 */
@Composable
fun MainScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A2E))
            .systemBarsPadding(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp),
        ) {
            Text("⏰", fontSize = 64.sp)

            Text(
                text = "WakeyWakey",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFFFFE03A),
            )

            Text(
                text = "Never miss a meeting.\nSeriously.",
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                lineHeight = 24.sp,
            )

            Spacer(Modifier.height(16.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White.copy(alpha = 0.08f),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "Meeting alerts active ✓",
                    modifier = Modifier.padding(16.dp),
                    color = Color(0xFFFFE03A),
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                )
            }

            // TODO Slice 2: botón "Grant permissions" si faltan permisos
            // TODO Slice 3: lista de próximas reuniones del día
        }
    }
}
