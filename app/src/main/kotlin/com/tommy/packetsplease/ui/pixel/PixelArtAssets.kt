package com.tommy.packetsplease.ui.pixel

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.tommy.packetsplease.domain.Verdict
import kotlin.math.abs

object PixelArtAssets {

  private val avatarCache = HashMap<Int, ImageBitmap>()
  private val boothEntrantCache = HashMap<Int, ImageBitmap>()
  private val virusFrameCache = HashMap<Int, ImageBitmap>()
  private val stampCache = HashMap<Verdict, ImageBitmap>()

  private val pixelFont5x7: Map<Char, List<String>> = mapOf(
    'P' to listOf("11110", "10001", "10001", "11110", "10000", "10000", "10000"),
    'A' to listOf("01110", "10001", "10001", "11111", "10001", "10001", "10001"),
    'S' to listOf("01111", "10000", "10000", "01110", "00001", "00001", "11110"),
    'B' to listOf("11110", "10001", "10001", "11110", "10001", "10001", "11110"),
    'L' to listOf("10000", "10000", "10000", "10000", "10000", "10000", "11111"),
    'O' to listOf("01110", "10001", "10001", "10001", "10001", "10001", "01110"),
    'C' to listOf("01111", "10000", "10000", "10000", "10000", "10000", "01111"),
    'K' to listOf("10001", "10010", "10100", "11000", "10100", "10010", "10001"),
    'Q' to listOf("01110", "10001", "10001", "10001", "10101", "10010", "01101"),
    'U' to listOf("10001", "10001", "10001", "10001", "10001", "10001", "01110"),
    'R' to listOf("11110", "10001", "10001", "11110", "10100", "10010", "10001"),
    'N' to listOf("10001", "11001", "10101", "10011", "10001", "10001", "10001"),
    'T' to listOf("11111", "00100", "00100", "00100", "00100", "00100", "00100"),
    'I' to listOf("01110", "00100", "00100", "00100", "00100", "00100", "01110"),
    'E' to listOf("11111", "10000", "10000", "11110", "10000", "10000", "11111"),
  )

  val packetSprite: ImageBitmap by lazy { buildPacketSprite(AndroidColor.rgb(140, 255, 159)) }
  val alertPacketSprite: ImageBitmap by lazy { buildPacketSprite(AndroidColor.rgb(255, 82, 82)) }
  val quarantinePacketSprite: ImageBitmap by lazy { buildPacketSprite(AndroidColor.rgb(255, 193, 69)) }
  val externalNodeSprite: ImageBitmap by lazy { buildServerSprite(isInternal = false) }
  val internalNodeSprite: ImageBitmap by lazy { buildServerSprite(isInternal = true) }
  val firewallGateSprite: ImageBitmap by lazy { buildFirewallGateSprite() }
  val appLogoCrestBitmap: ImageBitmap by lazy { buildLogoCrest32x32() }

  fun getStampBitmap(verdict: Verdict): ImageBitmap {
    return stampCache.getOrPut(verdict) { buildStampTexture(verdict) }
  }

  fun getAvatarBitmap(seed: Int): ImageBitmap {
    return avatarCache.getOrPut(seed) { buildAvatar24x24(seed) }
  }

  fun boothEntrantBitmap(seed: Int): ImageBitmap {
    val variant = abs(seed) % 8
    return boothEntrantCache.getOrPut(variant) { buildBoothEntrant16x20(variant) }
  }

  fun virusExplosionBitmap(frame: Int): ImageBitmap {
    val normalized = abs(frame) % 3
    return virusFrameCache.getOrPut(normalized) { buildVirusExplosion24x24(normalized) }
  }

  /**
   * 3階調制限の24x24ドット絵送信者アバターを生成する。
   */
  private fun buildAvatar24x24(seed: Int): ImageBitmap {
    val size = 24
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val s = abs(seed)

    val bgTone = when (s % 3) {
      0 -> AndroidColor.rgb(34, 52, 42)
      1 -> AndroidColor.rgb(52, 42, 34)
      else -> AndroidColor.rgb(38, 42, 58)
    }
    val bgBorder = AndroidColor.rgb(20, 18, 16)

    val skinMid = when ((s / 3) % 3) {
      0 -> AndroidColor.rgb(232, 194, 158)
      1 -> AndroidColor.rgb(214, 174, 138)
      else -> AndroidColor.rgb(198, 212, 202) // ロボット/システム系トーン
    }
    val skinShade = darken(skinMid, 0.78f)

    val hairColors = listOf(
      AndroidColor.rgb(42, 34, 30),
      AndroidColor.rgb(98, 64, 38),
      AndroidColor.rgb(74, 86, 102),
      AndroidColor.rgb(142, 54, 46),
    )
    val hairMain = hairColors[(s / 7) % hairColors.size]
    val hairHighlight = lighten(hairMain, 1.25f)

    val suitMain = listOf(
      AndroidColor.rgb(56, 76, 98),
      AndroidColor.rgb(66, 92, 68),
      AndroidColor.rgb(92, 58, 52),
      AndroidColor.rgb(72, 68, 78),
    )[(s / 11) % 4]
    val suitShade = darken(suitMain, 0.72f)
    val eyeColor = AndroidColor.rgb(24, 20, 18)

    for (y in 0 until size) {
      for (x in 0 until size) {
        val isFrame = x == 0 || y == 0 || x == size - 1 || y == size - 1
        bmp.setPixel(x, y, if (isFrame) bgBorder else bgTone)
      }
    }

    // 肩・スーツ (y: 17..22)
    for (y in 17..22) {
      val halfW = if (y == 17) 5 else 7
      for (x in (12 - halfW) until (12 + halfW)) {
        val color = if (x == 11 || x == 12) AndroidColor.rgb(235, 224, 205)
        else if (y == 17 || x > 14) suitShade
        else suitMain
        bmp.setPixel(x, y, color)
      }
    }

    // 首
    for (y in 15..16) {
      for (x in 10..13) {
        bmp.setPixel(x, y, skinShade)
      }
    }

    // 顔ベース (y: 6..15, x: 7..16)
    for (y in 6..15) {
      val inset = if (y == 6 || y == 15) 1 else 0
      for (x in (7 + inset)..(16 - inset)) {
        val color = if (x >= 15 || y == 15) skinShade else skinMid
        bmp.setPixel(x, y, color)
      }
    }

    // 髪型スタイル
    val hairStyle = (s / 5) % 4
    when (hairStyle) {
      0 -> {
        for (y in 3..7) {
          for (x in 6..17) {
            if (y <= 5 || x <= 8 || x >= 15) {
              bmp.setPixel(x, y, if (y == 4 && x in 9..13) hairHighlight else hairMain)
            }
          }
        }
      }
      1 -> {
        for (y in 4..13) {
          for (x in 6..17) {
            if (y <= 6 || (y <= 13 && (x == 6 || x == 17))) {
              bmp.setPixel(x, y, if (y == 5) hairHighlight else hairMain)
            }
          }
        }
      }
      2 -> {
        // 端末アンテナ/サイバー風
        for (y in 4..6) {
          for (x in 7..16) {
            bmp.setPixel(x, y, hairMain)
          }
        }
        bmp.setPixel(16, 2, AndroidColor.rgb(72, 224, 118))
        bmp.setPixel(16, 3, AndroidColor.rgb(72, 224, 118))
      }
      else -> {
        // 刈り上げ・帽子風
        for (y in 4..6) {
          for (x in 7..16) {
            bmp.setPixel(x, y, if (y == 4) hairHighlight else hairMain)
          }
        }
      }
    }

    // 目 (y = 10..11)
    bmp.setPixel(9, 10, eyeColor)
    bmp.setPixel(9, 11, eyeColor)
    bmp.setPixel(14, 10, eyeColor)
    bmp.setPixel(14, 11, eyeColor)

    // メガネ（3分の1の確率）
    if ((s / 13) % 3 == 0) {
      val glassColor = AndroidColor.rgb(58, 62, 72)
      for (x in 8..10) {
        bmp.setPixel(x, 9, glassColor)
        bmp.setPixel(x, 12, glassColor)
      }
      for (x in 13..15) {
        bmp.setPixel(x, 9, glassColor)
        bmp.setPixel(x, 12, glassColor)
      }
      bmp.setPixel(11, 10, glassColor)
      bmp.setPixel(12, 10, glassColor)
    }

    // 口
    val mouthY = 13
    bmp.setPixel(11, mouthY, skinShade)
    bmp.setPixel(12, mouthY, skinShade)

    return bmp.asImageBitmap()
  }

  /**
   * フォントを使用せず自前ドットマップから3階調スタンプテクスチャを生成する。
   */
  private fun buildStampTexture(verdict: Verdict): ImageBitmap {
    val label = verdict.displayLabel
    val charWidth = 5
    val charSpacing = 1
    val textPixelWidth = label.length * charWidth + (label.length - 1) * charSpacing
    val padX = 5
    val padY = 4
    val w = textPixelWidth + padX * 2
    val h = 7 + padY * 2

    val (dark, mid, light) = when (verdict) {
      Verdict.PASS -> Triple(
        AndroidColor.rgb(23, 78, 40),
        AndroidColor.rgb(42, 133, 71),
        AndroidColor.rgb(83, 196, 116),
      )
      Verdict.BLOCK -> Triple(
        AndroidColor.rgb(107, 20, 20),
        AndroidColor.rgb(179, 41, 41),
        AndroidColor.rgb(232, 90, 90),
      )
      Verdict.QUARANTINE -> Triple(
        AndroidColor.rgb(112, 69, 7),
        AndroidColor.rgb(196, 127, 22),
        AndroidColor.rgb(245, 183, 59),
      )
    }

    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    bmp.eraseColor(AndroidColor.TRANSPARENT)

    // 二重スタンプ外枠
    for (y in 0 until h) {
      for (x in 0 until w) {
        val outerBorder = x == 0 || y == 0 || x == w - 1 || y == h - 1
        val innerBorder = (x == 2 || y == 2 || x == w - 3 || y == h - 3) &&
          (x in 2..(w - 3) && y in 2..(h - 3))
        if (outerBorder) {
          // ゴム印らしい僅かな欠け演出
          if ((x * 17 + y * 31) % 19 != 0) bmp.setPixel(x, y, dark)
        } else if (innerBorder) {
          bmp.setPixel(x, y, mid)
        }
      }
    }

    // 5x7 ドット文字描画
    var cursorX = padX
    for (ch in label) {
      val glyph = pixelFont5x7[ch]
      if (glyph != null) {
        for (gy in glyph.indices) {
          val row = glyph[gy]
          for (gx in row.indices) {
            if (row[gx] == '1') {
              val px = cursorX + gx
              val py = padY + gy
              val inkColor = when {
                gy == 0 -> light
                gy >= 5 -> dark
                else -> mid
              }
              bmp.setPixel(px, py, inkColor)
            }
          }
        }
      }
      cursorX += charWidth + charSpacing
    }

    return bmp.asImageBitmap()
  }

  private fun buildPacketSprite(mainColor: Int): ImageBitmap {
    val w = 12
    val h = 9
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val dark = darken(mainColor, 0.55f)
    val fill = darken(mainColor, 0.25f)
    for (y in 0 until h) {
      for (x in 0 until w) {
        val border = x == 0 || y == 0 || x == w - 1 || y == h - 1
        val flap = (y == x && x <= w / 2) || (y == (w - 1 - x) && x >= w / 2)
        val color = when {
          border -> mainColor
          flap -> mainColor
          y == 1 -> dark
          else -> fill
        }
        bmp.setPixel(x, y, color)
      }
    }
    return bmp.asImageBitmap()
  }

  private fun buildServerSprite(isInternal: Boolean): ImageBitmap {
    val w = 16
    val h = 16
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    bmp.eraseColor(AndroidColor.TRANSPARENT)
    val border = if (isInternal) AndroidColor.rgb(140, 255, 159) else AndroidColor.rgb(255, 193, 69)
    val body = if (isInternal) AndroidColor.rgb(19, 54, 29) else AndroidColor.rgb(54, 42, 18)
    val led = if (isInternal) AndroidColor.rgb(51, 204, 94) else AndroidColor.rgb(255, 110, 74)

    for (y in 1 until h - 1) {
      for (x in 2 until w - 2) {
        val isEdge = x == 2 || x == w - 3 || y == 1 || y == h - 2 || y == 6 || y == 11
        val isLed = (x == 4 && (y == 3 || y == 8 || y == 13))
        bmp.setPixel(
          x,
          y,
          when {
            isLed -> led
            isEdge -> border
            else -> body
          },
        )
      }
    }
    return bmp.asImageBitmap()
  }

  private fun buildFirewallGateSprite(): ImageBitmap {
    val w = 20
    val h = 24
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    bmp.eraseColor(AndroidColor.TRANSPARENT)
    val bright = AndroidColor.rgb(140, 255, 159)
    val mid = AndroidColor.rgb(51, 204, 94)
    val dark = AndroidColor.rgb(19, 54, 29)

    for (y in 0 until h) {
      for (x in 0 until w) {
        val outer = x == 1 || x == w - 2 || y == 0 || y == h - 1
        val gateSlot = y in 9..14 && x in 6..13
        val shield = (y in 4..7 && x in 7..12) || (y in 16..19 && x in 7..12)
        if (x in 1..(w - 2)) {
          val color = when {
            gateSlot -> AndroidColor.rgb(7, 18, 10)
            outer -> bright
            shield -> mid
            else -> dark
          }
          bmp.setPixel(x, y, color)
        }
      }
    }
    return bmp.asImageBitmap()
  }

  /**
   * 32x32 公式エンブレムロゴ（アプリアイコンと統一された盾＋ゲート＋封筒シルエット）
   */
  private fun buildLogoCrest32x32(): ImageBitmap {
    val size = 32
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    bmp.eraseColor(AndroidColor.TRANSPARENT)

    val bright = AndroidColor.rgb(140, 255, 159)
    val mid = AndroidColor.rgb(51, 204, 94)
    val dark = AndroidColor.rgb(19, 54, 29)
    val voidBlack = AndroidColor.rgb(7, 18, 10)
    val paper = AndroidColor.rgb(243, 231, 211)
    val ink = AndroidColor.rgb(33, 24, 18)
    val passC = AndroidColor.rgb(83, 196, 116)
    val warnC = AndroidColor.rgb(245, 183, 59)
    val blockC = AndroidColor.rgb(232, 90, 90)

    fun halfWidthAtRow(y: Int): Int = when {
      y in 3..17 -> 13
      y in 18..20 -> 11
      y in 21..23 -> 9
      y in 24..26 -> 6
      y in 27..28 -> 3
      else -> -1
    }

    for (y in 3..28) {
      val hw = halfWidthAtRow(y)
      if (hw <= 0) continue
      val left = 16 - hw
      val right = 15 + hw
      val nextHw = halfWidthAtRow(y + 1)
      for (x in left..right) {
        val isBorder = y == 3 || y == 28 || x == left || x == right ||
          (nextHw > 0 && (x < 16 - nextHw || x > 15 + nextHw))
        bmp.setPixel(x, y, if (isBorder) bright else dark)
      }
    }

    // 内側ゲート枠 & 黒窓
    for (y in 7..20) {
      for (x in 8..23) {
        val isGateFrame = x <= 9 || x >= 22 || y <= 8 || y >= 19
        bmp.setPixel(x, y, if (isGateFrame) mid else voidBlack)
      }
    }

    // 中央封筒 + フラップ
    for (y in 11..16) {
      for (x in 11..20) {
        val isFlap = (y == 11 && (x == 11 || x == 20)) ||
          (y == 12 && (x == 12 || x == 19)) ||
          (y == 13 && (x == 13 || x == 18)) ||
          (y == 14 && x in 14..17)
        bmp.setPixel(x, y, if (isFlap) ink else paper)
      }
    }

    // PASS / QUARANTINE / BLOCK ドットアクセント
    for (y in 22..23) {
      for (x in 11..12) bmp.setPixel(x, y, passC)
      for (x in 15..16) bmp.setPixel(x, y, warnC)
      for (x in 19..20) bmp.setPixel(x, y, blockC)
    }

    return bmp.asImageBitmap()
  }

  /**
   * 本家入国審査ブース風の16x20待機人物シルエット・ドット絵
   */
  private fun buildBoothEntrant16x20(variant: Int): ImageBitmap {
    val w = 16
    val h = 20
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    bmp.eraseColor(AndroidColor.TRANSPARENT)

    val outline = AndroidColor.rgb(140, 255, 159)
    val coat = when (variant % 4) {
      0 -> AndroidColor.rgb(34, 92, 54)
      1 -> AndroidColor.rgb(44, 78, 96)
      2 -> AndroidColor.rgb(92, 68, 42)
      else -> AndroidColor.rgb(58, 72, 64)
    }
    val skin = AndroidColor.rgb(184, 232, 168)
    val visor = AndroidColor.rgb(19, 54, 29)
    val hasHat = variant % 2 == 1
    val stepShift = if (variant % 3 == 0) 1 else 0

    // 帽子/髪 (y: 1..3)
    val topY = if (hasHat) 1 else 2
    for (y in topY..3) {
      for (x in 5..10) bmp.setPixel(x, y, outline)
    }
    if (hasHat) {
      for (x in 4..11) bmp.setPixel(x, 3, outline)
    }

    // 頭部 (y: 4..8)
    for (y in 4..8) {
      for (x in 5..10) {
        val isEye = y == 6 && (x == 7 || x == 9)
        val isEdge = x == 5 || x == 10
        bmp.setPixel(x, y, when {
          isEye -> visor
          isEdge -> outline
          else -> skin
        })
      }
    }

    // 胴体・コート (y: 9..15)
    for (y in 9..15) {
      for (x in 3..12) {
        if (y == 9 && (x == 3 || x == 12)) continue
        val isBorder = x == 3 || x == 12 || y == 9 || y == 15
        val isTie = y in 10..13 && x == 8
        bmp.setPixel(x, y, when {
          isTie -> outline
          isBorder -> outline
          else -> coat
        })
      }
    }

    // 脚 (y: 16..19)
    for (y in 16..19) {
      for (x in (5 - stepShift)..(6 - stepShift)) {
        if (x in 0 until w) bmp.setPixel(x, y, outline)
      }
      for (x in (9 + stepShift)..(10 + stepShift)) {
        if (x in 0 until w) bmp.setPixel(x, y, outline)
      }
    }

    return bmp.asImageBitmap()
  }

  /**
   * 致命的ウイルス誤通過時の24x24破壊アニメーション (3フレーム)
   */
  private fun buildVirusExplosion24x24(frame: Int): ImageBitmap {
    val size = 24
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    bmp.eraseColor(AndroidColor.TRANSPARENT)

    val redBright = AndroidColor.rgb(255, 82, 82)
    val redDark = AndroidColor.rgb(165, 24, 24)
    val amber = AndroidColor.rgb(255, 193, 69)
    val white = AndroidColor.rgb(255, 244, 214)

    val radius = when (frame) {
      0 -> 4
      1 -> 8
      else -> 11
    }

    for (y in 0 until size) {
      for (x in 0 until size) {
        val dx = x - 12
        val dy = y - 12
        val manhattan = abs(dx) + abs(dy)
        val diag = abs(abs(dx) - abs(dy)) <= 1
        val axis = dx == 0 || dy == 0
        when {
          frame == 2 && (abs(dx) <= 4 && abs(dy) <= 4) -> {
            // 中央にウイルス警告スカル/Xマーク
            val isX = abs(dx) == abs(dy)
            bmp.setPixel(x, y, if (isX) white else redDark)
          }
          manhattan <= radius -> {
            bmp.setPixel(
              x,
              y,
              when {
                manhattan <= 2 -> white
                manhattan <= radius - 2 -> amber
                else -> redBright
              },
            )
          }
          (diag || axis) && manhattan <= radius + 5 && (x + y + frame) % 2 == 0 -> {
            bmp.setPixel(x, y, if ((x * y + frame) % 3 == 0) amber else redBright)
          }
        }
      }
    }

    return bmp.asImageBitmap()
  }

  private fun darken(color: Int, factor: Float): Int {
    val r = (AndroidColor.red(color) * factor).toInt().coerceIn(0, 255)
    val g = (AndroidColor.green(color) * factor).toInt().coerceIn(0, 255)
    val b = (AndroidColor.blue(color) * factor).toInt().coerceIn(0, 255)
    return AndroidColor.rgb(r, g, b)
  }

  private fun lighten(color: Int, factor: Float): Int {
    val r = (AndroidColor.red(color) * factor).toInt().coerceIn(0, 255)
    val g = (AndroidColor.green(color) * factor).toInt().coerceIn(0, 255)
    val b = (AndroidColor.blue(color) * factor).toInt().coerceIn(0, 255)
    return AndroidColor.rgb(r, g, b)
  }
}
