package com.worksched.ui.theme

// Modified for WorkPause: restrained Galaxy-inspired surfaces with native system typography.
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun WorkSchedTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) {
        darkColorScheme(
            primary = Color(0xFF9DBDFF),
            onPrimary = Color(0xFF092B62),
            background = Color(0xFF09090B),
            surface = Color(0xFF19191D),
            surfaceContainerLow = Color(0xFF19191D),
            surfaceContainerHighest = Color(0xFF29292F),
            onSurface = Color(0xFFF3F3F5),
            onSurfaceVariant = Color(0xFFB9BAC3),
            outlineVariant = Color(0xFF3A3B43)
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF2B5BB6),
            onPrimary = Color.White,
            background = Color(0xFFF5F5F7),
            surface = Color.White,
            surfaceContainerLow = Color.White,
            surfaceContainerHighest = Color(0xFFF0F0F4),
            onSurface = Color(0xFF16171B),
            onSurfaceVariant = Color(0xFF646670),
            outlineVariant = Color(0xFFDFE0E5)
        )
    }
    MaterialTheme(
        colorScheme = colors,
        shapes = Shapes(
            extraSmall = RoundedCornerShape(8.dp),
            small = RoundedCornerShape(12.dp),
            medium = RoundedCornerShape(18.dp),
            large = RoundedCornerShape(24.dp),
            extraLarge = RoundedCornerShape(28.dp)
        ),
        content = content
    )
}
