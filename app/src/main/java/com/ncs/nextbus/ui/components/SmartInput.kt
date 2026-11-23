package com.ncs.nextbus.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.keyboard.ImeAction
import androidx.compose.ui.input.keyboard.KeyboardOptions
import androidx.compose.ui.input.keyboard.KeyboardType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ncs.nextbus.ui.theme.*
import com.ncs.nextbus.ui.theme.Charcoal900
import com.ncs.nextbus.ui.theme.Indigo600
import com.ncs.nextbus.ui.theme.OnIndigo50
import com.ncs.nextbus.ui.theme.Surface0

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartOriginDestinationInput(
    departure: String,
    destination: String,
    onDepartureChange: (String) -> Unit,
    onDestinationChange: (String) -> Unit,
    onSwap: () -> Unit,
    onSetCurrentLocation: () -> Unit,
    onSearch: () -> Unit,
    enabled: Boolean = true,
    searchEnabled: Boolean = true
) {
    val showDestination by animateContentSize(
        animationSpec = tween(durationMillis = 300, easing = LinearOutSlowInEasing)
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.Start
    ) {
        // Departure row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = { /* trigger departure focus */ }),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlassTextField(
                value = departure,
                onValueChange = onDepartureChange,
                placeholder = "Departure",
                leadingIcon = { Icon(imageVector = Search, contentDescription = null, tint = Charcoal900.copy(alpha = 0.5f)) },
                trailingIcon = {
                    if (departure.isNotEmpty()) {
                        IconButton(onClick = { /* clear */ }) {
                            Icon(imageVector = Close, contentDescription = null, tint = Charcoal900.copy(alpha = 0.5f))
                        }
                    }
                },
                rightIcon = {
                    IconButton(onClick = onSetCurrentLocation) {
                        Icon(
                            imageVector = LocationOn,
                            contentDescription = "Current location",
                            tint = Indigo600
                        )
                    }
                },
                enabled = enabled
            )
        }

        // Swap button
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(
                    Brush.verticalGradient(
                        0f to Indigo600,
                        1f to Indigo600.copy(alpha = 0.85f)
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
                .clickable(onClick = onSwap),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = androidx.compose.material.icons.filled.SwapVert,
                contentDescription = "Swap",
                tint = OnIndigo50
            )
        }

        // Destination row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = { /* trigger destination focus */ }),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlassTextField(
                value = destination,
                onValueChange = onDestinationChange,
                placeholder = "Destination",
                leadingIcon = { Icon(imageVector = Search, contentDescription = null, tint = Charcoal900.copy(alpha = 0.5f)) },
                trailingIcon = {
                    if (destination.isNotEmpty()) {
                        IconButton(onClick = { /* clear */ }) {
                            Icon(imageVector = Close, contentDescription = null, tint = Charcoal900.copy(alpha = 0.5f))
                        }
                    }
                },
                rightIcon = {
                    IconButton(onClick = { /* go action */ }) {
                        Icon(
                            imageVector = LocationOn,
                            contentDescription = "Destination pin",
                            tint = Indigo600
                        )
                    }
                },
                enabled = enabled
            )
        }

        // Action button
        if (searchEnabled) {
            TextButton(
                onClick = onSearch,
                modifier = Modifier.fillMaxWidth(),
                colors = androidx.compose.material3.TextButtonDefaults.textColors(containerColor = Indigo600)
            ) {
                Text(
                    text = "Find Bus",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Semibold,
                    color = OnIndigo50
                )
            }
        }
    }
}

@Composable
fun GlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: @Composable () -> Unit = {},
    trailingIcon: @Composable () -> Unit = {},
    rightIcon: @Composable () -> Unit = {},
    enabled: Boolean = true
) {
    val focus = rememberUpdatedState(true)
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(text = placeholder, color = Charcoal900.copy(alpha = 0.5f)) },
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    0f to Surface0.copy(alpha = 0.9f),
                    1f to Surface0.copy(alpha = 0.7f)
                ),
                shape = RoundedCornerShape(14.dp)
            )
            .border(
                1.5.dp,
                if (focus.value) Indigo600 else Charcoal900.copy(alpha = 0.15f),
                RoundedCornerShape(14.dp)
            ),
        singleLine = true,
        enabled = enabled,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.TEXT,
            imeAction = ImeAction.DONE
        ),
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        colors = TextFieldDefaults.outlinedTextFieldColors(
            backgroundColor = Surface0.copy(alpha = 0.9f),
            unfocusedBorderColor = Charcoal900.copy(alpha = 0.15f),
            focusedBorderColor = Indigo600,
            disabledBorderColor = Charcoal900.copy(alpha = 0.08f)
        ),
        trailingIcon = rightIcon
    )
}

@Composable
fun AnimatedSwapButton(
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    val rotate by animateFloatAsState(
        targetValue = if (enabled) 180f else 0f,
        animationSpec = SpringSpec(stiffness = 400f, dampingRatio = 22f)
    )

    Box(
        modifier = Modifier
            .size(56.dp)
            .background(
                Brush.verticalGradient(
                    0f to Indigo600,
                    1f to Indigo600.copy(alpha = 0.85f)
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .scale(if (enabled) 1f else 0.9f)
            .rotate(rotate)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = androidx.compose.material.icons.filled.SwapVert,
            contentDescription = "Swap",
            tint = OnIndigo50
        )
    }
}

@Composable
fun LiveTransitCard(
    busNumber: String,
    routeName: String,
    etaMinutes: Int,
    fuelType: String = "EV",
    onClick: () -> Unit
) {
    val fuelColor = when (fuelType) {
        "EV" -> Indigo600
        "DIESEL" -> Amber500
        else -> Emerald500
    }
    val etaColor = if (etaMinutes <= 5) Amber500 else if (etaMinutes <= 10) Indigo600 else Emerald500

    Column(
        modifier = Modifier
            .width(160.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.verticalGradient(
                    0f to Surface0.copy(alpha = 0.95f),
                    1f to Surface0.copy(alpha = 0.85f)
                )
            )
            .border(1.5.dp, Charcoal900.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Glow accent at top
        Box(
            modifier = Modifier
                .width(32.dp)
                .height(4.dp)
                .background(fuelColor, shape = RoundedCornerShape(2.dp))
                .padding(top = 12.dp)
        )

        Box(
            modifier = Modifier
                .size(52.dp)
                .background(fuelColor.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = busNumber,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = fuelColor
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = routeName,
            fontSize = 14.sp,
            fontWeight = FontWeight.Semibold,
            color = Charcoal900,
            maxLines = 2,
            textAlign = androidx.compose.ui.text.TextAlign.Center,
            overflow = androidx.compose.ui.text.TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            contentPadding = PaddingValues(horizontal = 12.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Arriving",
                    fontSize = 10.sp,
                    color = Charcoal900.copy(alpha = 0.5f)
                )
                Text(
                    text = "$etaMinutes min",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = etaColor
                )
            }

            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(fuelColor.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = fuelType.first().uppercase(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = fuelColor
                )
            }
        }
    }
}