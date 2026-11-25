package com.ncs.nextbus.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Person
import androidx.compose.ui.graphics.vector.ImageVector
import com.ncs.nextbus.R
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

@Serializable data object HomeRoute
@Serializable data object MapRoute
@Serializable data object SavedRoute
@Serializable data object ProfileRoute

/** Destinations shown in the bottom navigation bar, in display order. */
enum class TopLevelDestination(
    val route: Any,
    val routeClass: KClass<*>,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    @param:StringRes val label: Int,
) {
    HOME(HomeRoute, HomeRoute::class, Icons.Filled.Home, Icons.Outlined.Home, R.string.nav_home),
    MAP(MapRoute, MapRoute::class, Icons.Filled.Map, Icons.Outlined.Map, R.string.nav_map),
    SAVED(SavedRoute, SavedRoute::class, Icons.Filled.Bookmarks, Icons.Outlined.Bookmarks, R.string.nav_saved),
    PROFILE(ProfileRoute, ProfileRoute::class, Icons.Filled.Person, Icons.Outlined.Person, R.string.nav_profile),
}
