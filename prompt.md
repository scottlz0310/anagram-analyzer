## プロジェクト目的

* ひらがなの文字列を並べ替えて作れる **一語の普通名詞**候補を返すCLI。
* 誘導（意味的プライミング）に引っ張られないため、**総当たりではなく辞書側の索引化**で解く。
* 公開はソースのみ。**辞書データは同梱しない**（ユーザーがuv等で導入）。

---

## 主要方針

### 解法（性能設計）

* **アナグラム索引**方式：

  * 正規化した単語 `w` に対し `key = ''.join(sorted(w))`
  * `key -> [w...]` のマップを作る
  * 入力も同じkeyを作り、1回の検索で候補を取得
* n! の総当たりはしない（Termuxでも快適）。

### 辞書ソース

* 初期：**Jamdict + jamdict-data（JMdict系）**を推奨（導入容易、語彙抽出しやすい）。
* ライセンス配慮：辞書データは同梱しない。READMEで導入方法とライセンス注意を記載。

---

## CLI仕様（Typer + Rich）

コマンドは3つで開始。

1. `anagram build`

* 辞書から語彙抽出 → ひらがな名詞中心にフィルタ → SQLiteへ索引化
* オプション案：

  * `--min-len / --max-len`
  * `--noun-only`（最初はデフォルトONでもよい）
  * `--db PATH`（省略時はキャッシュ）

2. `anagram solve <letters>`

* 入力を正規化（NFKC/空白除去/ひらがな限定）→ key生成 → DB lookup
* Richで候補表示
* オプション案：

  * `--top N`
  * `--spoiler off|hint|full`（段階表示で学習用途に寄せる）
  * `--sort alpha|freq`（freqは後回しでもOK）

3. `anagram doctor`

* DB存在確認、辞書依存（jamdict-data等）の存在確認、キャッシュ位置表示

---

## データ/ストレージ設計

* DB：SQLite（`key TEXT`, `word TEXT`, index on key）
* 配置：`platformdirs` の user cache dir

  * Termuxでも扱いやすい（必要なら外部ストレージは `termux-setup-storage` でユーザーが対応）
* 生成物（DB）は `.gitignore`（コミットしない）

---

## モジュール構成案（拡張前提）

```
src/anagram_cli/
  cli.py          # typer entry
  normalize.py    # NFKC + ひらがな制約 + key生成
  index.py        # SQLiteアクセス（init/add/lookup）
  lexicon/
    jmdict.py     # jamdictから語彙を抽出・フィルタ
  ui.py           # rich table/format（任意）
config.py         # cache dir
```

---

## 実装ステップ（順番）

1. **normalize.py**

   * `normalize_hiragana(s)`：NFKC、空白除去、ひらがな以外は例外（初期は厳格）
   * `anagram_key(s)`：ソートしてkey化

2. **index.py**

   * SQLite schema作成
   * `add(key, word)` / `lookup(key)` / `commit()`

3. **lexicon/jmdict.py**

   * jamdict/jamdict-dataから見出し語を抽出
   * フィルタ：

     * ひらがなのみ
     * 長さ範囲
     * 名詞優先（できる範囲で。難しければ初期は“ひらがな語彙”だけでも可）
   * `iter_words(...) -> Iterator[str]`

4. **cli.py**

   * `build`：抽出→索引化（Progress表示）
   * `solve`：lookup→Rich Table
   * `doctor`：状態表示

5. **（任意）ランキング**

   * `wordfreq`導入で頻度スコア順に並べる（候補が多い場合の“正解っぽさ”向上）

6. **テスト**

   * 正規化（空白/全角など）
   * key生成
   * index add/lookup
   * 小さな語彙で build→solve の統合テスト

---

## 公開時のREADMEに入れる要点

* 本ツールは**汎用アナグラム支援**で、特定コンテンツの問題セットは含まない
* 辞書データは同梱しない（ユーザーが `uv add jamdict jamdict-data` などで導入）
* ネタバレ配慮として `--spoiler` を用意
* ライセンス注意（JMdict系はCC BY-SA等の条件があるため、データ再配布しない方針）
* 現段階はローカル開発でgithub公開は未定（要望あれば検討）
