package com.ncs.nextbus.feature.saved

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ncs.nextbus.R
import com.ncs.nextbus.core.designsystem.component.EmptyState
import com.ncs.nextbus.core.designsystem.component.ErrorState
import com.ncs.nextbus.core.designsystem.component.LoadingState
import com.ncs.nextbus.core.designsystem.component.RouteBadge
import com.ncs.nextbus.core.designsystem.component.SectionHeader
import com.ncs.nextbus.core.designsystem.theme.Spacing
import com.ncs.nextbus.core.model.Trip
import com.ncs.nextbus.core.ui.formatDate
import com.ncs.nextbus.core.ui.formatDistance
import com.ncs.nextbus.core.ui.formatDurationMinutes
import com.ncs.nextbus.core.ui.formatRelativeAge
import com.ncs.nextbus.core.ui.rememberNow

private enum class SavedTab(val label: Int, val icon: ImageVector) {
    FAVORITES(R.string.saved_tab_favorites, Icons.Outlined.StarOutline),
    RECENT(R.string.saved_tab_recent, Icons.Outlined.History),
    TRIPS(R.string.saved_tab_trips, Icons.Outlined.Timeline),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedScreen(
    onRouteClick: (routeId: String) -> Unit,
    onJourneyClick: (fromStopId: String, toStopId: String) -> Unit,
    onBusClick: (busId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SavedViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(modifier = modifier, topBar = { TopAppBar(title = { Text(stringResource(R.string.nav_saved)) }) }) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            PrimaryTabRow(selectedTabIndex = tab) {
                SavedTab.entries.forEachIndexed { index, item ->
                    Tab(
                        selected = tab == index,
                        onClick = { tab = index },
                        text = { Text(stringResource(item.label)) },
                        icon = { Icon(item.icon, contentDescription = null) },
                    )
                }
            }
            when (val current = state) {
                SavedUiState.Loading -> LoadingState()
                SavedUiState.Error -> ErrorState()
                is SavedUiState.Success -> when (SavedTab.entries[tab]) {
                    SavedTab.FAVORITES -> FavoritesTab(current.content, viewModel, onRouteClick, onJourneyClick)
                    SavedTab.RECENT -> RecentTab(current.content, viewModel::clearRecentSearches, onJourneyClick)
                    SavedTab.TRIPS -> TripsTab(current.content, viewModel, onBusClick)
                }
            }
        }
    }
}

@Composable
private fun FavoritesTab(
    content: SavedContent,
    viewModel: SavedViewModel,
    onRouteClick: (String) -> Unit,
    onJourneyClick: (String, String) -> Unit,
) {
    if (content.favoriteRoutes.isEmpty() && content.savedJourneys.isEmpty()) {
        EmptyState(
            icon = Icons.Outlined.Bookmarks,
            title = stringResource(R.string.saved_empty_favorites_title),
            message = stringResource(R.string.saved_empty_favorites_body),
        )
        return
    }
    LazyColumn(Modifier.fillMaxSize()) {
        if (content.favoriteRoutes.isNotEmpty()) {
            item { SectionHeader(stringResource(R.string.saved_routes)) }
            items(content.favoriteRoutes, key = { "route-${it.id}" }) { route ->
                ListItem(
                    headlineContent = { Text(route.name) },
                    supportingContent = { Text(stringResource(R.string.route_every_minutes, route.headwayMinutes)) },
                    leadingContent = { RouteBadge(route.number, route.colorArgb) },
                    trailingContent = {
                        IconButton(onClick = { viewModel.removeFavoriteRoute(route.id) }) {
                            Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.route_unfavorite))
                        }
                    },
                    modifier = Modifier.clickable(role = Role.Button) { onRouteClick(route.id) },
                )
            }
        }
        if (content.savedJourneys.isNotEmpty()) {
            item { SectionHeader(stringResource(R.string.saved_journeys)) }
            items(content.savedJourneys, key = { "journey-${it.from.id}-${it.to.id}" }) { journey ->
                ListItem(
                    headlineContent = { Text(stringResource(R.string.journey_from_to, journey.from.name, journey.to.name)) },
                    leadingContent = { Icon(Icons.Outlined.Route, contentDescription = null) },
                    trailingContent = {
                        IconButton(onClick = { viewModel.removeSavedJourney(journey) }) {
                            Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.search_unsave_journey))
                        }
                    },
                    modifier = Modifier.clickable(role = Role.Button) { onJourneyClick(journey.from.id, journey.to.id) },
                )
            }
        }
    }
}

@Composable
private fun RecentTab(content: SavedContent, onClear: () -> Unit, onJourneyClick: (String, String) -> Unit) {
    val now by rememberNow(60_000)
    if (content.recentSearches.isEmpty()) {
        EmptyState(
            icon = Icons.Outlined.History,
            title = stringResource(R.string.saved_empty_recent_title),
            message = stringResource(R.string.saved_empty_recent_body),
        )
        return
    }
    LazyColumn(Modifier.fillMaxSize()) {
        item {
            SectionHeader(
                title = stringResource(R.string.saved_tab_recent),
                actionLabel = stringResource(R.string.saved_clear),
                onAction = onClear,
            )
        }
        items(content.recentSearches, key = { "${it.from.id}-${it.to.id}" }) { recent ->
            ListItem(
                headlineContent = { Text(stringResource(R.string.journey_from_to, recent.from.name, recent.to.name)) },
                supportingContent = { Text(formatRelativeAge(now - recent.searchedAtMillis)) },
                leadingContent = { Icon(Icons.Outlined.History, contentDescription = null) },
                modifier = Modifier.clickable(role = Role.Button) { onJourneyClick(recent.from.id, recent.to.id) },
            )
        }
    }
}

@Composable
private fun TripsTab(content: SavedContent, viewModel: SavedViewModel, onBusClick: (String) -> Unit) {
    var confirmClear by rememberSaveable { mutableStateOf(false) }
    if (content.activeTrip == null && content.pastTrips.isEmpty()) {
        EmptyState(
            icon = Icons.Outlined.Timeline,
            title = stringResource(R.string.saved_empty_trips_title),
            message = stringResource(R.string.saved_empty_trips_body),
        )
        return
    }
    LazyColumn(Modifier.fillMaxSize()) {
        content.activeTrip?.let { trip ->
            item {
                Card(
                    onClick = { onBusClick(trip.busId) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.padding(Spacing.screenHorizontal),
                ) {
                    ListItem(
                        overlineContent = { Text(stringResource(R.string.trip_active_title)) },
                        headlineContent = { Text(trip.routeName) },
                        supportingContent = { Text(stringResource(R.string.trip_boarded_at, trip.boardStopName)) },
                        leadingContent = { RouteBadge(trip.routeNumber, content.colorOf(trip.routeId)) },
                    )
                }
            }
        }
        if (content.pastTrips.isNotEmpty()) {
            item {
                SectionHeader(
                    title = stringResource(R.string.saved_trip_history),
                    actionLabel = stringResource(R.string.saved_clear),
                    onAction = { confirmClear = true },
                )
            }
            items(content.pastTrips, key = { it.id }) { trip ->
                PastTripRow(trip, content.colorOf(trip.routeId), onDelete = { viewModel.deleteTrip(trip.id) })
            }
        }
    }
    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text(stringResource(R.string.saved_clear_history_title)) },
            text = { Text(stringResource(R.string.saved_clear_history_body)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearTripHistory()
                    confirmClear = false
                }) { Text(stringResource(R.string.saved_clear)) }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

@Composable
private fun PastTripRow(trip: Trip, routeColor: Long, onDelete: () -> Unit) {
    val context = LocalContext.current
    val minutes = trip.endedAtMillis?.let { ((it - trip.startedAtMillis) / 60_000).toInt().coerceAtLeast(1) } ?: 0
    ListItem(
        headlineContent = { Text(stringResource(R.string.journey_from_to, trip.boardStopName, trip.alightStopName ?: trip.boardStopName)) },
        supportingContent = {
            Text(
                listOf(
                    formatDate(context, trip.startedAtMillis),
                    formatDurationMinutes(minutes),
                    formatDistance(trip.distanceMeters ?: 0.0),
                ).joinToString(" · "),
            )
        },
        leadingContent = { RouteBadge(trip.routeNumber, routeColor) },
        trailingContent = {
            IconButton(onClick = onDelete) {
                Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.saved_delete_trip))
            }
        },
    )
}
