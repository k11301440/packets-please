package com.tommy.packetsplease.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val PacketsColorScheme = darkColorScheme(
  primary = PacketsPalette.CrtPhosphorMid,
  onPrimary = PacketsPalette.CrtBlack,
  secondary = PacketsPalette.QuarantineMid,
  onSecondary = PacketsPalette.InkDark,
  background = PacketsPalette.DeskDark,
  onBackground = PacketsPalette.PaperBright,
  surface = PacketsPalette.PaperBright,
  onSurface = PacketsPalette.InkDark,
  error = PacketsPalette.BlockMid,
)

@Composable
fun PacketsPleaseTheme(content: @Composable () -> Unit) {
  MaterialTheme(
    colorScheme = PacketsColorScheme,
    typography = PacketsTypography,
    content = content,
  )
}
