package com.sierraespada.wakeywakey.billing

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.revenuecat.purchases.kmp.models.Package
import com.revenuecat.purchases.kmp.models.PackageType
import com.sierraespada.wakeywakey.R
import com.sierraespada.wakeywakey.analytics.AnalyticsProvider
import com.sierraespada.wakeywakey.analytics.Event

private val Yellow = Color(0xFFFFE03A)
private val Navy   = Color(0xFF1A1A2E)
private val NavySurface = Color(0xFF16213E)
private val Coral  = Color(0xFFFF6B6B)
private val Green  = Color(0xFF4CAF50)

@Composable
fun PaywallScreen(onDismiss: () -> Unit) {
    val offerings     by EntitlementManager.offerings.collectAsState()
    val trialDaysLeft by EntitlementManager.trialDaysLeft.collectAsState()
    val isLoading by EntitlementManager.isLoading.collectAsState()
    val isPro     by EntitlementManager.isPro.collectAsState()

    // Cerrar automáticamente solo si la compra se completó DURANTE esta sesión
    val wasProOnOpen = remember { isPro }
    LaunchedEffect(isPro) { if (isPro && !wasProOnOpen) onDismiss() }

    // Analytics: paywall_shown al abrir
    LaunchedEffect(Unit) {
        AnalyticsProvider.instance.track(Event.PAYWALL_SHOWN)
    }

    val current   = offerings?.current
    val monthly   = current?.monthly
    val annual    = current?.annual
    val lifetime  = current?.lifetime

    // Plan seleccionado por defecto: anual (mejor valor)
    var selected by remember { mutableStateOf<Package?>(null) }
    LaunchedEffect(annual, monthly, lifetime) {
        if (selected == null) selected = annual ?: monthly ?: lifetime
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Navy)
            .systemBarsPadding()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {

        // ── Cerrar ────────────────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(onClick = {
                AnalyticsProvider.instance.track(Event.PAYWALL_DISMISSED)
                onDismiss()
            }) {
                Text("✕", color = Color.White.copy(alpha = 0.5f), fontSize = 18.sp)
            }
        }

        // ── Hero ──────────────────────────────────────────────────────────────
        Text("⭐", fontSize = 52.sp)
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(R.string.paywall_title),
            fontSize   = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            color      = Yellow,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            stringResource(R.string.paywall_subtitle),
            fontSize  = 15.sp,
            color     = Color.White.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            modifier  = Modifier.padding(horizontal = 32.dp),
        )

        // Banner: trial expirado
        if (trialDaysLeft == 0) {
            Spacer(Modifier.height(12.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Coral.copy(alpha = 0.15f),
                modifier = Modifier.padding(horizontal = 28.dp),
            ) {
                Text(
                    stringResource(R.string.paywall_trial_expired),
                    fontSize  = 13.sp,
                    color     = Coral,
                    textAlign = TextAlign.Center,
                    modifier  = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }

        Spacer(Modifier.height(28.dp))

        // ── Features list ─────────────────────────────────────────────────────
        Column(
            modifier            = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            listOf(
                "⏱  " + stringResource(R.string.paywall_feature_alert_time),
                "📅  " + stringResource(R.string.paywall_feature_calendars),
                "🕐  " + stringResource(R.string.paywall_feature_work_hours),
                "🔲  " + stringResource(R.string.paywall_feature_widget),
                "⚡  " + stringResource(R.string.paywall_feature_tile),
                "💤  " + stringResource(R.string.paywall_feature_snooze),
            ).forEach { feature ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text     = feature,
                        fontSize = 14.sp,
                        color    = Color.White.copy(alpha = 0.85f),
                    )
                }
            }
        }

        Spacer(Modifier.height(28.dp))

        // ── Plan selector ─────────────────────────────────────────────────────
        Column(
            modifier            = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (current == null) {
                Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Yellow, modifier = Modifier.size(32.dp))
                }
            } else {
                annual?.let {
                    PlanCard(
                        pkg        = it,
                        label      = stringResource(R.string.paywall_plan_annual),
                        badge      = stringResource(R.string.paywall_badge_best_value),
                        badgeColor = Green,
                        trialBadge = stringResource(R.string.paywall_badge_trial),
                        isSelected = selected == it,
                        onClick    = { selected = it; AnalyticsProvider.instance.track(Event.PLAN_SELECTED, mapOf("plan" to "annual")) },
                    )
                }
                monthly?.let {
                    PlanCard(
                        pkg        = it,
                        label      = stringResource(R.string.paywall_plan_monthly),
                        badge      = null,
                        badgeColor = Color.Transparent,
                        trialBadge = stringResource(R.string.paywall_badge_trial),
                        isSelected = selected == it,
                        onClick    = { selected = it; AnalyticsProvider.instance.track(Event.PLAN_SELECTED, mapOf("plan" to "monthly")) },
                    )
                }
                lifetime?.let {
                    PlanCard(
                        pkg        = it,
                        label      = stringResource(R.string.paywall_plan_lifetime),
                        badge      = stringResource(R.string.paywall_badge_one_time),
                        badgeColor = Coral,
                        trialBadge = null,
                        isSelected = selected == it,
                        onClick    = { selected = it; AnalyticsProvider.instance.track(Event.PLAN_SELECTED, mapOf("plan" to "lifetime")) },
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // ── CTA ───────────────────────────────────────────────────────────────
        val isLifetime = selected?.packageType == PackageType.LIFETIME
        val ctaLabel   = if (isLifetime) stringResource(R.string.paywall_cta_buy)
                         else            stringResource(R.string.paywall_cta_trial)

        Button(
            onClick  = {
                val pkg = selected ?: return@Button
                val planName = pkg.packageType.name.lowercase()
                AnalyticsProvider.instance.track(Event.PURCHASE_STARTED, mapOf("plan" to planName))
                EntitlementManager.purchase(
                    rcPackage = pkg,
                    onSuccess = {
                        AnalyticsProvider.instance.track(Event.PURCHASE_COMPLETED, mapOf("plan" to planName))
                    },
                    onError   = {
                        AnalyticsProvider.instance.track(Event.PURCHASE_FAILED, mapOf("plan" to planName))
                    },
                )
            },
            enabled  = selected != null && !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .height(56.dp),
            shape    = RoundedCornerShape(16.dp),
            colors   = ButtonDefaults.buttonColors(
                containerColor = Yellow,
                contentColor   = Navy,
            ),
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = Navy, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
            } else {
                Text(ctaLabel, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
            }
        }

        Spacer(Modifier.height(12.dp))

        // ── Restore ───────────────────────────────────────────────────────────
        TextButton(
            onClick = {
                EntitlementManager.restore(
                    onSuccess = { },
                    onError   = { },
                )
            },
            enabled = !isLoading,
        ) {
            Text(
                stringResource(R.string.paywall_restore),
                color    = Color.White.copy(alpha = 0.4f),
                fontSize = 13.sp,
            )
        }

        Text(
            stringResource(R.string.paywall_cancel_anytime),
            fontSize = 11.sp,
            color    = Color.White.copy(alpha = 0.25f),
            modifier = Modifier.padding(bottom = 24.dp),
        )
    }
}

// ─── PlanCard ─────────────────────────────────────────────────────────────────

@Composable
private fun PlanCard(
    pkg:        Package,
    label:      String,
    badge:      String?,
    badgeColor: Color,
    trialBadge: String?,
    isSelected: Boolean,
    onClick:    () -> Unit,
) {
    val borderColor = if (isSelected) Yellow else Color.White.copy(alpha = 0.12f)
    val bgColor     = if (isSelected) Yellow.copy(alpha = 0.08f) else NavySurface

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(14.dp),
            )
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(
            modifier          = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Radio indicator
            RadioButton(
                selected = isSelected,
                onClick  = onClick,
                colors   = RadioButtonDefaults.colors(
                    selectedColor   = Yellow,
                    unselectedColor = Color.White.copy(alpha = 0.3f),
                ),
            )

            Spacer(Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    label,
                    fontSize   = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = Color.White,
                )
                if (trialBadge != null) {
                    Text(
                        trialBadge,
                        fontSize = 11.sp,
                        color    = Yellow.copy(alpha = 0.7f),
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    pkg.storeProduct.price.formatted,
                    fontSize   = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color      = if (isSelected) Yellow else Color.White,
                )
                if (badge != null) {
                    Spacer(Modifier.height(3.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = badgeColor.copy(alpha = 0.2f),
                    ) {
                        Text(
                            badge,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color    = badgeColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }
            }
        }
    }
}
