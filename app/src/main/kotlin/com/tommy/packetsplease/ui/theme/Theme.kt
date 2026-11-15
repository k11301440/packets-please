package com.tommy.packetsplease.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import com.tommy.packetsplease.domain.AppSettings

@Composable
fun PacketsPleaseTheme(
  settings: AppSettings = AppSettings(),
  content: @Composable () -> Unit,
) {
  val tokens = remember(settings.themeMode, settings.accentColor) {
    buildStyleTokens(settings)
  }
  val scaledTypography = remember(settings.fontScale) {
    buildScaledTypography(settings.fontScale.multiplier)
  }

  SideEffect {
    if (PacketsPalette.currentTokens != tokens) {
      PacketsPalette.currentTokens = tokens
    }
  }

  val colorScheme = remember(tokens) {
    darkColorScheme(
      primary = tokens.crtPhosphorMid,
      secondary = PacketsPalette.CrtAlertAmber,
      tertiary = PacketsPalette.QuarantineLight,
      background = tokens.crtBlack,
      surface = tokens.deskDark,
      onPrimary = tokens.crtBlack,
      onBackground = tokens.paperBright,
      onSurface = tokens.paperBright,
    )
  }

  CompositionLocalProvider(LocalAppStyle provides tokens) {
    MaterialTheme(
      colorScheme = colorScheme,
      typography = scaledTypography,
      content = content,
    )
  }
}
