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
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
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
            text = text,
            color = if (selected) Color.White else Dojo.Text,
            fontFamily = Dojo.Mono,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun DojoTag(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Dojo.Red
) {
    val shape = RoundedCornerShape(4.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(color.copy(alpha = 0.15f))
            .border(BorderStroke(1.dp, color.copy(alpha = 0.5f)), shape)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            color = color,
            fontFamily = Dojo.Mono,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun StatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = Dojo.Text
) {
    DojoCard(modifier = modifier, padding = 12.dp) {
        MonoLabel(label, size = 9)
        Spacer(Modifier.height(4.dp))
        Text(
            text = value,
            color = valueColor,
            fontFamily = Dojo.Mono,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun IconBadge(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    onClick: () -> Unit = {}
) {
    val shape = CircleShape
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(Dojo.SurfaceAlt)
            .border(BorderStroke(1.dp, Dojo.Border), shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Dojo.Text,
            modifier = Modifier.size(size * 0.5f)
        )
    }
}

@Composable
fun SectionHeader(
    title: String,
    actionText: String? = null,
    modifier: Modifier = Modifier,
    onAction: () -> Unit = {}
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = Dojo.Text,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        if (actionText != null) {
            Text(
                text = actionText,
                color = Dojo.Red,
                fontFamily = Dojo.Mono,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onAction() }
            )
        }
    }
}

@Composable
fun DojoProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    height: Dp = 8.dp,
    color: Color = Dojo.Red,
    trackColor: Color = Dojo.SurfaceAlt
) {
    val shape = RoundedCornerShape(50)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(shape)
            .background(trackColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .clip(shape)
                .background(color)
        )
    }
}

@Composable
fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 92.dp,
    stroke: Dp = 8.dp,
    color: Color = Dojo.Red,
    trackColor: Color = Dojo.SurfaceAlt,
    content: @Composable BoxScope.() -> Unit = {}
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = stroke.toPx()
            val arcSize = Size(size.toPx() - strokePx, size.toPx() - strokePx)
            val topLeft = Offset(strokePx / 2, strokePx / 2)

            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f * progress.coerceIn(0f, 1f),
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
        }
        content()
    }
}

private data class DojoNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

@Composable
fun DojoBottomBar(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        DojoNavItem("dashboard", "Beranda", Icons.Default.Home),
        DojoNavItem("latihan", "Latihan", Icons.Default.FitnessCenter),
        DojoNavItem("progres", "Progres", Icons.Default.BarChart),
        DojoNavItem("market", "Market", Icons.Default.ShoppingCart),
        DojoNavItem("profil", "Profil", Icons.Default.Person)
    )

    Surface(
        color = Dojo.Surface,
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .border(BorderStroke(1.dp, Dojo.Border))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val isSelected = currentRoute == item.route || (item.route == "progres" && currentRoute == "aktivitas")
                val tint = if (isSelected) Dojo.Red else Dojo.Muted
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { onNavigate(item.route) }
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        tint = tint,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = item.label,
                        color = tint,
                        fontFamily = Dojo.Mono,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}
