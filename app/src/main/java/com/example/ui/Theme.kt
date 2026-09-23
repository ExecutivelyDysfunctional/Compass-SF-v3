package com.example.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

enum class AppThemeMode(
    val id: String,
    val displayName: String,
    val subtitle: String,
    val isDark: Boolean,
    val bgHex: Color,
    val cardHex: Color
) {
    MIDNIGHT(
        id = "midnight",
        displayName = "Midnight Ink",
        subtitle = "Classic deep navy & slate (Default)",
        isDark = true,
        bgHex = Color(0xFF0A0F1A),
        cardHex = Color(0xFF0F1626)
    ),
    OLED(
        id = "oled",
        displayName = "OLED Pure Black",
        subtitle = "Pitch black background, battery saver",
        isDark = true,
        bgHex = Color(0xFF000000),
        cardHex = Color(0xFF0F0F12)
    ),
    DAYLIGHT(
        id = "daylight",
        displayName = "Daylight Fog",
        subtitle = "Crisp light theme for outdoor sunlight",
        isDark = false,
        bgHex = Color(0xFFF1F5F9),
        cardHex = Color(0xFFFFFFFF)
    ),
    SUNSET(
        id = "sunset",
        displayName = "Warm Sunset",
        subtitle = "Warm amber charcoal, reduced blue light",
        isDark = true,
        bgHex = Color(0xFF181310),
        cardHex = Color(0xFF241C18)
    ),
    PACIFIC(
        id = "pacific",
        displayName = "Pacific Marine",
        subtitle = "Deep oceanic teal & cool slate",
        isDark = true,
        bgHex = Color(0xFF07141E),
        cardHex = Color(0xFF0C2130)
    );

    companion object {
        fun fromId(id: String): AppThemeMode = values().firstOrNull { it.id == id } ?: MIDNIGHT
    }
}

enum class AppAccentColor(
    val id: String,
    val displayName: String,
    val primary: Color,
    val secondary: Color,
    // Darker, WCAG-AA-safe variant used for accent-colored TEXT/labels whenever the
    // active theme is light (Daylight Fog). The base `primary`/`secondary` tones are
    // tuned for dark surfaces and fall well under 4.5:1 on a white/light card.
    val lightSafe: Color
) {
    BEACON(
        id = "beacon",
        displayName = "Beacon Gold",
        primary = Color(0xFFFFB020),
        secondary = Color(0xFFFFC350),
        lightSafe = Color(0xFFB45309) // amber-700 — 5.0:1 on white
    ),
    GOLDEN_GATE(
        id = "golden_gate",
        displayName = "Golden Gate Rust",
        primary = Color(0xFFEA580C),
        secondary = Color(0xFFFB923C),
        lightSafe = Color(0xFFC2410C) // orange-700 — 5.2:1 on white
    ),
    PACIFIC_EMERALD(
        id = "emerald",
        displayName = "Pacific Emerald",
        primary = Color(0xFF10B981),
        secondary = Color(0xFF34D399),
        lightSafe = Color(0xFF047857) // emerald-700 — 5.5:1 on white
    ),
    OCEAN_CYAN(
        id = "cyan",
        displayName = "Ocean Cyan",
        primary = Color(0xFF06B6D4),
        secondary = Color(0xFF38BDF8),
        lightSafe = Color(0xFF0E7490) // cyan-700 — 5.4:1 on white
    ),
    MISSION_PURPLE(
        id = "purple",
        displayName = "Mission Violet",
        // Lightened from the original 0xFF8B5CF6, which read 3.9–4.6:1 on every dark
        // theme's surface (below 4.5:1 AA). This clears 4.8:1+ on all of them.
        primary = Color(0xFF9773F8),
        secondary = Color(0xFFA78BFA),
        lightSafe = Color(0xFF7C3AED) // violet-600 — 5.7:1 on white
    );

    companion object {
        fun fromId(id: String): AppAccentColor = values().firstOrNull { it.id == id } ?: BEACON
    }
}

enum class AppFontScale(
    val id: String,
    val displayName: String,
    val scaleFactor: Float,
    val description: String
) {
    STANDARD("standard", "Standard", 1.0f, "100% normal font scale"),
    LARGE("large", "Large", 1.15f, "115% larger for outdoor navigation"),
    EXTRA_LARGE("extra_large", "Extra Large", 1.30f, "130% maximum readability");

    companion object {
        fun fromId(id: String): AppFontScale = values().firstOrNull { it.id == id } ?: STANDARD
    }
}

enum class MapClusteringMode(
    val id: String,
    val displayName: String,
    val subtitle: String,
    val radiusDp: Int
) {
    TIGHT(
        id = "tight",
        displayName = "Tight",
        subtitle = "Small radius (24dp) — pins stay separated longer",
        radiusDp = 24
    ),
    BALANCED(
        id = "balanced",
        displayName = "Balanced",
        subtitle = "Optimal radius (38dp) — standard grouping (Default)",
        radiusDp = 38
    ),
    SPREAD(
        id = "spread",
        displayName = "Spread",
        subtitle = "Large radius (56dp) — groups distant pins aggressively",
        radiusDp = 56
    );

    companion object {
        fun fromId(id: String): MapClusteringMode = entries.firstOrNull { it.id == id } ?: BALANCED
    }
}

data class CompassThemeColors(
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val border: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val accent: Color,
    val accentLight: Color,
    val onAccent: Color = Color(0xFF0A0F1A),
    val success: Color = Color(0xFF10B981),
    val danger: Color = Color(0xFFEF4444),
    val isDark: Boolean = true,
    val highContrast: Boolean = false,
    val compactView: Boolean = false
)

fun resolveCompassColors(
    mode: AppThemeMode,
    accent: AppAccentColor,
    highContrast: Boolean,
    compactView: Boolean
): CompassThemeColors {
    return when (mode) {
        AppThemeMode.MIDNIGHT -> CompassThemeColors(
            background = Color(0xFF0A0F1A),
            surface = Color(0xFF0F1626),
            surfaceVariant = Color(0xFF1A2438),
            border = if (highContrast) Color(0xFF4A5C7A) else Color(0xFF26324A),
            textPrimary = if (highContrast) Color(0xFFFFFFFF) else Color(0xFFE6ECF7),
            textSecondary = if (highContrast) Color(0xFFA6B8D6) else Color(0xFF8FA0BD),
            accent = accent.primary,
            accentLight = accent.secondary,
            onAccent = Color(0xFF0A0F1A),
            success = Color(0xFF10B981),
            danger = Color(0xFFEF4444),
            isDark = true,
            highContrast = highContrast,
            compactView = compactView
        )
        AppThemeMode.OLED -> CompassThemeColors(
            background = Color(0xFF000000),
            surface = Color(0xFF0D0D11),
            surfaceVariant = Color(0xFF18181F),
            border = if (highContrast) Color(0xFF555566) else Color(0xFF2D2D38),
            textPrimary = Color(0xFFFFFFFF),
            textSecondary = Color(0xFFA1A1AA),
            accent = accent.primary,
            accentLight = accent.secondary,
            onAccent = Color(0xFF000000),
            success = Color(0xFF10B981),
            danger = Color(0xFFEF4444),
            isDark = true,
            highContrast = highContrast,
            compactView = compactView
        )
        AppThemeMode.DAYLIGHT -> CompassThemeColors(
            background = Color(0xFFF1F5F9),
            surface = Color(0xFFFFFFFF),
            surfaceVariant = Color(0xFFE2E8F0),
            border = if (highContrast) Color(0xFF64748B) else Color(0xFFCBD5E1),
            textPrimary = Color(0xFF0F172A),
            textSecondary = Color(0xFF475569),
            // Every accent (not just Beacon) needs its darker `lightSafe` tone here —
            // the bright dark-theme primaries fail AA contrast on a white/light card.
            accent = accent.lightSafe,
            accentLight = accent.lightSafe,
            onAccent = Color(0xFF0A0F1A),
            success = Color(0xFF059669),
            danger = Color(0xFFDC2626),
            isDark = false,
            highContrast = highContrast,
            compactView = compactView
        )
        AppThemeMode.SUNSET -> CompassThemeColors(
            background = Color(0xFF181310),
            surface = Color(0xFF241C18),
            surfaceVariant = Color(0xFF332722),
            border = if (highContrast) Color(0xFF6B5147) else Color(0xFF44332C),
            textPrimary = Color(0xFFFDF0EC),
            textSecondary = Color(0xFFCBB2A9),
            accent = accent.primary,
            accentLight = accent.secondary,
            onAccent = Color(0xFF181310),
            success = Color(0xFF10B981),
            danger = Color(0xFFEF4444),
            isDark = true,
            highContrast = highContrast,
            compactView = compactView
        )
        AppThemeMode.PACIFIC -> CompassThemeColors(
            background = Color(0xFF07141E),
            surface = Color(0xFF0C2130),
            surfaceVariant = Color(0xFF133247),
            border = if (highContrast) Color(0xFF2E658C) else Color(0xFF1C4460),
            textPrimary = Color(0xFFE0F2FE),
            textSecondary = Color(0xFF7DD3FC),
            accent = accent.primary,
            accentLight = accent.secondary,
            onAccent = Color(0xFF07141E),
            success = Color(0xFF10B981),
            danger = Color(0xFFEF4444),
            isDark = true,
            highContrast = highContrast,
            compactView = compactView
        )
    }
}

val LocalCompassTheme = staticCompositionLocalOf {
    resolveCompassColors(
        mode = AppThemeMode.MIDNIGHT,
        accent = AppAccentColor.BEACON,
        highContrast = false,
        compactView = false
    )
}

// --- Dynamic Color Accessors for Composables ---
val Ink950: Color @Composable get() = LocalCompassTheme.current.background
val Ink900: Color @Composable get() = LocalCompassTheme.current.surface
val Ink850: Color @Composable get() = LocalCompassTheme.current.surfaceVariant.copy(alpha = 0.7f)
val Ink800: Color @Composable get() = LocalCompassTheme.current.surfaceVariant
val Ink700: Color @Composable get() = LocalCompassTheme.current.border
val Mist400: Color @Composable get() = LocalCompassTheme.current.textSecondary
val Mist300: Color @Composable get() = LocalCompassTheme.current.textSecondary.copy(alpha = 0.8f)
val Mist200: Color @Composable get() = LocalCompassTheme.current.textPrimary.copy(alpha = 0.85f)
val Mist100: Color @Composable get() = LocalCompassTheme.current.textPrimary
val Beacon500: Color @Composable get() = LocalCompassTheme.current.accent
val Beacon400: Color @Composable get() = LocalCompassTheme.current.accentLight
val Emerald500: Color @Composable get() = LocalCompassTheme.current.success
val Rose500: Color @Composable get() = LocalCompassTheme.current.danger
val Amber500: Color = Color(0xFFF59E0B)
val OnAccentColor: Color @Composable get() = LocalCompassTheme.current.onAccent

@Composable
fun CompassAppTheme(
    viewModel: CompassViewModel,
    content: @Composable () -> Unit
) {
    val mode = viewModel.currentThemeMode.value
    val accent = viewModel.currentAccentColor.value
    val fontScale = viewModel.currentFontScale.value
    val highContrast = viewModel.highContrastEnabled.value
    val compactView = viewModel.compactListingEnabled.value

    val colors = remember(mode, accent, highContrast, compactView) {
        resolveCompassColors(
            mode = mode,
            accent = accent,
            highContrast = highContrast,
            compactView = compactView
        )
    }

    val materialColorScheme = if (colors.isDark) {
        darkColorScheme(
            primary = colors.accent,
            onPrimary = colors.onAccent,
            primaryContainer = colors.surfaceVariant,
            onPrimaryContainer = colors.textPrimary,
            background = colors.background,
            onBackground = colors.textPrimary,
            surface = colors.surface,
            onSurface = colors.textPrimary,
            surfaceVariant = colors.surfaceVariant,
            onSurfaceVariant = colors.textSecondary,
            outline = colors.border
        )
    } else {
        lightColorScheme(
            primary = colors.accent,
            onPrimary = Color.White,
            primaryContainer = colors.surfaceVariant,
            onPrimaryContainer = colors.textPrimary,
            background = colors.background,
            onBackground = colors.textPrimary,
            surface = colors.surface,
            onSurface = colors.textPrimary,
            surfaceVariant = colors.surfaceVariant,
            onSurfaceVariant = colors.textSecondary,
            outline = colors.border
        )
    }

    val currentDensity = LocalDensity.current
    val customDensity = remember(currentDensity, fontScale.scaleFactor) {
        Density(
            density = currentDensity.density,
            fontScale = currentDensity.fontScale * fontScale.scaleFactor
        )
    }

    CompositionLocalProvider(
        LocalCompassTheme provides colors,
        LocalDensity provides customDensity
    ) {
        MaterialTheme(
            colorScheme = materialColorScheme,
            content = content
        )
    }
}
