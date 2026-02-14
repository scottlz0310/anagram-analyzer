# anagram-analyzer

ひらがなの文字列を並べ替えて作れる**日本語の単語候補**を返すCLIツールです。

## 特徴

- **アナグラム索引方式** — n! の総当たりではなく、辞書側を索引化して高速検索
- **軽量** — 辞書データは同梱せず、必要に応じて別途インストール
- **Termux対応** — モバイル環境でも快適に動作
- **ネタバレ配慮** — spoiler オプションで段階的に候補を表示

## インストール

### 基本インストール

```bash
uv pip install -e .
```

### 辞書のインストール

本ツールは辞書データを同梱していません。アナグラム検索を行うには、辞書パッケージを別途インストールしてください：

```bash
uv pip install -e ".[dict]"
```

または、既にインストール済みの場合：

```bash
uv pip install jamdict jamdict-data
```

## 使い方

### 1. 辞書インデックスの構築

```bash
anagram build
```

| オプション | 説明 | デフォルト |
|------------|------|------------|
| `--min-len N` | 最小文字数 | 2 |
| `--max-len N` | 最大文字数 | 20 |
| `--db PATH` | データベースファイルのパス | (後述) |
| `--force` | 既存DBを上書き | - |

### 2. アナグラム検索

```bash
anagram solve <ひらがな文字列>
```

例：
```bash
anagram solve "りんご"
anagram solve "ほおかにくん"
```

| オプション | 説明 | デフォルト |
|------------|------|------------|
| `--top N` | 上位N件を表示 | 全件 |
| `--spoiler MODE` | ネタバレ表示モード | `full` |
| `--db PATH` | データベースファイルのパス | (後述) |

#### spoiler モード

| モード | 説明 |
|--------|------|
| `off` | 候補を非表示（件数のみ） |
| `hint` | 最初の1文字のみ表示 |
| `full` | 全て表示 |

### 3. 環境診断

```bash
anagram doctor
```

データベースの存在確認、辞書パッケージの確認などを行います。

## データの保存場所

アナグラムインデックス（SQLite）は以下の場所に保存されます：

| OS | パス |
|----|------|
| Linux | `~/.cache/anagram-cli/anagram_index.db` |
| macOS | `~/Library/Caches/anagram-cli/anagram_index.db` |
| Windows | `%LOCALAPPDATA%\anagram-cli\Cache\anagram_index.db` |

`--db` オプションで任意のパスを指定することも可能です。

---

## 開発者向け情報

### 開発環境セットアップ

```bash
# 開発用依存関係をインストール
uv pip install -e ".[dev,dict]"

# pre-commit フックをインストール
pre-commit install
pre-commit install --hook-type pre-push
```

### Lint / 型チェック

```bash
# Ruff (linter & formatter)
ruff check src/
ruff format src/

# 型チェック
basedpyright src/

# pre-commit で全チェック実行
pre-commit run --all-files
```

### テスト実行

```bash
# 基本
pytest

# カバレッジ付き
pytest --cov=anagram_cli

# 詳細出力
pytest -v
```

---

## ライセンス

### 本ソフトウェア

MIT License — 詳細は [LICENSE](LICENSE) を参照してください。

### 辞書データについて

本ツールは辞書データを**同梱していません**。ユーザーが別途インストールする辞書パッケージには、それぞれ固有のライセンスが適用されます。

- **jamdict-data**: JMdict 等の辞書データを含みます
- **JMdict**: Electronic Dictionary Research and Development Group により [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0/) 等でライセンスされています

辞書データを利用・再配布する際は、各パッケージおよび元データのライセンス条件に従ってください。詳細は以下を参照：

- [JMdict Project](https://www.edrdg.org/wiki/index.php/JMdict-EDICT_Dictionary_Project)
- [jamdict-data PyPI](https://pypi.org/project/jamdict-data/)

---

## Androidアプリ版（初期実装中）

本プロジェクトでは、CLI版のコアロジックを移植した **Androidネイティブアプリ** を段階的に実装中です。

### 技術スタック

- **言語**: Kotlin
- **UI**: Jetpack Compose
- **データ**: Room (SQLite) + DataStore
- **辞書**: seed TSV（JMdict由来データを段階導入）

### 現在できること（手動テスト可能）

- ひらがな/カタカナ入力の正規化
- アナグラムキー生成
- Room（ローカルDB）での候補検索と候補表示（seed辞書データ）
- 候補タップで詳細ダイアログを表示（読み表示、漢字/意味は暫定プレースホルダ）
- ライト/ダークテーマの切替（Material 3）
- アプリ内の「辞書クレジット」ダイアログで JMdict ライセンス表記を確認

### 辞書seedの更新（開発者向け）

`jamdict` / `jamdict-data` を利用して、Android用 seed TSV を生成できます。

```bash
uv run python scripts/export_android_seed.py \
  --output android/app/src/main/assets/anagram_seed.tsv \
  --min-len 2 \
  --max-len 8
```

- 推奨運用値は `--max-len 8`（約154,387件 / 約5.6MB）です。
- `max-len 10` 以上はサイズ増分に対する語彙増分が小さいため、まず `8` を基準に運用します。
- ローカルSQLiteでの初回投入シミュレーションでは `max-len 8` が約584ms、`max-len 10` が約712ms でした（`8` を継続採用）。

### 手動テスト手順（Android）

```bash
cd android
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.anagram.analyzer/.MainActivity
# 初回投入ログ確認（任意）
adb logcat -d -s AnagramPreload
```

確認例：
- `りんご` を入力 → キー `ごりん`、候補に `りんご`
- `リンゴ` を入力 → ひらがなに正規化され同様に候補表示
- `abc` を入力 → エラーメッセージ表示
- 候補（例: `・りんご`）をタップ → 候補詳細ダイアログを表示
- `テーマ: ライト/ダーク` ボタンをタップ → テーマ表示が切り替わる
- `辞書クレジット` ボタン → JMdict の CC BY-SA 4.0 表記ダイアログを表示
- 起動直後の logcat に `preload source=... total=... inserted=... elapsedMs=...` が出力される

### 今後の機能

詳細は [Issue #14](https://github.com/scottlz0310/anagram-analyzer/issues/14) を参照してください。
