# パケッツ,プリーズ (Packets, Please) — v0.3 Prototype

[パケッツ-プリーズ-要件定義書-v0-3.md](docs/パケッツ-プリーズ-要件定義書-v0-3.md) に基づいて実装した、Android ネイティブ（Kotlin / Jetpack Compose + Canvas）製の試作プロトタイプです。

## 概要

- **ジャンル**: 通信検閲・監査シミュレーション（縦持ち専用・完全オフライン動作）
- **対象環境**: Android minSdk 26 / compileSdk 36 / targetSdk 36
- **画面構成**:
  - **上段監視 CRT モニタ（緑・黒リン光配色）**: 経過時間・部署予算・誤審警告カウント・パケットフロー（社外 ↔ FW検閲ゲート ↔ 社内LAN / 検疫）・警告バナー・M1デバッグパネル起動ボタン
  - **下段検閲デスク（暖色紙面）**: 差出人ピクセルアバター、認証署名・宛先・件名・本文の突合カード、社内セキュリティポリシーブック、解析ツール（脅威DB・平文ログ・流量計測・深度スキャン）、突合モード（2要素比較による矛盾検出）、PASS / BLOCK / QUARANTINE 自作ビットマップスタンプ
- **プロトタイプ実装機能**:
  - 14日間キャンペーン / エンドレス監査モード
  - 日次タイムスケジュール（朝の通達 → 検閲業務シフト → 夕方の部署予算精算・オートセーブ）
  - 誤審警告（1日2回まで無料・3回目以降罰金）、賄賂・監査リスク、監査回避ネットワーク（AAN）クエスト、6種類のエンディング分岐
  - M1プロトタイプ調整パネル（基準解像度切替、1日時間設定、不正検知通知タイミング切替、効果音ミュート、Dayジャンプ）
  - AudioTrack による効果音合成（BGMなし）およびハプティクス演出

## ディレクトリ構成（3層分離）

```text
app/src/main/
├── assets/
│   ├── game_content.json         # 14日分のシナリオ・ポリシー規則・イベント定義
│   └── OFL.txt                   # DotGothic16 SIL Open Font License
├── res/font/dotgothic16.ttf      # DotGothic16 ピクセルフォント
└── kotlin/com/tommy/packetsplease/
    ├── domain/                   # 純粋Kotlinドメイン層（Android非依存）
    │   ├── Models.kt
    │   ├── RuleEngine.kt
    │   ├── EconomyEngine.kt
    │   └── PacketGenerator.kt
    ├── data/                     # データ永続化・アセット読込層
    │   ├── GameContentRepository.kt
    │   └── SaveDataRepository.kt
    └── ui/                       # Compose / Canvas プレゼンテーション層
        ├── MainActivity.kt
        ├── MainScreen.kt
        ├── GameViewModel.kt
        ├── audio/RetroSoundEngine.kt
        ├── pixel/PixelArtAssets.kt
        ├── components/
        │   ├── SurveillanceMonitor.kt
        │   └── InspectionDeskArea.kt
        └── theme/
```

## ビルド・テストコマンド

Windows 環境（プロジェクトパスに非ASCII文字を含む場合でも動作するよう `app/build.gradle.kts` でビルド出力を `~/.gradle/olease-build/app` に分離しています）：

```powershell
# 単体テスト + Debug APK ビルド
.\gradlew.bat testDebugUnitTest assembleDebug --no-daemon
```

- 単体テスト: `app/src/test/kotlin/com/tommy/packetsplease/DomainLogicTest.kt`（7テスト）
- APK のダウンロードは本リポジトリの **Releases** を参照してください。

## ライセンス・フォントクレジット

- フォント: [DotGothic16](OFL.txt) (SIL Open Font License 1.1)
