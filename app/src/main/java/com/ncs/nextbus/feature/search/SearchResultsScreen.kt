package com.ncs.nextbus.feature.search

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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.DirectionsBus
import androidx.compose.material.icons.outlined.WrongLocation
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ncs.nextbus.R
import com.ncs.nextbus.core.designsystem.component.EmptyState
import com.ncs.nextbus.core.designsystem.component.ErrorState
import com.ncs.nextbus.core.designsystem.component.FuelBadge
import com.ncs.nextbus.core.designsystem.component.LoadingState
import com.ncs.nextbus.core.designsystem.component.RouteBadge
import com.ncs.nextbus.core.designsystem.theme.Spacing
import com.ncs.nextbus.core.model.JourneyOption
import com.ncs.nextbus.core.ui.fare
import com.ncs.nextbus.core.ui.formatDistance
import com.ncs.nextbus.core.ui.formatDurationMinutes
import com.ncs.nextbus.core.ui.formatEta
import com.ncs.nextbus.core.ui.towards
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchResultsScreen(
    onBack: () -> Unit,
    onBusClick: (busId: String) -> Unit,
    onRouteClick: (routeId: String) -> Unit,
    viewModel: SearchResultsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val success = state as? SearchResultsUiState.Success

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (success != null) {
                        Column {
                            Text(success.from.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                stringResource(R.string.journey_to_stop, success.to.name),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    } else {
                        Text(stringResource(R.string.search_results_title))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    if (success != null) {
                        IconButton(onClick = viewModel::toggleSaved) {
                            Icon(
                                if (success.isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                contentDescription = stringResource(
                                    if (success.isSaved) R.string.search_unsave_journey else R.string.search_save_journey,
                                ),
                            )
                        }
                    }
                },
            )
        },
    ) { padding ->
        val modifier = Modifier.padding(padding)
        when (val current = state) {
            SearchResultsUiState.Loading -> LoadingState(modifier, rows = 3, rowHeight = androidx.compose.ui.unit.Dp(140f))
            SearchResultsUiState.Error -> ErrorState(modifier, onRetry = viewModel::retry)
            SearchResultsUiState.UnknownStops -> EmptyState(
                icon = Icons.Outlined.WrongLocation,
                title = stringResource(R.string.search_unknown_stops_title),
                message = stringResource(R.string.search_unknown_stops_body),
                actionLabel = stringResource(R.string.search_change_stops),
                onAction = onBack,
                modifier = modifier,
            )
            is SearchResultsUiState.Success -> if (current.options.isEmpty()) {
                EmptyState(
                    icon = Icons.Outlined.DirectionsBus,
                    title = stringResource(R.string.search_no_direct_title),
                    message = stringResource(R.string.search_no_direct_body, current.from.name, current.to.name),
                    actionLabel = stringResource(R.string.search_change_stops),
                    onAction = onBack,
                    modifier = modifier,
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(
                        top = padding.calculateTopPadding() + Spacing.sm,
                        bottom = padding.calculateBottomPadding() + Spacing.xl,
                        start = Spacing.screenHorizontal,
                        end = Spacing.screenHorizontal,
                    ),
                    verticalArrangement = Arrangement.spacedBy(Spacing.md),
                ) {
                    item {
                        Text(
                            text = pluralStringResource(R.plurals.search_direct_routes, current.options.size, current.options.size),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    items(current.options, key = { "${it.route.id}-${it.direction}" }) { option ->
                        JourneyOptionCard(option, onBusClick = onBusClick, onRouteClick = { onRouteClick(option.route.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun JourneyOptionCard(option: JourneyOption, onBusClick: (String) -> Unit, onRouteClick: () -> Unit) {
    val next = option.upcoming.firstOrNull()
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.padding(Spacing.lg)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RouteBadge(option.route.number, option.route.colorArgb, large = true)
                Spacer(Modifier.width(Spacing.md))
                Column(Modifier.weight(1f)) {
                    Text(option.route.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        towards(option.route, option.direction),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                FuelBadge(option.route.fuelType)
            }
            Spacer(Modifier.height(Spacing.md))
            Row(verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.search_next_bus_at, option.boardStop.stop.name),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = next?.let { formatEta(it.etaSeconds) } ?: stringResource(R.string.search_no_live_bus),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                if (option.upcoming.size > 1) {
                    val later = option.upcoming.drop(1).map { formatEta(it.etaSeconds) }
                    Text(
                        text = stringResource(R.string.search_then, later.joinToString(", ")),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(Spacing.md))
            HorizontalDivider()
            Spacer(Modifier.height(Spacing.md))
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.lg)) {
                Fact(stringResource(R.string.search_fact_ride), formatDurationMinutes(option.rideMinutes))
                Fact(
                    stringResource(R.string.search_fact_stops),
                    abs(option.alightStop.index - option.boardStop.index).toString(),
                )
                Fact(stringResource(R.string.search_fact_distance), formatDistance(option.distanceMeters))
                Fact(stringResource(R.string.search_fact_fare), fare(option.fareRupees))
            }
            Spacer(Modifier.height(Spacing.md))
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), modifier = Modifier.fillMaxWidth()) {
                if (next != null) {
                    Button(onClick = { onBusClick(next.liveBus.bus.id) }) { Text(stringResource(R.string.search_track_bus)) }
                }
                TextButton(onClick = onRouteClick) { Text(stringResource(R.string.search_route_details)) }
            }
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
