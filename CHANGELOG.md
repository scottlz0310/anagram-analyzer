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
