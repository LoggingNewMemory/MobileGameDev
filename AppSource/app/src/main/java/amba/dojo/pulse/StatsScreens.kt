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

/** "Progres" / Kemajuan tab. */
@Composable
fun ProgresScreen(onNavigate: (String) -> Unit = {}, onNextCourse: () -> Unit = {}) {
    TabScaffold("progres", onNavigate, "Kemajuan", "Minggu Ini") {
        DojoCard {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                MonoLabel("WEIGHT TRACKING", size = 9)
                MonoLabel("↓ 1.2 kg", size = 9, color = Dojo.Red, bold = true)
            }
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text("72.5", color = Dojo.Text, fontFamily = Dojo.Mono, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(4.dp))
                MonoLabel("kg", size = 11, modifier = Modifier.padding(bottom = 4.dp))
            }
            Spacer(Modifier.height(8.dp))
            LineChart(
                values = listOf(0.72f, 0.66f, 0.7f, 0.58f, 0.52f, 0.46f, 0.4f, 0.3f),
                modifier = Modifier.fillMaxWidth().height(72.dp)
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile("ACCURACY", "88.4%", Modifier.weight(1f))
            StatTile("VELOCITY", "24.5 m/s", Modifier.weight(1f), valueColor = Dojo.Red)
            StatTile("FATIGUE", "Normal", Modifier.weight(1f))
        }

        DojoCard {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                MonoLabel("ENERGY BURNED", size = 9)
                Text("2,350 Cal", color = Dojo.Red, fontFamily = Dojo.Mono, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(10.dp))
            BarChart(
                values = listOf(0.28f, 0.34f, 0.8f, 0.3f, 1f, 0.5f, 0.2f),
                labels = listOf("M", "T", "W", "T", "F", "S", "S"),
                highlight = setOf(2, 4)
            )
        }

        DojoCard {
            MonoLabel("TODAY'S TARGET:", size = 9)
            Spacer(Modifier.height(4.dp))
            Text("6,842 / 10,000", color = Dojo.Red, fontFamily = Dojo.Mono, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            DojoProgressBar(0.68f)
        }

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

/** "Aktivitas" tab. */
@Composable
fun AktivitasScreen(onNavigate: (String) -> Unit = {}, onStartTraining: () -> Unit = {}) {
    TabScaffold("aktivitas", onNavigate, "Aktivitas") {
        val days = listOf("M", "T", "W", "T", "F", "S", "S")
        val dates = listOf(22, 23, 24, 25, 26, 27, 28)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            days.forEachIndexed { i, d ->
                val today = dates[i] == 25
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    MonoLabel(d, size = 9)
                    Spacer(Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(if (today) Dojo.Red else Color.Transparent),
                        contentAlignment = Alignment.Center
                    ) {
                        MonoLabel("${dates[i]}", size = 10, color = if (today) Color.White else Dojo.Muted, bold = today)
                    }
                }
            }
        }

        DojoCard {
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                ProgressRing(progress = 0.68f, size = 130.dp, stroke = 10.dp) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("6,842", color = Dojo.Text, fontFamily = Dojo.Mono, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        MonoLabel("PUNCHES", size = 8)
                    }
                }
                Spacer(Modifier.height(14.dp))
                val pill = RoundedCornerShape(50)
                Box(
                    Modifier
                        .clip(pill)
                        .border(1.dp, Dojo.Red, pill)
                        .padding(horizontal = 14.dp, vertical = 5.dp)
                ) { MonoLabel("Target Achieved", size = 9, color = Dojo.Red) }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile("DISTANCE", "5.3 km", Modifier.weight(1f))
            StatTile("IMPACT", "486 kg", Modifier.weight(1f), valueColor = Dojo.Red)
            StatTile("ACTIVE", "1h 08m", Modifier.weight(1f))
        }

        DojoCard {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                MonoLabel("HEART RATE", size = 9)
                MonoLabel("86 bpm Avg", size = 9, color = Dojo.Red, bold = true)
            }
            Spacer(Modifier.height(8.dp))
            LineChart(
                values = listOf(0.5f, 0.5f, 0.5f, 0.15f, 0.9f, 0.5f, 0.5f, 0.5f, 0.15f, 0.9f, 0.5f, 0.5f),
                modifier = Modifier.fillMaxWidth().height(60.dp),
                smooth = false
            )
        }

        val shape = RoundedCornerShape(12.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(shape)
                .border(1.dp, Dojo.Red, shape)
                .clickable { onStartTraining() }
                .padding(horizontal = 16.dp),
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
                                "DOJOPULSE // SYS",
                                color = AccentCoral,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.5.sp,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                            Text(
                                "Profil Fighter",
                                color = TextPrim,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Notification icon
                            Box(
                                modifier = Modifier.size(36.dp).clip(CircleShape)
                                    .background(Color(0xFF16161C))
                                    .border(1.dp, Color(0xFF262630), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Notifications, contentDescription = "Notifikasi", tint = TextSec, modifier = Modifier.size(18.dp))
                            }
                            // Settings icon
                            Box(
                                modifier = Modifier.size(36.dp).clip(CircleShape)
                                    .background(Color(0xFF16161C))
                                    .border(1.dp, Color(0xFF262630), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Settings, contentDescription = "Pengaturan", tint = TextSec, modifier = Modifier.size(18.dp))
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
                            // ID + VIP badge
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "ID: $shortId",
                                    color = TextMut,
                                    fontSize = 10.sp,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    letterSpacing = 1.sp
                                )
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFF2B1419), RoundedCornerShape(4.dp))
                                        .border(1.dp, Color(0xFF522129), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text("ELITE STRIKER VIP", color = AccentCoral, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                                }
                            }

                            Spacer(Modifier.height(14.dp))

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
                                    Text("⚡ REWARD HUB", color = AccentCoral, fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
                                    Text("TIER: ELITE", color = TextPrim, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                                Column {
                                    Text("Saldo Poin Fighter", color = TextMut, fontSize = 10.sp, letterSpacing = 1.sp)
                                    Spacer(Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                        Text("%,d".format(points), color = TextPrim, fontSize = 32.sp, fontWeight = FontWeight.Black)
                                        Text("DP", color = AccentCoral, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                                    }
                                    Text("Estimasi: Rp ${"%.0f".format(points * 50.0).reversed().chunked(3).joinToString(".").reversed()} · Diskon 15% All Gear", color = TextSec, fontSize = 11.sp)
                                }
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFF241C22), RoundedCornerShape(10.dp))
                                        .border(1.dp, Color(0xFF44232C), RoundedCornerShape(10.dp))
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("Tukar Poin", color = AccentCoral, fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
                                Text("KATALOG REDEEM HADIAH", color = TextPrim, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                Box(modifier = Modifier.background(Color(0xFF1D1D26), RoundedCornerShape(50)).border(1.dp, Color(0xFF2B2B38), RoundedCornerShape(50)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                                    Text("4 Tersedia", color = TextSec, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Text("Update Mingguan", color = AccentCoral, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }

                        data class RItem(val cat: String, val title: String, val sub: String, val cost: Int, val type: String)
                        val catalog = listOf(
                            RItem("Voucher Belanja Gear", "Potongan Rp 50.000 Store", "Semua item gear di Dojo Store", 1_000, "voucher"),
                            RItem("Akses Khusus", "Gratis 1 Sesi Sparring VR / AI Ar…", "Analisis MediaPipe Biomekanik", 1_500, "vr"),
                            RItem("Hardware Reward", "Smart Biometric Handwraps Ma…", "Reticle optik kompatibel sensor HUD", 2_000, "hardware"),
                            RItem("Official Merch", "Tactical Velcro Patch DojoPulse", "Patch bordir taktikal edisi terbatas", 800, "patch")
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
                                                toastMessage = "Berhasil menukar ${item.cost} DP untuk: ${item.title}"
                                            } else {
                                                toastMessage = "Poin tidak mencukupi (Perlu ${item.cost} DP)"
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
                            Text("PROMO & VOUCHER SAYA", color = TextPrim, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            Text("[LOG_SYNC: ON]", color = TextMut, fontSize = 10.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                        }
                        // Tab pills
                        Row(
                            modifier = Modifier.fillMaxWidth().background(BgCard, RoundedCornerShape(10.dp)).border(1.dp, BgCardBorder, RoundedCornerShape(10.dp)).padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf(true to "Voucher Aktif (2)", false to "Riwayat Terpakai (1)").forEach { (isAktif, label) ->
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
                                        Text("DOJOCHAMP", color = AccentCoral, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                                    }
                                    Box(modifier = Modifier.background(Color(0x1A34D399), RoundedCornerShape(4.dp)).border(1.dp, Color(0xFF166534), RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                                        Text("Aktif", color = Emerald, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Text("Diskon 30% Matras & Wraps", color = TextPrim, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                androidx.compose.material3.HorizontalDivider(color = Color(0xFF1E1E28), thickness = 1.dp)
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text("Berlaku s.d. 30 Nov 2026", color = TextMut, fontSize = 11.sp)
                                    Row(modifier = Modifier.clickable { toastMessage = "Kode DOJOCHAMP disalin!" }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = null, tint = AccentCoral, modifier = Modifier.size(14.dp))
                                        Text("Salin Kode", color = AccentCoral, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                            // Active voucher 2
                            Column(modifier = Modifier.fillMaxWidth().background(BgCard, RoundedCornerShape(12.dp)).border(1.dp, BgCardBorder, RoundedCornerShape(12.dp)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.background(Color(0xFF291419), RoundedCornerShape(4.dp)).border(1.dp, Color(0xFF4B2029), RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                                        Text("GEARCASH20", color = AccentCoral, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                                    }
                                    Box(modifier = Modifier.background(Color(0x1A34D399), RoundedCornerShape(4.dp)).border(1.dp, Color(0xFF166534), RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                                        Text("Aktif", color = Emerald, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Text("Cashback 20% Apparel & Fightwear", color = TextPrim, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                androidx.compose.material3.HorizontalDivider(color = Color(0xFF1E1E28), thickness = 1.dp)
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text("Min. belanja Rp 250.000", color = TextMut, fontSize = 11.sp)
                                    Row(modifier = Modifier.clickable { toastMessage = "Kode GEARCASH20 disalin!" }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = null, tint = AccentCoral, modifier = Modifier.size(14.dp))
                                        Text("Salin Kode", color = AccentCoral, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        } else {
                            // Used voucher
                            Column(modifier = Modifier.fillMaxWidth().background(BgCard.copy(alpha = 0.6f), RoundedCornerShape(12.dp)).border(1.dp, BgCardBorder, RoundedCornerShape(12.dp)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.background(Color(0xFF1C1C24), RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                                        Text("HBAG-OCT-FREE", color = TextMut, fontSize = 11.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                                    }
                                    Box(modifier = Modifier.background(Color(0xFF27272A), RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                                        Text("Sudah Digunakan", color = TextMut, fontSize = 10.sp)
                                    }
                                }
                                Text("Gratis 1 Jam Sesi Heavy Bag Zona B", color = TextSec, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                androidx.compose.material3.HorizontalDivider(color = Color(0xFF1E1E28), thickness = 1.dp)
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Ditukar 02 Okt 2026", color = TextMut, fontSize = 11.sp)
                                    Text("STATION #02", color = TextMut, fontSize = 10.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }

                // ── 5. Settings ──────────────────────────────────────────
                item {
                    Column(modifier = Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("PENGATURAN AKUN & SENSOR", color = TextPrim, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        data class SRow(val icon: androidx.compose.ui.graphics.vector.ImageVector, val iconBg: Color, val iconBrd: Color, val iconTint: Color, val title: String, val sub: String, val badge: String? = null)
                        listOf(
                            SRow(Icons.Default.Wifi,  Color(0xFF21151B), Color(0xFF3E1D27), AccentCoral, "Hubungkan Sensor Wearable", "MediaPipe Ring Sensor v2 aktif", "Terhubung 94%"),
                            SRow(Icons.Default.Lock,  Color(0xFF171B26), Color(0xFF1F2D42), Sky,         "Ganti Kata Sandi & Biometrik",  "Update kredensial fighter HUD"),
                            SRow(Icons.Default.Help,  Color(0xFF241D14), Color(0xFF3E2E18), Amber,       "FAQ & Bantuan Dojo",            "Panduan poin, klaim garansi & instruksi")
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
                            Text("LOGOUT FIGHTER HUD", color = AccentCoral, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        }
                        Text(
                            "DOJOPULSE FIRMWARE V4.1.2 // TEL-ENGINE ACTIVE",
                            color = TextMut,
                            fontSize = 10.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
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
                        Text("TUTUP", color = AccentCoral, fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
