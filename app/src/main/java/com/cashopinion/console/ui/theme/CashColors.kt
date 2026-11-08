package com.cashopinion.console.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

@Immutable
data class ConsoleColors(
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val primary: Color,
    val secondary: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val border: Color,
    val success: Color,
    val warning: Color,
    val error: Color,
    val isDark: Boolean
) {
    val goldButtonGradient: Brush
        get() = Brush.horizontalGradient(
            listOf(primary, secondary)
        )
}

object ThemePalettes {
    val Dark = ConsoleColors(
        background = Color(0xFF0C0D12),
        surface = Color(0xFF151720),
        surfaceVariant = Color(0xFF1E212D),
        primary = Color(0xFFFFB800),
        secondary = Color(0xFFFF9500),
        textPrimary = Color(0xFFF8FAFC),
        textSecondary = Color(0xFF94A3B8),
        border = Color(0xFF2D261E),
        success = Color(0xFF10B981),
        warning = Color(0xFFF59E0B),
        error = Color(0xFFEF4444),
        isDark = true
    )

    val Light = ConsoleColors(
        background = Color(0xFFF8FAFC),
        surface = Color(0xFFFFFFFF),
        surfaceVariant = Color(0xFFF1F5F9),
        primary = Color(0xFFFFB800),
        secondary = Color(0xFFFF9500),
        textPrimary = Color(0xFF0F172A),
        textSecondary = Color(0xFF64748B),
        border = Color(0xFFE2E8F0),
        success = Color(0xFF10B981),
        warning = Color(0xFFF59E0B),
        error = Color(0xFFEF4444),
        isDark = false
    )
}

object CashColors {
    val GoldPrimary = Color(0xFFFFB800)
    val AmberSecondary = Color(0xFFFF9500)
    val GoldSecondary = Color(0xFFFF9500)
    val Dark = ThemePalettes.Dark
    val Light = ThemePalettes.Light
}
