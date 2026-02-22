# anagram-analyzer

ひらがなアナグラム候補を検索する **Androidアプリ** プロジェクトです。  
現在は Android 実装を唯一の本実装として運用しています。

## 現在の構成

- **Androidアプリ本体**: `android/`
- **辞書seed生成ツール**: `android/tools/seed-generator/`（Kotlin/JVM CLIツール）
- **Python CLI版**: 2026-02-21時点で削除済み（プロトタイプ運用終了）

## Androidアプリ開発

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
adb shell am start -n com.anagram.analyzer/.MainActivity
```

確認例:
- `りんご` → 候補に `りんご`
- `リンゴ` → ひらがな正規化後に候補表示
- `abc` → エラー表示

## CI運用（要点）

- `CI` ワークフロー: Android Unit Test / Build（PRはAndroid差分時のみ）
- `Android UI Tests` ワークフロー: `androidTest` をクラス単位2シャードで実行
- `Android Release` ワークフロー: 署名済み `app-release.apk` を配布

## ライセンス

### 本ソフトウェア

MIT License — 詳細は [LICENSE](LICENSE) を参照してください。

### 辞書データ

JMdict は Electronic Dictionary Research and Development Group により  
[CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0/) でライセンスされています。
