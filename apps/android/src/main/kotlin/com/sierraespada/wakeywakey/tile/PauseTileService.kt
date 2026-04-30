package com.sierraespada.wakeywakey.tile

import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import com.sierraespada.wakeywakey.settings.SettingsRepository
import com.sierraespada.wakeywakey.widget.MeetingWidgetReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

/**
 * Quick Settings tile que pausa/reanuda las alertas de WakeyWakey.
 *
 * Estado activo (azul)   → alertas pausadas hasta [PAUSE_DURATION_MS] ms
 * Estado inactivo (gris) → alertas activas
 *
 * Requiere Android 7.0+ (API 24). Registrado en el manifest con la action
 * android.service.quicksettings.action.QS_TILE.
 */
@RequiresApi(Build.VERSION_CODES.N)
class PauseTileService : TileService() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onStartListening() {
        super.onStartListening()
        refreshTile()
    }

    override fun onClick() {
        super.onClick()
        scope.launch {
            val repo     = SettingsRepository.getInstance(applicationContext)
            val settings = repo.settings.firstOrNull()
            if (settings?.isPaused == true) {
                repo.clearPause()
            } else {
                repo.pauseUntil(System.currentTimeMillis() + PAUSE_DURATION_MS)
            }
            // Refrescar tile y widget
            refreshTile()
            MeetingWidgetReceiver.update(applicationContext)
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    // ─────────────────────────────────────────────────────────────────────────

    private fun refreshTile() {
        scope.launch {
            val settings = SettingsRepository.getInstance(applicationContext).settings.firstOrNull()
            val isPaused = settings?.isPaused == true
            val tile     = qsTile ?: return@launch

            tile.state    = if (isPaused) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            tile.label    = "WakeyWakey"
            tile.subtitle = if (isPaused) "Pausado" else "Activo"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.stateDescription = if (isPaused) "Alertas pausadas 1 hora" else "Alertas activadas"
            }
            tile.updateTile()
        }
    }

    companion object {
        private const val PAUSE_DURATION_MS = 60 * 60 * 1_000L  // 1 hora
    }
}
