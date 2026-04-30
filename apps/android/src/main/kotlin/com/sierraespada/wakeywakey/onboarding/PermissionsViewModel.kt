package com.sierraespada.wakeywakey.onboarding

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class PermissionsState(
    val calendar: Boolean            = false,
    val notifications: Boolean       = false,
    val exactAlarm: Boolean          = false,
    val batteryOptimization: Boolean = false,   // true = ignorando la optimización (bueno)
    val fullScreenIntent: Boolean    = false,   // true = permitido (Android 14+)
    val overlay: Boolean             = false,   // SYSTEM_ALERT_WINDOW → full-screen con teléfono desbloqueado
) {
    /**
     * Los permisos sin los que la app no puede funcionar en absoluto.
     * batteryOptimization, fullScreenIntent y overlay son "muy recomendados" pero no bloquean el flujo.
     */
    val requiredGranted: Boolean get() = calendar && notifications && exactAlarm
    val allGranted: Boolean get() = requiredGranted && batteryOptimization && fullScreenIntent && overlay
}

class PermissionsViewModel : ViewModel() {

    private val _state = MutableStateFlow(PermissionsState())
    val state: StateFlow<PermissionsState> = _state.asStateFlow()

    /** Llámalo en onResume para refrescar el estado tras volver de Settings. */
    fun refresh(context: Context) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val powerManager = context.getSystemService(PowerManager::class.java)
        val notifManager = context.getSystemService(NotificationManager::class.java)

        _state.update {
            PermissionsState(
                calendar = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.READ_CALENDAR
                ) == PackageManager.PERMISSION_GRANTED,

                notifications = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    ContextCompat.checkSelfPermission(
                        context, Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                } else {
                    true    // Android < 13: POST_NOTIFICATIONS no existe, siempre concedido
                },

                exactAlarm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    alarmManager.canScheduleExactAlarms()
                } else {
                    true    // Android < 12: exact alarms siempre permitidas
                },

                batteryOptimization = powerManager.isIgnoringBatteryOptimizations(context.packageName),

                fullScreenIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    notifManager.canUseFullScreenIntent()
                } else {
                    true    // Android < 14: siempre permitido
                },

                overlay = Settings.canDrawOverlays(context),
            )
        }
    }
}
