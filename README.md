# anagram-analyzer

ひらがなの文字列を並べ替えて作れる**日本語の単語候補**を返すCLIツール及びAndroidアプリです。

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
- **辞書**: seed Asset（`anagram_seed.db` 優先 / `anagram_seed.tsv` フォールバック）

### 現在できること（手動テスト可能）

- ひらがな/カタカナ入力の正規化
- アナグラムキー生成
- Room（ローカルDB）での候補検索と候補表示（seed辞書データ）
- 候補タップで候補詳細画面を表示（`candidate_detail_seed.tsv` 収録語は即時表示、未収録語はオンライン辞書からオンデマンド取得してローカルキャッシュ）
- 設定ダイアログでライト/ダークテーマを切替（Material 3、DataStoreで永続化）
- 設定ダイアログで文字数範囲（最小/最大）を変更（DataStoreで永続化、範囲外入力はエラー表示）
- 設定ダイアログの追加辞書ダウンロードで `anagram_additional_seed.tsv` を適用（適用件数/最新/失敗を表示）
- 入力履歴の折りたたみ表示（最新10件、表示/非表示トグル、タップで再入力、DataStoreで永続化）
- ランチャーアイコンに `AnagramAnalyzerICON.png` を適用
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

### Room向けSQLiteの生成（開発者向け）

JMdict XML（`.xml` / `.gz`）から、Android Roomの `anagram_entries` スキーマ互換SQLiteを生成できます。

```bash
uv run python scripts/export_android_room_db.py \
  --xml ~/.jamdict/data/JMdict_e.gz \
  --output android/app/src/main/assets/anagram_seed.db \
  --min-len 2 \
  --max-len 8 \
  --force
```

- `candidate_detail_cache` テーブルと `PRAGMA user_version=3` も同時に初期化されます。
- 初回投入時は `anagram_seed.db` が同梱されていれば優先読込し、未同梱時は `anagram_seed.tsv` を利用します。

### 手動テスト手順（Android）

```bash
cd android
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.anagram.analyzer/.MainActivity
# 初回投入ログ確認（任意）
adb logcat -d -s AnagramPreload
```

GitHub Actions の `Android Build` ジョブでは `android-debug-apk` artifact として `app-debug.apk` をダウンロードできます。

確認例：
- `りんご` を入力 → キー `ごりん`、候補に `りんご`
- `リンゴ` を入力 → ひらがなに正規化され同様に候補表示
- `abc` を入力 → エラーメッセージ表示
- `設定` ボタン → 文字数範囲（最小/最大）を変更可能
- 設定の文字数範囲外（例: 最小4文字設定で `りんご`）を入力 → 範囲エラー表示
- 候補（例: `・りんご`）をタップ → 候補詳細画面（読み: りんご / 漢字表記: 林檎 / 意味: apple）を表示
- seed未収録語の候補詳細で `詳細を取得` をタップ → 取得成功時は漢字/意味を表示（以降はキャッシュから再表示）
- オンデマンド取得を試す際はネットワーク接続が必要
- 設定ダイアログ内の `テーマ: ライト/ダーク` ボタンをタップ → テーマ表示が切り替わる
- アプリを再起動しても、選択したテーマ状態が維持される
- 入力後に「入力履歴を表示」ボタンから履歴を展開でき、履歴タップで同じ語を再入力できる
- アプリ再起動後も入力履歴（最新10件）が保持される
- 設定ダイアログ内の `追加辞書をダウンロード` ボタンをタップ → `追加辞書を適用しました...` または `追加辞書は最新です...` を表示
- ホーム画面で虫眼鏡デザインのアプリアイコンが表示される
- `辞書クレジット` ボタン → JMdict の CC BY-SA 4.0 表記ダイアログを表示
- 起動直後の logcat に `preload source=... total=... inserted=... elapsedMs=...` が出力される

### CI運用（Android UIテスト）

CI待ち時間短縮のため、Android関連ジョブは用途ごとに実行条件を分離しています。

- **PR必須**: `CI` ワークフロー
  - Python lint/test は常時実行
  - Android Unit Test / Build は `dorny/paths-filter`（commit SHA固定）で差分判定し、`android/**` または関連workflow変更時のみ実行（`--configuration-cache` + `org.gradle.configuration-cache=true` を有効化）
- **PR補助（任意）**: `Android UI Tests` ワークフロー
  - `pull_request`: `android/**` または関連ワークフロー変更時のみ自動実行
  - `workflow_dispatch`: 任意ブランチで手動実行
  - `schedule`: 毎日 03:00 JST（`0 18 * * *`）に定期実行
- UIテストは `androidTest` の `*Test.kt` クラスを2シャードに分割して並列実行し、失敗時は各シャードのレポート artifact と再現コマンドをジョブサマリに出力（`connectedDebugAndroidTest` も `--configuration-cache` を有効化）
- 計測例（`cd android && ./gradlew :app:testDebugUnitTest --dry-run --no-daemon`）: `--no-configuration-cache` 6.69s → Configuration Cache再利用時 4.10s（約39%短縮）
- 注意: Configuration Cache は初回実行時に生成コストが発生し、Gradle/AGP/プラグイン更新時は再生成される

### GitHub Release での配布（署名済みAPK）

まずは GitHub Release から `app-release.apk` を配布し、端末へインストールする運用を想定しています。

1. リポジトリ Secrets に以下を設定
   - `ANDROID_KEYSTORE_BASE64`（keystoreファイルを base64 化した文字列）
   - `ANDROID_SIGNING_STORE_PASSWORD`
   - `ANDROID_SIGNING_KEY_ALIAS`
   - `ANDROID_SIGNING_KEY_PASSWORD`
2. `v*` タグを push（または Actions の `Android Release` を手動実行）
   - 手動実行時は `tag` 未指定でも実行可能（`v0.0.0-auto-<branch>-<run_id>` 形式で自動タグ作成）
3. GitHub Release に添付された `app-release.apk` をダウンロードしてインストール

インストール例（ADB）:

```bash
adb install -r app-release.apk
```

同一署名鍵で継続配布することで、将来のアップデート配信へ移行しやすい構成にしています。

### 今後の機能

詳細は [Issue #14](https://github.com/scottlz0310/anagram-analyzer/issues/14) を参照してください。
