package com.ncs.nextbus.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Chip
import androidx.compose.material3.ChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.drawArc
import androidx.compose.ui.graphics.drawscope.drawCircle
import androidx.compose.ui.graphics.drawscope.drawLine
import androidx.compose.ui.graphics.drawscope.drawRect
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.solidColor
import androidx.compose.ui.input.pointer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.paint.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.ncs.nextbus.model.RealtimeDB
import com.ncs.nextbus.ui.theme.*
import com.ncs.nextbus.ui.theme.Indigo600
import com.ncs.nextbus.ui.theme.Emerald500
import com.ncs.nextbus.ui.theme.Amber500
import com.ncs.nextbus.ui.theme.Charcoal900
import com.ncs.nextbus.ui.theme.OnIndigo50
import com.ncs.nextbus.ui.theme.OnAmber50
import com.ncs.nextbus.ui.theme.OnCharcoal50

object ChipState {
    enum class Mode { EV, DIESEL, Other, Alert }
}

@Composable
fun TransitBadge(
    mode: ChipState.Mode,
    onRemove: () -> Unit = {},
    content: @Composable () -> Unit,
    glow: Boolean = true
) {
    val (bgColor, textColor, glowColor, icon) = when (mode) {
        ChipState.Mode.EV -> (Indigo600.copy(alpha = 0.9f), OnIndigo50, IndigoGlow, "⚡ EV")
        ChipState.Mode.DIESEL -> (Amber500.copy(alpha = 0.9f), OnAmber50, AmberGlow, "🛢️ DSL")
        ChipState.Mode.Other -> (Charcoal900.copy(alpha = 0.8f), OnCharcoal50, Color.Gray.copy(alpha = 0.3f), "🚌")
        ChipState.Mode.Alert -> (Color.Red.copy(alpha = 0.9f), OnCharcoal50, Color.Red.copy(alpha = 0.4f), "⚠️")
    }

    val hover by remember { mutableStateOf(false) }
    val animatedScale by animateFloatAsState(
        targetValue = if (hover) 1.05f else 1f,
        animationSpec = spring(stiffness = 500f, dampingRatio = 20f)
    )

    Box(
        modifier = Modifier
            .scale(animatedScale)
            .pointerInput(Unit) {
                detectTapGestures(onPress = { /* press */ })
                detectDragGestures(
                    onDragStart = { _, _ -> true },
                    onDragCancel = { hover = false },
                    onDragEnd = { _, _ -> hover = false }
                )
            }
    ) {
        if (glow) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.radialGradient(
                            center = Alignment.Center,
                            colors = listOf(glowColor, glowColor.copy(alpha = 0f)),
                            stops = listOf(0f, 0.6f)
                        )
                    )
            )
        }

        Chip(
            modifier = Modifier
                .padding(2.dp)
                .background(bgColor, RoundedCornerShape(10.dp))
                .border(1.dp, textColor.copy(alpha = 0.3f), RoundedCornerShape(10.dp)),
            style = ChipDefaults.ChipWithoutTrailingIconDefaults().colors.copy(containerColor = bgColor),
            onClick = onRemove,
            label = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = icon,
                        fontSize = 12.sp,
                        color = textColor,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = mode.toString().replace("EV", "Electric").replace("DIESEL", "Diesel"),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = textColor,
                        maxLines = 1
                    )
                }
            }
        )
    }
}

@Composable
fun PillChip(
    text: String,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    glow: Boolean = false
) {
    val (bgColor, textColor) = if (selected) {
        if (glow) Indigo600 else Indigo600.copy(alpha = 0.95f)
    } else {
        Charcoal900.copy(alpha = 0.08f)
    } to if (selected) OnIndigo50 else Charcoal900.copy(alpha = 0.6f)

    val hover by remember { mutableStateOf(false) }
    val animatedScale by animateFloatAsState(
        targetValue = if (hover && enabled) 1.03f else 1f,
        animationSpec = spring(stiffness = 500f, dampingRatio = 22f)
    )

    Chip(
        modifier = Modifier
            .padding(2.dp)
            .scale(animatedScale)
            .background(bgColor, shape = RoundedCornerShape(20.dp))
            .border(
                1.dp,
                if (selected) Indigo600 else Charcoal900.copy(alpha = 0.15f),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(
                onClick = if (enabled) onClick else {},
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
            ),
        style = ChipDefaults.ChipDefaults().colors.copy(
            containerColor = bgColor,
            contentColor = textColor
        ),
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = text,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = textColor,
                maxLines = 1,
                overflow = androidx.compose.ui.text.TextOverflow.Ellipsis
            )
        }
    )
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    elevation: Dp = 0.dp,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    tint: Color = Charcoal900.copy(alpha = 0.06f),
    borderColor: Color = Color(0xFF000000).copy(alpha = 0.1f),
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.verticalGradient(
                    0f to tint.copy(alpha = 0.8f),
                    1f to tint
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .border(1.dp, borderColor.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
        contentPadding = contentPadding
    ) {
        content()
    }
}

@Composable
fun GlowBox(
    color: Color,
    size: Dp = 60.dp,
    blur: Float = 24f
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    center = Alignment.Center,
                    colors = listOf(color, color.copy(alpha = 0f)),
                    stops = listOf(0f, 1f)
                )
            )
    )
}

@Composable
fun NearbyBusCard(
    bus: RealtimeDB,
    onClick: () -> Unit,
    showFavorite: Boolean = false,
    isFavorite: Boolean = false
) {
    var favoriteToggle by remember { mutableStateOf(isFavorite) }
    val hover by remember { mutableStateOf(false) }
    val animatedScale by animateFloatAsState(
        targetValue = if (hover) 1.02f else 1f,
        animationSpec = spring(stiffness = 400f, dampingRatio = 20f)
    )

    val fuelColor = when (bus.item?.fueltype) {
        "EV" -> Indigo600
        "DIESEL" -> Amber500
        else -> Emerald500
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .scale(animatedScale)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.verticalGradient(
                    0f to Color(0xFFFFFFFF).copy(alpha = 0.9f),
                    1f to Color(0xFFFFFFFF).copy(alpha = 0.8f)
                )
            )
            .border(1.dp, Charcoal900.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
            .clickable(
                onClick = onClick,
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
            ),
        verticalArrangement = Arrangement.Center,
        horizontalArrangement = Arrangement.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(fuelColor.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = bus.item?.busNum ?: "—",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = fuelColor
                )
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(end = 4.dp)
            ) {
                TransitBadge(
                    mode = when (bus.item?.fueltype) {
                        "EV" -> ChipState.Mode.EV
                        "DIESEL" -> ChipState.Mode.DIESEL
                        else -> ChipState.Mode.Other
                    }
                )
            }

            if (showFavorite) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            if (favoriteToggle) Amber500 else Surface0,
                            RoundedCornerShape(16.dp)
                        )
                        .border(1.dp, if (favoriteToggle) Amber500 else Charcoal900.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                        .clickable(onClick = { favoriteToggle = !favoriteToggle }),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (favoriteToggle) androidx.compose.material.icons.filled.Favorite else androidx.compose.material.icons.outlined.Favorite,
                        contentDescription = null,
                        tint = if (favoriteToggle) Amber500 else Charcoal900.copy(alpha = 0.4f),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            contentPadding = PaddingValues(12.dp, 4.dp)
        ) {
            Text(
                text = bus.item?.busName ?: "Bus",
                fontSize = 14.sp,
                fontWeight = FontWeight.Semibold,
                color = Charcoal900,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "${bus.item?.start ?: ""} → ${bus.item?.destination ?: ""}",
                fontSize = 12.sp,
                color = Charcoal900.copy(alpha = 0.5f),
                overflow = TextOverflow.Ellipsis,
                maxLines = 1
            )
        }
    }
}