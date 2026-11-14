package com.tommy.packetsplease.domain

data class EndOfDayOutcome(
  val ledger: DailyLedger,
  val ending: EndingType?,
  val auditAlertTriggered: Boolean,
)

object EconomyEngine {

  const val CAMPAIGN_TOTAL_DAYS = 14
  const val AUDIT_DETECTION_THRESHOLD = 75

  fun settleEndOfDay(
    day: Int,
    gameMode: GameMode,
    currentBudget: Int,
    auditRisk: Int,
    aanCooperationCount: Int,
    totalBribesAccepted: Int,
    processedCount: Int,
    correctCount: Int,
    warningCount: Int,
    incomeTotal: Int,
    bribeTotal: Int,
    penaltyTotal: Int,
    config: M1TuningConfig,
  ): EndOfDayOutcome {
    val operatingCost = config.dailyOperatingCost
    val endingBudget = currentBudget - operatingCost
    val ledger = DailyLedger(
      day = day,
      processedCount = processedCount,
      correctCount = correctCount,
      warningCount = warningCount,
      incomeTotal = incomeTotal,
      bribeTotal = bribeTotal,
      penaltyTotal = penaltyTotal,
      operatingCost = operatingCost,
      endingBudget = endingBudget,
    )

    if (endingBudget < 0) {
      return EndOfDayOutcome(
        ledger = ledger,
        ending = EndingType.ABOLISHED_BUDGET,
        auditAlertTriggered = false,
      )
    }

    val isPeriodicAuditDay = (day == 7 || day == 11 || day >= CAMPAIGN_TOTAL_DAYS)
    if (auditRisk >= 100 || (isPeriodicAuditDay && auditRisk >= AUDIT_DETECTION_THRESHOLD)) {
      return EndOfDayOutcome(
        ledger = ledger,
        ending = EndingType.ABOLISHED_AUDIT,
        auditAlertTriggered = true,
      )
    }

    if (gameMode == GameMode.CAMPAIGN && day >= CAMPAIGN_TOTAL_DAYS) {
      val ending = resolveCampaignEnding(
        auditRisk = auditRisk,
        aanCooperationCount = aanCooperationCount,
        bribeAcceptedCount = totalBribesAccepted,
      )
      return EndOfDayOutcome(
        ledger = ledger,
        ending = ending,
        auditAlertTriggered = false,
      )
    }

    return EndOfDayOutcome(
      ledger = ledger,
      ending = null,
      auditAlertTriggered = false,
    )
  }

  fun resolveCampaignEnding(
    auditRisk: Int,
    aanCooperationCount: Int,
    bribeAcceptedCount: Int,
  ): EndingType {
    return when {
      auditRisk >= AUDIT_DETECTION_THRESHOLD -> EndingType.ABOLISHED_AUDIT
      aanCooperationCount >= 3 -> EndingType.SHADOW_GATEKEEPER
      aanCooperationCount in 1..2 -> EndingType.WHISTLEBLOWER
      auditRisk > 0 || bribeAcceptedCount > 0 -> EndingType.GRAY_SURVIVOR
      else -> EndingType.LOYAL_FIREWALL
    }
  }
}
