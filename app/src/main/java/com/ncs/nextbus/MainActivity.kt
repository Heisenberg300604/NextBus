package com.ncs.nextbus

import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ncs.nextbus.core.designsystem.theme.NextBusTheme
import com.ncs.nextbus.core.model.ThemeMode
import com.ncs.nextbus.feature.onboarding.OnboardingScreen
import com.ncs.nextbus.ui.NextBusApp
import dagger.hilt.android.AndroidEntryPoint

/**
 * Single activity host. It extends [AppCompatActivity] (not plain ComponentActivity) so that
 * per-app language changes via `AppCompatDelegate.setApplicationLocales` also apply on Android 12 and below.
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // Keep the splash up until preferences (theme, onboarding state) are known, to avoid a flash.
        splashScreen.setKeepOnScreenCondition { viewModel.uiState.value is MainUiState.Loading }

        setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            val preferences = (state as? MainUiState.Ready)?.preferences ?: return@setContent

            val darkTheme = when (preferences.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            // System bar icons must follow the app theme, not just the system setting.
            DisposableEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme },
                    navigationBarStyle = SystemBarStyle.auto(LIGHT_SCRIM, DARK_SCRIM) { darkTheme },
                )
                onDispose { }
            }

            NextBusTheme(darkTheme = darkTheme, dynamicColor = preferences.useDynamicColor) {
                if (preferences.onboardingCompleted) {
                    NextBusApp(onLocationPermissionChanged = viewModel::refreshLocationPermission)
                } else {
                    OnboardingScreen(
                        onLocationPermissionResult = { viewModel.refreshLocationPermission() },
                        onFinished = viewModel::completeOnboarding,
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // The user may have changed location permission in system settings while we were paused.
        viewModel.refreshLocationPermission()
    }

    private companion object {
        val LIGHT_SCRIM = Color.argb(0xe6, 0xFF, 0xFF, 0xFF)
        val DARK_SCRIM = Color.argb(0x80, 0x1b, 0x1b, 0x1b)
    }
}
