package com.ncs.nextbus.feature.map

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.maps.android.compose.rememberCameraPositionState
import com.ncs.nextbus.R
import com.ncs.nextbus.core.designsystem.component.BusListItem
import com.ncs.nextbus.core.designsystem.component.ErrorState
import com.ncs.nextbus.core.designsystem.component.InlineBanner
import com.ncs.nextbus.core.designsystem.component.LoadingState
import com.ncs.nextbus.core.designsystem.component.RouteBadge
import com.ncs.nextbus.core.designsystem.theme.Spacing
import com.ncs.nextbus.core.model.Route
import com.ncs.nextbus.core.ui.etaToNextStopSeconds
import com.ncs.nextbus.core.ui.formatEta
import com.ncs.nextbus.core.ui.rememberNow
import com.ncs.nextbus.core.ui.toLatLng
import com.ncs.nextbus.feature.common.CityCenterCamera
import com.ncs.nextbus.feature.common.MapUnavailable
import com.ncs.nextbus.feature.common.TransitMap
import com.ncs.nextbus.feature.common.boundsOf
import com.ncs.nextbus.feature.common.mapsAvailable
import kotlinx.coroutines.launch

@Composable
fun MapScreen(
    onBusClick: (busId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MapViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val now by rememberNow()
    when (val current = state) {
        MapUiState.Loading -> LoadingState(modifier.windowInsetsPadding(WindowInsets.statusBars))
        MapUiState.Error -> ErrorState(modifier, onRetry = viewModel::retry)
        is MapUiState.Success -> {
            val content = current.content
            if (mapsAvailable) {
                LiveMap(content, now, onBusClick, viewModel::setFilter, viewModel::selectBus, modifier)
            } else {
                // Without a Maps key, keep the screen useful: the same live buses as a list.
                Column(modifier.windowInsetsPadding(WindowInsets.statusBars)) {
                    RouteFilterRow(content.routes, content.filterRouteId, viewModel::setFilter)
                    MapUnavailable(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.screenHorizontal),
                        fillHeight = false,
                    )
                    LazyColumn(Modifier.fillMaxSize()) {
                        items(content.buses, key = { it.bus.id }) { bus ->
                            BusListItem(
                                liveBus = bus,
                                nowMillis = now,
                                trailingPrimary = bus.etaToNextStopSeconds()?.let { formatEta(it) } ?: "",
                                trailingSecondary = stringResource(R.string.bus_to_next_stop),
                                onClick = { onBusClick(bus.bus.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveMap(
    content: MapContent,
    now: Long,
    onBusClick: (String) -> Unit,
    onFilter: (String?) -> Unit,
    onSelectBus: (String?) -> Unit,
    modifier: Modifier,
) {
    val cameraState = rememberCameraPositionState { position = CityCenterCamera }
    val scope = rememberCoroutineScope()
    var mapLoaded by remember { mutableStateOf(false) }

    Box(modifier.fillMaxSize()) {
        TransitMap(
            routes = content.visibleRoutes,
            buses = content.buses,
            nowMillis = now,
            cameraPositionState = cameraState,
            onBusClick = { onSelectBus(it.bus.id) },
            selectedBusId = content.selectedBus?.bus?.id,
            showStops = content.filterRouteId != null,
            myLocationEnabled = content.reference.isDevice,
            contentPadding = PaddingValues(top = 96.dp, bottom = if (content.selectedBus != null) 180.dp else 0.dp),
            onMapClick = { onSelectBus(null) },
            onMapLoaded = { mapLoaded = true },
            modifier = Modifier.fillMaxSize(),
        )

        Column(
            Modifier
                .windowInsetsPadding(WindowInsets.statusBars)
                .fillMaxWidth(),
        ) {
            RouteFilterRow(content.routes, content.filterRouteId) { routeId ->
                onFilter(routeId)
                val route = content.routes.firstOrNull { it.id == routeId }
                val bounds = boundsOf(route?.path ?: content.routes.flatMap { it.path })
                if (mapLoaded && bounds != null) {
                    scope.launch { runCatching { cameraState.animate(CameraUpdateFactory.newLatLngBounds(bounds, 120)) } }
                }
            }
            if (!content.isOnline) {
                InlineBanner(
                    icon = Icons.Outlined.CloudOff,
                    message = stringResource(R.string.offline_map_banner),
                    modifier = Modifier.padding(horizontal = Spacing.screenHorizontal),
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(Spacing.lg),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            if (content.reference.isDevice) {
                SmallFloatingActionButton(onClick = {
                    scope.launch {
                        runCatching { cameraState.animate(CameraUpdateFactory.newLatLngZoom(content.reference.point.toLatLng(), 15f)) }
                    }
                }) {
                    Icon(Icons.Outlined.MyLocation, contentDescription = stringResource(R.string.map_my_location))
                }
            }
            AnimatedVisibility(
                visible = content.selectedBus != null,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
            ) {
                content.selectedBus?.let { bus ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    ) {
                        BusListItem(
                            liveBus = bus,
                            nowMillis = now,
                            trailingPrimary = bus.etaToNextStopSeconds()?.let { formatEta(it) } ?: "",
                            trailingSecondary = stringResource(R.string.bus_to_next_stop),
                            onClick = { onBusClick(bus.bus.id) },
                        )
                        TextButton(
                            onClick = { onBusClick(bus.bus.id) },
                            modifier = Modifier
                                .align(Alignment.End)
                                .padding(end = Spacing.sm, bottom = Spacing.xs),
                        ) { Text(stringResource(R.string.map_track_bus)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun RouteFilterRow(routes: List<Route>, selectedRouteId: String?, onSelect: (String?) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = Spacing.screenHorizontal, vertical = Spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        item(key = "all") {
            FilterChip(
                selected = selectedRouteId == null,
                onClick = { onSelect(null) },
                label = { Text(stringResource(R.string.map_all_routes)) },
                colors = FilterChipDefaults.filterChipColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        }
        items(routes, key = { it.id }) { route ->
            FilterChip(
                selected = selectedRouteId == route.id,
                onClick = { onSelect(if (selectedRouteId == route.id) null else route.id) },
                label = { Text(route.name) },
                leadingIcon = { RouteBadge(route.number, route.colorArgb) },
                colors = FilterChipDefaults.filterChipColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        }
    }
}
