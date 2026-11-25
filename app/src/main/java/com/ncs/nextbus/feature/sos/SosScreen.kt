package com.ncs.nextbus.feature.sos

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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.ContactPhone
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ncs.nextbus.R
import com.ncs.nextbus.core.designsystem.component.LoadingState
import com.ncs.nextbus.core.designsystem.component.RouteBadge
import com.ncs.nextbus.core.designsystem.component.SectionHeader
import com.ncs.nextbus.core.designsystem.theme.NextBusTheme
import com.ncs.nextbus.core.designsystem.theme.Spacing
import com.ncs.nextbus.core.ui.composeSms
import com.ncs.nextbus.core.ui.dial
import com.ncs.nextbus.core.ui.formatClockTime
import com.ncs.nextbus.core.ui.mapsUrl
import com.ncs.nextbus.core.ui.shareText
import com.ncs.nextbus.core.ui.towards

const val EMERGENCY_NUMBER = "112"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SosScreen(
    onBack: () -> Unit,
    onManageContacts: () -> Unit,
    viewModel: SosViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.sos_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
            )
        },
    ) { padding ->
        when (val current = state) {
            SosUiState.Loading -> LoadingState(Modifier.padding(padding), rows = 3)
            is SosUiState.Ready -> SosBody(current.content, padding, onManageContacts)
        }
    }
}

@Composable
private fun SosBody(content: SosContent, padding: PaddingValues, onManageContacts: () -> Unit) {
    val context = LocalContext.current
    val sos = NextBusTheme.extendedColors.sos
    val onSos = NextBusTheme.extendedColors.onSos
    val message = sosMessage(content)
    val shareTitle = stringResource(R.string.sos_share_chooser)

    LazyColumn(
        contentPadding = PaddingValues(
            top = padding.calculateTopPadding() + Spacing.sm,
            bottom = padding.calculateBottomPadding() + Spacing.xl,
        ),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        item {
            Button(
                onClick = { context.dial(EMERGENCY_NUMBER) },
                colors = ButtonDefaults.buttonColors(containerColor = sos, contentColor = onSos),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.screenHorizontal)
                    .heightIn(min = 72.dp),
            ) {
                Icon(Icons.Filled.Call, contentDescription = null, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(Spacing.md))
                Column {
                    Text(stringResource(R.string.sos_call_emergency, EMERGENCY_NUMBER), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.sos_call_emergency_body), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.screenHorizontal),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            ) {
                Column(Modifier.padding(Spacing.lg)) {
                    Text(stringResource(R.string.sos_share_title), style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(Spacing.xs))
                    val trip = content.trip
                    val bus = content.tripBus
                    if (trip != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RouteBadge(trip.routeNumber, bus?.route?.colorArgb ?: 0xFF1F5BD3)
                            Spacer(Modifier.width(Spacing.sm))
                            Text(
                                bus?.let { towards(it.route, it.direction) } ?: trip.routeName,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        Spacer(Modifier.height(Spacing.xs))
                    }
                    Text(
                        text = stringResource(
                            when {
                                trip != null -> R.string.sos_share_includes_trip
                                content.location != null -> R.string.sos_share_includes_location
                                else -> R.string.sos_share_includes_nothing
                            },
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(Spacing.md))
                    FilledTonalButton(onClick = { context.shareText(message, shareTitle) }) {
                        Icon(Icons.Outlined.Share, contentDescription = null)
                        Spacer(Modifier.width(Spacing.sm))
                        Text(stringResource(R.string.sos_share_action))
                    }
                }
            }
        }
        item { SectionHeader(stringResource(R.string.sos_contacts)) }
        if (content.contacts.isEmpty()) {
            item {
                Column(Modifier.padding(horizontal = Spacing.screenHorizontal)) {
                    Text(
                        stringResource(R.string.sos_no_contacts),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(Spacing.sm))
                    OutlinedButton(onClick = onManageContacts) {
                        Icon(Icons.Outlined.ContactPhone, contentDescription = null)
                        Spacer(Modifier.width(Spacing.sm))
                        Text(stringResource(R.string.sos_add_contacts))
                    }
                }
            }
        }
        items(content.contacts, key = { it.phone }) { contact ->
            ListItem(
                headlineContent = { Text(contact.name) },
                supportingContent = { Text(contact.phone) },
                trailingContent = {
                    Row {
                        IconButton(onClick = { context.composeSms(contact.phone, message) }) {
                            Icon(Icons.Outlined.Sms, contentDescription = stringResource(R.string.sos_text_contact, contact.name))
                        }
                        IconButton(onClick = { context.dial(contact.phone) }) {
                            Icon(Icons.Outlined.Call, contentDescription = stringResource(R.string.sos_call_contact, contact.name))
                        }
                    }
                },
            )
        }
    }
}

/**
 * The text shared with contacts. The driver's contact number appears only here, never on screen.
 */
@Composable
private fun sosMessage(content: SosContent): String {
    val context = LocalContext.current
    val name = content.displayName.ifBlank { stringResource(R.string.profile_default_name) }
    val lines = mutableListOf(stringResource(R.string.sos_message_header, name))
    val trip = content.trip
    val bus = content.tripBus
    if (trip != null) {
        lines += stringResource(
            R.string.sos_message_trip,
            trip.routeNumber,
            trip.routeName,
            trip.busRegistration,
            bus?.headsign?.name ?: "-",
            bus?.nextStop?.stop?.name ?: trip.boardStopName,
        )
        if (bus != null) {
            lines += stringResource(R.string.sos_message_driver, bus.bus.driverName, bus.bus.driverPhone)
        }
    }
    val location = content.location
    lines += if (location != null) {
        stringResource(
            R.string.sos_message_location,
            location.mapsUrl(),
            formatClockTime(context, content.locationTimeMillis ?: System.currentTimeMillis()),
        )
    } else {
        stringResource(R.string.sos_message_no_location)
    }
    lines += stringResource(R.string.sos_message_footer)
    return lines.joinToString("\n")
}
