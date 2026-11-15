package com.tommy.packetsplease.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.tommy.packetsplease.R
import com.tommy.packetsplease.domain.FontScaleOption

val DotGothicFontFamily = FontFamily(
  Font(R.font.dotgothic16, FontWeight.Normal),
  Font(R.font.dotgothic16, FontWeight.Bold),
)

fun buildScaledTypography(scale: Float): Typography {
  val clamped = scale.coerceIn(0.85f, 1.40f)
  return Typography(
    displaySmall = TextStyle(
      fontFamily = DotGothicFontFamily,
      fontWeight = FontWeight.Bold,
      fontSize = (26f * clamped).sp,
      lineHeight = (32f * clamped).sp,
    ),
    titleLarge = TextStyle(
      fontFamily = DotGothicFontFamily,
      fontWeight = FontWeight.Bold,
      fontSize = (20f * clamped).sp,
      lineHeight = (26f * clamped).sp,
    ),
    titleMedium = TextStyle(
      fontFamily = DotGothicFontFamily,
      fontWeight = FontWeight.Bold,
      fontSize = (16f * clamped).sp,
      lineHeight = (22f * clamped).sp,
    ),
    bodyLarge = TextStyle(
      fontFamily = DotGothicFontFamily,
      fontWeight = FontWeight.Normal,
      fontSize = (15f * clamped).sp,
      lineHeight = (21f * clamped).sp,
    ),
    bodyMedium = TextStyle(
      fontFamily = DotGothicFontFamily,
      fontWeight = FontWeight.Normal,
      fontSize = (13.5f * clamped).sp,
      lineHeight = (19f * clamped).sp,
    ),
    labelMedium = TextStyle(
      fontFamily = DotGothicFontFamily,
      fontWeight = FontWeight.Bold,
      fontSize = (12.5f * clamped).sp,
      lineHeight = (17f * clamped).sp,
    ),
    labelSmall = TextStyle(
      fontFamily = DotGothicFontFamily,
      fontWeight = FontWeight.Normal,
      fontSize = (11.5f * clamped).sp,
      lineHeight = (15f * clamped).sp,
    ),
  )
}

val PacketsTypography = buildScaledTypography(FontScaleOption.LARGE.multiplier)
