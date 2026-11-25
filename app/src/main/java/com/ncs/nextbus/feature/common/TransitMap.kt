package com.ncs.nextbus.feature.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.MarkerComposable
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberUpdatedMarkerState
import com.ncs.nextbus.BuildConfig
import com.ncs.nextbus.R
import com.ncs.nextbus.core.data.seed.SeedNetwork
import com.ncs.nextbus.core.designsystem.theme.Spacing
import com.ncs.nextbus.core.model.Freshness
import com.ncs.nextbus.core.model.GeoPoint
import com.ncs.nextbus.core.model.LiveBus
import com.ncs.nextbus.core.model.Route
import com.ncs.nextbus.core.ui.toLatLng
import kotlin.math.roundToInt

/** True when this build has a Maps SDK key; without one the map would render blank tiles. */
val mapsAvailable: Boolean get() = BuildConfig.HAS_MAPS_KEY

val CityCenterCamera: CameraPosition = CameraPosition.fromLatLngZoom(SeedNetwork.CITY_CENTER.toLatLng(), 12.5f)

fun boundsOf(points: List<GeoPoint>): LatLngBounds? {
    if (points.isEmpty()) return null
    val builder = LatLngBounds.builder()
    points.forEach { builder.include(it.toLatLng()) }
    return builder.build()
}

/**
 * Google Map showing route lines, their stops and live buses. Bus markers are composables showing the
 * route number with a heading arrow; offline buses fade out.
 */
@Composable
fun TransitMap(
    routes: List<Route>,
    buses: List<LiveBus>,
    nowMillis: Long,
    cameraPositionState: CameraPositionState,
    onBusClick: (LiveBus) -> Unit,
    modifier: Modifier = Modifier,
    selectedBusId: String? = null,
    showStops: Boolean = true,
    myLocationEnabled: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(),
    onMapClick: () -> Unit = {},
    onMapLoaded: () -> Unit = {},
) {
    if (!mapsAvailable) {
        MapUnavailable(modifier)
        return
    }
    val context = LocalContext.current
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val style = remember(dark) {
        if (dark) MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style_dark) else null
    }
    val stopFill = MaterialTheme.colorScheme.surface

    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState,
        properties = MapProperties(isMyLocationEnabled = myLocationEnabled, mapStyleOptions = style),
        uiSettings = MapUiSettings(
            zoomControlsEnabled = false,
            myLocationButtonEnabled = false,
            mapToolbarEnabled = false,
            compassEnabled = true,
        ),
        contentPadding = contentPadding,
        onMapClick = { onMapClick() },
        onMapLoaded = onMapLoaded,
    ) {
        routes.forEach { route ->
            key(route.id) {
                val color = Color(route.colorArgb)
                Polyline(points = route.path.map { it.toLatLng() }, color = color, width = 12f, zIndex = 0f)
                if (showStops) {
                    route.stops.forEach { routeStop ->
                        Circle(
                            center = routeStop.stop.location.toLatLng(),
                            radius = 28.0,
                            fillColor = stopFill,
                            strokeColor = color,
                            strokeWidth = 5f,
                            zIndex = 1f,
                        )
                    }
                }
            }
        }
        buses.forEach { bus ->
            key(bus.bus.id) {
                val selected = bus.bus.id == selectedBusId
                val offline = Freshness.of(bus.lastUpdatedMillis, nowMillis) == Freshness.OFFLINE
                // Bucket the heading so the marker bitmap is only re-rendered on meaningful turns.
                val heading = ((bus.bearingDegrees / 15f).roundToInt() * 15) % 360
                MarkerComposable(
                    bus.bus.id, bus.route.number, selected, offline, heading,
                    state = rememberUpdatedMarkerState(position = bus.position.toLatLng()),
                    anchor = Offset(0.5f, 0.5f),
                    title = bus.route.number,
                    zIndex = if (selected) 3f else 2f,
                    onClick = {
                        onBusClick(bus)
                        true
                    },
                ) {
                    BusMarker(
                        number = bus.route.number,
                        color = Color(bus.route.colorArgb),
                        headingDegrees = heading.toFloat(),
                        selected = selected,
                        offline = offline,
                    )
                }
            }
        }
    }
}

@Composable
private fun BusMarker(number: String, color: Color, headingDegrees: Float, selected: Boolean, offline: Boolean) {
    val size = if (selected) 52.dp else 44.dp
    Box(
        modifier = Modifier
            .size(size)
            .alpha(if (offline) 0.45f else 1f),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .matchParentSize()
                .rotate(headingDegrees),
        ) {
            Icon(
                imageVector = Icons.Filled.Navigation,
                contentDescription = null,
                tint = color,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .size(14.dp),
            )
        }
        Box(
            modifier = Modifier
                .size(size - 16.dp)
                .background(color, CircleShape)
                .border(if (selected) 3.dp else 2.dp, Color.White, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = number,
                color = Color.White,
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** Shown instead of the map when this build has no Maps key. Live data still works everywhere else. */
@Composable
fun MapUnavailable(modifier: Modifier = Modifier, fillHeight: Boolean = true) {
    Surface(modifier = modifier, color = MaterialTheme.colorScheme.surfaceContainer, shape = MaterialTheme.shapes.medium) {
        Column(
            modifier = (if (fillHeight) Modifier.fillMaxSize() else Modifier.fillMaxWidth())
                .padding(if (fillHeight) Spacing.xl else Spacing.lg),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(Icons.Outlined.Map, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(Spacing.sm))
            Text(
                text = stringResource(R.string.map_unavailable_title),
                style = MaterialTheme.typography.titleSmall,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.map_unavailable_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** Convenience for screens that only need a LatLng from a domain point. */
fun LiveBus.latLng(): LatLng = position.toLatLng()
