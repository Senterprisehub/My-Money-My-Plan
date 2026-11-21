package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val SpaceGroteskFontFamily = FontFamily(
    Font(R.font.space_grotesk, FontWeight.Normal),
    Font(R.font.space_grotesk, FontWeight.Medium),
    Font(R.font.space_grotesk, FontWeight.Bold)
)

val PlusJakartaSansFontFamily = FontFamily(
    Font(R.font.plus_jakarta_sans, FontWeight.Normal),
    Font(R.font.plus_jakarta_sans, FontWeight.Medium),
    Font(R.font.plus_jakarta_sans, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans, FontWeight.Bold)
)

val CarlitoFontFamily = FontFamily(
    Font(R.font.carlito, FontWeight.Normal),
    Font(R.font.carlito, FontWeight.Bold)
)

val TinosFontFamily = FontFamily(
    Font(R.font.tinos, FontWeight.Normal),
    Font(R.font.tinos, FontWeight.Bold)
)

val MerriweatherFontFamily = FontFamily(
    Font(R.font.merriweather, FontWeight.Normal),
    Font(R.font.merriweather, FontWeight.Bold)
)

val JetBrainsMonoFontFamily = FontFamily(
    Font(R.font.jetbrains_mono, FontWeight.Normal),
    Font(R.font.jetbrains_mono, FontWeight.Bold)
)

val MontserratFontFamily = FontFamily(
    Font(R.font.montserrat, FontWeight.Normal),
    Font(R.font.montserrat, FontWeight.Bold)
)

fun getTypographyForStyle(fontStyle: String): Typography {
    val bodyFamily = when (fontStyle.lowercase()) {
        "calibri" -> CarlitoFontFamily
        "times roman", "times new roman" -> TinosFontFamily
        "serif" -> MerriweatherFontFamily
        "monospace" -> JetBrainsMonoFontFamily
        "bold sans", "bold", "montserrat" -> MontserratFontFamily
        else -> PlusJakartaSansFontFamily
    }

    val headerFamily = when (fontStyle.lowercase()) {
        "calibri" -> CarlitoFontFamily
        "times roman", "times new roman" -> TinosFontFamily
        "serif" -> MerriweatherFontFamily
        "monospace" -> JetBrainsMonoFontFamily
        "bold sans", "bold", "montserrat" -> MontserratFontFamily
        else -> SpaceGroteskFontFamily
    }

    return Typography(
        displayLarge = TextStyle(
            fontFamily = headerFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 38.sp,
            lineHeight = 44.sp,
            letterSpacing = (-0.25).sp
        ),
        displayMedium = TextStyle(
            fontFamily = headerFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 30.sp,
            lineHeight = 36.sp,
            letterSpacing = (-0.25).sp
        ),
        headlineLarge = TextStyle(
            fontFamily = headerFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 26.sp,
            lineHeight = 32.sp
        ),
        headlineMedium = TextStyle(
            fontFamily = headerFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 22.sp,
            lineHeight = 28.sp
        ),
        titleLarge = TextStyle(
            fontFamily = headerFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 19.sp,
            lineHeight = 25.sp
        ),
        titleMedium = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            lineHeight = 22.sp,
            letterSpacing = 0.1.sp
        ),
        titleSmall = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.1.sp
        ),
        bodyLarge = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 24.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 20.sp
        ),
        bodySmall = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            lineHeight = 16.sp
        ),
        labelLarge = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            lineHeight = 20.sp
        ),
        labelMedium = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 16.sp
        ),
        labelSmall = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            lineHeight = 15.sp
        )
    )
}

val Typography = getTypographyForStyle("Default")
