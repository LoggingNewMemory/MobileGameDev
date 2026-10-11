package amba.dojo.pulse

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
    onNavigateToArenas: () -> Unit = {}
) {
    val backgroundColor = Color(0xFF131314)
    val surfaceColor = Color(0xFF1E1E1E)
    val accentRed = Color(0xFF4A1525)
    val textPrimary = Color.White
    val textSecondary = Color.LightGray
    val textMuted = Color.Gray

    Scaffold(
        containerColor = backgroundColor,
        bottomBar = { DashboardBottomNav(onNavigateToArenas = onNavigateToArenas) },
        topBar = { DashboardTopBar(onLogout) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            Spacer(modifier = Modifier.height(16.dp))

            UserHeaderCard(surfaceColor, accentRed, textPrimary, textSecondary, textMuted)

            MainTrainingCard(surfaceColor, accentRed, textPrimary, textSecondary)

            WeeklyTelemetrySummary(surfaceColor, textPrimary, textSecondary)

            DisciplineTrainingList(surfaceColor, textPrimary, textSecondary)

            Text(
                text = "\"Disiplin bukanlah kecepatan menyerang, melainkan ketepatan kembali ke posisi guard sebelum lawan menyadari ruang.\"\n— DOJO BIOMETRICS CODE // RULE 04",
                color = textSecondary,
                fontSize = 12.sp,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                textAlign = TextAlign.Left
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun DashboardTopBar(onLogout: () -> Unit = {}) {
    val surfaceColor = Color(0xFF131314)
    val primaryColor = Color(0xFFfab4b8)
    val onSurface = Color(0xFFe4e2e2)
    val onSurfaceVariant = Color(0xFFd6c2c2)

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
                    text = "DASHBOARD",
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
                        .background(primaryColor, shape = CircleShape)
                        .clickable {
                            com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
                            onLogout()
                        },
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
}

@Composable
fun DashboardBottomNav(onNavigateToArenas: () -> Unit = {}) {
    val surfaceColor = Color(0xFF131314)
    val primaryColor = Color(0xFFfab4b8)
    val primaryContainer = Color(0xFF381015)
    val onSurfaceVariant = Color(0xFFd6c2c2)

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
            BottomNavItem(icon = Icons.Default.GridView, label = "Dashboard", isSelected = true, tintColor = primaryColor, onClick = { /* Stay here */ })
            BottomNavItem(icon = Icons.Default.SportsKabaddi, label = "Arenas", isSelected = false, tintColor = onSurfaceVariant, onClick = onNavigateToArenas)

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
            BottomNavItem(icon = Icons.Default.Assessment, label = "History", isSelected = false, tintColor = onSurfaceVariant)
            BottomNavItem(icon = Icons.Default.Person, label = "Profile", isSelected = false, tintColor = onSurfaceVariant)
        }
    }
}

@Composable
fun BottomNavItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, isSelected: Boolean, tintColor: Color, onClick: () -> Unit = {}) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
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


@Composable
fun UserHeaderCard(surfaceColor: Color, accentRed: Color, textPrimary: Color, textSecondary: Color, textMuted: Color) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(accentRed, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("AC", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("DOJO PULSE // ID.884", color = textMuted, fontSize = 10.sp, letterSpacing = 1.sp)
                Text("Alex Chen", color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 22.sp)
            }
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
fun MainTrainingCard(surfaceColor: Color, accentRed: Color, textPrimary: Color, textSecondary: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .background(Color.Black, RoundedCornerShape(8.dp))
            ) {
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
                onClick = { /* TODO */ },
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
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(8.dp).background(Color(0xFFFFB3BA)))
            Spacer(modifier = Modifier.width(8.dp))
            Text("RINGKASAN TELEMETRI MINGGUAN", color = textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.weight(1f))
            Text("MINGGU 14", color = textSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
fun DisciplineTrainingList(surfaceColor: Color, textPrimary: Color, textSecondary: Color) {
    Column(modifier = Modifier.fillMaxWidth()) {
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
            metrics = "14 DRILL   •   PROGRES 72%"
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
fun DisciplineItem(surfaceColor: Color, textPrimary: Color, textSecondary: Color, icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, tag: String, desc: String, metrics: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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

@Preview(showBackground = true)
@Composable
fun DashboardPreview() {
    DojoPulseTheme {
        DashboardPage()
    }
}