# AGENTS.md - anagram-analyzer 開発ガイド

このドキュメントはAIコーディングエージェント向けに、現在の実装方針と開発ルールをまとめたものです。

## プロジェクト概要

**anagram-analyzer** は、ひらがな文字列を並べ替えて作れる日本語単語候補を返す Android アプリです。

- 本実装: **Android版（Kotlin + Jetpack Compose）**
- 補助ツール: **Python辞書変換スクリプト（seed生成用）**
- 旧 Python CLI プロトタイプ: **削除済み（2026-02-21）**

## 技術スタック

### Androidアプリ本体

| カテゴリ | 技術 |
|---------|------|
| 言語 | Kotlin |
| UI | Jetpack Compose |
| アーキテクチャ | MVVM + Clean Architecture |
| DI | Hilt |
| データストア | Room (SQLite), DataStore |
| テスト | JUnit, Espresso/Compose Testing |
| ビルド | Gradle (Kotlin DSL) |
| CI | GitHub Actions（Android Unit/Build/UI/Release） |

### 辞書生成スクリプト

| カテゴリ | 技術 |
|---------|------|
| 言語 | Python 3 |
| 入力 | JMdict XML (`.xml` / `.gz`) |
| 出力 | `anagram_seed.tsv`, `anagram_seed.db` |

## ディレクトリ構造（現行）

```
anagram-analyzer/
├── android/                             # Androidアプリ本体
│   ├── app/
│   │   └── src/
│   │       ├── main/
│   │       │   ├── assets/
│   │       │   │   ├── anagram_seed.tsv
│   │       │   │   ├── anagram_additional_seed.tsv
│   │       │   │   └── candidate_detail_seed.tsv
│   │       │   └── java/com/anagram/analyzer/
│   │       │       ├── data/
│   │       │       ├── di/
│   │       │       ├── domain/
│   │       │       └── ui/
│   │       ├── test/
│   │       └── androidTest/
│   ├── tools/
│   │   └── seed-generator/              # JMdict→TSV/DB 生成ツール（Kotlin/JVM CLI）
│   │       ├── build.gradle.kts
│   │       └── src/
│   │           ├── main/kotlin/com/anagram/tools/seedgenerator/
│   │           │   ├── Main.kt
│   │           │   ├── Normalizer.kt
│   │           │   ├── JmdictParser.kt
│   │           │   ├── TsvExporter.kt
│   │           │   └── DbExporter.kt
│   │           └── test/
│   │               ├── kotlin/…/NormalizerTest.kt
│   │               ├── kotlin/…/SeedGeneratorIntegrationTest.kt
│   │               └── resources/jmdict_sample.xml / expected_anagram_seed.tsv
│   ├── gradle/
│   ├── gradlew
│   └── settings.gradle.kts
├── asset/
├── README.md
├── tasks.md
├── CHANGELOG.md
└── prompt.md
```

## モジュール詳細（Android）

### `domain/model/HiraganaNormalizer.kt`

- NFKC正規化
- 空白除去
- カタカナ→ひらがな変換
- ひらがな判定
- アナグラムキー生成

### `data/db/`

- `AnagramEntry.kt`: アナグラム索引Entity（`sorted_key + word` 一意制約）
- `AnagramDao.kt`: seed投入・キー検索
- `AnagramDatabase.kt`: Room DB本体（`version 3`、Migration `1→2` / `2→3`）
- `CandidateDetailCacheEntry.kt` / `CandidateDetailCacheDao.kt`: 候補詳細キャッシュ

### `data/seed/`

- `AssetSeedEntryLoader.kt`: `anagram_seed.db` 優先、未同梱時 `anagram_seed.tsv` フォールバック
- `AssetAdditionalSeedEntryLoader.kt`: 追加seed適用
- `AssetCandidateDetailLoader.kt`: 候補詳細seed + キャッシュ統合
- `JishoCandidateDetailRemoteDataSource.kt`: 未収録語のオンデマンド取得

### `ui/`

- `MainScreen.kt`: 入力・候補一覧・設定・候補詳細画面
- `MainViewModel.kt`: 検索/履歴/設定/辞書適用のUI状態管理

## モジュール詳細（辞書生成ツール）

### `tools/seed-generator/`（Kotlin/JVM CLIツール）

JMdict XML/gzip から `anagram_seed.tsv` / Room互換SQLite を生成する独立JVMツール。

| ファイル | 説明 |
|---------|------|
| `Main.kt` | CLIエントリポイント（`--jmdict/--out-tsv/--out-db/--mode/--min-len/--max-len/--limit/--force`） |
| `Normalizer.kt` | NFKC正規化・カタカナ→ひらがな・ひらがな判定・anagramKey（Python版互換） |
| `JmdictParser.kt` | StAXベースXML/gzipパーサ。AnagramRowデータクラスを返す |
| `TsvExporter.kt` | word順ソートでTSV出力（`sorted_key\tword\tlength\n`） |
| `DbExporter.kt` | Room互換SQLite生成（`PRAGMA user_version=3`、`anagram_entries` + `candidate_detail_cache`、全インデックス付） |

## 開発コマンド

### Android

```bash
# Debugビルド
cd android && ./gradlew :app:assembleDebug

# ユニットテスト
cd android && ./gradlew :app:testDebugUnitTest

# UIテスト
cd android && ./gradlew :app:connectedDebugAndroidTest

# Lint
cd android && ./gradlew :app:lintDebug

# Release APK（署名情報を環境変数で指定）
cd android && ANDROID_SIGNING_STORE_FILE=/path/to/release.keystore ANDROID_SIGNING_STORE_PASSWORD=*** ANDROID_SIGNING_KEY_ALIAS=*** ANDROID_SIGNING_KEY_PASSWORD=*** ./gradlew :app:assembleRelease
```

### 辞書seed生成

```bash
# TSV生成
cd android && ./gradlew :tools:seed-generator:run \
  --args="--jmdict ~/.jamdict/data/JMdict_e.gz --out-tsv app/src/main/assets/anagram_seed.tsv --mode tsv --min-len 2 --max-len 8"

# Room互換SQLite生成
cd android && ./gradlew :tools:seed-generator:run \
  --args="--jmdict ~/.jamdict/data/JMdict_e.gz --out-db app/src/main/assets/anagram_seed.db --mode db --min-len 2 --max-len 8 --force"

# TSV + DB 同時生成
cd android && ./gradlew :tools:seed-generator:run \
  --args="--jmdict ~/.jamdict/data/JMdict_e.gz --out-tsv app/src/main/assets/anagram_seed.tsv --out-db app/src/main/assets/anagram_seed.db --mode both --min-len 2 --max-len 8 --force"

# seed-generatorのテスト実行
cd android && ./gradlew :tools:seed-generator:test --no-daemon
```

## CI運用

- `CI`（`.github/workflows/ci.yml`）
  - Android Unit Test / Build を実行
  - PRでは `dorny/paths-filter` で Android関連差分時のみ実行
  - `--configuration-cache` + `android/.gradle/configuration-cache` を `actions/cache` で保存/復元
- `Android UI Tests`（`.github/workflows/android-ui-tests.yml`）
  - `androidTest` をクラス単位2シャード実行
  - `pull_request`（path filter）/ `workflow_dispatch` / `schedule`
- `Android Release`（`.github/workflows/android-release.yml`）
  - 署名済み `app-release.apk` を artifact / Release asset として公開

## 開発ルール（必須）

### ブランチ運用

- 原則として `main` に直接コミットしない。feature/fix ブランチで作業して PR でマージする
- 大きいタスクは先に Issue を作成して整理する
- ドキュメント更新やリリース準備は `main` で直接作業しても構わない

### PR作成後の自動レビュー対応ルーティン

PR を作成した後は、Copilot 自動レビューおよび CI の結果を確認し、指摘がなくなるまで自動で修正イテレーションを行う。

**監視ループ**

- PR 作成直後に監視ループを開始する
- 監視頻度: 2 分間隔、最大継続時間: 10 分（6 回チェック）
- 各チェックで以下を確認する
  - PR のレビューコメント（Copilot 自動レビュー等）と通常コメント（Issue comments: Codecov など Bot コメントを含む）の有無
  - CI（GitHub Actions）のステータス（pending / running / success / failure）
  - 「Copilot code review」ワークフロー（Copilot がレビューリクエスト時に自動実行する特別なワークフロー）が実行中の場合は、レビュー未完了として待機を継続する

**指摘・CI エラー検出時の修正**

- レビューコメント、通常コメント（Issue comments）、または CI 失敗を検出した場合は即座に修正する
- レビューコメントと通常コメントは内容を精査し採否を判断する。正当な指摘は修正し、的外れな指摘は理由を付けて却下する。いずれの場合もコメントに返信を残す
- 修正後は必ず新しいコミットを作成して push する（Copilot 自動レビューは新しいコミットの push でのみ再実行されるため）
- コード変更がない場合でも空コミットで push して再レビューを発火させる

  ```bash
  git commit --allow-empty -m "chore: Copilot 自動レビューを再実行"
  git push
  ```

- push 完了後、監視ループを最初からリセット（再度 10 分間の監視）して再確認する

**完了条件**

以下がすべて満たされた場合、PR を「レビュー完了」とみなしループを終了する。

- 新しいレビューコメントと通常コメント（Issue comments）が無い
- CI がすべて成功している
- 「Copilot code review」ワークフローが完了済みである

### バージョン調査の注意

- AI から見て不自然に新しいバージョンに感じても、勝手にバージョンダウンしない
- 学習時期のタイムラグを前提に、必要に応じて Web で最新情報を確認する

### 言語

- Issue、PR、コミットメッセージ、コードコメント、ドキュメントは日本語で記述
- 識別子（変数/関数/クラス名）は英語可

### ドキュメント更新義務

イテレーションごとに以下を必ず更新すること。

| ドキュメント | 更新内容 |
|-------------|---------|
| `tasks.md` | 完了タスクのチェック、新規タスクの追加、進捗サマリ更新 |
| `CHANGELOG.md` | [Unreleased] へ変更を追記 |
| `AGENTS.md` | 構造変更・モジュール変更を反映 |
| `README.md` | ユーザー向け説明の整合性を維持 |
| `prompt.md` | 仕様変更時に更新 |

## コーディング規約

### Android（Kotlin）

- Kotlin公式コーディング規約に準拠
- 単一責任原則（SRP）を厳守
- UI状態は StateFlow ベースの一方向データフロー
- 非同期処理は Coroutines / Flow
- DIは Hilt
- テストは Unit Test + UI Test を維持

### Python（補助スクリプト）

- 補助用途のみ（アプリ本体ロジックを実装しない）
- 標準ライブラリ中心で依存最小化
- 失敗時は `RuntimeError` で理由を明示

## 注意事項

### 辞書データとライセンス

- JMdict は **CC BY-SA 4.0**
- アプリ内にクレジット表記を必ず表示する
- 生成DBファイルは成果物扱い（必要に応じて `.gitignore` 管理）

### seed運用

- `--max-len=8` を推奨（サイズ/投入時間バランス）
- `anagram_seed.db` 同梱時はDB優先読込、未同梱時はTSVフォールバック

## トラブルシューティング

### JMdict XMLが見つからない

- `--xml` で明示的にパスを指定する
- 省略時の自動解決には `jamdict` / `jamdict-data` が必要

### Androidビルド失敗

```bash
cd android && ./gradlew --stop
cd android && ./gradlew :app:assembleDebug --no-daemon --configuration-cache
```
