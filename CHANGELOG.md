# CHANGELOG.md

このファイルはプロジェクトの変更履歴を記録します。
[Keep a Changelog](https://keepachangelog.com/ja/1.1.0/) の形式に準拠します。

## [Unreleased]

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
