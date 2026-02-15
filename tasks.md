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

## フェーズ 1: Pythonロジック抽出・仕様明確化

- [ ] 正規化ロジックの仕様書作成（入出力・エッジケース一覧）
- [ ] アナグラムキー生成の仕様書作成
- [ ] インデックス検索の仕様書作成（スキーマ・クエリパターン）
- [ ] JMdict語彙抽出のフィルタ条件の明文化
- [ ] Python版とAndroid版の動作一致テストケース設計

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
- [x] 候補詳細画面（漢字表記・意味表示）
- [x] 候補詳細画面の最小実装（読み表示 + 漢字/意味プレースホルダ、旧: ダイアログ）
- [x] 候補詳細画面で漢字/意味のseed実データ表示（未収録語はプレースホルダ、旧: ダイアログ）
- [x] テーマ設定（Material 3: ライト/ダーク切替）
- [x] UIテスト（Compose Testing）

## フェーズ 5: 辞書データ対応

- [ ] JMdict XML → Room DB 変換スクリプト/ツール作成
- [ ] Asset同梱方式の実装（初回起動時インポート）
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
- [ ] Android用CI/CDパイプライン完成
- [ ] リリースビルド設定（署名、ProGuard/R8）
- [ ] Google Play Store 公開準備
- [ ] iOS対応の検討・計画策定

---

## 進捗サマリ

| フェーズ | 状態 | 備考 |
|---------|------|------|
| 0: 基盤整備 | ✅ 完了 | Python CLI版 + ドキュメント整備済み |
| 1: ロジック抽出 | 🔲 未着手 | |
| 2: Android初期構築 | ✅ 完了 | Room最小DB構成 + Hilt DI基盤 + Android CIジョブ追加 + 起動クラッシュ対策まで完了 |
| 3: ロジック移植 | ✅ 完了 | normalize移植 + Room検索接続 + Python版との一致テスト追加 |
| 4: UI実装 | 🟡 進行中 | メイン画面実装、候補詳細画面（漢字/意味のseed実データ表示 + 未収録語オンデマンド取得導線）、ライト/ダーク切替、ランチャーアイコン適用、手動テスト可能な最小フロー、Compose UIテスト追加 |
| 5: 辞書データ | 🟡 進行中 | seed変換/取込導線 + サイズ最適化（`max-len=8`）+ ライセンス表示 + 初回インポート計測ログ + 8/10投入時間比較 + 候補詳細オンデマンド取得/キャッシュまで実施 |
| 6: 追加機能 | 🟡 進行中 | DataStore によるテーマ設定永続化 + 入力履歴永続化 + 履歴折りたたみ表示 + 設定画面（文字数範囲/テーマ/追加辞書DL適用）まで実装 |
| 7: CI/CD・リリース | 🟡 進行中 | Android UIテスト（エミュレータ）+ debug APK artifact アップロードを追加 |
