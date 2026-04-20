# tasks.md - 開発タスク管理

このファイルはプロジェクト全体の進捗管理に使用します。
各イテレーションの開始・完了時に更新してください。

過去の完了タスク詳細は `tasks_archive_20260421.md` を参照。

---

## 完了済みフェーズ（サマリ）

| フェーズ | 状態 | 完了内容 |
|---------|------|---------|
| 0: 基盤整備 | ✅ 完了 | Python CLI初期実装・テスト・CI・ドキュメント整備 |
| 2: Android初期構築 | ✅ 完了 | Gradleプロジェクト・Room・Hilt・Android CI |
| 3: ロジック移植 | ✅ 完了 | ひらがな正規化・アナグラムキー・Room検索・ユニットテスト |
| 8: Pythonプロトタイプ撤去 | ✅ 完了 | Python CLI・テスト・CI設定・ドキュメントを削除し Android単一実装へ |
| 9: seed生成Kotlin/JVM移行 | ✅ 完了 | tools:seed-generator 実装（Parser/Normalizer/TsvExporter/DbExporter/Main）|
| 10: Issue #81 事前リファクタ | ✅ 完了 | MainViewModel UseCase分割・画面コンポーネント切り出し |
| 11: Issue #60 クイズモード | ✅ 完了 | QuizDifficulty/QuizQuestion/GenerateQuizUseCase/QuizScoreStore/QuizViewModel/QuizScreen |
| 12: Issue #88 クイズ単語重みづけ | ✅ 完了 | JMdict re_pri → isCommon フラグ・DB version 4・一般語優先出題 |

---

## 進行中フェーズ

### フェーズ 4: UI実装（Jetpack Compose）

- [x] メイン画面・候補一覧・候補詳細画面
- [x] テーマ切替（ライト/ダーク）
- [x] UIカラー強化・イラスト配置・Pastel配色
- [x] 候補一覧50件上限制御
- [x] Compose UIテスト
- [ ] お気に入り（ブックマーク）機能

### フェーズ 5: 辞書データ対応

- [x] JMdict XML → TSV/DB 変換ツール・seed投入導線・サイズ最適化・ライセンス表示
- [x] 候補詳細オンデマンド取得・Roomキャッシュ
- [ ] オフライン完全対応の検証

### フェーズ 6: 追加機能

- [x] 入力履歴・折りたたみ表示・DataStore永続化
- [x] 設定画面（文字数範囲・テーマ切替・追加辞書DL）

### フェーズ 7: CI/CD・QA・リリース

- [x] Android UIテスト分離（2シャード）・差分判定・Configuration Cache
- [x] debug APK artifact・署名済みリリースAPKワークフロー
- [ ] Android用CI/CDパイプライン完成
- [ ] リリースビルド設定（署名、ProGuard/R8）
- [ ] Google Play Store 公開準備
- [ ] Cloudflare への配布導線デプロイ（安定化後）
- [ ] iOS対応の検討・計画策定

---

## 未着手フェーズ

### フェーズ 13: Issue #40 クイズカードタップ式入力UI

- [ ] `CharCard` データクラスを定義（`id: Int`, `char: Char`, `isPlaced: Boolean`）
- [ ] `QuizUiState` を新フィールド構成に更新（`shuffledCards` / `answerSlots` / `selectedCardId` 追加、`inputAnswer` 削除）
- [ ] `QuizViewModel` に `onCardTapped` / `onSlotTapped` を追加し `onInputAnswerChanged` を削除
- [ ] `onSubmitAnswer` を `answerSlots` から文字列組み立て→判定する方式に変更
- [ ] `GenerateQuizUseCase` が `shuffledCards: List<CharCard>` を返すよう更新
- [ ] `QuizScreen.kt` の `AnsweringSection` をカード選択UI + 解答グリッドに刷新
- [ ] 選択ハイライト（`animateColorAsState`）・グリッドハイライト（`animateDpAsState`）を実装
- [ ] 配置時バウンスアニメーション（`spring`, DampingRatioMediumBouncy）+ 触覚フィードバックを実装
- [ ] `QuizViewModelTest.kt` に新操作イベントのユニットテストを追加
- [ ] 既存の正誤判定テスト・スコアテストが通過する

### フェーズ 14: Issue #14 クイズモード拡張（タイマー・ヒント・ランキング・デコレーション）

- [ ] クイズ画面にイラストを追加（MainScreen の charactor 素材流用 or 専用イラスト）
- [ ] ビジュアル強化（パステルカラー統一・問題文グラデーション・正解/不正解アクセント）
- [ ] タイマー（30秒カウントダウン・時間切れ→不正解自動遷移）
- [ ] ヒント消費システム（一部文字公開 or 意味ヒント・スコアペナルティ）
- [ ] ランキング（セッションスコア・Room保存・上位表示）
- [ ] テーマ別カテゴリ（品詞フィルタ）※DB変更を伴うため最後

### フェーズ 15: Issue #12 クイズメインコンテンツ化・フリーミアム戦略

- [ ] クイズモードをトップ画面として前面化
- [ ] 解析モードをオプショナル（プレミアム機能）に移行
- [ ] フリーミアム設計・課金導線の設計・実装

---

## 実装推奨順序

```
#40（カードUI） → #14-ビジュアル → #14-タイマー → #14-ヒント → #14-ランキング
                                                                       ↓
                                                              #12（クイズ主役化）
                                                                       ↓
                                                           フェーズ7完成 → ストア公開
```

| 優先順 | Issue / フェーズ | 理由 |
|--------|----------------|------|
| 1 | #40 カードタップUI | クイズ体験の基盤。これが固まると #14 のビジュアル作業が自然に続く |
| 2 | #14 ビジュアル強化・イラスト | 難易度低。#40 の完成後すぐ着手可能 |
| 3 | #14 タイマー | ゲーム性向上。実装は中程度 |
| 4 | #14 ヒント・ランキング | ゲームループの深化 |
| 5 | #12 クイズ主役化 | クイズ品質が揃ってからリストラクチャー |
| 6 | フェーズ7残タスク | ストア公開準備（ProGuard・Play Store） |

---

## 進捗サマリ

| フェーズ | 状態 | 備考 |
|---------|------|------|
| 0〜3, 8〜12 | ✅ 完了 | `tasks_archive_20260421.md` 参照 |
| 4: UI実装 | 🟡 進行中 | お気に入り機能のみ未着手 |
| 5: 辞書データ | 🟡 進行中 | オフライン検証のみ未着手 |
| 6: 追加機能 | 🟡 進行中 | お気に入り機能のみ未着手 |
| 7: CI/CD・リリース | 🟡 進行中 | ProGuard・ストア公開準備が未着手 |
| 13: Issue #40 カードUI | ⬜ 未着手 | 次イテレーション最優先 |
| 14: Issue #14 クイズ拡張 | ⬜ 未着手 | #40 完了後に着手 |
| 15: Issue #12 主役化 | ⬜ 未着手 | クイズ品質確立後に着手 |
