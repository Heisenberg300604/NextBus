package com.ncs.nextbus.ui.screens

import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.ncs.nextbus.ui.theme.Amber500
import com.ncs.nextbus.ui.theme.OnIndigo50
import com.ncs.nextbus.ui.theme.OnCharcoal50
import com.ncs.nextbus.ui.theme.Surface0

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = hiltViewModel(),
    navigator: NavHostNavigator = hiltViewModel()
) {
    val localContext = LocalContext.current
    var selectedLanguage by remember { mutableStateOf("English") }
    var showSOSSheet by remember { mutableStateOf(false) }
    var showLanguageMenu by remember { mutableStateOf(false) }

    val userName = "Rahul Sharma"
    val userEmail = "rahul.sharma@edu.example.com"
    val totalTrips = 47
    val favoritedRoutes = 12
    val memberSince = "Jan 2023"

    val travelHistory = remember {
        mutableStateOf<List<TripHistoryItem>>(listOf(
            TripHistoryItem(
                routeName = "College Express #14",
                start = "North Campus",
                destination = "City Center",
                date = "Mar 20, 2024",
                distance = "8.5 km",
                duration = "22 min",
                fuelType = "EV"
            ),
            TripHistoryItem(
                routeName = "Railway Station Shuttle",
                start = "Hotel",
                destination = "Railway Station",
                date = "Mar 18, 2024",
                distance = "3.2 km",
                duration = "10 min",
                fuelType = "Diesel"
            ),
            TripHistoryItem(
                routeName = "North Campus Loop",
                start = "Main Gate",
                destination = "Sports Complex",
                date = "Mar 15, 2024",
                distance = "5.0 km",
                duration = "15 min",
                fuelType = "EV"
            )
        ))
    }

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
            // ── Profile Header ──
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                tint = Surface0.copy(alpha = 0.05f),
                borderColor = Charcoal900.copy(alpha = 0.1f),
                contentPadding = PaddingValues(0.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                Brush.verticalGradient(
                                    0f to Indigo600,
                                    1f to Indigo600.copy(alpha = 0.8f)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = 0,
                            contentDescription = "User profile",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(64.dp).clip(RoundedCornerShape(12.dp))
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = userName,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Charcoal900
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = userEmail,
                        fontSize = 14.sp,
                        color = Charcoal900.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Stats Row ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ProfileStatChip(
                    label = "Total Trips",
                    value = "$totalTrips",
                    icon = androidx.compose.material.icons.filled.DirectionsBus,
                    color = Indigo600
                )
                ProfileStatChip(
                    label = "Favorites",
                    value = "$favoritedRoutes",
                    icon = androidx.compose.material.icons.filled.Favorite,
                    color = Amber500
                )
                ProfileStatChip(
                    label = "Member Since",
                    value = memberSince,
                    icon = androidx.compose.material.icons.filled.CalendarToday,
                    color = Emerald500
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Language Switcher ──
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                tint = Surface0.copy(alpha = 0.05f)
            ) {
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
                            text = "Language",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Semibold,
                            color = Charcoal900
                        )
                        Text(
                            text = "Instant toggle • No restart",
                            fontSize = 12.sp,
                            color = Charcoal900.copy(alpha = 0.5f)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        PillChip(
                            text = "English",
                            selected = selectedLanguage == "English",
                            onClick = { selectedLanguage = "English" }
                        )
                        PillChip(
                            text = "हिंदी",
                            selected = selectedLanguage == "हिंदी",
                            onClick = { selectedLanguage = "हिंदी" }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Travel History ──
            Text(
                text = "Travel History",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Charcoal900
            )

            if (travelHistory.value.isEmpty()) {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = androidx.compose.material.icons.filled.History,
                            contentDescription = null,
                            tint = Charcoal900.copy(alpha = 0.3f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "No travel history yet",
                            fontSize = 14.sp,
                            color = Charcoal900.copy(alpha = 0.5f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    items = travelHistory.value,
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) { trip ->
                    ModernTripHistoryCard(
                        trip = trip,
                        onClick = { /* track route */ }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Safety Section ──
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = 2.dp,
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.colors(
                    containerColor = Color.Red.copy(alpha = 0.05f)
                )
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Red.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = androidx.compose.material.icons.filled.Emergency,
                            contentDescription = null,
                            tint = Color.Red
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Safety & SOS",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Red
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Share live trip with family & emergency contacts",
                        fontSize = 12.sp,
                        color = Charcoal900.copy(alpha = 0.6f),
                        textAlign = androidx.compose.ui.text.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    ElevatedButton(
                        onClick = { showSOSSheet = true },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        colors = androidx.compose.material3.ButtonDefaults.elevatedButtonColors(
                            containerColor = Color.Red
                        )
                    ) {
                        Text(
                            text = "Share Live Trip Link",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Semibold,
                            color = Surface0
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { /* logout */ },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        colors = androidx.compose.material3.OutlinedButtonDefaults.outlinedButtonColors(
                            containerColor = Color.Red.copy(alpha = 0.05f),
                            borderColor = Color.Red
                        )
                    ) {
                        Text(
                            text = "Log Out",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Semibold,
                            color = Color.Red
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── App Version ──
            Text(
                text = "NextBus v2.0.0 • Build 2024.03.21",
                fontSize = 11.sp,
                color = Charcoal900.copy(alpha: 0.4f),
                style = androidx.compose.material3.MaterialTheme.typography.labelSmall
            )
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
                            tint = Charcoal900.copy(alpha = 0.4f)
                        )
                    },
                    label = { Text("Home") },
                    selected = false,
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
                            tint = Indigo600
                        )
                    },
                    label = { Text("Profile") },
                    selected = true,
                    onClick = { /* already on profile */ }
                )
            }
        )
    }

    // ── SOS Sheet ──
    if (showSOSSheet) {
        SOSBottomSheet(
            onClose = { showSOSSheet = false }
        )
    }
}

@Composable
fun ProfileStatChip(
    label: String,
    value: String,
    icon: androidx.compose.material3.ImageVector,
    color: Color
) {
    Column(
        modifier = Modifier
            .weight(1f)
            .padding(vertical = 12.dp)
            .background(
                Brush.verticalGradient(
                    0f to Surface0.copy(alpha = 0.9f),
                    1f to Surface0.copy(alpha = 0.7f)
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .border(1.5.dp, color.copy(alpha = 0.2f), RoundedCornerShape(16.dp)),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Charcoal900
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = Charcoal900.copy(alpha = 0.5f)
        )
    }
}

@Composable
fun ModernTripHistoryCard(
    trip: TripHistoryItem,
    onClick: () -> Unit
) {
    val fuelColor = when (trip.fuelType) {
        "EV" -> Indigo600
        "Diesel" -> Amber500
        else -> Emerald500
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 1.dp,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.colors(containerColor = Surface0.copy(alpha = 0.8f))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                contentPadding = PaddingValues(16.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = trip.routeName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Semibold,
                        color = Charcoal900
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${trip.start} → ${trip.destination}",
                            fontSize = 13.sp,
                            color = Charcoal900.copy(alpha = 0.6f)
                        )
                        Text(
                            text = "${trip.distance} • ${trip.duration}",
                            fontSize = 12.sp,
                            color = Charcoal900.copy(alpha = 0.5f)
                        )
                    }
                }

                TransitBadge(
                    mode = when (trip.fuelType) {
                        "EV" -> ChipState.Mode.EV
                        "Diesel" -> ChipState.Mode.DIESEL
                        else -> ChipState.Mode.Other
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                colors = androidx.compose.material3.OutlinedButtonDefaults.outlinedButtonColors(
                    containerColor = Indigo600.copy(alpha = 0.05f),
                    borderColor = Indigo600
                )
            ) {
                Text(
                    text = "Track this route",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Semibold,
                    color = Indigo600
                )
            }
        }
    }
}

@Composable
fun SOSBottomSheet(
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
                .height(0.45f),
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

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Red.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = androidx.compose.material.icons.filled.Emergency,
                        contentDescription = null,
                        tint = Color.Red
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Live Trip Sharing",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Charcoal900
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Your live trip location is being shared with\nfamily and emergency contacts.",
                    fontSize = 14.sp,
                    color = Charcoal900.copy(alpha: 0.6f),
                    textAlign = androidx.compose.ui.text.TextAlign.Center,
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onClose,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text(
                        text = "Stop Sharing",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Semibold,
                        color = Surface0
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(
                    onClick = onClose,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    colors = androidx.compose.material3.OutlinedButtonDefaults.outlinedButtonColors(
                        containerColor = Charcoal900.copy(alpha = 0.05f),
                        borderColor = Charcoal900
                    )
                ) {
                    Text(
                        text = "Close",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Semibold,
                        color = Charcoal900
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}