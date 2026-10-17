package amba.dojo.pulse

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import amba.dojo.pulse.ui.theme.*
import com.google.firebase.auth.FirebaseAuth

/** "Beranda" - home tab. */
@Composable
fun DashboardPage(
    onNavigate: (String) -> Unit = {},
    onStartTraining: () -> Unit = {},
    onOpenWorkout: (String) -> Unit = {}
) {
    val user = try { FirebaseAuth.getInstance().currentUser } catch (_: Exception) { null }
    val name = user?.displayName?.takeIf { it.isNotBlank() }?.substringBefore(' ')
        ?: user?.email?.substringBefore('@')
        ?: "Alex"

    Scaffold(
        containerColor = Dojo.Bg,
        bottomBar = { DojoBottomBar("dashboard", onNavigate) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            HomeHeader(name)
            ActivityOverviewCard()

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionHeader("Recommended for you", "View All") { onNavigate("latihan") }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    RecommendedCard(
                        title = "Muay Thai Sparring",
                        meta = "35 min • High Intensity",
                        tag = "Muay Thai",
                        colors = listOf(Color(0xFF3A1217), Color(0xFF0B0B0F)),
                        modifier = Modifier.weight(1f),
                        onClick = { onOpenWorkout("striking") }
                    )
                    RecommendedCard(
                        title = "Shadow Combinations",
                        meta = "20 min • Footwork Focus",
                        tag = "Drills",
                        colors = listOf(Color(0xFF2A2314), Color(0xFF0B0B0F)),
                        modifier = Modifier.weight(1f),
                        onClick = { onOpenWorkout("shadow") }
                    )
                }
            }

            DailyTargetCard(punches = 6842, goal = 10000)

            Button(
                onClick = onStartTraining,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Dojo.Red)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text("MULAI LATIHAN", color = Color.White, fontFamily = Dojo.Mono, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun HomeHeader(name: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text("Good morning, $name!", color = Dojo.Text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(2.dp))
            Text("Ready to crush your targets today?", color = Dojo.Muted, fontSize = 12.sp)
        }
        IconBadge(Icons.Default.Notifications)
    }
}

@Composable
private fun ActivityOverviewCard() {
    DojoCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            MonoLabel("ACTIVITY OVERVIEW", color = Dojo.Text, size = 11, bold = true)
            Box(
                Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Dojo.SurfaceAlt)
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) { MonoLabel("This Week", size = 9) }
        }
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            ProgressRing(progress = 0.68f, size = 92.dp, stroke = 8.dp) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("68%", color = Dojo.Text, fontFamily = Dojo.Mono, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    MonoLabel("ACCURACY", size = 7)
                }
            }
            Spacer(Modifier.width(24.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OverviewRow("Sessions", "5/7")
                OverviewRow("Calories", "1,950 / 2,400")
                OverviewRow("Mat Time", "4h 25m")
            }
        }
    }
}

@Composable
private fun OverviewRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        MonoLabel(label, size = 11)
        Text(value, color = Dojo.Text, fontFamily = Dojo.Mono, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun RecommendedCard(
    title: String,
    meta: String,
    tag: String,
    colors: List<Color>,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(Dojo.Surface)
            .border(1.dp, Dojo.Border, shape)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(78.dp)
                .background(Brush.verticalGradient(colors)),
            contentAlignment = Alignment.TopStart
        ) {
            DojoTag(tag, modifier = Modifier.padding(8.dp), color = Dojo.Amber.copy(alpha = 0.9f))
        }
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, color = Dojo.Text, fontSize = 13.sp, fontWeight = FontWeight.Bold, lineHeight = 17.sp)
            Spacer(Modifier.height(4.dp))
            MonoLabel(meta, size = 9)
        }
    }
}

@Composable
private fun DailyTargetCard(punches: Int, goal: Int) {
    DojoCard {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            MonoLabel("TODAY'S TARGET: PUNCHES", size = 10)
            Text(
                "%,d / %,d".format(punches, goal),
                color = Dojo.Red,
                fontFamily = Dojo.Mono,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(10.dp))
        DojoProgressBar(punches.toFloat() / goal)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0B0F)
@Composable
fun DashboardPreview() {
    DojoPulseTheme { DashboardPage() }
}
