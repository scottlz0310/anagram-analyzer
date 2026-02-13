# AGENTS.md - anagram-analyzer 開発ガイド

このドキュメントはAIコーディングエージェント向けに、プロジェクトの構造と開発方針をまとめたものです。

## プロジェクト概要

**anagram-analyzer** は、ひらがな文字列を並べ替えて作れる日本語の単語候補を返すツールです。
現在 **Python CLI版**（プロトタイプ）と **Android版**（計画中）の2つのプラットフォームで展開します。

### 主な特徴

- **アナグラム索引方式**: n!の総当たりではなく、辞書側を索引化して高速検索
- **辞書データ非同梱**: ライセンス配慮のため、ユーザーが別途インストール（CLI版）
- **Termux対応**: モバイル環境でも快適に動作（CLI版）
- **Androidネイティブ対応**: Kotlin + Jetpack Compose によるモバイルアプリ（計画中）

### プラットフォーム構成

| プラットフォーム | 状態 | 位置 |
|-----------------|------|------|
| Python CLI版 | ✅ 実装済み | `src/anagram_cli/` |
| Android版 | 🚧 計画中 | `android/` （予定） |

## 技術スタック

### Python CLI版

| カテゴリ | 技術 |
|---------|------|
| 言語 | Python 3.10+ |
| CLI フレームワーク | Typer |
| 表示 | Rich |
| データストア | SQLite |
| 辞書ソース | jamdict + jamdict-data (JMdict) |
| パッケージ管理 | uv (推奨) |
| テスト | pytest |

### Android版（計画中）

| カテゴリ | 技術 |
|---------|------|
| 言語 | Kotlin |
| UI | Jetpack Compose |
| アーキテクチャ | MVVM + Clean Architecture |
| データストア | Room (SQLite) |
| 設定管理 | DataStore |
| 辞書ソース | JMdict (CC BY-SA 4.0) |
| ビルド | Gradle (Kotlin DSL) |
| テスト | JUnit, Espresso |
| CI | GitHub Actions |

## ディレクトリ構造

### Python CLI版（現行）

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

### Android版（予定）

```
android/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/anagram/analyzer/
│   │   │   │   ├── data/               # データ層
│   │   │   │   │   ├── db/             # Room DB・DAO・Entity
│   │   │   │   │   ├── repository/     # リポジトリ実装
│   │   │   │   │   └── datastore/      # DataStore設定管理
│   │   │   │   ├── domain/             # ドメイン層
│   │   │   │   │   ├── model/          # ドメインモデル
│   │   │   │   │   ├── usecase/        # ユースケース
│   │   │   │   │   └── repository/     # リポジトリインターフェース
│   │   │   │   ├── ui/                 # プレゼンテーション層
│   │   │   │   │   ├── screen/         # Compose画面
│   │   │   │   │   ├── component/      # 再利用可能なUI部品
│   │   │   │   │   ├── theme/          # テーマ定義
│   │   │   │   │   └── viewmodel/      # ViewModel
│   │   │   │   └── di/                 # DI設定
│   │   │   ├── assets/                 # JMdict辞書データ
│   │   │   └── res/                    # リソース
│   │   ├── test/                       # ユニットテスト
│   │   └── androidTest/                # UIテスト (Espresso)
│   └── build.gradle.kts
├── build.gradle.kts                    # ルートビルド設定
├── settings.gradle.kts
└── gradle.properties
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

### Python CLI版

```bash
# 依存関係インストール（開発用）
uv pip install -e ".[dev]"

# 辞書データインストール
uv add jamdict jamdict-data

# テスト実行
pytest

# カバレッジ付きテスト
pytest --cov=anagram_cli

# Lint・型チェック
ruff check src/
ruff format src/
basedpyright src/

# CLIテスト
anagram doctor
anagram build
anagram solve "りんご"
```

### Android版（予定）

```bash
# ビルド
cd android && ./gradlew assembleDebug

# ユニットテスト
cd android && ./gradlew test

# UIテスト（エミュレータ/実機必要）
cd android && ./gradlew connectedAndroidTest

# Lint
cd android && ./gradlew lint
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

## 開発ルール（必須）

### ブランチ運用

- **mainブランチへの直接pushは禁止**。すべての変更はfeatureブランチからPull Requestを経由すること。
- ブランチ命名例: `feature/android-init`, `fix/normalize-edge-case`, `docs/update-agents`

### 言語

- **Issue、PR、コミットメッセージ、コードコメント、ドキュメントはすべて日本語**で記述すること。
- 変数名・関数名・クラス名など識別子は英語で構わない。

### ドキュメント更新義務

イテレーション（PR単位の作業）ごとに、以下のドキュメントを**必ず更新**すること：

| ドキュメント | 更新内容 |
|-------------|---------|
| `tasks.md` | 完了タスクのチェック、新規タスクの追加、進捗サマリの更新 |
| `CHANGELOG.md` | 変更内容を [Unreleased] セクションに追記 |
| `AGENTS.md` | 構造変更・新モジュール追加時にディレクトリ構造・モジュール詳細を更新 |
| `README.md` | ユーザー向け機能・使い方に変更がある場合に更新 |
| `prompt.md` | 仕様変更がある場合に更新 |

## コーディング規約

### Python CLI版

- **型ヒント**: 全ての関数に型ヒントを付ける
- **docstring**: 主要な関数・クラスにdocstringを記述
- **例外処理**: 適切なカスタム例外を使用
- **テスト**: 新機能には対応するテストを追加

### Android版（Kotlin）

- **命名規則**: Kotlin公式コーディング規約に準拠
- **責務分離**: ゴッドクラス禁止、単一責任原則（SRP）を厳守
- **アーキテクチャ**: MVVM + Clean Architecture（data / domain / ui の3層）
- **UIステート**: StateFlowベースの一方向データフロー
- **非同期処理**: Kotlin Coroutines + Flow
- **DI**: Hilt（推奨）
- **テスト**: ユニットテスト（JUnit）、UIテスト（Espresso/Compose Testing）を必須化

## 注意事項

### 辞書データについて

- 辞書データ（jamdict-data）はPython CLI版では **同梱しない**
- JMdictは **CC BY-SA 4.0** ライセンス（最新版準拠）
- Python CLI版：ユーザーが自分でインストールする方式
- Android版：JMdictフルデータをAssetにバンドル予定（XML解凍後110〜120MB、Room DB化後200〜300MB見込み）
- Android版のクレジット表記（About画面に記載必須）：
  > このアプリはElectronic Dictionary Research and Development GroupのJMdictデータを使用しています。ライセンス: CC BY-SA 4.0

### データベースについて

- 生成されたDBファイルは `.gitignore` に含める
- デフォルト保存先: `platformdirs.user_cache_dir()`

## 今後の拡張予定

### Python CLI版

1. **頻度ランキング**: `wordfreq` 導入で候補を頻度順にソート
2. **品詞フィルタ**: 名詞のみに絞る機能
3. **ソートオプション**: `--sort alpha|freq`
4. **部分一致検索**: 一部の文字だけ使った候補も表示

### Androidアプリ化（Issue #14）

開発ステップ（段階的に実施）：

1. **ロジック抽出・仕様明確化**: Python版から正規化・キー生成・検索ロジックを抽出
2. **Kotlinロジック再実装**: 正規化・アナグラムキー生成・Room DB検索をKotlinで再実装
3. **UIプロトタイプ**: Jetpack Composeで基本画面（入力・候補表示）
4. **アプリ基盤設計**: MVVM + Clean Architecture によるクラス分割
5. **辞書データ対応**: JMdictのAndroid Asset化、Room Entityへの変換
6. **機能実装**: 入力履歴、お気に入り、オフライン辞書、設定画面
7. **CI/CD・QA**: GitHub Actions でのAndroidビルド・テスト自動化
8. **iOS対応**: 要望・実績に応じて別途計画

#### Android版で移植するPythonロジック

| Python モジュール | 移植対象 | Kotlin実装先 |
|------------------|---------|-------------|
| `normalize.py` | `normalize_hiragana()`, `anagram_key()`, `katakana_to_hiragana()` | `domain/model/` |
| `index.py` | `AnagramIndex` (SQLite検索) | `data/db/` (Room DAO) |
| `lexicon/jmdict.py` | JMdict語彙抽出ロジック | `data/repository/` |

#### Android版 AnagramEntry スキーマ設計（Room Entity）

```kotlin
@Entity(indices = [Index("sorted_key"), Index("length")])
data class AnagramEntry(
    @PrimaryKey val sortedKey: String,          // ひらがな正規化→ソート済み
    val readings: String,                       // 複数読みを"|"区切り
    val kanji: String?,                         // 代表表記
    val glossSummary: String?,                   // 短い英語訳まとめ
    val entryId: Long,                          // JMdict元ID（詳細用）
    val length: Int,
    val isCommon: Boolean = false
)
```

#### Android版 想定画面構成

- **メイン画面**: テキスト入力 + 検索ボタン + 候補リスト
- **候補詳細画面**: 漢字表記・意味・JMdict情報
- **履歴画面**: 入力履歴一覧
- **お気に入り画面**: ブックマーク管理
- **設定画面**: 文字数範囲、UIテーマ切替、辞書更新

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
