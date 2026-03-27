package movility.hackaton

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkRouteColorScheme = darkColorScheme(
    primary = Color(0xFF7CC3FF),
    onPrimary = Color(0xFF002E4E),
    primaryContainer = Color(0xFF00446F),
    onPrimaryContainer = Color(0xFFD3E9FF),
    secondary = Color(0xFFB6C9DB),
    onSecondary = Color(0xFF203342),
    secondaryContainer = Color(0xFF364A59),
    onSecondaryContainer = Color(0xFFD2E5F8),
    tertiary = Color(0xFFD8BDE4),
    onTertiary = Color(0xFF3B2846),
    tertiaryContainer = Color(0xFF533F5F),
    onTertiaryContainer = Color(0xFFF5D8FF),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF101417),
    onBackground = Color(0xFFE0E3E7),
    surface = Color(0xFF101417),
    onSurface = Color(0xFFE0E3E7),
    surfaceVariant = Color(0xFF3F484F),
    onSurfaceVariant = Color(0xFFBEC8D0),
    outline = Color(0xFF88929A),
)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkRouteColorScheme,
        typography = Typography(),
        content = content,
    )
}

