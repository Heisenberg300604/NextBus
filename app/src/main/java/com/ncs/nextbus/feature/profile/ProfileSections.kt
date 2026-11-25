package com.ncs.nextbus.feature.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Sos
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import com.ncs.nextbus.R
import com.ncs.nextbus.core.designsystem.theme.NextBusTheme
import com.ncs.nextbus.core.designsystem.theme.Spacing
import com.ncs.nextbus.core.model.EmergencyContact
import com.ncs.nextbus.core.ui.formatDistance

@Composable
internal fun StatsRow(stats: TravelStats) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.screenHorizontal),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Spacing.lg),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            Stat(stats.completedTrips.toString(), stringResource(R.string.profile_stat_trips))
            Stat(formatDistance(stats.totalDistanceMeters), stringResource(R.string.profile_stat_distance))
            Stat(stats.savedItems.toString(), stringResource(R.string.profile_stat_saved))
        }
    }
}

@Composable
private fun Stat(value: String, label: String) {
    Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

internal fun LazyListScope.safetySection(
    contacts: List<EmergencyContact>,
    onAdd: (name: String, phone: String) -> ContactError?,
    onRemove: (EmergencyContact) -> Unit,
    onOpenSos: () -> Unit,
) {
    item { SettingsHeader(stringResource(R.string.profile_safety)) }
    item {
        ListItem(
            headlineContent = { Text(stringResource(R.string.sos_title)) },
            supportingContent = { Text(stringResource(R.string.profile_sos_summary)) },
            leadingContent = { Icon(Icons.Outlined.Sos, contentDescription = null, tint = NextBusTheme.extendedColors.sos) },
            modifier = Modifier.clickable(role = Role.Button, onClick = onOpenSos),
        )
    }
    items(contacts, key = { it.phone }) { contact ->
        ListItem(
            headlineContent = { Text(contact.name) },
            supportingContent = { Text(contact.phone) },
            trailingContent = {
                IconButton(onClick = { onRemove(contact) }) {
                    Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.profile_remove_contact, contact.name))
                }
            },
        )
    }
    if (contacts.size < ProfileViewModel.MAX_CONTACTS) {
        item { AddContactRow(onAdd) }
    }
}

@Composable
private fun AddContactRow(onAdd: (String, String) -> ContactError?) {
    var open by rememberSaveable { mutableStateOf(false) }
    ListItem(
        headlineContent = { Text(stringResource(R.string.profile_add_contact)) },
        supportingContent = { Text(stringResource(R.string.profile_add_contact_summary)) },
        leadingContent = { Icon(Icons.Outlined.PersonAdd, contentDescription = null) },
        modifier = Modifier.clickable(role = Role.Button) { open = true },
    )
    if (open) AddContactDialog(onDismiss = { open = false }, onSave = { name, phone -> onAdd(name, phone).also { if (it == null) open = false } })
}

@Composable
private fun AddContactDialog(onDismiss: () -> Unit, onSave: (String, String) -> ContactError?) {
    var name by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<ContactError?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.profile_add_contact)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it.take(ProfileViewModel.MAX_NAME_LENGTH)
                        error = null
                    },
                    label = { Text(stringResource(R.string.profile_contact_name)) },
                    singleLine = true,
                    isError = error == ContactError.NAME_REQUIRED,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = {
                        phone = it.take(20)
                        error = null
                    },
                    label = { Text(stringResource(R.string.profile_contact_phone)) },
                    singleLine = true,
                    isError = error == ContactError.INVALID_PHONE || error == ContactError.DUPLICATE,
                    supportingText = error?.let { { Text(stringResource(it.message())) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done),
                )
            }
        },
        confirmButton = { TextButton(onClick = { error = onSave(name, phone) }) { Text(stringResource(R.string.action_save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

private fun ContactError.message(): Int = when (this) {
    ContactError.NAME_REQUIRED -> R.string.profile_contact_error_name
    ContactError.INVALID_PHONE -> R.string.profile_contact_error_phone
    ContactError.DUPLICATE -> R.string.profile_contact_error_duplicate
    ContactError.LIMIT_REACHED -> R.string.profile_contact_error_limit
}
