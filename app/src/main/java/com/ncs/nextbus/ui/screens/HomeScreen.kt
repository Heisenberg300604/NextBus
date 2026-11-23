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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.keyboard.ImeAction
import androidx.compose.ui.input.keyboard.KeyboardOptions
import androidx.compose.ui.input.keyboard.KeyboardType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.ncs.nextbus.model.RealtimeDB
import com.ncs.nextbus.ui.components.*
import com.ncs.nextbus.ui.theme.*
import com.ncs.nextbus.ui.theme.Charcoal900
import com.ncs.nextbus.ui.theme.Indigo600
import com.ncs.nextbus.ui.theme.Emerald500
import com.ncs.nextbus.ui.theme.OnIndigo50
import com.ncs.nextbus.ui.theme.OnCharcoal50
import com.ncs.nextbus.ui.theme.Surface0

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    navigator: NavHostNavigator = hiltViewModel()
) {
    val localContext = LocalContext.current
    var departureText by remember { mutableStateOf("Current Location") }
    var destinationText by remember { mutableStateOf("") }
    var showSearchSheet by remember { mutableStateOf(false) }
    var showFavoritesSheet by remember { mutableStateOf(false) }

    // Sample nearby buses for demonstration
    val nearbyBuses = remember {
        mutableStateOf<List<RealtimeDB>>(emptyList())
    }

    // Favorite routes
    val favoriteRoutes = remember { mutableStateOf<List<String>>(emptyList()) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        backgroundColor = Color.Transparent,
        containerColor = Color.Transparent
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.Start
        ) {
            // ── Header ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                contentPadding = PaddingValues(top = 8.dp)
            ) {
                Text(
                    text = "Good Morning",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = Charcoal900.copy(alpha = 0.7f)
                )
                IconButton(onClick = { /* profile */ }) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.verticalGradient(
                                    0f to Indigo600,
                                    1f to Indigo600.copy(alpha = 0.8f)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = androidx.compose.material.icons.filled.Person,
                            contentDescription = "Profile",
                            tint = OnIndigo50
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── Smart Search Card ──
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                SmartOriginDestinationInput(
                    departure = departureText,
                    destination = destinationText,
                    onDepartureChange = { departureText = it },
                    onDestinationChange = { destinationText = it },
                    onSwap = { /* spring animation swap */ },
                    onSetCurrentLocation = { departureText = "Current Location" },
                    onSearch = { showSearchSheet = true },
                    searchEnabled = true
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Live Nearby Section ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Live Nearby",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Charcoal900
                )
                Text(
                    text = "All active",
                    fontSize = 12.sp,
                    color = Charcoal900.copy(alpha = 0.5f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Horizontal carousel of nearby bus cards
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.Center,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Sample bus cards
                LiveTransitCard(
                    busNumber = "14A",
                    routeName = "College Express",
                    etaMinutes = 3,
                    fuelType = "EV",
                    onClick = { /* navigate to bus details */ }
                )
                LiveTransitCard(
                    busNumber = "7B",
                    routeName = "Railway Shuttle",
                    etaMinutes = 7,
                    fuelType = "DIESEL",
                    onClick = { /* navigate to bus details */ }
                )
                LiveTransitCard(
                    busNumber = "22C",
                    routeName = "North Campus Loop",
                    etaMinutes = 12,
                    fuelType = "EV",
                    onClick = { /* navigate to bus details */ }
                )
                LiveTransitCard(
                    busNumber = "5D",
                    routeName = "City Center Express",
                    etaMinutes = 5,
                    fuelType = "DIESEL",
                    onClick = { /* navigate to bus details */ }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Quick Favorites Section ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Favorite Routes",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Charcoal900
                )
                TextButton(
                    onClick = { showFavoritesSheet = true },
                    colors = androidx.compose.material3.TextButtonDefaults.textColors(containerColor = Indigo600)
                ) {
                    Text(text = "View All")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (favoriteRoutes.value.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(favoriteRoutes.value) { route ->
                        PillChip(text = route, selected = true) {}
                    }
                }
            } else {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = androidx.compose.material.icons.filled.StarBorder,
                            contentDescription = null,
                            tint = Charcoal900.copy(alpha = 0.3f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "No favorite routes yet",
                            fontSize = 14.sp,
                            color = Charcoal900.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Quick Action Buttons ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                QuickActionButton(
                    icon = androidx.compose.material.icons.filled.Map,
                    label = "Live Map",
                    color = Indigo600,
                    onClick = { navigator.navigateToMap() }
                )
                QuickActionButton(
                    icon = androidx.compose.material.icons.filled.History,
                    label = "History",
                    color = Emerald500,
                    onClick = { /* show history */ }
                )
                QuickActionButton(
                    icon = androidx.compose.material.icons.filled.Favorite,
                    label = "Favorites",
                    color = Amber500,
                    onClick = { showFavoritesSheet = true }
                )
                QuickActionButton(
                    icon = androidx.compose.material.icons.filled.Emergency,
                    label = "SOS",
                    color = Color.Red,
                    onClick = { /* trigger SOS */ }
                )
            }

            Spacer(modifier = Modifier.height(40.dp))
        }

        // Bottom navigation
        BottomNavigation(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = Surface0.copy(alpha = 0.9f),
            content = {
                BottomNavigationItem(
                    icon = {
                        Icon(
                            imageVector = androidx.compose.material.icons.filled.Home,
                            contentDescription = "Home",
                            tint = Indigo600
                        )
                    },
                    label = { Text("Home") },
                    selected = true,
                    onClick = { navigator.navigateToHome() }
                )
                BottomNavigationItem(
                    icon = {
                        Icon(
                            imageVector = androidx.compose.material.icons.filled.Map,
                            contentDescription = "Map",
                            tint = Charcoal900.copy(alpha = 0.4f)
                        )
                    },
                    label = { Text("Map") },
                    selected = false,
                    onClick = { navigator.navigateToMap() }
                )
                BottomNavigationItem(
                    icon = {
                        Icon(
                            imageVector = androidx.compose.material.icons.filled.Person,
                            contentDescription = "Profile",
                            tint = Charcoal900.copy(alpha = 0.4f)
                        )
                    },
                    label = { Text("Profile") },
                    selected = false,
                    onClick = { navigator.navigateToProfile() }
                )
            }
        )
    }

    // ── Search Results Sheet ──
    if (showSearchSheet) {
        SearchResultsBottomSheet(
            onClose = { showSearchSheet = false },
            onBusSelect = { bus ->
                showSearchSheet = false
                navigator.navigateToBusDetails(bus)
            }
        )
    }

    // ── Favorites Sheet ──
    if (showFavoritesSheet) {
        FavoritesBottomSheet(
            onClose = { showFavoritesSheet = false }
        )
    }
}

@Composable
fun QuickActionButton(
    icon: androidx.compose.material3.ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    val hover by remember { mutableStateOf(false) }
    val animatedScale by animateFloatAsState(
        targetValue = if (hover) 1.05f else 1f,
        animationSpec = SpringSpec(stiffness = 400f, dampingRatio = 20f)
    )

    Column(
        modifier = Modifier
            .scale(animatedScale)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .background(color.copy(alpha = 0.1f), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = Charcoal900.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun SearchResultsBottomSheet(
    onClose: () -> Unit,
    onBusSelect: (RealtimeDB) -> Unit
) {
    val sampleBuses = listOf(
        RealtimeDB(
            item = RealtimeDB.locationData(
                busNum = "14A",
                busName = "College Express",
                start = "North Campus",
                destination = "City Center",
                fueltype = "EV",
                nextDeparture = "3 min",
                eta = "3 min",
                speedKmh = 42,
                accuracyM = 5,
                passengerCount = 24,
                crowdLevel = "Medium",
                driverName = "Rajesh Kumar",
                driverprofilepic = 0,
                startinglat = 28.6139f,
                startinglong = 77.2090f,
                endinglat = 28.6500f,
                endinglong = 77.2500f,
                fareEstimate = 25
            ),
            key = "bus-1"
        ),
        RealtimeDB(
            item = RealtimeDB.locationData(
                busNum = "7B",
                busName = "Railway Shuttle",
                start = "Railway Station",
                destination = "North Campus",
                fueltype = "DIESEL",
                nextDeparture = "7 min",
                eta = "7 min",
                speedKmh = 38,
                accuracyM = 8,
                passengerCount = 45,
                crowdLevel = "High",
                driverName = "Amit Singh",
                driverprofilepic = 0,
                startinglat = 28.6200f,
                startinglong = 77.2150f,
                endinglat = 28.6300f,
                endinglong = 77.2200f,
                fareEstimate = 20
            ),
            key = "bus-2"
        )
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Charcoal900.copy(alpha = 0.5f)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .height(0.65f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.colors(containerColor = Surface0)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Handle bar
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .height(4.dp)
                        .background(Charcoal900.copy(alpha = 0.2f), RoundedCornerShape(2.dp))
                        .padding(top = 12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Available Buses",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Charcoal900
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Choose your bus for live tracking",
                    fontSize = 14.sp,
                    color = Charcoal900.copy(alpha = 0.6f)
                )

                Spacer(modifier = Modifier.height(20.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    items = sampleBuses
                ) { bus ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .background(Charcoal900.copy(alpha = 0.02f), RoundedCornerShape(12.dp))
                            .padding(vertical = 4.dp)
                            .clickable(onClick = { onBusSelect(bus) }),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        contentPadding = PaddingValues(12.dp)
                    ) {
                        Column(
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(
                                text = "${bus.item?.busNum} - ${bus.item?.busName}",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Semibold,
                                color = Charcoal900
                            )
                            Text(
                                text = "${bus.item?.start} → ${bus.item?.destination}",
                                fontSize = 12.sp,
                                color = Charcoal900.copy(alpha = 0.5f),
                                maxLines = 1
                            )
                        }

                        Column(
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = "Next: ${bus.item?.nextDeparture}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Indigo600
                            )
                            TransitBadge(
                                mode = when (bus.item?.fueltype) {
                                    "EV" -> ChipState.Mode.EV
                                    "DIESEL" -> ChipState.Mode.DIESEL
                                    else -> ChipState.Mode.Other
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                TextButton(
                    onClick = onClose,
                    modifier = Modifier.fillMaxWidth(),
                    colors = androidx.compose.material3.TextButtonDefaults.textColors(containerColor = Indigo600)
                ) {
                    Text(text = "Close", fontSize = 14.sp, fontWeight = FontWeight.Semibold)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun FavoritesBottomSheet(
    onClose: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Charcoal900.copy(alpha = 0.5f)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .height(0.5f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.colors(containerColor = Surface0)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .height(4.dp)
                        .background(Charcoal900.copy(alpha = 0.2f), RoundedCornerShape(2.dp))
                        .padding(top = 12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Favorite Routes",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Charcoal900
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    items = listOf("College Express #14", "Railway Shuttle #7B", "North Campus Loop #22C", "City Center Express #5D")
                ) { route ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .clickable(onClick = {}),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        contentPadding = PaddingValues(12.dp)
                    ) {
                        Text(
                            text = route,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Semibold,
                            color = Charcoal900
                        )
                        Icon(
                            imageVector = androidx.compose.material.icons.filled.Star,
                            contentDescription = null,
                            tint = Amber500
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                TextButton(
                    onClick = onClose,
                    modifier = Modifier.fillMaxWidth(),
                    colors = androidx.compose.material3.TextButtonDefaults.textColors(containerColor = Indigo600)
                ) {
                    Text(text = "Done", fontSize = 14.sp, fontWeight = FontWeight.Semibold)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}