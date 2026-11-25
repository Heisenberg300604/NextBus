package com.ncs.nextbus.feature.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.LocationOff
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.ncs.nextbus.BuildConfig
import com.ncs.nextbus.R
import com.ncs.nextbus.core.designsystem.component.ErrorState
import com.ncs.nextbus.core.designsystem.component.LoadingState
import com.ncs.nextbus.core.designsystem.theme.Sizes
import com.ncs.nextbus.core.designsystem.theme.Spacing
import com.ncs.nextbus.core.designsystem.theme.supportsDynamicColor
import com.ncs.nextbus.core.model.ThemeMode
import com.ncs.nextbus.core.model.UserPreferences
import com.ncs.nextbus.core.ui.AppLanguage
import com.ncs.nextbus.core.ui.hasLocationPermission
import com.ncs.nextbus.core.ui.openAppSettings
import com.ncs.nextbus.core.ui.rememberLocationPermissionRequest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onLocationPermissionChanged: () -> Unit,
    modifier: Modifier = Modifier,
    extraSections: LazyListScope.() -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.nav_profile)) }, scrollBehavior = scrollBehavior)
        },
    ) { padding ->
        when (val current = state) {
            ProfileUiState.Loading -> LoadingState(Modifier.padding(padding))
            ProfileUiState.Error -> ErrorState(Modifier.padding(padding))
            is ProfileUiState.Ready -> LazyColumn(
                contentPadding = padding,
                modifier = Modifier.fillMaxWidth(),
            ) {
                item { ProfileHeader(current.preferences, onSaveName = viewModel::setDisplayName) }
                extraSections()
                settingsSections(
                    preferences = current.preferences,
                    onThemeMode = viewModel::setThemeMode,
                    onDynamicColor = viewModel::setDynamicColor,
                    onLocationPermissionChanged = onLocationPermissionChanged,
                )
            }
        }
    }
}

@Composable
private fun ProfileHeader(preferences: UserPreferences, onSaveName: (String) -> Boolean) {
    var editing by rememberSaveable { mutableStateOf(false) }
    val name = preferences.displayName.ifBlank { stringResource(R.string.profile_default_name) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(Sizes.avatar)) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = name.trim().take(1).uppercase(),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
        Spacer(Modifier.width(Spacing.lg))
        Column(Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.titleLarge)
            Text(
                text = stringResource(R.string.profile_local_only),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = { editing = true }) {
            Icon(Icons.Outlined.Edit, contentDescription = stringResource(R.string.profile_edit_name))
        }
    }

    if (editing) {
        EditNameDialog(
            initial = preferences.displayName,
            onDismiss = { editing = false },
            onSave = { if (onSaveName(it)) editing = false },
        )
    }
}

@Composable
private fun EditNameDialog(initial: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var text by rememberSaveable { mutableStateOf(initial) }
    val tooLong = !ProfileViewModel.isValidName(text)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.profile_edit_name)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(stringResource(R.string.profile_name_label)) },
                singleLine = true,
                isError = tooLong,
                supportingText = {
                    Text(
                        if (tooLong) {
                            pluralStringResource(R.plurals.profile_name_too_long, ProfileViewModel.MAX_NAME_LENGTH, ProfileViewModel.MAX_NAME_LENGTH)
                        } else {
                            stringResource(R.string.profile_name_supporting)
                        },
                    )
                },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(text) }, enabled = !tooLong) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

private fun LazyListScope.settingsSections(
    preferences: UserPreferences,
    onThemeMode: (ThemeMode) -> Unit,
    onDynamicColor: (Boolean) -> Unit,
    onLocationPermissionChanged: () -> Unit,
) {
    item { SettingsHeader(stringResource(R.string.settings_appearance)) }
    item { ThemeSelector(selected = preferences.themeMode, onSelect = onThemeMode) }
    if (supportsDynamicColor) {
        item {
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_dynamic_color)) },
                supportingContent = { Text(stringResource(R.string.settings_dynamic_color_summary)) },
                leadingContent = { Icon(Icons.Outlined.Palette, contentDescription = null) },
                trailingContent = {
                    Switch(checked = preferences.useDynamicColor, onCheckedChange = null)
                },
                modifier = Modifier.toggleRow(preferences.useDynamicColor) { onDynamicColor(!preferences.useDynamicColor) },
            )
        }
    }
    item { SettingsHeader(stringResource(R.string.settings_language)) }
    item { LanguageSelector() }
    item { SettingsHeader(stringResource(R.string.settings_location)) }
    item { LocationPermissionRow(onLocationPermissionChanged) }
    item { SettingsHeader(stringResource(R.string.settings_about)) }
    item {
        ListItem(
            headlineContent = { Text(stringResource(R.string.about_version, BuildConfig.VERSION_NAME)) },
            supportingContent = { Text(stringResource(R.string.about_demo_data)) },
            leadingContent = { Icon(Icons.Outlined.Info, contentDescription = null) },
        )
    }
    item { Spacer(Modifier.padding(bottom = Spacing.lg)) }
}

@Composable
fun SettingsHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier
            .padding(start = Spacing.screenHorizontal, end = Spacing.screenHorizontal, top = Spacing.xl, bottom = Spacing.sm)
            .semantics { heading() },
    )
}

@Composable
private fun ThemeSelector(selected: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    val options = listOf(
        ThemeMode.SYSTEM to R.string.theme_system,
        ThemeMode.LIGHT to R.string.theme_light,
        ThemeMode.DARK to R.string.theme_dark,
    )
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.xs),
    ) {
        options.forEachIndexed { index, (mode, label) ->
            SegmentedButton(
                selected = mode == selected,
                onClick = { onSelect(mode) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
            ) { Text(stringResource(label)) }
        }
    }
}

@Composable
private fun LanguageSelector() {
    // Read on composition: changing language recreates the activity, so this is always current.
    val current = remember { AppLanguage.current() }
    val labels = mapOf(
        AppLanguage.SYSTEM to stringResource(R.string.language_system),
        AppLanguage.ENGLISH to stringResource(R.string.language_english),
        AppLanguage.HINDI to stringResource(R.string.language_hindi),
    )
    Column(Modifier.selectableGroup()) {
        AppLanguage.entries.forEach { language ->
            ListItem(
                headlineContent = { Text(labels.getValue(language)) },
                leadingContent = { RadioButton(selected = language == current, onClick = null) },
                modifier = Modifier.selectable(
                    selected = language == current,
                    role = Role.RadioButton,
                    onClick = { if (language != current) AppLanguage.apply(language) },
                ),
                colors = ListItemDefaults.colors(),
            )
        }
    }
}

@Composable
private fun LocationPermissionRow(onLocationPermissionChanged: () -> Unit) {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(context.hasLocationPermission()) }
    var askedOnce by rememberSaveable { mutableStateOf(false) }
    LifecycleResumeEffect(Unit) {
        granted = context.hasLocationPermission()
        onPauseOrDispose { }
    }
    val request = rememberLocationPermissionRequest { result ->
        granted = result
        askedOnce = true
        onLocationPermissionChanged()
    }
    ListItem(
        headlineContent = { Text(stringResource(R.string.settings_location)) },
        supportingContent = {
            Text(stringResource(if (granted) R.string.settings_location_allowed else R.string.settings_location_denied))
        },
        leadingContent = {
            Icon(if (granted) Icons.Outlined.LocationOn else Icons.Outlined.LocationOff, contentDescription = null)
        },
        modifier = Modifier.clickable(role = Role.Button) {
            // After one denial the system may stop showing the dialog, so send the user to settings.
            if (granted || askedOnce) context.openAppSettings() else request()
        },
    )
}

private fun Modifier.toggleRow(checked: Boolean, onToggle: () -> Unit): Modifier =
    this.then(Modifier.selectable(selected = checked, role = Role.Switch, onClick = onToggle))

