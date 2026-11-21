package com.example.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.example.domain.MaterialYouPalette

// Semantic Financial Colors (Harmonized with Material Design 3)
val IncomeGreen = Color(0xFF146C2E)
val ExpenseCoral = Color(0xFFB3261E)
val TransferBlue = Color(0xFF0061A4)
val BroughtForwardGold = Color(0xFFB26200)
val WarningAmber = Color(0xFFE37400)

// 1. OCEAN BLUE (Default Material You Seed Palette)
private val OceanBlueLightScheme = lightColorScheme(
    primary = Color(0xFF0061A4),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD1E4FF),
    onPrimaryContainer = Color(0xFF001D36),
    secondary = Color(0xFF535F70),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD7E3F7),
    onSecondaryContainer = Color(0xFF101C2B),
    tertiary = Color(0xFF6B5778),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFF2DAFF),
    onTertiaryContainer = Color(0xFF251431),
    background = Color(0xFFF8F9FF),
    onBackground = Color(0xFF191C20),
    surface = Color(0xFFF8F9FF),
    onSurface = Color(0xFF191C20),
    surfaceVariant = Color(0xFFDFE2EB),
    onSurfaceVariant = Color(0xFF43474E),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF2F3FA),
    surfaceContainer = Color(0xFFECEDF4),
    surfaceContainerHigh = Color(0xFFE6E8EE),
    surfaceContainerHighest = Color(0xFFE1E2E8),
    outline = Color(0xFF73777F),
    outlineVariant = Color(0xFFC3C7CF)
)

private val OceanBlueDarkScheme = darkColorScheme(
    primary = Color(0xFF9ECAFF),
    onPrimary = Color(0xFF003258),
    primaryContainer = Color(0xFF00497D),
    onPrimaryContainer = Color(0xFFD1E4FF),
    secondary = Color(0xFFBBC7DB),
    onSecondary = Color(0xFF253140),
    secondaryContainer = Color(0xFF3B4858),
    onSecondaryContainer = Color(0xFFD7E3F7),
    tertiary = Color(0xFFD6BEE4),
    onTertiary = Color(0xFF3B2948),
    tertiaryContainer = Color(0xFF523F5F),
    onTertiaryContainer = Color(0xFFF2DAFF),
    background = Color(0xFF111418),
    onBackground = Color(0xFFE1E2E8),
    surface = Color(0xFF111418),
    onSurface = Color(0xFFE1E2E8),
    surfaceVariant = Color(0xFF43474E),
    onSurfaceVariant = Color(0xFFC3C7CF),
    surfaceContainerLowest = Color(0xFF0C0E13),
    surfaceContainerLow = Color(0xFF191C20),
    surfaceContainer = Color(0xFF1D2024),
    surfaceContainerHigh = Color(0xFF272A2F),
    surfaceContainerHighest = Color(0xFF32353A),
    outline = Color(0xFF8D9199),
    outlineVariant = Color(0xFF43474E)
)

// 2. EMERALD MINT (Material You Seed Palette)
private val EmeraldMintLightScheme = lightColorScheme(
    primary = Color(0xFF006C4C),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF89F8C7),
    onPrimaryContainer = Color(0xFF002114),
    secondary = Color(0xFF4D6357),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCFE9D9),
    onSecondaryContainer = Color(0xFF092016),
    tertiary = Color(0xFF3D6373),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFC1E8FB),
    onTertiaryContainer = Color(0xFF001F29),
    background = Color(0xFFF5FBF5),
    onBackground = Color(0xFF171D1A),
    surface = Color(0xFFF5FBF5),
    onSurface = Color(0xFF171D1A),
    surfaceVariant = Color(0xFFDBE5DD),
    onSurfaceVariant = Color(0xFF404943),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFEFF5EF),
    surfaceContainer = Color(0xFFE9EFEA),
    surfaceContainerHigh = Color(0xFFE3EAE4),
    surfaceContainerHighest = Color(0xFFDEE4DE),
    outline = Color(0xFF707973),
    outlineVariant = Color(0xFFBFC9C2)
)

private val EmeraldMintDarkScheme = darkColorScheme(
    primary = Color(0xFF6CDBAC),
    onPrimary = Color(0xFF003826),
    primaryContainer = Color(0xFF005138),
    onPrimaryContainer = Color(0xFF89F8C7),
    secondary = Color(0xFFB3CCBE),
    onSecondary = Color(0xFF1F352A),
    secondaryContainer = Color(0xFF354B40),
    onSecondaryContainer = Color(0xFFCFE9D9),
    tertiary = Color(0xFFA5CCDF),
    onTertiary = Color(0xFF073543),
    tertiaryContainer = Color(0xFF244C5B),
    onTertiaryContainer = Color(0xFFC1E8FB),
    background = Color(0xFF0F1512),
    onBackground = Color(0xFFDEE4DE),
    surface = Color(0xFF0F1512),
    onSurface = Color(0xFFDEE4DE),
    surfaceVariant = Color(0xFF404943),
    onSurfaceVariant = Color(0xFFBFC9C2),
    surfaceContainerLowest = Color(0xFF0A0F0D),
    surfaceContainerLow = Color(0xFF171D1A),
    surfaceContainer = Color(0xFF1B211E),
    surfaceContainerHigh = Color(0xFF252B28),
    surfaceContainerHighest = Color(0xFF303633),
    outline = Color(0xFF89938C),
    outlineVariant = Color(0xFF404943)
)

// 3. ROYAL VIOLET (Material You Baseline M3 Seed Palette)
private val RoyalVioletLightScheme = lightColorScheme(
    primary = Color(0xFF6750A4),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFEADDFF),
    onPrimaryContainer = Color(0xFF21005D),
    secondary = Color(0xFF625B71),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE8DEF8),
    onSecondaryContainer = Color(0xFF1D192B),
    tertiary = Color(0xFF7D5260),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFD8E4),
    onTertiaryContainer = Color(0xFF31111D),
    background = Color(0xFFFEF7FF),
    onBackground = Color(0xFF1D1B20),
    surface = Color(0xFFFEF7FF),
    onSurface = Color(0xFF1D1B20),
    surfaceVariant = Color(0xFFE7E0EC),
    onSurfaceVariant = Color(0xFF49454F),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF7F2FA),
    surfaceContainer = Color(0xFFF3EDF7),
    surfaceContainerHigh = Color(0xFFECE6F0),
    surfaceContainerHighest = Color(0xFFE6E0E9),
    outline = Color(0xFF79747E),
    outlineVariant = Color(0xFFCAC4D0)
)

private val RoyalVioletDarkScheme = darkColorScheme(
    primary = Color(0xFFD0BCFF),
    onPrimary = Color(0xFF381E72),
    primaryContainer = Color(0xFF4F378B),
    onPrimaryContainer = Color(0xFFEADDFF),
    secondary = Color(0xFFCCC2DC),
    onSecondary = Color(0xFF332D41),
    secondaryContainer = Color(0xFF4A4458),
    onSecondaryContainer = Color(0xFFE8DEF8),
    tertiary = Color(0xFFEFB8C8),
    onTertiary = Color(0xFF492532),
    tertiaryContainer = Color(0xFF633B48),
    onTertiaryContainer = Color(0xFFFFD8E4),
    background = Color(0xFF141218),
    onBackground = Color(0xFFE6E0E9),
    surface = Color(0xFF141218),
    onSurface = Color(0xFFE6E0E9),
    surfaceVariant = Color(0xFF49454F),
    onSurfaceVariant = Color(0xFFCAC4D0),
    surfaceContainerLowest = Color(0xFF0F0D13),
    surfaceContainerLow = Color(0xFF1D1B20),
    surfaceContainer = Color(0xFF211F26),
    surfaceContainerHigh = Color(0xFF2B2930),
    surfaceContainerHighest = Color(0xFF36343B),
    outline = Color(0xFF938F99),
    outlineVariant = Color(0xFF49454F)
)

// 4. SUNSET ROSE (Material You Seed Palette)
private val SunsetRoseLightScheme = lightColorScheme(
    primary = Color(0xFF984061),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFD9E2),
    onPrimaryContainer = Color(0xFF3E001D),
    secondary = Color(0xFF74565F),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFD9E2),
    onSecondaryContainer = Color(0xFF2B151C),
    tertiary = Color(0xFF7C5635),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFDCC1),
    onTertiaryContainer = Color(0xFF2E1500),
    background = Color(0xFFFFF8F8),
    onBackground = Color(0xFF22191C),
    surface = Color(0xFFFFF8F8),
    onSurface = Color(0xFF22191C),
    surfaceVariant = Color(0xFFF2DDE1),
    onSurfaceVariant = Color(0xFF514347),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFF0F2),
    surfaceContainer = Color(0xFFFAEAED),
    surfaceContainerHigh = Color(0xFFF4E4E7),
    surfaceContainerHighest = Color(0xFFEFDFE1),
    outline = Color(0xFF837377),
    outlineVariant = Color(0xFFD5C2C6)
)

private val SunsetRoseDarkScheme = darkColorScheme(
    primary = Color(0xFFFFB1C8),
    onPrimary = Color(0xFF5E1133),
    primaryContainer = Color(0xFF7B2949),
    onPrimaryContainer = Color(0xFFFFD9E2),
    secondary = Color(0xFFE3BDC6),
    onSecondary = Color(0xFF422931),
    secondaryContainer = Color(0xFF5A3F47),
    onSecondaryContainer = Color(0xFFFFD9E2),
    tertiary = Color(0xFFEFBD94),
    onTertiary = Color(0xFF48290B),
    tertiaryContainer = Color(0xFF623F20),
    onTertiaryContainer = Color(0xFFFFDCC1),
    background = Color(0xFF191113),
    onBackground = Color(0xFFEFDFE1),
    surface = Color(0xFF191113),
    onSurface = Color(0xFFEFDFE1),
    surfaceVariant = Color(0xFF514347),
    onSurfaceVariant = Color(0xFFD5C2C6),
    surfaceContainerLowest = Color(0xFF140C0E),
    surfaceContainerLow = Color(0xFF22191C),
    surfaceContainer = Color(0xFF261D20),
    surfaceContainerHigh = Color(0xFF31282A),
    surfaceContainerHighest = Color(0xFF3C3235),
    outline = Color(0xFF9E8C90),
    outlineVariant = Color(0xFF514347)
)

// 5. AMBER GOLD (Material You Seed Palette)
private val AmberGoldLightScheme = lightColorScheme(
    primary = Color(0xFF785900),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDF9E),
    onPrimaryContainer = Color(0xFF261A00),
    secondary = Color(0xFF6B5D3F),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF5E0BB),
    onSecondaryContainer = Color(0xFF241A04),
    tertiary = Color(0xFF4A6547),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFCCEBC4),
    onTertiaryContainer = Color(0xFF072109),
    background = Color(0xFFFFF8F2),
    onBackground = Color(0xFF1F1B13),
    surface = Color(0xFFFFF8F2),
    onSurface = Color(0xFF1F1B13),
    surfaceVariant = Color(0xFFEDE1CF),
    onSurfaceVariant = Color(0xFF4D4639),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFDF2E5),
    surfaceContainer = Color(0xFFF7ECDF),
    surfaceContainerHigh = Color(0xFFF1E6D9),
    surfaceContainerHighest = Color(0xFFEBE1D4),
    outline = Color(0xFF7F7667),
    outlineVariant = Color(0xFFD0C5B4)
)

private val AmberGoldDarkScheme = darkColorScheme(
    primary = Color(0xFFFABD00),
    onPrimary = Color(0xFF3F2E00),
    primaryContainer = Color(0xFF5B4300),
    onPrimaryContainer = Color(0xFFFFDF9E),
    secondary = Color(0xFFD8C4A0),
    onSecondary = Color(0xFF3B2F15),
    secondaryContainer = Color(0xFF52452A),
    onSecondaryContainer = Color(0xFFF5E0BB),
    tertiary = Color(0xFFB0CFAA),
    onTertiary = Color(0xFF1C361C),
    tertiaryContainer = Color(0xFF334D31),
    onTertiaryContainer = Color(0xFFCCEBC4),
    background = Color(0xFF17130B),
    onBackground = Color(0xFFEBE1D4),
    surface = Color(0xFF17130B),
    onSurface = Color(0xFFEBE1D4),
    surfaceVariant = Color(0xFF4D4639),
    onSurfaceVariant = Color(0xFFD0C5B4),
    surfaceContainerLowest = Color(0xFF110E07),
    surfaceContainerLow = Color(0xFF1F1B13),
    surfaceContainer = Color(0xFF231F17),
    surfaceContainerHigh = Color(0xFF2E2921),
    surfaceContainerHighest = Color(0xFF39342B),
    outline = Color(0xFF998F80),
    outlineVariant = Color(0xFF4D4639)
)

fun colorSchemeForPalette(palette: MaterialYouPalette, darkTheme: Boolean): ColorScheme {
    return when (palette) {
        MaterialYouPalette.OCEAN_BLUE -> if (darkTheme) OceanBlueDarkScheme else OceanBlueLightScheme
        MaterialYouPalette.EMERALD_MINT -> if (darkTheme) EmeraldMintDarkScheme else EmeraldMintLightScheme
        MaterialYouPalette.ROYAL_VIOLET -> if (darkTheme) RoyalVioletDarkScheme else RoyalVioletLightScheme
        MaterialYouPalette.SUNSET_ROSE -> if (darkTheme) SunsetRoseDarkScheme else SunsetRoseLightScheme
        MaterialYouPalette.AMBER_GOLD -> if (darkTheme) AmberGoldDarkScheme else AmberGoldLightScheme
    }
}
