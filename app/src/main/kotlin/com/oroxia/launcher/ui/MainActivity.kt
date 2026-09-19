package com.oroxia.launcher.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.oroxia.launcher.ui.drawer.AppDrawerScreen
import com.oroxia.launcher.ui.home.HomeScreen
import com.oroxia.launcher.ui.home.HomeViewModel
import com.oroxia.launcher.ui.settings.SettingsScreen
import com.oroxia.launcher.ui.theme.DarkBackground
import com.oroxia.launcher.ui.theme.OroxiaTheme

enum class LauncherScreen {
    HOME,
    DRAWER,
    SETTINGS
}

class MainActivity : ComponentActivity() {
    private val homeViewModel: HomeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OroxiaTheme {
                var currentScreen by remember { mutableStateOf(LauncherScreen.HOME) }

                BackHandler(enabled = currentScreen != LauncherScreen.HOME) {
                    currentScreen = LauncherScreen.HOME
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkBackground
                ) {
                    when (currentScreen) {
                        LauncherScreen.HOME -> {
                            HomeScreen(
                                viewModel = homeViewModel,
                                onOpenDrawer = { currentScreen = LauncherScreen.DRAWER },
                                onOpenSettings = { currentScreen = LauncherScreen.SETTINGS }
                            )
                        }
                        LauncherScreen.DRAWER -> {
                            AppDrawerScreen(
                                viewModel = homeViewModel,
                                onBackToHome = { currentScreen = LauncherScreen.HOME }
                            )
                        }
                        LauncherScreen.SETTINGS -> {
                            SettingsScreen(
                                onBack = { currentScreen = LauncherScreen.HOME }
                            )
                        }
                    }
                }
            }
        }
    }
}
