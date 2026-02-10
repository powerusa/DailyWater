package com.watercamel

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.watercamel.ui.ChartsScreen
import com.watercamel.ui.HistoryScreen
import com.watercamel.ui.HomeScreen
import com.watercamel.ui.SettingsScreen
import com.watercamel.ui.theme.DailyWaterTheme
import com.watercamel.viewmodel.WaterViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val waterViewModel: WaterViewModel = viewModel()
            val isDarkMode by waterViewModel.isDarkMode.collectAsState()
            val isDarkModeSet by waterViewModel.isDarkModeSet.collectAsState()
            val systemDark = isSystemInDarkTheme()
            val effectiveDark = if (isDarkModeSet) isDarkMode else systemDark

            DailyWaterTheme(darkTheme = effectiveDark) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {

                    // Observe lifecycle: on every resume (foreground), check if the
                    // calendar day has changed and reset intake if needed.
                    // This uses the local timezone dateKey (yyyy-MM-dd).
                    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
                    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
                        val observer = LifecycleEventObserver { _, event ->
                            if (event == Lifecycle.Event.ON_RESUME) {
                                waterViewModel.checkForNewDayAndResetIfNeeded()
                            }
                        }
                        lifecycleOwner.lifecycle.addObserver(observer)
                        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                    }

                    DailyWaterNavHost(waterViewModel)
                }
            }
        }
    }
}

@Composable
fun DailyWaterNavHost(viewModel: WaterViewModel) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                viewModel = viewModel,
                onNavigateToSettings = { navController.navigate("settings") },
                onNavigateToHistory = { navController.navigate("history") },
                onNavigateToCharts = { navController.navigate("charts") }
            )
        }
        composable("history") {
            HistoryScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable("settings") {
            SettingsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable("charts") {
            ChartsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
