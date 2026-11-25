package com.ncs.nextbus.feature.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ncs.nextbus.R
import com.ncs.nextbus.core.designsystem.theme.NextBusTheme
import com.ncs.nextbus.core.designsystem.theme.Sizes
import com.ncs.nextbus.core.designsystem.theme.Spacing
import com.ncs.nextbus.core.ui.rememberLocationPermissionRequest

/** First-run welcome. Explains the app and asks for location with a clear rationale; skippable. */
@Composable
fun OnboardingScreen(
    onLocationPermissionResult: (granted: Boolean) -> Unit,
    onFinished: () -> Unit,
) {
    val requestPermission = rememberLocationPermissionRequest { granted ->
        onLocationPermissionResult(granted)
        onFinished()
    }
    OnboardingContent(onAllowLocation = requestPermission, onSkip = onFinished)
}

@Composable
private fun OnboardingContent(onAllowLocation: () -> Unit, onSkip: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.xl, vertical = Spacing.lg),
        ) {
            Spacer(Modifier.height(Spacing.xxl))
            Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.primary) {
                Image(
                    painter = painterResource(R.drawable.ic_launcher_foreground),
                    contentDescription = null,
                    modifier = Modifier.size(88.dp),
                )
            }
            Spacer(Modifier.height(Spacing.xl))
            Text(
                text = stringResource(R.string.onboarding_title),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.semantics { heading() },
            )
            Spacer(Modifier.height(Spacing.sm))
            Text(
                text = stringResource(R.string.onboarding_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(Spacing.xl))

            Column(verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
                Feature(Icons.Outlined.Map, R.string.onboarding_live_title, R.string.onboarding_live_body)
                Feature(Icons.Outlined.Route, R.string.onboarding_plan_title, R.string.onboarding_plan_body)
                Feature(
                    Icons.Outlined.Shield,
                    R.string.onboarding_safe_title,
                    R.string.onboarding_safe_body,
                    tint = NextBusTheme.extendedColors.sos,
                )
            }

            Spacer(Modifier.weight(1f, fill = true).heightIn(min = Spacing.xl))

            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    Icons.Outlined.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(Sizes.iconSmall),
                )
                Spacer(Modifier.width(Spacing.sm))
                Text(
                    text = stringResource(R.string.onboarding_location_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(Spacing.lg))
            Button(
                onClick = onAllowLocation,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp),
            ) { Text(stringResource(R.string.onboarding_allow_location)) }
            TextButton(
                onClick = onSkip,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = Sizes.touchTarget),
            ) { Text(stringResource(R.string.onboarding_skip), textAlign = TextAlign.Center) }
        }
    }
}

@Composable
private fun Feature(icon: ImageVector, title: Int, body: Int, tint: Color = MaterialTheme.colorScheme.primary) {
    Row(verticalAlignment = Alignment.Top) {
        Surface(shape = MaterialTheme.shapes.medium, color = tint.copy(alpha = 0.12f)) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.padding(Spacing.sm + 2.dp).size(22.dp))
        }
        Spacer(Modifier.width(Spacing.lg))
        Column(Modifier.weight(1f)) {
            Text(stringResource(title), style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(Spacing.xxs))
            Text(
                stringResource(body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview
@Composable
private fun OnboardingPreview() {
    NextBusTheme { OnboardingContent(onAllowLocation = {}, onSkip = {}) }
}
