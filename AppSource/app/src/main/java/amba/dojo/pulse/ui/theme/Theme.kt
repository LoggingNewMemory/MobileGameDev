package amba.dojo.pulse.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DojoColorScheme = darkColorScheme(
    primary = Dojo.Red,
    onPrimary = Color.White,
    secondary = Dojo.RedSoft,
    tertiary = Dojo.Amber,
    background = Dojo.Bg,
    onBackground = Dojo.Text,
    surface = Dojo.Surface,
    onSurface = Dojo.Text,
    surfaceVariant = Dojo.SurfaceAlt,
    onSurfaceVariant = Dojo.Muted,
    outline = Dojo.Border
)

@Composable
fun DojoPulseTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DojoColorScheme,
        typography = Typography,
        content = content
    )
}
