package com.sierraespada.wakeywakey

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sierraespada.wakeywakey.home.HomeScreen
import com.sierraespada.wakeywakey.onboarding.OnboardingScreen
import com.sierraespada.wakeywakey.onboarding.PermissionsViewModel
import com.sierraespada.wakeywakey.scheduler.SchedulerService
import com.sierraespada.wakeywakey.ui.theme.WakeyWakeyTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { AppRoot() }
    }
}

@Composable
private fun AppRoot() {
    val context = LocalContext.current
    val vm: PermissionsViewModel = viewModel()

    LaunchedEffect(Unit) { vm.refresh(context) }

    val state by vm.state.collectAsState()

    if (state.requiredGranted) {
        LaunchedEffect(Unit) {
            context.startService(Intent(context, SchedulerService::class.java))
        }
        WakeyWakeyTheme {
            HomeScreen()
        }
    } else {
        OnboardingScreen(
            vm                   = vm,
            onAllRequiredGranted = {
                context.startService(Intent(context, SchedulerService::class.java))
                vm.refresh(context)
            },
        )
    }
}
