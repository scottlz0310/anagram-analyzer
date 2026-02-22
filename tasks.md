# tasks.md - 開発タスク管理

このファイルはプロジェクト全体の進捗管理に使用します。
各イテレーションの開始・完了時に更新してください。

---

## フェーズ 0: 基盤整備

- [x] Python CLI版の初期実装（normalize / index / lexicon / cli）
- [x] テストスイート整備（test_normalize / test_index / test_integration）
- [x] CI/CD（GitHub Actions: lint + test マトリクス）
- [x] pre-commit 設定（ruff, basedpyright, pytest）
- [x] ドキュメント整備（README.md, AGENTS.md, prompt.md）
- [x] Androidアプリ化に向けたドキュメント拡充（Issue #14）

## フェーズ 2: Androidプロジェクト初期構築

- [x] Androidプロジェクト雛形作成（Gradle Kotlin DSL）
- [x] パッケージ構成（data / domain / ui）のスケルトン作成
- [x] Gradle Wrapper（`gradlew`）の導入
- [x] Room DB セットアップ（AnagramEntry Entity + DAO）
- [x] Roomスキーマ差分による起動クラッシュ対策（DB version更新 + Migration 1→2追加）
- [x] Hilt DI 基盤設定
- [x] GitHub Actions にAndroidビルドジョブ追加

## フェーズ 3: コアロジック移植（Kotlin）

- [x] ひらがな正規化（NFKC / カタカナ→ひらがな / バリデーション）
- [x] アナグラムキー生成
- [x] Room DAOによるインデックス検索
- [x] ユニットテスト（JUnit）で Python版との動作一致を検証
- [x] ひらがな正規化・キー生成のユニットテストを追加（基本ケース）
- [x] `MainViewModel` の非同期レース回避に関するユニットテストを追加

## フェーズ 4: UI実装（Jetpack Compose）

- [x] メイン画面（テキスト入力 + 検索 + 候補リスト）
- [x] 手動テスト可能な最小フロー（debug APKビルド + 入力確認手順）
- [x] ランチャーアイコンをアプリ専用画像へ差し替え
- [x] 更新済み `asset/AnagramAnalyzerICON.png` から Android ランチャーアイコン（`mipmap-*/ic_launcher*.png`）を再生成
- [x] AndroidShell向け丸形アイコン更新後、`asset/AnagramAnalyzerICON.png` からランチャーアイコンを再生成
- [x] 候補詳細画面（漢字表記・意味表示）
- [x] 候補詳細画面の最小実装（読み表示 + 漢字/意味プレースホルダ、旧: ダイアログ）
- [x] 候補詳細画面で漢字/意味のseed実データ表示（未収録語はプレースホルダ、旧: ダイアログ）
- [x] 候補詳細画面の意味欄に共有導線を追加（共有ボタン + 長押し選択状態）
- [x] 共有導線のレビュー指摘対応（意味テキストのアクセシビリティ改善 + 共有起動安全化）
- [x] テーマ設定（Material 3: ライト/ダーク切替）
- [x] メイン画面のUIカラー強化（グラデーション背景 + カードレイアウト + カラーボタン）
- [x] メイン画面にイラスト配置（上部左右 + 下部）とPastel配色（Primary/Secondary/Tertiary）を適用
- [x] 装飾イラストをアクセシビリティ対応（`contentDescription = null`）に調整
- [x] 候補一覧の描画最適化（表示最大50件 + 残件数表示）
- [x] UIテスト（Compose Testing）

## フェーズ 5: 辞書データ対応

- [x] JMdict XML → Room DB 変換スクリプト/ツール作成
- [x] Asset同梱方式の実装（初回起動時インポート）
- [x] JMdict語彙を `anagram_seed.tsv` へ出力する最小変換スクリプトを追加
- [x] `anagram_seed.tsv` を初回起動時にRoomへ投入する導線を実装（seedデータ）
- [x] 辞書データサイズ最適化検討（`--max-len` シミュレーションで運用値を決定）
- [x] ライセンスクレジット表記（About画面）
- [x] 初回起動インポートの計測ログ（source/件数/経過ms）を追加
- [x] seedサイズ別の投入時間を計測（`max-len=8/10` 比較）
- [x] 計測結果に基づき運用上限を `max-len=8` に確定
- [x] 候補詳細のオンデマンド取得（未収録語のみ）とRoomキャッシュを追加

## フェーズ 6: 追加機能

- [x] 入力履歴機能
- [x] 入力履歴の折りたたみ表示（表示/非表示トグル）
- [ ] お気に入り（ブックマーク）機能
- [x] 設定画面（文字数範囲、テーマ切替）
- [x] 追加辞書ダウンロード機能（追加seedの適用・状態表示）
- [x] DataStore による設定永続化（テーマ切替）
- [x] DataStore による入力履歴永続化
- [ ] オフライン完全対応の検証

## フェーズ 7: CI/CD・QA・リリース

- [x] GitHub Actions に Android UIテスト（エミュレータ）ジョブを追加
- [x] GitHub Actions の Android Build ジョブで debug APK artifact をアップロード
- [x] GitHub Release 向けに署名済み `app-release.apk` を生成・公開するワークフローを追加
- [x] `Android Release` の `workflow_dispatch` でタグ未指定時の自動タグ発行を追加
- [x] Android release 署名設定（keystore / alias / password の環境変数読込）を追加
- [x] GitHub Actions CI の Androidジョブを分割し、Unit Test / Build / UI Test を並列実行できるよう最適化
- [x] GitHub Actions CI の AndroidジョブのGradleキャッシュ方式を `setup-java` の `cache: gradle` に統一し、PRでのコールドビルドを抑制
- [x] Android UIテストを `Android UI Tests` 専用ワークフローへ分離し、`androidTest` クラス単位2シャード（`pull_request` / `workflow_dispatch` / `schedule`）で実行
- [x] GitHub Actions CI に変更差分判定（`dorny/paths-filter` をcommit SHA固定）を追加し、PR時はAndroid関連変更のみ Android Unit Test / Build を実行
- [x] Android CI（Unit/Build/UI）で Gradle Configuration Cache を有効化し、`android/.gradle/configuration-cache` の保存・復元を追加。ローカル連続実行の `testDebugUnitTest --dry-run --no-daemon` 計測で 6.69s → 4.10s（再利用時、約39%短縮）を確認
- [x] `Android UI Tests` の Configuration Cacheキーに `github.sha` を含め、古いコミットのキャッシュ再利用で発生する `mergeDebugAndroidTestAssets` 失敗（AAR欠損）を回避
- [x] `MainScreenTest` の「システム戻るキー」検証を `onBackPressedDispatcher` ベースへ調整し、CIエミュレータでのタイムアウト（`ComposeTimeoutException`）を抑制
- [ ] Android用CI/CDパイプライン完成
- [ ] リリースビルド設定（署名、ProGuard/R8）
- [ ] Google Play Store 公開準備
- [ ] Cloudflare への配布導線デプロイ（安定化後）
- [ ] iOS対応の検討・計画策定

## フェーズ 8: Pythonプロトタイプ撤去

- [x] `src/anagram_cli`（Python CLI本体）を削除
- [x] Python CLI向けテスト（`tests/`）を削除
- [x] 辞書生成スクリプトをCLI依存なしで実行できるよう更新
- [x] Python CLI前提のCI/依存管理設定（`ci.yml` / `renovate.json` / `pyproject.toml` / `uv.lock` / `.pre-commit-config.yaml`）を整理
- [x] Python CLI前提の記述を主要ドキュメントから削除し、Android単一実装に整合

## フェーズ 9: seed生成Kotlin/JVM移行（Issue #82）

- [x] `tools:seed-generator` モジュール新規作成（Kotlin/JVM + application plugin）
- [x] `JmdictParser.kt`（StAXベースXML/gzipパーサ）
- [x] `Normalizer.kt`（NFKC正規化・カタカナ→ひらがな・anagramKey、Python互換）
- [x] `TsvExporter.kt`（word順ソートTSV出力、フォーマット互換）
- [x] `DbExporter.kt`（Room互換SQLite生成、user_version=3完全スキーマ互換）
- [x] `Main.kt`（CLI: --jmdict/--out-tsv/--out-db/--mode/--min-len/--max-len/--limit/--force）
- [x] `NormalizerTest.kt` / `SeedGeneratorIntegrationTest.kt`（fixture XMLゴールデンテスト）
- [x] `scripts/*.py` 削除・`scripts/` ディレクトリ撤去（Python完全撤去）
- [x] CI に `:tools:seed-generator:test` を追加
- [x] `android/settings.gradle.kts` / `android/build.gradle.kts` に tools:seed-generator を追加

## フェーズ 10: Issue #81 事前リファクタ（MainViewModel / MainScreen 責務分割）

- [x] `domain/usecase/PreloadSeedUseCase.kt` 新規作成（seed初期化・候補詳細ロード・計測ログをカプセル化）
- [x] `domain/usecase/SearchAnagramUseCase.kt` 新規作成（アナグラム索引検索）
- [x] `domain/usecase/LoadCandidateDetailUseCase.kt` 新規作成（候補詳細オンデマンド取得）
- [x] `domain/usecase/ApplyAdditionalDictionaryUseCase.kt` 新規作成（追加辞書適用）
- [x] `domain/model/PreloadLogger.kt` 新規作成（ui→domain の依存逆転を解消）
- [x] `ui/viewmodel/MainUiState.kt` 新規作成（MainUiState を分離ファイルへ）
- [x] `MainViewModel.kt` をユースケース注入版に書き換え（直接依存 anagramDao 等→UC4+store2+dispatcher1）
- [x] `ui/screen/CandidateDetailScreen.kt` 新規作成（MainScreenから切り出し）
- [x] `ui/screen/SettingsDialog.kt` 新規作成（AboutDialog / SettingsDialog を切り出し）
- [x] `ui/screen/ShareUtil.kt` 新規作成（shareCandidateDetail を切り出し）
- [x] `MainScreen.kt` をスリム化（580行→~330行）
- [x] `MainViewModelTest.kt` の26箇所コンストラクタ呼び出しを `buildViewModel` ヘルパー経由に更新

## フェーズ 11: Issue #60 クイズモード実装

- [x] `AnagramDao` に `getRandomEntry(minLen, maxLen)` クエリを追加（`ORDER BY RANDOM() LIMIT 1`）
- [x] `domain/model/QuizDifficulty.kt` 新規作成（EASY/NORMAL/HARD 文字数範囲プリセット enum）
- [x] `domain/model/QuizQuestion.kt` 新規作成（shuffledChars / sortedKey / correctWords を保持するデータクラス）
- [x] `domain/usecase/GenerateQuizUseCase.kt` 新規作成（ランダムエントリ取得 → シャッフル → 正解リスト生成）
- [x] `data/datastore/QuizScoreStore.kt` 新規作成（score / streak / bestStreak の永続化 interface + DataStoreQuizScoreStore 実装）
- [x] `ui/viewmodel/QuizUiState.kt` 新規作成（QuizPhase enum + QuizUiState data class）
- [x] `ui/viewmodel/QuizViewModel.kt` 新規作成（@HiltViewModel、quiz フロー全体制御）
- [x] `di/AppModule.kt` に `provideQuizScoreStore()` を追加
- [x] `ui/screen/QuizScreen.kt` 新規作成（難易度選択・問題表示・入力・正解判定・スコア表示）
- [x] `MainActivity.kt` に `showQuiz` state を追加し、MainScreen と QuizScreen を切り替え
- [x] `MainScreen.kt` に `onNavigateToQuiz` コールバック追加とクイズモードボタン追加
- [x] `QuizViewModelTest.kt` 新規作成（6テストケース: ANSWERING遷移/正解/不正解/エントリなし/難易度/リセット）
- [x] `MainViewModelTest.kt` の `FakeAnagramDao` に `getRandomEntry` メソッドを追加

## フェーズ 12: Issue #88 クイズ単語重みづけ（JMdict re_pri による一般語優先）

- [x] `tools/seed-generator/JmdictParser.kt` に `re_pri` 解析を追加（`isCommon` フラグ付与）
- [x] `tools/seed-generator/AnagramRow.kt` に `isCommon: Boolean = false` フィールド追加
- [x] `tools/seed-generator/TsvExporter.kt` で TSV 4列目 `is_common`（0/1）を出力
- [x] `tools/seed-generator/DbExporter.kt` に `is_common` 列追加・`USER_VERSION=4` に更新
- [x] テスト fixture（`jmdict_sample.xml`）に `re_pri` タグを追加・`expected_anagram_seed.tsv` を4列に更新
- [x] `SeedGeneratorIntegrationTest.kt` に `isCommon` フラグ検証・`user_version=4`・`is_common=1` カウント追加
- [x] `data/db/AnagramEntry.kt` に `is_common` カラム追加（`defaultValue = "0"`）
- [x] `data/db/AnagramDatabase.kt` を version 4 へ更新・`Migration(3, 4)` 追加
- [x] `data/db/AnagramDao.kt` に `countCommonByLength` / `getCommonEntryAtOffset` を追加
- [x] `data/seed/AssetSeedEntryLoader.kt` で3列後方互換を維持しながら4列目 `is_common` をパース
- [x] `domain/usecase/GenerateQuizUseCase.kt` を一般語優先ロジックに変更（`countCommonByLength > 0` なら優先、0件時フォールバック）
- [x] `QuizViewModelTest.kt` と `MainViewModelTest.kt` の `FakeAnagramDao` に新DAOメソッドを追加
- [x] `AssetSeedEntryLoaderTest.kt` に4列TSVパーステストを追加

---

## 進捗サマリ

| フェーズ | 状態 | 備考 |
|---------|------|------|
| 0: 基盤整備 | ✅ 完了 | Android基盤に必要な初期整備とドキュメント整備を完了 |
| 2: Android初期構築 | ✅ 完了 | Room最小DB構成 + Hilt DI基盤 + Android CIジョブ追加 + 起動クラッシュ対策まで完了 |
| 3: ロジック移植 | ✅ 完了 | normalize移植 + Room検索接続 + Kotlin側ユニットテスト整備 |
| 4: UI実装 | 🟡 進行中 | メイン画面実装、候補詳細画面（漢字/意味のseed実データ表示 + 未収録語オンデマンド取得導線 + 共有ボタン/意味長押し選択）、ライト/ダーク切替、グラデーション背景/カード/カラーボタンによるUIカラー強化、上部左右/下部イラスト配置 + Pastel配色適用、候補一覧の50件上限制御、ランチャーアイコン適用（更新素材から再生成含む）、手動テスト可能な最小フロー、Compose UIテスト追加 |
| 5: 辞書データ | 🟡 進行中 | seed変換/取込導線 + サイズ最適化（`max-len=8`）+ ライセンス表示 + 初回インポート計測ログ + 8/10投入時間比較 + 候補詳細オンデマンド取得/キャッシュ + JMdict XML→Room DB 変換ツール + `anagram_seed.db` 優先読込まで実施 |
| 6: 追加機能 | 🟡 進行中 | DataStore によるテーマ設定永続化 + 入力履歴永続化 + 履歴折りたたみ表示 + 設定画面（文字数範囲/テーマ/追加辞書DL適用）まで実装 |
| 7: CI/CD・リリース | 🟡 進行中 | Android UIテスト分離（2シャード）+ CI本体の差分判定でPR時のAndroid Unit/Build条件実行 + Android Unit/Build/UIでConfiguration Cache有効化（`android/.gradle/configuration-cache` 保存復元、ローカル連続計測で `testDebugUnitTest --dry-run` 6.69s→4.10s）に加え、UIテストのConfiguration Cacheキーへ `github.sha` を導入して古いコミットのキャッシュ再利用起因の失敗を抑制。debug APK artifact と GitHub Release向け署名済みAPK公開ワークフロー（dispatch自動タグ発行対応）も継続 |
| 8: Pythonプロトタイプ撤去 | ✅ 完了 | Python CLI本体と関連CI/依存管理を削除し、Android単一実装に整理 |
| 9: seed生成Kotlin/JVM移行 | ✅ 完了 | tools:seed-generator 実装（JmdictParser/Normalizer/TsvExporter/DbExporter/Main）、scripts/*.py削除、CI更新 |
| 12: クイズ単語重みづけ | ✅ 完了 | JMdict re_pri による isCommon フラグ導入、DB version 4、一般語優先出題（フォールバック付き） |
| 10: Issue #81 事前リファクタ | ✅ 完了 | MainViewModelをUC4クラス（PreloadSeed/SearchAnagram/LoadCandidateDetail/ApplyAdditionalDictionary）に分割、MainScreenからCandidateDetailScreen/SettingsDialog/ShareUtilを切り出し、PreloadLoggerをdomain.modelへ移動、MainViewModelTestをbuildViewModelヘルパー経由に更新 |
| 11: Issue #60 クイズモード | ✅ 完了 | QuizDifficulty/QuizQuestion/GenerateQuizUseCase/QuizScoreStore/QuizUiState/QuizViewModel/QuizScreen 新規実装、MainScreen にクイズモードボタン追加、MainActivity で画面切替、QuizViewModelTest 追加 |
