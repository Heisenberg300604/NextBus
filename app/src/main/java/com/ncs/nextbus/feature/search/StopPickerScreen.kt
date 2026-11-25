package com.ncs.nextbus.feature.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ncs.nextbus.R
import com.ncs.nextbus.core.designsystem.component.EmptyState
import com.ncs.nextbus.core.designsystem.component.ErrorState
import com.ncs.nextbus.core.designsystem.component.LoadingState
import com.ncs.nextbus.core.designsystem.component.RouteBadge
import com.ncs.nextbus.core.designsystem.theme.Spacing
import com.ncs.nextbus.core.ui.formatDistance
import com.ncs.nextbus.navigation.StopField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StopPickerScreen(
    onBack: () -> Unit,
    onStopSelected: (field: StopField, stopId: String) -> Unit,
    viewModel: StopPickerViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val title = stringResource(if (viewModel.field == StopField.FROM) R.string.journey_choose_from else R.string.journey_choose_to)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            val query = (state as? StopPickerUiState.Success)?.query.orEmpty()
            OutlinedTextField(
                value = query,
                onValueChange = viewModel::onQueryChange,
                placeholder = { Text(stringResource(R.string.stop_search_hint)) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onQueryChange("") }) {
                            Icon(Icons.Filled.Clear, contentDescription = stringResource(R.string.stop_search_clear))
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.sm)
                    .focusRequester(focusRequester),
            )
            LaunchedEffect(Unit) { focusRequester.requestFocus() }

            when (val current = state) {
                StopPickerUiState.Loading -> LoadingState(rows = 6, rowHeight = androidx.compose.ui.unit.Dp(56f))
                StopPickerUiState.Error -> ErrorState()
                is StopPickerUiState.Success -> {
                    if (current.results.isEmpty()) {
                        EmptyState(
                            icon = Icons.Outlined.SearchOff,
                            title = stringResource(R.string.stop_search_empty_title),
                            message = stringResource(R.string.stop_search_empty_body, current.query),
                        )
                    } else {
                        LazyColumn(Modifier.fillMaxSize()) {
                            current.nearest?.let { nearest ->
                                item(key = "nearest") {
                                    StopOptionRow(
                                        option = nearest,
                                        overline = stringResource(R.string.stop_search_nearest),
                                        highlighted = true,
                                        onClick = { onStopSelected(viewModel.field, nearest.stop.id) },
                                    )
                                    HorizontalDivider()
                                }
                            }
                            items(current.results, key = { it.stop.id }) { option ->
                                StopOptionRow(option = option, onClick = { onStopSelected(viewModel.field, option.stop.id) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StopOptionRow(
    option: StopOption,
    onClick: () -> Unit,
    overline: String? = null,
    highlighted: Boolean = false,
) {
    ListItem(
        overlineContent = overline?.let { { Text(it) } },
        headlineContent = { Text(option.stop.name) },
        supportingContent = {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                option.routes.forEach { RouteBadge(it.number, it.colorArgb) }
            }
        },
        leadingContent = {
            Icon(
                if (highlighted) Icons.Outlined.MyLocation else Icons.Outlined.Place,
                contentDescription = null,
                tint = if (highlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        trailingContent = { Text(formatDistance(option.distanceMeters), style = MaterialTheme.typography.labelMedium) },
        colors = ListItemDefaults.colors(),
        modifier = Modifier.clickable(role = Role.Button, onClick = onClick),
    )
}
