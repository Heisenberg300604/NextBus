package com.ncs.nextbus.feature.stop

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ncs.nextbus.R
import com.ncs.nextbus.core.designsystem.component.EmptyState
import com.ncs.nextbus.core.designsystem.component.ErrorState
import com.ncs.nextbus.core.designsystem.component.FreshnessIndicator
import com.ncs.nextbus.core.designsystem.component.LoadingState
import com.ncs.nextbus.core.designsystem.component.RouteBadge
import com.ncs.nextbus.core.designsystem.component.SectionHeader
import com.ncs.nextbus.core.designsystem.theme.NextBusTheme
import com.ncs.nextbus.core.designsystem.theme.Spacing
import com.ncs.nextbus.core.ui.formatClockTime
import com.ncs.nextbus.core.ui.formatEta
import com.ncs.nextbus.core.ui.rememberNow
import com.ncs.nextbus.core.ui.towards

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StopDetailScreen(
    onBack: () -> Unit,
    onBusClick: (busId: String) -> Unit,
    onRouteClick: (routeId: String) -> Unit,
    viewModel: StopDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val now by rememberNow()
    val context = LocalContext.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        (state as? StopDetailUiState.Success)?.stop?.name ?: stringResource(R.string.stop_title_generic),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
            )
        },
    ) { padding ->
        val modifier = Modifier.padding(padding)
        when (val current = state) {
            StopDetailUiState.Loading -> LoadingState(modifier)
            StopDetailUiState.Error -> ErrorState(modifier, onRetry = viewModel::retry)
            StopDetailUiState.NotFound -> EmptyState(
                icon = Icons.Outlined.SearchOff,
                title = stringResource(R.string.stop_not_found_title),
                message = stringResource(R.string.stop_not_found_body),
                actionLabel = stringResource(R.string.action_back),
                onAction = onBack,
                modifier = modifier,
            )
            is StopDetailUiState.Success -> LazyColumn(
                contentPadding = PaddingValues(top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding() + Spacing.xl),
            ) {
                item { SectionHeader(stringResource(R.string.stop_routes_here)) }
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = Spacing.screenHorizontal),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        items(current.routes, key = { it.id }) { route ->
                            Row(Modifier.clickable(role = Role.Button) { onRouteClick(route.id) }) {
                                RouteBadge(route.number, route.colorArgb, large = true)
                            }
                        }
                    }
                }
                item { SectionHeader(stringResource(R.string.stop_arrivals)) }
                if (current.arrivals.isEmpty()) {
                    item {
                        EmptyState(
                            icon = Icons.Outlined.EventBusy,
                            title = stringResource(R.string.stop_no_arrivals),
                            message = stringResource(R.string.stop_no_arrivals_body),
                        )
                    }
                }
                items(current.arrivals, key = { "${it.liveBus.bus.id}-${it.direction}" }) { arrival ->
                    ListItem(
                        headlineContent = { Text(towards(arrival.liveBus.route, arrival.direction), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        supportingContent = {
                            FreshnessIndicator(arrival.liveBus.lastUpdatedMillis, now, textStyle = MaterialTheme.typography.labelSmall)
                        },
                        leadingContent = { RouteBadge(arrival.liveBus.route.number, arrival.liveBus.route.colorArgb) },
                        trailingContent = {
                            androidx.compose.foundation.layout.Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                                Text(
                                    formatEta(arrival.etaSeconds),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (arrival.etaSeconds < 60) NextBusTheme.extendedColors.success else MaterialTheme.colorScheme.primary,
                                )
                                Text(formatClockTime(context, now + arrival.etaSeconds * 1000L), style = MaterialTheme.typography.labelSmall)
                            }
                        },
                        modifier = Modifier
                            .animateItem()
                            .clickable(role = Role.Button) { onBusClick(arrival.liveBus.bus.id) },
                    )
                }
            }
        }
    }
}
