package amba.dojo.pulse.ui.theme

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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

val BgSurface = Color(0xFF131314)
val SurfaceLow = Color(0xFF1b1b1c)
val SurfaceContainer = Color(0xFF1f2020)
val SurfaceHigh = Color(0xFF2a2a2a)
val Primary = Color(0xFFfab4b8)
val PrimaryContainer = Color(0xFF381015)
val Secondary = Color(0xFFf5b6be)
val SecondaryContainer = Color(0xFF693c43)
val OnSurface = Color(0xFFe4e2e2)
val OnSurfaceVar = Color(0xFFd6c2c2)
val ErrorColor = Color(0xFFffb4ab)
val OutlineVar = Color(0xFF524344)

@Composable
fun ArenasScreen() {
    var currentStage by remember { mutableIntStateOf(0) }

    if (currentStage == 0) {
        ArenaDashboardContent(onStartAR = { currentStage = 1 })
    } else {
        ARTrainingFlowManager(
            stage = currentStage,
            onNext = { if (currentStage < 4) currentStage++ else currentStage = 0 },
            onClose = { currentStage = 0 }
        )
    }
}

@Composable
fun ARTrainingFlowManager(stage: Int, onNext: () -> Unit, onClose: () -> Unit) {
    Scaffold(
        topBar = {
            Surface(color = BgSurface.copy(alpha = 0.95f), modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.height(64.dp).padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { onClose() }) {
                        Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Back", tint = OnSurface, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (stage == 4) "BOUT DEBRIEF ANALYSIS" else "ACTIVE BOUT TELEMETRY",
                            fontSize = 16.sp, fontWeight = FontWeight.Bold, color = OnSurface
                        )
                    }
                    Box(modifier = Modifier.size(32.dp).background(Primary, CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF4f2227), modifier = Modifier.size(18.dp))
                    }
                }
            }
        },
        containerColor = BgSurface
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when (stage) {
                1 -> ARWayfindingStage(onNext)
                2 -> CalibrationStage(onNext)
                3 -> ActiveTelemetryStage(onNext)
                4 -> DebriefStage(onNext)
            }
        }
    }
}

@Composable
fun ARWayfindingStage(onNext: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(BgSurface)) {
        Row(modifier = Modifier.fillMaxWidth().background(SurfaceLow).padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(modifier = Modifier.size(6.dp).background(Primary))
                Text("PANDUAN LOKASI MATRAS", fontSize = 10.sp, color = Secondary, fontWeight = FontWeight.Bold)
            }
            Text("VPS SYNC: 99.4%", fontSize = 10.sp, color = Primary, fontWeight = FontWeight.Bold, modifier = Modifier.background(PrimaryContainer).padding(4.dp))
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth().background(Color(0xFF0A0A0A)), contentAlignment = Alignment.Center) {
            Column(modifier = Modifier.align(Alignment.TopCenter).padding(16.dp).fillMaxWidth().background(BgSurface.copy(alpha=0.9f)).padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(36.dp).background(Primary), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.TurnLeft, contentDescription = null, tint = Color(0xFF4f2227))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("NAVIGASI LANGKAH 02/03", fontSize = 10.sp, color = Secondary)
                        Text("Belok kiri melewati Ring Boxing", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = OnSurface)
                        Text("Menuju Matras Zona B // Station 02", fontSize = 12.sp, color = OnSurfaceVar)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("4.2 M", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Primary)
                        Text("JARAK", fontSize = 10.sp, color = OnSurfaceVar)
                    }
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("TARGET: [STN.02] HEAVY BAG", fontSize = 12.sp, color = OnSurface, fontWeight = FontWeight.Bold, modifier = Modifier.background(PrimaryContainer).padding(8.dp))
                Box(modifier = Modifier.width(2.dp).height(40.dp).background(Primary))
                Box(modifier = Modifier.size(12.dp).background(Primary))
            }
        }

        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(modifier = Modifier.fillMaxWidth().background(SurfaceLow).padding(16.dp)) {
                Text("HEAVY BAG & AGILITY GRID", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = OnSurface)
                Text("Campus Martial Arts Hall • Lantai 2 Combat Lab", fontSize = 12.sp, color = OnSurfaceVar)
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("SISA JARAK", fontSize = 10.sp, color = OnSurfaceVar)
                        Text("12 M", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Primary)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("ESTIMASI TIBA", fontSize = 10.sp, color = OnSurfaceVar)
                        Text("15 DETIK", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Secondary)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("SENSOR CAHAYA", fontSize = 10.sp, color = OnSurfaceVar)
                        Text("OPTIMAL", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = OnSurface, modifier = Modifier.padding(top=4.dp))
                    }
                }
            }

            Button(
                onClick = onNext,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text("SAYA SUDAH DI MATRAS (KALIBRASI) ->", color = Color(0xFF4f2227), fontWeight = FontWeight.Bold)
            }

            Text("LEWATI PANDUAN AR (LANGSUNG DOCKING)", fontSize = 12.sp, color = OnSurfaceVar, modifier = Modifier.fillMaxWidth().clickable(onClick = onNext), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun CalibrationStage(onNext: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(BgSurface).verticalScroll(rememberScrollState())) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("KALIBRASI & DOCKING", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = OnSurface)
            Text("Letakkan HP di permukaan stabil setinggi 1-1.2m, hadapkan ke area latihan.", fontSize = 12.sp, color = OnSurfaceVar)

            Box(modifier = Modifier.fillMaxWidth().height(300.dp).background(Color(0xFF101010)).border(1.dp, OutlineVar), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.AccessibilityNew, contentDescription = null, tint = Primary, modifier = Modifier.size(120.dp))
                Text("POSE CALIBRATED", fontSize = 10.sp, color = Primary, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp).background(PrimaryContainer).padding(4.dp))
            }

            ChecklistItem("Ketinggian Dok 1.0 - 1.2 Meter", "Sudut kemiringan 0° sejajar dada")
            ChecklistItem("Pencahayaan Area Tatami", "Kontras siluet tubuh optimal")

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onNext,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                shape = RoundedCornerShape(4.dp)
            ) {
                Icon(Icons.Default.SportsMartialArts, contentDescription = null, tint = Color(0xFF4f2227))
                Spacer(modifier = Modifier.width(8.dp))
                Text("MULAI DRILL (HITUNG MUNDUR 5 DETIK)", color = Color(0xFF4f2227), fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun ChecklistItem(title: String, desc: String) {
    Row(modifier = Modifier.fillMaxWidth().background(SurfaceLow).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Primary, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = OnSurface)
            Text(desc, fontSize = 10.sp, color = OnSurfaceVar)
        }
    }
}

@Composable
fun ActiveTelemetryStage(onNext: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(BgSurface).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth().background(SurfaceLow).padding(16.dp).border(1.dp, OutlineVar, RoundedCornerShape(8.dp)), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("TOTAL PUKULAN", fontSize = 10.sp, color = OnSurfaceVar, fontWeight = FontWeight.Bold)
                Text("14 / 20", fontSize = 24.sp, color = Primary, fontWeight = FontWeight.Bold)
            }
            Box(modifier = Modifier.width(1.dp).height(30.dp).background(OutlineVar))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("DURASI", fontSize = 10.sp, color = OnSurfaceVar, fontWeight = FontWeight.Bold)
                Text("02:45", fontSize = 24.sp, color = OnSurface, fontWeight = FontWeight.Bold)
            }
            Box(modifier = Modifier.width(1.dp).height(30.dp).background(OutlineVar))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("FORM AKURASI", fontSize = 10.sp, color = OnSurfaceVar, fontWeight = FontWeight.Bold)
                Text("96%", fontSize = 24.sp, color = Primary, fontWeight = FontWeight.Bold)
            }
        }

        Row(modifier = Modifier.fillMaxWidth().background(SecondaryContainer).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(40.dp).background(Primary, CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.VolumeUp, contentDescription = null, tint = Color(0xFF4f2227))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text("AUDIO TTS AKTIF", fontSize = 10.sp, color = Primary, fontWeight = FontWeight.Bold)
                Text("\"Jaga dagu tetap rapat & siku terkunci!\"", fontSize = 14.sp, color = OnSurface, fontWeight = FontWeight.Bold)
            }
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth().background(Color(0xFF0F0F0F)).border(1.dp, OutlineVar, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.AccessibilityNew, contentDescription = null, tint = OnSurfaceVar, modifier = Modifier.size(200.dp))

            Row(modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.background(SurfaceHigh.copy(alpha=0.8f)).padding(8.dp)) {
                    Text("SIKU KANAN", fontSize = 10.sp, color = OnSurfaceVar)
                    Text("42° OPTIMAL", fontSize = 16.sp, color = Primary, fontWeight = FontWeight.Bold)
                }
                Column(modifier = Modifier.background(SurfaceHigh.copy(alpha=0.8f)).padding(8.dp), horizontalAlignment = Alignment.End) {
                    Text("ROTASI BAHU", fontSize = 10.sp, color = OnSurfaceVar)
                    Text("18° TERKUNCI", fontSize = 16.sp, color = OnSurface, fontWeight = FontWeight.Bold)
                }
            }
        }

        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Primary),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(Icons.Default.Flag, contentDescription = null, tint = Color(0xFF4f2227))
            Spacer(modifier = Modifier.width(8.dp))
            Text("SELESAI SESI (SENTUH / HANDS-FREE)", color = Color(0xFF4f2227), fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}
@Composable
fun DebriefStage(onClose: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(BgSurface).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {

        Box(modifier = Modifier.fillMaxWidth().height(220.dp).background(SurfaceLow), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(140.dp)) {
                drawArc(color = SurfaceHigh, startAngle = 0f, sweepAngle = 360f, useCenter = false, style = Stroke(width = 16f, cap = StrokeCap.Round))
                drawArc(color = Primary, startAngle = -90f, sweepAngle = 360f * 0.88f, useCenter = false, style = Stroke(width = 16f, cap = StrokeCap.Round))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("88%", fontSize = 48.sp, fontWeight = FontWeight.Bold, color = OnSurface)
                Text("FORM ACCURACY", fontSize = 10.sp, color = OnSurfaceVar, fontWeight = FontWeight.Bold)
            }
            Text("TINGKAT A: ELITE STRIKER", fontSize = 12.sp, color = Primary, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom=16.dp))
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DebriefStatCard(modifier = Modifier.weight(1f), title = "TARGET HIT", value = "20/20", sub = "100% EFISIENSI")
            DebriefStatCard(modifier = Modifier.weight(1f), title = "FORM FAULTS", value = "02", sub = "Elbow Flare", isAlert = true)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DebriefStatCard(modifier = Modifier.weight(1f), title = "DRILL TIME", value = "08:30", sub = "Ritme Konstan")
            DebriefStatCard(modifier = Modifier.weight(1f), title = "STREAK", value = "+1 Hari", sub = "6 HARI BERUNTUN")
        }

        Column(modifier = Modifier.fillMaxWidth().background(SurfaceLow).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Primary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("GEMINI AI BREAKDOWN", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = OnSurface)
            }
            Column(modifier = Modifier.background(SurfaceContainer).padding(12.dp)) {
                Text("KEKUATAN KINETIK - OPTIMAL", fontSize = 12.sp, color = Primary, fontWeight = FontWeight.Bold)
                Text("Retensi guard solid saat jab ekstensi. Momentum bahu menyerap daya pantul dengan presisi tinggi.", fontSize = 12.sp, color = OnSurfaceVar, modifier = Modifier.padding(top=4.dp))
            }
            Column(modifier = Modifier.background(SurfaceContainer).padding(12.dp)) {
                Text("EVALUASI POSTUR - CATATAN", fontSize = 12.sp, color = Secondary, fontWeight = FontWeight.Bold)
                Text("Variasi penurunan pinggul ringan terdeteksi pada reps akhir. Pertahankan rotasi panggul tetap sejajar sumbu kaki kiri.", fontSize = 12.sp, color = OnSurfaceVar, modifier = Modifier.padding(top=4.dp))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = onClose,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Primary),
            shape = RoundedCornerShape(4.dp)
        ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4f2227))
            Spacer(modifier = Modifier.width(8.dp))
            Text("SIMPAN & KEMBALI KE BERANDA", color = Color(0xFF4f2227), fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun DebriefStatCard(modifier: Modifier = Modifier, title: String, value: String, sub: String, isAlert: Boolean = false) {
    Column(modifier = modifier.background(SurfaceLow).padding(16.dp)) {
        Text(title, fontSize = 10.sp, color = OutlineVar, fontWeight = FontWeight.Bold)
        Text(value, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = if(isAlert) Secondary else OnSurface, modifier = Modifier.padding(vertical = 4.dp))
        Text(sub, fontSize = 10.sp, color = if(isAlert) OnSurfaceVar else Primary)
    }
}
@Composable
fun ArenaDashboardContent(onStartAR: () -> Unit) {
    var selectedMode by remember { mutableStateOf("venue") }
    var isInitializing by remember { mutableStateOf(false) }
    var buttonText by remember { mutableStateOf("Mulai Panduan AR") }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            Surface(color = BgSurface.copy(alpha = 0.8f), modifier = Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars)) {
                Row(modifier = Modifier.height(64.dp).padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(modifier = Modifier.size(8.dp).background(Primary))
                        Text(text = "ARENAS", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = OnSurface, letterSpacing = (-0.5).sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(onClick = { }) { Icon(imageVector = Icons.Default.Notifications, contentDescription = "Notifications", tint = OnSurfaceVar) }
                        Box(modifier = Modifier.size(32.dp).background(Primary, shape = CircleShape), contentAlignment = Alignment.Center) {
                            Icon(imageVector = Icons.Default.Person, contentDescription = "Profile", tint = Color(0xFF4f2227), modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        },
        bottomBar = {
            Surface(color = BgSurface.copy(alpha = 0.9f), modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 4.dp), horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically) {
                    BottomNavItem(icon = Icons.Default.GridView, label = "Dashboard", isSelected = false, OnSurfaceVar)
                    BottomNavItem(icon = Icons.Default.SportsKabaddi, label = "Arenas", isSelected = true, Primary)
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { }) {
                        Box(modifier = Modifier.size(32.dp).background(PrimaryContainer), contentAlignment = Alignment.Center) {
                            Icon(imageVector = Icons.Default.Timer, contentDescription = "Training", tint = Primary, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(text = "Training", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Primary)
                    }
                    BottomNavItem(icon = Icons.Default.Assessment, label = "History", isSelected = false, OnSurfaceVar)
                    BottomNavItem(icon = Icons.Default.Person, label = "Profile", isSelected = false, OnSurfaceVar)
                }
            }
        },
        containerColor = BgSurface
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(modifier = Modifier.size(6.dp).background(Primary))
                        Text(text = "LOC.ID // DOJO-902", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = OnSurfaceVar, letterSpacing = 1.2.sp)
                    }
                    Row(modifier = Modifier.background(SurfaceHigh).padding(horizontal = 8.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(modifier = Modifier.size(6.dp).background(Secondary))
                        Text(text = "LINK OK", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Secondary)
                    }
                }
                Column(modifier = Modifier.fillMaxWidth().background(SurfaceLow).padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = "Designated Tactical Venue", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFb27579), letterSpacing = 0.8.sp)
                    Text(text = "Campus Martial Arts Hall", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = OnSurface)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(imageVector = Icons.Default.Place, contentDescription = "Pin", tint = Primary, modifier = Modifier.size(16.dp))
                        Text(text = "Tower B, Level 2 Combat Lab", fontSize = 12.sp, color = OnSurfaceVar)
                    }
                }
            }

            Column(modifier = Modifier.fillMaxWidth().background(PrimaryContainer).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "[STN.02]", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Primary)
                        Text(text = "Optical Grid A2", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Primary)
                    }
                    Row(modifier = Modifier.background(SecondaryContainer).padding(horizontal = 8.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.size(6.dp).background(Primary))
                        Text(text = "Dipilih", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFe5a9b0))
                    }
                }
                Text(text = "Heavy Bag & Stance Mat", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Primary)
            }

            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Button(
                    onClick = onStartAR, // MEMICU AR TRAINING FLOW
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SecondaryContainer),
                    shape = RoundedCornerShape(0.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(imageVector = Icons.Default.Visibility, contentDescription = null, tint = Color(0xFF4c242b))
                        Text(text = buttonText, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4c242b))
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun BottomNavItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, isSelected: Boolean, tintColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { }.padding(horizontal = 8.dp)) {
        Icon(imageVector = icon, contentDescription = label, tint = tintColor, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = label, fontSize = 10.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, color = tintColor)
    }
}