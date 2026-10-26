package amba.dojo.pulse.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** DojoPulse "tactical telemetry" design tokens. */
object Dojo {
    val Bg = Color(0xFF0B0B0F)
    val Surface = Color(0xFF141418)
    val SurfaceAlt = Color(0xFF1A1A20)
    val Border = Color(0xFF26262E)
    val Red = Color(0xFFE23A3A)
    val RedSoft = Color(0xFFFF6B6B)
    val RedDeep = Color(0xFF3A1217)
    val Text = Color(0xFFF2F2F4)
    val Muted = Color(0xFF8A8A94)
    val Dim = Color(0xFF5C5C66)
    val Amber = Color(0xFFFFB74D)
    val Mono = FontFamily.Monospace
}

/** Small uppercase monospaced caption used across the telemetry UI. */
@Composable
fun MonoLabel(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Dojo.Muted,
    size: Int = 10,
    bold: Boolean = false
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        fontFamily = Dojo.Mono,
        fontSize = size.sp,
        letterSpacing = 0.8.sp,
        fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal
    )
}

@Composable
fun DojoCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    padding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(14.dp)
    val base = modifier
        .clip(shape)
        .background(Dojo.Surface)
        .border(BorderStroke(1.dp, Dojo.Border), shape)
    val clickable = if (onClick != null) base.clickable { onClick() } else base
    Column(modifier = clickable.padding(padding), content = content)
}

@Composable
fun DojoChip(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val shape = RoundedCornerShape(50)
    Box(
        modifier = modifier
            .clip(shape)
            .background(if (selected) Dojo.Red else Dojo.Surface)
            .border(BorderStroke(1.dp, if (selected) Dojo.Red else Dojo.Border), shape)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(
            text,
            color = if (selected) Color.White else Dojo.Muted,
            fontFamily = Dojo.Mono,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun DojoTag(text: String, modifier: Modifier = Modifier, color: Color = Dojo.Red) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(text, color = Color.White, fontFamily = Dojo.Mono, fontSize = 8.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SectionHeader(title: String, action: String? = null, onAction: () -> Unit = {}) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title.uppercase(),
            color = Dojo.Text,
            fontFamily = Dojo.Mono,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        if (action != null) {
            Text(
                action,
                color = Dojo.Red,
                fontFamily = Dojo.Mono,
                fontSize = 10.sp,
                modifier = Modifier.clickable { onAction() }
            )
        }
    }
}

@Composable
fun DojoProgressBar(progress: Float, modifier: Modifier = Modifier, height: Dp = 4.dp) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(50))
            .background(Dojo.Border)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .fillMaxHeight()
                .background(Dojo.Red)
        )
    }
}

@Composable
fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 84.dp,
    stroke: Dp = 7.dp,
    content: @Composable () -> Unit = {}
) {
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val s = stroke.toPx()
            val inset = s / 2
            val arcSize = Size(this.size.width - s, this.size.height - s)
            drawArc(
                color = Dojo.Border,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = s)
            )
            drawArc(
                color = Dojo.Red,
                startAngle = -90f,
                sweepAngle = 360f * progress.coerceIn(0f, 1f),
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = s, cap = StrokeCap.Round)
            )
        }
        content()
    }
}

@Composable
fun StatTile(label: String, value: String, modifier: Modifier = Modifier, valueColor: Color = Dojo.Text) {
    DojoCard(modifier = modifier, padding = 12.dp) {
        MonoLabel(label, size = 8)
        Spacer(Modifier.height(4.dp))
        Text(value, color = valueColor, fontFamily = Dojo.Mono, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun IconBadge(icon: ImageVector, modifier: Modifier = Modifier, size: Dp = 36.dp) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Dojo.Surface)
            .border(BorderStroke(1.dp, Dojo.Border), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = Dojo.Text, modifier = Modifier.size(size * 0.5f))
    }
}

private data class NavEntry(val route: String, val label: String, val icon: ImageVector)

private val NavEntries = listOf(
    NavEntry("dashboard", "Beranda", Icons.Default.Home),
    NavEntry("latihan", "Latihan", Icons.Default.FitnessCenter),
    NavEntry("progres", "Progres", Icons.Default.BarChart),
    NavEntry("aktivitas", "Aktivitas", Icons.Default.DirectionsRun),
    NavEntry("profil", "Profil", Icons.Default.Person)
)

@Composable
fun DojoBottomBar(selectedRoute: String, onNavigate: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Dojo.Bg)
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(Dojo.Border))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(62.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavEntries.forEach { entry ->
                val selected = entry.route == selectedRoute
                val tint = if (selected) Dojo.Red else Dojo.Muted
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { if (!selected) onNavigate(entry.route) }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(entry.icon, contentDescription = entry.label, tint = tint, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.height(3.dp))
                    Text(
                        entry.label,
                        color = tint,
                        fontFamily = Dojo.Mono,
                        fontSize = 9.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}
