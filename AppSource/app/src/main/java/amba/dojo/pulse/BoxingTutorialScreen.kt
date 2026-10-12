package amba.dojo.pulse

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import amba.dojo.pulse.vision.PunchType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoxingTutorialScreen(
    onNavigateBack: () -> Unit,
    onStartDrill: () -> Unit
) {
    val backgroundColor = Color(0xFF121212)
    val surfaceColor = Color(0xFF1E1E1E)
    val accentRed = Color(0xFF4A1525)
    val accentPink = Color(0xFFFFB3BA)
    val textPrimary = Color.White
    val textSecondary = Color.LightGray

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("STANCE & GUARD", "6 PUNCHES", "AI COACH HUD")

    Scaffold(
        containerColor = backgroundColor,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(10.dp).background(accentPink))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "BOXING TUTORIAL",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            letterSpacing = 1.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = backgroundColor)
            )
        },
        bottomBar = {
            Surface(
                color = surfaceColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                Box(modifier = Modifier.padding(16.dp)) {
                    Button(
                        onClick = onStartDrill,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = accentRed)
                    ) {
                        Icon(Icons.Default.Videocam, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "MULAI LATIHAN DENGAN KAMERA",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Tab Selector
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = backgroundColor,
                contentColor = accentPink,
                indicator = {}
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                title,
                                fontSize = 11.sp,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == index) accentPink else Color.Gray
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                when (selectedTab) {
                    0 -> StanceAndGuardSection(surfaceColor, accentRed, accentPink, textPrimary, textSecondary)
                    1 -> SixPunchesSection(surfaceColor, accentRed, accentPink, textPrimary, textSecondary)
                    2 -> AiCoachHudSection(surfaceColor, accentRed, accentPink, textPrimary, textSecondary)
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun StanceAndGuardSection(
    surfaceColor: Color,
    accentRed: Color,
    accentPink: Color,
    textPrimary: Color,
    textSecondary: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Welcome Card
        Card(
            colors = CardDefaults.cardColors(containerColor = surfaceColor),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.background(accentRed, RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                        Text("DASAR 01", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("POSISI KUDA-KUDA (ORTHODOX STANCE)", color = textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    "Bagi orang yang dominan tangan kanan (Orthodox):\n" +
                    "• Kaki kiri di depan, kaki kanan di belakang (jarak selebar bahu).\n" +
                    "• Tekuk kedua lutut sedikit untuk menjaga kelenturan dan keseimbangan.\n" +
                    "• Tumpukan berat badan di tengah-tengah kedua kaki.",
                    color = textSecondary,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }
        }

        // The Guard
        Card(
            colors = CardDefaults.cardColors(containerColor = surfaceColor),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.background(accentRed, RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                        Text("DASAR 02", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("TUTUP GUARD (PERTAHANAN)", color = textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    "Guard adalah benteng Anda. Jangan pernah menjatuhkan tangan!\n" +
                    "• Tangan kiri (Lead) menempel di pipi kiri.\n" +
                    "• Tangan kanan (Rear) menempel erat melindungi dagu kanan.\n" +
                    "• Kedua siku merapat ke tubuh untuk melindungi rusuk dan ulu hati.\n" +
                    "• Tundukkan dagu sedikit ke arah dada.",
                    color = textSecondary,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF25161C), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        "⚡ ATURAN EMAS: Setiap selesai memukul, tangan HARUS langsung ditarik kembali ke posisi Guard dalam waktu < 0.23 detik! AI akan mengukur waktu ini.",
                        color = accentPink,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Camera Placement
        Card(
            colors = CardDefaults.cardColors(containerColor = surfaceColor),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, tint = accentPink, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("SETUP KAMERA HP ANDA", color = textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "1. Sandarkan HP di meja setinggi dada atau pinggang.\n" +
                    "2. Berdirilah sekitar 1.5 hingga 2 meter dari kamera.\n" +
                    "3. Pastikan kepala, kedua tangan, dan dada terlihat jelas di dalam layar.\n" +
                    "4. AI Skeleton akan langsung mengunci sendi bahu, siku, dan pergelangan tangan Anda.",
                    color = textSecondary,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
fun SixPunchesSection(
    surfaceColor: Color,
    accentRed: Color,
    accentPink: Color,
    textPrimary: Color,
    textSecondary: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PunchTutorialCard(
            number = "1",
            title = "LEAD JAB",
            subtitle = "Pukulan lurus cepat tangan depan (kiri)",
            targetAngle = "140° - 165°",
            targetRecovery = "< 0.20s",
            howTo = "Luncurkan tangan kiri lurus ke depan target. Putar kepalan tangan mendatar tepat saat mengenai sasaran. Segera tarik kembali ke pipi kiri secepat kilat.",
            commonMistake = "Jangan menjatuhkan tangan kanan saat melempar jab!",
            surfaceColor = surfaceColor,
            accentRed = accentRed,
            accentPink = accentPink,
            textPrimary = textPrimary,
            textSecondary = textSecondary
        )

        PunchTutorialCard(
            number = "2",
            title = "REAR CROSS",
            subtitle = "Pukulan lurus bertenaga tangan belakang (kanan)",
            targetAngle = "140° - 165°",
            targetRecovery = "< 0.23s",
            howTo = "Dorong kaki kanan belakang dan putar pinggul kanan ke depan. Luncurkan tangan kanan lurus ke sasaran dengan kekuatan rotasi badan.",
            commonMistake = "Tangan kiri harus tetap rapat di pipi untuk melindungi wajah.",
            surfaceColor = surfaceColor,
            accentRed = accentRed,
            accentPink = accentPink,
            textPrimary = textPrimary,
            textSecondary = textSecondary
        )

        PunchTutorialCard(
            number = "3",
            title = "LEAD HOOK",
            subtitle = "Pukulan melingkar tangan depan (kiri)",
            targetAngle = "85° - 110° (Siku Bengkok)",
            targetRecovery = "< 0.30s",
            howTo = "Tekuk siku kiri membentuk sudut 90°. Putar tumit kiri dan pinggul melingkar ke arah kanan, seolah menampar dengan buku jari secara horizontal.",
            commonMistake = "Jangan membuka siku terlalu lebar. Jaga siku sejajar dengan tanah.",
            surfaceColor = surfaceColor,
            accentRed = accentRed,
            accentPink = accentPink,
            textPrimary = textPrimary,
            textSecondary = textSecondary
        )

        PunchTutorialCard(
            number = "4",
            title = "REAR HOOK",
            subtitle = "Pukulan melingkar tangan belakang (kanan)",
            targetAngle = "85° - 110° (Siku Bengkok)",
            targetRecovery = "< 0.15s",
            howTo = "Putar kaki dan pinggul kanan penuh. Ayunkan tangan kanan melingkar horizontal ke sisi kiri sasaran dengan siku membentuk sudut 90°.",
            commonMistake = "Segera tarik tangan kanan kembali ke dagu setelah memutar.",
            surfaceColor = surfaceColor,
            accentRed = accentRed,
            accentPink = accentPink,
            textPrimary = textPrimary,
            textSecondary = textSecondary
        )

        PunchTutorialCard(
            number = "5",
            title = "LEAD UPPERCUT",
            subtitle = "Pukulan sodok ke atas tangan depan (kiri)",
            targetAngle = "75° - 105°",
            targetRecovery = "< 0.21s",
            howTo = "Turunkan tangan kiri sedikit saja, tekuk lutut kiri, lalu dorong badan ke atas meluncurkan kepalan tangan ke arah dagu lawan dari bawah ke atas.",
            commonMistake = "Jangan menurunkan tangan terlalu jauh ke bawah pinggang!",
            surfaceColor = surfaceColor,
            accentRed = accentRed,
            accentPink = accentPink,
            textPrimary = textPrimary,
            textSecondary = textSecondary
        )

        PunchTutorialCard(
            number = "6",
            title = "REAR UPPERCUT",
            subtitle = "Pukulan sodok ke atas tangan belakang (kanan)",
            targetAngle = "75° - 105°",
            targetRecovery = "< 0.31s",
            howTo = "Dorong kaki kanan belakang dan putar pinggul ke atas. Luncurkan kepalan tangan kanan vertikal ke arah dagu lawan.",
            commonMistake = "Gunakan dorongan kaki dan pinggul, bukan hanya tenaga lengan.",
            surfaceColor = surfaceColor,
            accentRed = accentRed,
            accentPink = accentPink,
            textPrimary = textPrimary,
            textSecondary = textSecondary
        )
    }
}

@Composable
fun PunchTutorialCard(
    number: String,
    title: String,
    subtitle: String,
    targetAngle: String,
    targetRecovery: String,
    howTo: String,
    commonMistake: String,
    surfaceColor: Color,
    accentRed: Color,
    accentPink: Color,
    textPrimary: Color,
    textSecondary: Color
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = surfaceColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(accentRed, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(number, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(title, color = textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Text(subtitle, color = textSecondary, fontSize = 10.sp)
                    }
                }

                Box(
                    modifier = Modifier
                        .background(Color(0xFF25161C), RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("PUNCH $number", color = accentPink, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Metrics Targets
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF141414), RoundedCornerShape(8.dp))
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("SUDUT SIKU TARGET", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Text(targetAngle, color = textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("RECOVERY GUARD TARGET", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Text(targetRecovery, color = accentPink, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                "Cara Melakukan:\n$howTo",
                color = textSecondary,
                fontSize = 11.sp,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFFB74D), modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    "Kesalahan Umum: $commonMistake",
                    color = Color(0xFFFFB74D),
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
fun AiCoachHudSection(
    surfaceColor: Color,
    accentRed: Color,
    accentPink: Color,
    textPrimary: Color,
    textSecondary: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Card(
            colors = CardDefaults.cardColors(containerColor = surfaceColor),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("CARA MEMBACA HUD SAAT LATIHAN", color = textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "Kamera AI kami memindai 33 titik sendi tubuh Anda secara real-time (60 FPS) untuk menilai teknik Anda.",
                    color = textSecondary,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                HudGuideItem(
                    title = "PUNCH ANGLE (Sudut Siku)",
                    desc = "Mengukur seberapa lurus Anda meluruskan lengan saat jab/cross. Target: 140° - 165°. Jika lengan tidak lurus, skor form akan berkurang.",
                    accentPink = accentPink,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary
                )

                Spacer(modifier = Modifier.height(12.dp))

                HudGuideItem(
                    title = "RETURN GUARD (Kecepatan Pemulihan)",
                    desc = "Waktu dari saat pukulan mencapai titik terjauh sampai tangan Anda kembali melindungi dagu. Standar emas: <= 0.23 detik.",
                    accentPink = accentPink,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary
                )

                Spacer(modifier = Modifier.height(12.dp))

                HudGuideItem(
                    title = "FORM SCORE (Skor Akurasi %)",
                    desc = "Nilai ketepatan teknik Anda (mulai dari 88%). Skor naik saat pukulan lurus dan guard cepat, serta turun jika guard lambat atau tangan terjatuh.",
                    accentPink = accentPink,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary
                )

                Spacer(modifier = Modifier.height(12.dp))

                HudGuideItem(
                    title = "TARGET PUKULAN (180 Strikes)",
                    desc = "Pace test 3 menit dirancang untuk melatih 180 pukulan terstruktur (rata-rata 1 pukulan per detik).",
                    accentPink = accentPink,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary
                )
            }
        }
    }
}

@Composable
fun HudGuideItem(
    title: String,
    desc: String,
    accentPink: Color,
    textPrimary: Color,
    textSecondary: Color
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(6.dp).background(accentPink, CircleShape))
            Spacer(modifier = Modifier.width(8.dp))
            Text(title, color = textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(desc, color = textSecondary, fontSize = 11.sp, lineHeight = 16.sp)
    }
}
