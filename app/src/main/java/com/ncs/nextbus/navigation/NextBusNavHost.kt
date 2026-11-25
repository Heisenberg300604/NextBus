package com.ncs.nextbus.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.ncs.nextbus.feature.bus.BusDetailScreen
import com.ncs.nextbus.feature.home.HomeScreen
import com.ncs.nextbus.feature.home.HomeViewModel
import com.ncs.nextbus.feature.map.MapScreen
import com.ncs.nextbus.feature.profile.ProfileScreen
import com.ncs.nextbus.feature.route.RouteDetailScreen
import com.ncs.nextbus.feature.saved.SavedScreen
import com.ncs.nextbus.feature.search.SearchResultsScreen
import com.ncs.nextbus.feature.search.StopPickerScreen
import com.ncs.nextbus.feature.sos.SosScreen
import com.ncs.nextbus.feature.stop.StopDetailScreen
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

private const val TRANSITION_MILLIS = 250

private val enter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    fadeIn(tween(TRANSITION_MILLIS)) + slideInHorizontally(tween(TRANSITION_MILLIS)) { it / 12 }
}
private val exit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    fadeOut(tween(TRANSITION_MILLIS / 2))
}
private val popEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    fadeIn(tween(TRANSITION_MILLIS))
}
private val popExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    fadeOut(tween(TRANSITION_MILLIS / 2)) + slideOutHorizontally(tween(TRANSITION_MILLIS)) { it / 12 }
}

@Composable
fun NextBusNavHost(
    navController: NavHostController,
    onLocationPermissionChanged: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val back: () -> Unit = { navController.navigateUp() }
    val toBus: (String) -> Unit = { navController.navigate(BusDetailRoute(it)) }
    val toRoute: (String) -> Unit = { navController.navigate(RouteDetailRoute(it)) }
    val toStop: (String) -> Unit = { navController.navigate(StopDetailRoute(it)) }
    val toSearch: (String, String) -> Unit = { from, to -> navController.navigate(SearchResultsRoute(from, to)) }
    val toSos: () -> Unit = { navController.navigate(SosRoute) { launchSingleTop = true } }

    NavHost(
        navController = navController,
        startDestination = HomeRoute,
        modifier = modifier,
        enterTransition = enter,
        exitTransition = exit,
        popEnterTransition = popEnter,
        popExitTransition = popExit,
    ) {
        composable<HomeRoute> { entry ->
            val viewModel: HomeViewModel = hiltViewModel()
            // Stop picker results come back through this entry's SavedStateHandle.
            LaunchedEffect(entry) {
                StopField.entries.forEach { field ->
                    val key = NavResults.keyFor(field)
                    launch {
                        entry.savedStateHandle.getStateFlow<String?>(key, null).filterNotNull().collect { stopId ->
                            viewModel.onStopPicked(field, stopId)
                            entry.savedStateHandle.remove<String>(key)
                        }
                    }
                }
            }
            HomeScreen(
                onPickStop = { navController.navigate(StopPickerRoute(it.name)) },
                onSearch = toSearch,
                onBusClick = toBus,
                onStopClick = toStop,
                onRouteClick = toRoute,
                onOpenMap = { navController.navigateToTopLevel(TopLevelDestination.MAP) },
                onOpenSos = toSos,
                onLocationPermissionChanged = onLocationPermissionChanged,
                viewModel = viewModel,
            )
        }
        composable<MapRoute> { MapScreen(onBusClick = toBus) }
        composable<SavedRoute> {
            SavedScreen(onRouteClick = toRoute, onJourneyClick = toSearch, onBusClick = toBus)
        }
        composable<ProfileRoute> {
            ProfileScreen(onLocationPermissionChanged = onLocationPermissionChanged, onOpenSos = toSos)
        }
        composable<StopPickerRoute> {
            StopPickerScreen(
                onBack = back,
                onStopSelected = { field, stopId ->
                    navController.previousBackStackEntry?.savedStateHandle?.set(NavResults.keyFor(field), stopId)
                    navController.popBackStack()
                },
            )
        }
        composable<SearchResultsRoute> {
            SearchResultsScreen(onBack = back, onBusClick = toBus, onRouteClick = toRoute)
        }
        composable<BusDetailRoute> {
            BusDetailScreen(onBack = back, onRouteClick = toRoute, onStopClick = toStop, onOpenSos = toSos)
        }
        composable<RouteDetailRoute> {
            RouteDetailScreen(onBack = back, onBusClick = toBus, onStopClick = toStop)
        }
        composable<StopDetailRoute> {
            StopDetailScreen(onBack = back, onBusClick = toBus, onRouteClick = toRoute)
        }
        composable<SosRoute> {
            SosScreen(
                onBack = back,
                onManageContacts = { navController.navigateToTopLevel(TopLevelDestination.PROFILE) },
            )
        }
    }
}

/** Standard bottom-nav behaviour: single top, and each tab keeps its own back stack state. */
fun NavHostController.navigateToTopLevel(destination: TopLevelDestination) {
    navigate(destination.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
