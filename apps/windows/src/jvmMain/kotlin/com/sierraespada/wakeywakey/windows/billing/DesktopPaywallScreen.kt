package com.sierraespada.wakeywakey.windows.billing

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import com.sierraespada.wakeywakey.windows.ui.HourglassTop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.rememberWindowState
import com.sierraespada.wakeywakey.windows.AppColorScheme
import com.sierraespada.wakeywakey.windows.AppIcon
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.awt.Desktop
import java.net.URI

// ─── Paleta ──────────────────────────────────────────────────────────────────

private val Yellow   = Color(0xFFFFE03A)
private val Navy     = Color(0xFF1A1A2E)
private val Surface  = Color(0xFF16213E)
private val Surface2 = Color(0xFF0F3460)
private val White    = Color.White
private val Subtitle = Color(0xFF8892AA)
private val Danger   = Color(0xFFFF6B6B)
private val Green    = Color(0xFF4CAF50)

// ─── Ventana paywall ──────────────────────────────────────────────────────────

@Composable
fun DesktopPaywallWindow(
    trialDaysLeft: Int,
    onDismiss:     () -> Unit,
) {
    Window(
        onCloseRequest = onDismiss,
        state          = rememberWindowState(size = DpSize(520.dp, 780.dp)),
        title          = "WakeyWakey Pro",
        icon           = AppIcon,
        resizable      = false,
    ) {
        MaterialTheme(colorScheme = AppColorScheme) {
            DesktopPaywallScreen(trialDaysLeft = trialDaysLeft, onDismiss = onDismiss)
        }
    }
}

// ─── Pantalla paywall ─────────────────────────────────────────────────────────

@Composable
fun DesktopPaywallScreen(
    trialDaysLeft: Int,
    onDismiss:     () -> Unit,
) {
    var selectedPlan     by remember { mutableStateOf(Plan.ANNUAL) }
    var showLicenseInput by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Navy)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {

        // ── Header compacto ───────────────────────────────────────────────────
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("⏰", fontSize = 28.sp)
            Text(
                "WakeyWakey Pro",
                fontSize   = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color      = Yellow,
            )
        }

        // ── Banner trial / expirado ───────────────────────────────────────────
        if (trialDaysLeft > 0) {
            TrialBanner(trialDaysLeft)
        } else {
            ExpiredBanner()
        }

        // ── Free vs Pro ───────────────────────────────────────────────────────
        FreeVsProTable()

        // ── Selector de plan ──────────────────────────────────────────────────
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Plan.entries.forEach { plan ->
                PlanCard(
                    plan     = plan,
                    selected = selectedPlan == plan,
                    onClick  = { selectedPlan = plan },
                )
            }
        }

        // ── CTA principal ─────────────────────────────────────────────────────
        Button(
            onClick  = { openCheckout(selectedPlan) },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape    = RoundedCornerShape(12.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = Yellow, contentColor = Navy),
        ) {
            Text(selectedPlan.ctaLabel, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
        }

        // ── Activar licencia / continuar trial ────────────────────────────────
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            TextButton(onClick = { showLicenseInput = !showLicenseInput }) {
                Text("Have a license key?", color = Subtitle, fontSize = 11.sp)
            }
            if (trialDaysLeft > 0) {
                TextButton(onClick = onDismiss) {
                    Text("Continue trial ($trialDaysLeft days left)", color = Subtitle, fontSize = 11.sp)
                }
            }
        }

        if (showLicenseInput) {
            LicenseActivationRow(onActivated = onDismiss)
        }

        Text(
            "Secure checkout via LemonSqueezy · VAT included",
            color     = Subtitle.copy(alpha = 0.5f),
            fontSize  = 9.sp,
            textAlign = TextAlign.Center,
        )
    }
}

// ─── Banners ──────────────────────────────────────────────────────────────────

@Composable
private fun TrialBanner(daysLeft: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Yellow.copy(alpha = 0.10f))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(Icons.Filled.HourglassTop, null, tint = Yellow, modifier = Modifier.size(16.dp))
        Column {
            Text(
                "$daysLeft days left in your free trial",
                color = Yellow, fontWeight = FontWeight.SemiBold, fontSize = 12.sp,
            )
            Text(
                "All Pro features unlocked · No credit card needed",
                color = Yellow.copy(alpha = 0.6f), fontSize = 10.sp,
            )
        }
    }
}

@Composable
private fun ExpiredBanner() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Danger.copy(alpha = 0.10f))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(Icons.Filled.Warning, null, tint = Danger, modifier = Modifier.size(16.dp))
        Column {
            Text(
                "Your trial has expired",
                color = Danger, fontWeight = FontWeight.SemiBold, fontSize = 12.sp,
            )
            Text(
                "Upgrade to keep all your calendars and unlimited alerts",
                color = Danger.copy(alpha = 0.7f), fontSize = 10.sp,
            )
        }
    }
}

// ─── Free vs Pro table ────────────────────────────────────────────────────────

private data class FeatureRow(
    val label:   String,
    val free:    String,   // texto en columna Free
    val pro:     String,   // texto en columna Pro
    val proGood: Boolean = true,
)

private val FEATURE_ROWS = listOf(
    FeatureRow("Calendars",         "1",             "Unlimited"),
    FeatureRow("Daily alerts",      "3 / day",       "Unlimited"),
    FeatureRow("Alert sounds",      "3 sounds",      "All 15 sounds"),
    FeatureRow("Custom snooze",     "—",             "✓"),
    FeatureRow("Work hours filter", "—",             "✓"),
    FeatureRow("Multiple accounts", "—",             "Google + Outlook"),
)

@Composable
private fun FreeVsProTable() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Surface),
    ) {
        // Cabecera
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Surface2)
                .padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
            Text("", modifier = Modifier.weight(1.8f))
            Text(
                "Free", color = Subtitle, fontSize = 10.sp,
                fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
            )
            Text(
                "Pro", color = Yellow, fontSize = 10.sp,
                fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
            )
        }
        FEATURE_ROWS.forEachIndexed { idx, row ->
            if (idx > 0) HorizontalDivider(color = White.copy(alpha = 0.05f))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(row.label, color = White, fontSize = 11.sp, modifier = Modifier.weight(1.8f))
                Text(
                    row.free,
                    color      = Subtitle,
                    fontSize   = 11.sp,
                    textAlign  = TextAlign.Center,
                    modifier   = Modifier.weight(1f),
                )
                Text(
                    row.pro,
                    color      = if (row.proGood) Green else White,
                    fontSize   = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign  = TextAlign.Center,
                    modifier   = Modifier.weight(1f),
                )
            }
        }
    }
}

// ─── Plan selector ────────────────────────────────────────────────────────────

private enum class Plan(
    val label:    String,
    val price:    String,
    val detail:   String,
    val badge:    String?,
    val ctaLabel: String,
    val url:      String,
) {
    ANNUAL(
        label    = "Annual",
        price    = DesktopEntitlementManager.PRICE_ANNUAL,
        detail   = "/year  ·  ~€1.25/mo",
        badge    = "Best value",
        ctaLabel = "Get Annual — ${DesktopEntitlementManager.PRICE_ANNUAL}/yr",
        url      = DesktopEntitlementManager.CHECKOUT_ANNUAL,
    ),
    MONTHLY(
        label    = "Monthly",
        price    = DesktopEntitlementManager.PRICE_MONTHLY,
        detail   = "/month  ·  cancel anytime",
        badge    = null,
        ctaLabel = "Get Monthly — ${DesktopEntitlementManager.PRICE_MONTHLY}/mo",
        url      = DesktopEntitlementManager.CHECKOUT_MONTHLY,
    ),
    LIFETIME(
        label    = "Lifetime",
        price    = DesktopEntitlementManager.PRICE_LIFETIME,
        detail   = "one-time  ·  forever",
        badge    = "No subscription",
        ctaLabel = "Get Lifetime — ${DesktopEntitlementManager.PRICE_LIFETIME}",
        url      = DesktopEntitlementManager.CHECKOUT_LIFETIME,
    ),
}

@Composable
private fun PlanCard(plan: Plan, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) Yellow.copy(alpha = 0.07f) else Surface)
            .border(1.dp, if (selected) Yellow else White.copy(alpha = 0.07f), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            RadioButton(
                selected = selected, onClick = onClick,
                colors   = RadioButtonDefaults.colors(selectedColor = Yellow, unselectedColor = Subtitle),
                modifier = Modifier.size(20.dp),
            )
            Column {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                ) {
                    Text(plan.label, color = White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    if (plan.badge != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(3.dp))
                                .background(Yellow.copy(alpha = 0.18f))
                                .padding(horizontal = 5.dp, vertical = 1.dp),
                        ) {
                            Text(plan.badge, color = Yellow, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Text(plan.detail, color = Subtitle, fontSize = 10.sp)
            }
        }
        Text(
            plan.price,
            color = if (selected) Yellow else White,
            fontWeight = FontWeight.ExtraBold,
            fontSize   = 15.sp,
        )
    }
}

// ─── Activar licencia ─────────────────────────────────────────────────────────

@Composable
private fun LicenseActivationRow(onActivated: () -> Unit) {
    var key     by remember { mutableStateOf("") }
    var error   by remember { mutableStateOf<String?>(null) }
    var success by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Surface)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value         = key,
                onValueChange = { key = it.uppercase().take(50); error = null },
                placeholder   = { Text("License key", color = Subtitle, fontSize = 12.sp) },
                singleLine    = true,
                modifier      = Modifier.weight(1f).height(48.dp),
                colors        = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = Yellow,
                    unfocusedBorderColor = White.copy(alpha = 0.2f),
                    focusedTextColor     = White,
                    unfocusedTextColor   = White,
                ),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
            )
            Button(
                onClick = {
                    if (key.length < 10) { error = "Invalid key"; return@Button }
                    DesktopEntitlementManager.activateLicense(key)
                    success = true
                    CoroutineScope(Dispatchers.Default).launch {
                        delay(1200)
                        onActivated()
                    }
                },
                enabled  = key.isNotBlank() && !success,
                modifier = Modifier.height(48.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = Yellow, contentColor = Navy),
                shape    = RoundedCornerShape(8.dp),
            ) {
                Text(if (success) "✓" else "Activate", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
        if (error != null)  Text(error!!, color = Danger, fontSize = 10.sp)
        if (success)        Text("✅ License activated!", color = Green, fontSize = 11.sp)
    }
}

// ─── Helper ───────────────────────────────────────────────────────────────────

private fun openCheckout(plan: Plan) {
    runCatching { Desktop.getDesktop().browse(URI(plan.url)) }
}
