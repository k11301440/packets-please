package com.tommy.packetsplease.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.tommy.packetsplease.domain.AlertTimingMode
import com.tommy.packetsplease.domain.GameMode
import com.tommy.packetsplease.domain.ReferenceResolution
import com.tommy.packetsplease.domain.Verdict
import com.tommy.packetsplease.ui.components.InspectionDeskArea
import com.tommy.packetsplease.ui.components.SurveillanceMonitor
import com.tommy.packetsplease.ui.pixel.PixelArtAssets
import com.tommy.packetsplease.ui.theme.PacketsPalette

@Composable
fun PacketsPleaseApp(viewModel: GameViewModel) {
  val state by viewModel.uiState.collectAsState()

  BoxWithConstraints(
    modifier = Modifier
      .fillMaxSize()
      .background(PacketsPalette.CrtBlack)
      .windowInsetsPadding(WindowInsets.safeDrawing),
  ) {
    val screenPxW = constraints.maxWidth
    val screenPxH = constraints.maxHeight

    when (state.phase) {
      GamePhase.TITLE -> TitleScreen(
        hasSavedGame = state.hasSavedGame,
        savedDay = state.savedDayPreview,
        onNewCampaign = { viewModel.startNewGame(GameMode.CAMPAIGN) },
        onContinue = { viewModel.continueSavedGame() },
        onStartEndless = { viewModel.startNewGame(GameMode.ENDLESS) },
        onOpenM1 = { viewModel.toggleM1DebugDialog() },
      )

      GamePhase.MORNING_BRIEFING -> MorningBriefingScreen(
        state = state,
        onBeginShift = { viewModel.startWorkShift() },
        onBackToTitle = { viewModel.returnToTitle() },
      )

      GamePhase.WORK_SHIFT -> {
        val compactMonitor = state.isPolicyBookOpen || state.isInspectionMode || state.activeToolSlip != null
        val monitorFraction by animateFloatAsState(
          targetValue = if (compactMonitor) 0.25f else 0.33f,
          animationSpec = tween(durationMillis = 150),
          label = "monitorRatio",
        )
        Column(modifier = Modifier.fillMaxSize()) {
          SurveillanceMonitor(
            day = state.currentDay,
            remainingSeconds = state.remainingSeconds,
            budget = state.budget,
            dailyWarnings = state.dailyWarnings,
            freeWarnings = state.tuningConfig.freeWarningsPerDay,
            processedCount = state.dailyProcessed,
            totalQueueCount = state.packetQueue.size,
            flowAnim = state.packetFlowAnim,
            activeAlerts = state.activeOsAlerts,
            onDismissAlert = { viewModel.dismissOsAlert(it) },
            onOpenM1Menu = { viewModel.toggleM1DebugDialog() },
            modifier = Modifier.weight(monitorFraction),
          )
          InspectionDeskArea(
            state = state,
            onSelectInspectTarget = { viewModel.selectInspectTarget(it) },
            onClearInspection = { viewModel.clearInspectionSelection() },
            onToggleInspectionMode = { viewModel.toggleInspectionMode() },
            onTogglePolicyBook = { viewModel.togglePolicyBook() },
            onToggleToolTray = { viewModel.toggleToolTray() },
            onOpenTool = { viewModel.openTool(it) },
            onCloseToolSlip = { viewModel.closeToolSlip() },
            onApplyVerdict = { viewModel.applyVerdict(it) },
            modifier = Modifier.weight(1f - monitorFraction),
          )
        }
      }

      GamePhase.EVENING_LEDGER -> EveningLedgerScreen(
        state = state,
        onNextDay = { viewModel.proceedToNextDay() },
        onManualSave = { viewModel.manualSaveCurrentProgress() },
        onReturnTitle = { viewModel.returnToTitle() },
      )

      GamePhase.ENDING -> EndingScreen(
        state = state,
        onReturnTitle = { viewModel.returnToTitle() },
      )
    }

    if (state.showM1DebugDialog) {
      M1DebugPanelOverlay(
        state = state,
        screenPxW = screenPxW,
        screenPxH = screenPxH,
        onClose = { viewModel.toggleM1DebugDialog() },
        onUpdateTuning = { res, sec, timing ->
          viewModel.updateTuningConfig(res, sec, timing)
        },
        onToggleMute = { viewModel.toggleMute() },
        onJumpToDay = { viewModel.jumpToDay(it) },
        onFinishShiftNow = { viewModel.finishShiftNowForDebug() },
      )
    }
  }
}

@Composable
private fun TitleScreen(
  hasSavedGame: Boolean,
  savedDay: Int,
  onNewCampaign: () -> Unit,
  onContinue: () -> Unit,
  onStartEndless: () -> Unit,
  onOpenM1: () -> Unit,
) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(PacketsPalette.CrtBlack)
      .border(2.dp, PacketsPalette.CrtGridDark)
      .padding(20.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.SpaceBetween,
  ) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Spacer(modifier = Modifier.height(20.dp))
      Text(
        text = "OLEASE CORP. // FIREWALL AI UNIT",
        style = MaterialTheme.typography.labelSmall,
        color = PacketsPalette.CrtPhosphorMid,
      )
      Spacer(modifier = Modifier.height(14.dp))
      Canvas(modifier = Modifier.size(80.dp, 96.dp)) {
        val bmp = PixelArtAssets.firewallGateSprite
        val scale = (minOf(size.width / bmp.width, size.height / bmp.height)).toInt().coerceAtLeast(2)
        val drawW = bmp.width * scale
        val drawH = bmp.height * scale
        drawImage(
          image = bmp,
          dstOffset = IntOffset(((size.width - drawW) / 2f).toInt(), ((size.height - drawH) / 2f).toInt()),
          dstSize = IntSize(drawW, drawH),
          filterQuality = FilterQuality.None,
        )
      }
      Spacer(modifier = Modifier.height(12.dp))
      Text(
        text = "パケッツ,プリーズ",
        style = MaterialTheme.typography.displaySmall,
        color = PacketsPalette.CrtPhosphorBright,
      )
      Text(
        text = "Packets, Please  [v0.3 Prototype]",
        style = MaterialTheme.typography.labelMedium,
        color = PacketsPalette.CrtPhosphorMid,
      )
      Spacer(modifier = Modifier.height(14.dp))
      Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        listOf(Verdict.PASS, Verdict.BLOCK).forEach { v ->
          val bmp = PixelArtAssets.getStampBitmap(v)
          Canvas(modifier = Modifier.size(96.dp, 28.dp)) {
            val sc = (minOf(size.width / bmp.width, size.height / bmp.height)).toInt().coerceAtLeast(1)
            val dw = bmp.width * sc
            val dh = bmp.height * sc
            drawImage(
              image = bmp,
              dstOffset = IntOffset(((size.width - dw) / 2f).toInt(), ((size.height - dh) / 2f).toInt()),
              dstSize = IntSize(dw, dh),
              filterQuality = FilterQuality.None,
            )
          }
        }
      }
    }

    Column(
      modifier = Modifier.fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      if (hasSavedGame) {
        RetroButton(
          label = "続きから再開 (DAY %02d)".format(savedDay),
          border = PacketsPalette.QuarantineLight,
          textColor = PacketsPalette.QuarantineLight,
          onClick = onContinue,
        )
      }
      RetroButton(
        label = "新規業務開始 (14日キャンペーン)",
        border = PacketsPalette.CrtPhosphorBright,
        textColor = PacketsPalette.CrtPhosphorBright,
        onClick = onNewCampaign,
      )
      RetroButton(
        label = "エンドレス監査モード",
        border = PacketsPalette.CrtPhosphorMid,
        textColor = PacketsPalette.CrtPhosphorMid,
        onClick = onStartEndless,
      )
      RetroButton(
        label = "M1プロトタイプ調整 / デバッグ",
        border = PacketsPalette.PaperShadow,
        textColor = PacketsPalette.PaperBright,
        onClick = onOpenM1,
      )
    }

    Text(
      text = "完全オフライン動作 / Font: DotGothic16 (OFL)",
      style = MaterialTheme.typography.labelSmall,
      color = PacketsPalette.CrtGridDark,
    )
  }
}

@Composable
private fun MorningBriefingScreen(
  state: GameUiState,
  onBeginShift: () -> Unit,
  onBackToTitle: () -> Unit,
) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(PacketsPalette.CrtBlack)
      .border(2.dp, PacketsPalette.CrtPhosphorMid)
      .padding(16.dp),
    verticalArrangement = Arrangement.SpaceBetween,
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(
          text = "MORNING POLICY DISPATCH",
          style = MaterialTheme.typography.labelSmall,
          color = PacketsPalette.CrtPhosphorMid,
        )
        Text(
          text = "予算 ¥%,d".format(state.budget),
          style = MaterialTheme.typography.labelMedium,
          color = PacketsPalette.CrtPhosphorBright,
        )
      }
      Spacer(modifier = Modifier.height(10.dp))
      Text(
        text = state.morningHeadline,
        style = MaterialTheme.typography.titleLarge,
        color = PacketsPalette.CrtPhosphorBright,
      )
      Spacer(modifier = Modifier.height(10.dp))
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .background(PacketsPalette.CrtGridDark.copy(alpha = 0.45f))
          .border(1.dp, PacketsPalette.CrtPhosphorMid)
          .padding(10.dp),
      ) {
        Text(
          text = state.morningMemo,
          style = MaterialTheme.typography.bodyLarge,
          color = PacketsPalette.CrtPhosphorBright,
        )
      }

      Spacer(modifier = Modifier.height(14.dp))
      Text(
        text = "■ 本日の新規ポリシー通達",
        style = MaterialTheme.typography.titleMedium,
        color = PacketsPalette.QuarantineLight,
      )
      Spacer(modifier = Modifier.height(6.dp))

      if (state.newRulesToday.isEmpty()) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, PacketsPalette.CrtGridDark)
            .padding(10.dp),
        ) {
          Text(
            text = "新規追加規則はありません。現在有効な ${state.activeRules.size} 項目の規則を引き続き遵守してください。",
            style = MaterialTheme.typography.bodyMedium,
            color = PacketsPalette.CrtPhosphorMid,
          )
        }
      } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          state.newRulesToday.forEach { rule ->
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .background(PacketsPalette.PaperBright)
                .border(2.dp, PacketsPalette.QuarantineMid)
                .padding(10.dp),
            ) {
              Text(
                text = rule.title,
                style = MaterialTheme.typography.titleMedium,
                color = PacketsPalette.InkDark,
              )
              Spacer(modifier = Modifier.height(3.dp))
              Text(
                text = rule.description,
                style = MaterialTheme.typography.bodyMedium,
                color = PacketsPalette.InkMuted,
              )
            }
          }
        }
      }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      RetroButton(
        label = "検閲ゲートを開く (処理予定: ${state.packetQueue.size}件)",
        border = PacketsPalette.CrtPhosphorBright,
        textColor = PacketsPalette.CrtPhosphorBright,
        onClick = onBeginShift,
      )
      RetroButton(
        label = "タイトルへ戻る",
        border = PacketsPalette.CrtGridDark,
        textColor = PacketsPalette.CrtPhosphorMid,
        onClick = onBackToTitle,
      )
    }
  }
}

@Composable
private fun EveningLedgerScreen(
  state: GameUiState,
  onNextDay: () -> Unit,
  onManualSave: () -> Unit,
  onReturnTitle: () -> Unit,
) {
  val ledger = state.lastLedger
  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(PacketsPalette.DeskDark)
      .padding(16.dp),
    verticalArrangement = Arrangement.SpaceBetween,
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .background(PacketsPalette.PaperBright)
        .border(2.dp, PacketsPalette.InkDark)
        .padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Text(
        text = "DAY %02d 業務終了・部署予算精算".format(ledger?.day ?: state.currentDay),
        style = MaterialTheme.typography.titleLarge,
        color = PacketsPalette.InkDark,
      )
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(2.dp)
          .background(PacketsPalette.InkDark),
      )
      LedgerRow("処理通信数", "${ledger?.processedCount ?: 0} 件 (正審 ${ledger?.correctCount ?: 0} 件)")
      LedgerRow("誤審警告", "${ledger?.warningCount ?: 0} 回")
      LedgerRow("検閲正規報酬", "+¥%,d".format(ledger?.incomeTotal ?: 0), PacketsPalette.PassDark)
      if ((ledger?.bribeTotal ?: 0) > 0) {
        LedgerRow("袖之下収入", "+¥%,d".format(ledger?.bribeTotal ?: 0), PacketsPalette.QuarantineDark)
      }
      LedgerRow("誤審罰金", "-¥%,d".format(ledger?.penaltyTotal ?: 0), PacketsPalette.BlockMid)
      LedgerRow("翌日運用維持費", "-¥%,d".format(ledger?.operatingCost ?: 0), PacketsPalette.BlockMid)
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(2.dp)
          .background(PacketsPalette.InkDark),
      )
      LedgerRow("繰越部署予算", "¥%,d".format(ledger?.endingBudget ?: state.budget), PacketsPalette.InkHighlight)

      if (state.saveStatusToast != null) {
        Text(
          text = "✓ ${state.saveStatusToast}",
          style = MaterialTheme.typography.labelMedium,
          color = PacketsPalette.PassDark,
        )
      }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      RetroButton(
        label = "DAY %02d へ進む".format(state.currentDay + 1),
        border = PacketsPalette.CrtPhosphorBright,
        textColor = PacketsPalette.CrtPhosphorBright,
        onClick = onNextDay,
      )
      RetroButton(
        label = "手動セーブを実行",
        border = PacketsPalette.QuarantineLight,
        textColor = PacketsPalette.QuarantineLight,
        onClick = onManualSave,
      )
      RetroButton(
        label = "セーブしてタイトルへ戻る",
        border = PacketsPalette.PaperShadow,
        textColor = PacketsPalette.PaperBright,
        onClick = onReturnTitle,
      )
    }
  }
}

@Composable
private fun LedgerRow(
  label: String,
  value: String,
  valueColor: Color = PacketsPalette.InkDark,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Text(label, style = MaterialTheme.typography.bodyLarge, color = PacketsPalette.InkMuted)
    Text(value, style = MaterialTheme.typography.titleMedium, color = valueColor)
  }
}

@Composable
private fun EndingScreen(
  state: GameUiState,
  onReturnTitle: () -> Unit,
) {
  val ending = state.endingType
  val accent = if (ending?.isGameOver == true) PacketsPalette.CrtAlertRed else PacketsPalette.CrtPhosphorBright
  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(PacketsPalette.CrtBlack)
      .border(2.dp, accent)
      .padding(20.dp),
    verticalArrangement = Arrangement.SpaceBetween,
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
      Text(
        text = ending?.subtitle ?: "EVALUATION COMPLETE",
        style = MaterialTheme.typography.labelMedium,
        color = accent,
      )
      Text(
        text = ending?.title ?: "運用終了",
        style = MaterialTheme.typography.displaySmall,
        color = accent,
      )
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .background(PacketsPalette.CrtGridDark.copy(alpha = 0.4f))
          .border(1.dp, accent)
          .padding(12.dp),
      ) {
        Text(
          text = ending?.epilogue ?: "",
          style = MaterialTheme.typography.bodyLarge,
          color = PacketsPalette.PaperBright,
        )
      }
      Spacer(modifier = Modifier.height(6.dp))
      Text("【運用記録】", style = MaterialTheme.typography.titleMedium, color = PacketsPalette.CrtPhosphorMid)
      Text("到達日: DAY ${state.currentDay}", style = MaterialTheme.typography.bodyMedium, color = PacketsPalette.PaperBright)
      Text("総処理数: ${state.totalProcessed}件 (正審 ${state.totalCorrect}件)", style = MaterialTheme.typography.bodyMedium, color = PacketsPalette.PaperBright)
      Text("最終予算: ¥%,d".format(state.budget), style = MaterialTheme.typography.bodyMedium, color = PacketsPalette.PaperBright)
      Text("AAN協力回数: ${state.aanCooperationCount}回 / 袖之下回数: ${state.totalBribesAccepted}回", style = MaterialTheme.typography.bodyMedium, color = PacketsPalette.PaperBright)
    }

    RetroButton(
      label = "タイトル画面に戻る",
      border = accent,
      textColor = accent,
      onClick = onReturnTitle,
    )
  }
}

@Composable
private fun M1DebugPanelOverlay(
  state: GameUiState,
  screenPxW: Int,
  screenPxH: Int,
  onClose: () -> Unit,
  onUpdateTuning: (ReferenceResolution, Int, AlertTimingMode) -> Unit,
  onToggleMute: () -> Unit,
  onJumpToDay: (Int) -> Unit,
  onFinishShiftNow: () -> Unit,
) {
  val cfg = state.tuningConfig
  val scaleX = screenPxW / cfg.referenceResolution.widthPx
  val scaleY = screenPxH / cfg.referenceResolution.heightPx
  val intScale = minOf(scaleX, scaleY).coerceAtLeast(1)

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color.Black.copy(alpha = 0.85f))
      .padding(16.dp),
    contentAlignment = Alignment.Center,
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .background(PacketsPalette.CrtBlack)
        .border(2.dp, PacketsPalette.CrtPhosphorBright)
        .verticalScroll(rememberScrollState())
        .padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text("M1 プロトタイプ調整 & デバッグ", style = MaterialTheme.typography.titleMedium, color = PacketsPalette.CrtPhosphorBright)
        Text("[閉じる]", style = MaterialTheme.typography.labelMedium, color = PacketsPalette.CrtAlertAmber, modifier = Modifier.clickable { onClose() })
      }

      Text(
        text = "実機描画域: ${screenPxW}x${screenPxH}px -> 基準 ${cfg.referenceResolution.widthPx}x${cfg.referenceResolution.heightPx} (${intScale}x整数倍)",
        style = MaterialTheme.typography.labelSmall,
        color = PacketsPalette.CrtPhosphorMid,
      )

      Text("基準解像度 (要6.1実機検証):", style = MaterialTheme.typography.labelMedium, color = PacketsPalette.PaperBright)
      Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        ReferenceResolution.entries.forEach { res ->
          OptionChip(
            text = res.label,
            selected = cfg.referenceResolution == res,
            onClick = { onUpdateTuning(res, cfg.dayDurationSeconds, cfg.alertTimingMode) },
            modifier = Modifier.weight(1f),
          )
        }
      }

      Text("1日の実時間 (要4.6/10調整):", style = MaterialTheme.typography.labelMedium, color = PacketsPalette.PaperBright)
      Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(60, 180, 300).forEach { sec ->
          OptionChip(
            text = "${sec}秒",
            selected = cfg.dayDurationSeconds == sec,
            onClick = { onUpdateTuning(cfg.referenceResolution, sec, cfg.alertTimingMode) },
            modifier = Modifier.weight(1f),
          )
        }
      }

      Text("不正検知通知タイミング (要4.5調整):", style = MaterialTheme.typography.labelMedium, color = PacketsPalette.PaperBright)
      Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        AlertTimingMode.entries.forEach { mode ->
          OptionChip(
            text = mode.label,
            selected = cfg.alertTimingMode == mode,
            onClick = { onUpdateTuning(cfg.referenceResolution, cfg.dayDurationSeconds, mode) },
            modifier = Modifier.weight(1f),
          )
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text("効果音ミュート:", style = MaterialTheme.typography.labelMedium, color = PacketsPalette.PaperBright)
        OptionChip(
          text = if (state.isMuted) "ミュート中" else "効果音ON",
          selected = !state.isMuted,
          onClick = onToggleMute,
        )
      }

      Text("シナリオ検証用 Dayジャンプ:", style = MaterialTheme.typography.labelMedium, color = PacketsPalette.QuarantineLight)
      Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(1, 2, 3, 4, 7, 14, 15).forEach { d ->
          Box(
            modifier = Modifier
              .weight(1f)
              .border(1.dp, PacketsPalette.CrtPhosphorMid)
              .clickable { onJumpToDay(d) }
              .padding(vertical = 5.dp),
            contentAlignment = Alignment.Center,
          ) {
            Text(if (d == 15) "∞" else "D$d", style = MaterialTheme.typography.labelSmall, color = PacketsPalette.CrtPhosphorBright)
          }
        }
      }

      if (state.phase == GamePhase.WORK_SHIFT) {
        RetroButton(
          label = "現在のシフトを即時終了して決算へ",
          border = PacketsPalette.CrtAlertAmber,
          textColor = PacketsPalette.CrtAlertAmber,
          onClick = onFinishShiftNow,
        )
      }
    }
  }
}

@Composable
private fun OptionChip(
  text: String,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Box(
    modifier = modifier
      .background(if (selected) PacketsPalette.CrtGridDark else PacketsPalette.CrtBlack)
      .border(1.dp, if (selected) PacketsPalette.CrtPhosphorBright else PacketsPalette.PaperShadow)
      .clickable { onClick() }
      .padding(horizontal = 8.dp, vertical = 6.dp),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = text,
      style = MaterialTheme.typography.labelSmall,
      color = if (selected) PacketsPalette.CrtPhosphorBright else PacketsPalette.PaperBright,
      maxLines = 1,
    )
  }
}

@Composable
private fun RetroButton(
  label: String,
  border: Color,
  textColor: Color,
  onClick: () -> Unit,
) {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .background(PacketsPalette.CrtBlack)
      .border(2.dp, border)
      .clickable { onClick() }
      .padding(vertical = 12.dp, horizontal = 10.dp),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.titleMedium,
      color = textColor,
      textAlign = TextAlign.Center,
    )
  }
}
