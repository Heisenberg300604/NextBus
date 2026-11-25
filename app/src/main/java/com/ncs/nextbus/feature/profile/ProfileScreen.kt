package com.ncs.nextbus.feature.profile

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.ncs.nextbus.R
import com.ncs.nextbus.core.designsystem.component.ComingSoon

@Composable
fun ProfileScreen(modifier: Modifier = Modifier) {
    ComingSoon(title = stringResource(R.string.nav_profile), modifier = modifier)
}
