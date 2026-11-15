package com.tommy.packetsplease

import com.tommy.packetsplease.domain.EconomyEngine
import com.tommy.packetsplease.domain.EndingType
import com.tommy.packetsplease.domain.GameContentAsset
import com.tommy.packetsplease.domain.GameMode
import com.tommy.packetsplease.domain.InspectableTarget
import com.tommy.packetsplease.domain.M1TuningConfig
import com.tommy.packetsplease.domain.PacketField
import com.tommy.packetsplease.domain.PacketGenerator
import com.tommy.packetsplease.domain.AppSettings
import com.tommy.packetsplease.domain.FontScaleOption
import com.tommy.packetsplease.domain.RuleEngine
import com.tommy.packetsplease.domain.UpgradeState
import com.tommy.packetsplease.domain.UpgradeType
import com.tommy.packetsplease.domain.Verdict
import java.io.File
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DomainLogicTest {

  private lateinit var content: GameContentAsset
  private val config = M1TuningConfig()

  @Before
  fun setUp() {
    val candidates = listOf(
      File("app/src/main/assets/game_content.json"),
      File("src/main/assets/game_content.json"),
    )
    val jsonFile = candidates.firstOrNull { it.exists() }
      ?: error("game_content.json not found from ${File(".").absolutePath}")

    val json = Json { ignoreUnknownKeys = true }
    content = json.decodeFromString(jsonFile.readText())
  }

  @Test
  fun day1StartsWithTwoPacketsAndOneRule() {
    val rules = RuleEngine.activeRulesForDay(content, 1)
    val queue = PacketGenerator.generateDayQueue(content, 1)

    assertEquals(1, rules.size)
    assertEquals("R1", rules.first().id)
    assertEquals(2, queue.size)
    assertEquals(Verdict.PASS, queue[0].expectedVerdict)
    assertEquals(Verdict.BLOCK, queue[1].expectedVerdict)
  }

  @Test
  fun campaignDays1Through14GenerateCleanly() {
    assertEquals(14, content.days.size)
    for (day in 1..15) {
      val queue = PacketGenerator.generateDayQueue(content, day)
      assertTrue("Day $day queue should not be empty", queue.isNotEmpty())
      queue.forEach { pkt ->
        assertTrue(pkt.from.email.contains("@"))
        assertTrue(pkt.to.email.contains("@"))
      }
    }
  }

  @Test
  fun inspectionFindsDiscrepancyAndRejectsSameOrUnrelatedTargets() {
    val day1Queue = PacketGenerator.generateDayQueue(content, 1)
    val fraudulent = day1Queue[1]

    val fromTarget = InspectableTarget("from.email", PacketField.FROM, "差出人", fraudulent.from.email)
    val ruleTarget = InspectableTarget("rule.R1", PacketField.RULE_ITEM, "R1", "未承認外部ドメイン禁止")
    val bodyTarget = InspectableTarget("body", PacketField.BODY, "本文", fraudulent.body)

    val matchRes = RuleEngine.inspect(fromTarget, ruleTarget, fraudulent)
    assertTrue(matchRes.matched)

    val sameRes = RuleEngine.inspect(fromTarget, fromTarget, fraudulent)
    assertFalse(sameRes.matched)

    val unrelatedRes = RuleEngine.inspect(fromTarget, bodyTarget, fraudulent)
    assertFalse(unrelatedRes.matched)
  }

  @Test
  fun warningThresholdAppliesPenaltyOnThirdMistake() {
    val packet = PacketGenerator.generateDayQueue(content, 1)[1] // expected BLOCK

    val firstMistake = RuleEngine.evaluateVerdict(packet, Verdict.PASS, dailyWarningsSoFar = 0, config = config)
    assertFalse(firstMistake.isCorrect)
    assertEquals(0, firstMistake.penaltyApplied)
    assertEquals(1, firstMistake.warningIncrement)
    assertNotNull(firstMistake.osAlert)

    val secondMistake = RuleEngine.evaluateVerdict(packet, Verdict.PASS, dailyWarningsSoFar = 1, config = config)
    assertEquals(0, secondMistake.penaltyApplied)
    assertNotNull(secondMistake.osAlert)

    val thirdMistake = RuleEngine.evaluateVerdict(packet, Verdict.PASS, dailyWarningsSoFar = 2, config = config)
    assertEquals(config.penaltyAmount, thirdMistake.penaltyApplied)
    assertEquals(-config.penaltyAmount, thirdMistake.budgetDelta)
    assertTrue(thirdMistake.osAlert!!.isPenalty)
  }

  @Test
  fun quarantineGivesIncidentBonusWhenCorrect() {
    val day3Queue = PacketGenerator.generateDayQueue(content, 3)
    val malware = day3Queue.first { it.expectedVerdict == Verdict.QUARANTINE }

    val eval = RuleEngine.evaluateVerdict(malware, Verdict.QUARANTINE, dailyWarningsSoFar = 0, config = config)
    assertTrue(eval.isCorrect)
    assertEquals(config.passReward + config.quarantineBonus, eval.budgetDelta)
    assertNull(eval.osAlert)
  }

  @Test
  fun bribeAndAuditAvoidanceBranchesWork() {
    val day2Scripted = content.days.first { it.day == 2 }.scriptedPackets.first()
    val bribeAccepted = RuleEngine.evaluateVerdict(day2Scripted, Verdict.PASS, 0, config)
    assertTrue(bribeAccepted.acceptedBribe)
    assertEquals(day2Scripted.bribeOffer!!.bonusAmount, bribeAccepted.budgetDelta)
    assertTrue(bribeAccepted.auditRiskDelta > 0)

    val bribeRejected = RuleEngine.evaluateVerdict(day2Scripted, Verdict.BLOCK, 0, config)
    assertTrue(bribeRejected.isCorrect)
    assertFalse(bribeRejected.acceptedBribe)

    val day4Aan = content.days.first { it.day == 4 }.scriptedPackets.first()
    val aanFollowed = RuleEngine.evaluateVerdict(day4Aan, Verdict.PASS, 0, config)
    assertEquals(1, aanFollowed.aanCooperationDelta)
  }

  @Test
  fun allSixEndingsAreReachable() {
    val bankrupt = EconomyEngine.settleEndOfDay(
      day = 3,
      gameMode = GameMode.CAMPAIGN,
      currentBudget = 1000,
      auditRisk = 0,
      aanCooperationCount = 0,
      totalBribesAccepted = 0,
      processedCount = 3,
      correctCount = 2,
      warningCount = 1,
      incomeTotal = 1200,
      bribeTotal = 0,
      penaltyTotal = 0,
      config = config,
    )
    assertEquals(EndingType.ABOLISHED_BUDGET, bankrupt.ending)

    val audited = EconomyEngine.settleEndOfDay(
      day = 7,
      gameMode = GameMode.CAMPAIGN,
      currentBudget = 8000,
      auditRisk = 80,
      aanCooperationCount = 0,
      totalBribesAccepted = 2,
      processedCount = 5,
      correctCount = 4,
      warningCount = 0,
      incomeTotal = 4800,
      bribeTotal = 0,
      penaltyTotal = 0,
      config = config,
    )
    assertEquals(EndingType.ABOLISHED_AUDIT, audited.ending)

    assertEquals(EndingType.LOYAL_FIREWALL, EconomyEngine.resolveCampaignEnding(auditRisk = 0, aanCooperationCount = 0, bribeAcceptedCount = 0))
    assertEquals(EndingType.WHISTLEBLOWER, EconomyEngine.resolveCampaignEnding(auditRisk = 0, aanCooperationCount = 2, bribeAcceptedCount = 0))
    assertEquals(EndingType.SHADOW_GATEKEEPER, EconomyEngine.resolveCampaignEnding(auditRisk = 10, aanCooperationCount = 3, bribeAcceptedCount = 0))
    assertEquals(EndingType.GRAY_SURVIVOR, EconomyEngine.resolveCampaignEnding(auditRisk = 25, aanCooperationCount = 0, bribeAcceptedCount = 1))
  }

  @Test
  fun defaultShiftDurationIsThreeMinutesAndDefaultFontIsLarge() {
    assertEquals(180, M1TuningConfig().dayDurationSeconds)
    val defaults = AppSettings()
    assertEquals(FontScaleOption.LARGE, defaults.fontScale)
  }

  @Test
  fun fatalVirusBreachAndQuarantineIntelUpgradeWorkCorrectly() {
    val day3Queue = PacketGenerator.generateDayQueue(content, 3)
    val virusPacket = day3Queue.first { it.isFatalVirus }

    // Passing fatal malware triggers fatalVirusBreached
    val breached = RuleEngine.evaluateVerdict(
      packet = virusPacket,
      verdict = Verdict.PASS,
      dailyWarningsSoFar = 0,
      config = config,
    )
    assertFalse(breached.isCorrect)
    assertTrue(breached.fatalVirusBreached)

    // Quarantining registers threat family
    val quarantined = RuleEngine.evaluateVerdict(
      packet = virusPacket,
      verdict = Verdict.QUARANTINE,
      dailyWarningsSoFar = 0,
      config = config,
    )
    assertTrue(quarantined.isCorrect)
    assertNotNull(quarantined.newlyQuarantinedFamily)

    val knownFamily = quarantined.newlyQuarantinedFamily!!
    // Once QUARANTINE_INTEL is owned, BLOCK is accepted for known threat family + FAST_GATE adds +300
    val blockedWithIntel = RuleEngine.evaluateVerdict(
      packet = virusPacket,
      verdict = Verdict.BLOCK,
      dailyWarningsSoFar = 0,
      config = config,
      upgrades = UpgradeState(setOf(UpgradeType.QUARANTINE_INTEL, UpgradeType.FAST_GATE)),
      quarantinedFamilies = setOf(knownFamily),
    )
    assertTrue(blockedWithIntel.isCorrect)
    assertTrue(blockedWithIntel.usedQuarantineIntel)
    assertEquals(config.passReward + RuleEngine.FAST_GATE_BONUS, blockedWithIntel.budgetDelta)
  }

  @Test
  fun combinatorialShiftBatchProducesDiversePackets() {
    val batch1 = PacketGenerator.generateShiftBatch(content, day = 5, batchIndex = 1, count = 12)
    val batch2 = PacketGenerator.generateShiftBatch(content, day = 5, batchIndex = 2, count = 12)
    assertEquals(12, batch1.size)
    assertEquals(12, batch2.size)
    val uniqueSubjects = (batch1 + batch2).map { it.subject }.toSet()
    val uniqueSenders = (batch1 + batch2).map { it.from.email }.toSet()
    assertTrue("Expected high subject variety, got ${uniqueSubjects.size}", uniqueSubjects.size >= 10)
    assertTrue("Expected high sender variety, got ${uniqueSenders.size}", uniqueSenders.size >= 10)
  }
}
