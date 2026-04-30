package com.sierraespada.wakeywakey.billing

import android.content.Context
import com.revenuecat.purchases.kmp.Purchases
import com.revenuecat.purchases.kmp.models.CustomerInfo
import com.revenuecat.purchases.kmp.models.Offerings
import com.revenuecat.purchases.kmp.models.Package
import com.revenuecat.purchases.kmp.models.PurchasesError
import com.revenuecat.purchases.kmp.models.StoreTransaction
import com.sierraespada.wakeywakey.analytics.AnalyticsProvider
import com.sierraespada.wakeywakey.analytics.Event
import com.sierraespada.wakeywakey.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/**
 * Gestiona el acceso Pro del usuario combinando dos fuentes:
 *
 *  1. Trial de app (14 días desde primera instalación, sin pedir nada)
 *  2. Suscripción o compra única via RevenueCat / Play Store
 *
 * [isPro] = trial activo OR suscripción activa
 */
object EntitlementManager {

    private const val ENTITLEMENT_ID = "WakeyWakey Pro"
    const val TRIAL_DAYS = 14L

    private val scope = CoroutineScope(Dispatchers.IO)

    // ── Estado ────────────────────────────────────────────────────────────────

    /** Días restantes del trial de app (0 = expirado o ya es suscriptor) */
    private val _trialDaysLeft = MutableStateFlow(TRIAL_DAYS.toInt())
    val trialDaysLeft: StateFlow<Int> = _trialDaysLeft.asStateFlow()

    /** true si tiene suscripción/compra activa en RevenueCat */
    private val _isRevenueCatPro = MutableStateFlow(false)

    /** true si trial activo O suscripción activa */
    private val _isPro = MutableStateFlow(true) // true por defecto hasta que init() compruebe
    val isPro: StateFlow<Boolean> = _isPro.asStateFlow()

    private val _offerings = MutableStateFlow<Offerings?>(null)
    val offerings: StateFlow<Offerings?> = _offerings.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // ── Init ──────────────────────────────────────────────────────────────────

    /**
     * Llamar en Application.onCreate() con el contexto.
     * Calcula los días de trial restantes y carga el estado de RevenueCat.
     */
    fun init(context: Context) {
        scope.launch {
            val repo        = SettingsRepository.getInstance(context)
            val isFirstRun  = repo.getInstallDate() == null
            val installDate = repo.getOrSetInstallDate()
            val daysSince   = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - installDate)
            val daysLeft    = (TRIAL_DAYS - daysSince).coerceAtLeast(0L).toInt()
            _trialDaysLeft.value = daysLeft
            updateIsPro()

            // Analytics
            if (isFirstRun) {
                AnalyticsProvider.instance.track(Event.TRIAL_STARTED,
                    mapOf("trial_days" to TRIAL_DAYS))
            }
            if (daysLeft == 0 && !_isRevenueCatPro.value) {
                AnalyticsProvider.instance.track(Event.TRIAL_EXPIRED)
            }
        }
        refresh()
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun CustomerInfo.hasPro(): Boolean =
        entitlements.active.containsKey(ENTITLEMENT_ID)

    private fun updateIsPro() {
        _isPro.value = _trialDaysLeft.value > 0 || _isRevenueCatPro.value
    }

    // ── Refresh desde RevenueCat ──────────────────────────────────────────────

    fun refresh() {
        Purchases.sharedInstance.getCustomerInfo(
            onSuccess = { customerInfo: CustomerInfo ->
                _isRevenueCatPro.value = customerInfo.hasPro()
                updateIsPro()
            },
            onError = { _: PurchasesError -> },
        )
        Purchases.sharedInstance.getOfferings(
            onSuccess = { offerings: Offerings -> _offerings.value = offerings },
            onError   = { _: PurchasesError -> },
        )
    }

    // ── Compra ────────────────────────────────────────────────────────────────

    fun purchase(
        rcPackage: Package,
        onSuccess: () -> Unit,
        onError:   (String) -> Unit,
    ) {
        _isLoading.value = true
        Purchases.sharedInstance.purchase(
            packageToPurchase = rcPackage,
            onError   = { _: PurchasesError, userCancelled: Boolean ->
                _isLoading.value = false
                if (!userCancelled) onError("Purchase failed")
            },
            onSuccess = { _: StoreTransaction, customerInfo: CustomerInfo ->
                _isRevenueCatPro.value = customerInfo.hasPro()
                _isLoading.value       = false
                updateIsPro()
                onSuccess()
            },
        )
    }

    // ── Debug / QA ────────────────────────────────────────────────────────────

    /**
     * SOLO DEBUG. Sobreescribe los días de trial restantes sin tocar DataStore.
     * Útil para verificar que el paywall, los lock icons y los banners se muestran
     * correctamente en cada estado del ciclo de vida del trial.
     *
     * Llamar desde los botones de debug de HomeScreen.
     */
    fun debugSetTrialDays(days: Int) {
        _trialDaysLeft.value = days
        updateIsPro()
    }

    // ── Restaurar ─────────────────────────────────────────────────────────────

    fun restore(
        onSuccess: () -> Unit,
        onError:   (String) -> Unit,
    ) {
        _isLoading.value = true
        Purchases.sharedInstance.restorePurchases(
            onSuccess = { customerInfo: CustomerInfo ->
                _isRevenueCatPro.value = customerInfo.hasPro()
                _isLoading.value       = false
                updateIsPro()
                AnalyticsProvider.instance.track(Event.PURCHASE_RESTORED)
                onSuccess()
            },
            onError = { _: PurchasesError ->
                _isLoading.value = false
                onError("Restore failed")
            },
        )
    }
}
