## プロジェクト目的

- ひらがな入力からアナグラム候補を返す **Androidアプリ** を提供する。
- 検索は総当たりではなく、辞書側の索引（アナグラムキー）で高速化する。
- 辞書はJMdict系データを基盤にし、ローカル検索を中心に動作させる。

---

## 現在の前提

- 本番実装は Android（Kotlin + Compose + Room + DataStore）。
- 旧 Python CLI プロトタイプは削除済み。
- 辞書seed生成は `android/tools/seed-generator` の Kotlin/JVM CLI を使う。

---

## 検索仕様（Android）

1. 入力文字列を正規化
   - NFKC
   - 空白除去
   - カタカナ→ひらがな
   - ひらがな以外はエラー
2. `key = ''.join(sorted(normalized))` を生成
3. Room の `sorted_key` で完全一致検索
4. 候補一覧を表示（表示上限あり、残件数表示）

---

## 辞書/データ設計

- メイン索引: `anagram_entries`
  - `sorted_key`
  - `word`
  - `length`
  - `is_common`
- 候補詳細キャッシュ: `candidate_detail_cache`
  - `word`
  - `kanji`
  - `meaning`
  - `updated_at`

初回投入:
- `anagram_seed.db` があれば優先
- なければ `anagram_seed.tsv` から投入

---

## UI要件（現行）

- メイン画面: 入力、検索、候補一覧、設定導線
- 候補詳細画面: 読み・漢字・意味、必要時オンデマンド取得
- クイズ画面: 難易度選択、カードタップ式入力、スコア/ストリーク表示、正解/不正解表示
- 設定: テーマ切替、文字数範囲、追加seed適用
- 履歴: 最新10件を保存・再利用

---

## 補助ツール仕様（Kotlin/JVM）

### `android/tools/seed-generator`

- JMdict XML/gzip（`.xml` / `.gz`）→ `anagram_seed.tsv` / `anagram_seed.db`
- 正規化・ひらがな判定・重複除去・長さフィルタ・一般語フラグ付与を実施
- `anagram_entries` / `candidate_detail_cache` を含む Room互換SQLite を生成
- `PRAGMA user_version=5` を設定

---

## 開発コマンド

```bash
# Android Build
cd android && ./gradlew :app:assembleDebug

# Android Unit Test
cd android && ./gradlew :app:testDebugUnitTest

# Android UI Test
cd android && ./gradlew :app:connectedDebugAndroidTest

# CIメモ
# Android UI Tests（schedule含む）は安定性のため --configuration-cache を付けない

# seed TSV生成
cd android && ./gradlew :tools:seed-generator:run --args="--jmdict ~/.jamdict/data/JMdict_e.gz --out-tsv app/src/main/assets/anagram_seed.tsv --mode tsv --min-len 2 --max-len 8"

# Room DB生成
cd android && ./gradlew :tools:seed-generator:run --args="--jmdict ~/.jamdict/data/JMdict_e.gz --out-db app/src/main/assets/anagram_seed.db --mode db --min-len 2 --max-len 8 --force"
```

---

## ライセンス方針

- JMdict は CC BY-SA 4.0
- アプリ内クレジット表示を必須とする
- 辞書データの再配布条件を常に確認する

---

## 今後の拡張候補

- お気に入り機能
- オフライン完全対応の品質検証
- 検索候補の並び順改善（頻度・優先度）
- リリース導線の強化（継続配布運用）
