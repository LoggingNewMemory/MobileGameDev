package amba.dojo.pulse.ui.theme

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ArenasScreen() {
    var selectedMode by remember { mutableStateOf("venue") } // "venue" or "home"
    var isInitializing by remember { mutableStateOf(false) }
    var buttonText by remember { mutableStateOf("Mulai Panduan AR") }
    val coroutineScope = rememberCoroutineScope()

    // Warna kustom berdasarkan tema HTML Anda
    val surfaceColor = Color(0xFF131314)
    val surfaceContainerLow = Color(0xFF1b1b1c)
    val surfaceContainer = Color(0xFF1f2020)
    val surfaceContainerHigh = Color(0xFF2a2a2a)
    val surfaceContainerHighest = Color(0xFF353535)
    val surfaceContainerLowest = Color(0xFF0e0e0f)

    val primaryColor = Color(0xFFfab4b8)
    val primaryContainer = Color(0xFF381015)
    val secondaryColor = Color(0xFFf5b6be)
    val secondaryContainer = Color(0xFF693c43)
    val onSecondary = Color(0xFF4c242b)
    val onSurface = Color(0xFFe4e2e2)
    val onSurfaceVariant = Color(0xFFd6c2c2)
    val errorColor = Color(0xFFffb4ab)

    Scaffold(
        topBar = {
            // Header
            Surface(
                color = surfaceColor.copy(alpha = 0.8f),
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
            ) {
                Row(
                    modifier = Modifier
                        .height(64.dp)
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(modifier = Modifier.size(8.dp).background(primaryColor))
                        Text(
                            text = "ARENAS",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = onSurface,
                            letterSpacing = (-0.5).sp
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(onClick = { /* TODO: Notifications */ }) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Notifications",
                                tint = onSurfaceVariant
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(primaryColor, shape = androidx.compose.foundation.shape.CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Profile",
                                tint = Color(0xFF4f2227),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            // Bottom Navigation Bar
            Surface(
                color = surfaceColor.copy(alpha = 0.9f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BottomNavItem(icon = Icons.Default.GridView, label = "Dashboard", isSelected = false, onSurfaceVariant)
                    BottomNavItem(icon = Icons.Default.SportsKabaddi, label = "Arenas", isSelected = true, primaryColor)
                    // Training Button Special Icon Container
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.Timer, contentDescription = "Training", tint = primaryColor, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(text = "Training", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = primaryColor)
                    }
                    BottomNavItem(icon = Icons.Default.Assessment, label = "History", isSelected = false, onSurfaceVariant)
                    BottomNavItem(icon = Icons.Default.Person, label = "Profile", isSelected = false, onSurfaceVariant)
                }
            }
        },
        containerColor = surfaceColor
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Sub-header context & Arena telemetry badge
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(modifier = Modifier.size(6.dp).background(primaryColor))
                        Text(
                            text = "LOC.ID // DOJO-902",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = onSurfaceVariant,
                            letterSpacing = 1.2.sp
                        )
                    }
                    Row(
                        modifier = Modifier
                            .background(surfaceContainerHigh)
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(modifier = Modifier.size(6.dp).background(secondaryColor))
                        Text(
                            text = "LINK OK",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = secondaryColor
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(surfaceContainerLow)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Designated Tactical Venue",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFb27579),
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        text = "Campus Martial Arts Hall",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = onSurface
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = "Pin",
                            tint = primaryColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Tower B, Level 2 Combat Lab",
                            fontSize = 12.sp,
                            color = onSurfaceVariant
                        )
                    }
                }
            }

            // Mode Selector: Segmented Split-Cell Toggle Matrix
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Telemetry Mode",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = onSurfaceVariant,
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        text = if (selectedMode == "venue") "Active: Venue HUD" else "Active: Standalone Cam",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryColor
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(surfaceContainerLowest)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Button(
                        onClick = { selectedMode = "venue" },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedMode == "venue") secondaryContainer else Color.Transparent
                        ),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(imageVector = Icons.Default.Visibility, contentDescription = null, tint = if (selectedMode == "venue") onSecondary else onSurfaceVariant, modifier = Modifier.size(18.dp))
                            Text(text = "Venue (AR)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (selectedMode == "venue") onSecondary else onSurfaceVariant)
                        }
                    }
                    Button(
                        onClick = { selectedMode = "home" },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedMode == "home") secondaryContainer else Color.Transparent
                        ),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(imageVector = Icons.Default.Home, contentDescription = null, tint = if (selectedMode == "home") onSecondary else onSurfaceVariant, modifier = Modifier.size(18.dp))
                            Text(text = "Home Mode", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (selectedMode == "home") onSecondary else onSurfaceVariant)
                        }
                    }
                }
            }

            // Stations List
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Available Stations (03)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = onSurfaceVariant, letterSpacing = 0.8.sp)
                    Text(text = "Tap to Lock", fontSize = 10.sp, color = onSurfaceVariant)
                }

                // Station 1: In Use
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(surfaceContainerLow)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "[STN.01]", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = onSurfaceVariant)
                            Text(text = "Platform A", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = onSurfaceVariant)
                        }
                        Row(
                            modifier = Modifier.background(surfaceContainerHighest).padding(horizontal = 8.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(modifier = Modifier.size(6.dp).background(errorColor))
                            Text(text = "In Use", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = onSurface)
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column {
                            Text(text = "Speed Bag Platform", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = onSurface)
                            Text(text = "Biometric Cadence Tracker Active", fontSize = 12.sp, color = onSurfaceVariant)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(text = "Occupied", fontSize = 10.sp, color = onSurfaceVariant)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(imageVector = Icons.Default.HourglassBottom, contentDescription = null, tint = errorColor, modifier = Modifier.size(14.dp))
                                Text(text = "08:42", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = errorColor)
                            }
                        }
                    }
                }

                // Station 2: Selected (Highlight State)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(primaryContainer)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "[STN.02]", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = primaryColor)
                            Text(text = "Optical Grid A2", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = primaryColor)
                        }
                        Row(
                            modifier = Modifier.background(secondaryContainer).padding(horizontal = 8.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(modifier = Modifier.size(6.dp).background(primaryColor))
                            Text(text = "Dipilih", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFe5a9b0))
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Heavy Bag & Stance Mat", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = primaryColor)
                            Text(text = "Ready for Vision AI tracking", fontSize = 12.sp, color = onSurface)
                        }
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(secondaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = primaryColor)
                        }
                    }
                    // Reticle status indicators
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .background(surfaceContainerLowest)
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(imageVector = Icons.Default.CenterFocusStrong, contentDescription = null, tint = primaryColor, modifier = Modifier.size(14.dp))
                            Text(text = "FOV: Calibrated", fontSize = 10.sp, color = onSurface)
                        }
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .background(surfaceContainerLowest)
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Sensors, contentDescription = null, tint = primaryColor, modifier = Modifier.size(14.dp))
                            Text(text = "Pressure Mat: Live", fontSize = 10.sp, color = onSurface)
                        }
                    }
                }

                // Station 3: Available
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(surfaceContainer)
                        .clickable { }
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "[STN.03]", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = onSurfaceVariant)
                            Text(text = "Zone Center", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = onSurface)
                        }
                        Row(
                            modifier = Modifier.background(surfaceContainerHighest).padding(horizontal = 8.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(modifier = Modifier.size(6.dp).background(primaryColor))
                            Text(text = "Tersedia", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = primaryColor)
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column {
                            Text(text = "Open Floor Ring", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = onSurface)
                            Text(text = "Sensor Calibrated // Footwork Sparring", fontSize = 12.sp, color = onSurfaceVariant)
                        }
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(surfaceContainerLowest),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.RadioButtonUnchecked, contentDescription = null, tint = onSurfaceVariant, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }

            // Environmental Diagnostics Strip
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(surfaceContainerLow)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Optical Environment Check", fontSize = 10.sp, color = onSurfaceVariant)
                    Text(text = "Lux: Optimal", fontSize = 10.sp, color = primaryColor)
                }
                // Progress Bars Segment
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Box(modifier = Modifier.weight(1f).fillMaxHeight().background(secondaryColor))
                    Box(modifier = Modifier.weight(1f).fillMaxHeight().background(secondaryColor))
                    Box(modifier = Modifier.weight(1f).fillMaxHeight().background(secondaryColor))
                    Box(modifier = Modifier.weight(1f).fillMaxHeight().background(secondaryColor))
                    Box(modifier = Modifier.weight(1f).fillMaxHeight().background(surfaceContainerHighest))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Spatial Anchor Confidence", fontSize = 10.sp, color = onSurfaceVariant)
                    Text(text = "94.8%", fontSize = 10.sp, color = onSurface)
                }
            }

            // Sticky Bottom Trigger Section
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Button(
                    onClick = {
                        if (!isInitializing) {
                            isInitializing = true
                            buttonText = "Menginisialisasi AR..."
                            coroutineScope.launch {
                                delay(1200)
                                isInitializing = false
                                buttonText = "Ready - Masuk Ring"
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = secondaryContainer),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(0.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(imageVector = Icons.Default.Visibility, contentDescription = null, tint = onSecondary)
                        Text(
                            text = if (selectedMode == "venue") buttonText else "Mulai Home Session",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = onSecondary
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.LockClock, contentDescription = null, tint = onSurfaceVariant, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Station 2 Mat Locked for 10:00 Mins",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun BottomNavItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, isSelected: Boolean, tintColor: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { }
            .padding(horizontal = 8.dp)
    ) {
        Icon(imageVector = icon, contentDescription = label, tint = tintColor, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = tintColor
        )
    }
}