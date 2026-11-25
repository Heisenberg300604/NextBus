package com.ncs.nextbus.feature.route

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ncs.nextbus.R
import com.ncs.nextbus.core.designsystem.component.BusListItem
import com.ncs.nextbus.core.designsystem.component.EmptyState
import com.ncs.nextbus.core.designsystem.component.ErrorState
import com.ncs.nextbus.core.designsystem.component.FuelBadge
import com.ncs.nextbus.core.designsystem.component.LoadingState
import com.ncs.nextbus.core.designsystem.component.RouteBadge
import com.ncs.nextbus.core.designsystem.component.SectionHeader
import com.ncs.nextbus.core.designsystem.component.TimelineRow
import com.ncs.nextbus.core.designsystem.component.TimelineState
import com.ncs.nextbus.core.designsystem.theme.NextBusTheme
import com.ncs.nextbus.core.designsystem.theme.Spacing
import com.ncs.nextbus.core.model.Direction
import com.ncs.nextbus.core.ui.etaToNextStopSeconds
import com.ncs.nextbus.core.ui.fare
import com.ncs.nextbus.core.ui.formatClockTime
import com.ncs.nextbus.core.ui.formatDistance
import com.ncs.nextbus.core.ui.formatDurationMinutes
import com.ncs.nextbus.core.ui.formatEta
import com.ncs.nextbus.core.ui.rememberNow
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteDetailScreen(
    onBack: () -> Unit,
    onBusClick: (busId: String) -> Unit,
    onStopClick: (stopId: String) -> Unit,
    viewModel: RouteDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val content = (state as? RouteDetailUiState.Success)?.content
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        content?.let { stringResource(R.string.cd_route_number, it.route.number) }
                            ?: stringResource(R.string.route_title_generic),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    if (content != null) {
                        IconButton(onClick = viewModel::toggleFavorite) {
                            Icon(
                                if (content.isFavorite) Icons.Filled.Star else Icons.Outlined.StarOutline,
                                contentDescription = stringResource(
                                    if (content.isFavorite) R.string.route_unfavorite else R.string.route_favorite,
                                ),
                                tint = if (content.isFavorite) NextBusTheme.extendedColors.warning else Color.Unspecified,
                            )
                        }
                    }
                },
            )
        },
    ) { padding ->
        val modifier = Modifier.padding(padding)
        when (val current = state) {
            RouteDetailUiState.Loading -> LoadingState(modifier)
            RouteDetailUiState.Error -> ErrorState(modifier, onRetry = viewModel::retry)
            RouteDetailUiState.NotFound -> EmptyState(
                icon = Icons.Outlined.SearchOff,
                title = stringResource(R.string.route_not_found_title),
                message = stringResource(R.string.route_not_found_body),
                actionLabel = stringResource(R.string.action_back),
                onAction = onBack,
                modifier = modifier,
            )
            is RouteDetailUiState.Success -> RouteDetailBody(
                content = current.content,
                padding = padding,
                onDirection = viewModel::selectDirection,
                onBusClick = onBusClick,
                onStopClick = onStopClick,
            )
        }
    }
}

@Composable
private fun RouteDetailBody(
    content: RouteDetailContent,
    padding: PaddingValues,
    onDirection: (Direction) -> Unit,
    onBusClick: (String) -> Unit,
    onStopClick: (String) -> Unit,
) {
    val now by rememberNow()
    val context = LocalContext.current
    val route = content.route
    LazyColumn(
        contentPadding = PaddingValues(top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding() + Spacing.xl),
    ) {
        item {
            Column(Modifier.padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.md)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RouteBadge(route.number, route.colorArgb, large = true)
                    Spacer(Modifier.width(Spacing.md))
                    Text(route.name, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    FuelBadge(route.fuelType)
                }
                Spacer(Modifier.height(Spacing.lg))
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xl)) {
                    Fact(stringResource(R.string.route_frequency_label), stringResource(R.string.route_every_minutes, route.headwayMinutes))
                    Fact(stringResource(R.string.route_run_time), formatDurationMinutes(route.runMinutes.roundToInt()))
                    Fact(stringResource(R.string.route_length), formatDistance(route.lengthMeters))
                    Fact(
                        stringResource(R.string.search_fact_fare),
                        stringResource(R.string.route_fare_range, fare(content.minFare), fare(content.maxFare)),
                    )
                }
            }
        }
        item {
            SingleChoiceSegmentedButtonRow(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.sm),
            ) {
                Direction.entries.forEachIndexed { index, direction ->
                    SegmentedButton(
                        selected = content.direction == direction,
                        onClick = { onDirection(direction) },
                        shape = SegmentedButtonDefaults.itemShape(index, Direction.entries.size),
                    ) {
                        Text(
                            stringResource(R.string.route_to_stop, route.headsign(direction).name),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
        if (content.departures.isNotEmpty()) {
            item { SectionHeader(stringResource(R.string.route_next_departures, content.stops.first().name)) }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = Spacing.screenHorizontal),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    items(content.departures) { time ->
                        SuggestionChip(onClick = {}, enabled = false, label = { Text(formatClockTime(context, time)) })
                    }
                }
            }
        }
        item { SectionHeader(stringResource(R.string.route_stops)) }
        itemsIndexed(content.stops, key = { _, row -> row.stopId }) { index, row ->
            TimelineRow(
                title = row.name,
                state = TimelineState.UPCOMING,
                lineColor = Color(route.colorArgb),
                isFirst = index == 0,
                isLast = index == content.stops.lastIndex,
                subtitle = stringResource(R.string.route_scheduled_offset, row.scheduledOffsetMinutes),
                trailing = row.nextEtaSeconds?.let { formatEta(it) },
                onClick = { onStopClick(row.stopId) },
            )
        }
        item { SectionHeader(stringResource(R.string.route_live_buses)) }
        items(content.liveBuses, key = { it.bus.id }) { bus ->
            BusListItem(
                liveBus = bus,
                nowMillis = now,
                trailingPrimary = bus.etaToNextStopSeconds()?.let { formatEta(it) } ?: "",
                trailingSecondary = stringResource(R.string.bus_to_next_stop),
                onClick = { onBusClick(bus.bus.id) },
                modifier = Modifier.animateItem(),
            )
        }
    }
}

@Composable
private fun Fact(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleSmall)
    }
}
