package amba.dojo.pulse

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SportsKabaddi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import amba.dojo.pulse.ui.theme.*

private data class Discipline(
    val key: String,
    val title: String,
    val category: String,
    val blurb: String,
    val colors: List<Color>
)

private val Disciplines = listOf(
    Discipline(
        "boxing", "Boxing", "Boxing",
        "Combat stance, guard and punch trajectories with AI form validation.",
        listOf(Color(0xFF5A1A1F), Color(0xFF14080A))
    ),
    Discipline(
        "swim", "Swim", "Swim",
        "Aquatic endurance sets focused on stroke efficiency, breathing and pace.",
        listOf(Color(0xFF0E4A5C), Color(0xFF071318))
    ),
    Discipline(
        "gym", "Gymnastics", "Gymnastics",
        "Strength, balance and flexibility circuits for explosive body control.",
        listOf(Color(0xFF5C3B14), Color(0xFF140E07))
    )
)

/** "Latihan" - browsable list of disciplines. */
@Composable
fun LatihanScreen(
    onNavigate: (String) -> Unit = {},
    onOpenTutorial: () -> Unit = {},
    onOpenWorkout: (String) -> Unit = {},
    onOpenArenas: () -> Unit = {}
) {
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Semua") }
    val categories = listOf("Semua") + Disciplines.map { it.category }

    val filtered = Disciplines.filter {
        (category == "Semua" || it.category == category) &&
            it.title.contains(query.trim(), ignoreCase = true)
    }

    Scaffold(
        containerColor = Dojo.Bg,
        bottomBar = { DojoBottomBar("latihan", onNavigate) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Latihan", color = Dojo.Text, fontSize = 22.sp, fontWeight = FontWeight.Bold)

            val searchShape = RoundedCornerShape(10.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(searchShape)
                    .background(Dojo.Surface)
                    .border(1.dp, Dojo.Border, searchShape)
                    .padding(horizontal = 12.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    if (query.isEmpty()) MonoLabel("Search Combos...", size = 11)
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        textStyle = TextStyle(color = Dojo.Text, fontSize = 12.sp, fontFamily = Dojo.Mono),
                        cursorBrush = SolidColor(Dojo.Red),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Icon(Icons.Default.Search, contentDescription = null, tint = Dojo.Muted, modifier = Modifier.size(16.dp))
            }

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categories) { c ->
                    DojoChip(c, selected = c == category) { category = c }
                }
            }

            filtered.forEach { d ->
                DisciplineCard(d) {
                    if (d.key == "boxing") onOpenTutorial() else onOpenWorkout(d.key)
                }
            }
            if (filtered.isEmpty()) {
                MonoLabel("No drills match your search.", size = 11, modifier = Modifier.padding(vertical = 24.dp))
            }

            DojoCard(onClick = onOpenArenas) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(Icons.Default.SportsKabaddi)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Arena AR", color = Dojo.Text, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        MonoLabel("Guided bout with live telemetry", size = 9)
                    }
                    DojoTag("NEW")
                }
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun DisciplineCard(d: Discipline, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(168.dp)
            .clip(shape)
            .border(1.dp, Dojo.Border, shape)
            .background(Brush.verticalGradient(d.colors))
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xCC000000))))
        )
        Column(
            modifier = Modifier.align(Alignment.BottomStart).padding(14.dp).fillMaxWidth(0.82f)
        ) {
            Text(d.title, color = Dojo.Text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(3.dp))
            MonoLabel(d.blurb, size = 9, color = Color(0xFFC9C9D1))
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(14.dp)
                .size(34.dp)
                .clip(CircleShape)
                .background(Dojo.Red),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = "Open", tint = Color.White, modifier = Modifier.size(18.dp))
        }
    }
}

private data class Drill(val title: String, val meta: String)

private data class WorkoutInfo(
    val title: String,
    val series: String,
    val coach: String,
    val level: String,
    val minutes: Int,
    val kcal: Int,
    val drills: List<Drill>
)

private fun workoutFor(id: String): WorkoutInfo = when (id) {
    "shadow" -> WorkoutInfo(
        "Shadow Combinations", "Footwork Flow Series", "Pro Trainer", "Beginner", 20, 190,
        listOf(
            Drill("Light Bounce Warm-up", "2 Rounds • 2:00 min each"),
            Drill("Jab-Cross Shadow", "3 Rounds • Hand speed"),
            Drill("Pivot & Slip Flow", "3 Rounds • Footwork focus"),
            Drill("Level Change Combos", "2 Rounds • Head movement"),
            Drill("Cooldown Mobility", "5 min • Recovery")
        )
    )
    else -> WorkoutInfo(
        "Heavy Bag Power Series", "Striking & Stance Power", "Coach Miller", "Intermediate", 32, 380,
        listOf(
            Drill("Shadow Boxing (Jab-Cross-Hook)", "3 Rounds • 3:00 min each"),
            Drill("Heavy Bag Combinations", "3 Rounds • Kinetic force focus"),
            Drill("Double-End Bag Reflex", "2 Rounds • Head movement"),
            Drill("Speed Bag Rhythms", "2 Rounds • Shoulder endurance"),
            Drill("Rotational Core Burnout", "5 min • Power transfer")
        )
    )
}

/** Workout detail with the drill sequence and a start button. */
@Composable
fun WorkoutDetailScreen(
    workoutId: String = "striking",
    onNavigateBack: () -> Unit = {},
    onStartRound: () -> Unit = {}
) {
    val info = workoutFor(workoutId)
    Scaffold(containerColor = Dojo.Bg) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.clickable { onNavigateBack() }) {
                    IconBadge(Icons.Default.ArrowBackIosNew, size = 34.dp)
                }
                Text(info.series, color = Dojo.Text, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                IconBadge(Icons.Default.MoreHoriz, size = 34.dp)
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                val shape = RoundedCornerShape(14.dp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .clip(shape)
                        .background(Brush.verticalGradient(listOf(Color(0xFF5A1A1F), Color(0xFF0B0B0F))))
                ) {
                    Column(modifier = Modifier.align(Alignment.BottomStart).padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            DojoTag("COACH ${info.coach.substringAfterLast(' ').uppercase()}")
                            Spacer(Modifier.width(8.dp))
                            MonoLabel(info.coach, size = 9, color = Dojo.Amber)
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(info.title, color = Dojo.Text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    MonoLabel("${info.minutes} min", size = 11, color = Dojo.Text)
                    MonoLabel(info.level, size = 11, color = Dojo.Text)
                    MonoLabel("${info.kcal} Cal", size = 11, color = Dojo.Amber)
                }

                MonoLabel("COMBAT DRILLS SEQUENCE (${info.drills.size} ROUNDS)", size = 10, bold = true)

                info.drills.forEachIndexed { index, drill ->
                    DrillRow(index + 1, drill, done = index == 0, next = index == 1)
                }
                Spacer(Modifier.height(4.dp))
            }

            Button(
                onClick = onStartRound,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Dojo.Red)
            ) {
                Text("MULAI ROUND  →", color = Color.White, fontFamily = Dojo.Mono, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }
        }
    }
}

@Composable
private fun DrillRow(index: Int, drill: Drill, done: Boolean, next: Boolean) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (done) Dojo.RedDeep else Dojo.Surface)
            .border(1.dp, if (done) Dojo.Red.copy(alpha = 0.6f) else Dojo.Border, shape)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(if (done) Dojo.Red else Dojo.SurfaceAlt),
            contentAlignment = Alignment.Center
        ) {
            if (done) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
            } else {
                MonoLabel("$index", size = 10, color = Dojo.Muted)
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("$index. ${drill.title}", color = Dojo.Text, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(2.dp))
            MonoLabel(drill.meta, size = 9)
        }
        if (done) MonoLabel("DONE", size = 9, color = Dojo.Red, bold = true)
        else if (next) MonoLabel("NEXT", size = 9, color = Dojo.Muted)
    }
}
