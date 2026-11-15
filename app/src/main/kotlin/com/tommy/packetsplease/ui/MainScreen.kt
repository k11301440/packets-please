package com.tommy.packetsplease.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tommy.packetsplease.domain.AchievementId
import com.tommy.packetsplease.domain.AlertTimingMode
import com.tommy.packetsplease.domain.AppLanguage
import com.tommy.packetsplease.domain.AppSettings
import com.tommy.packetsplease.domain.BaseAccentColor
import com.tommy.packetsplease.domain.CollectionProgress
import com.tommy.packetsplease.domain.FontScaleOption
import com.tommy.packetsplease.domain.GameMode
import com.tommy.packetsplease.domain.M1TuningConfig
import com.tommy.packetsplease.domain.ReferenceResolution
import com.tommy.packetsplease.domain.ThemeMode
import com.tommy.packetsplease.domain.UpgradeType
import com.tommy.packetsplease.ui.components.InspectionDeskArea
import com.tommy.packetsplease.ui.components.SurveillanceMonitor
import com.tommy.packetsplease.ui.pixel.PixelArtAssets
import com.tommy.packetsplease.ui.theme.LocalAppStyle
import com.tommy.packetsplease.ui.theme.PacketsPalette
import com.tommy.packetsplease.ui.theme.PacketsPleaseTheme
import kotlinx.coroutines.delay

private const val GITHUB_REPO_URL = "https://github.com/k11301440/packets-please"

@Composable
fun PacketsPleaseApp(viewModel: GameViewModel) {
  val state by viewModel.uiState.collectAsState()
  val uriHandler = LocalUriHandler.current

  PacketsPleaseTheme(settings = state.settings) {
    val style = LocalAppStyle.current
    val isJa = state.settings.language == AppLanguage.JA

    BoxWithConstraints(
      modifier = Modifier
        .fillMaxSize()
        .background(style.crtBlack)
        .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
      val screenPxW = constraints.maxWidth
      val screenPxH = constraints.maxHeight
      val monitorFraction = 0.27f

      when (state.phase) {
        GamePhase.TITLE -> TitleScreen(
          hasSavedGame = state.hasSavedGame,
          savedDayPreview = state.savedDayPreview,
          isJa = isJa,
          onContinue = { viewModel.continueSavedGame() },
          onStartCampaign = { viewModel.startNewGame(GameMode.CAMPAIGN) },
          onStartEndless = { viewModel.startNewGame(GameMode.ENDLESS) },
          onOpenCollection = { viewModel.toggleCollectionDialog(true) },
          onOpenSettings = { viewModel.toggleSettingsDialog(true) },
        )

        GamePhase.MORNING_BRIEFING -> MorningBriefingScreen(
          state = state,
          isJa = isJa,
          onStartShift = { viewModel.startWorkShift() },
          onReturnTitle = { viewModel.returnToTitle() },
        )

        GamePhase.WORK_SHIFT -> Column(modifier = Modifier.fillMaxSize()) {
          val packetSeed = state.currentPacket?.id?.hashCode() ?: state.currentPacketIndex
          SurveillanceMonitor(
            day = state.currentDay,
            remainingSeconds = state.remainingSeconds,
            budget = state.budget,
            dailyWarnings = state.dailyWarnings,
            freeWarnings = state.tuningConfig.freeWarningsPerDay,
            processedCount = state.dailyProcessed,
            packetSeed = packetSeed,
            language = state.settings.language,
            flowAnim = state.packetFlowAnim,
            isVirusExplosionActive = state.isVirusExplosionActive,
            activeAlerts = state.activeOsAlerts,
            onDismissAlert = { viewModel.dismissOsAlert(it) },
            onOpenSettings = { viewModel.toggleSettingsDialog(true) },
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
            onSkipInterval = { viewModel.skipIntervalToNextPacket() },
            modifier = Modifier.weight(1f - monitorFraction),
          )
        }

        GamePhase.EVENING_LEDGER -> EveningLedgerScreen(
          state = state,
          isJa = isJa,
          onBuyUpgrade = { viewModel.buyUpgrade(it) },
          onOpenSponsorAd = { viewModel.openSponsorAdModal() },
          onProceedNextDay = { viewModel.proceedToNextDay() },
          onReturnTitle = { viewModel.returnToTitle() },
          onOpenSettings = { viewModel.toggleSettingsDialog(true) },
        )

        GamePhase.ENDING -> EndingScreen(
          state = state,
          isJa = isJa,
          onOpenCollection = { viewModel.toggleCollectionDialog(true) },
          onReturnTitle = { viewModel.returnToTitle() },
        )
      }

      if (state.isSponsorAdModalOpen) {
        SponsorAdModalOverlay(
          isJa = isJa,
          onClaimReward = { viewModel.claimDailySponsorAdReward() },
          onCancel = { viewModel.closeSponsorAdModal() },
        )
      }

      if (state.isCollectionOpen) {
        CollectionDialogOverlay(
          collection = state.collection,
          isJa = isJa,
          onClose = { viewModel.toggleCollectionDialog(false) },
        )
      }

      if (state.isSettingsOpen) {
        SettingsDialogOverlay(
          settings = state.settings,
          inWorkShift = state.phase == GamePhase.WORK_SHIFT,
          isJa = isJa,
          onUpdateSettings = { viewModel.updateSettings(it) },
          onOpenM1Debug = {
            viewModel.toggleSettingsDialog(false)
            viewModel.toggleM1DebugDialog()
          },
          onOpenPrivacyPolicy = {
            viewModel.toggleSettingsDialog(false)
            viewModel.togglePrivacyPolicyDialog(true)
          },
          onOpenGithub = { uriHandler.openUri(GITHUB_REPO_URL) },
          onReturnToTitle = if (state.phase != GamePhase.TITLE) {
            {
              viewModel.toggleSettingsDialog(false)
              viewModel.returnToTitle()
            }
          } else {
            null
          },
          onClose = { viewModel.toggleSettingsDialog(false) },
        )
      }

      if (state.isPrivacyPolicyOpen) {
        PrivacyPolicyDialogOverlay(
          isJa = isJa,
          onOpenGithub = { uriHandler.openUri(GITHUB_REPO_URL) },
          onClose = { viewModel.togglePrivacyPolicyDialog(false) },
        )
      }

      if (state.showM1DebugDialog) {
        M1DebugPanelOverlay(
          screenPxW = screenPxW,
          screenPxH = screenPxH,
          state = state,
          onUpdateTuning = { res, sec, alert ->
            viewModel.updateTuningConfig(res, sec, alert)
          },
          onJumpToDay = { day -> viewModel.jumpToDay(day) },
          onFinishShiftNow = { viewModel.finishShiftNowForDebug() },
          onManualSave = { viewModel.manualSaveCurrentProgress() },
          onToggleMute = { viewModel.toggleMute() },
          onReturnTitle = { viewModel.returnToTitle() },
          onClose = { viewModel.toggleM1DebugDialog() },
        )
      }
    }
  }
}

@Composable
private fun TitleScreen(
  hasSavedGame: Boolean,
  savedDayPreview: Int,
  isJa: Boolean,
  onContinue: () -> Unit,
  onStartCampaign: () -> Unit,
  onStartEndless: () -> Unit,
  onOpenCollection: () -> Unit,
  onOpenSettings: () -> Unit,
) {
  val style = LocalAppStyle.current
  val infinite = rememberInfiniteTransition(label = "title_ambient")
  val drift by infinite.animateFloat(
    initialValue = 0f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 6500, easing = LinearEasing),
      repeatMode = RepeatMode.Restart,
    ),
    label = "packet_drift",
  )
  val pulse by infinite.animateFloat(
    initialValue = 0.96f,
    targetValue = 1.04f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 1500, easing = LinearEasing),
      repeatMode = RepeatMode.Reverse,
    ),
    label = "crest_pulse",
  )

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(style.crtBlack),
  ) {
    // Subtle retro pixel ambient background (borderless for rounded screens)
    Canvas(modifier = Modifier.fillMaxSize()) {
      val w = size.width
      val h = size.height
      val gridStep = 28.dp.toPx()
      var y = 0f
      while (y < h) {
        drawLine(style.crtGridDark.copy(alpha = 0.25f), Offset(0f, y), Offset(w, y), strokeWidth = 1f)
        y += gridStep
      }

      val lanes = listOf(0.18f, 0.36f, 0.68f, 0.84f)
      lanes.forEachIndexed { idx, yFrac ->
        val phaseShift = (drift + idx * 0.27f) % 1f
        val xPx = -40f + phaseShift * (w + 80f)
        val yPx = h * yFrac
        drawRect(
          color = style.crtPhosphorMid.copy(alpha = 0.20f),
          topLeft = Offset(xPx, yPx),
          size = androidx.compose.ui.geometry.Size(18.dp.toPx(), 12.dp.toPx()),
        )
        drawRect(
          color = style.crtPhosphorBright.copy(alpha = 0.30f),
          topLeft = Offset(xPx + 2.dp.toPx(), yPx + 2.dp.toPx()),
          size = androidx.compose.ui.geometry.Size(14.dp.toPx(), 3.dp.toPx()),
        )
      }
    }

    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 24.dp, vertical = 20.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.SpaceBetween,
    ) {
      Spacer(modifier = Modifier.height(8.dp))

      // Official Pixel Emblem & Clean Title
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Image(
          bitmap = PixelArtAssets.appLogoCrestBitmap,
          contentDescription = "Packets, Please Crest",
          filterQuality = FilterQuality.None,
          modifier = Modifier
            .size(92.dp)
            .scale(pulse),
        )
        Text(
          text = if (isJa) "パケッツ, プリーズ" else "PACKETS, PLEASE",
          style = MaterialTheme.typography.displaySmall,
          color = style.crtPhosphorBright,
          textAlign = TextAlign.Center,
        )
        Text(
          text = "OLE-FW // GATEKEEPER TERMINAL",
          style = MaterialTheme.typography.labelMedium,
          color = style.crtPhosphorMid,
          textAlign = TextAlign.Center,
        )
      }

      // Main Large Action Buttons
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        if (hasSavedGame) {
          RetroMenuButton(
            title = if (isJa) "続きから業務再開 (DAY %02d)".format(savedDayPreview) else "CONTINUE (DAY %02d)".format(savedDayPreview),
            bgColor = PacketsPalette.PassDark,
            borderColor = PacketsPalette.PassLight,
            onClick = onContinue,
          )
        }
        RetroMenuButton(
          title = if (isJa) "キャンペーン開始 (3分シフト)" else "CAMPAIGN (3 MIN SHIFT)",
          bgColor = style.crtGridDark,
          borderColor = style.crtPhosphorBright,
          onClick = onStartCampaign,
        )
        RetroMenuButton(
          title = if (isJa) "エンドレス監視モード" else "ENDLESS OVERWATCH",
          bgColor = style.deskMid,
          borderColor = PacketsPalette.QuarantineLight,
          onClick = onStartEndless,
        )
      }

      // Bottom Collection & Settings Bar
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        UtilityActionCard(
          label = if (isJa) "🏆 図鑑・実績" else "🏆 COLLECTION",
          onClick = onOpenCollection,
          modifier = Modifier.weight(1f),
        )
        UtilityActionCard(
          label = if (isJa) "⚙ 設定・情報" else "⚙ SETTINGS",
          onClick = onOpenSettings,
          modifier = Modifier.weight(1f),
        )
      }
    }
  }
}

@Composable
private fun MorningBriefingScreen(
  state: GameUiState,
  isJa: Boolean,
  onStartShift: () -> Unit,
  onReturnTitle: () -> Unit,
) {
  val style = LocalAppStyle.current
  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(style.crtBlack)
      .padding(horizontal = 18.dp, vertical = 16.dp),
    verticalArrangement = Arrangement.SpaceBetween,
  ) {
    Column(
      modifier = Modifier
        .weight(1f)
        .verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "DAY %02d // BRIEFING".format(state.currentDay),
          style = MaterialTheme.typography.titleLarge,
          color = style.crtPhosphorBright,
        )
        Text(
          text = "¥%,d".format(state.budget),
          style = MaterialTheme.typography.titleMedium,
          color = style.crtPhosphorMid,
        )
      }

      Column(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(12.dp))
          .background(style.paperBright)
          .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Text(
          text = state.morningHeadline,
          style = MaterialTheme.typography.titleMedium,
          color = style.inkHighlight,
        )
        Text(
          text = state.morningMemo,
          style = MaterialTheme.typography.bodyLarge,
          color = style.inkDark,
        )
      }

      Text(
        text = if (state.newRulesToday.isNotEmpty()) {
          if (isJa) "本日の新着セキュリティ通達 (${state.newRulesToday.size}件)" else "NEW DIRECTIVES (${state.newRulesToday.size})"
        } else {
          if (isJa) "継続運用指示 (有効規則 ${state.activeRules.size}項目)" else "ACTIVE DIRECTIVES (${state.activeRules.size})"
        },
        style = MaterialTheme.typography.labelMedium,
        color = PacketsPalette.CrtAlertAmber,
      )

      if (state.newRulesToday.isEmpty()) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(style.paperMid)
            .padding(12.dp),
        ) {
          Text(
            text = if (isJa) {
              "新規通達はありません。制限時間 ${state.totalDaySeconds / 60}分以内に通信を裁き、攻撃系パケットは必ず『検疫』してください。"
            } else {
              "No new rules today. Process traffic within time limit and QUARANTINE active attacks."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = style.inkDark,
          )
        }
      } else {
        state.newRulesToday.forEach { rule ->
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(10.dp))
              .background(style.paperMid)
              .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Text(
              text = rule.title,
              style = MaterialTheme.typography.titleMedium,
              color = style.inkDark,
            )
            Text(
              text = rule.description,
              style = MaterialTheme.typography.bodyMedium,
              color = style.inkDark,
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      RetroMenuButton(
        title = if (isJa) "タイトル" else "TITLE",
        bgColor = style.deskMid,
        borderColor = style.paperShadow,
        onClick = onReturnTitle,
        modifier = Modifier.weight(0.36f),
      )
      RetroMenuButton(
        title = if (isJa) "業務開始 (%02d:%02d)".format(state.totalDaySeconds / 60, state.totalDaySeconds % 60) else "START SHIFT ▶",
        bgColor = PacketsPalette.PassDark,
        borderColor = PacketsPalette.PassLight,
        onClick = onStartShift,
        modifier = Modifier.weight(0.64f),
      )
    }
  }
}

@Composable
private fun EveningLedgerScreen(
  state: GameUiState,
  isJa: Boolean,
  onBuyUpgrade: (UpgradeType) -> Unit,
  onOpenSponsorAd: () -> Unit,
  onProceedNextDay: () -> Unit,
  onReturnTitle: () -> Unit,
  onOpenSettings: () -> Unit,
) {
  val style = LocalAppStyle.current
  val ledger = state.lastLedger

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(style.crtBlack)
      .padding(horizontal = 16.dp, vertical = 14.dp),
    verticalArrangement = Arrangement.SpaceBetween,
  ) {
    Column(
      modifier = Modifier
        .weight(1f)
        .verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "DAY %02d // 決算・設備投資".format(ledger?.day ?: state.currentDay),
          style = MaterialTheme.typography.titleLarge,
          color = style.crtPhosphorBright,
        )
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(style.crtGridDark)
            .clickable(onClick = onOpenSettings)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        ) {
          Text("⚙", style = MaterialTheme.typography.titleMedium, color = style.crtPhosphorBright)
        }
      }

      if (state.saveStatusToast != null) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1A3A24))
            .padding(horizontal = 12.dp, vertical = 7.dp),
        ) {
          Text(
            text = state.saveStatusToast,
            style = MaterialTheme.typography.labelMedium,
            color = style.crtPhosphorBright,
          )
        }
      }

      // Settlement Summary Card
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(12.dp))
          .background(style.paperBright)
          .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Text(
          text = if (isJa) "■ 本日の収支報告書" else "■ DAILY LEDGER",
          style = MaterialTheme.typography.titleMedium,
          color = style.inkDark,
        )
        Text(
          text = "判定実績: ${ledger?.processedCount ?: 0}件 (適正 ${ledger?.correctCount ?: 0} / 警告 ${ledger?.warningCount ?: 0})",
          style = MaterialTheme.typography.bodyMedium,
          color = style.inkDark,
        )
        Text(
          text = "正規報酬 +¥%,d  |  特別枠 +¥%,d  |  罰金 -¥%,d  |  運用費 -¥%,d".format(
            ledger?.incomeTotal ?: 0,
            ledger?.bribeTotal ?: 0,
            ledger?.penaltyTotal ?: 0,
            ledger?.operatingCost ?: 0,
          ),
          style = MaterialTheme.typography.bodyMedium,
          color = style.inkMuted,
        )
        Text(
          text = "現在利用可能予算: ¥%,d".format(state.budget),
          style = MaterialTheme.typography.titleLarge,
          color = if (state.budget < 1500) style.inkHighlight else PacketsPalette.PassDark,
        )
      }

      // Sponsor Ad Element (Opt-in 5s retro corporate CM for +¥1,000)
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(12.dp))
          .background(Color(0xFF1F2B1D))
          .border(1.5.dp, PacketsPalette.QuarantineLight, RoundedCornerShape(12.dp))
          .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Text(
          text = if (isJa) "📺 【広報スポンサー枠】東都サイバー保険" else "📺 [SPONSOR BRIEF] TOTO CYBER INS.",
          style = MaterialTheme.typography.titleMedium,
          color = PacketsPalette.QuarantineLight,
        )
        Text(
          text = if (isJa) {
            "5秒間の保安啓発CMを視聴すると、本日のゲート運用補助金 +¥1,000 を受け取れます（1日1回）"
          } else {
            "Watch a 5s safety sponsor clip to receive +¥1,000 gate budget grant (once per day)."
          },
          style = MaterialTheme.typography.bodyMedium,
          color = style.paperBright,
        )
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(
              if (state.sponsorAdClaimedToday) style.deskMid else PacketsPalette.QuarantineDark,
            )
            .border(
              1.5.dp,
              if (state.sponsorAdClaimedToday) style.paperShadow else PacketsPalette.QuarantineLight,
              RoundedCornerShape(10.dp),
            )
            .clickable(enabled = !state.sponsorAdClaimedToday, onClick = onOpenSponsorAd)
            .padding(horizontal = 12.dp, vertical = 10.dp),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = if (state.sponsorAdClaimedToday) {
              if (isJa) "✓ 本日の補助金受取済み" else "✓ SPONSOR GRANT CLAIMED TODAY"
            } else {
              if (isJa) "▶ 5秒CMを見て補助金 +¥1,000 を受け取る" else "▶ WATCH 5s AD FOR +¥1,000"
            },
            style = MaterialTheme.typography.labelMedium,
            color = if (state.sponsorAdClaimedToday) style.paperShadow else PacketsPalette.QuarantineLight,
          )
        }
      }

      // Upgrade Shop Section
      Text(
        text = if (isJa) "🛠 セキュリティ設備投資 (アップグレード)" else "🛠 TERMINAL UPGRADES",
        style = MaterialTheme.typography.titleMedium,
        color = style.crtPhosphorBright,
      )

      UpgradeType.entries.forEach { upgrade ->
        val owned = state.upgrades.has(upgrade)
        val canAfford = state.budget >= upgrade.cost
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(style.deskMid)
            .border(
              width = 1.dp,
              color = if (owned) PacketsPalette.PassLight else style.crtGridDark,
              shape = RoundedCornerShape(10.dp),
            )
            .padding(12.dp),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
          ) {
            Text(
              text = if (isJa) upgrade.titleJa else upgrade.titleEn,
              style = MaterialTheme.typography.titleMedium,
              color = if (owned) PacketsPalette.PassLight else style.paperBright,
            )
            Text(
              text = if (isJa) upgrade.descriptionJa else upgrade.descriptionEn,
              style = MaterialTheme.typography.bodyMedium,
              color = style.paperMid,
            )
          }

          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(8.dp))
              .background(
                when {
                  owned -> PacketsPalette.PassDark
                  canAfford -> style.crtGridDark
                  else -> Color(0xFF332825)
                },
              )
              .border(
                width = 1.5.dp,
                color = when {
                  owned -> PacketsPalette.PassLight
                  canAfford -> style.crtPhosphorBright
                  else -> style.paperShadow.copy(alpha = 0.4f)
                },
                shape = RoundedCornerShape(8.dp),
              )
              .clickable(enabled = !owned) { onBuyUpgrade(upgrade) }
              .heightIn(min = 46.dp)
              .padding(horizontal = 12.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = if (owned) {
                if (isJa) "導入済 ✓" else "OWNED"
              } else {
                "¥%,d\n購入".format(upgrade.cost)
              },
              style = MaterialTheme.typography.labelMedium,
              color = if (owned) PacketsPalette.PassLight else if (canAfford) style.crtPhosphorBright else style.paperShadow,
              textAlign = TextAlign.Center,
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      RetroMenuButton(
        title = if (isJa) "タイトルへ" else "TITLE",
        bgColor = style.deskMid,
        borderColor = style.paperShadow,
        onClick = onReturnTitle,
        modifier = Modifier.weight(0.36f),
      )
      RetroMenuButton(
        title = if (isJa) "次の日へ (DAY %02d) ▶".format(state.currentDay + 1) else "NEXT DAY (DAY %02d) ▶".format(state.currentDay + 1),
        bgColor = PacketsPalette.PassDark,
        borderColor = PacketsPalette.PassLight,
        onClick = onProceedNextDay,
        modifier = Modifier.weight(0.64f),
      )
    }
  }
}

@Composable
private fun EndingScreen(
  state: GameUiState,
  isJa: Boolean,
  onOpenCollection: () -> Unit,
  onReturnTitle: () -> Unit,
) {
  val style = LocalAppStyle.current
  val ending = state.endingType
  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(style.crtBlack)
      .padding(20.dp),
    verticalArrangement = Arrangement.SpaceBetween,
  ) {
    Column(
      modifier = Modifier
        .weight(1f)
        .verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Text(
        text = ending?.subtitle ?: "TERMINAL RECORD",
        style = MaterialTheme.typography.labelMedium,
        color = if (ending?.isGameOver == true) PacketsPalette.CrtAlertRed else style.crtPhosphorBright,
      )
      Text(
        text = ending?.title ?: "記録終了",
        style = MaterialTheme.typography.displaySmall,
        color = style.paperBright,
      )
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(12.dp))
          .background(style.paperBright)
          .padding(14.dp),
      ) {
        Text(
          text = ending?.epilogue ?: "",
          style = MaterialTheme.typography.bodyLarge,
          color = style.inkDark,
        )
      }
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(10.dp))
          .background(style.deskMid)
          .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Text(
          text = "■ 最終勤務記録",
          style = MaterialTheme.typography.titleMedium,
          color = style.crtPhosphorBright,
        )
        Text(
          text = "最終到達日: DAY %02d  |  残余予算: ¥%,d".format(state.currentDay, state.budget),
          style = MaterialTheme.typography.bodyMedium,
          color = style.paperBright,
        )
        Text(
          text = "総処理: %d件  |  適正: %d件  |  通算警告: %d回".format(
            state.totalProcessed,
            state.totalCorrect,
            state.totalWarnings,
          ),
          style = MaterialTheme.typography.bodyMedium,
          color = style.paperMid,
        )
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
      RetroMenuButton(
        title = if (isJa) "🏆 図鑑・実績を確認する" else "🏆 VIEW COLLECTION",
        bgColor = style.deskMid,
        borderColor = PacketsPalette.QuarantineLight,
        onClick = onOpenCollection,
      )
      RetroMenuButton(
        title = if (isJa) "タイトル画面へ戻る" else "RETURN TO TITLE",
        bgColor = style.crtGridDark,
        borderColor = style.crtPhosphorBright,
        onClick = onReturnTitle,
      )
    }
  }
}

@Composable
private fun SponsorAdModalOverlay(
  isJa: Boolean,
  onClaimReward: () -> Unit,
  onCancel: () -> Unit,
) {
  val style = LocalAppStyle.current
  var secondsLeft by remember { mutableIntStateOf(5) }
  LaunchedEffect(Unit) {
    while (secondsLeft > 0) {
      delay(1000L)
      secondsLeft -= 1
    }
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color(0xDA000000))
      .padding(20.dp),
    contentAlignment = Alignment.Center,
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(14.dp))
        .background(Color(0xFF152218))
        .border(2.dp, PacketsPalette.QuarantineLight, RoundedCornerShape(14.dp))
        .padding(18.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Text(
        text = if (isJa) "📺 スポンサーCM再生中" else "📺 SPONSOR BROADCAST",
        style = MaterialTheme.typography.titleMedium,
        color = PacketsPalette.QuarantineLight,
      )
      Image(
        bitmap = PixelArtAssets.appLogoCrestBitmap,
        contentDescription = "Sponsor",
        filterQuality = FilterQuality.None,
        modifier = Modifier.size(64.dp),
      )
      Text(
        text = if (isJa) {
          "「不審な拡張子を見たら、迷わず『検疫(QUARANTINE)』！あなたの職場を守る 東都サイバー保険」"
        } else {
          "\"Spot a suspicious executable? QUARANTINE immediately! — Toto Cyber Insurance\""
        },
        style = MaterialTheme.typography.bodyLarge,
        color = Color.White,
        textAlign = TextAlign.Center,
      )

      RetroMenuButton(
        title = if (secondsLeft > 0) {
          if (isJa) "補助金アンロックまで あと %d 秒…".format(secondsLeft) else "REWARD IN %d s...".format(secondsLeft)
        } else {
          if (isJa) "補助金 +¥1,000 を受け取る！" else "CLAIM +¥1,000 GRANT!"
        },
        bgColor = if (secondsLeft == 0) PacketsPalette.PassDark else style.deskMid,
        borderColor = if (secondsLeft == 0) PacketsPalette.PassLight else style.paperShadow,
        onClick = { if (secondsLeft == 0) onClaimReward() },
      )

      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(8.dp))
          .clickable(onClick = onCancel)
          .padding(horizontal = 16.dp, vertical = 8.dp),
      ) {
        Text(
          text = if (isJa) "スキップして閉じる" else "SKIP / CLOSE",
          style = MaterialTheme.typography.labelMedium,
          color = style.paperMid,
        )
      }
    }
  }
}

@Composable
private fun CollectionDialogOverlay(
  collection: CollectionProgress,
  isJa: Boolean,
  onClose: () -> Unit,
) {
  val style = LocalAppStyle.current
  var selectedTab by remember { mutableIntStateOf(0) }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color(0xD8000000))
      .padding(16.dp),
    contentAlignment = Alignment.Center,
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .clip(RoundedCornerShape(14.dp))
        .background(style.crtBlack)
        .border(2.dp, style.crtPhosphorMid, RoundedCornerShape(14.dp))
        .padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = if (isJa) "🏆 図鑑・実績アーカイブ" else "🏆 COLLECTION & ACHIEVEMENTS",
          style = MaterialTheme.typography.titleMedium,
          color = style.crtPhosphorBright,
        )
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(PacketsPalette.BlockDark)
            .clickable(onClick = onClose)
            .heightIn(min = 38.dp)
            .padding(horizontal = 12.dp, vertical = 6.dp),
          contentAlignment = Alignment.Center,
        ) {
          Text(if (isJa) "閉じる ✕" else "CLOSE ✕", style = MaterialTheme.typography.labelMedium, color = Color.White)
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        SelectOptionChip(
          label = if (isJa) "エンディング図鑑 (${collection.unlockedEndings.size}種)" else "ENDINGS (${collection.unlockedEndings.size})",
          selected = selectedTab == 0,
          onClick = { selectedTab = 0 },
          modifier = Modifier.weight(1f),
        )
        SelectOptionChip(
          label = if (isJa) {
            "実績 (${collection.unlockedAchievements.size}/${AchievementId.entries.size})"
          } else {
            "ACHIEVEMENTS (${collection.unlockedAchievements.size}/${AchievementId.entries.size})"
          },
          selected = selectedTab == 1,
          onClick = { selectedTab = 1 },
          modifier = Modifier.weight(1f),
        )
      }

      Column(
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        if (selectedTab == 0) {
          // NOTE: Intentionally only shows discovered endings; total ending count is NOT spoiled.
          Text(
            text = if (isJa) {
              "発見済みエンディング: ${collection.unlockedEndings.size}種"
            } else {
              "Discovered Endings: ${collection.unlockedEndings.size}"
            },
            style = MaterialTheme.typography.labelMedium,
            color = PacketsPalette.QuarantineLight,
          )
          if (collection.unlockedEndings.isEmpty()) {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(style.deskMid)
                .padding(16.dp),
            ) {
              Text(
                text = if (isJa) {
                  "まだ記録されたエンディングはありません。キャンペーンを進め、あなたの判断が導いた結末がここに収集されます。"
                } else {
                  "No endings recorded yet. Reach endings in Campaign mode to archive them here."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = style.paperBright,
              )
            }
          } else {
            collection.unlockedEndings.forEach { ending ->
              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(10.dp))
                  .background(style.paperBright)
                  .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
              ) {
                Text(
                  text = "${ending.subtitle} — ${ending.title}",
                  style = MaterialTheme.typography.titleMedium,
                  color = if (ending.isGameOver) style.inkHighlight else PacketsPalette.PassDark,
                )
                Text(
                  text = ending.epilogue,
                  style = MaterialTheme.typography.bodyMedium,
                  color = style.inkDark,
                )
              }
            }
          }
        } else {
          AchievementId.entries.forEach { ach ->
            val unlocked = ach in collection.unlockedAchievements
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(if (unlocked) Color(0xFF193622) else style.deskMid)
                .border(
                  1.dp,
                  if (unlocked) PacketsPalette.PassLight else style.paperShadow.copy(alpha = 0.3f),
                  RoundedCornerShape(10.dp),
                )
                .padding(12.dp),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = if (unlocked) "🏆" else "🔒",
                style = MaterialTheme.typography.titleLarge,
              )
              Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                  text = if (isJa) ach.titleJa else ach.titleEn,
                  style = MaterialTheme.typography.titleMedium,
                  color = if (unlocked) style.crtPhosphorBright else style.paperMid,
                )
                Text(
                  text = if (isJa) ach.descriptionJa else ach.descriptionEn,
                  style = MaterialTheme.typography.bodyMedium,
                  color = if (unlocked) Color.White else style.paperShadow,
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun SettingsDialogOverlay(
  settings: AppSettings,
  inWorkShift: Boolean,
  isJa: Boolean,
  onUpdateSettings: (AppSettings) -> Unit,
  onOpenM1Debug: () -> Unit,
  onOpenPrivacyPolicy: () -> Unit,
  onOpenGithub: () -> Unit,
  onReturnToTitle: (() -> Unit)?,
  onClose: () -> Unit,
) {
  val style = LocalAppStyle.current
  val volumes = listOf(0.0f to "0%", 0.25f to "25%", 0.5f to "50%", 0.8f to "80%", 1.0f to "100%")

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color(0xD8000000))
      .padding(16.dp),
    contentAlignment = Alignment.Center,
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .clip(RoundedCornerShape(14.dp))
        .background(style.crtBlack)
        .border(2.dp, style.crtPhosphorMid, RoundedCornerShape(14.dp))
        .padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = if (isJa) "⚙ 設定・ターミナル情報" else "⚙ SETTINGS & INFO",
          style = MaterialTheme.typography.titleLarge,
          color = style.crtPhosphorBright,
        )
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(PacketsPalette.BlockDark)
            .clickable(onClick = onClose)
            .heightIn(min = 38.dp)
            .padding(horizontal = 12.dp, vertical = 6.dp),
          contentAlignment = Alignment.Center,
        ) {
          Text(if (isJa) "閉じる ✕" else "CLOSE ✕", style = MaterialTheme.typography.labelMedium, color = Color.White)
        }
      }

      Column(
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        // Theme Mode
        SettingsSectionTitle(if (isJa) "テーマ" else "THEME")
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          ThemeMode.entries.forEach { mode ->
            SelectOptionChip(
              label = if (isJa) mode.labelJa else mode.labelEn,
              selected = settings.themeMode == mode,
              onClick = { onUpdateSettings(settings.copy(themeMode = mode)) },
              modifier = Modifier.weight(1f),
            )
          }
        }

        // Base Accent Color (2x2 Grid so labels never wrap awkwardly)
        SettingsSectionTitle(if (isJa) "ベースカラー" else "BASE ACCENT COLOR")
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          BaseAccentColor.entries.chunked(2).forEach { pair ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              pair.forEach { color ->
                val dotColor = when (color) {
                  BaseAccentColor.EMERALD -> Color(0xFF33CC5E)
                  BaseAccentColor.AMBER -> Color(0xFFF2A922)
                  BaseAccentColor.CYAN -> Color(0xFF24C8E0)
                  BaseAccentColor.CRIMSON -> Color(0xFFE84858)
                }
                Row(
                  modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 46.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (settings.accentColor == color) style.crtGridDark else style.deskMid)
                    .border(
                      1.5.dp,
                      if (settings.accentColor == color) dotColor else style.paperShadow.copy(alpha = 0.35f),
                      RoundedCornerShape(8.dp),
                    )
                    .clickable { onUpdateSettings(settings.copy(accentColor = color)) }
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                  horizontalArrangement = Arrangement.Center,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(dotColor))
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(
                    text = if (isJa) color.labelJa else color.labelEn,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    maxLines = 1,
                  )
                }
              }
            }
          }
        }

        // Language
        SettingsSectionTitle(if (isJa) "言語 / Language" else "LANGUAGE")
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          AppLanguage.entries.forEach { lang ->
            SelectOptionChip(
              label = lang.label,
              selected = settings.language == lang,
              onClick = { onUpdateSettings(settings.copy(language = lang)) },
              modifier = Modifier.weight(1f),
            )
          }
        }

        // Sound Volume
        SettingsSectionTitle(if (isJa) "効果音ボリューム" else "SOUND VOLUME")
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          volumes.forEach { (vol, label) ->
            SelectOptionChip(
              label = label,
              selected = kotlin.math.abs(settings.soundVolume - vol) < 0.08f,
              onClick = { onUpdateSettings(settings.copy(soundVolume = vol)) },
              modifier = Modifier.weight(1f),
            )
          }
        }

        // Font Scale
        SettingsSectionTitle(if (isJa) "文字の大きさ" else "FONT SIZE")
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          FontScaleOption.entries.forEach { opt ->
            SelectOptionChip(
              label = if (isJa) opt.labelJa else opt.labelEn,
              selected = settings.fontScale == opt,
              onClick = { onUpdateSettings(settings.copy(fontScale = opt)) },
              modifier = Modifier.weight(1f),
            )
          }
        }

        // Developer M1 Tuning Switch
        SettingsSectionTitle(if (isJa) "開発・チューニング" else "DEVELOPER")
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          SelectOptionChip(
            label = if (settings.showDeveloperDebug) "M1パネル表示: ON" else "M1パネル表示: OFF",
            selected = settings.showDeveloperDebug,
            onClick = { onUpdateSettings(settings.copy(showDeveloperDebug = !settings.showDeveloperDebug)) },
            modifier = Modifier.weight(1f),
          )
          if (settings.showDeveloperDebug) {
            SelectOptionChip(
              label = "M1チューニング開く",
              selected = false,
              onClick = onOpenM1Debug,
              modifier = Modifier.weight(1f),
            )
          }
        }

        // Links & Legal
        SettingsSectionTitle(if (isJa) "情報・リンク" else "INFO & LINKS")
        RetroMenuButton(
          title = if (isJa) "📜 プライバシーポリシーを読む" else "📜 PRIVACY POLICY",
          bgColor = style.deskMid,
          borderColor = style.paperShadow,
          onClick = onOpenPrivacyPolicy,
        )
        RetroMenuButton(
          title = if (isJa) "🌐 GitHub リポジトリを開く" else "🌐 OPEN GITHUB REPO",
          bgColor = style.crtGridDark,
          borderColor = style.crtPhosphorBright,
          onClick = onOpenGithub,
        )

        if (onReturnToTitle != null && inWorkShift) {
          RetroMenuButton(
            title = if (isJa) "業務を中断してタイトルへ戻る" else "ABORT SHIFT TO TITLE",
            bgColor = PacketsPalette.BlockDark,
            borderColor = PacketsPalette.BlockLight,
            onClick = onReturnToTitle,
          )
        }
      }
    }
  }
}

@Composable
private fun PrivacyPolicyDialogOverlay(
  isJa: Boolean,
  onOpenGithub: () -> Unit,
  onClose: () -> Unit,
) {
  val style = LocalAppStyle.current
  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color(0xDA000000))
      .padding(16.dp),
    contentAlignment = Alignment.Center,
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .clip(RoundedCornerShape(14.dp))
        .background(style.crtBlack)
        .border(2.dp, style.crtPhosphorMid, RoundedCornerShape(14.dp))
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = if (isJa) "📜 プライバシーポリシー" else "📜 PRIVACY POLICY",
          style = MaterialTheme.typography.titleLarge,
          color = style.crtPhosphorBright,
        )
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(PacketsPalette.BlockDark)
            .clickable(onClick = onClose)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
          Text(if (isJa) "閉じる ✕" else "CLOSE ✕", style = MaterialTheme.typography.labelMedium, color = Color.White)
        }
      }

      Column(
        modifier = Modifier
          .weight(1f)
          .verticalScroll(rememberScrollState())
          .clip(RoundedCornerShape(10.dp))
          .background(style.paperBright)
          .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Text(
          text = "パケッツ, プリーズ (Packets, Please) プライバシーポリシー",
          style = MaterialTheme.typography.titleMedium,
          color = style.inkDark,
        )
        Text(
          text = """
            1. データの収集について
            本アプリは完全オフラインで動作するスタンドアロンゲームです。氏名、メールアドレス、端末識別子、位置情報その他の個人情報を外部サーバーへ送信・収集することはありません。

            2. ローカル保存データ (DataStore)
            ゲームの進行状況、アンロック済みエンディング・実績図鑑、および設定情報（テーマ・言語・音量・文字サイズ）はお使いの端末内の DataStore にのみ保存されます。

            3. スポンサー広告枠について
            アプリ内の「東都サイバー保険 スポンサーCM枠」は世界観演出およびゲーム内通貨報酬のためのオフライン模擬イベントであり、外部アドネットワークSDKによるトラッキングは一切行っておりません。

            4. オープンソース・お問い合わせ
            ソースコードおよび最新のポリシー文書は以下の公式 GitHub リポジトリにて公開しています：
            $GITHUB_REPO_URL
          """.trimIndent(),
          style = MaterialTheme.typography.bodyMedium,
          color = style.inkDark,
        )
      }

      RetroMenuButton(
        title = if (isJa) "🌐 GitHub で全文を開く" else "🌐 VIEW ON GITHUB",
        bgColor = style.crtGridDark,
        borderColor = style.crtPhosphorBright,
        onClick = onOpenGithub,
      )
    }
  }
}

@Composable
private fun M1DebugPanelOverlay(
  screenPxW: Int,
  screenPxH: Int,
  state: GameUiState,
  onUpdateTuning: (ReferenceResolution, Int, AlertTimingMode) -> Unit,
  onJumpToDay: (Int) -> Unit,
  onFinishShiftNow: () -> Unit,
  onManualSave: () -> Unit,
  onToggleMute: () -> Unit,
  onReturnTitle: () -> Unit,
  onClose: () -> Unit,
) {
  val cfg = state.tuningConfig
  val style = LocalAppStyle.current
  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color(0xCC000000))
      .padding(16.dp),
    contentAlignment = Alignment.Center,
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(14.dp))
        .background(style.crtBlack)
        .border(2.dp, style.crtPhosphorMid, RoundedCornerShape(14.dp))
        .padding(14.dp)
        .verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "M1 TUNING DEBUG",
          style = MaterialTheme.typography.titleMedium,
          color = style.crtPhosphorBright,
        )
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(PacketsPalette.BlockDark)
            .clickable(onClick = onClose)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
          Text("CLOSE ✕", style = MaterialTheme.typography.labelSmall, color = Color.White)
        }
      }

      Text(
        text = "画面実測: ${screenPxW}x${screenPxH}px",
        style = MaterialTheme.typography.labelSmall,
        color = style.crtPhosphorMid,
      )

      Text("1日の制限時間", style = MaterialTheme.typography.labelMedium, color = style.paperBright)
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(120, 180, 300).forEach { sec ->
          SelectOptionChip(
            label = "${sec / 60}分(${sec}s)",
            selected = cfg.dayDurationSeconds == sec,
            onClick = { onUpdateTuning(cfg.referenceResolution, sec, cfg.alertTimingMode) },
            modifier = Modifier.weight(1f),
          )
        }
      }

      Text("警告タイミング", style = MaterialTheme.typography.labelMedium, color = style.paperBright)
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        AlertTimingMode.entries.forEach { mode ->
          SelectOptionChip(
            label = if (mode == AlertTimingMode.IMMEDIATE) "即時通知" else "遅延通知",
            selected = cfg.alertTimingMode == mode,
            onClick = { onUpdateTuning(cfg.referenceResolution, cfg.dayDurationSeconds, mode) },
            modifier = Modifier.weight(1f),
          )
        }
      }

      Text("デバッグ日程ジャンプ", style = MaterialTheme.typography.labelMedium, color = style.paperBright)
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        listOf(1, 2, 3, 5, 8, 14, 15).forEach { d ->
          Box(
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(6.dp))
              .background(style.crtGridDark)
              .clickable { onJumpToDay(d) }
              .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center,
          ) {
            Text(if (d == 15) "∞" else "D$d", style = MaterialTheme.typography.labelSmall, color = style.crtPhosphorBright)
          }
        }
      }

      if (state.phase == GamePhase.WORK_SHIFT) {
        RetroMenuButton(
          title = "今すぐシフト終了 (日次精算へ)",
          bgColor = PacketsPalette.QuarantineDark,
          borderColor = PacketsPalette.QuarantineLight,
          onClick = onFinishShiftNow,
        )
      }

      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        SelectOptionChip("手動セーブ", false, onManualSave, Modifier.weight(1f))
        SelectOptionChip(if (state.isMuted) "音:OFF" else "音:ON", !state.isMuted, onToggleMute, Modifier.weight(1f))
        SelectOptionChip("タイトルへ", false, onReturnTitle, Modifier.weight(1f))
      }
    }
  }
}

@Composable
private fun SettingsSectionTitle(text: String) {
  val style = LocalAppStyle.current
  Text(
    text = text,
    style = MaterialTheme.typography.labelMedium,
    color = style.crtPhosphorMid,
  )
}

@Composable
private fun SelectOptionChip(
  label: String,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val style = LocalAppStyle.current
  Box(
    modifier = modifier
      .heightIn(min = 44.dp)
      .clip(RoundedCornerShape(8.dp))
      .background(if (selected) PacketsPalette.PassDark else style.deskMid)
      .border(
        width = if (selected) 2.dp else 1.dp,
        color = if (selected) style.crtPhosphorBright else style.paperShadow.copy(alpha = 0.4f),
        shape = RoundedCornerShape(8.dp),
      )
      .clickable(onClick = onClick)
      .padding(horizontal = 8.dp, vertical = 8.dp),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = if (selected) style.crtPhosphorBright else Color.White,
      textAlign = TextAlign.Center,
      maxLines = 2,
    )
  }
}

@Composable
private fun UtilityActionCard(
  label: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val style = LocalAppStyle.current
  Box(
    modifier = modifier
      .height(52.dp)
      .clip(RoundedCornerShape(12.dp))
      .background(style.deskMid)
      .border(1.5.dp, style.crtGridDark, RoundedCornerShape(12.dp))
      .clickable(onClick = onClick)
      .padding(horizontal = 10.dp),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelMedium,
      color = style.paperBright,
      fontWeight = FontWeight.Bold,
    )
  }
}

@Composable
private fun RetroMenuButton(
  title: String,
  bgColor: Color,
  borderColor: Color,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Box(
    modifier = modifier
      .fillMaxWidth()
      .heightIn(min = 56.dp)
      .clip(RoundedCornerShape(12.dp))
      .background(bgColor)
      .border(2.dp, borderColor, RoundedCornerShape(12.dp))
      .clickable(onClick = onClick)
      .padding(vertical = 12.dp, horizontal = 14.dp),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = title,
      style = MaterialTheme.typography.titleMedium,
      color = Color.White,
      textAlign = TextAlign.Center,
    )
  }
}
