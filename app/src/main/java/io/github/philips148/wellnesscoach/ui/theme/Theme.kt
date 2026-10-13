package io.github.philips148.wellnesscoach.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val WellnessColorScheme = lightColorScheme(
    // Main buttons and their text.
    primary = WellnessNavy,
    onPrimary = WellnessWhite,
    primaryContainer = WellnessLightBlue,
    onPrimaryContainer = WellnessNavy,

    // Supporting accents.
    secondary = WellnessLightBlue,
    onSecondary = WellnessNavy,
    secondaryContainer = WellnessLightBlue,
    onSecondaryContainer = WellnessNavy,

    // Green accent for progress indicators.
    tertiary = WellnessGreen,
    onTertiary = WellnessText,
    tertiaryContainer = WellnessLightGray,
    onTertiaryContainer = WellnessText,

    // Screen backgrounds and text.
    background = WellnessWhite,
    onBackground = WellnessText,
    surface = WellnessWhite,
    onSurface = WellnessText,
    surfaceTint = WellnessNavy,

    // Fields, cards, and secondary text.
    surfaceContainerLowest = WellnessWhite,
    surfaceContainerLow = WellnessLightGray,
    surfaceContainer = WellnessLightGray,
    surfaceContainerHigh = WellnessLightGray,
    surfaceContainerHighest = WellnessLightGray,
    onSurfaceVariant = WellnessMutedText,

    // Borders and errors.
    outline = WellnessBorder,
    outlineVariant = WellnessBorder,
    error = WellnessRed,
    onError = WellnessWhite,
    errorContainer = WellnessLightGray,
    onErrorContainer = WellnessRed
)

@Composable
fun HealthAndWellnessCoachTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = WellnessColorScheme,
        typography = Typography,
        content = content
    )
}