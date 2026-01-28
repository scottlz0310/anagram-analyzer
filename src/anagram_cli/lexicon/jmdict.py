"""
JMdict（jamdict）からの語彙抽出モジュール
"""

from collections.abc import Callable, Iterator

from ..normalize import is_all_hiragana, katakana_to_hiragana

# jamdict は型スタブがないため、インポート時に type: ignore を使用


def check_jamdict_available() -> bool:
    """jamdictが利用可能か確認"""
    try:
        import importlib.util

        return importlib.util.find_spec("jamdict") is not None
    except ImportError:
        return False


def check_jamdict_data_available() -> bool:
    """jamdict-dataが利用可能か確認"""
    try:
        from jamdict import Jamdict  # type: ignore[import-untyped]

        jmd = Jamdict()
        # データが存在するかテスト
        _ = jmd.lookup("テスト")
        return True
    except Exception:
        return False


def iter_words(
    min_len: int = 2,
    max_len: int = 20,
    _noun_only: bool = True,
    progress_callback: Callable[[int, int], None] | None = None,
) -> Iterator[str]:
    """
    JMdictから語彙を抽出する。

    Args:
        min_len: 最小文字数
        max_len: 最大文字数
        _noun_only: 名詞のみに制限するか（現在は未実装、全語彙を返す）
        progress_callback: 進捗コールバック (current, total) -> None

    Yields:
        ひらがなの単語
    """
    try:
        from jamdict import Jamdict  # type: ignore[import-untyped]
    except ImportError as exc:
        msg = (
            "jamdictがインストールされていません。\n"
            "以下のコマンドでインストールしてください:\n"
            "  uv add jamdict jamdict-data"
        )
        raise RuntimeError(msg) from exc

    jmd = Jamdict()

    # JMdictの全エントリを取得（lookup_iterでワイルドカード検索）
    try:
        result = jmd.lookup_iter("%%", limit=0)  # limit=0 で全件取得
        all_entries = list(result.entries)
    except Exception as e:
        msg = (
            f"辞書データの読み込みに失敗しました: {e}\n"
            "jamdict-dataがインストールされているか確認してください:\n"
            "  uv add jamdict-data"
        )
        raise RuntimeError(msg) from e

    total = len(all_entries)
    seen_words: set[str] = set()

    for i, entry in enumerate(all_entries):
        if progress_callback and i % 1000 == 0:
            progress_callback(i, total)

        # 見出し語（かな表記）を取得
        for kana in entry.kana_forms:
            word = str(kana)

            # カタカナをひらがなに変換
            word = katakana_to_hiragana(word)

            # フィルタリング
            if len(word) < min_len or len(word) > max_len:
                continue

            if not is_all_hiragana(word):
                continue

            # 重複除去
            if word in seen_words:
                continue

            seen_words.add(word)

            # 名詞フィルタ（将来実装）
            # 現時点では全てのひらがな語彙を返す
            # _noun_only が True でも、品詞判定は複雑なので初期は無視

            yield word

    if progress_callback:
        progress_callback(total, total)
