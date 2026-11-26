package amba.dojo.pulse

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import amba.dojo.pulse.ui.theme.GoogleSans
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import amba.dojo.pulse.ui.theme.*
import com.google.firebase.auth.FirebaseAuth

@Composable
private fun TabScaffold(
    route: String,
    onNavigate: (String) -> Unit,
    title: String,
    trailing: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Scaffold(
        containerColor = Dojo.Bg,
        bottomBar = { DojoBottomBar(route, onNavigate) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, color = Dojo.Text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                if (trailing != null) {
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Dojo.SurfaceAlt)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) { MonoLabel(trailing, size = 9) }
                }
            }
            content()
            Spacer(Modifier.height(4.dp))
        }
    }
}

/** "Progres" / Progress tab. */
@Composable
fun ProgresScreen(onNavigate: (String) -> Unit = {}, onNextCourse: () -> Unit = {}) {
    // ── Collect live data from repository ──
    val sessions by TrainingRepository.sessions.collectAsState()
    val stats = remember(sessions) { TrainingRepository.weeklyStats() }

    // Weight tracking: derive from sessions (simulated drift for display)
    val baseWeight = 72.5f
    val weightDelta = if (sessions.isEmpty()) 0f else -(sessions.takeLast(7).size * 0.17f)
    val currentWeight = baseWeight + weightDelta
    val weightChange  = weightDelta

    // Build line chart values from weekly session durations (normalised)
    val chartValues: List<Float> = if (sessions.size >= 2) {
        val last8 = sessions.takeLast(8)
        val maxVal = last8.maxOf { it.durationSeconds }.coerceAtLeast(1)
        last8.map { 1f - (it.durationSeconds.toFloat() / maxVal).coerceIn(0.1f, 1f) }
    } else {
        listOf(0.72f, 0.66f, 0.7f, 0.58f, 0.52f, 0.46f, 0.4f, 0.3f)
    }

    val accuracyLabel = if (stats.avgFormScore > 0) "${stats.avgFormScore}.0%" else "88.4%"
    val velocityLabel = if (stats.peakVelocityMs > 0f) "${"%.1f".format(stats.peakVelocityMs)} m/s" else "24.5 m/s"
    val energyTotal   = if (stats.totalStrikes > 0) stats.totalStrikes * 3 else 2_350  // ~3 Cal / punch
    val targetPunches = if (sessions.isNotEmpty()) sessions.last().totalStrikes else 6_842
    val goalPunches   = 10_000
    val targetProgress = (targetPunches.toFloat() / goalPunches).coerceIn(0f, 1f)

    // Bar chart: weekly daily totals
    val barLabels = listOf("M", "T", "W", "T", "F", "S", "S")
    val barValues = if (stats.weeklyBarValues.any { it > 0f }) stats.weeklyBarValues
                    else listOf(0.28f, 0.34f, 0.8f, 0.3f, 1f, 0.5f, 0.2f)
    val barHighlight = if (stats.weeklyBarHighlights.isNotEmpty()) stats.weeklyBarHighlights
                       else setOf(2, 4)

    TabScaffold("progres", onNavigate, "Progress", "This Week") {
        // Weight Tracking card
        DojoCard {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                MonoLabel("WEIGHT TRACKING", size = 9)
                val changeSign = if (weightChange <= 0f) "↓" else "↑"
                val changeAbs  = kotlin.math.abs(weightChange)
                MonoLabel(
                    "$changeSign ${"%.1f".format(changeAbs)} kg",
                    size = 9, color = Dojo.Red, bold = true
                )
            }
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text("${"%.1f".format(currentWeight)}", color = Dojo.Text, fontFamily = Dojo.Mono, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(4.dp))
                MonoLabel("kg", size = 11, modifier = Modifier.padding(bottom = 4.dp))
            }
            Spacer(Modifier.height(8.dp))
            LineChart(values = chartValues, modifier = Modifier.fillMaxWidth().height(72.dp))
        }

        // Stats tiles
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile("ACCURACY",   accuracyLabel,  Modifier.weight(1f))
            StatTile("VELOCITY",   velocityLabel,  Modifier.weight(1f), valueColor = Dojo.Red)
            StatTile("FATIGUE",    if (stats.avgFormScore >= 70) "Normal" else "High", Modifier.weight(1f))
        }

        // Energy burned card
        DojoCard {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                MonoLabel("ENERGY BURNED", size = 9)
                Text("%,d Cal".format(energyTotal), color = Dojo.Red, fontFamily = Dojo.Mono, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(10.dp))
            BarChart(values = barValues, labels = barLabels, highlight = barHighlight)
        }

        // Today's target card
        DojoCard {
            MonoLabel("TODAY'S TARGET:", size = 9)
            Spacer(Modifier.height(4.dp))
            Text("%,d / %,d".format(targetPunches, goalPunches), color = Dojo.Red, fontFamily = Dojo.Mono, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            DojoProgressBar(targetProgress)
        }

        // Next course banner
        val shape = RoundedCornerShape(14.dp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp)
                .clip(shape)
                .background(Brush.horizontalGradient(listOf(Color(0xFF0E4A5C), Color(0xFF0B0B0F))))
                .clickable { onNextCourse() }
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("NEXT COURSE", color = Color.White, fontFamily = Dojo.Mono, fontSize = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Text("▶", color = Color.White, fontSize = 16.sp)
            }
        }
    }
}

/** "Aktivitas" / Activity tab. */
@Composable
fun AktivitasScreen(onNavigate: (String) -> Unit = {}, onStartTraining: () -> Unit = {}) {
    // ── Live data ──
    val sessions by TrainingRepository.sessions.collectAsState()
    val stats = remember(sessions) { TrainingRepository.weeklyStats() }

    // Calendar week: today highlighted
    val cal = java.util.Calendar.getInstance()
    val todayDow = cal.get(java.util.Calendar.DAY_OF_WEEK) // Sun=1..Sat=7
    val days  = listOf("M", "T", "W", "T", "F", "S", "S")
    // Build 7 dates for Mon..Sun of current week
    val mondayCal = cal.clone() as java.util.Calendar
    mondayCal.set(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.MONDAY)
    val dates = (0..6).map { offset ->
        val c = mondayCal.clone() as java.util.Calendar
        c.add(java.util.Calendar.DAY_OF_MONTH, offset)
        c.get(java.util.Calendar.DAY_OF_MONTH)
    }
    val todayIndex = ((todayDow + 5) % 7) // Mon=0..Sun=6

    // Last session stats for activity ring
    val lastSession = sessions.lastOrNull()
    val punchCount  = lastSession?.totalStrikes ?: 6_842
    val goalPunch   = 10_000
    val ringProgress = (punchCount.toFloat() / goalPunch).coerceIn(0f, 1f)
    val achieved = ringProgress >= 1f

    // Distance/Impact/Active from last session
    val distanceKm  = lastSession?.let { "%.1f".format(it.totalStrikes * 0.00077f) } ?: "5.3"
    val impactKg    = lastSession?.let { it.totalStrikes * 3 / 10 } ?: 486
    val activeLabel = lastSession?.let {
        val h = it.durationSeconds / 3600; val m = (it.durationSeconds % 3600) / 60
        if (h > 0) "${h}h ${"%02d".format(m)}m" else "${m}m"
    } ?: "1h 08m"

    // Heart rate line chart — varies by formScore (simulate)
    val hrBase = lastSession?.let { (it.formScore * 0.6f + 50f) / 160f } ?: 0.5f
    val hrValues = listOf(hrBase, hrBase, hrBase, hrBase * 0.3f, hrBase * 1.8f, hrBase,
                          hrBase, hrBase, hrBase * 0.3f, hrBase * 1.8f, hrBase, hrBase)
                        .map { it.coerceIn(0.05f, 1f) }
    val avgHr = lastSession?.let { (it.formScore * 0.6f + 50f).toInt() } ?: 86

    TabScaffold("aktivitas", onNavigate, "Activity") {
        // Week strip
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            days.forEachIndexed { i, d ->
                val isToday = i == todayIndex
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    MonoLabel(d, size = 9)
                    Spacer(Modifier.height(6.dp))
                    Box(
                        modifier = Modifier.size(30.dp).clip(CircleShape)
                            .background(if (isToday) Dojo.Red else Color.Transparent),
                        contentAlignment = Alignment.Center
                    ) {
                        MonoLabel("${dates[i]}", size = 10, color = if (isToday) Color.White else Dojo.Muted, bold = isToday)
                    }
                }
            }
        }

        // Punch ring card
        DojoCard {
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                ProgressRing(progress = ringProgress, size = 130.dp, stroke = 10.dp) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("%,d".format(punchCount), color = Dojo.Text, fontFamily = Dojo.Mono, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        MonoLabel("PUNCHES", size = 8)
                    }
                }
                Spacer(Modifier.height(14.dp))
                val pill = RoundedCornerShape(50)
                Box(
                    Modifier.clip(pill).border(1.dp, Dojo.Red, pill).padding(horizontal = 14.dp, vertical = 5.dp)
                ) {
                    MonoLabel(
                        if (achieved) "Target Achieved" else "%,d / %,d target".format(punchCount, goalPunch),
                        size = 9, color = Dojo.Red
                    )
                }
            }
        }

        // Activity tiles
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile("DISTANCE", "$distanceKm km",    Modifier.weight(1f))
            StatTile("IMPACT",   "$impactKg kg",       Modifier.weight(1f), valueColor = Dojo.Red)
            StatTile("ACTIVE",   activeLabel,           Modifier.weight(1f))
        }

        // Heart rate card
        DojoCard {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                MonoLabel("HEART RATE", size = 9)
                MonoLabel("$avgHr bpm Avg", size = 9, color = Dojo.Red, bold = true)
            }
            Spacer(Modifier.height(8.dp))
            LineChart(values = hrValues, modifier = Modifier.fillMaxWidth().height(60.dp), smooth = false)
        }

        // Start training button
        val shape = RoundedCornerShape(12.dp)
        Row(
            modifier = Modifier.fillMaxWidth().height(52.dp).clip(shape).border(1.dp, Dojo.Red, shape)
                .clickable { onStartTraining() }.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("NEXT", color = Dojo.Red, fontFamily = Dojo.Mono, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text("▶", color = Dojo.Red, fontSize = 14.sp)
        }
    }
}

/** "Profil" tab — full DojoPulse fighter profile matching HTML reference. */
@Composable
fun ProfilScreen(onNavigate: (String) -> Unit = {}, onLogout: () -> Unit = {}) {
    val auth = try { FirebaseAuth.getInstance() } catch (_: Exception) { null }
    val user = auth?.currentUser
    val displayName = user?.displayName?.takeIf { it.isNotBlank() }
        ?: user?.email?.substringBefore('@') ?: "Fighter"
    val email = user?.email ?: "-"
    val userId = user?.uid ?: "guest"
    val shortId = "DP-${userId.take(5).uppercase()}"

    // ── UI-only state ──
    var points by remember { androidx.compose.runtime.mutableIntStateOf(2_450) }
    var voucherTabAktif by remember { androidx.compose.runtime.mutableStateOf(true) }
    var toastMessage by remember { androidx.compose.runtime.mutableStateOf<String?>(null) }

    LaunchedEffect(toastMessage) {
        if (toastMessage != null) {
            kotlinx.coroutines.delay(3_000)
            toastMessage = null
        }
    }

    // ── Colour tokens (local) ──
    val BgDark       = Color(0xFF0F0F11)
    val BgCard       = Color(0xFF141419)
    val BgCardBorder = Color(0xFF23232C)
    val BgDeep       = Color(0xFF0F0F13)
    val AccentRed    = Color(0xFFE63946)
    val AccentCoral  = Color(0xFFFF4D4F)
    val TextPrim     = Color.White
    val TextSec      = Color(0xFFA0A0B0)
    val TextMut      = Color(0xFF7E7E8C)
    val Emerald      = Color(0xFF34D399)
    val Amber        = Color(0xFFFBBF24)
    val Sky          = Color(0xFF38BDF8)

    // initials
    val initials = displayName.trim().split(" ")
        .mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("").uppercase()
        .ifBlank { "JA" }

    Box(modifier = Modifier.fillMaxSize().background(BgDark)) {

        Scaffold(
            containerColor = BgDark,
            bottomBar = { DojoBottomBar("profil", onNavigate) }
        ) { innerPadding ->

            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {

                // ── Top bar ──────────────────────────────────────────────
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "Profile",
                                color = TextPrim,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Settings icon
                            Box(
                                modifier = Modifier.size(36.dp).clip(CircleShape)
                                    .background(Color(0xFF16161C))
                                    .border(1.dp, Color(0xFF262630), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Settings, contentDescription = "Settings", tint = TextSec, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }

                // ── 1. Fighter Identity Card ─────────────────────────────
                item {
                    androidx.compose.material3.Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = BgCard),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BgCardBorder)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Avatar + info
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Box(
                                    modifier = Modifier.size(56.dp),
                                    contentAlignment = Alignment.BottomEnd
                                ) {
                                    Box(
                                        modifier = Modifier.size(56.dp).clip(CircleShape)
                                            .background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(AccentRed, AccentCoral))),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(initials, color = TextPrim, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                                    }
                                    // Online dot
                                    Box(
                                        modifier = Modifier.size(14.dp).clip(CircleShape).background(BgCard).padding(2.dp).clip(CircleShape).background(Emerald)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "@$displayName",
                                        color = TextPrim, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold,
                                        maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                    Text(email, color = TextMut, fontSize = 12.sp, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                                    Spacer(Modifier.height(6.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(AccentCoral))
                                        Text("DOJO MUAY THAI HQ", color = TextSec, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp)
                                    }
                                }
                            }

                            Spacer(Modifier.height(14.dp))
                            androidx.compose.material3.HorizontalDivider(color = Color(0xFF1D1D26), thickness = 1.dp)
                            Spacer(Modifier.height(14.dp))

                            // Telemetry grid
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(
                                    Triple("Workouts", "5/7", TextPrim),
                                    Triple("Form Score", "88%", AccentCoral),
                                    Triple("Mat Time", "4h 25m", TextPrim)
                                ).forEach { (label, value, vColor) ->
                                    Column(
                                        modifier = Modifier.weight(1f)
                                            .background(BgDeep, RoundedCornerShape(10.dp))
                                            .border(1.dp, Color(0xFF1F1F28), RoundedCornerShape(10.dp))
                                            .padding(10.dp)
                                    ) {
                                        Text(label, color = TextMut, fontSize = 10.sp, letterSpacing = 0.5.sp)
                                        Spacer(Modifier.height(4.dp))
                                        Text(value, color = vColor, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }

                // ── 2. Points Reserve Card ───────────────────────────────
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color(0xFF1B171C), Color(0xFF141419))))
                            .border(1.dp, Color(0xFF2F2127), RoundedCornerShape(16.dp))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.background(AccentRed, RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                                        Text("POINTS RESERVE", color = TextPrim, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                                    }
                                    Text("REWARD HUB", color = AccentCoral, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Row(
                                    modifier = Modifier
                                        .background(Color(0xFF241D24), RoundedCornerShape(50.dp))
                                        .border(1.dp, Color(0xFF3E2329), RoundedCornerShape(50.dp))
                                        .padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(AccentCoral))
                                    Text("TIER: ELITE", color = TextPrim, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = GoogleSans)
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                                Column {
                                    Text("Fighter Points Balance", color = TextMut, fontSize = 10.sp, letterSpacing = 1.sp)
                                    Spacer(Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                        Text("%,d".format(points), color = TextPrim, fontSize = 32.sp, fontWeight = FontWeight.Black)
                                        Text("DP", color = AccentCoral, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                                    }
                                    Text("Est: Rp ${"%.0f".format(points * 50.0).reversed().chunked(3).joinToString(".").reversed()} · 15% Off All Gear", color = TextSec, fontSize = 11.sp)
                                }
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFF241C22), RoundedCornerShape(10.dp))
                                        .border(1.dp, Color(0xFF44232C), RoundedCornerShape(10.dp))
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("Redeem Points", color = AccentCoral, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = AccentCoral, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                            Spacer(Modifier.height(14.dp))
                            androidx.compose.material3.HorizontalDivider(color = Color(0xFF232029), thickness = 1.dp)
                            Spacer(Modifier.height(10.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("VIP Striker Tier", color = TextMut, fontSize = 10.sp)
                                Text("550 DP to Master Belt", color = AccentCoral, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Spacer(Modifier.height(6.dp))
                            Box(modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50)).background(Color(0xFF201D24))) {
                                Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(0.82f).clip(RoundedCornerShape(50)).background(AccentRed))
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }

                // ── 3. Redeem Catalog ────────────────────────────────────
                item {
                    Column(modifier = Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("REWARDS REDEEM CATALOG", color = TextPrim, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                Box(modifier = Modifier.background(Color(0xFF1D1D26), RoundedCornerShape(50)).border(1.dp, Color(0xFF2B2B38), RoundedCornerShape(50)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                                    Text("4 Available", color = TextSec, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Text("Weekly Update", color = AccentCoral, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }

                        data class RItem(val cat: String, val title: String, val sub: String, val cost: Int, val type: String)
                        val catalog = listOf(
                            RItem("Gear Shopping Voucher", "Rp 50,000 Store Discount", "All gear items in Dojo Store", 1_000, "voucher"),
                            RItem("Special Access", "Free 1 Session VR Sparring / AI Ar…", "MediaPipe Biomechanics Analysis", 1_500, "vr"),
                            RItem("Hardware Reward", "Smart Biometric Handwraps Ma…", "Optical reticle compatible with sensor HUD", 2_000, "hardware"),
                            RItem("Official Merch", "Tactical Velcro Patch DojoPulse", "Limited edition tactical embroidered patch", 800, "patch")
                        )
                        catalog.forEach { item ->
                            val (iconBg, iconBrd, iconTint) = when (item.type) {
                                "voucher" -> Triple(Color(0xFF1E1720), Color(0xFF3D232C), AccentCoral)
                                "vr"      -> Triple(Color(0xFF171C26), Color(0xFF23324A), Sky)
                                "hardware"-> Triple(Color(0xFF101014), Color(0xFF2C2C36), TextSec)
                                else      -> Triple(Color(0xFF241D14), Color(0xFF3E2E18), Amber)
                            }
                            val iconVec = when (item.type) {
                                "voucher"  -> Icons.Default.ConfirmationNumber
                                "vr"       -> Icons.Default.ViewInAr
                                "hardware" -> Icons.Default.Devices
                                else       -> Icons.Default.Star
                            }
                            val canAfford = points >= item.cost
                            Row(
                                modifier = Modifier.fillMaxWidth().background(BgCard, RoundedCornerShape(12.dp)).border(1.dp, BgCardBorder, RoundedCornerShape(12.dp)).padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(modifier = Modifier.size(48.dp).background(iconBg, RoundedCornerShape(10.dp)).border(1.dp, iconBrd, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                                    Icon(iconVec, contentDescription = null, tint = iconTint, modifier = Modifier.size(24.dp))
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.cat, color = TextMut, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
                                    Text(item.title, color = TextPrim, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                                    Text(item.sub, color = TextMut, fontSize = 11.sp, maxLines = 1)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("%,d DP".format(item.cost), color = AccentCoral, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                                    Spacer(Modifier.height(6.dp))
                                    androidx.compose.material3.Button(
                                        onClick = {
                                            if (canAfford) {
                                                points -= item.cost
                                                toastMessage = "Successfully redeemed ${item.cost} DP for: ${item.title}"
                                            } else {
                                                toastMessage = "Insufficient points (Need ${item.cost} DP)"
                                            }
                                        },
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                            containerColor = if (canAfford) AccentRed else Color(0xFF2A1A1A),
                                            contentColor = TextPrim
                                        )
                                    ) { Text("Tukar", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }

                // ── 4. Promo & Voucher ───────────────────────────────────
                item {
                    Column(modifier = Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("MY PROMOS & VOUCHERS", color = TextPrim, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        }
                        // Tab pills
                        Row(
                            modifier = Modifier.fillMaxWidth().background(BgCard, RoundedCornerShape(10.dp)).border(1.dp, BgCardBorder, RoundedCornerShape(10.dp)).padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf(true to "Active Vouchers (2)", false to "Used History (1)").forEach { (isAktif, label) ->
                                Box(
                                    modifier = Modifier.weight(1f)
                                        .background(if (voucherTabAktif == isAktif) AccentRed else Color.Transparent, RoundedCornerShape(8.dp))
                                        .clickable { voucherTabAktif = isAktif }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(label, color = if (voucherTabAktif == isAktif) TextPrim else TextMut, fontSize = 12.sp, fontWeight = if (voucherTabAktif == isAktif) FontWeight.SemiBold else FontWeight.Normal)
                                }
                            }
                        }

                        if (voucherTabAktif) {
                            // Active voucher 1
                            Column(modifier = Modifier.fillMaxWidth().background(BgCard, RoundedCornerShape(12.dp)).border(1.dp, BgCardBorder, RoundedCornerShape(12.dp)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.background(Color(0xFF291419), RoundedCornerShape(4.dp)).border(1.dp, Color(0xFF4B2029), RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                                        Text("DOJOCHAMP", color = AccentCoral, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = GoogleSans)
                                    }
                                    Box(modifier = Modifier.background(Color(0x1A34D399), RoundedCornerShape(4.dp)).border(1.dp, Color(0xFF166534), RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                                        Text("Active", color = Emerald, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Text("30% Off Mats & Wraps", color = TextPrim, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                androidx.compose.material3.HorizontalDivider(color = Color(0xFF1E1E28), thickness = 1.dp)
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text("Valid until Nov 30, 2026", color = TextMut, fontSize = 11.sp)
                                    Row(modifier = Modifier.clickable { toastMessage = "Code DOJOCHAMP copied!" }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = null, tint = AccentCoral, modifier = Modifier.size(14.dp))
                                        Text("Copy Code", color = AccentCoral, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                            // Active voucher 2
                            Column(modifier = Modifier.fillMaxWidth().background(BgCard, RoundedCornerShape(12.dp)).border(1.dp, BgCardBorder, RoundedCornerShape(12.dp)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.background(Color(0xFF291419), RoundedCornerShape(4.dp)).border(1.dp, Color(0xFF4B2029), RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                                        Text("GEARCASH20", color = AccentCoral, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = GoogleSans)
                                    }
                                    Box(modifier = Modifier.background(Color(0x1A34D399), RoundedCornerShape(4.dp)).border(1.dp, Color(0xFF166534), RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                                        Text("Active", color = Emerald, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Text("Cashback 20% Apparel & Fightwear", color = TextPrim, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                androidx.compose.material3.HorizontalDivider(color = Color(0xFF1E1E28), thickness = 1.dp)
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text("Min. spend Rp 250,000", color = TextMut, fontSize = 11.sp)
                                    Row(modifier = Modifier.clickable { toastMessage = "Code GEARCASH20 copied!" }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = null, tint = AccentCoral, modifier = Modifier.size(14.dp))
                                        Text("Copy Code", color = AccentCoral, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        } else {
                            // Used voucher
                            Column(modifier = Modifier.fillMaxWidth().background(BgCard.copy(alpha = 0.6f), RoundedCornerShape(12.dp)).border(1.dp, BgCardBorder, RoundedCornerShape(12.dp)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.background(Color(0xFF1C1C24), RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                                        Text("HBAG-OCT-FREE", color = TextMut, fontSize = 11.sp, fontFamily = GoogleSans)
                                    }
                                    Box(modifier = Modifier.background(Color(0xFF27272A), RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                                        Text("Already Used", color = TextMut, fontSize = 10.sp)
                                    }
                                }
                                Text("Free 1 Hour Heavy Bag Session Zone B", color = TextSec, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                androidx.compose.material3.HorizontalDivider(color = Color(0xFF1E1E28), thickness = 1.dp)
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Redeemed Oct 02, 2026", color = TextMut, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }

                // ── 5. Settings ──────────────────────────────────────────
                item {
                    Column(modifier = Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("ACCOUNT & SENSOR SETTINGS", color = TextPrim, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        data class SRow(val icon: androidx.compose.ui.graphics.vector.ImageVector, val iconBg: Color, val iconBrd: Color, val iconTint: Color, val title: String, val sub: String, val badge: String? = null)
                        listOf(
                            SRow(Icons.Default.Wifi,  Color(0xFF21151B), Color(0xFF3E1D27), AccentCoral, "Connect Wearable Sensor", "MediaPipe Ring Sensor v2 active", "Connected 94%"),
                            SRow(Icons.Default.Lock,  Color(0xFF171B26), Color(0xFF1F2D42), Sky,         "Change Password & Biometrics",  "Update fighter HUD credentials"),
                            SRow(Icons.Default.Help,  Color(0xFF241D14), Color(0xFF3E2E18), Amber,       "FAQ & Dojo Support",            "Points guide, warranty claims & manuals")
                        ).forEach { s ->
                            Row(
                                modifier = Modifier.fillMaxWidth().background(BgCard, RoundedCornerShape(12.dp)).border(1.dp, Color(0xFF202028), RoundedCornerShape(12.dp)).clickable { }.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(modifier = Modifier.background(s.iconBg, RoundedCornerShape(8.dp)).border(1.dp, s.iconBrd, RoundedCornerShape(8.dp)).padding(8.dp)) {
                                    Icon(s.icon, contentDescription = null, tint = s.iconTint, modifier = Modifier.size(16.dp))
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(s.title, color = TextPrim, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        if (s.badge != null) {
                                            Box(modifier = Modifier.background(Emerald.copy(alpha = 0.12f), RoundedCornerShape(4.dp)).border(1.dp, Color(0xFF166534), RoundedCornerShape(4.dp)).padding(horizontal = 5.dp, vertical = 2.dp)) {
                                                Text(s.badge, color = Emerald, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                    Text(s.sub, color = TextMut, fontSize = 11.sp)
                                }
                                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextMut, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }

                // ── 6. Logout ────────────────────────────────────────────
                item {
                    Column(
                        modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .background(BgCard, RoundedCornerShape(12.dp))
                                .border(1.dp, Color(0xFF262632), RoundedCornerShape(12.dp))
                                .clickable {
                                    try { auth?.signOut() } catch (_: Exception) { }
                                    onLogout()
                                }
                                .padding(vertical = 14.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Logout, contentDescription = null, tint = AccentCoral, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("LOG OUT", color = AccentCoral, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        }
                        Text(
                            "DojoPulse v4.1.2",
                            color = TextMut,
                            fontSize = 10.sp,
                            fontFamily = GoogleSans
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }

        // ── Toast overlay ─────────────────────────────────────────────────
        toastMessage?.let { msg ->
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 90.dp, start = 20.dp, end = 20.dp)
                    .fillMaxWidth()
                    .background(Color(0xFF1A1720), RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFF442834), RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(AccentCoral))
                    Spacer(Modifier.width(10.dp))
                    Text(msg, color = TextPrim, fontSize = 12.sp, modifier = Modifier.weight(1f))
                    androidx.compose.material3.TextButton(onClick = { toastMessage = null }) {
                        Text("CLOSE", color = AccentCoral, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun LineChart(values: List<Float>, modifier: Modifier = Modifier, smooth: Boolean = true) {
    Canvas(modifier = modifier) {
        if (values.size < 2) return@Canvas
        val stepX = size.width / (values.size - 1)
        val points = values.mapIndexed { i, v -> Offset(i * stepX, size.height * v) }
        val line = Path().apply {
            moveTo(points[0].x, points[0].y)
            for (i in 1 until points.size) {
                val p0 = points[i - 1]
                val p1 = points[i]
                if (smooth) {
                    val mx = (p0.x + p1.x) / 2f
                    cubicTo(mx, p0.y, mx, p1.y, p1.x, p1.y)
                } else {
                    lineTo(p1.x, p1.y)
                }
            }
        }
        val fill = Path().apply {
            addPath(line)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(fill, Brush.verticalGradient(listOf(Dojo.Red.copy(alpha = 0.25f), Color.Transparent)))
        drawPath(line, Dojo.Red, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawCircle(Color.White, radius = 3.dp.toPx(), center = points.last())
    }
}

@Composable
private fun BarChart(values: List<Float>, labels: List<String>, highlight: Set<Int>) {
    Row(
        modifier = Modifier.fillMaxWidth().height(96.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        values.forEachIndexed { i, v ->
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                Box(
                    modifier = Modifier
                        .width(10.dp)
                        .fillMaxHeight(v * 0.8f)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (i in highlight) Dojo.Red else Dojo.Border)
                )
                Spacer(Modifier.height(4.dp))
                MonoLabel(labels[i], size = 8, color = Dojo.Muted)
            }
        }
    }
}
