package com.tommy.packetsplease.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.tommy.packetsplease.R

val DotGothicFontFamily = FontFamily(
  Font(R.font.dotgothic16, FontWeight.Normal),
  Font(R.font.dotgothic16, FontWeight.Bold),
)

val PacketsTypography = Typography(
  displaySmall = TextStyle(
    fontFamily = DotGothicFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 24.sp,
    lineHeight = 28.sp,
  ),
  titleLarge = TextStyle(
    fontFamily = DotGothicFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 18.sp,
    lineHeight = 22.sp,
  ),
  titleMedium = TextStyle(
    fontFamily = DotGothicFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 15.sp,
    lineHeight = 19.sp,
  ),
  bodyLarge = TextStyle(
    fontFamily = DotGothicFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 13.sp,
    lineHeight = 18.sp,
  ),
  bodyMedium = TextStyle(
    fontFamily = DotGothicFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 12.sp,
    lineHeight = 16.sp,
  ),
  labelMedium = TextStyle(
    fontFamily = DotGothicFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 11.sp,
    lineHeight = 14.sp,
  ),
  labelSmall = TextStyle(
    fontFamily = DotGothicFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 10.sp,
    lineHeight = 12.sp,
  ),
)
