package com.ncs.nextbus

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.ncs.nextbus.core.designsystem.theme.NextBusTheme
import com.ncs.nextbus.ui.NextBusApp
import dagger.hilt.android.AndroidEntryPoint

/**
 * Single activity host. It extends [AppCompatActivity] (not plain ComponentActivity) so that
 * per-app language changes via `AppCompatDelegate.setApplicationLocales` also apply on Android 12 and below.
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            NextBusTheme {
                NextBusApp()
            }
        }
    }
}
