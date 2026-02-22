# CHANGELOG.md

このファイルはプロジェクトの変更履歴を記録します。
[Keep a Changelog](https://keepachangelog.com/ja/1.1.0/) の形式に準拠します。

## [Unreleased]

### Added

- **クイズモード**（Issue #60）
  - `domain/model/QuizDifficulty.kt`（EASY/NORMAL/HARD 文字数範囲プリセット）
  - `domain/model/QuizQuestion.kt`（shuffledChars / sortedKey / correctWords データクラス）
  - `domain/usecase/GenerateQuizUseCase.kt`（ランダムエントリ取得 → 文字シャッフル → 正解リスト生成）
  - `data/datastore/QuizScoreStore.kt`（score / streak / bestStreak 永続化、interface + DataStoreQuizScoreStore）
  - `ui/viewmodel/QuizUiState.kt` + `QuizPhase` enum（IDLE/LOADING/ANSWERING/CORRECT/INCORRECT）
  - `ui/viewmodel/QuizViewModel.kt`（@HiltViewModel、難易度選択・出題・回答判定・スコア管理）
  - `ui/screen/QuizScreen.kt`（難易度選択→問題→回答→正解/不正解フローUI）
  - `ui/viewmodel/QuizViewModelTest.kt`（6テストケース: 出題/正解/不正解/エントリなし/難易度/リセット）
- `AnagramDao` に `getRandomEntry(minLen, maxLen): AnagramEntry?` クエリを追加
- `AppModule` に `provideQuizScoreStore()` を追加
- `MainScreen` に「🎯 クイズモード」ボタン（`onNavigateToQuiz` コールバック）を追加
- `MainActivity` に `showQuiz` state を追加し、MainScreen ⇔ QuizScreen を切り替え

- `tools:seed-generator` モジュールを新規追加（Kotlin/JVM + application plugin）
  - JMdict XML/gzip から `anagram_seed.tsv` / Room互換SQLite DB を生成する独立JVMツール
  - `Normalizer.kt`（NFKC正規化・カタカナ→ひらがな・anagramKey）
  - `JmdictParser.kt`（StAXベースXML/gzipパーサ）
  - `TsvExporter.kt`（word順ソートTSV出力）
  - `DbExporter.kt`（Room互換SQLite生成、user_version=3、完全スキーマ互換）
  - `Main.kt`（CLI: --jmdict/--out-tsv/--out-db/--mode/--min-len/--max-len/--limit/--force）
  - `NormalizerTest.kt` / `SeedGeneratorIntegrationTest.kt`（fixture XMLゴールデンテスト）
  - `jmdict_sample.xml` / `expected_anagram_seed.tsv`（CI用fixture）
- GitHub Actions `CI` の android-unit ジョブに `:tools:seed-generator:test` を追加

### Changed

- `MainViewModel` をユースケース注入版にリファクタリング（`PreloadSeedUseCase` / `SearchAnagramUseCase` / `LoadCandidateDetailUseCase` / `ApplyAdditionalDictionaryUseCase` の4クラスに責務分割）
- `MainScreen.kt`（580行）から `CandidateDetailScreen.kt` / `SettingsDialog.kt`（`AboutDialog` + `SettingsDialog`）/ `ShareUtil.kt` を切り出し（~330行にスリム化）
- `PreloadLogger` fun interface を `ui.viewmodel` から `domain.model` へ移動し、依存方向を domain→ui から正しい方向に修正
- `MainUiState` data class を `ui/viewmodel/MainUiState.kt` に分離
- `MainViewModelTest.kt` の26箇所 `MainViewModel(...)` 直接構築を `buildViewModel` ヘルパー経由に更新


- `scripts/` ディレクトリを削除（Python実行スクリプトのリポジトリ完全撤去）
- `android/settings.gradle.kts` に `:tools:seed-generator` を追加
- `android/build.gradle.kts` に `org.jetbrains.kotlin.jvm` プラグイン宣言を追加
- Android `MainScreen` の候補詳細画面に共有導線を追加し、意味がある語は `共有` ボタンから `ACTION_SEND` で外部アプリへ共有できるよう更新
- 候補詳細画面の意味テキストを長押しすると選択状態に切り替わるよう更新し、`選択解除` 操作を追加
- `MainScreenTest` に共有ボタン表示と意味長押し時の選択状態UIテストを追加
- レビュー指摘対応として、意味テキストのタップでも選択状態に入りアクセシビリティの誤誘導を回避するよう更新
- 共有実行時に `resolveActivity` で共有先の存在を確認し、非Activityコンテキストでは `FLAG_ACTIVITY_NEW_TASK` を付与するよう更新

## [1.0.0] - 2026-02-21

### Added

- Androidプロジェクトの初期実装を追加
  - `android/` に Gradle Kotlin DSL ベースの最小構成（root/app）を追加
  - `gradlew` / `gradlew.bat` / `gradle/wrapper/*` を追加（Gradle Wrapper導入）
  - `MainActivity` / `MainScreen` / `MainViewModel` の最小UI・状態管理を追加
  - `HiraganaNormalizer.kt`（NFKC正規化・カタカナ→ひらがな・キー生成）を追加
  - `HiraganaNormalizerTest.kt`（JUnit）を追加
  - Room最小構成を追加（`AnagramEntry` Entity, `AnagramDao`, `AnagramDatabase`）
  - `MainViewModel` からRoom検索を実行し、`MainScreen` で候補リスト表示を追加
  - READMEにAndroid手動テスト手順（`assembleDebug` / `adb install` / 起動コマンド）を追加
- AndroidのDI基盤としてHiltを追加
  - `AnagramApplication`（`@HiltAndroidApp`）を追加
  - `di/AppModule.kt` で `AnagramDatabase` / `AnagramDao` / `CoroutineDispatcher` の提供を追加
  - `MainViewModel` を `@HiltViewModel` + `@Inject` コンストラクタへ移行
  - `MainActivity` を `@AndroidEntryPoint` 化し、`MainScreen` を `hiltViewModel()` 利用へ変更
  - GradleにHiltプラグイン・依存関係を追加
- GitHub Actions の CI に Android ビルドジョブを追加
  - JDK 17 をセットアップして `android/gradlew` を実行
  - `:app:testDebugUnitTest` と `:app:assembleDebug` をPR/Pushで検証
- Android辞書seed導入の最小実装を追加
  - `scripts/export_android_seed.py` を追加（JMdict語彙→`anagram_seed.tsv` 変換）
  - `android/app/src/main/assets/anagram_seed.tsv` を追加（seed語彙）
  - `AssetSeedEntryLoader` を追加（初回起動時のseed取込）
  - `AssetSeedEntryLoaderTest` を追加（TSV parse検証）
- Android向け辞書変換ツールを拡張
  - `scripts/export_android_room_db.py` を追加（JMdict XML(.gz) → Room互換SQLite）
  - `tests/test_export_android_room_db.py` を追加（XML/gzip入力の変換検証）
- Androidアプリ化に向けたドキュメント整備（Issue #14）
  - AGENTS.md: Android版の技術スタック、ディレクトリ構造案、コーディング規約、ビルドコマンド、AnagramEntryスキーマ設計
  - prompt.md: Android版の移植対象ロジック、辞書運用方針、アーキテクチャ設計、想定機能一覧
  - README.md: Androidアプリ版（計画中）セクション追加
  - .gitignore: Android/Kotlin/Gradle関連の除外パターン追加
- tasks.md 作成（開発進捗管理）
- CHANGELOG.md 作成（変更履歴管理）
- AGENTS.md に開発ルール追加（mainブランチ保護、日本語義務、ドキュメント更新義務）

### Changed

- JMdictライセンス表記を CC BY-SA 3.0 → CC BY-SA 4.0 に更新（最新版準拠）
- `MainScreen` のエラーメッセージ表示でKotlinコンパイルエラーが出ないよう null 判定を調整
- Androidビルドをグローバルgradle依存から Gradle Wrapper（`./gradlew`）中心に更新
- デモデータ投入完了前の検索でも候補取得できるよう、`MainViewModel` の初期ロード待機を追加
- `AnagramEntry` に `sorted_key + word` の一意制約を追加し、`INSERT IGNORE` の重複抑止を有効化
- `AnagramDao.count()` の戻り値を `Long` に変更
- `MainViewModel` のDB初期化失敗時にエラーメッセージをUIへ反映
- 候補一覧が増えても閲覧できるよう `MainScreen` を縦スクロール対応
- `MainViewModelTest` を追加し、preload待機・連続入力時の最新結果反映・preload失敗時の挙動を検証
- `HiraganaNormalizerTest` を拡張し、Python版 `tests/test_normalize.py` 相当ケースでの一致検証を追加
- Roomスキーマ差分での起動クラッシュを回避するため、`AnagramDatabase` を version 2 に更新し、`Migration(1,2)` で重複解消と一意インデックス付与を実施
- `MainScreen` を `MainScreenContent` に分離し、Compose UIテストから状態注入できる構成へ更新
- `MainScreenTest`（androidTest）を追加し、入力→候補表示→エラー表示の最小E2Eを検証
- `MainViewModel` の初期投入をデモ固定から seed asset 優先方式へ変更（seed未配置時のみデモ投入）
- `scripts/export_android_seed.py` の `--max-len` デフォルトを 8 に変更し、`anagram_seed.tsv` を `max-len=8` で再生成（154,387件 / 約5.6MB）
- seedサイズシミュレーション結果に基づき、文字数制限（`--max-len`）中心の運用方針を README / AGENTS に反映
- GitHub Actions CI に Android UIテスト（`reactivecircus/android-emulator-runner` + `:app:connectedDebugAndroidTest`）ジョブを追加
- `MainScreen` に「辞書クレジット」ダイアログを追加し、JMdictライセンス表示をアプリ内から確認可能に更新
- `MainViewModel` の初回seed投入で計測ログを追加し、`source / total / inserted / elapsedMs` をUI状態に保持するよう更新
- seed投入時間の比較計測（`max-len=8`: 約584ms / `max-len=10`: 約712ms、ローカルSQLite測定）を実施し、運用上限を `max-len=8` 継続に更新
- `MainScreen` の候補をタップして詳細ダイアログを開けるように更新（読み表示 + 漢字/意味はプレースホルダ）
- `MainScreenTest` に候補詳細ダイアログ表示のUIテストを追加
- `MainActivity` / `MainScreen` にライト/ダークテーマ切替ボタンを追加し、Material 3 の `colorScheme` を切り替え可能に更新
- `MainScreenTest` にテーマ切替UIテストを追加
- `ThemePreferenceStore`（DataStore Preferences）を追加し、テーマ切替状態を再起動後も維持するよう更新
- `AssetCandidateDetailLoader` と `candidate_detail_seed.tsv` を追加し、候補詳細ダイアログで漢字/意味の実データ表示を可能に更新（未収録語はプレースホルダ）
- `MainScreenTest` の候補詳細検証を実データ表示（林檎 / apple）ベースへ更新
- `MainViewModel` に入力履歴（最新10件・重複は先頭へ寄せる）を追加し、`MainScreen` で履歴表示と再入力を可能に更新
- `MainViewModelTest` / `MainScreenTest` に入力履歴のテストを追加
- `InputHistoryStore`（DataStore Preferences）を追加し、入力履歴（最新10件）を再起動後も保持するよう更新
- `MainViewModelTest` に入力履歴の復元/永続化テストを追加
- `SettingsDataStore` を追加し、`ThemePreferenceStore` と `InputHistoryStore` が同一 DataStore インスタンスを共有するよう更新
- GitHub Actions CI の Android Build ジョブで `app-debug.apk` を artifact としてアップロードするよう更新
- `MainScreen` の入力履歴を折りたたみ表示に変更し、表示/非表示トグルで必要時のみ展開できるよう更新
- `AndroidManifest.xml` に `android:icon` / `android:roundIcon` を追加し、`asset/AnagramAnalyzerICON.png` をランチャーアイコンとして適用
- `MainScreen` に設定ダイアログを追加し、文字数範囲（最小/最大）設定・テーマ切替・追加辞書ダウンロード項目（準備中表示）を実装
- `SearchSettingsStore`（DataStore Preferences）を追加し、文字数範囲設定を永続化
- `MainViewModel` に文字数範囲バリデーションを追加し、設定範囲外入力でエラー表示するよう更新
- `AssetAdditionalSeedEntryLoader` と `anagram_additional_seed.tsv` を追加し、設定ダイアログの「追加辞書をダウンロード」から追加seedをDBへ適用できるよう更新
- 追加辞書適用中のボタン無効化・適用結果メッセージ（適用件数/最新/失敗）を `MainViewModel` / `MainScreen` に追加
- `MainViewModelTest` / `MainScreenTest` を更新し、追加辞書適用の成功・失敗・UI表示を検証
- `MainScreen` の候補詳細をダイアログ表示から専用画面表示へ変更し、戻るボタンで検索画面へ戻れるよう更新
- `MainScreenTest` の候補詳細UIテストを詳細画面表示と戻る操作の検証に更新
- `CandidateDetailLoader` を拡張し、`candidate_detail_seed.tsv` 未収録語はオンライン（Jisho API）からオンデマンド取得して `candidate_detail_cache`（Room）へ保存するよう更新
- `AnagramDatabase` を version 3 へ更新し、`candidate_detail_cache` テーブル（`word`/`kanji`/`meaning`/`updated_at`）を追加
- `MainScreen` の候補詳細画面に「詳細を取得/再取得」導線と取得中・失敗表示を追加
- `AssetSeedEntryLoader` を更新し、`anagram_seed.db`（Room互換SQLite）を優先読込し、未同梱時は `anagram_seed.tsv` へフォールバックする初回投入導線に変更
- Android `app/build.gradle.kts` に release 署名設定を追加し、`ANDROID_SIGNING_*` 環境変数（または Gradle Property）から keystore/alias/password を読込可能に更新
- GitHub Actions に `Android Release` ワークフローを追加し、署名済み `app-release.apk` を artifact と GitHub Release asset の両方で配布可能に更新
- `Android Release` ワークフローを改善し、`workflow_dispatch` でタグ未指定時に自動タグを作成して配布できるよう更新
- `Android Release` ワークフローを改善し、自動タグ push による二重実行（dispatch + push）を防ぐガードを追加
- GitHub Actions CI の Android処理を `Android Unit Test` / `Android Build` / `Android UI Test` に分割し、ジョブ依存を減らして並列実行できるよう更新
- GitHub Actions CI の AndroidジョブのGradleキャッシュ方式を `actions/setup-java` の `cache: gradle` に統一し、PRでのコールドビルドを抑制
- GitHub Actions CI から Android UIテストを分離し、`Android UI Tests` ワークフロー（`pull_request` / `workflow_dispatch` / `schedule`）へ移行
- `Android UI Tests` ワークフローで `androidTest` の `*Test.kt` をクラス単位2シャードで並列実行し、失敗時にシャード別レポート artifact と再現コマンドを出力するよう更新
- GitHub Actions `CI` に `dorny/paths-filter`（commit SHA固定）ベースの差分判定を追加し、PR時はAndroid関連変更（`android/**` と関連workflow）に限定して `Android Unit Test` / `Android Build` を実行するよう更新
- Android CI（`Android Unit Test` / `Android Build` / `Android UI Tests`）で Gradle Configuration Cache を有効化（`--configuration-cache` + `android/.gradle/configuration-cache` の保存・復元）し、ローカル連続実行の `testDebugUnitTest --dry-run --no-daemon` 計測で 6.69s → 4.10s（再利用時、約39%短縮）を確認
- Android `MainActivity` / `MainScreen` のCompose UIを更新し、カスタムColorScheme・グラデーション背景・カードレイアウト・カラーボタンで視認性を改善
- Android `MainScreen` の候補表示を最大50件 + 残件数表示に最適化し、大量候補時のCompose描画負荷を軽減
- Android `MainScreen` の上部左右に `charactor1.png` / `charactor2.png`、下部に `spot-illustration.png`（`spot_illustration.png`）を配置
- Android `MainActivity` の `ColorScheme` をPastelパレットへ更新（Primary: ピンク `#FF8AAE` / Secondary: ミント `#6EDDD3` / Tertiary: ラベンダー `#C39BFF` / 背景: アイボリー `#FFF8E7`）
- Android `MainScreen` の装飾イラスト3点で `contentDescription = null` を設定し、スクリーンリーダーが不要読み上げしないようアクセシビリティを改善
- `Android UI Tests` ワークフローの `android/.gradle/configuration-cache` キャッシュキーに `github.sha` を追加し、古いコミットのConfiguration Cache再利用で発生する `:app:mergeDebugAndroidTestAssets`（AAR欠損）失敗を回避
- Android `MainScreenTest` の「候補詳細画面でシステム戻るキー操作で戻れる」を `Espresso.pressBack()` から `onBackPressedDispatcher` 呼び出しへ変更し、CI環境での `ComposeTimeoutException` 発生を抑制
- 更新済み `asset/AnagramAnalyzerICON.png` をもとに Android ランチャーアイコン（`mipmap-*/ic_launcher.png` / `ic_launcher_round.png`）を再生成し、密度別サイズ（48/72/96/144/192px）へ正規化
- AndroidShell向け丸形アイコン素材の更新に合わせて、`asset/AnagramAnalyzerICON.png` から Android ランチャーアイコンを再生成

## [0.1.0] - 2026-02-07

### Added

- Python CLI版の初期実装
  - `anagram build`: 辞書からアナグラムインデックスを構築
  - `anagram solve`: アナグラム候補を検索
  - `anagram doctor`: 環境診断
- ひらがな正規化・アナグラムキー生成（normalize.py）
- SQLiteアナグラムインデックス管理（index.py）
- JMdict語彙抽出（lexicon/jmdict.py）
- テストスイート（test_normalize, test_index, test_integration）
- CI/CD（GitHub Actions: lint + test マトリクス Python 3.10〜3.13）
- pre-commit 設定（ruff, basedpyright, pytest）
- README.md、AGENTS.md、prompt.md
