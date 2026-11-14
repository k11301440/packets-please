package com.tommy.packetsplease.ui.components

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.tommy.packetsplease.domain.OsAlert
import com.tommy.packetsplease.ui.PacketFlowAnim
import com.tommy.packetsplease.ui.pixel.PixelArtAssets
import com.tommy.packetsplease.ui.theme.PacketsPalette
import kotlin.math.roundToInt

@Composable
fun SurveillanceMonitor(
  day: Int,
  remainingSeconds: Int,
  budget: Int,
  dailyWarnings: Int,
  freeWarnings: Int,
  processedCount: Int,
  totalQueueCount: Int,
  flowAnim: PacketFlowAnim,
  activeAlerts: List<OsAlert>,
  onDismissAlert: (String) -> Unit,
  onOpenM1Menu: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val packetTargetProgress = when (flowAnim) {
    PacketFlowAnim.INCOMING -> 0.44f
    PacketFlowAnim.IDLE -> 0.44f
    PacketFlowAnim.OUT_PASS -> 0.86f
    PacketFlowAnim.OUT_BLOCK -> 0.14f
    PacketFlowAnim.OUT_QUARANTINE -> 0.50f
  }
  val animX by animateFloatAsState(
    targetValue = packetTargetProgress,
    animationSpec = tween(durationMillis = 160),
    label = "packetX",
  )
  val animY by animateFloatAsState(
    targetValue = if (flowAnim == PacketFlowAnim.OUT_QUARANTINE) 0.78f else 0.48f,
    animationSpec = tween(durationMillis = 160),
    label = "packetY",
  )

  val minutes = remainingSeconds / 60
  val secs = remainingSeconds % 60
  val timeStr = "%02d:%02d".format(minutes, secs)

  Box(
    modifier = modifier
      .fillMaxWidth()
      .background(PacketsPalette.CrtBlack)
      .border(2.dp, PacketsPalette.CrtGridDark)
      .padding(horizontal = 8.dp, vertical = 6.dp),
  ) {
    Column(modifier = Modifier.fillMaxSize()) {
      // 上部HUDバー
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "DAY %02d  [%d/%d]".format(day, (processedCount + 1).coerceAtMost(totalQueueCount.coerceAtLeast(1)), totalQueueCount),
          style = MaterialTheme.typography.labelMedium,
          color = PacketsPalette.CrtPhosphorBright,
        )
        Text(
          text = "残 $timeStr",
          style = MaterialTheme.typography.labelMedium,
          color = if (remainingSeconds <= 30) PacketsPalette.CrtAlertRed else PacketsPalette.CrtPhosphorBright,
        )
        Text(
          text = "予算 ¥%,d".format(budget),
          style = MaterialTheme.typography.labelMedium,
          color = if (budget < 1500) PacketsPalette.CrtAlertAmber else PacketsPalette.CrtPhosphorMid,
        )
        Text(
          text = "警$dailyWarnings/$freeWarnings",
          style = MaterialTheme.typography.labelSmall,
          color = if (dailyWarnings > freeWarnings) PacketsPalette.CrtAlertRed else PacketsPalette.CrtPhosphorMid,
        )
        Box(
          modifier = Modifier
            .border(1.dp, PacketsPalette.CrtPhosphorMid)
            .clickable { onOpenM1Menu() }
            .padding(horizontal = 5.dp, vertical = 1.dp),
        ) {
          Text(
            text = "M1",
            style = MaterialTheme.typography.labelSmall,
            color = PacketsPalette.CrtPhosphorBright,
          )
        }
      }

      Spacer(modifier = Modifier.height(4.dp))

      // ピクセルアートネットワークフロー図 (タッチ不可の劇場領域)
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
          .border(1.dp, PacketsPalette.CrtGridDark),
      ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
          drawCrtGridAndScanlines()
          val w = size.width
          val h = size.height

          // 通信経路の破線
          val laneY = (h * 0.48f).roundToInt().toFloat()
          drawLine(
            color = PacketsPalette.CrtPhosphorMid,
            start = Offset(w * 0.14f, laneY),
            end = Offset(w * 0.86f, laneY),
            strokeWidth = 3f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f),
          )
          // 検疫分岐ライン
          drawLine(
            color = PacketsPalette.CrtAlertAmber,
            start = Offset(w * 0.50f, laneY),
            end = Offset(w * 0.50f, h * 0.82f),
            strokeWidth = 2f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f),
          )

          // ドット絵整数倍スケール基準 (3x or 2x スナップ)
          val pixelScale = (minOf(w / 180f, h / 64f)).toInt().coerceIn(2, 4)

          // 社外ノード
          val extBmp = PixelArtAssets.externalNodeSprite
          val extW = extBmp.width * pixelScale
          val extH = extBmp.height * pixelScale
          drawImage(
            image = extBmp,
            dstOffset = IntOffset((w * 0.08f - extW / 2f).roundToInt(), (laneY - extH / 2f).roundToInt()),
            dstSize = IntSize(extW, extH),
            filterQuality = FilterQuality.None,
          )

          // ファイアウォールゲート
          val fwBmp = PixelArtAssets.firewallGateSprite
          val fwW = fwBmp.width * pixelScale
          val fwH = fwBmp.height * pixelScale
          drawImage(
            image = fwBmp,
            dstOffset = IntOffset((w * 0.50f - fwW / 2f).roundToInt(), (laneY - fwH / 2f).roundToInt()),
            dstSize = IntSize(fwW, fwH),
            filterQuality = FilterQuality.None,
          )

          // 社内ノード
          val intBmp = PixelArtAssets.internalNodeSprite
          val intW = intBmp.width * pixelScale
          val intH = intBmp.height * pixelScale
          drawImage(
            image = intBmp,
            dstOffset = IntOffset((w * 0.92f - intW / 2f).roundToInt(), (laneY - intH / 2f).roundToInt()),
            dstSize = IntSize(intW, intH),
            filterQuality = FilterQuality.None,
          )

          // 移動中の通信パケットスプライト
          val pktBmp = when (flowAnim) {
            PacketFlowAnim.OUT_BLOCK -> PixelArtAssets.alertPacketSprite
            PacketFlowAnim.OUT_QUARANTINE -> PixelArtAssets.quarantinePacketSprite
            else -> PixelArtAssets.packetSprite
          }
          val pktW = pktBmp.width * pixelScale
          val pktH = pktBmp.height * pixelScale
          val px = (w * animX - pktW / 2f).roundToInt()
          val py = (h * animY - pktH / 2f).roundToInt()
          drawImage(
            image = pktBmp,
            dstOffset = IntOffset(px, py),
            dstSize = IntSize(pktW, pktH),
            filterQuality = FilterQuality.None,
          )
        }

        // ノードラベル
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.BottomCenter)
            .padding(horizontal = 8.dp, vertical = 2.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text("社外", style = MaterialTheme.typography.labelSmall, color = PacketsPalette.CrtAlertAmber)
          Text("FW検閲ゲート / 検疫(Q)", style = MaterialTheme.typography.labelSmall, color = PacketsPalette.CrtPhosphorMid)
          Text("社内LAN", style = MaterialTheme.typography.labelSmall, color = PacketsPalette.CrtPhosphorBright)
        }

        // OS不正検知アラート (作業中断せずタップで閉じる)
        val topAlert = activeAlerts.lastOrNull()
        if (topAlert != null) {
          val alertBorder = when {
            topAlert.isBribeNotice -> PacketsPalette.CrtAlertAmber
            topAlert.isPenalty -> PacketsPalette.CrtAlertRed
            else -> PacketsPalette.CrtAlertAmber
          }
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .align(Alignment.TopCenter)
              .padding(4.dp)
              .background(PacketsPalette.CrtBlack.copy(alpha = 0.94f))
              .border(2.dp, alertBorder)
              .clickable { onDismissAlert(topAlert.id) }
              .padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = topAlert.title,
                style = MaterialTheme.typography.labelMedium,
                color = alertBorder,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Text(
                text = topAlert.message,
                style = MaterialTheme.typography.labelSmall,
                color = PacketsPalette.CrtPhosphorBright,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
              )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "[確認済]",
              style = MaterialTheme.typography.labelSmall,
              color = alertBorder,
            )
          }
        }
      }
    }
  }
}

private fun DrawScope.drawCrtGridAndScanlines() {
  val step = 16f
  var x = 0f
  while (x < size.width) {
    drawLine(
      color = PacketsPalette.CrtGridDark,
      start = Offset(x, 0f),
      end = Offset(x, size.height),
      strokeWidth = 1f,
    )
    x += step
  }
  var y = 0f
  while (y < size.height) {
    drawLine(
      color = PacketsPalette.CrtGridDark,
      start = Offset(0f, y),
      end = Offset(size.width, y),
      strokeWidth = 1f,
    )
    y += step
  }
}
