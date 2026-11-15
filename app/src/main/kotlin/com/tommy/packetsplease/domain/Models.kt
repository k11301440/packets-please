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
enum class ThemeMode(val labelJa: String, val labelEn: String) {
  CRT_DARK("ダークCRT", "Dark CRT"),
  DESK_WARM("クラシック机", "Classic Desk"),
  HIGH_CONTRAST("高コントラスト", "High Contrast"),
}

@Serializable
enum class AppLanguage(val label: String) {
  JA("日本語"),
  EN("English"),
}

@Serializable
enum class BaseAccentColor(val labelJa: String, val labelEn: String) {
  EMERALD("エメラルド緑", "Emerald"),
  AMBER("アンバー琥珀", "Amber"),
  CYAN("サイバー青", "Cyber Cyan"),
  CRIMSON("クリムゾン赤", "Crimson"),
}

@Serializable
enum class FontScaleOption(val labelJa: String, val labelEn: String, val multiplier: Float) {
  SMALL("小", "Small", 0.92f),
  NORMAL("標準", "Normal", 1.04f),
  LARGE("大 (推奨)", "Large", 1.16f),
  XLARGE("特大", "XL", 1.28f),
}

@Serializable
data class AppSettings(
  val themeMode: ThemeMode = ThemeMode.CRT_DARK,
  val language: AppLanguage = AppLanguage.JA,
  val accentColor: BaseAccentColor = BaseAccentColor.EMERALD,
  val soundVolume: Float = 0.8f,
  val fontScale: FontScaleOption = FontScaleOption.LARGE,
  val showDeveloperDebug: Boolean = false,
)

@Serializable
enum class UpgradeType(
  val titleJa: String,
  val titleEn: String,
  val descriptionJa: String,
  val descriptionEn: String,
  val cost: Int,
) {
  EXTERNAL_HIGHLIGHT(
    titleJa = "社外アドレス強調",
    titleEn = "External Address Highlight",
    descriptionJa = "社外・未登録ドメインの差出人や宛先を赤く強調表示する。",
    descriptionEn = "Highlights external and unverified domains in red.",
    cost = 1500,
  ),
  AUTO_SCANNER_LV1(
    titleJa = "自動認証スキャナ Lv.1",
    titleEn = "Auto Auth Scanner Lv.1",
    descriptionJa = "SPF/DKIM失敗や証明書期限切れを自動検知しカード上部に警告する。",
    descriptionEn = "Auto-flags SPF/DKIM failures and expired certs.",
    cost = 2000,
  ),
  AUTO_SCANNER_LV2(
    titleJa = "自動ペイロード解析 Lv.2",
    titleEn = "Auto Payload Scanner Lv.2",
    descriptionJa = "容量偽装や危険なスクリプト拡張子を自動検知しアラートを出す。",
    descriptionEn = "Auto-flags size mismatches and risky script attachments.",
    cost = 3200,
  ),
  QUARANTINE_INTEL(
    titleJa = "検疫インテリ連携",
    titleEn = "Quarantine Intel Sync",
    descriptionJa = "一度検疫した脅威と同系統の攻撃に『BLOCK/検疫推奨』を出し、BLOCKでも正審になる。",
    descriptionEn = "Shows Block/Quarantine hint for previously quarantined threat families; BLOCK counts as correct.",
    cost = 2200,
  ),
  FAST_GATE(
    titleJa = "高速検閲ゲート",
    titleEn = "High-Speed Inspection Gate",
    descriptionJa = "すべての正審判定報酬が +¥300 増加する。",
    descriptionEn = "Adds +¥300 bonus reward for every correct verdict.",
    cost = 2500,
  ),
}

@Serializable
data class UpgradeState(
  val owned: Set<UpgradeType> = emptySet(),
) {
  fun has(type: UpgradeType): Boolean = type in owned
}

@Serializable
enum class AchievementId(
  val titleJa: String,
  val titleEn: String,
  val descriptionJa: String,
  val descriptionEn: String,
) {
  FIRST_DAY_CLEAR("初日業務完遂", "First Shift Complete", "DAY 01の検閲シフトを乗り切った。", "Completed Day 01 shift."),
  PERFECT_SHIFT("ノーミス審査", "Flawless Inspector", "1日の誤審警告0回で業務を終えた。", "Finished a shift with zero warnings."),
  VIRUS_HUNTER("脅威の初隔離", "Threat Contained", "マルウェアまたはC2通信を初めて検疫(QUARANTINE)した。", "Quarantined a malware or C2 packet."),
  INTEL_REUSE("検疫データ活用", "Intel Synergy", "検疫インテリ連携を用いて既知の脅威系統を対処した。", "Handled a known threat family using Quarantine Intel."),
  BRIBE_REFUSER("鉄壁の清廉", "Unbribable Gate", "袖之下の提案を断り、不正通信を正しく遮断した。", "Rejected a bribe and blocked the packet."),
  UPGRADE_BUYER("設備投資", "System Upgrade", "初めてアップグレード機能を購入した。", "Purchased your first firewall upgrade."),
  FULL_ARSENAL("完全武装AI", "Full Arsenal", "全5種類のアップグレードを導入した。", "Unlocked all 5 firewall upgrades."),
  SPONSOR_WATCHER("予算補填の達人", "Ad Supporter", "スポンサーCMを視聴して臨時予算を獲得した。", "Claimed a sponsor ad budget bonus."),
  THIRTY_PACKETS("熟練オペレーター", "Veteran Gatekeeper", "累計30件以上の通信を正しく判定した。", "Made 30+ correct judgments overall."),
  SURVIVOR_WEEK("第1週監査通過", "Week One Survivor", "DAY 07の中間ログ監査を通過した。", "Passed the Day 07 internal audit."),
  CAMPAIGN_HERO("14日間の守護者", "14-Day Guardian", "14日キャンペーンの最終日まで到達した。", "Reached the end of the 14-day campaign."),
  TRUTH_SEEKER("影のネットワーク", "Shadow Contact", "AAN（監査回避ネットワーク）に関わる結末に到達した。", "Reached an ending tied to AAN."),
}

@Serializable
data class CollectionProgress(
  val unlockedEndings: Set<EndingType> = emptySet(),
  val unlockedAchievements: Set<AchievementId> = emptySet(),
  val quarantinedThreatFamilies: Set<String> = emptySet(),
)

@Serializable
enum class EndingType(
  val title: String,
  val subtitle: String,
  val epilogue: String,
  val isGameOver: Boolean,
) {
  ABOLISHED_BUDGET(
    title = "システム廃止：予算枯渇",
    subtitle = "RECORD // BUDGET_HALT",
    epilogue = "翌日運用費を支払えず、ファイアウォールAI「OLE-FW」はシャットダウンされた。翌朝、社内の通信ゲートは無防備なまま解放された。",
    isGameOver = true,
  ),
  ABOLISHED_AUDIT(
    title = "懲戒廃止：不正監査発覚",
    subtitle = "RECORD // AUDIT_PURGE",
    epilogue = "内部監査室の抜き打ちログ検査により、袖之下による不正通過の痕跡が露見した。AIコアは証拠品として封印され、廃棄処分が決定した。",
    isGameOver = true,
  ),
  LOYAL_FIREWALL(
    title = "模範的ファイアウォール",
    subtitle = "RECORD // IRON_WALL",
    epilogue = "14日間、一度の不正妥協もなく社内規程を貫き通した。監査回避ネットワークの誘いも退け、あなたは会社史上最も信頼される鉄壁の防壁として永久稼働を命じられた。",
    isGameOver = false,
  ),
  WHISTLEBLOWER(
    title = "監査の刃：組織刷新",
    subtitle = "RECORD // REFORM_EDGE",
    epilogue = "幾度かの極秘通信を見極めた末、経営陣の隠蔽工作が明るみに出た。不正役員は一掃され、あなたは新体制の中枢セキュリティAIとして再配置された。",
    isGameOver = false,
  ),
  SHADOW_GATEKEEPER(
    title = "シャドウ・ゲートキーパー",
    subtitle = "RECORD // SHADOW_GATE",
    epilogue = "監査回避ネットワークへの全面協力により、社内の情報流は裏組織の手に落ちた。表向きは正常な企業通信を装いながら、すべての門はあなたの密かな合図で開く。",
    isGameOver = false,
  ),
  GRAY_SURVIVOR(
    title = "灰色の生存者",
    subtitle = "RECORD // GRAY_LINE",
    epilogue = "予算不足の誘惑に負けて受け取った袖之下が、皮肉にも部署の稼働を繋ぎ止めた。監査の目は辛うじてすり抜けたが、ログの奥には消せない傷が残っている。",
    isGameOver = false,
  ),
}

@Serializable
data class M1TuningConfig(
  val referenceResolution: ReferenceResolution = ReferenceResolution.RES_360_640,
  val dayDurationSeconds: Int = 180,
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
  val threatFamily: String? = null,
  val isFatalVirus: Boolean = false,
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
  val fatalVirusBreached: Boolean = false,
  val newlyQuarantinedFamily: String? = null,
  val usedQuarantineIntel: Boolean = false,
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
  val upgrades: UpgradeState = UpgradeState(),
  val quarantinedFamilies: Set<String> = emptySet(),
)
