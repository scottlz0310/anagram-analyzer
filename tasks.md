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
- [x] Roomスキーマ差分による起動クラッシュ対策（DB version更新 + 破壊的マイグレーション）
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
- [ ] 候補詳細画面（漢字表記・意味表示）
- [ ] テーマ設定（Material 3）
- [ ] UIテスト（Compose Testing）

## フェーズ 5: 辞書データ対応

- [ ] JMdict XML → Room DB 変換スクリプト/ツール作成
- [ ] Asset同梱方式の実装（初回起動時インポート）
- [ ] 辞書データサイズ最適化検討
- [ ] ライセンスクレジット表記（About画面）

## フェーズ 6: 追加機能

- [ ] 入力履歴機能
- [ ] お気に入り（ブックマーク）機能
- [ ] 設定画面（文字数範囲、テーマ切替）
- [ ] DataStore による設定永続化
- [ ] オフライン完全対応の検証

## フェーズ 7: CI/CD・QA・リリース

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
| 4: UI実装 | 🟡 進行中 | メイン画面実装、手動テスト可能な最小フローを確認 |
| 5: 辞書データ | 🔲 未着手 | |
| 6: 追加機能 | 🔲 未着手 | |
| 7: CI/CD・リリース | 🔲 未着手 | |
