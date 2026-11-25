package com.ncs.nextbus.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.ncs.nextbus.feature.home.HomeScreen
import com.ncs.nextbus.feature.map.MapScreen
import com.ncs.nextbus.feature.profile.ProfileScreen
import com.ncs.nextbus.feature.saved.SavedScreen

@Composable
fun NextBusNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = HomeRoute,
        modifier = modifier,
    ) {
        composable<HomeRoute> { HomeScreen() }
        composable<MapRoute> { MapScreen() }
        composable<SavedRoute> { SavedScreen() }
        composable<ProfileRoute> { ProfileScreen() }
    }
}
