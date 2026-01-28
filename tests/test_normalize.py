"""normalize モジュールのテスト"""

import pytest

from anagram_cli.normalize import (
    NormalizationError,
    anagram_key,
    is_all_hiragana,
    is_hiragana,
    katakana_to_hiragana,
    normalize_hiragana,
)


class TestKatakanaToHiragana:
    """カタカナ→ひらがな変換のテスト"""

    def test_katakana_to_hiragana(self):
        assert katakana_to_hiragana("アイウエオ") == "あいうえお"
        assert katakana_to_hiragana("カキクケコ") == "かきくけこ"

    def test_mixed_input(self):
        assert katakana_to_hiragana("あイうエお") == "あいうえお"

    def test_hiragana_unchanged(self):
        assert katakana_to_hiragana("あいうえお") == "あいうえお"

    def test_empty_string(self):
        assert katakana_to_hiragana("") == ""


class TestIsHiragana:
    """ひらがな判定のテスト"""

    def test_hiragana(self):
        assert is_hiragana("あ")
        assert is_hiragana("ん")
        assert is_hiragana("ゃ")

    def test_katakana_not_hiragana(self):
        assert not is_hiragana("ア")
        assert not is_hiragana("ン")

    def test_other_chars(self):
        assert not is_hiragana("a")
        assert not is_hiragana("漢")
        assert not is_hiragana("1")


class TestIsAllHiragana:
    """全ひらがな判定のテスト"""

    def test_all_hiragana(self):
        assert is_all_hiragana("あいうえお")
        assert is_all_hiragana("りんご")

    def test_mixed(self):
        assert not is_all_hiragana("あいアウエオ")
        assert not is_all_hiragana("りんご1")

    def test_empty(self):
        assert is_all_hiragana("")


class TestNormalizeHiragana:
    """ひらがな正規化のテスト"""

    def test_basic_hiragana(self):
        assert normalize_hiragana("あいうえお") == "あいうえお"

    def test_katakana_conversion(self):
        assert normalize_hiragana("アイウエオ") == "あいうえお"

    def test_whitespace_removal(self):
        assert normalize_hiragana("あ い う") == "あいう"
        assert normalize_hiragana("  あいう  ") == "あいう"

    def test_fullwidth_space(self):
        assert normalize_hiragana("あ　い　う") == "あいう"

    def test_nfkc_normalization(self):
        # 全角スペースなどはNFKCで処理
        pass

    def test_empty_raises(self):
        with pytest.raises(NormalizationError):
            normalize_hiragana("")

    def test_whitespace_only_raises(self):
        with pytest.raises(NormalizationError):
            normalize_hiragana("   ")

    def test_non_hiragana_raises(self):
        with pytest.raises(NormalizationError):
            normalize_hiragana("apple")

        with pytest.raises(NormalizationError):
            normalize_hiragana("漢字")

        with pytest.raises(NormalizationError):
            normalize_hiragana("あいう123")


class TestAnagramKey:
    """アナグラムキー生成のテスト"""

    def test_sorted_key(self):
        assert anagram_key("りんご") == "ごりん"
        assert anagram_key("さくら") == "くさら"

    def test_same_key_for_anagrams(self):
        # アナグラム同士は同じキーになる
        assert anagram_key("いろは") == anagram_key("はろい")
        assert anagram_key("いろは") == anagram_key("ろいは")

    def test_different_key_for_different_chars(self):
        assert anagram_key("あいう") != anagram_key("えおか")

    def test_empty_string(self):
        assert anagram_key("") == ""

    def test_single_char(self):
        assert anagram_key("あ") == "あ"
