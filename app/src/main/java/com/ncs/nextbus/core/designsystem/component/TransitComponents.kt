package com.ncs.nextbus.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ncs.nextbus.core.designsystem.theme.Spacing
import com.ncs.nextbus.core.model.LiveBus
import com.ncs.nextbus.core.ui.busStatusLine
import com.ncs.nextbus.core.ui.towards

/**
 * Standard row for a live bus: route badge, destination, what it's doing, freshness, and a
 * right-aligned headline figure (ETA or distance).
 */
@Composable
fun BusListItem(
    liveBus: LiveBus,
    nowMillis: Long,
    trailingPrimary: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailingSecondary: String? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) {}
            .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RouteBadge(number = liveBus.route.number, colorArgb = liveBus.route.colorArgb)
        Spacer(Modifier.width(Spacing.md))
        Column(Modifier.weight(1f)) {
            Text(
                text = towards(liveBus.route, liveBus.direction),
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = busStatusLine(liveBus),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(Spacing.xxs))
            FreshnessIndicator(
                lastUpdatedMillis = liveBus.lastUpdatedMillis,
                nowMillis = nowMillis,
                textStyle = MaterialTheme.typography.labelSmall,
            )
        }
        Spacer(Modifier.width(Spacing.sm))
        Column(horizontalAlignment = Alignment.End) {
            Text(trailingPrimary, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            if (trailingSecondary != null) {
                Text(
                    trailingSecondary,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

enum class TimelineState { PASSED, CURRENT, UPCOMING }

/** One stop in a vertical route timeline: connector line, dot, name and a trailing time. */
@Composable
fun TimelineRow(
    title: String,
    state: TimelineState,
    lineColor: Color,
    isFirst: Boolean,
    isLast: Boolean,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailing: String? = null,
    onClick: (() -> Unit)? = null,
) {
    val dimmed = state == TimelineState.PASSED
    val contentColor = if (dimmed) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
    val passedLine = MaterialTheme.colorScheme.outlineVariant
    val surface = MaterialTheme.colorScheme.surface
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .semantics(mergeDescendants = true) {}
            .padding(horizontal = Spacing.screenHorizontal),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(24.dp)
                .fillMaxHeight(),
        ) {
            Canvas(Modifier.matchParentSizeCompat()) {
                val x = size.width / 2
                val y = size.height / 2
                val stroke = 4.dp.toPx()
                val topColor = if (dimmed) passedLine else lineColor
                if (!isFirst) drawLine(topColor, Offset(x, 0f), Offset(x, y), stroke)
                if (!isLast) {
                    val bottomColor = if (state == TimelineState.UPCOMING || state == TimelineState.CURRENT) lineColor else passedLine
                    drawLine(bottomColor, Offset(x, y), Offset(x, size.height), stroke)
                }
                val radius = if (state == TimelineState.CURRENT) 8.dp.toPx() else 5.dp.toPx()
                drawCircle(surface, radius, Offset(x, y))
                drawCircle(if (dimmed) passedLine else lineColor, radius, Offset(x, y), style = Stroke(3.dp.toPx()))
                if (state == TimelineState.CURRENT) drawCircle(lineColor, radius / 2, Offset(x, y))
            }
        }
        Spacer(Modifier.width(Spacing.md))
        Column(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 52.dp)
                .padding(vertical = Spacing.sm),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (state == TimelineState.CURRENT) FontWeight.SemiBold else FontWeight.Normal,
                color = contentColor,
            )
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (trailing != null) {
            Text(
                text = trailing,
                style = MaterialTheme.typography.labelLarge,
                color = if (dimmed) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
            )
        }
    }
}

private fun Modifier.matchParentSizeCompat(): Modifier = this.fillMaxWidth().fillMaxHeight()
