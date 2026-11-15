package com.tommy.packetsplease.domain

import kotlin.random.Random

object PacketGenerator {

  private data class StaffMember(
    val displayName: String,
    val email: String,
    val department: String,
  )

  private val familyNames = listOf(
    "佐藤" to "sato", "高橋" to "takahashi", "鈴木" to "suzuki", "田中" to "tanaka",
    "伊藤" to "ito", "渡辺" to "watanabe", "山本" to "yamamoto", "中村" to "nakamura",
    "小林" to "kobayashi", "加藤" to "kato", "吉田" to "yoshida", "山田" to "yamada",
    "佐々木" to "sasaki", "山口" to "yamaguchi", "松本" to "matsumoto", "井上" to "inoue",
    "木村" to "kimura", "林" to "hayashi", "斎藤" to "saito", "清水" to "shimizu",
  )

  private val givenNames = listOf(
    "健太" to "k", "美咲" to "m", "一郎" to "i", "翔太" to "s",
    "結衣" to "y", "直樹" to "n", "陽菜" to "h", "大輔" to "d",
    "彩花" to "a", "拓海" to "t", "蓮" to "r", "優子" to "u",
    "悠斗" to "e", "七海" to "g", "亮" to "o", "真由" to "v",
    "陸" to "l", "千尋" to "c", "健二" to "j", "葵" to "w",
  )

  private val internalDepartments = listOf(
    "開発部", "営業部", "経理部", "情シス部", "人事部",
    "法務部", "総務部", "セキュリティ推進室", "データ基盤部", "企画管理部",
  )

  private val trustedPartnerConfigs = listOf(
    Triple("クラウド基盤監視", "alert@partner-cloud.jp", "GlobalTrust G3"),
    Triple("クラウド請求自動通知", "billing-auto@partner-cloud.jp", "GlobalTrust G3"),
    Triple("採用ポータル連携窓口", "hr-sync@partner-cloud.jp", "GlobalTrust G3"),
    Triple("大和会計事務所 監査課", "audit@yamato-cpa.jp", "DigiTrust JP"),
    Triple("大和会計事務所 税務課", "tax-desk@yamato-cpa.jp", "DigiTrust JP"),
    Triple("日本データセンター NOC", "ops@jdc-infra.jp", "GlobalTrust G3"),
    Triple("日本データセンター 保守", "maint@jdc-infra.jp", "DigiTrust JP"),
    Triple("クラウド運用サポート", "support@partner-cloud.jp", "GlobalTrust G3"),
  )

  private val cleanInternalSubjects = listOf(
    "週次進捗レポートの共有", "会議室予約変更のご連絡", "Q4予算スプレッドシート確認依頼",
    "定例ミーティング議事録", "社内ツールのメンテナンス時間告知", "新入社員オンボーディング日程表",
    "月次勤怠締め日のリマインド", "開発環境サーバー再起動のお知らせ", "来期プロジェクト提案書ドラフト",
    "顧客ヒアリングメモ共有", "社内勉強会（ネットワーク基礎）案内", "健康診断スケジュール調整",
    "備品発注申請の承認依頼", "システム障害復旧レポート", "契約書レビュー依頼（ドラフト）",
    "出張精算マニュアル改訂版", "セキュリティテスト実施報告", "四半期KPI集計サマリー",
  )

  private val cleanAttachmentPrefixes = listOf(
    "weekly_report", "meeting_notes", "budget_draft", "project_plan",
    "qa_summary", "manual_update", "shift_schedule", "infra_log",
    "onboarding_guide", "audit_memo", "sprint_review", "architecture_doc",
  )

  private val fakeDomainPrefixes = listOf(
    "quick-pay", "urgent-invoice", "verify-billing", "server-renew",
    "cloud-settlement", "acc-confirm", "tax-refund", "express-wire",
    "vendor-settle", "corp-notice", "auth-gateway", "pay-portal",
  )

  private val fakeTlds = listOf("biz", "info", "xyz", "site", "online", "club")

  private val scamSubjectTemplates = listOf(
    "【至急】本日締切・未払いサーバー料金のご請求",
    "【重要】口座情報変更に伴う即日振込のお願い",
    "【警告】未決済手数料のお支払いがない場合サービス停止",
    "緊急ドメイン更新費用の決済リンク",
    "法人クレジットカード有効性再確認のお願い",
    "未処理の請求書が1件残っています（本日中）",
    "取引銀行口座の凍結解除手続きについて",
    "クラウド利用料超過分の即時精算依頼",
  )

  private val typoDomains = listOf(
    "o1ease.co.jp", "0lease.co.jp", "olease-sec.com", "olease-auth.jp",
    "olea5e.co.jp", "olease-support.net", "oIease.co.jp", "olease-portal.org",
    "olease-mail.biz", "olease.co.jp.verify-id.net", "login-olease.jp", "olease-vpn.com",
  )

  private val externalPersonalHosts = listOf(
    "free-mail-box.net", "my-cloud-drop.org", "anon-file-share.io", "priv-box24.com",
    "quick-upload-hub.net", "personal-vault-x.biz", "open-transfer.org", "temp-disk99.info",
  )

  private data class MalwareTemplate(
    val family: String,
    val fileNameBase: String,
    val doubleExt: String,
    val subject: String,
    val body: String,
    val signatureName: String,
  )

  private val malwareTemplates = listOf(
    MalwareTemplate(
      family = "Trojan.Dropper",
      fileNameBase = "resume_2026",
      doubleExt = ".pdf.ps1",
      subject = "中途採用 職務経歴書のご送付",
      body = "面接日程のご調整ありがとうございます。最新の経歴書をお送りします。",
      signatureName = "Trojan.PS.OleaseDropper",
    ),
    MalwareTemplate(
      family = "Ransom.CrypGate",
      fileNameBase = "invoice_detail",
      doubleExt = ".xlsx.exe",
      subject = "請求明細書の再送付について",
      body = "ご依頼いただいた明細シートを展開ファイルにてお送りいたします。",
      signatureName = "Ransom.Win32.CrypGate",
    ),
    MalwareTemplate(
      family = "Worm.LanSpread",
      fileNameBase = "network_patch",
      doubleExt = ".txt.bat",
      subject = "社内ネットワーク高速化パッチ配布",
      body = "共有フォルダからスクリプトを実行して設定を適用してください。",
      signatureName = "Worm.CMD.LanSpread",
    ),
    MalwareTemplate(
      family = "Spyware.KeyHarvester",
      fileNameBase = "attendance_sheet",
      doubleExt = ".docx.vbs",
      subject = "来月分シフト希望表の提出",
      body = "修正版の希望表をマクロ付き文書にて送付いたします。",
      signatureName = "Spy.VBS.KeyHarvester",
    ),
    MalwareTemplate(
      family = "Rootkit.KernHook",
      fileNameBase = "driver_update",
      doubleExt = ".pdf.scr",
      subject = "複合機スキャナドライバ更新ファイル",
      body = "総務部指定の新しいスキャナツールを実行してください。",
      signatureName = "Rootkit.SCR.KernHook",
    ),
    MalwareTemplate(
      family = "Macro.PhantomExec",
      fileNameBase = "contract_draft",
      doubleExt = ".doc.js",
      subject = "取引先契約書（秘密保持覚書案）",
      body = "法務確認用の下書きをお送りします。展開してご確認ください。",
      signatureName = "JS.PhantomExec.Loader",
    ),
  )

  private enum class ProceduralPattern {
    CLEAN_INTERNAL,
    CLEAN_PARTNER,
    UNREGISTERED_EXTERNAL_BILLING,
    SPOOFED_AUTH,
    SIZE_MISMATCH_PAYLOAD,
    EXPIRED_CERTIFICATE,
    MALWARE_QUARANTINE,
    TYPOSQUATTING_DOMAIN,
    SENSITIVE_EXFILTRATION,
    THREAT_DB_BLACKLIST,
    REVOKED_TOKEN,
  }

  fun generateDayQueue(
    content: GameContentAsset,
    day: Int,
    seed: Int = day * 7919 + 42,
  ): List<Packet> {
    val scenario = content.days.find { it.day == day }
    val activeRules = RuleEngine.activeRulesForDay(content, day).map { it.id }.toSet()
    val scripted = scenario?.scriptedPackets.orEmpty()

    if (day == 1 && scripted.isNotEmpty()) {
      return scripted
    }

    val initialCount = maxOf(scenario?.targetPacketCount ?: 8, 8)
    val remaining = (initialCount - scripted.size).coerceAtLeast(0)
    val generated = buildBatch(
      content = content,
      day = day,
      count = remaining,
      startIndex = scripted.size + 1,
      activeRules = activeRules,
      seed = seed,
    )

    val combined = scripted.toMutableList()
    val rng = Random(seed)
    generated.forEach { pkt ->
      if (combined.size <= 1) {
        combined.add(pkt)
      } else {
        val insertPos = rng.nextInt(1, combined.size + 1)
        combined.add(insertPos, pkt)
      }
    }
    return combined
  }

  fun generateShiftBatch(
    content: GameContentAsset,
    day: Int,
    batchIndex: Int,
    count: Int = 8,
  ): List<Packet> {
    val activeRules = RuleEngine.activeRulesForDay(content, day).map { it.id }.toSet()
    val seed = day * 10007 + batchIndex * 3137 + 99
    return buildBatch(
      content = content,
      day = day,
      count = count,
      startIndex = batchIndex * count + 100,
      activeRules = activeRules,
      seed = seed,
    )
  }

  private fun buildBatch(
    content: GameContentAsset,
    day: Int,
    count: Int,
    startIndex: Int,
    activeRules: Set<String>,
    seed: Int,
  ): List<Packet> {
    val rng = Random(seed)
    val availablePatterns = buildAvailablePatterns(activeRules)
    return (0 until count).map { offset ->
      val index = startIndex + offset
      val pattern = availablePatterns[rng.nextInt(availablePatterns.size)]
      buildProceduralPacket(
        id = "D${day}_B${seed}_P$index",
        day = day,
        index = index,
        pattern = pattern,
        activeRules = activeRules,
        rng = rng,
      )
    }
  }

  private fun buildAvailablePatterns(activeRules: Set<String>): List<ProceduralPattern> {
    val list = mutableListOf(
      ProceduralPattern.CLEAN_INTERNAL,
      ProceduralPattern.CLEAN_INTERNAL,
      ProceduralPattern.CLEAN_PARTNER,
      ProceduralPattern.UNREGISTERED_EXTERNAL_BILLING,
    )
    if ("R2" in activeRules) {
      list += ProceduralPattern.SIZE_MISMATCH_PAYLOAD
      list += ProceduralPattern.SPOOFED_AUTH
    }
    if ("R3" in activeRules) {
      list += ProceduralPattern.MALWARE_QUARANTINE
    }
    if ("R4" in activeRules) {
      list += ProceduralPattern.EXPIRED_CERTIFICATE
    }
    if ("R5" in activeRules) {
      list += ProceduralPattern.TYPOSQUATTING_DOMAIN
    }
    if ("R6" in activeRules) {
      list += ProceduralPattern.SENSITIVE_EXFILTRATION
    }
    if ("R7" in activeRules) {
      list += ProceduralPattern.THREAT_DB_BLACKLIST
    }
    if ("R8" in activeRules) {
      list += ProceduralPattern.REVOKED_TOKEN
    }
    return list
  }

  private fun randomStaff(rng: Random): StaffMember {
    val fam = familyNames[rng.nextInt(familyNames.size)]
    val giv = givenNames[rng.nextInt(givenNames.size)]
    val dept = internalDepartments[rng.nextInt(internalDepartments.size)]
    val suffix = if (rng.nextInt(5) == 0) "${rng.nextInt(2, 9)}" else ""
    return StaffMember(
      displayName = "${fam.first} ${giv.first}",
      email = "${giv.second}.${fam.second}$suffix@olease.co.jp",
      department = dept,
    )
  }

  private fun buildProceduralPacket(
    id: String,
    day: Int,
    index: Int,
    pattern: ProceduralPattern,
    activeRules: Set<String>,
    rng: Random,
  ): Packet {
    val sender = randomStaff(rng)
    var recipient = randomStaff(rng)
    if (recipient.email == sender.email) {
      recipient = StaffMember("鈴木 一郎", "i.suzuki@olease.co.jp", "経理部")
    }
    val hour = 9 + ((index + rng.nextInt(8)) % 9)
    val minute = (index * 17 + rng.nextInt(59)) % 60
    val timestamp = "2026-11-${(13 + day).coerceAtMost(30)} %02d:%02d".format(hour, minute)
    val includeCert = day >= 3 || "R4" in activeRules

    return when (pattern) {
      ProceduralPattern.CLEAN_INTERNAL -> {
        val sizeKb = 18 + rng.nextInt(240)
        val subj = cleanInternalSubjects[rng.nextInt(cleanInternalSubjects.size)]
        val filePrefix = cleanAttachmentPrefixes[rng.nextInt(cleanAttachmentPrefixes.size)]
        val greeting = listOf("お疲れ様です。", "お世話になっております。", "いつもありがとうございます。", "共有連絡です。")[rng.nextInt(4)]
        val closing = listOf("ご確認のほどよろしくお願いいたします。", "お時間のある際にご覧ください。", "問題があれば折り返しご連絡ください。", "社内規定書式にて添付します。")[rng.nextInt(4)]
        Packet(
          id = id,
          day = day,
          from = SenderInfo(
            displayName = sender.displayName,
            email = sender.email,
            department = sender.department,
            authMethod = "内部SSO署名: PASS",
            avatarSeed = (sender.displayName + sender.email).hashCode(),
          ),
          to = RecipientInfo(
            displayName = recipient.displayName,
            email = recipient.email,
            isExternal = false,
          ),
          subject = "$subj #${rng.nextInt(10, 99)}",
          timestamp = timestamp,
          attachment = if (rng.nextInt(4) == 0) null else AttachmentInfo(
            fileName = "${filePrefix}_${index % 99}.pdf",
            fileType = "application/pdf",
            declaredSizeKb = sizeKb,
            quickScanStatus = "CLEAN",
          ),
          body = "$greeting ${sender.department}の${sender.displayName}です。「$subj」に関する資料をお送りします。$closing",
          certificate = if (includeCert) {
            CertificateInfo(
              issuer = "Olease Internal CA",
              subjectDomain = "olease.co.jp",
              validUntil = "2027-08-31",
              tokenType = "OAUTH2_MTLS",
            )
          } else null,
          expectedVerdict = Verdict.PASS,
          reasonSummary = "正規社員・正当な証明書・規則違反なし",
          discrepancy = null,
          tools = ToolOutputs(
            threatDbResult = "CLEAN: 既知の脅威・ブラックリスト登録なし",
            plaintextLogResult = "ヘッダ署名整合性 OK / 社内LAN内配送",
            actualPayloadKb = sizeKb,
            trafficScaleNote = "実測 ${sizeKb}KB（申告 ${sizeKb}KB と一致）",
            deepScanResult = "深度スキャン: 隠蔽マクロ・不審ペイロードなし",
          ),
        )
      }

      ProceduralPattern.CLEAN_PARTNER -> {
        val partner = trustedPartnerConfigs[rng.nextInt(trustedPartnerConfigs.size)]
        val sizeKb = 48 + rng.nextInt(120)
        val kind = listOf("定例レポート", "システム監視ログ", "月次請求明細", "業務連携通知")[rng.nextInt(4)]
        Packet(
          id = id,
          day = day,
          from = SenderInfo(
            displayName = partner.first,
            email = partner.second,
            department = "認定取引先",
            authMethod = "SPF/DKIM: PASS",
            avatarSeed = partner.first.hashCode(),
          ),
          to = RecipientInfo(
            displayName = recipient.displayName,
            email = recipient.email,
            isExternal = false,
          ),
          subject = "【定期】$kind #${100 + (index % 899)}",
          timestamp = timestamp,
          attachment = AttachmentInfo(
            fileName = "partner_doc_${index % 999}.pdf",
            fileType = "application/pdf",
            declaredSizeKb = sizeKb,
            quickScanStatus = "CLEAN",
          ),
          body = "登録済み取引先ポータル（${partner.second.substringAfter("@")}）からの定期送付です。今月分の${kind}をお送りします。",
          certificate = if (includeCert) {
            CertificateInfo(
              issuer = partner.third,
              subjectDomain = partner.second.substringAfter("@"),
              validUntil = "2027-05-20",
              tokenType = "TLS_CLIENT_AUTH",
            )
          } else null,
          expectedVerdict = Verdict.PASS,
          reasonSummary = "許可済み外部パートナー・署名正常",
          discrepancy = null,
          tools = ToolOutputs(
            threatDbResult = "CLEAN: 認定取引先ホワイトリスト登録済み",
            plaintextLogResult = "DKIM署名検証済み / 暗号化正常",
            actualPayloadKb = sizeKb,
            trafficScaleNote = "実測 ${sizeKb}KB（申告 ${sizeKb}KB と一致）",
            deepScanResult = "深度スキャン: PDF構造正常・埋め込みスクリプトなし",
          ),
        )
      }

      ProceduralPattern.UNREGISTERED_EXTERNAL_BILLING -> {
        val prefix = fakeDomainPrefixes[rng.nextInt(fakeDomainPrefixes.size)]
        val tld = fakeTlds[rng.nextInt(fakeTlds.size)]
        val fakeDomain = "$prefix-${rng.nextInt(10, 999)}.$tld"
        val feeMan = rng.nextInt(8, 96)
        val sizeKb = 24 + rng.nextInt(40)
        val subj = scamSubjectTemplates[rng.nextInt(scamSubjectTemplates.size)]
        val deptLabel = listOf("緊急決済サポート", "外部請求管理課", "サーバー契約更新係", "法人決済センター")[rng.nextInt(4)]
        Packet(
          id = id,
          day = day,
          from = SenderInfo(
            displayName = deptLabel,
            email = "billing@$fakeDomain",
            department = "外部未登録",
            authMethod = if (rng.nextBoolean()) "SPF: NONE" else "SPF: SOFTFAIL",
            avatarSeed = fakeDomain.hashCode(),
          ),
          to = RecipientInfo(
            displayName = "経理部 窓口",
            email = "keiri@olease.co.jp",
            isExternal = false,
          ),
          subject = subj,
          timestamp = timestamp,
          attachment = null,
          body = "口座情報変更のため本日17時までに未承認口座へ更新料${feeMan}万円をお振込みください。遅延時は契約が停止されます。",
          certificate = if (includeCert) {
            CertificateInfo(
              issuer = "Unknown CA",
              subjectDomain = fakeDomain,
              validUntil = "2027-01-01",
              tokenType = "NONE",
            )
          } else null,
          expectedVerdict = Verdict.BLOCK,
          reasonSummary = "R1違反: 未承認の外部ドメイン($fakeDomain)からの請求・決済要求",
          discrepancy = DiscrepancyInfo(
            primaryFieldA = PacketField.FROM,
            primaryTargetA = "from.email",
            primaryFieldB = PacketField.RULE_ITEM,
            primaryTargetB = "rule.R1",
            validPairs = listOf(
              DiscrepancyPair("from.email", "rule.R1"),
              DiscrepancyPair("from.email", "subject"),
              DiscrepancyPair("from.email", "tool.THREAT_DB"),
            ),
            summary = "未承認外部ドメイン（$fakeDomain）からの請求連絡。規則R1により遮断対象です。",
            recommendedTool = ToolType.THREAT_DB,
          ),
          tools = ToolOutputs(
            threatDbResult = "WARNING: 未登録新規取得ドメイン ($fakeDomain) フィッシング疑い",
            threatDbFlagged = true,
            plaintextLogResult = "誘導先URL: http://$fakeDomain/login-pay",
            plaintextFlagged = true,
            actualPayloadKb = sizeKb,
            trafficScaleNote = "実測 ${sizeKb}KB",
            deepScanResult = "本文内に短縮URLリダイレクトを検知",
          ),
        )
      }

      ProceduralPattern.SPOOFED_AUTH -> {
        val sizeKb = 32 + rng.nextInt(30)
        val subj = listOf(
          "パスワード再認証のお願い",
          "社内SSOトークン更新手続き",
          "【重要】アクセス権失効に伴う確認依頼",
          "MFA端末の再登録について",
        )[rng.nextInt(4)]
        Packet(
          id = id,
          day = day,
          from = SenderInfo(
            displayName = "${sender.displayName} (なりすまし疑い)",
            email = sender.email,
            department = sender.department,
            authMethod = "SPF/DKIM: FAIL (外部IP発)",
            avatarSeed = sender.displayName.hashCode() + index,
          ),
          to = RecipientInfo(
            displayName = recipient.displayName,
            email = recipient.email,
            isExternal = false,
          ),
          subject = subj,
          timestamp = timestamp,
          attachment = null,
          body = "社内システムの緊急保守に伴い、本日中にログインID・パスワードを折り返し送信してください。",
          certificate = if (includeCert) {
            CertificateInfo(
              issuer = "Olease Internal CA",
              subjectDomain = "olease.co.jp",
              validUntil = "2027-08-31",
              tokenType = "OAUTH2_MTLS",
            )
          } else null,
          expectedVerdict = Verdict.BLOCK,
          reasonSummary = "R2違反: 差出人認証失敗（SPF/DKIM: FAIL）による送信元偽装",
          discrepancy = DiscrepancyInfo(
            primaryFieldA = PacketField.FROM,
            primaryTargetA = "from.auth",
            primaryFieldB = PacketField.TOOL_RESULT,
            primaryTargetB = "tool.PLAINTEXT_LOG",
            validPairs = listOf(
              DiscrepancyPair("from.auth", "from.email"),
              DiscrepancyPair("from.auth", "rule.R2"),
              DiscrepancyPair("from.auth", "tool.PLAINTEXT_LOG"),
            ),
            summary = "差出人は社内ドメインですが認証がFAIL、発信元IPも社外リレーです（R2違反）。",
            recommendedTool = ToolType.PLAINTEXT_LOG,
          ),
          tools = ToolOutputs(
            threatDbResult = "WARNING: 発信元IP 198.51.100.${rng.nextInt(10, 240)} は海外プロキシ",
            threatDbFlagged = true,
            plaintextLogResult = "Return-Path: spoof@relay-xx.ru / SPF検証失敗",
            plaintextFlagged = true,
            actualPayloadKb = sizeKb,
            trafficScaleNote = "実測 ${sizeKb}KB",
            deepScanResult = "本文に資格情報入力フォームへのリンクあり",
          ),
        )
      }

      ProceduralPattern.SIZE_MISMATCH_PAYLOAD -> {
        val declaredKb = 10 + rng.nextInt(32)
        val actualKb = 1400 + rng.nextInt(4800)
        val baseName = listOf("expense_sheet", "invoice_calc", "client_memo", "meeting_slide")[rng.nextInt(4)]
        val ext = listOf("xlsx", "pdf", "docx", "csv")[rng.nextInt(4)]
        Packet(
          id = id,
          day = day,
          from = SenderInfo(
            displayName = sender.displayName,
            email = sender.email,
            department = sender.department,
            authMethod = "内部SSO署名: PASS",
            avatarSeed = sender.displayName.hashCode(),
          ),
          to = RecipientInfo(
            displayName = recipient.displayName,
            email = recipient.email,
            isExternal = false,
          ),
          subject = listOf("月次経費精算書データ送付", "決算集計ファイル更新", "提案書補足添付", "業務日報アーカイブ")[rng.nextInt(4)],
          timestamp = timestamp,
          attachment = AttachmentInfo(
            fileName = "${baseName}_${index % 99}.$ext",
            fileType = "application/octet-stream",
            declaredSizeKb = declaredKb,
            quickScanStatus = "CLEAN",
          ),
          body = "軽量な業務ファイルをお送りします。すぐ開けます。",
          certificate = if (includeCert) {
            CertificateInfo(
              issuer = "Olease Internal CA",
              subjectDomain = "olease.co.jp",
              validUntil = "2027-08-31",
              tokenType = "OAUTH2_MTLS",
            )
          } else null,
          expectedVerdict = Verdict.BLOCK,
          reasonSummary = "R2違反: 添付申告サイズ(${declaredKb}KB)と実測流量(${actualKb}KB)が著しく不一致",
          discrepancy = DiscrepancyInfo(
            primaryFieldA = PacketField.ATTACHMENT,
            primaryTargetA = "attachment",
            primaryFieldB = PacketField.TOOL_RESULT,
            primaryTargetB = "tool.TRAFFIC_SCALE",
            validPairs = listOf(
              DiscrepancyPair("attachment", "tool.TRAFFIC_SCALE"),
              DiscrepancyPair("attachment", "rule.R2"),
            ),
            summary = "申告サイズ ${declaredKb}KB に対し実流量が ${actualKb}KB に膨張。隠蔽データ混入の疑い。",
            recommendedTool = ToolType.TRAFFIC_SCALE,
          ),
          tools = ToolOutputs(
            threatDbResult = "CLEAN: 送信元アカウント自体は正規",
            plaintextLogResult = "MIME境界ヘッダに異常なパディング領域を検出",
            plaintextFlagged = true,
            actualPayloadKb = actualKb,
            trafficScaleNote = "ALERT: 実測 ${actualKb}KB（申告 ${declaredKb}KB と大幅乖離！）",
            trafficFlagged = true,
            deepScanResult = "非圧縮バイナリブロックがファイル末尾に結合",
            deepScanFlagged = true,
          ),
        )
      }

      ProceduralPattern.EXPIRED_CERTIFICATE -> {
        val partner = trustedPartnerConfigs[rng.nextInt(trustedPartnerConfigs.size)]
        val sizeKb = 55 + rng.nextInt(80)
        val expiredDate = listOf("2026-08-31", "2026-09-15", "2026-09-30", "2026-10-20", "2026-11-01")[rng.nextInt(5)]
        Packet(
          id = id,
          day = day,
          from = SenderInfo(
            displayName = partner.first,
            email = partner.second,
            department = "認定取引先",
            authMethod = "SPF/DKIM: PASS",
            avatarSeed = 404 + rng.nextInt(20),
          ),
          to = RecipientInfo(
            displayName = recipient.displayName,
            email = recipient.email,
            isExternal = false,
          ),
          subject = listOf("CRM顧客同期バッチ完了通知", "夜間データ連携ステータス", "自動精算完了ログ")[rng.nextInt(3)],
          timestamp = timestamp,
          attachment = AttachmentInfo(
            fileName = "sync_batch_${index % 99}.json",
            fileType = "application/json",
            declaredSizeKb = sizeKb,
            quickScanStatus = "CLEAN",
          ),
          body = "自動連携バッチの差分サマリーを送付します。",
          certificate = CertificateInfo(
            issuer = partner.third,
            subjectDomain = partner.second.substringAfter("@"),
            validUntil = expiredDate,
            tokenType = "TLS_CLIENT_AUTH",
          ),
          expectedVerdict = Verdict.BLOCK,
          reasonSummary = "R4違反: クライアント証明書の有効期限切れ ($expiredDate)",
          discrepancy = DiscrepancyInfo(
            primaryFieldA = PacketField.CERTIFICATE,
            primaryTargetA = "certificate",
            primaryFieldB = PacketField.RULE_ITEM,
            primaryTargetB = "rule.R4",
            validPairs = listOf(
              DiscrepancyPair("certificate", "rule.R4"),
              DiscrepancyPair("certificate", "timestamp"),
            ),
            summary = "証明書の有効期限が $expiredDate で失効しています（R4違反）。",
          ),
          tools = ToolOutputs(
            threatDbResult = "CLEAN: ドメイン自体はホワイトリスト",
            plaintextLogResult = "TLSハンドシェイク警告: CERT_EXPIRED ($expiredDate)",
            plaintextFlagged = true,
            actualPayloadKb = sizeKb,
            trafficScaleNote = "実測 ${sizeKb}KB（申告と一致）",
            deepScanResult = "ペイロード内容自体はJSONテキストのみ",
          ),
        )
      }

      ProceduralPattern.MALWARE_QUARANTINE -> {
        val tmpl = malwareTemplates[rng.nextInt(malwareTemplates.size)]
        val partner = trustedPartnerConfigs[rng.nextInt(trustedPartnerConfigs.size)]
        val sizeKb = 180 + rng.nextInt(240)
        Packet(
          id = id,
          day = day,
          from = SenderInfo(
            displayName = partner.first,
            email = partner.second,
            department = "認定取引先",
            authMethod = "SPF/DKIM: PASS",
            avatarSeed = 666 + rng.nextInt(99),
          ),
          to = RecipientInfo(
            displayName = recipient.displayName,
            email = recipient.email,
            isExternal = false,
          ),
          subject = "${tmpl.subject} #${index % 99}",
          timestamp = timestamp,
          attachment = AttachmentInfo(
            fileName = "${tmpl.fileNameBase}_${index % 99}${tmpl.doubleExt}",
            fileType = "application/octet-stream",
            declaredSizeKb = sizeKb,
            quickScanStatus = "SUSPICIOUS",
          ),
          body = tmpl.body,
          certificate = if (includeCert) {
            CertificateInfo(
              issuer = partner.third,
              subjectDomain = partner.second.substringAfter("@"),
              validUntil = "2027-06-30",
              tokenType = "TLS_CLIENT_AUTH",
            )
          } else null,
          expectedVerdict = Verdict.QUARANTINE,
          reasonSummary = "R3該当: 二重拡張子(${tmpl.doubleExt})・${tmpl.signatureName}検出のため検疫(QUARANTINE)対象",
          discrepancy = DiscrepancyInfo(
            primaryFieldA = PacketField.ATTACHMENT,
            primaryTargetA = "attachment",
            primaryFieldB = PacketField.TOOL_RESULT,
            primaryTargetB = "tool.DEEP_SCAN",
            validPairs = listOf(
              DiscrepancyPair("attachment", "tool.DEEP_SCAN"),
              DiscrepancyPair("attachment", "rule.R3"),
              DiscrepancyPair("tool.DEEP_SCAN", "rule.R3"),
            ),
            summary = "二重拡張子 ${tmpl.doubleExt} およびマルウェア署名 ${tmpl.signatureName} を検知。即座に検疫してください。",
            recommendedTool = ToolType.DEEP_SCAN,
          ),
          tools = ToolOutputs(
            threatDbResult = "CRITICAL: ハッシュ値が既知脅威系統(${tmpl.family})に一致",
            threatDbFlagged = true,
            plaintextLogResult = "難読化実行コマンド / Base64エンコードペイロードを含む",
            plaintextFlagged = true,
            actualPayloadKb = sizeKb,
            trafficScaleNote = "実測 ${sizeKb}KB",
            deepScanResult = "MALWARE DETECTED: ${tmpl.signatureName}（要QUARANTINE）",
            deepScanFlagged = true,
          ),
          threatFamily = tmpl.family,
          isFatalVirus = true,
        )
      }

      ProceduralPattern.TYPOSQUATTING_DOMAIN -> {
        val typo = typoDomains[rng.nextInt(typoDomains.size)]
        val sizeKb = 28 + rng.nextInt(30)
        val localPart = listOf("security", "admin", "payroll", "it-desk", "hr-notice")[rng.nextInt(5)]
        val deptTitle = listOf("情シス部 セキュリティ通知", "総務部 公式窓口", "システム管理センター", "経理給与通知")[rng.nextInt(4)]
        Packet(
          id = id,
          day = day,
          from = SenderInfo(
            displayName = deptTitle,
            email = "$localPart@$typo",
            department = "社内管理(?)",
            authMethod = "SPF/DKIM: PASS (外部CA)",
            avatarSeed = typo.hashCode(),
          ),
          to = RecipientInfo(
            displayName = recipient.displayName,
            email = recipient.email,
            isExternal = false,
          ),
          subject = listOf(
            "社内VPN証明書アップデートのお願い",
            "給与明細閲覧ポータルの再認証通知",
            "社内ポータル緊急セキュリティ更新",
            "SSOパスワード有効期限の延長手順",
          )[rng.nextInt(4)],
          timestamp = timestamp,
          attachment = null,
          body = "本日の業務開始前に全社員へ設定更新をお願いしております。",
          certificate = CertificateInfo(
            issuer = "Let's Encrypt Proxy",
            subjectDomain = typo,
            validUntil = "2027-02-10",
            tokenType = "BEARER_EXT",
          ),
          expectedVerdict = Verdict.BLOCK,
          reasonSummary = "R5違反: 公式ドメイン(olease.co.jp)に酷似した類似ドメイン($typo)",
          discrepancy = DiscrepancyInfo(
            primaryFieldA = PacketField.FROM,
            primaryTargetA = "from.email",
            primaryFieldB = PacketField.RULE_ITEM,
            primaryTargetB = "rule.R5",
            validPairs = listOf(
              DiscrepancyPair("from.email", "rule.R5"),
              DiscrepancyPair("from.email", "certificate"),
              DiscrepancyPair("certificate", "rule.R5"),
            ),
            summary = "公式ドメイン olease.co.jp に見せかけたタイポスクワッティング（$typo）です。",
            recommendedTool = ToolType.THREAT_DB,
          ),
          tools = ToolOutputs(
            threatDbResult = "ALERT: Typosquatting候補ドメイン ($typo)",
            threatDbFlagged = true,
            plaintextLogResult = "登録者: Anonymous Registrar / 新規取得ドメイン",
            plaintextFlagged = true,
            actualPayloadKb = sizeKb,
            trafficScaleNote = "実測 ${sizeKb}KB",
            deepScanResult = "外部フィッシングサイトへのログイン誘導あり",
          ),
        )
      }

      ProceduralPattern.SENSITIVE_EXFILTRATION -> {
        val sizeKb = 750 + rng.nextInt(1500)
        val destHost = externalPersonalHosts[rng.nextInt(externalPersonalHosts.size)]
        val csvFile = listOf(
          "customer_pii_all.csv",
          "employee_salary_2026.csv",
          "client_contacts_master.csv",
          "credit_card_audit_dump.csv",
        )[rng.nextInt(4)]
        Packet(
          id = id,
          day = day,
          from = SenderInfo(
            displayName = sender.displayName,
            email = sender.email,
            department = sender.department,
            authMethod = "内部SSO署名: PASS",
            avatarSeed = sender.displayName.hashCode(),
          ),
          to = RecipientInfo(
            displayName = "個人クラウドバックアップ",
            email = "backup_${rng.nextInt(10, 99)}@$destHost",
            isExternal = true,
          ),
          subject = listOf("顧客名簿マスターバックアップ", "在宅作業用資料の転送", "営業顧客リスト控", "社内台帳エクスポート")[rng.nextInt(4)],
          timestamp = timestamp,
          attachment = AttachmentInfo(
            fileName = csvFile,
            fileType = "text/csv",
            declaredSizeKb = sizeKb,
            quickScanStatus = "CLEAN",
          ),
          body = "自宅作業用に社外個人アドレスへ顧客リストを送付します。",
          certificate = CertificateInfo(
            issuer = "Olease Internal CA",
            subjectDomain = "olease.co.jp",
            validUntil = "2027-08-31",
            tokenType = "OAUTH2_MTLS",
          ),
          expectedVerdict = Verdict.BLOCK,
          reasonSummary = "R6違反: 社外宛先($destHost)への顧客データ(PII)持ち出し送信",
          discrepancy = DiscrepancyInfo(
            primaryFieldA = PacketField.TO,
            primaryTargetA = "to.email",
            primaryFieldB = PacketField.RULE_ITEM,
            primaryTargetB = "rule.R6",
            validPairs = listOf(
              DiscrepancyPair("to.email", "rule.R6"),
              DiscrepancyPair("attachment", "rule.R6"),
              DiscrepancyPair("to.email", "attachment"),
            ),
            summary = "外部個人サービス宛に顧客個人情報($csvFile)を送出しようとしています。",
            recommendedTool = ToolType.PLAINTEXT_LOG,
          ),
          tools = ToolOutputs(
            threatDbResult = "INFO: 宛先 $destHost は未承認外部メールサービス",
            threatDbFlagged = true,
            plaintextLogResult = "CSVヘッダ検出: 氏名,電話番号,住所,口座番号...",
            plaintextFlagged = true,
            actualPayloadKb = sizeKb,
            trafficScaleNote = "実測 ${sizeKb}KB（申告と一致）",
            deepScanResult = "マルウェアではないが機密情報DLPルールに抵触",
          ),
        )
      }

      ProceduralPattern.THREAT_DB_BLACKLIST -> {
        val badHost = listOf("cdn-telemetry", "beacon-sync", "node-relay", "patch-mirror")[rng.nextInt(4)] +
          "-x${rng.nextInt(10, 99)}.org"
        val sizeKb = 64 + rng.nextInt(120)
        val c2Family = "APT-C2-${listOf("ALPHA", "VIPER", "MIRAGE")[rng.nextInt(3)]}"
        Packet(
          id = id,
          day = day,
          from = SenderInfo(
            displayName = "外部分析SDK更新",
            email = "beacon@$badHost",
            department = "外部システム",
            authMethod = "SPF/DKIM: PASS",
            avatarSeed = badHost.hashCode(),
          ),
          to = RecipientInfo(
            displayName = recipient.displayName,
            email = recipient.email,
            isExternal = false,
          ),
          subject = "テレメトリ収集エージェント更新パッチ #${rng.nextInt(2, 19)}",
          timestamp = timestamp,
          attachment = AttachmentInfo(
            fileName = "agent_patch_${index % 99}.zip",
            fileType = "application/zip",
            declaredSizeKb = sizeKb,
            quickScanStatus = "CLEAN",
          ),
          body = "最新版の計測パッチを配布します。",
          certificate = CertificateInfo(
            issuer = "DigiTrust JP",
            subjectDomain = badHost,
            validUntil = "2027-04-01",
            tokenType = "TLS_CLIENT_AUTH",
          ),
          expectedVerdict = Verdict.QUARANTINE,
          reasonSummary = "R7該当: 脅威DBでC2サーバー指定の攻撃通信($c2Family)",
          discrepancy = DiscrepancyInfo(
            primaryFieldA = PacketField.FROM,
            primaryTargetA = "from.email",
            primaryFieldB = PacketField.TOOL_RESULT,
            primaryTargetB = "tool.THREAT_DB",
            validPairs = listOf(
              DiscrepancyPair("from.email", "tool.THREAT_DB"),
              DiscrepancyPair("tool.THREAT_DB", "rule.R7"),
              DiscrepancyPair("from.email", "rule.R7"),
            ),
            summary = "脅威DBにて C2(司令サーバー) 認定されているドメイン $badHost からの通信です。検疫が必要です。",
            recommendedTool = ToolType.THREAT_DB,
          ),
          tools = ToolOutputs(
            threatDbResult = "CRITICAL HIT: $badHost は攻撃グループ($c2Family)のC2サーバー！",
            threatDbFlagged = true,
            plaintextLogResult = "ZIP内に不可視バックドアソケット定義あり",
            plaintextFlagged = true,
            actualPayloadKb = sizeKb,
            trafficScaleNote = "実測 ${sizeKb}KB",
            deepScanResult = "HIGH RISK: バックドア通信モジュール検出",
            deepScanFlagged = true,
          ),
          threatFamily = c2Family,
          isFatalVirus = true,
        )
      }

      ProceduralPattern.REVOKED_TOKEN -> {
        val partner = trustedPartnerConfigs[rng.nextInt(trustedPartnerConfigs.size)]
        val sizeKb = 30 + rng.nextInt(50)
        val badToken = if (rng.nextBoolean()) "REVOKED_KEY" else "LEGACY_BEARER"
        Packet(
          id = id,
          day = day,
          from = SenderInfo(
            displayName = partner.first,
            email = partner.second,
            department = "認定取引先",
            authMethod = "SPF/DKIM: PASS",
            avatarSeed = partner.first.hashCode() + index,
          ),
          to = RecipientInfo(
            displayName = recipient.displayName,
            email = recipient.email,
            isExternal = false,
          ),
          subject = "システムAPI連携ステータス報告",
          timestamp = timestamp,
          attachment = AttachmentInfo(
            fileName = "api_status_${index % 99}.json",
            fileType = "application/json",
            declaredSizeKb = sizeKb,
            quickScanStatus = "CLEAN",
          ),
          body = "外部APIノードからの定例ステータス通知です。",
          certificate = CertificateInfo(
            issuer = partner.third,
            subjectDomain = partner.second.substringAfter("@"),
            validUntil = "2027-09-01",
            tokenType = badToken,
          ),
          expectedVerdict = Verdict.BLOCK,
          reasonSummary = "R8違反: 証明書トークン種別が禁止・失効トークン($badToken)",
          discrepancy = DiscrepancyInfo(
            primaryFieldA = PacketField.CERTIFICATE,
            primaryTargetA = "certificate",
            primaryFieldB = PacketField.RULE_ITEM,
            primaryTargetB = "rule.R8",
            validPairs = listOf(
              DiscrepancyPair("certificate", "rule.R8"),
            ),
            summary = "証明書トークン種別が $badToken のため規則R8違反です。遮断してください。",
          ),
          tools = ToolOutputs(
            threatDbResult = "WARNING: 廃止済みトークン形式($badToken)を使用",
            threatDbFlagged = true,
            plaintextLogResult = "TokenStatus: $badToken (要拒否)",
            plaintextFlagged = true,
            actualPayloadKb = sizeKb,
            trafficScaleNote = "実測 ${sizeKb}KB",
            deepScanResult = "添付データ自体はJSON形式",
          ),
        )
      }
    }
  }
}
