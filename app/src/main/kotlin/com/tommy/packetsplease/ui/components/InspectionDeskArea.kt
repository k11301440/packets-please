package com.tommy.packetsplease.ui.components

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tommy.packetsplease.domain.AppLanguage
import com.tommy.packetsplease.domain.InspectableTarget
import com.tommy.packetsplease.domain.InspectionResult
import com.tommy.packetsplease.domain.Packet
import com.tommy.packetsplease.domain.PacketField
import com.tommy.packetsplease.domain.PolicyRule
import com.tommy.packetsplease.domain.ToolType
import com.tommy.packetsplease.domain.UpgradeState
import com.tommy.packetsplease.domain.UpgradeType
import com.tommy.packetsplease.domain.Verdict
import com.tommy.packetsplease.ui.GameUiState
import com.tommy.packetsplease.ui.pixel.PixelArtAssets
import com.tommy.packetsplease.ui.theme.LocalAppStyle
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
  onSkipInterval: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val style = LocalAppStyle.current
  val packet = state.currentPacket
  val isJa = state.settings.language == AppLanguage.JA

  val selectedIds = remember(state.firstInspectTarget, state.secondInspectTarget) {
    setOfNotNull(state.firstInspectTarget?.targetId, state.secondInspectTarget?.targetId)
  }

  Column(
    modifier = modifier
      .fillMaxWidth()
      .background(style.deskDark)
      .padding(horizontal = 10.dp, vertical = 6.dp),
    verticalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    if (state.isInspectionMode) {
      InspectionCompareOverlay(
        first = state.firstInspectTarget,
        second = state.secondInspectTarget,
        result = state.lastInspectionResult,
        isJa = isJa,
        onClear = onClearInspection,
        onQuickOpenTool = { tool -> onOpenTool(tool) },
      )
    } else if (state.newlyUnlockedAchievementBanner != null) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(8.dp))
          .background(Color(0xFF1A3A24))
          .border(1.dp, style.crtPhosphorBright, RoundedCornerShape(8.dp))
          .padding(horizontal = 12.dp, vertical = 6.dp),
      ) {
        Text(
          text = "🏆 ${state.newlyUnlockedAchievementBanner}",
          style = MaterialTheme.typography.labelMedium,
          color = style.crtPhosphorBright,
        )
      }
    } else if (state.lastFeedbackBanner != null) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(8.dp))
          .background(style.deskMid)
          .padding(horizontal = 10.dp, vertical = 5.dp),
      ) {
        Text(
          text = state.lastFeedbackBanner,
          style = MaterialTheme.typography.labelSmall,
          color = style.paperBright,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }

    Box(
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth()
        .clip(RoundedCornerShape(12.dp))
        .background(style.deskMid)
        .padding(8.dp),
    ) {
      if (packet != null) {
        PacketCard(
          packet = packet,
          upgrades = state.upgrades,
          quarantinedFamilies = state.quarantinedFamilies,
          isInspectionMode = state.isInspectionMode,
          selectedTargetIds = selectedIds,
          isJa = isJa,
          onSelectTarget = { target ->
            if (!state.isInspectionMode) {
              onToggleInspectionMode()
            }
            onSelectInspectTarget(target)
          },
          modifier = Modifier.fillMaxSize(),
        )
      }

      val activeTool = state.activeToolSlip
      if (activeTool != null && packet != null) {
        ToolResultSlip(
          packet = packet,
          toolType = activeTool,
          isSelected = "tool.${activeTool.name}" in selectedIds,
          isJa = isJa,
          onSelectAsTarget = { target ->
            if (!state.isInspectionMode) {
              onToggleInspectionMode()
            }
            onSelectInspectTarget(target)
          },
          onClose = onCloseToolSlip,
          modifier = Modifier.align(Alignment.BottomCenter),
        )
      }

      if (state.isPolicyBookOpen) {
        PolicyBookOverlay(
          rules = state.activeRules,
          selectedTargetIds = selectedIds,
          isJa = isJa,
          onSelectRule = { target ->
            if (!state.isInspectionMode) {
              onToggleInspectionMode()
            }
            onSelectInspectTarget(target)
          },
          onClose = onTogglePolicyBook,
          modifier = Modifier.fillMaxSize(),
        )
      }

      if (state.isToolTrayOpen) {
        ToolPaletteStrip(
          inspectedTools = state.inspectedToolsForCurrentPacket,
          onSelectTool = onOpenTool,
          onClose = onToggleToolTray,
          modifier = Modifier.align(Alignment.BottomCenter),
        )
      }

      if (state.isInterPacketInterval) {
        InterPacketIntervalOverlay(
          headline = state.intervalHeadline,
          subtext = state.intervalSubtext,
          stampedVerdict = state.stampedVerdictAnim,
          isVirusAlert = state.isVirusExplosionActive,
          isJa = isJa,
          onSkipInterval = onSkipInterval,
          modifier = Modifier.fillMaxSize(),
        )
      }
    }

    PermanentActionTray(
      isPolicyBookOpen = state.isPolicyBookOpen,
      isToolTrayOpen = state.isToolTrayOpen,
      isInspectionMode = state.isInspectionMode,
      isInterPacketInterval = state.isInterPacketInterval,
      quarantineRecommended = packet?.threatFamily != null &&
        packet.threatFamily in state.quarantinedFamilies &&
        state.upgrades.has(UpgradeType.QUARANTINE_INTEL),
      isJa = isJa,
      onTogglePolicyBook = onTogglePolicyBook,
      onToggleToolTray = onToggleToolTray,
      onToggleInspectionMode = onToggleInspectionMode,
      onApplyVerdict = onApplyVerdict,
    )
  }
}

@Composable
private fun InterPacketIntervalOverlay(
  headline: String,
  subtext: String,
  stampedVerdict: Verdict?,
  isVirusAlert: Boolean,
  isJa: Boolean,
  onSkipInterval: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val style = LocalAppStyle.current
  Box(
    modifier = modifier
      .clip(RoundedCornerShape(12.dp))
      .background(if (isVirusAlert) Color(0xF2380909) else Color(0xE6141814))
      .padding(16.dp),
    contentAlignment = Alignment.Center,
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .verticalScroll(rememberScrollState()),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      if (isVirusAlert) {
        Image(
          bitmap = PixelArtAssets.virusExplosionBitmap(1),
          contentDescription = "Virus Breach",
          filterQuality = FilterQuality.None,
          modifier = Modifier.size(68.dp),
        )
      } else if (stampedVerdict != null) {
        Image(
          bitmap = PixelArtAssets.getStampBitmap(stampedVerdict),
          contentDescription = stampedVerdict.displayLabel,
          filterQuality = FilterQuality.None,
          modifier = Modifier
            .width(148.dp)
            .height(52.dp),
        )
      }

      Text(
        text = headline,
        style = MaterialTheme.typography.titleMedium,
        color = if (isVirusAlert) PacketsPalette.BlockLight else style.crtPhosphorBright,
        textAlign = TextAlign.Center,
      )

      Text(
        text = subtext,
        style = MaterialTheme.typography.bodyMedium,
        color = style.paperBright,
        textAlign = TextAlign.Center,
      )

      Text(
        text = if (isJa) {
          "上部モニターで通信パケットを転送中… 一息つくか下のボタンで即座に次を呼べます"
        } else {
          "Transferring packet across gate... Take a breath or call next packet now."
        },
        style = MaterialTheme.typography.labelSmall,
        color = style.paperMid.copy(alpha = 0.85f),
        textAlign = TextAlign.Center,
      )

      Box(
        modifier = Modifier
          .fillMaxWidth(0.88f)
          .heightIn(min = 54.dp)
          .clip(RoundedCornerShape(10.dp))
          .background(if (isVirusAlert) PacketsPalette.BlockMid else style.crtGridDark)
          .border(
            1.5.dp,
            if (isVirusAlert) PacketsPalette.BlockLight else style.crtPhosphorBright,
            RoundedCornerShape(10.dp),
          )
          .clickable(onClick = onSkipInterval)
          .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = if (isJa) "次の通信をすぐ呼ぶ ▶" else "CALL NEXT PACKET ▶",
          style = MaterialTheme.typography.titleMedium,
          color = Color.White,
        )
      }
    }
  }
}

@Composable
private fun InspectionCompareOverlay(
  first: InspectableTarget?,
  second: InspectableTarget?,
  result: InspectionResult?,
  isJa: Boolean,
  onClear: () -> Unit,
  onQuickOpenTool: (ToolType) -> Unit,
) {
  val style = LocalAppStyle.current
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(10.dp))
      .background(Color(0xFF15241B))
      .border(1.5.dp, style.crtPhosphorMid, RoundedCornerShape(10.dp))
      .padding(horizontal = 10.dp, vertical = 8.dp),
    verticalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      val statusText = when {
        first == null -> if (isJa) "【照合チェック】比較したい項目を2つタップしてください" else "[INSPECT] Tap 2 items to compare"
        second == null -> if (isJa) "1項目目: [${first.label}] → 比較する2項目目をタップ" else "1st: [${first.label}] -> Tap 2nd item"
        else -> if (isJa) "照合完了: [${first.label}] × [${second.label}]" else "Matched: [${first.label}] x [${second.label}]"
      }
      Text(
        text = statusText,
        style = MaterialTheme.typography.labelMedium,
        color = style.crtPhosphorBright,
        modifier = Modifier.weight(1f),
      )
      if (first != null) {
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(style.crtGridDark)
            .clickable(onClick = onClear)
            .heightIn(min = 34.dp)
            .padding(horizontal = 10.dp, vertical = 5.dp),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = if (isJa) "リセット" else "CLEAR",
            style = MaterialTheme.typography.labelSmall,
            color = style.crtPhosphorBright,
          )
        }
      }
    }

    if (result != null) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "${result.title}: ${result.detail}",
          style = MaterialTheme.typography.bodyMedium,
          color = if (!result.matched) PacketsPalette.QuarantineLight else style.paperBright,
          modifier = Modifier.weight(1f),
        )
        val recTool = result.recommendedTool
        if (recTool != null) {
          Spacer(modifier = Modifier.width(8.dp))
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(6.dp))
              .background(PacketsPalette.QuarantineDark)
              .clickable { onQuickOpenTool(recTool) }
              .heightIn(min = 36.dp)
              .padding(horizontal = 10.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = "${recTool.shortLabel}を開く",
              style = MaterialTheme.typography.labelSmall,
              color = PacketsPalette.QuarantineLight,
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
  upgrades: UpgradeState,
  quarantinedFamilies: Set<String>,
  isInspectionMode: Boolean,
  selectedTargetIds: Set<String>,
  isJa: Boolean,
  onSelectTarget: (InspectableTarget) -> Unit,
  modifier: Modifier = Modifier,
) {
  val style = LocalAppStyle.current
  val avatarBmp = remember(packet.from.avatarSeed) {
    PixelArtAssets.getAvatarBitmap(packet.from.avatarSeed)
  }

  val isExternalSender = !packet.from.email.endsWith("@olease.co.jp")
  val showExternalBadge = upgrades.has(UpgradeType.EXTERNAL_HIGHLIGHT) && (isExternalSender || packet.to.isExternal)
  val showScannerLv1Badge = upgrades.has(UpgradeType.AUTO_SCANNER_LV1) && (
    packet.tools.plaintextFlagged ||
      packet.from.authMethod.contains("FAIL", ignoreCase = true) ||
      packet.from.authMethod.contains("NONE", ignoreCase = true)
    )
  val showScannerLv2Badge = upgrades.has(UpgradeType.AUTO_SCANNER_LV2) && (
    packet.tools.deepScanFlagged ||
      packet.tools.threatDbFlagged ||
      packet.tools.trafficFlagged ||
      (packet.attachment != null && packet.attachment.quickScanStatus != "CLEAN")
    )
  val knownThreatFamily = packet.threatFamily?.takeIf { it in quarantinedFamilies }

  Column(
    modifier = modifier
      .clip(RoundedCornerShape(10.dp))
      .background(style.paperBright)
      .padding(12.dp)
      .verticalScroll(rememberScrollState()),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    if (showExternalBadge || showScannerLv1Badge || showScannerLv2Badge || knownThreatFamily != null) {
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (knownThreatFamily != null) {
          val hasIntelUpgrade = upgrades.has(UpgradeType.QUARANTINE_INTEL)
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(8.dp))
              .background(if (hasIntelUpgrade) Color(0xFF5E1919) else Color(0xFF5C4212))
              .padding(horizontal = 10.dp, vertical = 6.dp),
          ) {
            Text(
              text = if (hasIntelUpgrade) {
                "⚡ 検疫DB一致 [$knownThreatFamily]: 『遮断(BLOCK)』または『検疫』で即対処可能！"
              } else {
                "📌 既知の検疫型 [$knownThreatFamily]: 過去に検疫したマルウェアと同型です"
              },
              style = MaterialTheme.typography.labelMedium,
              color = Color.White,
            )
          }
        }
        if (showExternalBadge) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(6.dp))
              .background(Color(0xFF7A2E14))
              .padding(horizontal = 10.dp, vertical = 5.dp),
          ) {
            Text(
              text = if (isJa) "⚠ [社外アドレス強調] 社外ドメインを含む通信です" else "⚠ [EXTERNAL DOMAIN DETECTED]",
              style = MaterialTheme.typography.labelSmall,
              color = Color.White,
            )
          }
        }
        if (showScannerLv1Badge) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(6.dp))
              .background(Color(0xFF6B4812))
              .padding(horizontal = 10.dp, vertical = 5.dp),
          ) {
            Text(
              text = "🔍 [自動検知Lv1] 送信元認証またはヘッダに不審な不一致を検出",
              style = MaterialTheme.typography.labelSmall,
              color = Color.White,
            )
          }
        }
        if (showScannerLv2Badge) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(6.dp))
              .background(Color(0xFF6E1818))
              .padding(horizontal = 10.dp, vertical = 5.dp),
          ) {
            Text(
              text = "🚨 [自動検知Lv2] 危険ペイロード・不正通信または脅威シグネチャを検出！",
              style = MaterialTheme.typography.labelSmall,
              color = Color.White,
            )
          }
        }
      }
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Image(
        bitmap = avatarBmp,
        contentDescription = "Sender Avatar",
        filterQuality = FilterQuality.None,
        modifier = Modifier
          .size(56.dp)
          .clip(RoundedCornerShape(8.dp))
          .border(1.5.dp, style.inkDark, RoundedCornerShape(8.dp)),
      )

      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(3.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "${packet.from.displayName} (${packet.from.department})",
            style = MaterialTheme.typography.titleMedium,
            color = style.inkDark,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "#${packet.id}",
            style = MaterialTheme.typography.labelSmall,
            color = style.inkMuted,
            softWrap = false,
            maxLines = 1,
          )
        }
        Text(
          text = if (isInspectionMode) {
            "【照合チェック中】怪しい項目と規則・ツールをタップして照合"
          } else {
            "項目をタップすると『照合チェック』を開始できます"
          },
          style = MaterialTheme.typography.labelSmall,
          color = style.inkHighlight,
        )
      }
    }

    InspectableChip(
      target = InspectableTarget(
        targetId = "from.email",
        field = PacketField.FROM,
        label = "差出人アドレス",
        value = packet.from.email,
      ),
      isSelected = "from.email" in selectedTargetIds,
      text = "差出人: ${packet.from.email}",
      highlightAlert = showExternalBadge && isExternalSender,
      onSelect = onSelectTarget,
      modifier = Modifier.fillMaxWidth(),
    )

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      InspectableChip(
        target = InspectableTarget(
          targetId = "from.auth",
          field = PacketField.FROM,
          label = "認証方式",
          value = packet.from.authMethod,
        ),
        isSelected = "from.auth" in selectedTargetIds,
        text = "認証: ${packet.from.authMethod}",
        highlightAlert = showScannerLv1Badge,
        onSelect = onSelectTarget,
        modifier = Modifier.weight(1f),
      )
      InspectableChip(
        target = InspectableTarget(
          targetId = "timestamp",
          field = PacketField.TIMESTAMP,
          label = "送信日時",
          value = packet.timestamp,
        ),
        isSelected = "timestamp" in selectedTargetIds,
        text = "日時: ${packet.timestamp}",
        onSelect = onSelectTarget,
        modifier = Modifier.weight(1f),
      )
    }

    val extTag = if (packet.to.isExternal) "[社外] " else "[社内] "
    InspectableChip(
      target = InspectableTarget(
        targetId = "to.email",
        field = PacketField.TO,
        label = "宛先",
        value = "$extTag${packet.to.email}",
      ),
      isSelected = "to.email" in selectedTargetIds,
      text = "宛先: $extTag${packet.to.displayName} <${packet.to.email}>",
      highlightAlert = showExternalBadge && packet.to.isExternal,
      onSelect = onSelectTarget,
      modifier = Modifier.fillMaxWidth(),
    )

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

    val isBodySelected = "body" in selectedTargetIds
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .heightIn(min = 68.dp)
        .clip(RoundedCornerShape(8.dp))
        .background(if (isBodySelected) Color(0xFFFFE5B8) else style.paperMid)
        .border(
          width = if (isBodySelected) 2.dp else 1.dp,
          color = if (isBodySelected) style.inkHighlight else style.paperShadow,
          shape = RoundedCornerShape(8.dp),
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
        .padding(12.dp),
    ) {
      Text(
        text = packet.body,
        style = MaterialTheme.typography.bodyLarge,
        color = style.inkDark,
      )
    }

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
        text = "📎 添付: ${att.fileName}  |  申告 ${att.declaredSizeKb}KB  |  簡易検査: ${att.quickScanStatus}",
        highlightAlert = att.quickScanStatus != "CLEAN" || showScannerLv2Badge,
        onSelect = onSelectTarget,
        modifier = Modifier.fillMaxWidth(),
      )
    }

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
        text = "🔐 証明書: ${cert.issuer}  |  有効期限 ${cert.validUntil}  |  ${cert.tokenType}",
        onSelect = onSelectTarget,
        modifier = Modifier.fillMaxWidth(),
      )
    }

    val bribe = packet.bribeOffer
    if (bribe != null) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(8.dp))
          .background(PacketsPalette.BribeNoteBg)
          .border(1.5.dp, PacketsPalette.InkHighlight, RoundedCornerShape(8.dp))
          .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
      ) {
        Text(
          text = "💴 ${bribe.offerTitle} [特別報酬 +¥%,d]".format(bribe.bonusAmount),
          style = MaterialTheme.typography.labelMedium,
          color = PacketsPalette.InkHighlight,
        )
        Text(
          text = bribe.senderNote,
          style = MaterialTheme.typography.bodyMedium,
          color = PacketsPalette.InkDark,
        )
      }
    }

    if (packet.isAuditAvoidanceEvent && packet.auditAvoidanceInstruction != null) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(8.dp))
          .background(Color(0xFF1F2D3A))
          .border(1.5.dp, PacketsPalette.QuarantineLight, RoundedCornerShape(8.dp))
          .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
      ) {
        Text(
          text = "🕶 監査回避ネットワーク (AAN) 暗号指令",
          style = MaterialTheme.typography.labelMedium,
          color = PacketsPalette.QuarantineLight,
        )
        Text(
          text = packet.auditAvoidanceInstruction,
          style = MaterialTheme.typography.bodyMedium,
          color = Color.White,
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
  val style = LocalAppStyle.current
  val bg = when {
    isSelected -> Color(0xFFFFDF9E)
    highlightAlert -> Color(0xFFFAD8D0)
    else -> style.paperMid
  }
  val borderColor = when {
    isSelected -> style.inkHighlight
    highlightAlert -> PacketsPalette.BlockMid
    else -> style.paperShadow
  }
  Box(
    modifier = modifier
      .heightIn(min = 44.dp)
      .clip(RoundedCornerShape(8.dp))
      .background(bg)
      .border(if (isSelected) 2.dp else 1.dp, borderColor, RoundedCornerShape(8.dp))
      .clickable { onSelect(target) }
      .padding(horizontal = 10.dp, vertical = 8.dp),
    contentAlignment = Alignment.CenterStart,
  ) {
    Text(
      text = text,
      style = MaterialTheme.typography.bodyMedium,
      color = style.inkDark,
      fontWeight = if (isSelected || highlightAlert) FontWeight.Bold else FontWeight.Normal,
    )
  }
}

@Composable
private fun ToolResultSlip(
  packet: Packet,
  toolType: ToolType,
  isSelected: Boolean,
  isJa: Boolean,
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
      .fillMaxWidth(0.96f)
      .clip(RoundedCornerShape(10.dp))
      .background(if (isSelected) Color(0xFFFFE5B4) else PacketsPalette.SlipPaper)
      .border(
        width = 2.dp,
        color = if (isSelected || flagged) PacketsPalette.InkHighlight else PacketsPalette.InkDark,
        shape = RoundedCornerShape(10.dp),
      )
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
      .padding(12.dp),
    verticalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = if (isJa) {
          "🧾 [解析スリップ] ${toolType.displayName} (タップで照合)"
        } else {
          "🧾 [TOOL SLIP] ${toolType.displayName} (Tap to inspect)"
        },
        style = MaterialTheme.typography.labelMedium,
        color = if (flagged) PacketsPalette.InkHighlight else PacketsPalette.InkDark,
        modifier = Modifier.weight(1f),
      )
      Spacer(modifier = Modifier.width(6.dp))
      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(6.dp))
          .background(PacketsPalette.InkDark)
          .clickable(onClick = onClose)
          .heightIn(min = 34.dp)
          .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = if (isJa) "閉じる ✕" else "CLOSE ✕",
          style = MaterialTheme.typography.labelSmall,
          color = PacketsPalette.PaperBright,
        )
      }
    }
    Text(
      text = resultText,
      style = MaterialTheme.typography.bodyLarge,
      color = PacketsPalette.InkDark,
    )
  }
}

@Composable
private fun PolicyBookOverlay(
  rules: List<PolicyRule>,
  selectedTargetIds: Set<String>,
  isJa: Boolean,
  onSelectRule: (InspectableTarget) -> Unit,
  onClose: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier
      .clip(RoundedCornerShape(12.dp))
      .background(PacketsPalette.BookCover)
      .padding(12.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = if (isJa) "📘 保安規則ブック (タップで照合選択)" else "📘 SECURITY POLICY BOOK",
        style = MaterialTheme.typography.titleMedium,
        color = PacketsPalette.PaperBright,
        modifier = Modifier.weight(1f),
      )
      Spacer(modifier = Modifier.width(8.dp))
      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(8.dp))
          .background(PacketsPalette.BlockMid)
          .clickable(onClick = onClose)
          .heightIn(min = 42.dp)
          .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = if (isJa) "閉じる ✕" else "CLOSE ✕",
          style = MaterialTheme.typography.labelMedium,
          color = Color.White,
        )
      }
    }

    Column(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(8.dp))
        .background(Color(0xFF1A2736))
        .padding(8.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Text(
        text = if (isJa) "【判定スタンプの使い分けガイド】" else "[VERDICT GUIDE]",
        style = MaterialTheme.typography.labelMedium,
        color = PacketsPalette.QuarantineLight,
      )
      Text(
        text = "🟢 許可(PASS): 規則違反のない正常な業務メールを通過させる",
        style = MaterialTheme.typography.labelSmall,
        color = PacketsPalette.PassLight,
      )
      Text(
        text = "🔴 遮断(BLOCK): 未承認ドメイン・なりすまし・期限切れ証明書等の『規則違反』を拒否",
        style = MaterialTheme.typography.labelSmall,
        color = PacketsPalette.BlockLight,
      )
      Text(
        text = "🟡 検疫(QUARANTINE): ウイルス添付・容量偽装・C2ボット通信等の『攻撃』を隔離 (+¥600＆脅威DB登録)",
        style = MaterialTheme.typography.labelSmall,
        color = PacketsPalette.QuarantineLight,
      )
    }

    Column(
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth()
        .verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      rules.forEach { rule ->
        val targetId = "rule.${rule.id}"
        val selected = targetId in selectedTargetIds
        val isQuarantineRule = rule.id == "R3" || rule.id == "R5" || rule.description.contains("QUARANTINE")
        val badgeText = if (isQuarantineRule && rule.id == "R3") {
          "攻撃・偽装 → 検疫 (QUARANTINE)"
        } else if (rule.id == "R5") {
          "違反 → 遮断 / C2攻撃 → 検疫"
        } else {
          "規則違反 → 遮断 (BLOCK)"
        }
        val badgeBg = if (isQuarantineRule) PacketsPalette.QuarantineDark else PacketsPalette.BlockDark

        Column(
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 68.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) Color(0xFFFFDF9E) else PacketsPalette.BookPaper)
            .border(
              width = if (selected) 2.5.dp else 1.dp,
              color = if (selected) PacketsPalette.InkHighlight else PacketsPalette.PaperShadow,
              shape = RoundedCornerShape(10.dp),
            )
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
            .padding(12.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = rule.title,
              style = MaterialTheme.typography.titleMedium,
              color = PacketsPalette.InkDark,
              modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(badgeBg)
                .padding(horizontal = 8.dp, vertical = 3.dp),
            ) {
              Text(
                text = badgeText,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
              )
            }
          }
          Text(
            text = rule.description,
            style = MaterialTheme.typography.bodyMedium,
            color = PacketsPalette.InkDark,
          )
        }
      }
    }
  }
}

@Composable
private fun ToolPaletteStrip(
  inspectedTools: Set<ToolType>,
  onSelectTool: (ToolType) -> Unit,
  onClose: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val style = LocalAppStyle.current
  Column(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .background(Color(0xF5151E18))
      .border(1.5.dp, style.crtPhosphorMid, RoundedCornerShape(12.dp))
      .padding(10.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = "🛠 解析ツール選択 (タップで出力スリップ表示)",
        style = MaterialTheme.typography.labelMedium,
        color = style.crtPhosphorBright,
      )
      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(6.dp))
          .background(style.crtGridDark)
          .clickable(onClick = onClose)
          .padding(horizontal = 10.dp, vertical = 4.dp),
      ) {
        Text("✕", style = MaterialTheme.typography.labelMedium, color = style.crtPhosphorBright)
      }
    }
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      ToolType.entries.forEach { tool ->
        val used = tool in inspectedTools
        Box(
          modifier = Modifier
            .weight(1f)
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (used) style.crtGridDark else Color(0xFF233329))
            .border(1.dp, style.crtPhosphorMid, RoundedCornerShape(8.dp))
            .clickable { onSelectTool(tool) }
            .padding(horizontal = 6.dp, vertical = 8.dp),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = tool.shortLabel,
            style = MaterialTheme.typography.labelMedium,
            color = style.crtPhosphorBright,
            textAlign = TextAlign.Center,
          )
        }
      }
    }
  }
}

@Composable
private fun PermanentActionTray(
  isPolicyBookOpen: Boolean,
  isToolTrayOpen: Boolean,
  isInspectionMode: Boolean,
  isInterPacketInterval: Boolean,
  quarantineRecommended: Boolean,
  isJa: Boolean,
  onTogglePolicyBook: () -> Unit,
  onToggleToolTray: () -> Unit,
  onToggleInspectionMode: () -> Unit,
  onApplyVerdict: (Verdict) -> Unit,
) {
  Column(
    modifier = Modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      UtilityTrayButton(
        label = if (isJa) "📘 規則ブック" else "📘 RULES",
        active = isPolicyBookOpen,
        onClick = onTogglePolicyBook,
        modifier = Modifier.weight(1f),
      )
      UtilityTrayButton(
        label = if (isJa) "🛠 ツール" else "🛠 TOOLS",
        active = isToolTrayOpen,
        onClick = onToggleToolTray,
        modifier = Modifier.weight(1f),
      )
      UtilityTrayButton(
        label = if (isJa) {
          if (isInspectionMode) "🔍 照合中[ON]" else "🔍 照合チェック"
        } else {
          if (isInspectionMode) "🔍 INSPECT[ON]" else "🔍 INSPECT"
        },
        active = isInspectionMode,
        onClick = onToggleInspectionMode,
        modifier = Modifier.weight(1.1f),
      )
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      StampActionButton(
        title = "PASS",
        subtitle = if (isJa) "許可 (正常)" else "ALLOW",
        bgColor = PacketsPalette.PassDark,
        borderColor = PacketsPalette.PassLight,
        enabled = !isInterPacketInterval,
        onClick = { onApplyVerdict(Verdict.PASS) },
        modifier = Modifier.weight(1f),
      )
      StampActionButton(
        title = "BLOCK",
        subtitle = if (isJa) {
          if (quarantineRecommended) "遮断 (DB推奨!)" else "遮断 (違反)"
        } else {
          "REJECT"
        },
        bgColor = PacketsPalette.BlockDark,
        borderColor = if (quarantineRecommended) Color.White else PacketsPalette.BlockLight,
        enabled = !isInterPacketInterval,
        onClick = { onApplyVerdict(Verdict.BLOCK) },
        modifier = Modifier.weight(1f),
      )
      StampActionButton(
        title = "QUARANTINE",
        subtitle = if (isJa) "検疫 (攻撃+¥600)" else "ISOLATE +¥600",
        bgColor = PacketsPalette.QuarantineDark,
        borderColor = PacketsPalette.QuarantineLight,
        enabled = !isInterPacketInterval,
        onClick = { onApplyVerdict(Verdict.QUARANTINE) },
        modifier = Modifier.weight(1.15f),
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
  val style = LocalAppStyle.current
  Box(
    modifier = modifier
      .height(48.dp)
      .clip(RoundedCornerShape(10.dp))
      .background(if (active) style.crtGridDark else style.deskLight)
      .border(
        width = if (active) 2.dp else 1.dp,
        color = if (active) style.crtPhosphorBright else style.paperShadow.copy(alpha = 0.6f),
        shape = RoundedCornerShape(10.dp),
      )
      .clickable(onClick = onClick)
      .padding(horizontal = 6.dp),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelMedium,
      color = if (active) style.crtPhosphorBright else style.paperBright,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

@Composable
private fun StampActionButton(
  title: String,
  subtitle: String,
  bgColor: Color,
  borderColor: Color,
  enabled: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Box(
    modifier = modifier
      .height(64.dp)
      .clip(RoundedCornerShape(12.dp))
      .background(if (enabled) bgColor else bgColor.copy(alpha = 0.4f))
      .border(
        width = 2.dp,
        color = if (enabled) borderColor else borderColor.copy(alpha = 0.35f),
        shape = RoundedCornerShape(12.dp),
      )
      .clickable(enabled = enabled, onClick = onClick)
      .padding(horizontal = 6.dp, vertical = 6.dp),
    contentAlignment = Alignment.Center,
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = if (enabled) borderColor else borderColor.copy(alpha = 0.5f),
        fontWeight = FontWeight.Bold,
        maxLines = 1,
      )
      Text(
        text = subtitle,
        style = MaterialTheme.typography.labelSmall,
        color = if (enabled) Color.White else Color.White.copy(alpha = 0.5f),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}
