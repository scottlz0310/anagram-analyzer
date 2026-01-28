# AGENTS.md - anagram-analyzer 開発ガイド

このドキュメントはAIコーディングエージェント向けに、プロジェクトの構造と開発方針をまとめたものです。

## プロジェクト概要

**anagram-analyzer** は、ひらがな文字列を並べ替えて作れる日本語の単語候補を返すCLIツールです。

### 主な特徴

- **アナグラム索引方式**: n!の総当たりではなく、辞書側を索引化して高速検索
- **辞書データ非同梱**: ライセンス配慮のため、ユーザーが別途インストール
- **Termux対応**: モバイル環境でも快適に動作

## 技術スタック

| カテゴリ | 技術 |
|---------|------|
| 言語 | Python 3.10+ |
| CLI フレームワーク | Typer |
| 表示 | Rich |
| データストア | SQLite |
| 辞書ソース | jamdict + jamdict-data (JMdict) |
| パッケージ管理 | uv (推奨) |
| テスト | pytest |

## ディレクトリ構造

```
anagram-analyzer/
├── src/anagram_cli/           # メインパッケージ
│   ├── __init__.py
│   ├── cli.py                 # Typerエントリポイント（コマンド定義）
│   ├── config.py              # キャッシュディレクトリ等の設定
│   ├── index.py               # SQLiteアナグラムインデックス管理
│   ├── normalize.py           # ひらがな正規化・キー生成
│   └── lexicon/
│       ├── __init__.py
│       └── jmdict.py          # jamdict からの語彙抽出
├── tests/                     # テストスイート
│   ├── __init__.py
│   ├── test_normalize.py      # 正規化モジュールのテスト
│   ├── test_index.py          # インデックスモジュールのテスト
│   └── test_integration.py    # 統合テスト
├── pyproject.toml             # プロジェクト設定
├── prompt.md                  # 開発仕様書
└── README.md                  # ユーザー向けドキュメント
```

## モジュール詳細

### `normalize.py` - 正規化モジュール

**責務**: 入力文字列の正規化とアナグラムキー生成

| 関数 | 説明 |
|------|------|
| `normalize_hiragana(s)` | NFKC正規化、空白除去、カタカナ→ひらがな変換 |
| `katakana_to_hiragana(s)` | カタカナをひらがなに変換 |
| `is_hiragana(char)` | 文字がひらがなか判定 |
| `is_all_hiragana(s)` | 文字列が全てひらがなか判定 |
| `anagram_key(s)` | ソート済み文字列（検索キー）を生成 |

**例外**: `NormalizationError` - ひらがな以外の文字が含まれる場合

### `index.py` - インデックスモジュール

**責務**: SQLiteによるアナグラムインデックスの管理

**クラス**: `AnagramIndex`

| メソッド | 説明 |
|----------|------|
| `init_db()` | データベース初期化 |
| `add(key, word)` | キーと単語のペアを追加 |
| `add_batch(items)` | バッチ追加（高速） |
| `lookup(key)` | キーに対応する単語を検索 |
| `count()` | 登録エントリ数取得 |
| `clear()` | 全データ削除 |

**スキーマ**:
```sql
CREATE TABLE anagram_index (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    key TEXT NOT NULL,
    word TEXT NOT NULL,
    UNIQUE(key, word)
);
CREATE INDEX idx_key ON anagram_index(key);
```

### `lexicon/jmdict.py` - 辞書抽出モジュール

**責務**: jamdict から語彙を抽出・フィルタ

| 関数 | 説明 |
|------|------|
| `check_jamdict_available()` | jamdictが利用可能か確認 |
| `check_jamdict_data_available()` | jamdict-dataが利用可能か確認 |
| `iter_words(min_len, max_len, ...)` | 語彙をイテレート |

### `cli.py` - CLIエントリポイント

**コマンド**:

1. **`anagram build`** - 辞書インデックス構築
   - `--min-len N`: 最小文字数（デフォルト: 2）
   - `--max-len N`: 最大文字数（デフォルト: 20）
   - `--db PATH`: データベースパス
   - `--force`: 既存DB上書き

2. **`anagram solve <letters>`** - アナグラム検索
   - `--top N`: 上位N件表示
   - `--spoiler [off|hint|full]`: ネタバレモード
   - `--db PATH`: データベースパス

3. **`anagram doctor`** - 環境診断

### `config.py` - 設定モジュール

| 関数 | 説明 |
|------|------|
| `get_cache_dir()` | ユーザーキャッシュディレクトリ取得 |
| `get_default_db_path()` | デフォルトDBパス取得 |

## 開発コマンド

```bash
# 依存関係インストール（開発用）
uv pip install -e ".[dev]"

# 辞書データインストール
uv add jamdict jamdict-data

# テスト実行
pytest

# カバレッジ付きテスト
pytest --cov=anagram_cli

# CLIテスト
anagram doctor
anagram build
anagram solve "りんご"
```

## アルゴリズム解説

### アナグラムキー方式

1. 正規化した単語 `w` に対し `key = ''.join(sorted(w))` を計算
2. `key -> [w...]` のマップ（SQLiteインデックス）を構築
3. 検索時も入力を同じkeyに変換し、1回のDB lookupで候補取得

**例**:
- 「りんご」→ キー「ごりん」
- 「ごりん」→ キー「ごりん」（同じ）
- DBで「ごりん」を検索 → 「りんご」が見つかる

## コーディング規約

- **型ヒント**: 全ての関数に型ヒントを付ける
- **docstring**: 主要な関数・クラスにdocstringを記述
- **例外処理**: 適切なカスタム例外を使用
- **テスト**: 新機能には対応するテストを追加

## 注意事項

### 辞書データについて

- 辞書データ（jamdict-data）は **同梱しない**
- JMdictは **CC BY-SA 3.0** ライセンス
- ユーザーが自分でインストールする方式

### データベースについて

- 生成されたDBファイルは `.gitignore` に含める
- デフォルト保存先: `platformdirs.user_cache_dir()`

## 今後の拡張予定

1. **頻度ランキング**: `wordfreq` 導入で候補を頻度順にソート
2. **品詞フィルタ**: 名詞のみに絞る機能
3. **ソートオプション**: `--sort alpha|freq`
4. **部分一致検索**: 一部の文字だけ使った候補も表示

## トラブルシューティング

### jamdict関連エラー

```bash
# jamdict と jamdict-data を両方インストール
uv add jamdict jamdict-data

# 診断コマンドで確認
anagram doctor
```

### テスト失敗時

```bash
# 詳細出力で実行
pytest -v

# 特定テストのみ実行
pytest tests/test_normalize.py -v
```
