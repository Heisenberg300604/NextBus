package com.ncs.nextbus.feature.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Construction
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.ncs.nextbus.R
import com.ncs.nextbus.core.designsystem.component.EmptyState

@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    EmptyState(
        icon = Icons.Outlined.Construction,
        title = stringResource(R.string.nav_home),
        message = stringResource(R.string.placeholder_coming_soon),
        modifier = modifier,
    )
}
