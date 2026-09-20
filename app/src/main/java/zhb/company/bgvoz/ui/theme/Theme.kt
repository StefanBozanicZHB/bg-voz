package zhb.company.bgvoz.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

internal val DarkColors = darkColorScheme(
    primary = Blue80,
    onPrimary = OnBlue80,
    primaryContainer = BlueContainerDark,
    onPrimaryContainer = OnBlueContainerDark,
    secondary = Steel80,
    onSecondary = OnSteel80,
    secondaryContainer = SteelContainerDark,
    onSecondaryContainer = OnSteelContainerDark,
    tertiary = Steel80,
    error = Red80,
    onError = OnRed80,
    errorContainer = RedContainerDark,
    onErrorContainer = OnRedContainerDark,
)

internal val LightColors = lightColorScheme(
    primary = Blue40,
    onPrimary = Color.White,
    primaryContainer = BlueContainerLight,
    onPrimaryContainer = OnBlueContainerLight,
    secondary = Steel40,
    onSecondary = Color.White,
    secondaryContainer = SteelContainerLight,
    onSecondaryContainer = OnSteelContainerLight,
    tertiary = Steel40,
    error = Red40,
    onError = Color.White,
    errorContainer = RedContainerLight,
    onErrorContainer = OnRedContainerLight,
)

/**
 * Fiksna paleta (bez dynamic color) jer boje ovde nose značenje:
 * plava = sledeći voz, crvena = upozorenje.
 */
@Composable
fun BgVozTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography,
        content = content,
    )
}
