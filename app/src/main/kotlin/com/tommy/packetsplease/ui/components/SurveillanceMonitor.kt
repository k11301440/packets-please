package com.tommy.packetsplease.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.tommy.packetsplease.domain.AppLanguage
import com.tommy.packetsplease.domain.OsAlert
import com.tommy.packetsplease.ui.PacketFlowAnim
import com.tommy.packetsplease.ui.pixel.PixelArtAssets
import com.tommy.packetsplease.ui.theme.LocalAppStyle
import com.tommy.packetsplease.ui.theme.PacketsPalette
import kotlinx.coroutines.delay

@Composable
fun SurveillanceMonitor(
  day: Int,
  remainingSeconds: Int,
  budget: Int,
  dailyWarnings: Int,
  freeWarnings: Int,
  processedCount: Int,
  packetSeed: Int,
  language: AppLanguage,
  flowAnim: PacketFlowAnim,
  isVirusExplosionActive: Boolean,
  activeAlerts: List<OsAlert>,
  onDismissAlert: (String) -> Unit,
  onOpenSettings: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val style = LocalAppStyle.current
  val isJa = language == AppLanguage.JA

  val packetTargetX = when (flowAnim) {
    PacketFlowAnim.INCOMING -> 0.50f
    PacketFlowAnim.IDLE -> 0.50f
    PacketFlowAnim.OUT_PASS -> 0.84f
    PacketFlowAnim.OUT_BLOCK -> 0.28f
    PacketFlowAnim.OUT_QUARANTINE -> 0.50f
    PacketFlowAnim.VIRUS_DESTRUCTION -> 0.84f
  }
  val packetTargetY = when (flowAnim) {
    PacketFlowAnim.OUT_BLOCK -> 0.80f
    PacketFlowAnim.OUT_QUARANTINE -> 0.82f
    PacketFlowAnim.VIRUS_DESTRUCTION -> 0.50f
    else -> 0.50f
  }
  val entrantTargetX = when (flowAnim) {
    PacketFlowAnim.INCOMING -> 0.14f
    PacketFlowAnim.OUT_BLOCK -> 0.05f
    PacketFlowAnim.OUT_PASS -> 0.22f
    PacketFlowAnim.OUT_QUARANTINE -> 0.12f
    PacketFlowAnim.VIRUS_DESTRUCTION -> 0.18f
    PacketFlowAnim.IDLE -> 0.15f
  }

  val animX by animateFloatAsState(
    targetValue = packetTargetX,
    animationSpec = tween(durationMillis = 1150),
    label = "packet_x",
  )
  val animY by animateFloatAsState(
    targetValue = packetTargetY,
    animationSpec = tween(durationMillis = 1150),
    label = "packet_y",
  )
  val entrantAnimX by animateFloatAsState(
    targetValue = entrantTargetX,
    animationSpec = tween(durationMillis = 1200),
    label = "entrant_x",
  )

  val infinite = rememberInfiniteTransition(label = "crt_pulse")
  val pulseAlpha by infinite.animateFloat(
    initialValue = 0.45f,
    targetValue = 1.0f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 620, easing = LinearEasing),
      repeatMode = RepeatMode.Reverse,
    ),
    label = "timer_blink",
  )

  var explosionFrame by remember { mutableIntStateOf(0) }
  LaunchedEffect(flowAnim, isVirusExplosionActive) {
    if (flowAnim == PacketFlowAnim.VIRUS_DESTRUCTION || isVirusExplosionActive) {
      while (true) {
        explosionFrame = (explosionFrame + 1) % 3
        delay(160L)
      }
    } else {
      explosionFrame = 0
    }
  }

  val isVirusAlert = flowAnim == PacketFlowAnim.VIRUS_DESTRUCTION || isVirusExplosionActive
  val mins = remainingSeconds / 60
  val secs = remainingSeconds % 60
  val timeStr = "%02d:%02d".format(mins, secs)

  val timerColor = when {
    remainingSeconds <= 20 -> PacketsPalette.CrtAlertRed.copy(alpha = pulseAlpha)
    remainingSeconds <= 45 -> PacketsPalette.CrtAlertAmber
    else -> style.crtPhosphorBright
  }

  Box(
    modifier = modifier
      .fillMaxWidth()
      .background(if (isVirusAlert) Color(0xFF280808) else style.crtBlack),
  ) {
    Column(modifier = Modifier.fillMaxSize()) {
      // Compact, high-readability top HUD (No outer border around screen)
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .background(Color.Black.copy(alpha = 0.25f))
          .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
          Text(
            text = "DAY %02d".format(day),
            style = MaterialTheme.typography.labelMedium,
            color = style.crtPhosphorBright,
          )
          Text(
            text = if (isJa) "処理 %d件".format(processedCount) else "CHK %d".format(processedCount),
            style = MaterialTheme.typography.labelSmall,
            color = style.crtPhosphorMid,
          )
        }

        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(style.crtGridDark.copy(alpha = 0.55f))
            .padding(horizontal = 12.dp, vertical = 5.dp),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = if (isJa) "残 $timeStr" else timeStr,
            style = MaterialTheme.typography.titleMedium,
            color = timerColor,
            fontWeight = FontWeight.Bold,
          )
        }

        Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(horizontalAlignment = Alignment.End) {
            Text(
              text = "¥%,d".format(budget),
              style = MaterialTheme.typography.labelMedium,
              color = style.crtPhosphorBright,
            )
            Text(
              text = if (isJa) "警$dailyWarnings/$freeWarnings" else "W$dailyWarnings/$freeWarnings",
              style = MaterialTheme.typography.labelSmall,
              color = if (dailyWarnings > freeWarnings) {
                PacketsPalette.CrtAlertRed
              } else {
                PacketsPalette.CrtAlertAmber
              },
            )
          }

          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(8.dp))
              .background(style.crtGridDark)
              .clickable(onClick = onOpenSettings)
              .heightIn(min = 38.dp)
              .widthIn(min = 46.dp)
              .padding(horizontal = 10.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = if (isJa) "⚙ 設定" else "⚙ SET",
              style = MaterialTheme.typography.labelMedium,
              color = style.crtPhosphorBright,
            )
          }
        }
      }

      // CRT Booth + Network Pixel Monitor Canvas
      Box(
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth()
          .padding(horizontal = 10.dp, vertical = 4.dp),
      ) {
        val entrantBitmap = remember(packetSeed) { PixelArtAssets.boothEntrantBitmap(packetSeed) }
        val extNode = PixelArtAssets.externalNodeSprite
        val intNode = PixelArtAssets.internalNodeSprite
        val fwGate = PixelArtAssets.firewallGateSprite
        val explosionBmp = PixelArtAssets.virusExplosionBitmap(explosionFrame)
        val packetBitmap = when (flowAnim) {
          PacketFlowAnim.OUT_BLOCK -> PixelArtAssets.alertPacketSprite
          PacketFlowAnim.OUT_QUARANTINE -> PixelArtAssets.quarantinePacketSprite
          PacketFlowAnim.VIRUS_DESTRUCTION -> PixelArtAssets.alertPacketSprite
          else -> PixelArtAssets.packetSprite
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
          val w = size.width
          val h = size.height

          // Retro CRT grid + scanlines
          val gridStep = 18.dp.toPx()
          val gridColor = if (isVirusAlert) {
            Color(0xFF4F1313).copy(alpha = 0.5f)
          } else {
            style.crtGridDark.copy(alpha = 0.4f)
          }
          var gx = 0f
          while (gx < w) {
            drawLine(gridColor, Offset(gx, 0f), Offset(gx, h), strokeWidth = 1f)
            gx += gridStep
          }
          var gy = 0f
          while (gy < h) {
            drawLine(gridColor, Offset(0f, gy), Offset(w, gy), strokeWidth = 1f)
            gy += gridStep
          }

          val boothFloorY = h * 0.78f
          val centerY = h * 0.48f

          // Left booth floor & window shutter silhouette (Papers, Please booth nod)
          drawLine(
            color = style.crtGridDark,
            start = Offset(w * 0.03f, boothFloorY),
            end = Offset(w * 0.97f, boothFloorY),
            strokeWidth = 2.dp.toPx(),
          )

          // Entrant silhouette on left side walking up to the gate
          val entrantW = (h * 0.36f).toInt().coerceIn(28, 60)
          val entrantH = (entrantW * 20f / 16f).toInt()
          val entrantPx = (w * entrantAnimX - entrantW / 2f).toInt()
          val entrantPy = (boothFloorY - entrantH).toInt()
          drawImage(
            image = entrantBitmap,
            dstOffset = IntOffset(entrantPx, entrantPy),
            dstSize = IntSize(entrantW, entrantH),
            filterQuality = FilterQuality.None,
          )

          // Dashed transmission lane from EXT relay -> FW -> CORP CORE
          val laneColor = if (isVirusAlert) PacketsPalette.CrtAlertRed else style.crtPhosphorMid.copy(alpha = 0.72f)
          drawLine(
            color = laneColor,
            start = Offset(w * 0.28f, centerY),
            end = Offset(w * 0.84f, centerY),
            strokeWidth = 2.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f),
          )

          // Quarantine branch line going down from Firewall Gate
          drawLine(
            color = PacketsPalette.QuarantineLight.copy(alpha = 0.65f),
            start = Offset(w * 0.50f, centerY),
            end = Offset(w * 0.50f, h * 0.84f),
            strokeWidth = 2.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(7f, 6f), 0f),
          )

          val nodeSize = (h * 0.38f).toInt().coerceIn(32, 62)

          // External node
          drawImage(
            image = extNode,
            dstOffset = IntOffset((w * 0.28f - nodeSize / 2f).toInt(), (centerY - nodeSize / 2f).toInt()),
            dstSize = IntSize(nodeSize, nodeSize),
            filterQuality = FilterQuality.None,
          )

          // Firewall gate center
          drawImage(
            image = fwGate,
            dstOffset = IntOffset((w * 0.50f - nodeSize / 2f).toInt(), (centerY - nodeSize / 2f).toInt()),
            dstSize = IntSize(nodeSize, nodeSize),
            filterQuality = FilterQuality.None,
          )

          // Internal corporate core node
          drawImage(
            image = intNode,
            dstOffset = IntOffset((w * 0.84f - nodeSize / 2f).toInt(), (centerY - nodeSize / 2f).toInt()),
            dstSize = IntSize(nodeSize, nodeSize),
            filterQuality = FilterQuality.None,
          )

          // Drop / Quarantine bin marker at bottom
          val binSize = 18.dp.toPx()
          drawRect(
            color = PacketsPalette.QuarantineLight.copy(alpha = 0.35f),
            topLeft = Offset(w * 0.50f - binSize / 2f, h * 0.80f),
            size = Size(binSize, binSize * 0.65f),
          )

          // Moving packet sprite (when not obscured by full explosion or while traveling)
          val pktW = (h * 0.30f).toInt().coerceIn(24, 48)
          val pktH = (pktW * 0.72f).toInt()
          drawImage(
            image = packetBitmap,
            dstOffset = IntOffset((w * animX - pktW / 2f).toInt(), (h * animY - pktH / 2f).toInt()),
            dstSize = IntSize(pktW, pktH),
            filterQuality = FilterQuality.None,
          )

          // Fatal virus detonation animation over internal core node
          if (isVirusAlert) {
            val expSize = (nodeSize * 1.45f).toInt()
            drawImage(
              image = explosionBmp,
              dstOffset = IntOffset((w * 0.84f - expSize / 2f).toInt(), (centerY - expSize / 2f).toInt()),
              dstSize = IntSize(expSize, expSize),
              filterQuality = FilterQuality.None,
            )
          }
        }

        // Node role captions along top-left/center/right of lane
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.TopCenter)
            .padding(horizontal = 6.dp, vertical = 2.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text(
            text = if (isJa) "発信窓口" else "EXT RELAY",
            style = MaterialTheme.typography.labelSmall,
            color = style.crtPhosphorMid.copy(alpha = 0.85f),
          )
          Text(
            text = "FW GATE",
            style = MaterialTheme.typography.labelSmall,
            color = style.crtPhosphorBright,
          )
          Text(
            text = if (isVirusAlert) "!! BREACH !!" else if (isJa) "社内CORE" else "CORP CORE",
            style = MaterialTheme.typography.labelSmall,
            color = if (isVirusAlert) PacketsPalette.CrtAlertRed else style.crtPhosphorBright,
          )
        }

        // Critical breach banner during explosion
        if (isVirusAlert) {
          Box(
            modifier = Modifier
              .align(Alignment.Center)
              .clip(RoundedCornerShape(8.dp))
              .background(Color(0xE66E1010))
              .border(1.5.dp, PacketsPalette.CrtAlertRed, RoundedCornerShape(8.dp))
              .padding(horizontal = 14.dp, vertical = 6.dp),
          ) {
            Text(
              text = if (isJa) "CRITICAL BREACH // ウイルス侵入・破壊検知" else "CRITICAL BREACH // VIRUS DETONATION",
              style = MaterialTheme.typography.labelMedium,
              color = Color.White,
            )
          }
        } else if (activeAlerts.isNotEmpty()) {
          // Latest OS alert overlaid cleanly at bottom of monitor
          val alert = activeAlerts.last()
          Row(
            modifier = Modifier
              .align(Alignment.BottomCenter)
              .fillMaxWidth()
              .clip(RoundedCornerShape(8.dp))
              .background(if (alert.isPenalty) Color(0xF0521212) else Color(0xF02B2A10))
              .border(
                1.dp,
                if (alert.isPenalty) PacketsPalette.CrtAlertRed else PacketsPalette.CrtAlertAmber,
                RoundedCornerShape(8.dp),
              )
              .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = "${alert.title} ${alert.message}",
              style = MaterialTheme.typography.labelSmall,
              color = Color.White,
              maxLines = 2,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(Color.Black.copy(alpha = 0.45f))
                .clickable { onDismissAlert(alert.id) }
                .heightIn(min = 34.dp)
                .widthIn(min = 42.dp)
                .padding(horizontal = 10.dp, vertical = 4.dp),
              contentAlignment = Alignment.Center,
            ) {
              Text(
                text = "OK",
                style = MaterialTheme.typography.labelSmall,
                color = style.crtPhosphorBright,
              )
            }
          }
        }
      }

      // Clean booth divider line instead of enclosing screen rectangle
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(3.dp)
          .background(style.crtGridDark),
      )
    }
  }
}
