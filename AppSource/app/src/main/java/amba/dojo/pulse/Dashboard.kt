package amba.dojo.pulse

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import amba.dojo.pulse.ui.theme.DojoPulseTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardPage(
    onLogout: () -> Unit = {},
    onStartTraining: () -> Unit = {},
    onNavigateTutorial: () -> Unit = {}
) {
    val backgroundColor = Color(0xFF121212)
    val surfaceColor = Color(0xFF1E1E1E)
    val accentRed = Color(0xFF4A1525)
    val textPrimary = Color.White
    val textSecondary = Color.LightGray
    val textMuted = Color.Gray

    Scaffold(
        containerColor = backgroundColor,
        bottomBar = { DashboardBottomNav(onStartTraining) },
        topBar = { DashboardTopBar(onLogout) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            // User Header Profile Card
            UserHeaderCard(surfaceColor, accentRed, textPrimary, textSecondary, textMuted)
            
            Spacer(modifier = Modifier.height(16.dp))

            // Main Training Card
            MainTrainingCard(surfaceColor, accentRed, textPrimary, textSecondary, onStartTraining)

            Spacer(modifier = Modifier.height(16.dp))

            // Weekly Telemetry Summary
            WeeklyTelemetrySummary(surfaceColor, textPrimary, textSecondary)

            Spacer(modifier = Modifier.height(16.dp))

            // Discipline Training List
            DisciplineTrainingList(surfaceColor, textPrimary, textSecondary, onNavigateTutorial)
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Quote Section
            Text(
                text = "\"Disiplin bukanlah kecepatan menyerang, melainkan ketepatan kembali ke posisi guard sebelum lawan menyadari ruang.\"\n— DOJO BIOMETRICS CODE // RULE 04",
                color = textSecondary,
                fontSize = 12.sp,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                modifier = Modifier.padding(horizontal = 24.dp),
                textAlign = TextAlign.Left
            )
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardTopBar(onLogout: () -> Unit = {}) {
    TopAppBar(
        title = { 
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(12.dp).background(Color(0xFFFFB3BA)))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "DASHBOARD", 
                    color = Color.White, 
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        },
        actions = {
            IconButton(onClick = { /*TODO*/ }) {
                Icon(Icons.Default.Notifications, contentDescription = "Notifications", tint = Color.LightGray)
            }
            IconButton(onClick = { 
                com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
                onLogout() 
            }) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color(0xFFFFB3BA), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, contentDescription = "Profile", tint = Color.Black, modifier = Modifier.size(20.dp))
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF121212))
    )
}

@Composable
fun UserHeaderCard(surfaceColor: Color, accentRed: Color, textPrimary: Color, textSecondary: Color, textMuted: Color) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(accentRed, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("AC", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Info
            Column(modifier = Modifier.weight(1f)) {
                Text("DOJO PULSE // ID.884", color = textMuted, fontSize = 10.sp, letterSpacing = 1.sp)
                Text("Alex Chen", color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 22.sp)
            }
            
            // Badges
            Row {
                Box(modifier = Modifier.background(Color(0xFF8B4A5B), RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("LVL 4\nSTRIKER", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, lineHeight = 10.sp)
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Box(modifier = Modifier.background(accentRed, RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("5\nHARI", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, lineHeight = 10.sp)
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(8.dp).background(Color(0xFF8B4A5B)))
            Spacer(modifier = Modifier.width(8.dp))
            Text("SENSOR LINK: STABIL (120FPS HUD)", color = textSecondary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.weight(1f))
            Text("SINKRONISASI TEPAT WAKTU", color = Color(0xFFFFB3BA), fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun MainTrainingCard(
    surfaceColor: Color,
    accentRed: Color,
    textPrimary: Color,
    textSecondary: Color,
    onStartTraining: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = surfaceColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.background(accentRed, RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                    Text("LATIHAN UTAMA HARI INI", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("[SYS.DRL-09]", color = textSecondary, fontSize = 10.sp)
                Spacer(modifier = Modifier.weight(1f))
                Icon(Icons.Default.Schedule, contentDescription = null, tint = textSecondary, modifier = Modifier.size(12.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("3:00 MENIT", color = textSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Image Placeholder (A dark box with overlay text)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .background(Color.Black, RoundedCornerShape(8.dp))
            ) {
                // We'd normally use an Image here. For now a placeholder box
                Column(modifier = Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.SpaceBetween) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("PUNCH ANGLE", color = textSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text("142° PRECISION", color = textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("RETURN GUARD", color = textSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text("0.18 DETIK", color = Color(0xFFFFB3BA), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("ORTHODOX GUARD", color = textSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text("VALIDASI SUDUT AKTIF", color = Color(0xFFFFB3BA), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text("STANCE & 1-2 COMBO", color = textPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Pace test terstruktur 3 menit dengan validasi sudut lintasan jab-cross biomekanis dan kecepatan pulih guard.",
                color = textSecondary,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth().background(Color(0xFF161616), RoundedCornerShape(8.dp)).padding(12.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("TARGET PUKULAN", color = textSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text("180 Strikes", color = textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("STANDAR PRESISI", color = textSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text("> 85% Akurat", color = Color(0xFFFFB3BA), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = onStartTraining,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accentRed)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("MULAI LATIHAN", color = Color.White, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }
        }
    }
}

@Composable
fun WeeklyTelemetrySummary(surfaceColor: Color, textPrimary: Color, textSecondary: Color) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(8.dp).background(Color(0xFFFFB3BA)))
            Spacer(modifier = Modifier.width(8.dp))
            Text("RINGKASAN TELEMETRI MINGGUAN", color = textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.weight(1f))
            Text("MINGGU 14", color = textSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Workouts
            Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = surfaceColor)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("WORKOUTS", color = textSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Icon(Icons.Default.FitnessCenter, contentDescription = null, tint = textSecondary, modifier = Modifier.size(12.dp))
                    }
                    Text("6", color = textPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text("Sesi tuntas", color = textSecondary, fontSize = 9.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("TARGET", color = textSecondary, fontSize = 9.sp)
                        Text("7 SESI", color = textPrimary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            
            // Form Score
            Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = surfaceColor)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("FORM SCORE", color = textSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = textSecondary, modifier = Modifier.size(12.dp))
                    }
                    Text("88%", color = Color(0xFFFFB3BA), fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text("Akurat rata2", color = textSecondary, fontSize = 9.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("DELTA", color = textSecondary, fontSize = 9.sp)
                        Text("+4.2%", color = Color(0xFFFFB3BA), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            
            // Mat Time
            Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = surfaceColor)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("MAT TIME", color = textSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Icon(Icons.Default.Timer, contentDescription = null, tint = textSecondary, modifier = Modifier.size(12.dp))
                    }
                    Text("4.2", color = textPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text("Jam aktif", color = textSecondary, fontSize = 9.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("KAL", color = textSecondary, fontSize = 9.sp)
                        Text("1,840", color = textPrimary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun DisciplineTrainingList(
    surfaceColor: Color,
    textPrimary: Color,
    textSecondary: Color,
    onNavigateTutorial: () -> Unit = {}
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(8.dp).background(Color(0xFFFFB3BA)))
            Spacer(modifier = Modifier.width(8.dp))
            Text("DISIPLIN LATIHAN", color = textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.weight(1f))
            Text("PILIH MODUL", color = textSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        DisciplineItem(
            surfaceColor, textPrimary, textSecondary, 
            icon = Icons.Default.SportsMma, 
            title = "Boxing", 
            tag = "FUNDAMENTAL",
            desc = "Lead jab, cross trajectory, bob and weave",
            metrics = "14 DRILL   •   PANDUAN LENGKAP",
            onClick = onNavigateTutorial
        )
        Spacer(modifier = Modifier.height(8.dp))
        DisciplineItem(
            surfaceColor, textPrimary, textSecondary, 
            icon = Icons.Default.SportsMartialArts, 
            title = "Muay Thai", 
            tag = "POWER STRIKES",
            desc = "Rotasi pinggul roundhouse, siku klinch & teep",
            metrics = "9 DRILL   •   PROGRES 45%"
        )
        Spacer(modifier = Modifier.height(8.dp))
        DisciplineItem(
            surfaceColor, textPrimary, textSecondary, 
            icon = Icons.Default.SelfImprovement, 
            title = "Senam Mobility", 
            tag = "PEMULIHAN",
            desc = "Dekompresi panggul, pergelangan kaki, dan...",
            metrics = "6 SESI   •   SIAP MULAI"
        )
    }
}

@Composable
fun DisciplineItem(
    surfaceColor: Color,
    textPrimary: Color,
    textSecondary: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    tag: String,
    desc: String,
    metrics: String,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = surfaceColor)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = Color(0xFFFFB3BA), modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, color = textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier.background(Color(0xFF4A1525), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                        Text(tag, color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(desc, color = textSecondary, fontSize = 10.sp, maxLines = 1)
                Spacer(modifier = Modifier.height(4.dp))
                Text(metrics, color = textPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = textSecondary)
        }
    }
}

@Composable
fun DashboardBottomNav(onStartTraining: () -> Unit = {}) {
    NavigationBar(
        containerColor = Color(0xFF121212),
        contentColor = Color.LightGray
    ) {
        NavigationBarItem(
            selected = true,
            onClick = { /*TODO*/ },
            icon = { Icon(Icons.Default.GridView, contentDescription = "Dashboard") },
            label = { Text("DASHBOARD", fontSize = 8.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFFFFB3BA),
                selectedTextColor = Color(0xFFFFB3BA),
                indicatorColor = Color.Transparent
            )
        )
        NavigationBarItem(
            selected = false,
            onClick = { /*TODO*/ },
            icon = { Icon(Icons.Default.SportsKabaddi, contentDescription = "Arenas") },
            label = { Text("ARENAS", fontSize = 8.sp) }
        )
        NavigationBarItem(
            selected = false,
            onClick = onStartTraining,
            icon = { 
                Box(
                    modifier = Modifier.background(Color(0xFF4A1525), RoundedCornerShape(8.dp)).padding(8.dp)
                ) {
                    Icon(Icons.Default.Timer, contentDescription = "Training", tint = Color.White) 
                }
            },
            label = { Text("TRAINING", fontSize = 8.sp, color = Color(0xFFFFB3BA)) }
        )
        NavigationBarItem(
            selected = false,
            onClick = { /*TODO*/ },
            icon = { Icon(Icons.Default.InsertChartOutlined, contentDescription = "History") },
            label = { Text("HISTORY", fontSize = 8.sp) }
        )
        NavigationBarItem(
            selected = false,
            onClick = { /*TODO*/ },
            icon = { Icon(Icons.Default.PersonOutline, contentDescription = "Profile") },
            label = { Text("PROFILE", fontSize = 8.sp) }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DashboardPreview() {
    DojoPulseTheme {
        DashboardPage()
    }
}
