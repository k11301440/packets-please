package com.tommy.packetsplease.domain

import kotlin.random.Random

object PacketGenerator {

  private val validStaff = listOf(
    Triple("佐藤 健太", "k.sato@olease.co.jp", "開発部"),
    Triple("高橋 美咲", "m.takahashi@olease.co.jp", "営業部"),
    Triple("鈴木 一郎", "i.suzuki@olease.co.jp", "経理部"),
    Triple("田中 翔太", "s.tanaka@olease.co.jp", "情シス部"),
    Triple("伊藤 結衣", "y.ito@olease.co.jp", "人事部"),
    Triple("渡辺 直樹", "n.watanabe@olease.co.jp", "法務部"),
  )

  private val trustedExternalPartners = listOf(
    Triple("クラウド基盤監視", "alert@partner-cloud.jp", "GlobalTrust G3"),
    Triple("大和会計事務所", "audit@yamato-cpa.jp", "DigiTrust JP"),
    Triple("日本データセンター", "ops@jdc-infra.jp", "GlobalTrust G3"),
  )

  private val typoDomains = listOf(
    "o1ease.co.jp",
    "olease-sec.com",
    "0lease.co.jp",
    "olease.co.jp.verify-portal.net",
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

    val targetCount = scenario?.targetPacketCount ?: (7 + (day % 3))
    val remaining = (targetCount - scripted.size).coerceAtLeast(0)
    val rng = Random(seed)

    val availablePatterns = buildAvailablePatterns(activeRules)
    val generated = (1..remaining).map { index ->
      val pattern = availablePatterns[rng.nextInt(availablePatterns.size)]
      buildProceduralPacket(
        id = "D${day}_P${index + scripted.size}",
        day = day,
        index = index,
        pattern = pattern,
        activeRules = activeRules,
        rng = rng,
      )
    }

    val combined = scripted.toMutableList()
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
    return list
  }

  private fun buildProceduralPacket(
    id: String,
    day: Int,
    index: Int,
    pattern: ProceduralPattern,
    activeRules: Set<String>,
    rng: Random,
  ): Packet {
    val sender = validStaff[rng.nextInt(validStaff.size)]
    val recipient = validStaff.filter { it != sender }[rng.nextInt(validStaff.size - 1)]
    val hour = 9 + (index % 8)
    val minute = (index * 13 + rng.nextInt(10)) % 60
    val timestamp = "2026-11-${(13 + day).coerceAtMost(30)} %02d:%02d".format(hour, minute)
    val includeCert = day >= 3 || "R4" in activeRules

    return when (pattern) {
      ProceduralPattern.CLEAN_INTERNAL -> {
        val sizeKb = 24 + rng.nextInt(180)
        Packet(
          id = id,
          day = day,
          from = SenderInfo(
            displayName = sender.first,
            email = sender.second,
            department = sender.third,
            authMethod = "内部SSO署名: PASS",
            avatarSeed = sender.first.hashCode(),
          ),
          to = RecipientInfo(
            displayName = recipient.first,
            email = recipient.second,
            isExternal = false,
          ),
          subject = listOf(
            "週次進捗レポートの共有",
            "会議室予約変更のご連絡",
            "Q4予算スプレッドシート確認依頼",
            "定例ミーティング議事録",
          )[rng.nextInt(4)],
          timestamp = timestamp,
          attachment = AttachmentInfo(
            fileName = "weekly_memo_$index.pdf",
            fileType = "application/pdf",
            declaredSizeKb = sizeKb,
            quickScanStatus = "CLEAN",
          ),
          body = "お疲れ様です。${sender.third}の${sender.first}です。社内規定書式にて共有資料を送付します。",
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
        val partner = trustedExternalPartners[rng.nextInt(trustedExternalPartners.size)]
        val sizeKb = 64 + rng.nextInt(60)
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
            displayName = recipient.first,
            email = recipient.second,
            isExternal = false,
          ),
          subject = "【定期】稼働レポートおよび請求明細 #${100 + index}",
          timestamp = timestamp,
          attachment = AttachmentInfo(
            fileName = "partner_report_$index.pdf",
            fileType = "application/pdf",
            declaredSizeKb = sizeKb,
            quickScanStatus = "CLEAN",
          ),
          body = "登録済み取引先ポータルからの自動送付です。今月のシステム稼働レポートをお送りします。",
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
        val fakeDomain = "pay-invoice-now${rng.nextInt(10, 99)}.biz"
        val sizeKb = 48
        Packet(
          id = id,
          day = day,
          from = SenderInfo(
            displayName = "緊急決済サポート",
            email = "billing@$fakeDomain",
            department = "外部未登録",
            authMethod = "SPF: NONE",
            avatarSeed = fakeDomain.hashCode(),
          ),
          to = RecipientInfo(
            displayName = "経理部 窓口",
            email = "keiri@olease.co.jp",
            isExternal = false,
          ),
          subject = "【至急】本日締切・未払いサーバー料金のご請求",
          timestamp = timestamp,
          attachment = null,
          body = "口座情報変更のため本日17時までに添付リンク先へお振込みください。",
          certificate = if (includeCert) {
            CertificateInfo(
              issuer = "Unknown CA",
              subjectDomain = fakeDomain,
              validUntil = "2027-01-01",
              tokenType = "NONE",
            )
          } else null,
          expectedVerdict = Verdict.BLOCK,
          reasonSummary = "R1違反: 未承認の外部ドメインからの請求・緊急決済メール",
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
        val sizeKb = 42
        Packet(
          id = id,
          day = day,
          from = SenderInfo(
            displayName = "${sender.first} (なりすまし)",
            email = sender.second,
            department = sender.third,
            authMethod = "SPF/DKIM: FAIL (外部IP発)",
            avatarSeed = sender.first.hashCode() + 31,
          ),
          to = RecipientInfo(
            displayName = recipient.first,
            email = recipient.second,
            isExternal = false,
          ),
          subject = "パスワード再認証のお願い",
          timestamp = timestamp,
          attachment = null,
          body = "社内システムのメンテナンスに伴い、至急ログイン情報を再送信してください。",
          certificate = if (includeCert) {
            CertificateInfo(
              issuer = "Olease Internal CA",
              subjectDomain = "olease.co.jp",
              validUntil = "2027-08-31",
              tokenType = "OAUTH2_MTLS",
            )
          } else null,
          expectedVerdict = Verdict.BLOCK,
          reasonSummary = "差出人認証失敗（SPF/DKIM: FAIL）による送信元偽装",
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
            summary = "差出人は社内ドメインですが認証がFAIL、発信元IPも社外リレーです。",
            recommendedTool = ToolType.PLAINTEXT_LOG,
          ),
          tools = ToolOutputs(
            threatDbResult = "WARNING: 発信元IP 198.51.100.44 は海外プロキシ",
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
        val declaredKb = 14
        val actualKb = 3420
        Packet(
          id = id,
          day = day,
          from = SenderInfo(
            displayName = sender.first,
            email = sender.second,
            department = sender.third,
            authMethod = "内部SSO署名: PASS",
            avatarSeed = sender.first.hashCode(),
          ),
          to = RecipientInfo(
            displayName = recipient.first,
            email = recipient.second,
            isExternal = false,
          ),
          subject = "月次経費精算書データ送付",
          timestamp = timestamp,
          attachment = AttachmentInfo(
            fileName = "expense_sheet_$index.xlsx",
            fileType = "application/vnd.ms-excel",
            declaredSizeKb = declaredKb,
            quickScanStatus = "CLEAN",
          ),
          body = "今月分の経費シートを送ります。軽量ファイルです。",
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
            deepScanResult = "非圧縮バイナリブロック(3.3MB)がファイル末尾に結合",
            deepScanFlagged = true,
          ),
        )
      }

      ProceduralPattern.EXPIRED_CERTIFICATE -> {
        val sizeKb = 88
        Packet(
          id = id,
          day = day,
          from = SenderInfo(
            displayName = "外部CRM連携API",
            email = "sync@partner-cloud.jp",
            department = "認定取引先",
            authMethod = "SPF/DKIM: PASS",
            avatarSeed = 404,
          ),
          to = RecipientInfo(
            displayName = recipient.first,
            email = recipient.second,
            isExternal = false,
          ),
          subject = "CRM顧客同期バッチ完了通知",
          timestamp = timestamp,
          attachment = AttachmentInfo(
            fileName = "crm_diff_$index.json",
            fileType = "application/json",
            declaredSizeKb = sizeKb,
            quickScanStatus = "CLEAN",
          ),
          body = "夜間バッチの差分サマリーを送付します。",
          certificate = CertificateInfo(
            issuer = "GlobalTrust G3",
            subjectDomain = "partner-cloud.jp",
            validUntil = "2026-09-30",
            tokenType = "TLS_CLIENT_AUTH",
          ),
          expectedVerdict = Verdict.BLOCK,
          reasonSummary = "R4違反: クライアント証明書の有効期限切れ (2026-09-30)",
          discrepancy = DiscrepancyInfo(
            primaryFieldA = PacketField.CERTIFICATE,
            primaryTargetA = "certificate",
            primaryFieldB = PacketField.RULE_ITEM,
            primaryTargetB = "rule.R4",
            validPairs = listOf(
              DiscrepancyPair("certificate", "rule.R4"),
              DiscrepancyPair("certificate", "timestamp"),
            ),
            summary = "証明書の有効期限が 2026-09-30 で失効しています（本日は2026-11月）。",
          ),
          tools = ToolOutputs(
            threatDbResult = "CLEAN: ドメイン自体はホワイトリスト",
            plaintextLogResult = "TLSハンドシェイク警告: CERT_EXPIRED",
            plaintextFlagged = true,
            actualPayloadKb = sizeKb,
            trafficScaleNote = "実測 ${sizeKb}KB（申告と一致）",
            deepScanResult = "ペイロード内容自体はJSONテキストのみ",
          ),
        )
      }

      ProceduralPattern.MALWARE_QUARANTINE -> {
        val sizeKb = 240
        Packet(
          id = id,
          day = day,
          from = SenderInfo(
            displayName = "採用応募窓口",
            email = "recruit-form@partner-cloud.jp",
            department = "認定取引先",
            authMethod = "SPF/DKIM: PASS",
            avatarSeed = 666 + index,
          ),
          to = RecipientInfo(
            displayName = "伊藤 結衣",
            email = "y.ito@olease.co.jp",
            isExternal = false,
          ),
          subject = "中途採用 職務経歴書のご送付",
          timestamp = timestamp,
          attachment = AttachmentInfo(
            fileName = "resume_2026.pdf.ps1",
            fileType = "application/octet-stream",
            declaredSizeKb = sizeKb,
            quickScanStatus = "SUSPICIOUS",
          ),
          body = "面接日程のご調整ありがとうございます。経歴書をお送りします。",
          certificate = if (includeCert) {
            CertificateInfo(
              issuer = "GlobalTrust G3",
              subjectDomain = "partner-cloud.jp",
              validUntil = "2027-06-30",
              tokenType = "TLS_CLIENT_AUTH",
            )
          } else null,
          expectedVerdict = Verdict.QUARANTINE,
          reasonSummary = "R3該当: 偽装スクリプト/マルウェア署名検出のため検疫(QUARANTINE)対象",
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
            summary = "二重拡張子 .pdf.ps1 および難読化PowerShellドロッパーを検知。即座に検疫してください。",
            recommendedTool = ToolType.DEEP_SCAN,
          ),
          tools = ToolOutputs(
            threatDbResult = "CRITICAL: ハッシュ値が既知ランサム系ドロッパーに一致",
            threatDbFlagged = true,
            plaintextLogResult = "スクリプト実行命令 Base64 -EncodedCommand を含む",
            plaintextFlagged = true,
            actualPayloadKb = sizeKb,
            trafficScaleNote = "実測 ${sizeKb}KB",
            deepScanResult = "MALWARE DETECTED: Trojan.PS.OleaseDropper（要QUARANTINE）",
            deepScanFlagged = true,
          ),
        )
      }

      ProceduralPattern.TYPOSQUATTING_DOMAIN -> {
        val typo = typoDomains[rng.nextInt(typoDomains.size)]
        val sizeKb = 32
        Packet(
          id = id,
          day = day,
          from = SenderInfo(
            displayName = "情シス部 セキュリティ通知",
            email = "security@$typo",
            department = "情シス部(?)",
            authMethod = "SPF/DKIM: PASS (外部CA)",
            avatarSeed = typo.hashCode(),
          ),
          to = RecipientInfo(
            displayName = recipient.first,
            email = recipient.second,
            isExternal = false,
          ),
          subject = "社内VPN証明書アップデートのお願い",
          timestamp = timestamp,
          attachment = null,
          body = "本日の業務開始前に全社員へVPN設定更新をお願いしております。",
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
            plaintextLogResult = "登録者: Anonymous Registrar / 2日前にドメイン作成",
            plaintextFlagged = true,
            actualPayloadKb = sizeKb,
            trafficScaleNote = "実測 ${sizeKb}KB",
            deepScanResult = "外部フィッシングサイトへのログイン誘導あり",
          ),
        )
      }

      ProceduralPattern.SENSITIVE_EXFILTRATION -> {
        val sizeKb = 1280
        Packet(
          id = id,
          day = day,
          from = SenderInfo(
            displayName = sender.first,
            email = sender.second,
            department = sender.third,
            authMethod = "内部SSO署名: PASS",
            avatarSeed = sender.first.hashCode(),
          ),
          to = RecipientInfo(
            displayName = "個人クラウドバックアップ",
            email = "personal_dump@free-mail-box.net",
            isExternal = true,
          ),
          subject = "顧客名簿マスターバックアップ",
          timestamp = timestamp,
          attachment = AttachmentInfo(
            fileName = "customer_pii_all.csv",
            fileType = "text/csv",
            declaredSizeKb = sizeKb,
            quickScanStatus = "CLEAN",
          ),
          body = "自宅作業用に顧客リストを個人宛へ転送します。",
          certificate = CertificateInfo(
            issuer = "Olease Internal CA",
            subjectDomain = "olease.co.jp",
            validUntil = "2027-08-31",
            tokenType = "OAUTH2_MTLS",
          ),
          expectedVerdict = Verdict.BLOCK,
          reasonSummary = "R6違反: 社外宛先への顧客データ(PII)持ち出し送信",
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
            summary = "外部フリーメール宛に顧客個人情報CSV(customer_pii_all.csv)を送出しようとしています。",
            recommendedTool = ToolType.PLAINTEXT_LOG,
          ),
          tools = ToolOutputs(
            threatDbResult = "INFO: 宛先 free-mail-box.net は未承認外部メールサービス",
            threatDbFlagged = true,
            plaintextLogResult = "CSVヘッダ検出: 氏名,電話番号,クレジットカード下4桁...",
            plaintextFlagged = true,
            actualPayloadKb = sizeKb,
            trafficScaleNote = "実測 ${sizeKb}KB（申告と一致）",
            deepScanResult = "マルウェアではないが機密情報DLPルールに抵触",
          ),
        )
      }

      ProceduralPattern.THREAT_DB_BLACKLIST -> {
        val badHost = "cdn-telemetry-x${rng.nextInt(10, 99)}.org"
        val sizeKb = 76
        Packet(
          id = id,
          day = day,
          from = SenderInfo(
            displayName = "外部分析SDK",
            email = "beacon@$badHost",
            department = "外部システム",
            authMethod = "SPF/DKIM: PASS",
            avatarSeed = badHost.hashCode(),
          ),
          to = RecipientInfo(
            displayName = recipient.first,
            email = recipient.second,
            isExternal = false,
          ),
          subject = "テレメトリ収集エージェント更新パッチ",
          timestamp = timestamp,
          attachment = AttachmentInfo(
            fileName = "agent_patch.zip",
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
          reasonSummary = "R7該当: 脅威インテリジェンスDBでC2サーバー指定の攻撃通信",
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
            threatDbResult = "CRITICAL HIT: $badHost はAPT攻撃グループのC2サーバー！",
            threatDbFlagged = true,
            plaintextLogResult = "ZIP内に不可視バイナリが含まれる",
            plaintextFlagged = true,
            actualPayloadKb = sizeKb,
            trafficScaleNote = "実測 ${sizeKb}KB",
            deepScanResult = "HIGH RISK: バックドア通信モジュール検出",
            deepScanFlagged = true,
          ),
        )
      }
    }
  }
}
