package com.example.communityservice.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = Emerald800,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCFCE7),
    onPrimaryContainer = Emerald950,
    secondary = Amber500,
    onSecondary = Color.White,
    background = Color.White,
    surface = Slate50,
    onBackground = Slate900,
    onSurface = Slate800,
    error = RedUrgent
)

@Composable
fun CommunityServiceTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography(),
        content = content
    )
}
