package com.ncs.nextbus.ui.navigation

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.navigation.compose.NavHost
import androidx.compose.navigation.compose.composable
import androidx.compose.navigation.compose.rememberNavController
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ncs.nextbus.ui.screens.HomeScreen
import com.ncs.nextbus.ui.screens.MapTrackingScreen
import com.ncs.nextbus.ui.screens.ProfileScreen

@Composable
fun NavHostNavigator(
    navController: androidx.compose.navigation.compose.NavHostController = rememberNavController(),
    startDestination: String = "home"
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = Modifier.fillMaxSize(),
        enterTransition = { _, _ ->
            AnimatedContentScope.() -> {
                fadeIn(
                    animationSpec = tween(
                        durationMillis = 300,
                        easing = LinearOutSlowInEasing
                    )
                ) +
                slideInVertically(
                    initialOffsetY = { it.height },
                    animationSpec = SpringSpec(stiffness = 400f, dampingRatio = 20f)
                )
            }
        },
        exitTransition = { _, _ ->
            AnimatedContentScope.() -> {
                fadeOut(
                    animationSpec = tween(
                        durationMillis = 200,
                        easing = LinearOutSlowInEasing
                    )
                ) +
                slideOutVertically(
                    targetOffsetY = { it.height },
                    animationSpec = SpringSpec(stiffness = 400f, dampingRatio = 20f)
                )
            }
        }
    ) {
        composable(route = "home") {
            HomeScreen(
                navigator = NavHostNavigator(navController = navController, startDestination = "home")
            )
        }

        composable(route = "map") {
            MapTrackingScreen(
                navigator = NavHostNavigator(navController = navController, startDestination = "map")
            )
        }

        composable(route = "profile") {
            ProfileScreen(
                navigator = NavHostNavigator(navController = navController, startDestination = "profile")
            )
        }

        composable(route = "map_details") {
            MapTrackingScreen(
                navigator = NavHostNavigator(navController = navController, startDestination = "map_details")
            )
        }
    }
}