package com.tommy.packetsplease.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.tommy.packetsplease.domain.InspectableTarget
import com.tommy.packetsplease.domain.InspectionResult
import com.tommy.packetsplease.domain.Packet
import com.tommy.packetsplease.domain.PacketField
import com.tommy.packetsplease.domain.PolicyRule
import com.tommy.packetsplease.domain.ToolType
import com.tommy.packetsplease.domain.Verdict
import com.tommy.packetsplease.ui.GameUiState
import com.tommy.packetsplease.ui.pixel.PixelArtAssets
import com.tommy.packetsplease.ui.theme.PacketsPalette

@Composable
fun InspectionDeskArea(
  state: GameUiState,
  onSelectInspectTarget: (InspectableTarget) -> Unit,
  onClearInspection: () -> Unit,
  onToggleInspectionMode: () -> Unit,
  onTogglePolicyBook: () -> Unit,
  onToggleToolTray: () -> Unit,
  onOpenTool: (ToolType) -> Unit,
  onCloseToolSlip: () -> Unit,
  onApplyVerdict: (Verdict) -> Unit,
  modifier: Modifier = Modifier,
) {
  val packet = state.currentPacket
  Column(
    modifier = modifier
      .fillMaxWidth()
      .background(PacketsPalette.DeskDark)
      .padding(horizontal = 8.dp, vertical = 6.dp),
  ) {
    // ステータス・直前フィードバック帯
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .background(PacketsPalette.DeskMid)
        .border(1.dp, PacketsPalette.DeskLight)
        .padding(horizontal = 8.dp, vertical = 3.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      val banner = state.lastFeedbackBanner ?: "通信内容を検査し規則と突合してください"
      Text(
        text = banner,
        style = MaterialTheme.typography.labelSmall,
        color = PacketsPalette.PaperBright,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.weight(1f),
      )
      if (state.isInspectionMode) {
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = if (state.firstInspectTarget == null) "[突合:A選択]" else "[突合:B選択]",
          style = MaterialTheme.typography.labelSmall,
          color = PacketsPalette.QuarantineLight,
        )
      }
    }

    Spacer(modifier = Modifier.height(5.dp))

    // 中央メイン領域 (通信カード / 突合拡大ビュー / ツールスリップ / ポリシーブック)
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .weight(1f),
    ) {
      if (packet != null) {
        Column(modifier = Modifier.fillMaxSize()) {
          // 突合拡大ビュー (選択中または結果が出ているときに上下重ね拡大表示)
          if (state.firstInspectTarget != null) {
            InspectionCompareOverlay(
              first = state.firstInspectTarget,
              second = state.secondInspectTarget,
              result = state.lastInspectionResult,
              onClear = onClearInspection,
              onOpenRecommendedTool = onOpenTool,
            )
            Spacer(modifier = Modifier.height(4.dp))
          }

          // 通信カード本体
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .weight(1f),
          ) {
            PacketCard(
              packet = packet,
              selectedTargetIds = setOfNotNull(
                state.firstInspectTarget?.targetId,
                state.secondInspectTarget?.targetId,
              ),
              onSelectTarget = onSelectInspectTarget,
              modifier = Modifier.fillMaxSize(),
            )

            // ツール結果スリップ (通信カードと同じ紙形式で上に重ねて表示)
            val toolType = state.activeToolSlip
            if (toolType != null) {
              ToolResultSlip(
                packet = packet,
                toolType = toolType,
                isSelected = state.firstInspectTarget?.targetId == "tool.${toolType.name}" ||
                  state.secondInspectTarget?.targetId == "tool.${toolType.name}",
                onSelectAsTarget = onSelectInspectTarget,
                onClose = onCloseToolSlip,
                modifier = Modifier
                  .align(Alignment.BottomEnd)
                  .padding(6.dp),
              )
            }

            // スタンプ押印アニメーション (自作ドット絵スタンプテクスチャ)
            val stamped = state.stampedVerdictAnim
            if (stamped != null) {
              Box(
                modifier = Modifier
                  .fillMaxSize()
                  .background(Color.Black.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
              ) {
                val stampBmp = PixelArtAssets.getStampBitmap(stamped)
                Canvas(modifier = Modifier.size(width = 210.dp, height = 64.dp)) {
                  val scale = (minOf(size.width / stampBmp.width, size.height / stampBmp.height))
                    .toInt()
                    .coerceAtLeast(2)
                  val drawW = stampBmp.width * scale
                  val drawH = stampBmp.height * scale
                  drawImage(
                    image = stampBmp,
                    dstOffset = IntOffset(
                      ((size.width - drawW) / 2f).toInt(),
                      ((size.height - drawH) / 2f).toInt(),
                    ),
                    dstSize = IntSize(drawW, drawH),
                    filterQuality = FilterQuality.None,
                  )
                }
              }
            }
          }
        }
      }

      // ポリシーブックオーバーレイ (画面下部から呼び出せる参照資料)
      androidx.compose.animation.AnimatedVisibility(
        visible = state.isPolicyBookOpen,
        enter = fadeIn(tween(120)),
        exit = fadeOut(tween(100)),
        modifier = Modifier.fillMaxSize(),
      ) {
        PolicyBookOverlay(
          day = state.currentDay,
          rules = state.activeRules,
          selectedTargetIds = setOfNotNull(
            state.firstInspectTarget?.targetId,
            state.secondInspectTarget?.targetId,
          ),
          onSelectRule = onSelectInspectTarget,
          onClose = onTogglePolicyBook,
        )
      }

      // ツール選択トレイ (操作エリア下端直上に出る4ツールパレット)
      androidx.compose.animation.AnimatedVisibility(
        visible = state.isToolTrayOpen,
        enter = fadeIn(tween(110)),
        exit = fadeOut(tween(90)),
        modifier = Modifier
          .align(Alignment.BottomCenter)
          .padding(bottom = 4.dp),
      ) {
        ToolPaletteStrip(
          usedTools = state.inspectedToolsForCurrentPacket,
          onPickTool = onOpenTool,
        )
      }
    }

    Spacer(modifier = Modifier.height(6.dp))

    // 下端 常設アクショントレイ
    PermanentActionTray(
      isInspectionMode = state.isInspectionMode,
      isPolicyBookOpen = state.isPolicyBookOpen,
      isToolTrayOpen = state.isToolTrayOpen,
      activeToolCount = state.inspectedToolsForCurrentPacket.size,
      onTogglePolicyBook = onTogglePolicyBook,
      onToggleToolTray = onToggleToolTray,
      onToggleInspection = onToggleInspectionMode,
      onApplyVerdict = onApplyVerdict,
    )
  }
}

@Composable
private fun InspectionCompareOverlay(
  first: InspectableTarget,
  second: InspectableTarget?,
  result: InspectionResult?,
  onClear: () -> Unit,
  onOpenRecommendedTool: (ToolType) -> Unit,
) {
  val borderColor = when {
    result == null -> PacketsPalette.QuarantineLight
    result.matched -> PacketsPalette.CrtPhosphorMid
    else -> PacketsPalette.BlockLight
  }
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .background(PacketsPalette.DeskMid)
      .border(2.dp, borderColor)
      .padding(6.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = result?.title ?: "突合モード: 2つ目の比較対象をタップ",
        style = MaterialTheme.typography.labelMedium,
        color = borderColor,
      )
      Text(
        text = "[閉じる]",
        style = MaterialTheme.typography.labelSmall,
        color = PacketsPalette.PaperBright,
        modifier = Modifier.clickable { onClear() },
      )
    }
    Spacer(modifier = Modifier.height(3.dp))
    // 上下2フィールド重ね拡大比較
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .background(PacketsPalette.PaperBright)
        .border(1.dp, PacketsPalette.InkDark)
        .padding(horizontal = 6.dp, vertical = 3.dp),
    ) {
      Text(
        text = "A [${first.label}]: ${first.value}",
        style = MaterialTheme.typography.labelMedium,
        color = PacketsPalette.InkDark,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )
    }
    Spacer(modifier = Modifier.height(2.dp))
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .background(PacketsPalette.PaperMid)
        .border(1.dp, PacketsPalette.InkDark)
        .padding(horizontal = 6.dp, vertical = 3.dp),
    ) {
      Text(
        text = if (second != null) "B [${second.label}]: ${second.value}" else "B: カード項目 / 規則 / ツール結果を選択...",
        style = MaterialTheme.typography.labelMedium,
        color = PacketsPalette.InkDark,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )
    }
    if (result != null) {
      Spacer(modifier = Modifier.height(3.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = result.detail,
          style = MaterialTheme.typography.labelSmall,
          color = PacketsPalette.PaperBright,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f),
        )
        val recTool = result.recommendedTool
        if (recTool != null) {
          Spacer(modifier = Modifier.width(6.dp))
          Box(
            modifier = Modifier
              .background(PacketsPalette.CrtBlack)
              .border(1.dp, PacketsPalette.CrtPhosphorBright)
              .clickable { onOpenRecommendedTool(recTool) }
              .padding(horizontal = 6.dp, vertical = 3.dp),
          ) {
            Text(
              text = "${recTool.shortLabel}起動",
              style = MaterialTheme.typography.labelSmall,
              color = PacketsPalette.CrtPhosphorBright,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun PacketCard(
  packet: Packet,
  selectedTargetIds: Set<String>,
  onSelectTarget: (InspectableTarget) -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier
      .background(PacketsPalette.PaperBright)
      .border(2.dp, PacketsPalette.PaperShadow)
      .padding(8.dp),
    verticalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    // 差出人ヘッダ + 24x24ピクセルアバター
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      val avatarBmp = PixelArtAssets.getAvatarBitmap(packet.from.avatarSeed)
      Canvas(
        modifier = Modifier
          .size(48.dp)
          .border(1.dp, PacketsPalette.InkDark),
      ) {
        drawImage(
          image = avatarBmp,
          dstOffset = IntOffset.Zero,
          dstSize = IntSize(size.width.toInt(), size.height.toInt()),
          filterQuality = FilterQuality.None,
        )
      }
      Spacer(modifier = Modifier.width(8.dp))
      Column(modifier = Modifier.weight(1f)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "${packet.from.displayName} [${packet.from.department}]",
            style = MaterialTheme.typography.titleMedium,
            color = PacketsPalette.InkDark,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
          )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
          InspectableChip(
            target = InspectableTarget(
              targetId = "from.email",
              field = PacketField.FROM,
              label = "差出人アドレス",
              value = packet.from.email,
            ),
            isSelected = "from.email" in selectedTargetIds,
            text = packet.from.email,
            onSelect = onSelectTarget,
            modifier = Modifier.weight(1.2f),
          )
          InspectableChip(
            target = InspectableTarget(
              targetId = "from.auth",
              field = PacketField.FROM,
              label = "認証方式",
              value = packet.from.authMethod,
            ),
            isSelected = "from.auth" in selectedTargetIds,
            text = packet.from.authMethod,
            onSelect = onSelectTarget,
            modifier = Modifier.weight(0.9f),
          )
        }
      }
    }

    // 宛先 & 日時行
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      val extTag = if (packet.to.isExternal) "[社外] " else "[社内] "
      InspectableChip(
        target = InspectableTarget(
          targetId = "to.email",
          field = PacketField.TO,
          label = "宛先",
          value = "$extTag${packet.to.email}",
        ),
        isSelected = "to.email" in selectedTargetIds,
        text = "宛先: $extTag${packet.to.email}",
        highlightAlert = packet.to.isExternal,
        onSelect = onSelectTarget,
        modifier = Modifier.weight(1.4f),
      )
      InspectableChip(
        target = InspectableTarget(
          targetId = "timestamp",
          field = PacketField.TIMESTAMP,
          label = "送信日時",
          value = packet.timestamp,
        ),
        isSelected = "timestamp" in selectedTargetIds,
        text = packet.timestamp.takeLast(11),
        onSelect = onSelectTarget,
        modifier = Modifier.weight(0.75f),
      )
    }

    // 件名
    InspectableChip(
      target = InspectableTarget(
        targetId = "subject",
        field = PacketField.SUBJECT,
        label = "件名",
        value = packet.subject,
      ),
      isSelected = "subject" in selectedTargetIds,
      text = "件名: ${packet.subject}",
      onSelect = onSelectTarget,
      modifier = Modifier.fillMaxWidth(),
    )

    // 本文ボックス
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .weight(1f)
        .background(PacketsPalette.PaperMid)
        .border(
          width = if ("body" in selectedTargetIds) 2.dp else 1.dp,
          color = if ("body" in selectedTargetIds) PacketsPalette.InkHighlight else PacketsPalette.PaperShadow,
        )
        .clickable {
          onSelectTarget(
            InspectableTarget(
              targetId = "body",
              field = PacketField.BODY,
              label = "本文",
              value = packet.body,
            ),
          )
        }
        .padding(6.dp),
    ) {
      Text(
        text = packet.body,
        style = MaterialTheme.typography.bodyMedium,
        color = PacketsPalette.InkDark,
        maxLines = 4,
        overflow = TextOverflow.Ellipsis,
      )
    }

    // 添付ファイル (存在する場合)
    val att = packet.attachment
    if (att != null) {
      InspectableChip(
        target = InspectableTarget(
          targetId = "attachment",
          field = PacketField.ATTACHMENT,
          label = "添付",
          value = "${att.fileName} (${att.declaredSizeKb}KB / ${att.quickScanStatus})",
        ),
        isSelected = "attachment" in selectedTargetIds,
        text = "添付: ${att.fileName} | 申告 ${att.declaredSizeKb}KB | ${att.quickScanStatus}",
        highlightAlert = att.quickScanStatus != "CLEAN",
        onSelect = onSelectTarget,
        modifier = Modifier.fillMaxWidth(),
      )
    }

    // 認証・証明書情報 (存在する場合)
    val cert = packet.certificate
    if (cert != null) {
      InspectableChip(
        target = InspectableTarget(
          targetId = "certificate",
          field = PacketField.CERTIFICATE,
          label = "証明書",
          value = "${cert.issuer} / 期限:${cert.validUntil} / ${cert.tokenType}",
        ),
        isSelected = "certificate" in selectedTargetIds,
        text = "証明書: ${cert.issuer} | 期限 ${cert.validUntil} | ${cert.tokenType}",
        onSelect = onSelectTarget,
        modifier = Modifier.fillMaxWidth(),
      )
    }

    // 袖之下付箋 (賄賂イベント)
    val bribe = packet.bribeOffer
    if (bribe != null) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .background(PacketsPalette.BribeNoteBg)
          .border(1.dp, PacketsPalette.QuarantineDark)
          .padding(horizontal = 6.dp, vertical = 3.dp),
      ) {
        Text(
          text = "【袖之下付箋】${bribe.senderNote}",
          style = MaterialTheme.typography.labelSmall,
          color = PacketsPalette.InkDark,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }

    // 監査回避ネットワーク(AAN)指令帯
    val aanNote = packet.auditAvoidanceInstruction
    if (packet.isAuditAvoidanceEvent && aanNote != null) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .background(PacketsPalette.CrtBlack)
          .border(1.dp, PacketsPalette.CrtPhosphorBright)
          .padding(horizontal = 6.dp, vertical = 3.dp),
      ) {
        Text(
          text = aanNote,
          style = MaterialTheme.typography.labelSmall,
          color = PacketsPalette.CrtPhosphorBright,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}

@Composable
private fun InspectableChip(
  target: InspectableTarget,
  isSelected: Boolean,
  text: String,
  onSelect: (InspectableTarget) -> Unit,
  modifier: Modifier = Modifier,
  highlightAlert: Boolean = false,
) {
  val bg = when {
    isSelected -> PacketsPalette.QuarantineLight.copy(alpha = 0.35f)
    highlightAlert -> PacketsPalette.BlockLight.copy(alpha = 0.16f)
    else -> PacketsPalette.PaperMid
  }
  val border = when {
    isSelected -> PacketsPalette.InkHighlight
    highlightAlert -> PacketsPalette.BlockMid
    else -> PacketsPalette.PaperShadow
  }
  Box(
    modifier = modifier
      .background(bg)
      .border(if (isSelected) 2.dp else 1.dp, border)
      .clickable { onSelect(target) }
      .padding(horizontal = 5.dp, vertical = 3.dp),
  ) {
    Text(
      text = text,
      style = MaterialTheme.typography.labelSmall,
      color = PacketsPalette.InkDark,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

@Composable
private fun ToolResultSlip(
  packet: Packet,
  toolType: ToolType,
  isSelected: Boolean,
  onSelectAsTarget: (InspectableTarget) -> Unit,
  onClose: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val (resultText, flagged) = when (toolType) {
    ToolType.THREAT_DB -> packet.tools.threatDbResult to packet.tools.threatDbFlagged
    ToolType.PLAINTEXT_LOG -> packet.tools.plaintextLogResult to packet.tools.plaintextFlagged
    ToolType.TRAFFIC_SCALE -> packet.tools.trafficScaleNote to packet.tools.trafficFlagged
    ToolType.DEEP_SCAN -> packet.tools.deepScanResult to packet.tools.deepScanFlagged
  }
  val targetId = "tool.${toolType.name}"
  Column(
    modifier = modifier
      .fillMaxWidth(0.92f)
      .background(PacketsPalette.SlipPaper)
      .border(2.dp, if (isSelected) PacketsPalette.InkHighlight else if (flagged) PacketsPalette.BlockMid else PacketsPalette.InkDark)
      .clickable {
        onSelectAsTarget(
          InspectableTarget(
            targetId = targetId,
            field = PacketField.TOOL_RESULT,
            label = toolType.displayName,
            value = resultText,
          ),
        )
      }
      .padding(7.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = "[出力紙] ${toolType.displayName} (タップで突合)",
        style = MaterialTheme.typography.labelSmall,
        color = if (flagged) PacketsPalette.BlockDark else PacketsPalette.InkMuted,
      )
      Text(
        text = "[X]",
        style = MaterialTheme.typography.labelSmall,
        color = PacketsPalette.InkDark,
        modifier = Modifier.clickable { onClose() },
      )
    }
    Spacer(modifier = Modifier.height(2.dp))
    Text(
      text = resultText,
      style = MaterialTheme.typography.bodyMedium,
      color = if (flagged) PacketsPalette.BlockDark else PacketsPalette.InkDark,
      maxLines = 3,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

@Composable
private fun PolicyBookOverlay(
  day: Int,
  rules: List<PolicyRule>,
  selectedTargetIds: Set<String>,
  onSelectRule: (InspectableTarget) -> Unit,
  onClose: () -> Unit,
) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(PacketsPalette.BookCover)
      .border(2.dp, PacketsPalette.InkDark)
      .padding(6.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = "社内セキュリティポリシーブック (DAY $day)",
        style = MaterialTheme.typography.titleMedium,
        color = PacketsPalette.PaperBright,
      )
      Box(
        modifier = Modifier
          .background(PacketsPalette.PaperBright)
          .border(1.dp, PacketsPalette.InkDark)
          .clickable { onClose() }
          .padding(horizontal = 8.dp, vertical = 2.dp),
      ) {
        Text("閉じる", style = MaterialTheme.typography.labelSmall, color = PacketsPalette.InkDark)
      }
    }
    Spacer(modifier = Modifier.height(4.dp))
    Column(
      modifier = Modifier
        .fillMaxSize()
        .background(PacketsPalette.BookPaper)
        .border(1.dp, PacketsPalette.PaperShadow)
        .verticalScroll(rememberScrollState())
        .padding(6.dp),
      verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
      Text(
        text = "▼ 認定ドメイン: olease.co.jp / partner-cloud.jp / yamato-cpa.jp / jdc-infra.jp",
        style = MaterialTheme.typography.labelSmall,
        color = PacketsPalette.InkMuted,
      )
      Text(
        text = "▼ 各条項をタップすると通信カード項目と突合できます",
        style = MaterialTheme.typography.labelSmall,
        color = PacketsPalette.InkHighlight,
      )
      rules.forEach { rule ->
        val targetId = "rule.${rule.id}"
        val selected = targetId in selectedTargetIds
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .background(if (selected) PacketsPalette.QuarantineLight.copy(alpha = 0.35f) else PacketsPalette.PaperBright)
            .border(if (selected) 2.dp else 1.dp, if (selected) PacketsPalette.InkHighlight else PacketsPalette.PaperShadow)
            .clickable {
              onSelectRule(
                InspectableTarget(
                  targetId = targetId,
                  field = PacketField.RULE_ITEM,
                  label = rule.id,
                  value = rule.title,
                ),
              )
            }
            .padding(6.dp),
        ) {
          Text(
            text = "${rule.title} [DAY ${rule.effectiveFromDay}〜]",
            style = MaterialTheme.typography.labelMedium,
            color = PacketsPalette.InkDark,
          )
          Text(
            text = rule.description,
            style = MaterialTheme.typography.labelSmall,
            color = PacketsPalette.InkMuted,
          )
        }
      }
    }
  }
}

@Composable
private fun ToolPaletteStrip(
  usedTools: Set<ToolType>,
  onPickTool: (ToolType) -> Unit,
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .background(PacketsPalette.DeskMid)
      .border(2.dp, PacketsPalette.CrtPhosphorMid)
      .padding(6.dp),
    horizontalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    ToolType.entries.forEach { tool ->
      val isUsed = tool in usedTools
      Box(
        modifier = Modifier
          .weight(1f)
          .background(if (isUsed) PacketsPalette.CrtGridDark else PacketsPalette.CrtBlack)
          .border(1.dp, PacketsPalette.CrtPhosphorMid)
          .clickable { onPickTool(tool) }
          .padding(vertical = 6.dp, horizontal = 2.dp),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = tool.shortLabel,
          style = MaterialTheme.typography.labelSmall,
          color = PacketsPalette.CrtPhosphorBright,
          maxLines = 1,
        )
      }
    }
  }
}

@Composable
private fun PermanentActionTray(
  isInspectionMode: Boolean,
  isPolicyBookOpen: Boolean,
  isToolTrayOpen: Boolean,
  activeToolCount: Int,
  onTogglePolicyBook: () -> Unit,
  onToggleToolTray: () -> Unit,
  onToggleInspection: () -> Unit,
  onApplyVerdict: (Verdict) -> Unit,
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .background(PacketsPalette.DeskMid)
      .border(1.dp, PacketsPalette.DeskLight)
      .padding(6.dp),
    verticalArrangement = Arrangement.spacedBy(5.dp),
  ) {
    // 補助ツールボタン列
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      UtilityTrayButton(
        label = if (isPolicyBookOpen) "規則[開]" else "規則ブック",
        active = isPolicyBookOpen,
        onClick = onTogglePolicyBook,
        modifier = Modifier.weight(1f),
      )
      UtilityTrayButton(
        label = if (activeToolCount > 0) "ツール($activeToolCount)" else "ツール",
        active = isToolTrayOpen,
        onClick = onToggleToolTray,
        modifier = Modifier.weight(1f),
      )
      UtilityTrayButton(
        label = if (isInspectionMode) "突合[ON]" else "突合モード",
        active = isInspectionMode,
        onClick = onToggleInspection,
        modifier = Modifier.weight(1f),
      )
    }

    // スタンプボタン列 (PASS / BLOCK / QUARANTINE)
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      StampActionButton(
        verdict = Verdict.PASS,
        jpCaption = "通過",
        bg = PacketsPalette.PassMid,
        border = PacketsPalette.PassLight,
        shadow = PacketsPalette.PassDark,
        onTrigger = { onApplyVerdict(Verdict.PASS) },
        modifier = Modifier.weight(1f),
      )
      StampActionButton(
        verdict = Verdict.BLOCK,
        jpCaption = "遮断",
        bg = PacketsPalette.BlockMid,
        border = PacketsPalette.BlockLight,
        shadow = PacketsPalette.BlockDark,
        onTrigger = { onApplyVerdict(Verdict.BLOCK) },
        modifier = Modifier.weight(1f),
      )
      StampActionButton(
        verdict = Verdict.QUARANTINE,
        jpCaption = "検疫",
        bg = PacketsPalette.QuarantineMid,
        border = PacketsPalette.QuarantineLight,
        shadow = PacketsPalette.QuarantineDark,
        onTrigger = { onApplyVerdict(Verdict.QUARANTINE) },
        modifier = Modifier.weight(1f),
      )
    }
  }
}

@Composable
private fun UtilityTrayButton(
  label: String,
  active: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Box(
    modifier = modifier
      .background(if (active) PacketsPalette.QuarantineMid else PacketsPalette.DeskDark)
      .border(1.dp, if (active) PacketsPalette.QuarantineLight else PacketsPalette.PaperShadow)
      .clickable { onClick() }
      .padding(vertical = 6.dp),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelMedium,
      color = if (active) PacketsPalette.InkDark else PacketsPalette.PaperBright,
      maxLines = 1,
    )
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun StampActionButton(
  verdict: Verdict,
  jpCaption: String,
  bg: Color,
  border: Color,
  shadow: Color,
  onTrigger: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val haptic = LocalHapticFeedback.current
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  val pressOffset by animateDpAsState(
    targetValue = if (isPressed) 3.dp else 0.dp,
    animationSpec = tween(durationMillis = 60),
    label = "stampPress",
  )

  Box(
    modifier = modifier
      .height(50.dp)
      .semantics { contentDescription = "${verdict.displayLabel} ($jpCaption)" }
      .background(shadow)
      .padding(bottom = 3.dp)
      .offset(y = pressOffset)
      .background(bg)
      .border(2.dp, border)
      .combinedClickable(
        interactionSource = interactionSource,
        indication = null,
        onClick = {
          haptic.performHapticFeedback(HapticFeedbackType.LongPress)
          onTrigger()
        },
        onLongClick = {
          haptic.performHapticFeedback(HapticFeedbackType.LongPress)
          onTrigger()
        },
      )
      .padding(horizontal = 4.dp, vertical = 2.dp),
    contentAlignment = Alignment.Center,
  ) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      val stampBmp = PixelArtAssets.getStampBitmap(verdict)
      Canvas(modifier = Modifier.size(width = 76.dp, height = 22.dp)) {
        val scale = (minOf(size.width / stampBmp.width, size.height / stampBmp.height))
          .toInt()
          .coerceAtLeast(1)
        val drawW = stampBmp.width * scale
        val drawH = stampBmp.height * scale
        drawImage(
          image = stampBmp,
          dstOffset = IntOffset(
            ((size.width - drawW) / 2f).toInt(),
            ((size.height - drawH) / 2f).toInt(),
          ),
          dstSize = IntSize(drawW, drawH),
          filterQuality = FilterQuality.None,
        )
      }
      Text(
        text = jpCaption,
        style = MaterialTheme.typography.labelSmall,
        color = Color.White,
      )
    }
  }
}
