package amba.dojo.pulse

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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

/** "Profil" tab with sign-out. */
@Composable
fun ProfilScreen(onNavigate: (String) -> Unit = {}, onLogout: () -> Unit = {}) {
    val user = try { FirebaseAuth.getInstance().currentUser } catch (_: Exception) { null }
    val name = user?.displayName?.takeIf { it.isNotBlank() } ?: user?.email?.substringBefore('@') ?: "Fighter"
    val email = user?.email ?: "-"

    TabScaffold("profil", onNavigate, "Profil") {
        DojoCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(56.dp).clip(CircleShape).background(Dojo.RedDeep).border(1.dp, Dojo.Red, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = Dojo.RedSoft, modifier = Modifier.size(28.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    MonoLabel("DOJO PULSE // FIGHTER", size = 9)
                    Text(name, color = Dojo.Text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    MonoLabel(email, size = 10)
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile("SESSIONS", "5/7", Modifier.weight(1f))
            StatTile("FORM SCORE", "88%", Modifier.weight(1f), valueColor = Dojo.Red)
            StatTile("MAT TIME", "4h 25m", Modifier.weight(1f))
        }
        val shape = RoundedCornerShape(12.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(shape)
                .border(1.dp, Dojo.Red, shape)
                .clickable {
                    try { FirebaseAuth.getInstance().signOut() } catch (_: Exception) { }
                    onLogout()
                },
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Logout, contentDescription = null, tint = Dojo.Red, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("LOGOUT", color = Dojo.Red, fontFamily = Dojo.Mono, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
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
