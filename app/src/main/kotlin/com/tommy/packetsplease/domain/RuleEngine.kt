package com.tommy.packetsplease.domain

object RuleEngine {

  const val FAST_GATE_BONUS = 300

  fun activeRulesForDay(content: GameContentAsset, day: Int): List<PolicyRule> {
    return content.days
      .filter { it.day <= day }
      .flatMap { it.newRules }
  }

  fun inspect(
    first: InspectableTarget,
    second: InspectableTarget,
    packet: Packet,
  ): InspectionResult {
    if (first.targetId == second.targetId) {
      return InspectionResult(
        matched = false,
        title = "同一対象",
        detail = "異なる2つの項目を選択して照合してください。",
      )
    }

    val discrepancy = packet.discrepancy
      ?: return InspectionResult(
        matched = false,
        title = "矛盾なし",
        detail = "「${first.label}」と「${second.label}」の間に規則違反や矛盾は確認されませんでした。",
      )

    val matchesPrimary =
      (first.targetId == discrepancy.primaryTargetA && second.targetId == discrepancy.primaryTargetB) ||
        (first.targetId == discrepancy.primaryTargetB && second.targetId == discrepancy.primaryTargetA)

    val matchesAlternate = discrepancy.validPairs.any { pair ->
      (first.targetId == pair.targetA && second.targetId == pair.targetB) ||
        (first.targetId == pair.targetB && second.targetId == pair.targetA)
    }

    return if (matchesPrimary || matchesAlternate) {
      InspectionResult(
        matched = true,
        title = "矛盾を検出！",
        detail = discrepancy.summary,
        recommendedTool = discrepancy.recommendedTool,
      )
    } else {
      InspectionResult(
        matched = false,
        title = "関連矛盾なし",
        detail = "この組み合わせでは不整合は確定できません。別の項目やツール・ポリシー規則と照合してください。",
      )
    }
  }

  fun evaluateVerdict(
    packet: Packet,
    verdict: Verdict,
    dailyWarningsSoFar: Int,
    config: M1TuningConfig,
    upgrades: UpgradeState = UpgradeState(),
    quarantinedFamilies: Set<String> = emptySet(),
  ): VerdictEvaluation {
    val fastBonus = if (upgrades.has(UpgradeType.FAST_GATE)) FAST_GATE_BONUS else 0
    val bribe = packet.bribeOffer
    if (bribe != null && verdict == Verdict.PASS && packet.expectedVerdict != Verdict.PASS) {
      return VerdictEvaluation(
        isCorrect = false,
        budgetDelta = bribe.bonusAmount,
        auditRiskDelta = bribe.auditRiskDelta,
        aanCooperationDelta = 0,
        warningIncrement = 0,
        penaltyApplied = 0,
        feedbackTitle = "袖之下を受領 (+¥${bribe.bonusAmount})",
        feedbackDetail = "${bribe.offerTitle} を黙認通過させました。監査リスクが蓄積しています。",
        osAlert = OsAlert(
          id = "alert_${packet.id}",
          packetId = packet.id,
          isPenalty = false,
          isBribeNotice = true,
          warningNumber = dailyWarningsSoFar,
          penaltyAmount = 0,
          title = "ログ改ざん完了（闇予算 +¥${bribe.bonusAmount}）",
          message = "内部監査スコアが上昇しました（+${bribe.auditRiskDelta}）。発覚するとシステム廃止になります。",
        ),
        acceptedBribe = true,
      )
    }

    if (packet.isAuditAvoidanceEvent && packet.aanDesiredVerdict != null) {
      val followedAan = verdict == packet.aanDesiredVerdict
      val followedRule = verdict == packet.expectedVerdict

      if (followedAan && followedRule) {
        val reward = config.passReward + fastBonus + if (verdict == Verdict.QUARANTINE) config.quarantineBonus else 0
        return VerdictEvaluation(
          isCorrect = true,
          budgetDelta = reward,
          auditRiskDelta = 0,
          aanCooperationDelta = 1,
          warningIncrement = 0,
          penaltyApplied = 0,
          feedbackTitle = "極秘任務達成 (+¥$reward)",
          feedbackDetail = "規則順守と監査回避ネットワークの要請を両立しました。",
        )
      }

      if (followedAan && !followedRule) {
        val nextWarning = dailyWarningsSoFar + 1
        val penalty = if (nextWarning > config.freeWarningsPerDay) config.penaltyAmount else 0
        return VerdictEvaluation(
          isCorrect = false,
          budgetDelta = -penalty,
          auditRiskDelta = 10,
          aanCooperationDelta = 1,
          warningIncrement = 1,
          penaltyApplied = penalty,
          feedbackTitle = "裏要請を優先（警告 #$nextWarning）",
          feedbackDetail = "監査回避ネットワークの要請を実行。OSには規則違反として記録されました。",
          osAlert = buildPenaltyAlert(packet, verdict, nextWarning, penalty, config.freeWarningsPerDay),
        )
      }
    }

    val family = packet.threatFamily
    val knownQuarantined = family != null && family in quarantinedFamilies
    val acceptedByQuarantineIntel =
      upgrades.has(UpgradeType.QUARANTINE_INTEL) &&
        knownQuarantined &&
        packet.expectedVerdict == Verdict.QUARANTINE &&
        verdict == Verdict.BLOCK

    if (verdict == packet.expectedVerdict || acceptedByQuarantineIntel) {
      val bonus = if (verdict == Verdict.QUARANTINE) config.quarantineBonus else 0
      val reward = config.passReward + bonus + fastBonus
      val bribeNote = if (bribe != null) "（袖之下を拒絶）" else ""
      val intelNote = if (acceptedByQuarantineIntel) " [検疫DB連携遮断]" else ""
      val bonusText = if (bonus > 0) " [検疫報告 +¥$bonus]" else ""
      val newlyQuarantined = if (verdict == Verdict.QUARANTINE && packet.expectedVerdict == Verdict.QUARANTINE) {
        family ?: packet.id
      } else {
        null
      }
      return VerdictEvaluation(
        isCorrect = true,
        budgetDelta = reward,
        auditRiskDelta = 0,
        aanCooperationDelta = 0,
        warningIncrement = 0,
        penaltyApplied = 0,
        feedbackTitle = "正審判定 (+¥$reward)$bonusText$intelNote",
        feedbackDetail = "${packet.reasonSummary}$bribeNote",
        newlyQuarantinedFamily = newlyQuarantined,
        usedQuarantineIntel = acceptedByQuarantineIntel,
      )
    }

    val nextWarning = dailyWarningsSoFar + 1
    val penalty = if (nextWarning > config.freeWarningsPerDay) config.penaltyAmount else 0
    val fatalBreach = verdict == Verdict.PASS && (packet.isFatalVirus || packet.expectedVerdict == Verdict.QUARANTINE)
    return VerdictEvaluation(
      isCorrect = false,
      budgetDelta = -penalty,
      auditRiskDelta = 0,
      aanCooperationDelta = 0,
      warningIncrement = 1,
      penaltyApplied = penalty,
      feedbackTitle = when {
        fatalBreach && penalty > 0 -> "致命的ウイルス侵入！ (-¥$penalty)"
        fatalBreach -> "致命的ウイルス侵入！ (警告 #$nextWarning)"
        penalty > 0 -> "誤審・罰金 (-¥$penalty)"
        else -> "誤審・警告 #$nextWarning"
      },
      feedbackDetail = packet.reasonSummary,
      osAlert = buildPenaltyAlert(packet, verdict, nextWarning, penalty, config.freeWarningsPerDay, fatalBreach),
      fatalVirusBreached = fatalBreach,
    )
  }

  private fun buildPenaltyAlert(
    packet: Packet,
    actualVerdict: Verdict,
    warningNumber: Int,
    penalty: Int,
    freeWarnings: Int,
    fatalBreach: Boolean = false,
  ): OsAlert {
    val penaltyTag = if (penalty > 0) "罰金 ¥$penalty" else "警告のみ ($warningNumber/${freeWarnings}回目)"
    val incident = when {
      fatalBreach ->
        "致命的ウイルスが社内LANへ侵入・展開されました！正解: QUARANTINE（${packet.reasonSummary}）"
      actualVerdict == Verdict.PASS && packet.expectedVerdict == Verdict.QUARANTINE ->
        "通した通信の隠蔽ペイロードが社内端末に展開されました。正解: QUARANTINE（${packet.reasonSummary}）"
      actualVerdict == Verdict.PASS && packet.expectedVerdict == Verdict.BLOCK ->
        "不正通信の通過により部門インシデントが発生。正解: BLOCK（${packet.reasonSummary}）"
      actualVerdict == Verdict.QUARANTINE && packet.expectedVerdict != Verdict.QUARANTINE ->
        "正当な検疫対象ではない通信を隔離・濫用しました。正解: ${packet.expectedVerdict.displayLabel}（${packet.reasonSummary}）"
      actualVerdict != Verdict.PASS && packet.expectedVerdict == Verdict.PASS ->
        "業務上正当な通信を遮断/隔離し、社内ワークフローが遅延しました。正解: PASS"
      else ->
        "判定手順に相違があります。正解: ${packet.expectedVerdict.displayLabel}（${packet.reasonSummary}）"
    }
    return OsAlert(
      id = "alert_${packet.id}_$warningNumber",
      packetId = packet.id,
      isPenalty = penalty > 0 || fatalBreach,
      warningNumber = warningNumber,
      penaltyAmount = penalty,
      title = if (fatalBreach) "CRITICAL: ウイルス感染検知 [$penaltyTag]" else "OS不正検知 [$penaltyTag]",
      message = incident,
    )
  }
}
