package com.ncs.nextbus.core.designsystem.component

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Propane
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.ncs.nextbus.R
import com.ncs.nextbus.core.designsystem.theme.NextBusTheme
import com.ncs.nextbus.core.designsystem.theme.Spacing
import com.ncs.nextbus.core.model.Freshness
import com.ncs.nextbus.core.model.FuelType
import com.ncs.nextbus.core.ui.formatRelativeAge

/** Route number on the route's own colour, e.g. a green "1A" pill. */
@Composable
fun RouteBadge(
    number: String,
    colorArgb: Long,
    modifier: Modifier = Modifier,
    large: Boolean = false,
) {
    val background = Color(colorArgb)
    val foreground = if (background.luminance() > 0.5f) Color.Black else Color.White
    val description = stringResource(R.string.cd_route_number, number)
    Box(
        modifier = modifier
            .defaultMinSize(minWidth = if (large) 56.dp else 44.dp)
            .background(background, MaterialTheme.shapes.small)
            .padding(horizontal = Spacing.sm, vertical = if (large) Spacing.sm else Spacing.xs)
            .clearAndSetSemantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = number,
            color = foreground,
            style = if (large) MaterialTheme.typography.titleLarge else MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
fun FuelBadge(fuelType: FuelType, modifier: Modifier = Modifier) {
    val colors = NextBusTheme.extendedColors
    val (icon, color, label) = when (fuelType) {
        FuelType.ELECTRIC -> Triple(Icons.Filled.ElectricBolt, colors.electric, R.string.fuel_electric)
        FuelType.CNG -> Triple(Icons.Filled.Propane, colors.cng, R.string.fuel_cng)
        FuelType.DIESEL -> Triple(Icons.Filled.LocalGasStation, colors.diesel, R.string.fuel_diesel)
    }
    LabelChip(text = stringResource(label), color = color, modifier = modifier) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
    }
}

/** "On time" / "3 min late" pill. */
@Composable
fun DelayChip(delayMinutes: Int, isDelayed: Boolean, modifier: Modifier = Modifier) {
    val colors = NextBusTheme.extendedColors
    if (isDelayed) {
        LabelChip(
            text = pluralStringResource(R.plurals.status_minutes_late, delayMinutes, delayMinutes),
            color = colors.warning,
            modifier = modifier,
        )
    } else {
        LabelChip(text = stringResource(R.string.status_on_time), color = colors.success, modifier = modifier)
    }
}

@Composable
fun LabelChip(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = color.copy(alpha = 0.12f),
        contentColor = color,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xxs + 1.dp),
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            leading?.invoke()
            Text(text, style = MaterialTheme.typography.labelMedium)
        }
    }
}

/** Pulsing dot + "Live", "Updated 2 min ago" or "Offline". */
@Composable
fun FreshnessIndicator(
    lastUpdatedMillis: Long,
    nowMillis: Long,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = MaterialTheme.typography.labelMedium,
) {
    val colors = NextBusTheme.extendedColors
    val freshness = Freshness.of(lastUpdatedMillis, nowMillis)
    val (color, label) = when (freshness) {
        Freshness.LIVE -> colors.success to stringResource(R.string.freshness_live)
        Freshness.STALE -> colors.warning to stringResource(
            R.string.freshness_updated,
            formatRelativeAge(nowMillis - lastUpdatedMillis),
        )
        Freshness.OFFLINE -> colors.offline to stringResource(R.string.freshness_offline)
    }
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs + 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StatusDot(color = color, pulsing = freshness == Freshness.LIVE)
        Text(label, style = textStyle, color = color)
    }
}

@Composable
fun StatusDot(color: Color, pulsing: Boolean, modifier: Modifier = Modifier) {
    val alpha = if (pulsing) {
        val transition = rememberInfiniteTransition(label = "dot")
        val value by transition.animateFloat(
            initialValue = 1f,
            targetValue = 0.35f,
            animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
            label = "dotAlpha",
        )
        value
    } else {
        1f
    }
    Box(
        modifier
            .size(8.dp)
            .alpha(alpha)
            .background(color, CircleShape),
    )
}
