package com.tommy.packetsplease.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tommy.packetsplease.data.GameContentRepository
import com.tommy.packetsplease.data.SaveDataRepository
import com.tommy.packetsplease.domain.AlertTimingMode
import com.tommy.packetsplease.domain.EconomyEngine
import com.tommy.packetsplease.domain.EndingType
import com.tommy.packetsplease.domain.GameContentAsset
import com.tommy.packetsplease.domain.GameMode
import com.tommy.packetsplease.domain.InspectableTarget
import com.tommy.packetsplease.domain.InspectionResult
import com.tommy.packetsplease.domain.M1TuningConfig
import com.tommy.packetsplease.domain.OsAlert
import com.tommy.packetsplease.domain.Packet
import com.tommy.packetsplease.domain.PacketField
import com.tommy.packetsplease.domain.PacketGenerator
import com.tommy.packetsplease.domain.PolicyRule
import com.tommy.packetsplease.domain.ReferenceResolution
import com.tommy.packetsplease.domain.RuleEngine
import com.tommy.packetsplease.domain.SavedProgress
import com.tommy.packetsplease.domain.ToolType
import com.tommy.packetsplease.domain.Verdict
import com.tommy.packetsplease.ui.audio.RetroSoundEngine
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class GamePhase {
  TITLE,
  MORNING_BRIEFING,
  WORK_SHIFT,
  EVENING_LEDGER,
  ENDING,
}

enum class PacketFlowAnim {
  INCOMING,
  IDLE,
  OUT_PASS,
  OUT_BLOCK,
  OUT_QUARANTINE,
}

data class GameUiState(
  val phase: GamePhase = GamePhase.TITLE,
  val hasSavedGame: Boolean = false,
  val savedDayPreview: Int = 1,
  val gameMode: GameMode = GameMode.CAMPAIGN,
  val currentDay: Int = 1,
  val morningHeadline: String = "",
  val morningMemo: String = "",
  val newRulesToday: List<PolicyRule> = emptyList(),
  val activeRules: List<PolicyRule> = emptyList(),
  val packetQueue: List<Packet> = emptyList(),
  val currentPacketIndex: Int = 0,
  val remainingSeconds: Int = 300,
  val totalDaySeconds: Int = 300,
  val budget: Int = 5000,
  val auditRisk: Int = 0,
  val aanCooperationCount: Int = 0,
  val totalBribesAccepted: Int = 0,
  val totalProcessed: Int = 0,
  val totalCorrect: Int = 0,
  val totalWarnings: Int = 0,
  val dailyProcessed: Int = 0,
  val dailyCorrect: Int = 0,
  val dailyWarnings: Int = 0,
  val dailyIncome: Int = 0,
  val dailyBribeIncome: Int = 0,
  val dailyPenalty: Int = 0,
  val isInspectionMode: Boolean = false,
  val firstInspectTarget: InspectableTarget? = null,
  val secondInspectTarget: InspectableTarget? = null,
  val lastInspectionResult: InspectionResult? = null,
  val isPolicyBookOpen: Boolean = false,
  val isToolTrayOpen: Boolean = false,
  val activeToolSlip: ToolType? = null,
  val inspectedToolsForCurrentPacket: Set<ToolType> = emptySet(),
  val activeOsAlerts: List<OsAlert> = emptyList(),
  val delayedAlerts: List<Pair<Int, OsAlert>> = emptyList(),
  val lastFeedbackBanner: String? = null,
  val stampedVerdictAnim: Verdict? = null,
  val packetFlowAnim: PacketFlowAnim = PacketFlowAnim.IDLE,
  val lastLedger: com.tommy.packetsplease.domain.DailyLedger? = null,
  val endingType: EndingType? = null,
  val tuningConfig: M1TuningConfig = M1TuningConfig(),
  val showM1DebugDialog: Boolean = false,
  val isMuted: Boolean = false,
  val saveStatusToast: String? = null,
) {
  val currentPacket: Packet?
    get() = packetQueue.getOrNull(currentPacketIndex)
}

class GameViewModel(application: Application) : AndroidViewModel(application) {

  private val contentRepo = GameContentRepository(application)
  private val saveRepo = SaveDataRepository(application)
  private val content: GameContentAsset = contentRepo.loadContent()

  private val _uiState = MutableStateFlow(GameUiState())
  val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

  private var shiftTimerJob: Job? = null
  private var isTransitioningVerdict = false

  init {
    viewModelScope.launch {
      val saved = saveRepo.loadProgress()
      val tuning = saveRepo.loadTuningConfig()
      _uiState.update {
        it.copy(
          hasSavedGame = saved != null,
          savedDayPreview = saved?.currentDay ?: 1,
          tuningConfig = tuning,
          remainingSeconds = tuning.dayDurationSeconds,
          totalDaySeconds = tuning.dayDurationSeconds,
        )
      }
    }
  }

  fun startNewGame(mode: GameMode) {
    val cfg = _uiState.value.tuningConfig
    val startDay = if (mode == GameMode.ENDLESS) 15 else 1
    RetroSoundEngine.playStartupJingle()
    prepareMorningBriefing(
      day = startDay,
      mode = mode,
      budget = cfg.startingBudget,
      auditRisk = 0,
      aanCount = 0,
      totalBribes = 0,
      totalProcessed = 0,
      totalCorrect = 0,
      totalWarnings = 0,
    )
  }

  fun continueSavedGame() {
    viewModelScope.launch {
      val saved = saveRepo.loadProgress() ?: return@launch
      RetroSoundEngine.playStartupJingle()
      prepareMorningBriefing(
        day = saved.currentDay,
        mode = saved.gameMode,
        budget = saved.budget,
        auditRisk = saved.auditRisk,
        aanCount = saved.aanCooperationCount,
        totalBribes = saved.totalBribesAccepted,
        totalProcessed = saved.totalProcessed,
        totalCorrect = saved.totalCorrect,
        totalWarnings = saved.totalWarnings,
      )
    }
  }

  fun jumpToDay(day: Int) {
    val cfg = _uiState.value.tuningConfig
    val mode = if (day > EconomyEngine.CAMPAIGN_TOTAL_DAYS) GameMode.ENDLESS else GameMode.CAMPAIGN
    _uiState.update { it.copy(showM1DebugDialog = false) }
    prepareMorningBriefing(
      day = day,
      mode = mode,
      budget = _uiState.value.budget.coerceAtLeast(cfg.startingBudget),
      auditRisk = _uiState.value.auditRisk,
      aanCount = _uiState.value.aanCooperationCount,
      totalBribes = _uiState.value.totalBribesAccepted,
      totalProcessed = _uiState.value.totalProcessed,
      totalCorrect = _uiState.value.totalCorrect,
      totalWarnings = _uiState.value.totalWarnings,
    )
  }

  private fun prepareMorningBriefing(
    day: Int,
    mode: GameMode,
    budget: Int,
    auditRisk: Int,
    aanCount: Int,
    totalBribes: Int,
    totalProcessed: Int,
    totalCorrect: Int,
    totalWarnings: Int,
  ) {
    shiftTimerJob?.cancel()
    val scenario = content.days.find { it.day == day }
    val activeRules = RuleEngine.activeRulesForDay(content, day)
    val queue = PacketGenerator.generateDayQueue(content, day)
    val cfg = _uiState.value.tuningConfig

    val headline = scenario?.morningHeadline ?: "DAY $day: エンドレス特別監査"
    val memo = scenario?.morningMemo
      ?: "全8項目のセキュリティポリシーが適用中。可能な限り多くの通信を正確に処理せよ。"

    _uiState.update {
      it.copy(
        phase = GamePhase.MORNING_BRIEFING,
        gameMode = mode,
        currentDay = day,
        morningHeadline = headline,
        morningMemo = memo,
        newRulesToday = scenario?.newRules.orEmpty(),
        activeRules = activeRules,
        packetQueue = queue,
        currentPacketIndex = 0,
        remainingSeconds = cfg.dayDurationSeconds,
        totalDaySeconds = cfg.dayDurationSeconds,
        budget = budget,
        auditRisk = auditRisk,
        aanCooperationCount = aanCount,
        totalBribesAccepted = totalBribes,
        totalProcessed = totalProcessed,
        totalCorrect = totalCorrect,
        totalWarnings = totalWarnings,
        dailyProcessed = 0,
        dailyCorrect = 0,
        dailyWarnings = 0,
        dailyIncome = 0,
        dailyBribeIncome = 0,
        dailyPenalty = 0,
        isInspectionMode = false,
        firstInspectTarget = null,
        secondInspectTarget = null,
        lastInspectionResult = null,
        isPolicyBookOpen = false,
        isToolTrayOpen = false,
        activeToolSlip = null,
        inspectedToolsForCurrentPacket = emptySet(),
        activeOsAlerts = emptyList(),
        delayedAlerts = emptyList(),
        lastFeedbackBanner = null,
        stampedVerdictAnim = null,
        packetFlowAnim = PacketFlowAnim.IDLE,
        endingType = null,
      )
    }
  }

  fun startWorkShift() {
    RetroSoundEngine.playStartupJingle()
    _uiState.update {
      it.copy(
        phase = GamePhase.WORK_SHIFT,
        packetFlowAnim = PacketFlowAnim.INCOMING,
      )
    }
    viewModelScope.launch {
      delay(220)
      _uiState.update {
        if (it.packetFlowAnim == PacketFlowAnim.INCOMING) it.copy(packetFlowAnim = PacketFlowAnim.IDLE) else it
      }
    }
    startShiftTimer()
  }

  private fun startShiftTimer() {
    shiftTimerJob?.cancel()
    shiftTimerJob = viewModelScope.launch {
      while (true) {
        delay(1000L)
        val state = _uiState.value
        if (state.phase != GamePhase.WORK_SHIFT) break
        val nextSec = (state.remainingSeconds - 1).coerceAtLeast(0)
        _uiState.update { it.copy(remainingSeconds = nextSec) }
        if (nextSec <= 0) {
          finishShift()
          break
        }
      }
    }
  }

  fun toggleInspectionMode() {
    _uiState.update { state ->
      val next = !state.isInspectionMode
      state.copy(
        isInspectionMode = next,
        firstInspectTarget = null,
        secondInspectTarget = null,
        lastInspectionResult = null,
      )
    }
  }

  fun clearInspectionSelection() {
    _uiState.update {
      it.copy(
        firstInspectTarget = null,
        secondInspectTarget = null,
        lastInspectionResult = null,
      )
    }
  }

  fun selectInspectTarget(target: InspectableTarget) {
    val state = _uiState.value
    val packet = state.currentPacket ?: return

    val first = state.firstInspectTarget
    val second = state.secondInspectTarget

    if (first == null || second != null) {
      _uiState.update {
        it.copy(
          isInspectionMode = true,
          firstInspectTarget = target,
          secondInspectTarget = null,
          lastInspectionResult = null,
        )
      }
      return
    }

    val result = RuleEngine.inspect(first, target, packet)
    RetroSoundEngine.playInspectMatch(result.matched)
    _uiState.update {
      it.copy(
        isInspectionMode = true,
        secondInspectTarget = target,
        lastInspectionResult = result,
        isPolicyBookOpen = false,
      )
    }
  }

  fun togglePolicyBook() {
    _uiState.update {
      it.copy(
        isPolicyBookOpen = !it.isPolicyBookOpen,
        isToolTrayOpen = false,
      )
    }
  }

  fun toggleToolTray() {
    _uiState.update {
      it.copy(
        isToolTrayOpen = !it.isToolTrayOpen,
        isPolicyBookOpen = false,
      )
    }
  }

  fun openTool(tool: ToolType) {
    _uiState.update {
      it.copy(
        activeToolSlip = tool,
        inspectedToolsForCurrentPacket = it.inspectedToolsForCurrentPacket + tool,
        isToolTrayOpen = false,
      )
    }
  }

  fun closeToolSlip() {
    _uiState.update { it.copy(activeToolSlip = null) }
  }

  fun dismissOsAlert(alertId: String) {
    _uiState.update { state ->
      state.copy(activeOsAlerts = state.activeOsAlerts.filterNot { it.id == alertId })
    }
  }

  fun applyVerdict(verdict: Verdict) {
    if (isTransitioningVerdict) return
    val state = _uiState.value
    if (state.phase != GamePhase.WORK_SHIFT) return
    val packet = state.currentPacket ?: return

    isTransitioningVerdict = true
    RetroSoundEngine.playStampSound(isQuarantine = verdict == Verdict.QUARANTINE)

    val eval = RuleEngine.evaluateVerdict(
      packet = packet,
      verdict = verdict,
      dailyWarningsSoFar = state.dailyWarnings,
      config = state.tuningConfig,
    )

    val flowAnim = when (verdict) {
      Verdict.PASS -> PacketFlowAnim.OUT_PASS
      Verdict.BLOCK -> PacketFlowAnim.OUT_BLOCK
      Verdict.QUARANTINE -> PacketFlowAnim.OUT_QUARANTINE
    }

    when {
      eval.osAlert != null && !eval.osAlert.isBribeNotice -> RetroSoundEngine.playAlertWarning()
      verdict == Verdict.PASS -> RetroSoundEngine.playPassChime()
      else -> RetroSoundEngine.playBlockTone()
    }

    val newProcessed = state.dailyProcessed + 1
    val newAlerts = state.activeOsAlerts.toMutableList()
    val newDelayed = state.delayedAlerts.toMutableList()

    // 遅延通知キューからトリガー時刻に達したものを発火
    val triggeredDelayed = newDelayed.filter { (triggerAt, _) -> newProcessed >= triggerAt }
    newDelayed.removeAll(triggeredDelayed)
    triggeredDelayed.forEach { (_, alert) -> newAlerts.add(alert) }

    // 新規発生アラートを即時 or 遅延で追加
    if (eval.osAlert != null) {
      if (state.tuningConfig.alertTimingMode == AlertTimingMode.IMMEDIATE || eval.osAlert.isBribeNotice) {
        newAlerts.add(eval.osAlert)
      } else {
        newDelayed.add((newProcessed + 2) to eval.osAlert)
      }
    }

    val newBudget = state.budget + eval.budgetDelta
    val newIncome = state.dailyIncome + if (eval.isCorrect) eval.budgetDelta else 0
    val newBribeIncome = state.dailyBribeIncome + if (eval.acceptedBribe) eval.budgetDelta else 0
    val newPenalty = state.dailyPenalty + eval.penaltyApplied

    _uiState.update {
      it.copy(
        budget = newBudget,
        auditRisk = it.auditRisk + eval.auditRiskDelta,
        aanCooperationCount = it.aanCooperationCount + eval.aanCooperationDelta,
        totalBribesAccepted = it.totalBribesAccepted + if (eval.acceptedBribe) 1 else 0,
        totalProcessed = it.totalProcessed + 1,
        totalCorrect = it.totalCorrect + if (eval.isCorrect) 1 else 0,
        totalWarnings = it.totalWarnings + eval.warningIncrement,
        dailyProcessed = newProcessed,
        dailyCorrect = it.dailyCorrect + if (eval.isCorrect) 1 else 0,
        dailyWarnings = it.dailyWarnings + eval.warningIncrement,
        dailyIncome = newIncome,
        dailyBribeIncome = newBribeIncome,
        dailyPenalty = newPenalty,
        stampedVerdictAnim = verdict,
        packetFlowAnim = flowAnim,
        lastFeedbackBanner = eval.feedbackTitle,
        activeOsAlerts = newAlerts,
        delayedAlerts = newDelayed,
        isPolicyBookOpen = false,
        isToolTrayOpen = false,
        activeToolSlip = null,
        firstInspectTarget = null,
        secondInspectTarget = null,
        lastInspectionResult = null,
        isInspectionMode = false,
      )
    }

    viewModelScope.launch {
      delay(180L) // 要件5.1: アニメーションは100〜200ms以内
      val after = _uiState.value
      val nextIdx = after.currentPacketIndex + 1
      if (nextIdx >= after.packetQueue.size) {
        if (after.gameMode == GameMode.ENDLESS) {
          // エンドレスは残時間があればさらに補充する
          val nextBatch = PacketGenerator.generateDayQueue(
            content = content,
            day = after.currentDay,
            seed = after.currentDay * 1000 + after.dailyProcessed,
          )
          _uiState.update {
            it.copy(
              packetQueue = it.packetQueue + nextBatch,
              currentPacketIndex = nextIdx,
              stampedVerdictAnim = null,
              packetFlowAnim = PacketFlowAnim.INCOMING,
            )
          }
          delay(140L)
          _uiState.update { it.copy(packetFlowAnim = PacketFlowAnim.IDLE) }
        } else {
          finishShift()
        }
      } else {
        _uiState.update {
          it.copy(
            currentPacketIndex = nextIdx,
            stampedVerdictAnim = null,
            packetFlowAnim = PacketFlowAnim.INCOMING,
          )
        }
        delay(140L)
        _uiState.update { it.copy(packetFlowAnim = PacketFlowAnim.IDLE) }
      }
      isTransitioningVerdict = false
    }
  }

  fun finishShiftNowForDebug() {
    _uiState.update { it.copy(showM1DebugDialog = false) }
    finishShift()
  }

  private fun finishShift() {
    shiftTimerJob?.cancel()
    isTransitioningVerdict = false
    val state = _uiState.value

    // 未発火の遅延アラートも一日の終わりに確定表示可能
    val remainingAlerts = state.delayedAlerts.map { it.second }

    val outcome = EconomyEngine.settleEndOfDay(
      day = state.currentDay,
      gameMode = state.gameMode,
      currentBudget = state.budget,
      auditRisk = state.auditRisk,
      aanCooperationCount = state.aanCooperationCount,
      totalBribesAccepted = state.totalBribesAccepted,
      processedCount = state.dailyProcessed,
      correctCount = state.dailyCorrect,
      warningCount = state.dailyWarnings,
      incomeTotal = state.dailyIncome,
      bribeTotal = state.dailyBribeIncome,
      penaltyTotal = state.dailyPenalty,
      config = state.tuningConfig,
    )

    if (outcome.ending != null) {
      viewModelScope.launch { saveRepo.clearProgress() }
      _uiState.update {
        it.copy(
          phase = GamePhase.ENDING,
          budget = outcome.ledger.endingBudget,
          lastLedger = outcome.ledger,
          endingType = outcome.ending,
          hasSavedGame = false,
          activeOsAlerts = it.activeOsAlerts + remainingAlerts,
          delayedAlerts = emptyList(),
        )
      }
    } else {
      val nextDay = state.currentDay + 1
      val progress = SavedProgress(
        currentDay = nextDay,
        gameMode = state.gameMode,
        budget = outcome.ledger.endingBudget,
        auditRisk = state.auditRisk,
        aanCooperationCount = state.aanCooperationCount,
        totalWarnings = state.totalWarnings,
        totalProcessed = state.totalProcessed,
        totalCorrect = state.totalCorrect,
        totalBribesAccepted = state.totalBribesAccepted,
        tuningConfig = state.tuningConfig,
        lastLedger = outcome.ledger,
      )
      viewModelScope.launch { saveRepo.saveProgress(progress) }
      _uiState.update {
        it.copy(
          phase = GamePhase.EVENING_LEDGER,
          budget = outcome.ledger.endingBudget,
          lastLedger = outcome.ledger,
          hasSavedGame = true,
          savedDayPreview = nextDay,
          activeOsAlerts = it.activeOsAlerts + remainingAlerts,
          delayedAlerts = emptyList(),
          saveStatusToast = "日次オートセーブ完了 (DAY $nextDay)",
        )
      }
    }
  }

  fun proceedToNextDay() {
    val state = _uiState.value
    val nextDay = state.currentDay + 1
    prepareMorningBriefing(
      day = nextDay,
      mode = state.gameMode,
      budget = state.budget,
      auditRisk = state.auditRisk,
      aanCount = state.aanCooperationCount,
      totalBribes = state.totalBribesAccepted,
      totalProcessed = state.totalProcessed,
      totalCorrect = state.totalCorrect,
      totalWarnings = state.totalWarnings,
    )
  }

  fun manualSaveCurrentProgress() {
    val state = _uiState.value
    val targetDay = if (state.phase == GamePhase.EVENING_LEDGER) state.currentDay + 1 else state.currentDay
    val progress = SavedProgress(
      currentDay = targetDay,
      gameMode = state.gameMode,
      budget = state.budget,
      auditRisk = state.auditRisk,
      aanCooperationCount = state.aanCooperationCount,
      totalWarnings = state.totalWarnings,
      totalProcessed = state.totalProcessed,
      totalCorrect = state.totalCorrect,
      totalBribesAccepted = state.totalBribesAccepted,
      tuningConfig = state.tuningConfig,
      lastLedger = state.lastLedger,
    )
    viewModelScope.launch {
      saveRepo.saveProgress(progress)
      _uiState.update {
        it.copy(
          hasSavedGame = true,
          savedDayPreview = targetDay,
          saveStatusToast = "手動セーブ完了 (DAY $targetDay)",
        )
      }
    }
  }

  fun returnToTitle() {
    shiftTimerJob?.cancel()
    _uiState.update { it.copy(phase = GamePhase.TITLE, showM1DebugDialog = false) }
  }

  fun toggleM1DebugDialog() {
    _uiState.update { it.copy(showM1DebugDialog = !it.showM1DebugDialog, saveStatusToast = null) }
  }

  fun updateTuningConfig(
    resolution: ReferenceResolution = _uiState.value.tuningConfig.referenceResolution,
    dayDurationSec: Int = _uiState.value.tuningConfig.dayDurationSeconds,
    alertTiming: AlertTimingMode = _uiState.value.tuningConfig.alertTimingMode,
  ) {
    val updated = _uiState.value.tuningConfig.copy(
      referenceResolution = resolution,
      dayDurationSeconds = dayDurationSec,
      alertTimingMode = alertTiming,
    )
    viewModelScope.launch { saveRepo.saveTuningConfig(updated) }
    _uiState.update {
      it.copy(
        tuningConfig = updated,
        totalDaySeconds = dayDurationSec,
        remainingSeconds = minOf(it.remainingSeconds, dayDurationSec),
      )
    }
  }

  fun toggleMute() {
    val next = !_uiState.value.isMuted
    RetroSoundEngine.muted = next
    _uiState.update { it.copy(isMuted = next) }
  }
}
