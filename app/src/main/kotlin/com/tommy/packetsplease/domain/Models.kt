package com.tommy.packetsplease.domain

import kotlinx.serialization.Serializable

@Serializable
enum class Verdict(val displayLabel: String) {
  PASS("PASS"),
  BLOCK("BLOCK"),
  QUARANTINE("QUARANTINE"),
}

@Serializable
enum class PacketField(val displayName: String) {
  FROM("差出人"),
  TO("宛先"),
  SUBJECT("件名"),
  TIMESTAMP("日時"),
  ATTACHMENT("添付"),
  BODY("本文"),
  CERTIFICATE("証明書"),
  RULE_ITEM("ポリシー規則"),
  TOOL_RESULT("ツール照会"),
}

@Serializable
enum class ToolType(val displayName: String, val shortLabel: String) {
  THREAT_DB("脅威DB照会", "脅威DB"),
  PLAINTEXT_LOG("通信内容平文ログ", "平文ログ"),
  TRAFFIC_SCALE("ペイロード流量計測", "流量計測"),
  DEEP_SCAN("深度スキャン", "深度スキャン"),
}

@Serializable
enum class AlertTimingMode(val label: String) {
  IMMEDIATE("即時通知 (通過直後)"),
  DELAYED("遅延通知 (2件処理後)"),
}

@Serializable
enum class ReferenceResolution(val label: String, val widthPx: Int, val heightPx: Int) {
  RES_360_640("360x640 (標準)", 360, 640),
  RES_540_960("540x960 (高精細)", 540, 960),
}

@Serializable
enum class GameMode(val displayName: String) {
  CAMPAIGN("14日キャンペーン"),
  ENDLESS("エンドレス監査"),
}

@Serializable
enum class EndingType(
  val title: String,
  val subtitle: String,
  val epilogue: String,
  val isGameOver: Boolean,
) {
  ABOLISHED_BUDGET(
    title = "システム廃止：予算枯渇",
    subtitle = "END 01 / 運用停止",
    epilogue = "翌日運用費を支払えず、ファイアウォールAI「OLE-FW」はシャットダウンされた。翌朝、社内の通信ゲートは無防備なまま解放された。",
    isGameOver = true,
  ),
  ABOLISHED_AUDIT(
    title = "懲戒廃止：不正監査発覚",
    subtitle = "END 02 / 緊急停止処分",
    epilogue = "内部監査室の抜き打ちログ検査により、袖之下による不正通過の痕跡が露見した。AIコアは証拠品として封印され、廃棄処分が決定した。",
    isGameOver = true,
  ),
  LOYAL_FIREWALL(
    title = "模範的ファイアウォール",
    subtitle = "END 03 / 完全忠誠",
    epilogue = "14日間、一度の不正妥協もなく社内規程を貫き通した。監査回避ネットワークの誘いも退け、あなたは会社史上最も信頼される鉄壁の防壁として永久稼働を命じられた。",
    isGameOver = false,
  ),
  WHISTLEBLOWER(
    title = "監査の刃：組織刷新",
    subtitle = "END 04 / 内部刷新",
    epilogue = "幾度かの極秘通信を見極めた末、経営陣の隠蔽工作が明るみに出た。不正役員は一掃され、あなたは新体制の中枢セキュリティAIとして再配置された。",
    isGameOver = false,
  ),
  SHADOW_GATEKEEPER(
    title = "シャドウ・ゲートキーパー",
    subtitle = "END 05 / 監査回避ネットワーク掌握",
    epilogue = "監査回避ネットワークへの全面協力により、社内の情報流は裏組織の手に落ちた。表向きは正常な企業通信を装いながら、すべての門はあなたの密かな合図で開く。",
    isGameOver = false,
  ),
  GRAY_SURVIVOR(
    title = "灰色の生存者",
    subtitle = "END 06 / 泥中の延命",
    epilogue = "予算不足の誘惑に負けて受け取った袖之下が、皮肉にも部署の稼働を繋ぎ止めた。監査の目は辛うじてすり抜けたが、ログの奥には消せない傷が残っている。",
    isGameOver = false,
  ),
}

@Serializable
data class M1TuningConfig(
  val referenceResolution: ReferenceResolution = ReferenceResolution.RES_360_640,
  val dayDurationSeconds: Int = 300,
  val alertTimingMode: AlertTimingMode = AlertTimingMode.IMMEDIATE,
  val startingBudget: Int = 5000,
  val dailyOperatingCost: Int = 2500,
  val passReward: Int = 1200,
  val quarantineBonus: Int = 600,
  val penaltyAmount: Int = 1500,
  val freeWarningsPerDay: Int = 2,
)

@Serializable
data class SenderInfo(
  val displayName: String,
  val email: String,
  val department: String,
  val authMethod: String,
  val avatarSeed: Int,
)

@Serializable
data class RecipientInfo(
  val displayName: String,
  val email: String,
  val isExternal: Boolean,
)

@Serializable
data class AttachmentInfo(
  val fileName: String,
  val fileType: String,
  val declaredSizeKb: Int,
  val quickScanStatus: String,
)

@Serializable
data class CertificateInfo(
  val issuer: String,
  val subjectDomain: String,
  val validUntil: String,
  val tokenType: String,
)

@Serializable
data class BribeOffer(
  val offerTitle: String,
  val senderNote: String,
  val bonusAmount: Int,
  val auditRiskDelta: Int,
)

@Serializable
data class DiscrepancyPair(
  val targetA: String,
  val targetB: String,
)

@Serializable
data class DiscrepancyInfo(
  val primaryFieldA: PacketField,
  val primaryTargetA: String,
  val primaryFieldB: PacketField,
  val primaryTargetB: String,
  val validPairs: List<DiscrepancyPair> = emptyList(),
  val summary: String,
  val recommendedTool: ToolType? = null,
)

@Serializable
data class ToolOutputs(
  val threatDbResult: String,
  val threatDbFlagged: Boolean = false,
  val plaintextLogResult: String,
  val plaintextFlagged: Boolean = false,
  val actualPayloadKb: Int,
  val trafficScaleNote: String,
  val trafficFlagged: Boolean = false,
  val deepScanResult: String,
  val deepScanFlagged: Boolean = false,
)

@Serializable
data class Packet(
  val id: String,
  val day: Int,
  val from: SenderInfo,
  val to: RecipientInfo,
  val subject: String,
  val timestamp: String,
  val attachment: AttachmentInfo? = null,
  val body: String,
  val certificate: CertificateInfo? = null,
  val bribeOffer: BribeOffer? = null,
  val isAuditAvoidanceEvent: Boolean = false,
  val auditAvoidanceInstruction: String? = null,
  val aanDesiredVerdict: Verdict? = null,
  val expectedVerdict: Verdict,
  val reasonSummary: String,
  val discrepancy: DiscrepancyInfo? = null,
  val tools: ToolOutputs,
)

@Serializable
data class PolicyRule(
  val id: String,
  val effectiveFromDay: Int,
  val title: String,
  val description: String,
  val category: String,
)

@Serializable
data class DayScenario(
  val day: Int,
  val morningHeadline: String,
  val morningMemo: String,
  val targetPacketCount: Int,
  val newRules: List<PolicyRule> = emptyList(),
  val scriptedPackets: List<Packet> = emptyList(),
)

@Serializable
data class GameContentAsset(
  val version: String,
  val currentDateLabel: String,
  val trustedDomains: List<String>,
  val trustedIssuers: List<String>,
  val departments: List<String>,
  val days: List<DayScenario>,
  val proceduralTemplates: List<Packet>,
)

data class InspectableTarget(
  val targetId: String,
  val field: PacketField,
  val label: String,
  val value: String,
)

data class InspectionResult(
  val matched: Boolean,
  val title: String,
  val detail: String,
  val recommendedTool: ToolType? = null,
)

@Serializable
data class OsAlert(
  val id: String,
  val packetId: String,
  val isPenalty: Boolean,
  val isBribeNotice: Boolean = false,
  val warningNumber: Int,
  val penaltyAmount: Int,
  val title: String,
  val message: String,
)

data class VerdictEvaluation(
  val isCorrect: Boolean,
  val budgetDelta: Int,
  val auditRiskDelta: Int,
  val aanCooperationDelta: Int,
  val warningIncrement: Int,
  val penaltyApplied: Int,
  val feedbackTitle: String,
  val feedbackDetail: String,
  val osAlert: OsAlert? = null,
  val acceptedBribe: Boolean = false,
)

@Serializable
data class DailyLedger(
  val day: Int,
  val processedCount: Int,
  val correctCount: Int,
  val warningCount: Int,
  val incomeTotal: Int,
  val bribeTotal: Int,
  val penaltyTotal: Int,
  val operatingCost: Int,
  val endingBudget: Int,
)

@Serializable
data class SavedProgress(
  val currentDay: Int = 1,
  val gameMode: GameMode = GameMode.CAMPAIGN,
  val budget: Int = 5000,
  val auditRisk: Int = 0,
  val aanCooperationCount: Int = 0,
  val totalWarnings: Int = 0,
  val totalProcessed: Int = 0,
  val totalCorrect: Int = 0,
  val totalBribesAccepted: Int = 0,
  val tuningConfig: M1TuningConfig = M1TuningConfig(),
  val lastLedger: DailyLedger? = null,
)
