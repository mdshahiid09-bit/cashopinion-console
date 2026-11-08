package com.cashopinion.console.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val LocalConsoleColors = staticCompositionLocalOf { ThemePalettes.Dark }

object CashTheme {
    val colors: ConsoleColors
        @Composable
        @ReadOnlyComposable
        get() = LocalConsoleColors.current
}

@Composable
fun CashConsoleTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val consoleColors = if (darkTheme) ThemePalettes.Dark else ThemePalettes.Light

    val m3ColorScheme = if (darkTheme) {
        darkColorScheme(
            primary = consoleColors.primary,
            secondary = consoleColors.secondary,
            background = consoleColors.background,
            surface = consoleColors.surface,
            onPrimary = Color(0xFF140E00),
            onBackground = consoleColors.textPrimary,
            onSurface = consoleColors.textPrimary
        )
    } else {
        lightColorScheme(
            primary = consoleColors.primary,
            secondary = consoleColors.secondary,
            background = consoleColors.background,
            surface = consoleColors.surface,
            onPrimary = Color(0xFF140E00),
            onBackground = consoleColors.textPrimary,
            onSurface = consoleColors.textPrimary
        )
    }

    CompositionLocalProvider(LocalConsoleColors provides consoleColors) {
        MaterialTheme(
            colorScheme = m3ColorScheme,
            typography = ConsoleTypography,
            content = content
        )
    }
}
