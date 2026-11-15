package com.tommy.packetsplease.ui.theme

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.tommy.packetsplease.domain.AppSettings
import com.tommy.packetsplease.domain.BaseAccentColor
import com.tommy.packetsplease.domain.ThemeMode

data class AppStyleTokens(
  val crtBlack: Color,
  val crtGridDark: Color,
  val crtPhosphorMid: Color,
  val crtPhosphorBright: Color,
  val deskDark: Color,
  val deskMid: Color,
  val deskLight: Color,
  val paperBright: Color,
  val paperMid: Color,
  val paperShadow: Color,
  val inkDark: Color,
  val inkMuted: Color,
  val inkHighlight: Color,
)

fun buildStyleTokens(settings: AppSettings): AppStyleTokens {
  val (phosphorBright, phosphorMid, gridDark) = when (settings.accentColor) {
    BaseAccentColor.EMERALD -> Triple(Color(0xFF8CFF9F), Color(0xFF33CC5E), Color(0xFF13361D))
    BaseAccentColor.AMBER -> Triple(Color(0xFFFFDF73), Color(0xFFF2A922), Color(0xFF3D2908))
    BaseAccentColor.CYAN -> Triple(Color(0xFF85F7FF), Color(0xFF24C8E0), Color(0xFF0C313B))
    BaseAccentColor.CRIMSON -> Triple(Color(0xFFFF96A2), Color(0xFFE84858), Color(0xFF3D1017))
  }

  return when (settings.themeMode) {
    ThemeMode.CRT_DARK -> AppStyleTokens(
      crtBlack = when (settings.accentColor) {
        BaseAccentColor.EMERALD -> Color(0xFF07120A)
        BaseAccentColor.AMBER -> Color(0xFF120E06)
        BaseAccentColor.CYAN -> Color(0xFF061014)
        BaseAccentColor.CRIMSON -> Color(0xFF14070A)
      },
      crtGridDark = gridDark,
      crtPhosphorMid = phosphorMid,
      crtPhosphorBright = phosphorBright,
      deskDark = Color(0xFF261D17),
      deskMid = Color(0xFF382B22),
      deskLight = Color(0xFF523F32),
      paperBright = Color(0xFFF5E9D6),
      paperMid = Color(0xFFE4D2B7),
      paperShadow = Color(0xFFC4AD8B),
      inkDark = Color(0xFF1D1712),
      inkMuted = Color(0xFF5C4B3B),
      inkHighlight = Color(0xFF8C251D),
    )

    ThemeMode.DESK_WARM -> AppStyleTokens(
      crtBlack = Color(0xFF1B140E),
      crtGridDark = gridDark,
      crtPhosphorMid = phosphorMid,
      crtPhosphorBright = phosphorBright,
      deskDark = Color(0xFF36281E),
      deskMid = Color(0xFF4B382A),
      deskLight = Color(0xFF664D3B),
      paperBright = Color(0xFFFFF6E6),
      paperMid = Color(0xFFF0DFC6),
      paperShadow = Color(0xFFD1B997),
      inkDark = Color(0xFF21170E),
      inkMuted = Color(0xFF614A35),
      inkHighlight = Color(0xFF9C291E),
    )

    ThemeMode.HIGH_CONTRAST -> AppStyleTokens(
      crtBlack = Color(0xFF000000),
      crtGridDark = gridDark,
      crtPhosphorMid = phosphorMid,
      crtPhosphorBright = phosphorBright,
      deskDark = Color(0xFF101010),
      deskMid = Color(0xFF1E1E1E),
      deskLight = Color(0xFF343434),
      paperBright = Color(0xFFFFFFFF),
      paperMid = Color(0xFFEEEEEE),
      paperShadow = Color(0xFFCCCCCC),
      inkDark = Color(0xFF050505),
      inkMuted = Color(0xFF333333),
      inkHighlight = Color(0xFFB00020),
    )
  }
}

val LocalAppStyle = compositionLocalOf { buildStyleTokens(AppSettings()) }

object PacketsPalette {
  var currentTokens by mutableStateOf(buildStyleTokens(AppSettings()))

  val CrtBlack: Color get() = currentTokens.crtBlack
  val CrtGridDark: Color get() = currentTokens.crtGridDark
  val CrtPhosphorMid: Color get() = currentTokens.crtPhosphorMid
  val CrtPhosphorBright: Color get() = currentTokens.crtPhosphorBright
  val CrtAlertAmber = Color(0xFFF2A922)
  val CrtAlertRed = Color(0xFFE84848)

  val DeskDark: Color get() = currentTokens.deskDark
  val DeskMid: Color get() = currentTokens.deskMid
  val DeskLight: Color get() = currentTokens.deskLight

  val PaperBright: Color get() = currentTokens.paperBright
  val PaperMid: Color get() = currentTokens.paperMid
  val PaperShadow: Color get() = currentTokens.paperShadow

  val InkDark: Color get() = currentTokens.inkDark
  val InkMuted: Color get() = currentTokens.inkMuted
  val InkHighlight: Color get() = currentTokens.inkHighlight

  val PassDark = Color(0xFF1B5E2E)
  val PassMid = Color(0xFF288542)
  val PassLight = Color(0xFF63D47E)

  val BlockDark = Color(0xFF6E1D1D)
  val BlockMid = Color(0xFFA32B2B)
  val BlockLight = Color(0xFFF06767)

  val QuarantineDark = Color(0xFF6E4B11)
  val QuarantineMid = Color(0xFFA8741D)
  val QuarantineLight = Color(0xFFF5BC53)

  val BookCover = Color(0xFF2A3D54)
  val BookPaper = Color(0xFFEDE2CE)
  val SlipPaper = Color(0xFFDCE8DF)
  val BribeNoteBg = Color(0xFFE8D89E)
}
