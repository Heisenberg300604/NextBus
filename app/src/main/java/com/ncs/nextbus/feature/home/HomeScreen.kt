package com.ncs.nextbus.feature.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.TripOrigin
import androidx.compose.material.icons.outlined.DirectionsBus
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.LocationOff
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ncs.nextbus.R
import com.ncs.nextbus.core.designsystem.component.BusListItem
import com.ncs.nextbus.core.designsystem.component.ErrorState
import com.ncs.nextbus.core.designsystem.component.FreshnessIndicator
import com.ncs.nextbus.core.designsystem.component.InlineBanner
import com.ncs.nextbus.core.designsystem.component.LoadingState
import com.ncs.nextbus.core.designsystem.component.RouteBadge
import com.ncs.nextbus.core.designsystem.component.SectionHeader
import com.ncs.nextbus.core.designsystem.theme.NextBusTheme
import com.ncs.nextbus.core.designsystem.theme.Sizes
import com.ncs.nextbus.core.designsystem.theme.Spacing
import com.ncs.nextbus.core.model.LiveBus
import com.ncs.nextbus.core.model.LocationSource
import com.ncs.nextbus.core.model.ReferenceLocation
import com.ncs.nextbus.core.model.Stop
import com.ncs.nextbus.core.model.Trip
import com.ncs.nextbus.core.ui.busStatusLine
import com.ncs.nextbus.core.ui.etaToNextStopSeconds
import com.ncs.nextbus.core.ui.formatDistance
import com.ncs.nextbus.core.ui.formatEta
import com.ncs.nextbus.core.ui.hasLocationPermission
import com.ncs.nextbus.core.ui.openAppSettings
import com.ncs.nextbus.core.ui.rememberLocationPermissionRequest
import com.ncs.nextbus.core.ui.rememberNow
import com.ncs.nextbus.core.ui.towards
import com.ncs.nextbus.navigation.StopField
import java.util.Calendar

@Composable
fun HomeScreen(
    onPickStop: (StopField) -> Unit,
    onSearch: (fromStopId: String, toStopId: String) -> Unit,
    onBusClick: (busId: String) -> Unit,
    onStopClick: (stopId: String) -> Unit,
    onRouteClick: (routeId: String) -> Unit,
    onOpenMap: () -> Unit,
    onOpenSos: () -> Unit,
    onLocationPermissionChanged: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Scaffold(modifier = modifier) { padding ->
        when (val current = state) {
            HomeUiState.Loading -> LoadingState(Modifier.padding(padding), rows = 5)
            HomeUiState.Error -> ErrorState(Modifier.padding(padding), onRetry = viewModel::retry)
            is HomeUiState.Success -> HomeContentList(
                content = current.content,
                contentPadding = padding,
                onPickStop = onPickStop,
                onSwap = viewModel::swapStops,
                onSearch = onSearch,
                onBusClick = onBusClick,
                onStopClick = onStopClick,
                onRouteClick = onRouteClick,
                onOpenMap = onOpenMap,
                onOpenSos = onOpenSos,
                onLocationPermissionChanged = onLocationPermissionChanged,
            )
        }
    }
}

@Composable
private fun HomeContentList(
    content: HomeContent,
    contentPadding: PaddingValues,
    onPickStop: (StopField) -> Unit,
    onSwap: () -> Unit,
    onSearch: (String, String) -> Unit,
    onBusClick: (String) -> Unit,
    onStopClick: (String) -> Unit,
    onRouteClick: (String) -> Unit,
    onOpenMap: () -> Unit,
    onOpenSos: () -> Unit,
    onLocationPermissionChanged: () -> Unit,
) {
    val now by rememberNow()
    LazyColumn(
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding() + Spacing.lg,
            bottom = contentPadding.calculateBottomPadding() + Spacing.xl,
        ),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        item { Greeting(content.displayName) }
        item {
            JourneyPlannerCard(
                from = content.from,
                to = content.to,
                isSameStop = content.isSameStop,
                canSearch = content.canSearch,
                onPickStop = onPickStop,
                onSwap = onSwap,
                onSearch = { if (content.from != null && content.to != null) onSearch(content.from.id, content.to.id) },
            )
        }
        if (!content.reference.isDevice) {
            item { LocationBanner(content.reference, onLocationPermissionChanged) }
        }
        content.activeTrip?.let { trip ->
            item { ActiveTripCard(trip, content.activeTripBus, now, onTrack = { onBusClick(trip.busId) }, onSos = onOpenSos) }
        }
        content.nearestStop?.let { nearest ->
            item { SectionHeader(stringResource(R.string.home_nearest_stop)) }
            item {
                NearestStopCard(
                    stop = nearest.stop,
                    distanceMeters = nearest.distanceMeters,
                    arrivals = content.nearestStopArrivals,
                    onClick = { onStopClick(nearest.stop.id) },
                    onBusClick = onBusClick,
                )
            }
        }
        if (content.favoriteRoutes.isNotEmpty()) {
            item { SectionHeader(stringResource(R.string.home_favorite_routes)) }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = Spacing.screenHorizontal),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    items(content.favoriteRoutes, key = { it.id }) { route ->
                        Card(
                            onClick = { onRouteClick(route.id) },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                        ) {
                            Row(
                                modifier = Modifier.padding(Spacing.md),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RouteBadge(route.number, route.colorArgb)
                                Spacer(Modifier.width(Spacing.sm))
                                Text(route.name, style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            }
        }
        if (content.recentSearches.isNotEmpty()) {
            item { SectionHeader(stringResource(R.string.home_recent_searches)) }
            items(content.recentSearches, key = { "${it.from.id}-${it.to.id}" }) { recent ->
                ListItem(
                    headlineContent = { Text(stringResource(R.string.journey_from_to, recent.from.name, recent.to.name), maxLines = 2) },
                    leadingContent = { Icon(Icons.Outlined.History, contentDescription = null) },
                    modifier = Modifier.clickable(role = Role.Button) { onSearch(recent.from.id, recent.to.id) },
                )
            }
        }
        item {
            SectionHeader(
                title = stringResource(R.string.home_nearby_buses),
                actionLabel = stringResource(R.string.home_see_map),
                onAction = onOpenMap,
            )
        }
        if (content.nearbyBuses.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.home_no_nearby_buses),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = Spacing.screenHorizontal),
                )
            }
        }
        items(content.nearbyBuses, key = { it.liveBus.bus.id }) { nearby ->
            BusListItem(
                liveBus = nearby.liveBus,
                nowMillis = now,
                trailingPrimary = formatDistance(nearby.distanceMeters),
                trailingSecondary = stringResource(R.string.home_distance_away),
                onClick = { onBusClick(nearby.liveBus.bus.id) },
            )
        }
    }
}

@Composable
private fun Greeting(name: String) {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greeting = stringResource(
        when {
            hour < 12 -> R.string.home_greeting_morning
            hour < 17 -> R.string.home_greeting_afternoon
            else -> R.string.home_greeting_evening
        },
    )
    Column(Modifier.padding(horizontal = Spacing.screenHorizontal)) {
        Text(
            text = if (name.isBlank()) greeting else stringResource(R.string.home_greeting_named, greeting, name),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = stringResource(R.string.home_where_to),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Spacing.sm))
    }
}

@Composable
private fun JourneyPlannerCard(
    from: Stop?,
    to: Stop?,
    isSameStop: Boolean,
    canSearch: Boolean,
    onPickStop: (StopField) -> Unit,
    onSwap: () -> Unit,
    onSearch: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.screenHorizontal),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(Spacing.lg)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    StopSelector(
                        label = stringResource(R.string.journey_from),
                        value = from?.name,
                        placeholder = stringResource(R.string.journey_choose_from),
                        icon = { Icon(Icons.Filled.TripOrigin, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        onClick = { onPickStop(StopField.FROM) },
                    )
                    HorizontalDivider(Modifier.padding(start = 40.dp))
                    StopSelector(
                        label = stringResource(R.string.journey_to),
                        value = to?.name,
                        placeholder = stringResource(R.string.journey_choose_to),
                        icon = { Icon(Icons.Filled.Place, contentDescription = null, tint = NextBusTheme.extendedColors.sos) },
                        onClick = { onPickStop(StopField.TO) },
                    )
                }
                IconButton(onClick = onSwap, enabled = from != null || to != null) {
                    Icon(Icons.Filled.SwapVert, contentDescription = stringResource(R.string.journey_swap))
                }
            }
            if (isSameStop) {
                Text(
                    text = stringResource(R.string.journey_same_stop),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = Spacing.xs),
                )
            }
            Spacer(Modifier.height(Spacing.md))
            Button(
                onClick = onSearch,
                enabled = canSearch,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = Sizes.touchTarget),
            ) {
                Text(stringResource(R.string.journey_find_buses))
                Spacer(Modifier.width(Spacing.sm))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun StopSelector(
    label: String,
    value: String?,
    placeholder: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(role = Role.Button, onClickLabel = label, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon()
        Spacer(Modifier.width(Spacing.lg))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = value ?: placeholder,
                style = MaterialTheme.typography.bodyLarge,
                color = if (value == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun LocationBanner(reference: ReferenceLocation, onLocationPermissionChanged: () -> Unit) {
    val context = LocalContext.current
    var askedOnce by rememberSaveable { mutableStateOf(false) }
    val request = rememberLocationPermissionRequest {
        askedOnce = true
        onLocationPermissionChanged()
    }
    val (message, action) = when (reference.source) {
        LocationSource.NO_PERMISSION -> R.string.location_banner_no_permission to R.string.location_banner_turn_on
        LocationSource.LOCATING -> R.string.location_banner_locating to null
        LocationSource.OUT_OF_SERVICE_AREA -> R.string.location_banner_out_of_area to null
        LocationSource.UNAVAILABLE -> R.string.location_banner_unavailable to null
        LocationSource.DEVICE -> return
    }
    InlineBanner(
        icon = if (reference.source == LocationSource.LOCATING) Icons.Outlined.MyLocation else Icons.Outlined.LocationOff,
        message = stringResource(message),
        actionLabel = action?.let { stringResource(it) },
        onAction = {
            if (askedOnce && !context.hasLocationPermission()) context.openAppSettings() else request()
        },
        modifier = Modifier.padding(horizontal = Spacing.screenHorizontal),
    )
}

@Composable
private fun ActiveTripCard(trip: Trip, bus: LiveBus?, now: Long, onTrack: () -> Unit, onSos: () -> Unit) {
    Card(
        onClick = onTrack,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.screenHorizontal),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    ) {
        Column(Modifier.padding(Spacing.lg)) {
            Text(stringResource(R.string.trip_active_title), style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(Spacing.sm))
            Row(verticalAlignment = Alignment.CenterVertically) {
                RouteBadge(trip.routeNumber, bus?.route?.colorArgb ?: 0xFF1F5BD3)
                Spacer(Modifier.width(Spacing.md))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = bus?.let { towards(it.route, it.direction) } ?: trip.routeName,
                        style = MaterialTheme.typography.titleSmall,
                    )
                    if (bus != null) {
                        Text(busStatusLine(bus), style = MaterialTheme.typography.bodySmall)
                    }
                }
                bus?.etaToNextStopSeconds()?.let { eta ->
                    Text(formatEta(eta), style = MaterialTheme.typography.titleMedium)
                }
            }
            if (bus != null) {
                Spacer(Modifier.height(Spacing.xs))
                FreshnessIndicator(bus.lastUpdatedMillis, now)
            }
            Spacer(Modifier.height(Spacing.md))
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Button(onClick = onTrack) { Text(stringResource(R.string.trip_track)) }
                OutlinedButton(onClick = onSos) {
                    Icon(Icons.Outlined.Shield, contentDescription = null, tint = NextBusTheme.extendedColors.sos)
                    Spacer(Modifier.width(Spacing.xs))
                    Text(stringResource(R.string.sos_short))
                }
            }
        }
    }
}

@Composable
private fun NearestStopCard(
    stop: Stop,
    distanceMeters: Double,
    arrivals: List<com.ncs.nextbus.core.model.StopArrival>,
    onClick: () -> Unit,
    onBusClick: (String) -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.screenHorizontal),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(vertical = Spacing.md)) {
            Row(
                modifier = Modifier.padding(horizontal = Spacing.lg),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.DirectionsBus, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(Spacing.md))
                Text(stop.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text(formatDistance(distanceMeters), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(Spacing.sm))
            if (arrivals.isEmpty()) {
                Text(
                    text = stringResource(R.string.stop_no_arrivals),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = Spacing.lg),
                )
            }
            arrivals.forEach { arrival ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(role = Role.Button) { onBusClick(arrival.liveBus.bus.id) }
                        .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RouteBadge(arrival.liveBus.route.number, arrival.liveBus.route.colorArgb)
                    Spacer(Modifier.width(Spacing.md))
                    Text(
                        text = towards(arrival.liveBus.route, arrival.direction),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = formatEta(arrival.etaSeconds),
                        style = MaterialTheme.typography.titleSmall,
                        color = if (arrival.etaSeconds < 60) NextBusTheme.extendedColors.success else MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

