package com.sierraespada.wakeywakey

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sierraespada.wakeywakey.billing.PaywallScreen
import com.sierraespada.wakeywakey.home.HomeScreen
import com.sierraespada.wakeywakey.onboarding.PermissionsViewModel
import com.sierraespada.wakeywakey.onboarding.WelcomeOnboarding
import com.sierraespada.wakeywakey.scheduler.SchedulerService
import com.sierraespada.wakeywakey.settings.SettingsRepository
import com.sierraespada.wakeywakey.settings.SettingsScreen
import com.sierraespada.wakeywakey.ui.theme.WakeyWakeyTheme
import com.sierraespada.wakeywakey.worker.CalendarSyncWorker
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            AppRoot(windowSizeClass = windowSizeClass)
        }
    }
}

@Composable
private fun AppRoot(windowSizeClass: WindowSizeClass) {
    val context  = LocalContext.current
    val scope    = rememberCoroutineScope()
    val vm: PermissionsViewModel = viewModel()
    val isTablet = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded

    LaunchedEffect(Unit) { vm.refresh(context) }

    val repo = remember { SettingsRepository.getInstance(context) }
    var onboardingDone by remember {
        mutableStateOf(runBlocking { repo.onboardingCompleted.firstOrNull() } == true)
    }

    var showSettings by remember { mutableStateOf(false) }
    var showPaywall  by remember { mutableStateOf(false) }

    fun startServices() {
        context.startService(Intent(context, SchedulerService::class.java))
        CalendarSyncWorker.enqueue(context)
    }

    if (!onboardingDone) {
        WelcomeOnboarding(
            permVm     = vm,
            onComplete = {
                startServices()
                onboardingDone = true
            },
        )
    } else {
        LaunchedEffect(Unit) { startServices() }
        WakeyWakeyTheme {
            when {
                showPaywall  -> PaywallScreen(onDismiss = { showPaywall = false })
                showSettings -> SettingsScreen(
                    onBack        = { showSettings = false },
                    onShowPaywall = { showPaywall = true },
                    isTablet      = isTablet,
                )
                else         -> HomeScreen(
                    onOpenSettings    = { showSettings = true },
                    onShowPaywall     = { showPaywall = true },
                    isTablet          = isTablet,
                    onResetOnboarding = {
                        scope.launch { repo.resetOnboarding() }
                        onboardingDone = false
                    },
                )
            }
        }
    }
}
