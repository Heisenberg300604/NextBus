package com.ncs.nextbus.feature.bus

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.GpsNotFixed
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.maps.android.compose.CameraMoveStartedReason
import com.google.maps.android.compose.rememberCameraPositionState
import com.ncs.nextbus.R
import com.ncs.nextbus.core.designsystem.component.DelayChip
import com.ncs.nextbus.core.designsystem.component.EmptyState
import com.ncs.nextbus.core.designsystem.component.ErrorState
import com.ncs.nextbus.core.designsystem.component.FreshnessIndicator
import com.ncs.nextbus.core.designsystem.component.FuelBadge
import com.ncs.nextbus.core.designsystem.component.InlineBanner
import com.ncs.nextbus.core.designsystem.component.LoadingState
import com.ncs.nextbus.core.designsystem.component.RouteBadge
import com.ncs.nextbus.core.designsystem.component.SectionHeader
import com.ncs.nextbus.core.designsystem.component.TimelineRow
import com.ncs.nextbus.core.designsystem.component.TimelineState
import com.ncs.nextbus.core.designsystem.theme.NextBusTheme
import com.ncs.nextbus.core.designsystem.theme.Spacing
import com.ncs.nextbus.core.model.BusActivity
import com.ncs.nextbus.core.model.Direction
import com.ncs.nextbus.core.model.Freshness
import com.ncs.nextbus.core.model.LiveBus
import com.ncs.nextbus.core.ui.busStatusLine
import com.ncs.nextbus.core.ui.etaToNextStopSeconds
import com.ncs.nextbus.core.ui.formatClockTime
import com.ncs.nextbus.core.ui.formatEta
import com.ncs.nextbus.core.ui.formatRelativeAge
import com.ncs.nextbus.core.ui.rememberNow
import com.ncs.nextbus.core.ui.toLatLng
import com.ncs.nextbus.core.ui.towards
import com.ncs.nextbus.feature.common.TransitMap
import com.ncs.nextbus.feature.common.mapsAvailable
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusDetailScreen(
    onBack: () -> Unit,
    onRouteClick: (routeId: String) -> Unit,
    onStopClick: (stopId: String) -> Unit,
    onOpenSos: () -> Unit,
    viewModel: BusDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current

    LaunchedEffect(viewModel) {
        viewModel.eventFlow.collect { event ->
            val message = when (event) {
                BusDetailEvent.TripStarted -> resources.getString(R.string.trip_started)
                BusDetailEvent.TripEnded -> resources.getString(R.string.trip_ended)
                is BusDetailEvent.AlreadyOnTrip -> resources.getString(R.string.trip_already_active, event.routeNumber)
                is BusDetailEvent.FavoriteChanged -> resources.getString(
                    if (event.isFavorite) R.string.route_favorite_added else R.string.route_favorite_removed,
                )
                BusDetailEvent.ActionFailed -> resources.getString(R.string.error_generic)
            }
            snackbarHostState.showSnackbar(message)
        }
    }

    val content = (state as? BusDetailUiState.Success)?.content
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        content?.let { stringResource(R.string.bus_detail_title, it.liveBus.route.number, it.liveBus.route.name) }
                            ?: stringResource(R.string.bus_detail_title_generic),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    if (content != null) {
                        IconButton(onClick = viewModel::toggleFavoriteRoute) {
                            Icon(
                                if (content.isFavoriteRoute) Icons.Filled.Star else Icons.Outlined.StarOutline,
                                contentDescription = stringResource(
                                    if (content.isFavoriteRoute) R.string.route_unfavorite else R.string.route_favorite,
                                ),
                                tint = if (content.isFavoriteRoute) NextBusTheme.extendedColors.warning else Color.Unspecified,
                            )
                        }
                    }
                },
            )
        },
    ) { padding ->
        val modifier = Modifier.padding(padding)
        when (val current = state) {
            BusDetailUiState.Loading -> LoadingState(modifier)
            BusDetailUiState.Error -> ErrorState(modifier, onRetry = viewModel::retry)
            BusDetailUiState.NotFound -> EmptyState(
                icon = Icons.Outlined.SearchOff,
                title = stringResource(R.string.bus_not_found_title),
                message = stringResource(R.string.bus_not_found_body),
                actionLabel = stringResource(R.string.action_back),
                onAction = onBack,
                modifier = modifier,
            )
            is BusDetailUiState.Success -> BusDetailBody(
                content = current.content,
                padding = padding,
                onStartTrip = viewModel::startTrip,
                onEndTrip = viewModel::endTrip,
                onRouteClick = { onRouteClick(current.content.liveBus.route.id) },
                onStopClick = onStopClick,
                onOpenSos = onOpenSos,
            )
        }
    }
}

@Composable
private fun BusDetailBody(
    content: BusDetailContent,
    padding: PaddingValues,
    onStartTrip: () -> Unit,
    onEndTrip: () -> Unit,
    onRouteClick: () -> Unit,
    onStopClick: (String) -> Unit,
    onOpenSos: () -> Unit,
) {
    val now by rememberNow()
    var showPassedStops by rememberSaveable { mutableStateOf(false) }
    val onShowPassedStops = { showPassedStops = true }
    val bus = content.liveBus
    val freshness = Freshness.of(bus.lastUpdatedMillis, now)
    val context = LocalContext.current

    LazyColumn(
        contentPadding = PaddingValues(top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding() + Spacing.xl),
    ) {
        item { BusMap(bus, now) }
        if (!content.isOnline && mapsAvailable) {
            item {
                InlineBanner(
                    icon = Icons.Outlined.CloudOff,
                    message = stringResource(R.string.offline_map_banner),
                    modifier = Modifier.padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.sm),
                )
            }
        }
        item { BusHeader(bus, now) }
        if (freshness != Freshness.LIVE) {
            item {
                InlineBanner(
                    icon = Icons.Outlined.GpsNotFixed,
                    message = stringResource(R.string.bus_stale_banner, formatRelativeAge(now - bus.lastUpdatedMillis)),
                    containerColor = NextBusTheme.extendedColors.warningContainer,
                    contentColor = NextBusTheme.extendedColors.onWarningContainer,
                    modifier = Modifier.padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.sm),
                )
            }
        }
        item {
            TripActions(
                content = content,
                onStartTrip = onStartTrip,
                onEndTrip = onEndTrip,
                onRouteClick = onRouteClick,
                onOpenSos = onOpenSos,
            )
        }
        item { SectionHeader(stringResource(R.string.bus_stops_on_this_run)) }
        val order = if (bus.direction == Direction.OUTBOUND) bus.route.stops else bus.route.stops.reversed()
        val etas = bus.predictions.filter { it.direction == bus.direction }.associateBy { it.stopIndex }
        val nextPosition = order.indexOfFirst { it.index == bus.nextStopIndex }.coerceAtLeast(0)
        if (nextPosition > 0 && !showPassedStops) {
            item(key = "passed") {
                TextButton(
                    onClick = onShowPassedStops,
                    modifier = Modifier.padding(horizontal = Spacing.sm),
                ) { Text(pluralStringResource(R.plurals.bus_show_passed_stops, nextPosition, nextPosition)) }
            }
        }
        val visible = if (showPassedStops) order.withIndex().toList() else order.withIndex().drop(nextPosition)
        items(visible, key = { it.value.stop.id }) { (position, routeStop) ->
            val passed = position < nextPosition
            val eta = etas[routeStop.index]?.etaSeconds
            TimelineRow(
                title = routeStop.stop.name,
                state = when {
                    passed -> TimelineState.PASSED
                    position == nextPosition -> TimelineState.CURRENT
                    else -> TimelineState.UPCOMING
                },
                lineColor = Color(bus.route.colorArgb),
                isFirst = position == 0,
                isLast = position == order.lastIndex,
                subtitle = if (!passed && eta != null) formatClockTime(context, now + eta * 1000L) else null,
                trailing = if (!passed && eta != null) formatEta(eta) else null,
                onClick = { onStopClick(routeStop.stop.id) },
                modifier = Modifier.animateItem(),
            )
        }
        item {
            SectionHeader(stringResource(R.string.bus_about))
            BusFacts(bus)
        }
    }
}

@Composable
private fun BusMap(bus: LiveBus, now: Long) {
    val cameraState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(bus.position.toLatLng(), 15f)
    }
    var following by rememberSaveable { mutableStateOf(true) }

    // A drag or pinch stops auto-follow until the user taps "recenter".
    LaunchedEffect(cameraState.isMoving) {
        if (cameraState.isMoving && cameraState.cameraMoveStartedReason == CameraMoveStartedReason.GESTURE) following = false
    }
    LaunchedEffect(bus.position, following) {
        if (following && mapsAvailable) {
            runCatching { cameraState.animate(CameraUpdateFactory.newLatLng(bus.position.toLatLng()), 900) }
        }
    }

    Box(
        Modifier
            .fillMaxWidth()
            .height(if (mapsAvailable) 280.dp else 140.dp),
    ) {
        TransitMap(
            routes = listOf(bus.route),
            buses = listOf(bus),
            nowMillis = now,
            cameraPositionState = cameraState,
            onBusClick = { following = true },
            selectedBusId = bus.bus.id,
            modifier = Modifier.fillMaxWidth().height(if (mapsAvailable) 280.dp else 140.dp),
        )
        if (mapsAvailable && !following) {
            SmallFloatingActionButton(
                onClick = { following = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(Spacing.md),
            ) {
                Icon(Icons.Outlined.MyLocation, contentDescription = stringResource(R.string.map_recenter_bus))
            }
        }
    }
}

@Composable
private fun BusHeader(bus: LiveBus, now: Long) {
    Column(Modifier.padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.lg)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RouteBadge(bus.route.number, bus.route.colorArgb, large = true)
            Spacer(Modifier.width(Spacing.md))
            Column(Modifier.weight(1f)) {
                Text(towards(bus.route, bus.direction), style = MaterialTheme.typography.titleLarge)
                FreshnessIndicator(bus.lastUpdatedMillis, now)
            }
        }
        Spacer(Modifier.height(Spacing.md))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            DelayChip(bus.delayMinutes, bus.isDelayed)
            FuelBadge(bus.route.fuelType)
        }
        Spacer(Modifier.height(Spacing.lg))
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.lg),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(busStatusLine(bus), style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = when (bus.activity) {
                            BusActivity.MOVING -> stringResource(R.string.bus_speed, bus.speedKmh.roundToInt())
                            BusActivity.AT_STOP -> stringResource(R.string.bus_boarding)
                            BusActivity.LAYOVER -> stringResource(R.string.bus_departs_soon)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                bus.etaToNextStopSeconds()?.let {
                    Text(formatEta(it), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun TripActions(
    content: BusDetailContent,
    onStartTrip: () -> Unit,
    onEndTrip: () -> Unit,
    onRouteClick: () -> Unit,
    onOpenSos: () -> Unit,
) {
    Column(Modifier.padding(horizontal = Spacing.screenHorizontal)) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalAlignment = Alignment.CenterVertically) {
            if (content.isOnThisBus) {
                Button(onClick = onEndTrip) { Text(stringResource(R.string.trip_end)) }
                OutlinedButton(onClick = onOpenSos) {
                    Icon(Icons.Outlined.Shield, contentDescription = null, tint = NextBusTheme.extendedColors.sos)
                    Spacer(Modifier.width(Spacing.xs))
                    Text(stringResource(R.string.sos_short))
                }
            } else {
                Button(onClick = onStartTrip, enabled = content.activeTrip == null) {
                    Text(stringResource(R.string.trip_start))
                }
            }
            TextButton(onClick = onRouteClick) { Text(stringResource(R.string.search_route_details)) }
        }
        if (!content.isOnThisBus && content.activeTrip != null) {
            Text(
                text = stringResource(R.string.trip_already_active, content.activeTrip.routeNumber),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun BusFacts(bus: LiveBus) {
    Column(
        modifier = Modifier.padding(horizontal = Spacing.screenHorizontal),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Fact(stringResource(R.string.bus_registration), bus.bus.registration)
        Fact(stringResource(R.string.bus_capacity), pluralStringResource(R.plurals.bus_seats, bus.bus.capacity, bus.bus.capacity))
        Fact(stringResource(R.string.bus_driver), bus.bus.driverName)
        Fact(stringResource(R.string.route_frequency_label), stringResource(R.string.route_every_minutes, bus.route.headwayMinutes))
    }
}

@Composable
private fun Fact(label: String, value: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
