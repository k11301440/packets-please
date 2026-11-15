package com.tommy.packetsplease.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tommy.packetsplease.data.GameContentRepository
import com.tommy.packetsplease.data.SaveDataRepository
import com.tommy.packetsplease.domain.AchievementId
import com.tommy.packetsplease.domain.AlertTimingMode
import com.tommy.packetsplease.domain.AppSettings
import com.tommy.packetsplease.domain.CollectionProgress
import com.tommy.packetsplease.domain.DailyLedger
import com.tommy.packetsplease.domain.EconomyEngine
import com.tommy.packetsplease.domain.EndingType
import com.tommy.packetsplease.domain.GameContentAsset
import com.tommy.packetsplease.domain.GameMode
import com.tommy.packetsplease.domain.InspectableTarget
import com.tommy.packetsplease.domain.InspectionResult
import com.tommy.packetsplease.domain.M1TuningConfig
import com.tommy.packetsplease.domain.OsAlert
import com.tommy.packetsplease.domain.Packet
import com.tommy.packetsplease.domain.PacketGenerator
import com.tommy.packetsplease.domain.PolicyRule
import com.tommy.packetsplease.domain.ReferenceResolution
import com.tommy.packetsplease.domain.RuleEngine
import com.tommy.packetsplease.domain.SavedProgress
import com.tommy.packetsplease.domain.ToolType
import com.tommy.packetsplease.domain.UpgradeState
import com.tommy.packetsplease.domain.UpgradeType
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
  VIRUS_DESTRUCTION,
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
  val remainingSeconds: Int = 180,
  val totalDaySeconds: Int = 180,
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
  val lastLedger: DailyLedger? = null,
  val endingType: EndingType? = null,
  val tuningConfig: M1TuningConfig = M1TuningConfig(),
  val showM1DebugDialog: Boolean = false,
  val isMuted: Boolean = false,
  val saveStatusToast: String? = null,
  // v0.4 State Additions
  val settings: AppSettings = AppSettings(),
  val collection: CollectionProgress = CollectionProgress(),
  val upgrades: UpgradeState = UpgradeState(),
  val quarantinedFamilies: Set<String> = emptySet(),
  val isInterPacketInterval: Boolean = false,
  val intervalHeadline: String = "",
  val intervalSubtext: String = "",
  val isVirusExplosionActive: Boolean = false,
  val isSettingsOpen: Boolean = false,
  val isCollectionOpen: Boolean = false,
  val isPrivacyPolicyOpen: Boolean = false,
  val isSponsorAdModalOpen: Boolean = false,
  val sponsorAdClaimedToday: Boolean = false,
  val newlyUnlockedAchievementBanner: String? = null,
  val batchCounter: Int = 1,
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
  private var intervalJob: Job? = null
  private var isTransitioningVerdict = false
  private var dailyBribeOfferRejected = false

  init {
    viewModelScope.launch {
      val saved = saveRepo.loadProgress()
      val tuning = saveRepo.loadTuningConfig()
      val settings = saveRepo.loadSettings()
      val collection = saveRepo.loadCollection()

      RetroSoundEngine.volume = settings.soundVolume
      RetroSoundEngine.muted = settings.soundVolume <= 0.01f

      _uiState.update {
        it.copy(
          hasSavedGame = saved != null,
          savedDayPreview = saved?.currentDay ?: 1,
          tuningConfig = tuning,
          remainingSeconds = tuning.dayDurationSeconds,
          totalDaySeconds = tuning.dayDurationSeconds,
          settings = settings,
          isMuted = RetroSoundEngine.muted,
          collection = collection,
          quarantinedFamilies = collection.quarantinedThreatFamilies,
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
      upgrades = UpgradeState(),
      quarantinedFamilies = _uiState.value.collection.quarantinedThreatFamilies,
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
        upgrades = saved.upgrades,
        quarantinedFamilies = saved.quarantinedFamilies + _uiState.value.collection.quarantinedThreatFamilies,
      )
    }
  }

  fun jumpToDay(day: Int) {
    val cfg = _uiState.value.tuningConfig
    val mode = if (day > EconomyEngine.CAMPAIGN_TOTAL_DAYS) GameMode.ENDLESS else GameMode.CAMPAIGN
    _uiState.update { it.copy(showM1DebugDialog = false, isSettingsOpen = false) }
    prepareMorningBriefing(
      day = day,
      mode = mode,
      budget = maxOf(cfg.startingBudget, _uiState.value.budget),
      auditRisk = _uiState.value.auditRisk,
      aanCount = _uiState.value.aanCooperationCount,
      totalBribes = _uiState.value.totalBribesAccepted,
      totalProcessed = _uiState.value.totalProcessed,
      totalCorrect = _uiState.value.totalCorrect,
      totalWarnings = _uiState.value.totalWarnings,
      upgrades = _uiState.value.upgrades,
      quarantinedFamilies = _uiState.value.quarantinedFamilies,
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
    upgrades: UpgradeState,
    quarantinedFamilies: Set<String>,
  ) {
    shiftTimerJob?.cancel()
    intervalJob?.cancel()
    isTransitioningVerdict = false
    dailyBribeOfferRejected = false

    val scenario = content.days.find { it.day == day }
    val activeRules = RuleEngine.activeRulesForDay(content, day)
    val initialQueue = PacketGenerator.generateDayQueue(content, day)
    val paddedQueue = if (initialQueue.size < 6) {
      initialQueue + PacketGenerator.generateShiftBatch(content, day, batchIndex = 1, count = 8)
    } else {
      initialQueue
    }
    val nextBatchIndex = if (initialQueue.size < 6) 2 else 1
    val cfg = _uiState.value.tuningConfig

    val headline = scenario?.morningHeadline ?: "DAY %02d: エンドレス監視任務".format(day)
    val memo = scenario?.morningMemo
      ?: "全規則および過去検疫DBを総動員し、制限時間まで通信ゲートを守り抜け。違反は『遮断』、攻撃・マルウェアは『検疫』で処理せよ。"

    _uiState.update {
      it.copy(
        phase = GamePhase.MORNING_BRIEFING,
        gameMode = mode,
        currentDay = day,
        morningHeadline = headline,
        morningMemo = memo,
        newRulesToday = scenario?.newRules ?: emptyList(),
        activeRules = activeRules,
        packetQueue = paddedQueue,
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
        saveStatusToast = null,
        upgrades = upgrades,
        quarantinedFamilies = quarantinedFamilies,
        isInterPacketInterval = false,
        intervalHeadline = "",
        intervalSubtext = "",
        isVirusExplosionActive = false,
        isSponsorAdModalOpen = false,
        sponsorAdClaimedToday = false,
        newlyUnlockedAchievementBanner = null,
        batchCounter = nextBatchIndex,
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
      delay(650L)
      _uiState.update {
        if (it.packetFlowAnim == PacketFlowAnim.INCOMING) {
          it.copy(packetFlowAnim = PacketFlowAnim.IDLE)
        } else {
          it
        }
      }
    }
    startShiftTimer()
  }

  private fun startShiftTimer() {
    shiftTimerJob?.cancel()
    shiftTimerJob = viewModelScope.launch {
      while (true) {
        delay(1000L)
        val s = _uiState.value
        if (s.phase != GamePhase.WORK_SHIFT) break
        if (s.isSettingsOpen || s.showM1DebugDialog || s.isVirusExplosionActive) continue
        if (s.remainingSeconds <= 1) {
          _uiState.update { it.copy(remainingSeconds = 0) }
          finishShift()
          break
        } else {
          _uiState.update { it.copy(remainingSeconds = it.remainingSeconds - 1) }
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
        lastInspectionResult = if (next) state.lastInspectionResult else null,
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
    if (first == null) {
      _uiState.update {
        it.copy(
          firstInspectTarget = target,
          secondInspectTarget = null,
          lastInspectionResult = null,
        )
      }
      return
    }
    if (first.targetId == target.targetId) {
      _uiState.update {
        it.copy(
          firstInspectTarget = null,
          secondInspectTarget = null,
        )
      }
      return
    }
    val result = RuleEngine.inspect(first, target, packet)
    RetroSoundEngine.playInspectMatch(!result.matched)
    _uiState.update {
      it.copy(
        secondInspectTarget = target,
        lastInspectionResult = result,
      )
    }
  }

  fun togglePolicyBook() {
    RetroSoundEngine.playPaperSlide()
    _uiState.update {
      it.copy(
        isPolicyBookOpen = !it.isPolicyBookOpen,
        isToolTrayOpen = false,
        activeToolSlip = null,
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
    RetroSoundEngine.playPaperSlide()
    _uiState.update {
      it.copy(
        activeToolSlip = tool,
        isToolTrayOpen = false,
        inspectedToolsForCurrentPacket = it.inspectedToolsForCurrentPacket + tool,
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
    if (state.phase != GamePhase.WORK_SHIFT || state.isInterPacketInterval) return
    val packet = state.currentPacket ?: return

    isTransitioningVerdict = true

    val eval = RuleEngine.evaluateVerdict(
      packet = packet,
      verdict = verdict,
      dailyWarningsSoFar = state.dailyWarnings,
      config = state.tuningConfig,
      upgrades = state.upgrades,
      quarantinedFamilies = state.quarantinedFamilies,
    )

    if (eval.fatalVirusBreached) {
      RetroSoundEngine.playVirusExplosion()
    } else {
      RetroSoundEngine.playStampSound(isQuarantine = verdict == Verdict.QUARANTINE)
      if (eval.isCorrect) {
        RetroSoundEngine.playPassChime()
      } else if (eval.penaltyApplied > 0 || eval.warningIncrement > 0) {
        RetroSoundEngine.playAlertWarning()
      } else {
        RetroSoundEngine.playBlockTone()
      }
    }

    if (packet.bribeOffer != null && !eval.acceptedBribe && eval.isCorrect) {
      dailyBribeOfferRejected = true
    }

    val newProcessed = state.dailyProcessed + 1
    val newAlerts = state.activeOsAlerts.toMutableList()
    val newDelayed = state.delayedAlerts.toMutableList()

    val triggeredDelayed = newDelayed.filter { (triggerAt, _) -> newProcessed >= triggerAt }
    if (triggeredDelayed.isNotEmpty()) {
      newDelayed.removeAll(triggeredDelayed.toSet())
      newAlerts.addAll(triggeredDelayed.map { it.second })
    }

    if (eval.osAlert != null) {
      if (state.tuningConfig.alertTimingMode == AlertTimingMode.IMMEDIATE || eval.fatalVirusBreached) {
        newAlerts.add(eval.osAlert)
      } else {
        newDelayed.add((newProcessed + 1) to eval.osAlert)
      }
    }

    val newBudget = state.budget + eval.budgetDelta
    val newIncome = state.dailyIncome + if (eval.isCorrect) eval.budgetDelta else 0
    val newBribeIncome = state.dailyBribeIncome + if (eval.acceptedBribe) eval.budgetDelta else 0
    val newPenalty = state.dailyPenalty + eval.penaltyApplied

    val updatedFamilies = if (eval.newlyQuarantinedFamily != null) {
      state.quarantinedFamilies + eval.newlyQuarantinedFamily
    } else {
      state.quarantinedFamilies
    }

    val unlockedNow = mutableListOf<AchievementId>()
    if (eval.newlyQuarantinedFamily != null) {
      unlockedNow.add(AchievementId.VIRUS_HUNTER)
    }
    if (eval.usedQuarantineIntel) {
      unlockedNow.add(AchievementId.INTEL_REUSE)
    }
    val newTotalProcessed = state.totalProcessed + 1
    if (newTotalProcessed >= 30) {
      unlockedNow.add(AchievementId.THIRTY_PACKETS)
    }

    val flowAnim = if (eval.fatalVirusBreached) {
      PacketFlowAnim.VIRUS_DESTRUCTION
    } else {
      when (verdict) {
        Verdict.PASS -> PacketFlowAnim.OUT_PASS
        Verdict.BLOCK -> PacketFlowAnim.OUT_BLOCK
        Verdict.QUARANTINE -> PacketFlowAnim.OUT_QUARANTINE
      }
    }

    val headline = if (eval.fatalVirusBreached) {
      "【重大インシデント】致命的マルウェア侵入！"
    } else {
      "処理完了: ${verdictDisplayJa(verdict)} (${verdict.displayLabel})"
    }

    val subtext = if (eval.fatalVirusBreached) {
      "社内コアノードが汚染・破損した！マルウェアや攻撃は『検疫 (QUARANTINE)』で隔離せよ。"
    } else {
      "${eval.feedbackTitle} — ${eval.feedbackDetail}"
    }

    _uiState.update {
      it.copy(
        budget = newBudget,
        auditRisk = (it.auditRisk + eval.auditRiskDelta).coerceIn(0, 100),
        aanCooperationCount = it.aanCooperationCount + eval.aanCooperationDelta,
        totalBribesAccepted = it.totalBribesAccepted + if (eval.acceptedBribe) 1 else 0,
        totalProcessed = newTotalProcessed,
        totalCorrect = it.totalCorrect + if (eval.isCorrect) 1 else 0,
        totalWarnings = it.totalWarnings + eval.warningIncrement,
        dailyProcessed = newProcessed,
        dailyCorrect = it.dailyCorrect + if (eval.isCorrect) 1 else 0,
        dailyWarnings = it.dailyWarnings + eval.warningIncrement,
        dailyIncome = newIncome,
        dailyBribeIncome = newBribeIncome,
        dailyPenalty = newPenalty,
        activeOsAlerts = newAlerts,
        delayedAlerts = newDelayed,
        lastFeedbackBanner = "${eval.feedbackTitle}: ${eval.feedbackDetail}",
        stampedVerdictAnim = verdict,
        packetFlowAnim = flowAnim,
        quarantinedFamilies = updatedFamilies,
        isInterPacketInterval = true,
        intervalHeadline = headline,
        intervalSubtext = subtext,
        isVirusExplosionActive = eval.fatalVirusBreached,
        isPolicyBookOpen = false,
        isToolTrayOpen = false,
        activeToolSlip = null,
      )
    }

    recordCollectionUpdates(
      newAchievements = unlockedNow,
      newThreatFamilies = if (eval.newlyQuarantinedFamily != null) setOf(eval.newlyQuarantinedFamily) else emptySet(),
    )

    intervalJob?.cancel()
    intervalJob = viewModelScope.launch {
      delay(if (eval.fatalVirusBreached) 3000L else 2200L)
      advanceToNextPacket()
    }
  }

  fun skipIntervalToNextPacket() {
    val state = _uiState.value
    if (!state.isInterPacketInterval || state.phase != GamePhase.WORK_SHIFT) return
    intervalJob?.cancel()
    RetroSoundEngine.playPaperSlide()
    advanceToNextPacket()
  }

  private fun advanceToNextPacket() {
    val after = _uiState.value
    if (after.phase != GamePhase.WORK_SHIFT) {
      isTransitioningVerdict = false
      return
    }
    var updatedQueue = after.packetQueue
    var nextBatchCounter = after.batchCounter
    val nextIdx = after.currentPacketIndex + 1

    if (nextIdx >= updatedQueue.size - 1) {
      updatedQueue = updatedQueue + PacketGenerator.generateShiftBatch(
        content = content,
        day = after.currentDay,
        batchIndex = nextBatchCounter,
        count = 8,
      )
      nextBatchCounter += 1
    }

    _uiState.update {
      it.copy(
        packetQueue = updatedQueue,
        currentPacketIndex = nextIdx,
        batchCounter = nextBatchCounter,
        stampedVerdictAnim = null,
        packetFlowAnim = PacketFlowAnim.INCOMING,
        isInterPacketInterval = false,
        isVirusExplosionActive = false,
        intervalHeadline = "",
        intervalSubtext = "",
        firstInspectTarget = null,
        secondInspectTarget = null,
        lastInspectionResult = null,
        activeToolSlip = null,
        isToolTrayOpen = false,
        inspectedToolsForCurrentPacket = emptySet(),
      )
    }
    isTransitioningVerdict = false

    viewModelScope.launch {
      delay(650L)
      _uiState.update {
        if (it.packetFlowAnim == PacketFlowAnim.INCOMING) {
          it.copy(packetFlowAnim = PacketFlowAnim.IDLE)
        } else {
          it
        }
      }
    }
  }

  fun finishShiftNowForDebug() {
    _uiState.update { it.copy(showM1DebugDialog = false, isSettingsOpen = false) }
    finishShift()
  }

  private fun finishShift() {
    shiftTimerJob?.cancel()
    intervalJob?.cancel()
    isTransitioningVerdict = false

    val state = _uiState.value
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

    val shiftAchievements = buildList {
      add(AchievementId.FIRST_DAY_CLEAR)
      if (state.dailyWarnings == 0 && state.dailyCorrect >= 5) {
        add(AchievementId.PERFECT_SHIFT)
      }
      if (dailyBribeOfferRejected) {
        add(AchievementId.BRIBE_REFUSER)
      }
      if (state.currentDay >= 7) {
        add(AchievementId.SURVIVOR_WEEK)
      }
      if (outcome.ending == EndingType.LOYAL_FIREWALL ||
        outcome.ending == EndingType.WHISTLEBLOWER ||
        outcome.ending == EndingType.SHADOW_GATEKEEPER ||
        outcome.ending == EndingType.GRAY_SURVIVOR
      ) {
        add(AchievementId.CAMPAIGN_HERO)
      }
      if (outcome.ending == EndingType.WHISTLEBLOWER) {
        add(AchievementId.TRUTH_SEEKER)
      }
    }

    if (outcome.ending != null) {
      recordCollectionUpdates(
        newAchievements = shiftAchievements,
        newEndings = setOf(outcome.ending),
      )
      viewModelScope.launch { saveRepo.clearProgress() }
      _uiState.update {
        it.copy(
          phase = GamePhase.ENDING,
          budget = outcome.ledger.endingBudget,
          lastLedger = outcome.ledger,
          endingType = outcome.ending,
          hasSavedGame = false,
          isInterPacketInterval = false,
          isVirusExplosionActive = false,
          activeOsAlerts = it.activeOsAlerts + remainingAlerts,
          delayedAlerts = emptyList(),
        )
      }
    } else {
      recordCollectionUpdates(newAchievements = shiftAchievements)
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
        upgrades = state.upgrades,
        quarantinedFamilies = state.quarantinedFamilies,
      )
      viewModelScope.launch { saveRepo.saveProgress(progress) }
      _uiState.update {
        it.copy(
          phase = GamePhase.EVENING_LEDGER,
          budget = outcome.ledger.endingBudget,
          lastLedger = outcome.ledger,
          hasSavedGame = true,
          savedDayPreview = nextDay,
          isInterPacketInterval = false,
          isVirusExplosionActive = false,
          activeOsAlerts = it.activeOsAlerts + remainingAlerts,
          delayedAlerts = emptyList(),
          saveStatusToast = "日次オートセーブ完了 (DAY $nextDay)",
        )
      }
    }
  }

  fun buyUpgrade(type: UpgradeType) {
    val state = _uiState.value
    if (state.upgrades.has(type)) return
    if (state.budget < type.cost) {
      RetroSoundEngine.playBlockTone()
      _uiState.update { it.copy(saveStatusToast = "予算不足: ${type.titleJa} (必要 ¥%,d)".format(type.cost)) }
      return
    }
    RetroSoundEngine.playUpgradeChime()
    val updatedUpgrades = state.upgrades.copy(owned = state.upgrades.owned + type)
    val newBudget = state.budget - type.cost
    val unlocked = buildList {
      add(AchievementId.UPGRADE_BUYER)
      if (updatedUpgrades.owned.size >= UpgradeType.entries.size) {
        add(AchievementId.FULL_ARSENAL)
      }
    }
    _uiState.update {
      it.copy(
        budget = newBudget,
        upgrades = updatedUpgrades,
        saveStatusToast = "設備導入完了: ${type.titleJa}",
      )
    }
    recordCollectionUpdates(newAchievements = unlocked)
    persistEveningProgress(budget = newBudget, upgrades = updatedUpgrades)
  }

  fun openSponsorAdModal() {
    if (_uiState.value.sponsorAdClaimedToday) return
    _uiState.update { it.copy(isSponsorAdModalOpen = true) }
  }

  fun closeSponsorAdModal() {
    _uiState.update { it.copy(isSponsorAdModalOpen = false) }
  }

  fun claimDailySponsorAdReward() {
    val state = _uiState.value
    if (state.sponsorAdClaimedToday) return
    RetroSoundEngine.playUpgradeChime()
    val bonus = 1000
    val newBudget = state.budget + bonus
    _uiState.update {
      it.copy(
        budget = newBudget,
        sponsorAdClaimedToday = true,
        isSponsorAdModalOpen = false,
        saveStatusToast = "スポンサー補助金 +¥%,d を受領！".format(bonus),
      )
    }
    recordCollectionUpdates(newAchievements = listOf(AchievementId.SPONSOR_WATCHER))
    persistEveningProgress(budget = newBudget, upgrades = state.upgrades)
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
      upgrades = state.upgrades,
      quarantinedFamilies = state.quarantinedFamilies,
    )
  }

  fun manualSaveCurrentProgress() {
    val state = _uiState.value
    val targetDay = if (state.phase == GamePhase.EVENING_LEDGER) state.currentDay + 1 else state.currentDay
    val progress = buildSavedProgress(targetDay, state.budget, state.upgrades)
    viewModelScope.launch {
      saveRepo.saveProgress(progress)
      _uiState.update {
        it.copy(
          hasSavedGame = true,
          savedDayPreview = targetDay,
          saveStatusToast = "手動セーブを保存しました (DAY $targetDay)",
        )
      }
    }
  }

  private fun persistEveningProgress(budget: Int, upgrades: UpgradeState) {
    val state = _uiState.value
    val targetDay = if (state.phase == GamePhase.EVENING_LEDGER) state.currentDay + 1 else state.currentDay
    val progress = buildSavedProgress(targetDay, budget, upgrades)
    viewModelScope.launch {
      saveRepo.saveProgress(progress)
    }
  }

  private fun buildSavedProgress(
    targetDay: Int,
    budget: Int,
    upgrades: UpgradeState,
  ): SavedProgress {
    val state = _uiState.value
    return SavedProgress(
      currentDay = targetDay,
      gameMode = state.gameMode,
      budget = budget,
      auditRisk = state.auditRisk,
      aanCooperationCount = state.aanCooperationCount,
      totalWarnings = state.totalWarnings,
      totalProcessed = state.totalProcessed,
      totalCorrect = state.totalCorrect,
      totalBribesAccepted = state.totalBribesAccepted,
      tuningConfig = state.tuningConfig,
      lastLedger = state.lastLedger,
      upgrades = upgrades,
      quarantinedFamilies = state.quarantinedFamilies,
    )
  }

  private fun recordCollectionUpdates(
    newAchievements: List<AchievementId> = emptyList(),
    newEndings: Set<EndingType> = emptySet(),
    newThreatFamilies: Set<String> = emptySet(),
  ) {
    val current = _uiState.value.collection
    val addedAchievements = newAchievements.filter { it !in current.unlockedAchievements }
    val updated = current.copy(
      unlockedEndings = current.unlockedEndings + newEndings,
      unlockedAchievements = current.unlockedAchievements + newAchievements,
      quarantinedThreatFamilies = current.quarantinedThreatFamilies + newThreatFamilies,
    )
    if (updated == current) return
    val banner = addedAchievements.firstOrNull()?.let { "実績解除: ${it.titleJa}" }
    _uiState.update {
      it.copy(
        collection = updated,
        newlyUnlockedAchievementBanner = banner ?: it.newlyUnlockedAchievementBanner,
      )
    }
    viewModelScope.launch {
      saveRepo.saveCollection(updated)
    }
  }

  fun dismissAchievementBanner() {
    _uiState.update { it.copy(newlyUnlockedAchievementBanner = null) }
  }

  fun returnToTitle() {
    shiftTimerJob?.cancel()
    intervalJob?.cancel()
    isTransitioningVerdict = false
    _uiState.update {
      it.copy(
        phase = GamePhase.TITLE,
        showM1DebugDialog = false,
        isSettingsOpen = false,
        isCollectionOpen = false,
        isPrivacyPolicyOpen = false,
        isSponsorAdModalOpen = false,
        isInterPacketInterval = false,
        isVirusExplosionActive = false,
      )
    }
  }

  fun toggleSettingsDialog(open: Boolean) {
    _uiState.update {
      it.copy(
        isSettingsOpen = open,
        saveStatusToast = if (open) null else it.saveStatusToast,
      )
    }
  }

  fun toggleCollectionDialog(open: Boolean) {
    _uiState.update { it.copy(isCollectionOpen = open) }
  }

  fun togglePrivacyPolicyDialog(open: Boolean) {
    _uiState.update { it.copy(isPrivacyPolicyOpen = open) }
  }

  fun updateSettings(newSettings: AppSettings) {
    RetroSoundEngine.volume = newSettings.soundVolume
    RetroSoundEngine.muted = newSettings.soundVolume <= 0.01f
    _uiState.update {
      it.copy(
        settings = newSettings,
        isMuted = RetroSoundEngine.muted,
      )
    }
    viewModelScope.launch {
      saveRepo.saveSettings(newSettings)
    }
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
    val currentVol = _uiState.value.settings.soundVolume
    val nextMuted = !_uiState.value.isMuted
    RetroSoundEngine.muted = nextMuted
    if (!nextMuted && currentVol <= 0.01f) {
      updateSettings(_uiState.value.settings.copy(soundVolume = 0.8f))
    } else {
      _uiState.update { it.copy(isMuted = nextMuted) }
    }
  }

  private fun verdictDisplayJa(verdict: Verdict): String = when (verdict) {
    Verdict.PASS -> "許可"
    Verdict.BLOCK -> "遮断"
    Verdict.QUARANTINE -> "検疫"
  }
}
