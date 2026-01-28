"""
ひらがな正規化とアナグラムキー生成モジュール
"""

import re
import unicodedata


class NormalizationError(Exception):
    """正規化エラー"""

    pass


def normalize_hiragana(s: str) -> str:
    """
    文字列をひらがなに正規化する。

    - NFKC正規化
    - 空白除去
    - カタカナ→ひらがな変換
    - ひらがな以外が含まれる場合は例外

    Args:
        s: 入力文字列

    Returns:
        正規化されたひらがな文字列

    Raises:
        NormalizationError: ひらがな以外の文字が含まれる場合
    """
    if not s:
        raise NormalizationError("空の文字列は処理できません")

    # NFKC正規化（全角英数→半角、など）
    normalized = unicodedata.normalize("NFKC", s)

    # 空白除去
    normalized = re.sub(r"\s+", "", normalized)

    if not normalized:
        raise NormalizationError("空白のみの文字列は処理できません")

    # カタカナ→ひらがな変換
    normalized = katakana_to_hiragana(normalized)

    # ひらがなのみかチェック
    if not is_all_hiragana(normalized):
        invalid_chars = [c for c in normalized if not is_hiragana(c)]
        raise NormalizationError(f"ひらがな以外の文字が含まれています: {invalid_chars}")

    return normalized


def katakana_to_hiragana(s: str) -> str:
    """カタカナをひらがなに変換する"""
    result: list[str] = []
    for char in s:
        code = ord(char)
        # カタカナ範囲: U+30A1 (ァ) - U+30F6 (ヶ)
        # ひらがな範囲: U+3041 (ぁ) - U+3096 (ゖ)
        if 0x30A1 <= code <= 0x30F6:
            result.append(chr(code - 0x60))
        else:
            result.append(char)
    return "".join(result)


def is_hiragana(char: str) -> bool:
    """文字がひらがなかどうか判定する"""
    code = ord(char)
    # ひらがな範囲: U+3041 (ぁ) - U+3096 (ゖ)
    # 長音記号も許可: U+30FC (ー)→ひらがなの文脈で使われることがある
    return 0x3041 <= code <= 0x3096 or char == "ー"


def is_all_hiragana(s: str) -> bool:
    """文字列が全てひらがなかどうか判定する"""
    return all(is_hiragana(c) for c in s)


def anagram_key(s: str) -> str:
    """
    アナグラムキーを生成する。

    文字列をソートしてキー化する。
    同じ文字構成の単語は同じキーになる。

    Args:
        s: ひらがな文字列（正規化済み）

    Returns:
        ソートされた文字列（アナグラムキー）
    """
    return "".join(sorted(s))
