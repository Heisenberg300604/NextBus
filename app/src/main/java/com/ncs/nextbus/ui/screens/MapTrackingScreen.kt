package com.ncs.nextbus.ui.screens

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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.drawscope.drawCircle
import com.google.maps.android.compose.*
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.MapMarker
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.googleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.firebase.database.*
import coil.compose.AsyncImage
import com.ncs.nextbus.model.RealtimeDB
import com.ncs.nextbus.ui.components.*
import com.ncs.nextbus.ui.theme.*
import com.ncs.nextbus.ui.theme.Charcoal900
import com.ncs.nextbus.ui.theme.Indigo600
import com.ncs.nextbus.ui.theme.Emerald500
import com.ncs.nextbus.ui.theme.Amber500
import com.ncs.nextbus.ui.theme.OnIndigo50
import com.ncs.nextbus.ui.theme.OnCharcoal50
import com.ncs.nextbus.ui.theme.Surface0
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Composable
fun MapTrackingScreen(
    viewModel: MapViewModel = hiltViewModel(),
    navigator: NavHostNavigator = hiltViewModel()
) {
    val localContext = LocalContext.current
    val scope = rememberCoroutineScope()
    var showBottomSheet by remember { mutableStateOf(false) }
    var clickedBus by remember { mutableStateOf<RealtimeDB?>(null) }
    var showPolyline by remember { mutableStateOf(false) }
    var cameraPositionState = rememberCameraPositionState()
    var snackbarHostState = remember { SnackbarHostState() }

    // Firebase Realtime Database reference
    val databaseReference = Firebase.database.reference.child("bus_location")

    // Collect live bus locations
    val busLocations by remember { mutableStateOf<List<RealtimeDB>>(emptyList()) }

    LaunchedEffect(Unit) {
        databaseReference.orderByChild("timestamp").addValueEventListener(object: ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                var locations = mutableListOf<RealtimeDB>()
                val iter = dataSnapshot.children.iterator()
                while (iter.hasNext()) {
                    val child = iter.next()
                    val data = child.getValue(RealtimeDB::class.java)
                    if (data != null) {
                        locations.add(data)
                    }
                }
                busLocations.value = locations
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error silently
            }
        })
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {
            // ── Top App Bar ──
            TopAppBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = padding.calculateTopPadding()),
                colors = TopAppBarDefaults.smallTopAppBarColors(
                    containerColor = Surface0.copy(alpha = 0.9f)
                ),
                elevation = 4.dp,
                title = {
                    Text(
                        text = "Live Tracking",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Charcoal900
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navigator.navigateToHome() }) {
                        Icon(
                            imageVector = androidx.compose.material.icons.filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Charcoal900
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showBottomSheet = true }) {
                        Icon(
                            imageVector = androidx.compose.material.icons.filled.FilterList,
                            contentDescription = "Filters",
                            tint = Charcoal900
                        )
                    }
                }
            )

            // ── Map ──
            googleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                onMapLoaded = {
                    // Center on a sample location (Delhi)
                    cameraPositionState.cameraPosition = CameraPosition.fromLatLngZoom(
                        com.google.android.gms.maps.model.LatLng(28.6139, 77.2090),
                        13f
                    )
                },
                mapType = MapType.Normal,
                properties = MapProperties(
                    isMyLocationEnabled = true,
                    compassEnabled = true
                ),
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = true,
                    myLocationButtonEnabled = true
                )
            ) {
                // Route polyline
                if (showPolyline) {
                    Polyline(
                        points = generateRoutePoints(),
                        color = Indigo600,
                        strokeWidth = 6f,
                        geodesic = true,
                        jointType = JointType.ROUND,
                        startCap = Cap.ROUND,
                        endCap = Cap.ROUND
                    )
                }

                // Animated bus markers
                for (i in busLocations.indices) {
                    val bus = busLocations[i]
                    val fuelType = bus.item?.fueltype ?: "Other"

                    // Animated scale for hover effect
                    val markerScale by animateFloatAsState(
                        targetValue = 1f,
                        animationSpec = SpringSpec(stiffness = 300f, dampingRatio = 22f)
                    )

                    val fuelColor = when (fuelType) {
                        "EV" -> Indigo600
                        "DIESEL" -> Amber500
                        else -> Emerald500
                    }

                    // Glow effect around marker
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    center = Alignment.Center,
                                    colors = listOf(fuelColor.copy(alpha = 0.3f), fuelColor.copy(alpha = 0f)),
                                    stops = listOf(0f, 1f)
                                )
                            )
                    ) {
                        MapMarker(
                            position = com.google.android.gms.maps.model.LatLng(
                                bus.item?.latitude ?: 28.6139,
                                bus.item?.longitude ?: 77.2090
                            ),
                            title = bus.item?.busNum ?: "Bus $i",
                            snippet = "${bus.item?.destination ?: ""}",
                            rotationZ = calculateRotation(bus),
                            onInfoWindowClick = {
                                clickedBus = bus
                                showBottomSheet = true
                            },
                            icon = painterResource(
                                id = when (fuelType) {
                                    "EV" -> R.drawable.bluebus
                                    "DIESEL" -> R.drawable.redbus
                                    else -> R.drawable.greenbus
                                }
                            ),
                            infoWindowAnchor = 0.5f to 0f,
                            anchor = 0.5f to 0.5f,
                            flat = false,
                            zIndex = i.toFloat()
                        )
                    }
                }

                // Starting and destination markers for selected bus
                if (clickedBus.value != null) {
                    MapMarker(
                        position = com.google.android.gms.maps.model.LatLng(
                            clickedBus.value?.item?.startinglat ?: 28.6139f,
                            clickedBus.value?.item?.startinglong ?: 77.2090f
                        ),
                        title = "Start",
                        icon = painterResource(id = R.drawable.start),
                        infoWindowAnchor = 0.5f to 0f,
                        anchor = 0.5f to 0.5f
                    )
                    MapMarker(
                        position = com.google.android.gms.maps.model.LatLng(
                            clickedBus.value?.item?.endinglat ?: 28.6500f,
                            clickedBus.value?.item?.endinglong ?: 77.2500f
                        ),
                        title = "End",
                        icon = painterResource(id = R.drawable.finish),
                        infoWindowAnchor = 0.5f to 0f,
                        anchor = 0.5f to 0.5f
                    )
                }
            }

            // ── Expandable Bottom Sheet ──
            if (showBottomSheet && clickedBus.value != null) {
                ModalBottomSheet(
                    onDismissRequest = { showBottomSheet = false },
                    sheetState = rememberModalBottomSheetState(initialValue = ModalSheetValue.Expanded)
                ) {
                    ModernBusDetailBottomSheet(
                        bus = clickedBus.value!!,
                        onClose = { showBottomSheet = false },
                        onNavigate = { navigator.navigateToBusDetails(clickedBus.value!!) }
                    )
                }
            }
        }
    }
}

@Composable
fun ModernBusDetailBottomSheet(
    bus: RealtimeDB,
    onClose: () -> Unit,
    onNavigate: () -> Unit
) {
    val fuelColor = when (bus.item?.fueltype) {
        "EV" -> Indigo600
        "DIESEL" -> Amber500
        else -> Emerald500
    }
    var expanded by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.6f)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // ── Header ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "${bus.item?.busNum} - ${bus.item?.busName}",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Charcoal900
                )
                Text(
                    text = "${bus.item?.start} → ${bus.item?.destination}",
                    fontSize = 14.sp,
                    color = Charcoal900.copy(alpha = 0.5f)
                )
            }

            IconButton(onClick = onClose) {
                Icon(
                    imageVector = androidx.compose.material.icons.filled.Close,
                    contentDescription = "Close",
                    tint = Charcoal900.copy(alpha = 0.5f)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ── Key Stats Row ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatChip(
                label = "Speed",
                value = "${bus.item?.speedKmh?.toInt() ?: 0} km/h",
                icon = androidx.compose.material.icons.filled.DirectionsRun,
                color = Indigo600
            )
            StatChip(
                label = "ETA",
                value = bus.item?.eta ?: "—",
                icon = androidx.compose.material.icons.filled.Schedule,
                color = Emerald500
            )
            StatChip(
                label = "Accuracy",
                value = "±${bus.item?.accuracyM?.toInt() ?: 0}m",
                icon = androidx.compose.material.icons.filled.LocationOn,
                color = Amber500
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ── Fuel Type Badge ──
        TransitBadge(
            mode = when (bus.item?.fueltype) {
                "EV" -> ChipState.Mode.EV
                "DIESEL" -> ChipState.Mode.DIESEL
                else -> ChipState.Mode.Other
            },
            glow = true
        )

        Spacer(modifier = Modifier.height(20.dp))

        // ── Route Timeline ──
        Text(
            text = "Route Timeline",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Charcoal900
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.Top,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StopTimelineItem(
                position = "Start",
                distance = "0 km",
                indicatorColor = Indigo600,
                isCurrent = true,
                isFirst = true
            )
            StopTimelineItem(
                position = "Stop 2",
                distance = "2.1 km",
                indicatorColor = Emerald500,
                isCurrent = false,
                isFirst = false
            )
            StopTimelineItem(
                position = "Stop 3",
                distance = "4.5 km",
                indicatorColor = Amber500,
                isCurrent = false,
                isFirst = false
            )
            StopTimelineItem(
                position = "End",
                distance = "6.2 km",
                indicatorColor = Charcoal900.copy(alpha = 0.3f),
                isCurrent = false,
                isFirst = false,
                isLast = true
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── Driver Profile Card ──
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = 2.dp,
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.colors(containerColor = Surface0.copy(alpha = 0.8f))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                contentPadding = PaddingValues(16.dp)
            ) {
                // Driver avatar
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(fuelColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = bus.item?.driverprofilepic,
                        contentDescription = "Driver profile",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(48.dp).clip(CircleShape)
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.Start,
                    contentPadding = PaddingValues(start = 12.dp)
                ) {
                    Text(
                        text = "Driver: ${bus.item?.driverName ?: "—"}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Semibold,
                        color = Charcoal900
                    )
                    Text(
                        text = "Bus ${bus.item?.busNum ?: "—"} • ${bus.item?.fueltype ?: ""}",
                        fontSize = 12.sp,
                        color = Charcoal900.copy(alpha = 0.5f)
                    )
                }

                // SOS Button
                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.End
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color.Red, shape = RoundedCornerShape(12.dp))
                            .clickable(onClick = { /* trigger SOS */ }),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = androidx.compose.material.icons.filled.Emergency,
                            contentDescription = "SOS",
                            tint = Surface0
                        )
                    }
                    Text(
                        text = "SOS",
                        fontSize = 10.sp,
                        color = Color.Red,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── Actions ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = onNavigate,
                modifier = Modifier.weight(1f),
                colors = androidx.compose.material3.OutlinedButtonDefaults.outlinedButtonColors(
                    containerColor = Charcoal900.copy(alpha = 0.05f),
                    borderColor = Indigo600
                )
            ) {
                Text(
                    text = "Track Live",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Semibold,
                    color = Indigo600
                )
            }

            ElevatedButton(
                onClick = { /* share live trip */ },
                modifier = Modifier.weight(1f),
                colors = androidx.compose.material3.ButtonDefaults.elevatedButtonColors(
                    containerColor = Indigo600
                )
            ) {
                Text(
                    text = "Share Trip",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Semibold,
                    color = OnIndigo50
                )
            }
        }
    }
}

@Composable
fun StatChip(
    label: String,
    value: String,
    icon: androidx.compose.material3.ImageVector,
    color: Color
) {
    Column(
        modifier = Modifier
            .weight(1f)
            .padding(vertical = 8.dp)
            .background(
                Brush.verticalGradient(
                    0f to Surface0.copy(alpha = 0.7f),
                    1f to Surface0.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(12.dp)
            )
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Charcoal900
        )
        Text(
            text = label,
            fontSize = 10.sp,
            color = Charcoal900.copy(alpha = 0.5f)
        )
    }
}

@Composable
fun StopTimelineItem(
    position: String,
    distance: String,
    indicatorColor: Color,
    isCurrent: Boolean,
    isFirst: Boolean = false,
    isLast: Boolean = false
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Timeline line
        if (!isFirst) {
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(24.dp)
                    .background(indicatorColor.copy(alpha = 0.3f))
            )
        }

        // Indicator dot
        Box(
            modifier = Modifier
                .size(if (isCurrent) 14.dp else 10.dp)
                .clip(CircleShape)
                .background(indicatorColor)
                .border(
                    if (isCurrent) 2.dp else 1.dp,
                    Surface0,
                    CircleShape
                )
                .scale(if (isCurrent) 1.1f else 1f),
            contentAlignment = Alignment.Center
        ) {
            if (isCurrent) {
                Icon(
                    imageVector = androidx.compose.material.icons.filled.LocationOn,
                    contentDescription = null,
                    tint = Surface0,
                    modifier = Modifier.size(8.dp)
                )
            }
        }

        // Position label
        Text(
            text = position,
            fontSize = 11.sp,
            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
            color = if (isCurrent) indicatorColor else Charcoal900.copy(alpha = 0.6f)
        )

        // Distance
        Text(
            text = distance,
            fontSize = 10.sp,
            color = Charcoal900.copy(alpha = 0.4f)
        )

        // Timeline line
        if (!isLast) {
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(24.dp)
                    .background(indicatorColor.copy(alpha = 0.3f))
            )
        }
    }
}

fun calculateRotation(bus: RealtimeDB): Float {
    val currentLat = bus.item?.latitude ?: 0f
    val currentLng = bus.item?.longitude ?: 0f
    val nextLat = bus.item?.startinglat ?: currentLat
    val nextLng = bus.item?.startinglong ?: currentLng

    val dLon = nextLng - currentLng
    val lat1 = Math.toRadians(currentLat.toDouble())
    val lat2 = Math.toRadians(nextLat.toDouble())
    val y = dLon * Math.cos(lat2)
    val x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(dLon)
    var bearing = Math.atan2(y, x)
    return bearing.coerceIn(-180f..180f)
}

fun generateRoutePoints(): List<com.google.android.gms.maps.model.LatLng> {
    return listOf(
        com.google.android.gms.maps.model.LatLng(28.6139, 77.2090),
        com.google.android.gms.maps.model.LatLng(28.6150, 77.2110),
        com.google.android.gms.maps.model.LatLng(28.6170, 77.2130),
        com.google.android.gms.maps.model.LatLng(28.6190, 77.2150),
        com.google.android.gms.maps.model.LatLng(28.6210, 77.2170),
        com.google.android.gms.maps.model.LatLng(28.6230, 77.2190)
    )
}