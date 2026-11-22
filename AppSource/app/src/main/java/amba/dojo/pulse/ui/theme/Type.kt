package amba.dojo.pulse.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import amba.dojo.pulse.R

val GoogleSans = FontFamily(
    Font(R.font.googlesans_light, FontWeight.Light),
    Font(R.font.googlesans_regular, FontWeight.Normal),
    Font(R.font.googlesans_medium, FontWeight.Medium),
    Font(R.font.googlesans_semibold, FontWeight.SemiBold),
    Font(R.font.googlesans_bold, FontWeight.Bold)
)

private val defaultTypography = Typography()

// Complete Material 3 typography with standard Google Sans as the primary font family
val Typography = Typography(
    displayLarge = defaultTypography.displayLarge.copy(fontFamily = GoogleSans),
    displayMedium = defaultTypography.displayMedium.copy(fontFamily = GoogleSans),
    displaySmall = defaultTypography.displaySmall.copy(fontFamily = GoogleSans),
    headlineLarge = defaultTypography.headlineLarge.copy(fontFamily = GoogleSans),
    headlineMedium = defaultTypography.headlineMedium.copy(fontFamily = GoogleSans),
    headlineSmall = defaultTypography.headlineSmall.copy(fontFamily = GoogleSans),
    titleLarge = defaultTypography.titleLarge.copy(fontFamily = GoogleSans),
    titleMedium = defaultTypography.titleMedium.copy(fontFamily = GoogleSans),
    titleSmall = defaultTypography.titleSmall.copy(fontFamily = GoogleSans),
    bodyLarge = defaultTypography.bodyLarge.copy(fontFamily = GoogleSans),
    bodyMedium = defaultTypography.bodyMedium.copy(fontFamily = GoogleSans),
    bodySmall = defaultTypography.bodySmall.copy(fontFamily = GoogleSans),
    labelLarge = defaultTypography.labelLarge.copy(fontFamily = GoogleSans),
    labelMedium = defaultTypography.labelMedium.copy(fontFamily = GoogleSans),
    labelSmall = defaultTypography.labelSmall.copy(fontFamily = GoogleSans)
)
