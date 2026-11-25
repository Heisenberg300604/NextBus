package com.ncs.nextbus.feature.saved

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.ncs.nextbus.R
import com.ncs.nextbus.core.designsystem.component.ComingSoon

@Composable
fun SavedScreen(modifier: Modifier = Modifier) {
    ComingSoon(title = stringResource(R.string.nav_saved), modifier = modifier)
}
