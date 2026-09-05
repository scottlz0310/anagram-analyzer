# anagram-analyzer

ひらがなアナグラム候補を検索する **Androidアプリ** プロジェクトです。  
現在は Android 実装を唯一の本実装として運用しています。

## 現在の構成

- **Androidアプリ本体**: `android/`
- **辞書seed生成ツール**: `android/tools/seed-generator/`（Kotlin/JVM CLIツール）
- **Python CLI版**: 2026-02-21時点で削除済み（プロトタイプ運用終了）

## Androidアプリ開発

### Git フック（lefthook）

本リポジトリは [lefthook](https://github.com/evilmartians/lefthook) で Git hook を管理している。**clone 後に一度だけ**フックを登録すること（登録しないとフックは動作しない）。

```bash
# lefthook 未導入なら先にインストール（例: go install / Homebrew / scoop 等）
lefthook install
```

- `pre-commit`: staged な `*.kt` を ktlint CLI 1.8.0 で自動整形し、修正分を再 stage する
- `pre-push`: `:app:testDebugUnitTest` と `:tools:seed-generator:test` を実行する
- ktlint CLI は初回実行時に `.cache/ktlint/` へ取得され、SHA-256 検証後に `java -jar` で実行される
- ktlint の pre-commit には `powershell` コマンドと Java が必要。Windows 標準環境を前提にしているため、macOS / Linux では PowerShell を別途導入する

### ビルド

```bash
cd android && ./gradlew :app:assembleDebug
```

### ユニットテスト

```bash
cd android && ./gradlew :app:testDebugUnitTest
```

### UIテスト（エミュレータ/実機）

```bash
cd android && ./gradlew :app:connectedDebugAndroidTest
```

### Lint

```bash
cd android && ./gradlew :app:lintDebug
```

## クイズモード

- 文字カードをタップして解答スロットへ並べるカード入力UIを実装しています。
- 正解候補の並びそのものが問題文として出ないようにし、回避不能な問題は別の単語へ切り替えます。
- スコア、連続正解数、最高連続正解数はアプリ内で保持されます。

## 辞書seed更新（開発者向け）

Kotlin/JVM CLIツール（`tools:seed-generator`）で JMdict XML（`.xml` / `.gz`）から seed を生成します。

### TSV生成

```bash
cd android && ./gradlew :tools:seed-generator:run \
  --args="--jmdict ~/.jamdict/data/JMdict_e.gz \
          --out-tsv app/src/main/assets/anagram_seed.tsv \
          --mode tsv --min-len 2 --max-len 8"
```

### Room互換SQLite生成

```bash
cd android && ./gradlew :tools:seed-generator:run \
  --args="--jmdict ~/.jamdict/data/JMdict_e.gz \
          --out-db app/src/main/assets/anagram_seed.db \
          --mode db --min-len 2 --max-len 8 --force"
```

- 推奨運用値は `--max-len 8` です。

## 手動確認（Android）

```bash
cd android
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n io.github.scottlz0310.anagramanalyzer/.MainActivity
```

確認例:
- `りんご` → 候補に `りんご`
- `リンゴ` → ひらがな正規化後に候補表示
- `abc` → エラー表示
- クイズモード → カードをタップして答えを完成できる
- クイズモード → 正解候補そのままの並びが出題されない

## CI運用（要点）

- `CI` ワークフロー: Android Unit Test / Build（PRはAndroid差分時のみ）
- `Android UI Tests` ワークフロー: `androidTest` をクラス単位2シャードで実行
  - 安定性優先のため UI テストでは `--configuration-cache` を利用しない（Unit/Build では利用）
- `Android Release` ワークフロー: 署名済み `app-release.apk` を配布


## リリース署名

このリポジトリには**2つの署名経路**があります。混同しないよう注意してください。

| 配布経路 | 署名鍵 | 保有者 | 現状 |
|---|---|---|---|
| GitHub Releases の `app-release.apk` | 自前の keystore | 開発者 | 現行の配布経路 |
| Google Play（AAB） | **アプリ署名鍵** | **Google**（Play アプリ署名） | アプリ登録済み・未公開 |

Play アプリ署名が有効なため、**Play 経由で利用者の端末に届く APK は Google が保有する鍵で署名されます**。開発者側が用意するのは AAB をアップロードする際の**アップロード鍵**のみです。

Play Console 上のアプリ署名鍵は**量子対応ハイブリッド署名**（従来の鍵 + ポスト量子暗号鍵）で構成されており、Android デベロッパーの確認にはこの鍵群の証明書が登録済みです。自前の証明書を登録する必要はありません。

### 自前 keystore の位置づけ

鍵本体・パスワード・フィンガープリント・復旧手順は**リポジトリ外**の保管先 `README.md` を参照してください。

- `applicationId`: `io.github.scottlz0310.anagramanalyzer`
- alias: `anagram-analyzer-release`（PKCS12 / RSA 4096bit）
- 用途: GitHub Releases 用 APK の署名、および将来 Play へ AAB をアップロードする際のアップロード鍵

アップロード鍵の証明書は Play Console 上でまだ確定していません。**最初の App Bundle をアップロードした時点で、その署名鍵がアップロード鍵として登録されます。**

`Android Release` ワークフローは以下の secrets から署名情報を取得します。

| Secret | 内容 |
|---|---|
| `ANDROID_KEYSTORE_BASE64` | keystore を base64 エンコードしたもの |
| `ANDROID_SIGNING_STORE_PASSWORD` | keystore のパスワード |
| `ANDROID_SIGNING_KEY_ALIAS` | 鍵の alias |
| `ANDROID_SIGNING_KEY_PASSWORD` | 鍵のパスワード |

### ローカルで署名付き release ビルドを行う

`android/app/build.gradle.kts` は gradle property と環境変数のどちらからでも署名情報を読みます。4つすべてを与えないとビルドは明示的に失敗します（部分指定の検出）。

```bash
cd android
export ANDROID_SIGNING_STORE_FILE="<keystore への絶対パス>"
export ANDROID_SIGNING_STORE_PASSWORD="<store パスワード>"
export ANDROID_SIGNING_KEY_ALIAS="anagram-analyzer-release"
export ANDROID_SIGNING_KEY_PASSWORD="<key パスワード>"
./gradlew :app:assembleRelease
```

署名の検証:

```bash
apksigner verify --print-certs app/build/outputs/apk/release/app-release.apk
```

出力される証明書 SHA-256 が保管先 `fingerprints.txt` の値と一致することを確認してください。これは**自前鍵で署名した APK の検証**であり、Play 配信版の署名とは別物です。

### 鍵を失った場合

**アップロード鍵は再発行できます。** Play Console から新しいアップロード鍵へのリセットをリクエストでき、アプリが利用できなくなることはありません。アプリ署名鍵は Google が保護しているため、開発者側の鍵の紛失が配信を止めることはありません。

ただし GitHub Releases の APK は自前鍵で署名されているため、鍵を失うと**その経路で配布済みの APK を更新できなくなります**（Android は署名の異なる APK への更新を拒否します）。#76 で公式配布を Play のみへ移行するまでは、自前鍵のバックアップを維持してください。

> **キーストアをリポジトリ内へ配置しないこと。** 誤コミット防止として `.gitignore` に `*.jks` / `*.keystore` / `*.p12` を登録していますが、鍵はリポジトリ外に置くのが原則です。
## ライセンス

### 本ソフトウェア

MIT License — 詳細は [LICENSE](LICENSE) を参照してください。

### 辞書データ

JMdict は Electronic Dictionary Research and Development Group により  
[CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0/) でライセンスされています。
