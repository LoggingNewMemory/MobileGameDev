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
    val backgroundColor = Color(0xFF060A12)
    val surfaceColor = Color(0xFF0D1524)
    val accentRed = Color(0xFF00B8D4)
    val accentPink = Color(0xFF22D3EE)
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
                            "START TRAINING WITH CAMERA",
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
                        Text("FOUNDATION 01", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("FIGHTING STANCE (ORTHODOX STANCE)", color = textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    "For right-hand dominant fighters (Orthodox):\n" +
                    "• Lead with left foot forward, right foot back (shoulder-width apart).\n" +
                    "• Keep both knees slightly bent for agility and balance.\n" +
                    "• Center your body weight evenly between both feet.",
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
                        Text("FOUNDATION 02", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("TIGHT GUARD (DEFENSE)", color = textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    "Your guard is your fortress. Never drop your hands!\n" +
                    "• Lead left hand resting against your left cheek.\n" +
                    "• Rear right hand tucked tight protecting right chin.\n" +
                    "• Both elbows tucked tight to protect ribs and solar plexus.\n" +
                    "• Tuck your chin slightly down toward your chest.",
                    color = textSecondary,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0A2A3B), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        "GOLDEN RULE: After every strike, hands MUST instantly snap back to Guard in < 0.23 seconds! The AI measures this recovery time.",
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
                    Text("PHONE CAMERA SETUP", color = textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "1. Prop your phone at chest or waist height on a stable surface.\n" +
                    "2. Stand about 1.5 to 2 meters away from the camera.\n" +
                    "3. Ensure head, both hands, and chest are clearly visible in frame.\n" +
                    "4. AI Skeleton will immediately lock onto shoulders, elbows, and wrists.",
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
            title = "JAB",
            subtitle = "Rapid straight lead-hand punch",
            targetAngle = "140° - 165°",
            targetRecovery = "< 0.20s",
            howTo = "Snap lead left hand straight toward target. Rotate fist flat on impact. Instantly whip back to cheek like a rubber band.",
            commonMistake = "Don't drop your right hand while throwing the jab!",
            surfaceColor = surfaceColor,
            accentRed = accentRed,
            accentPink = accentPink,
            textPrimary = textPrimary,
            textSecondary = textSecondary
        )

        PunchTutorialCard(
            number = "2",
            title = "STRIKE",
            subtitle = "Powerful straight rear-hand cross",
            targetAngle = "140° - 165°",
            targetRecovery = "< 0.23s",
            howTo = "Drive off rear right foot and rotate hips forward. Launch right cross straight to target with full kinetic body rotation.",
            commonMistake = "Keep lead left hand glued to your cheek to protect your face.",
            surfaceColor = surfaceColor,
            accentRed = accentRed,
            accentPink = accentPink,
            textPrimary = textPrimary,
            textSecondary = textSecondary
        )

        PunchTutorialCard(
            number = "3",
            title = "LEFT HOOK",
            subtitle = "Lead-hand horizontal circular strike",
            targetAngle = "85° - 110° (Bent Elbow)",
            targetRecovery = "< 0.30s",
            howTo = "Bend left elbow to 90°. Pivot lead heel and rotate hips to the right, driving horizontal knuckles through the target.",
            commonMistake = "Don't flare elbow too wide. Keep forearm parallel to the ground.",
            surfaceColor = surfaceColor,
            accentRed = accentRed,
            accentPink = accentPink,
            textPrimary = textPrimary,
            textSecondary = textSecondary
        )

        PunchTutorialCard(
            number = "4",
            title = "RIGHT HOOK",
            subtitle = "Rear-hand horizontal circular strike",
            targetAngle = "85° - 110° (Bent Elbow)",
            targetRecovery = "< 0.15s",
            howTo = "Full pivot on rear foot and hips. Whip right hand horizontally across target with elbow locked at 90°.",
            commonMistake = "Instantly snap right hand back to chin after rotating.",
            surfaceColor = surfaceColor,
            accentRed = accentRed,
            accentPink = accentPink,
            textPrimary = textPrimary,
            textSecondary = textSecondary
        )

        PunchTutorialCard(
            number = "5",
            title = "LEFT UPPERCUT",
            subtitle = "Lead-hand vertical upward strike",
            targetAngle = "75° - 105°",
            targetRecovery = "< 0.21s",
            howTo = "Dip lead shoulder slightly, bend knees, and explode upward driving fist toward opponent's chin from below.",
            commonMistake = "Don't drop your hand below your waist before punching!",
            surfaceColor = surfaceColor,
            accentRed = accentRed,
            accentPink = accentPink,
            textPrimary = textPrimary,
            textSecondary = textSecondary
        )

        PunchTutorialCard(
            number = "6",
            title = "RIGHT UPPERCUT",
            subtitle = "Rear-hand vertical upward strike",
            targetAngle = "75° - 105°",
            targetRecovery = "< 0.31s",
            howTo = "Drive off rear foot and thrust hips upward. Launch right fist vertically toward opponent's chin.",
            commonMistake = "Use leg drive and hip thrust, not just arm strength.",
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
                        .background(Color(0xFF0A2A3B), RoundedCornerShape(4.dp))
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
                    Text("TARGET ELBOW ANGLE", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Text(targetAngle, color = textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("TARGET GUARD RECOVERY", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Text(targetRecovery, color = accentPink, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                "How To Perform:\n$howTo",
                color = textSecondary,
                fontSize = 11.sp,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFFB74D), modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    "Common Mistake: $commonMistake",
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
                Text("HOW TO READ HUD DURING TRAINING", color = textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "Our AI camera tracks 33 body skeletal landmarks in real-time (60 FPS) to evaluate your form and technique.",
                    color = textSecondary,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                HudGuideItem(
                    title = "PUNCH ANGLE (Elbow Extension)",
                    desc = "Measures arm extension on Jab / Strike. Target: 140° - 165°. Incomplete extension reduces form score.",
                    accentPink = accentPink,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary
                )

                Spacer(modifier = Modifier.height(12.dp))

                HudGuideItem(
                    title = "RETURN GUARD (Recovery Speed)",
                    desc = "Time from peak punch extension back to chin protection. Gold standard: <= 0.23 seconds.",
                    accentPink = accentPink,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary
                )

                Spacer(modifier = Modifier.height(12.dp))

                HudGuideItem(
                    title = "FORM SCORE (Accuracy Score %)",
                    desc = "Overall technique precision score. Increases with clean extension and fast recovery; decreases with lazy guard or dropped hands.",
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
