## プロジェクト目的

- ひらがな入力からアナグラム候補を返す **Androidアプリ** を提供する。
- 検索は総当たりではなく、辞書側の索引（アナグラムキー）で高速化する。
- 辞書はJMdict系データを基盤にし、ローカル検索を中心に動作させる。

---

## 現在の前提

- 本番実装は Android（Kotlin + Compose + Room + DataStore）。
- 旧 Python CLI プロトタイプは削除済み。
- Pythonは辞書seed生成スクリプト用途に限定する。

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
- 設定: テーマ切替、文字数範囲、追加seed適用
- 履歴: 最新10件を保存・再利用

---

## 補助スクリプト仕様（Python）

### `scripts/export_android_seed.py`

- JMdict XML（`.xml` / `.gz`）→ `anagram_seed.tsv`
- 正規化・ひらがな判定・重複除去・長さフィルタを実施

### `scripts/export_android_room_db.py`

- JMdict XML（`.xml` / `.gz`）→ Room互換SQLite
- `anagram_entries` / `candidate_detail_cache` を生成
- `PRAGMA user_version=3` を設定

---

## 開発コマンド

```bash
# Android Build
cd android && ./gradlew :app:assembleDebug

# Android Unit Test
cd android && ./gradlew :app:testDebugUnitTest

# Android UI Test
cd android && ./gradlew :app:connectedDebugAndroidTest

# seed TSV生成
python scripts/export_android_seed.py --xml ~/.jamdict/data/JMdict_e.gz --output android/app/src/main/assets/anagram_seed.tsv --min-len 2 --max-len 8

# Room DB生成
python scripts/export_android_room_db.py --xml ~/.jamdict/data/JMdict_e.gz --output android/app/src/main/assets/anagram_seed.db --min-len 2 --max-len 8 --force
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
