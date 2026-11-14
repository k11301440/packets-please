package com.tommy.packetsplease.data

import android.content.Context
import com.tommy.packetsplease.domain.DayScenario
import com.tommy.packetsplease.domain.GameContentAsset
import com.tommy.packetsplease.domain.PolicyRule
import kotlinx.serialization.json.Json

class GameContentRepository(private val context: Context) {

  private val json = Json {
    ignoreUnknownKeys = true
    isLenient = true
  }

  fun loadContent(): GameContentAsset {
    return runCatching {
      context.assets.open("game_content.json").bufferedReader().use { reader ->
        json.decodeFromString<GameContentAsset>(reader.readText())
      }
    }.getOrElse {
      fallbackContent()
    }
  }

  companion object {
    fun fallbackContent(): GameContentAsset = GameContentAsset(
      version = "0.3.0-fallback",
      currentDateLabel = "2026-11-14",
      trustedDomains = listOf("olease.co.jp", "partner-cloud.jp", "yamato-cpa.jp", "jdc-infra.jp"),
      trustedIssuers = listOf("Olease Internal CA", "GlobalTrust G3", "DigiTrust JP"),
      departments = listOf("開発部", "営業部", "経理部", "情シス部", "人事部", "法務部"),
      days = listOf(
        DayScenario(
          day = 1,
          morningHeadline = "DAY 01: ファイアウォール起動",
          morningMemo = "規則R1を確認し、未承認外部ドメインからの決済メールを遮断せよ。",
          targetPacketCount = 2,
          newRules = listOf(
            PolicyRule(
              id = "R1",
              effectiveFromDay = 1,
              title = "R1: 未承認外部ドメインの遮断",
              description = "社内(@olease.co.jp)および認定取引先以外からの決済・認証要求はBLOCK。",
              category = "DOMAIN",
            ),
          ),
        ),
      ),
      proceduralTemplates = emptyList(),
    )
  }
}
