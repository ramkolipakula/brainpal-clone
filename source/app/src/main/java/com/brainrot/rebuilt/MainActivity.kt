package com.brainrot.rebuilt

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.brainrot.rebuilt.service.UsageMonitorService
import com.brainrot.rebuilt.ui.screens.*
import com.brainrot.rebuilt.ui.theme.BrainPalTheme
import com.brainrot.rebuilt.ui.theme.CoralPrimary

enum class AppTab(val title: String, val icon: ImageVector) {
    HOME("Home", Icons.Default.Home),
    LIMITS("Limits", Icons.Default.Tune),
    STATS("Stats", Icons.Default.BarChart),
    SETTINGS("Settings", Icons.Default.Settings)
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = BrainPalApp.instance

        // Ensure background usage monitor service is running
        try {
            val serviceIntent = Intent(this, UsageMonitorService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        setContent {
            BrainPalTheme(darkTheme = true) {
                var isOnboardingDone by remember { mutableStateOf(app.prefsRepository.isOnboardingCompleted()) }
                var selectedTab by remember { mutableStateOf(AppTab.HOME) }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (!isOnboardingDone) {
                        OnboardingScreen(
                            onFinished = {
                                isOnboardingDone = true
                            }
                        )
                    } else {
                        Scaffold(
                            bottomBar = {
                                NavigationBar(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ) {
                                    AppTab.values().forEach { tab ->
                                        NavigationBarItem(
                                            selected = selectedTab == tab,
                                            onClick = { selectedTab = tab },
                                            icon = { Icon(tab.icon, contentDescription = tab.title) },
                                            label = { Text(tab.title) },
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = CoralPrimary,
                                                selectedTextColor = CoralPrimary,
                                                indicatorColor = CoralPrimary.copy(alpha = 0.15f)
                                            )
                                        )
                                    }
                                }
                            }
                        ) { innerPadding ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding)
                            ) {
                                when (selectedTab) {
                                    AppTab.HOME -> HomeScreen(onNavigateToLimits = { selectedTab = AppTab.LIMITS })
                                    AppTab.LIMITS -> AppLimitsScreen()
                                    AppTab.STATS -> StatsScreen()
                                    AppTab.SETTINGS -> SettingsScreen()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

