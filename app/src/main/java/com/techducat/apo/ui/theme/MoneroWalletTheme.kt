package com.techducat.apo.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun MoneroWalletTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFFFF6600),
            primaryContainer = Color(0xFFFF8833),
            secondary = Color(0xFF4D4D4D),
            tertiary = Color(0xFF4CAF50),
            background = Color(0xFF0F0F0F),
            surface = Color(0xFF1A1A1A),
            surfaceVariant = Color(0xFF252525),
            onPrimary = Color.White,
            onBackground = Color(0xFFE0E0E0),
            onSurface = Color(0xFFE0E0E0),
            error = Color(0xFFCF6679)
        ),
        content = content
    )
}
