"""統合テスト"""

import tempfile
from pathlib import Path

import pytest

from anagram_cli.index import AnagramIndex
from anagram_cli.normalize import anagram_key, normalize_hiragana


class TestIntegration:
    """build -> solve の統合テスト（小さな語彙で）"""

    @pytest.fixture
    def sample_vocabulary(self):
        """テスト用の小さな語彙"""
        return [
            "りんご",
            "ごりら",
            "らっぱ",
            "さくら",
            "くらす",
            "すいか",
            "かいす",
            "ばなな",
        ]

    @pytest.fixture
    def temp_index(self, sample_vocabulary):
        """テスト用インデックスを構築"""
        with tempfile.TemporaryDirectory() as tmpdir:
            db_path = Path(tmpdir) / "test.db"

            with AnagramIndex(db_path) as idx:
                idx.init_db()

                for word in sample_vocabulary:
                    key = anagram_key(word)
                    idx.add(key, word)
                idx.commit()

            yield db_path

    def test_solve_exact_match(self, temp_index):
        """完全一致の検索"""
        with AnagramIndex(temp_index) as idx:
            # 「りんご」の文字で検索
            input_str = "りんご"
            normalized = normalize_hiragana(input_str)
            key = anagram_key(normalized)

            results = idx.lookup(key)
            assert "りんご" in results

    def test_solve_anagram(self, temp_index):
        """アナグラムの検索"""
        with AnagramIndex(temp_index) as idx:
            # 「ごりん」（りんごのアナグラム）で検索
            input_str = "ごりん"
            normalized = normalize_hiragana(input_str)
            key = anagram_key(normalized)

            results = idx.lookup(key)
            assert "りんご" in results

    def test_solve_multiple_results(self, temp_index):
        """複数候補がある場合"""
        with AnagramIndex(temp_index) as idx:
            # 「すいか」と「かいす」は同じキー
            input_str = "すいか"
            normalized = normalize_hiragana(input_str)
            key = anagram_key(normalized)

            results = idx.lookup(key)
            assert len(results) == 2
            assert "すいか" in results
            assert "かいす" in results

    def test_solve_no_match(self, temp_index):
        """マッチしない場合"""
        with AnagramIndex(temp_index) as idx:
            input_str = "ぞうさん"
            normalized = normalize_hiragana(input_str)
            key = anagram_key(normalized)

            results = idx.lookup(key)
            assert results == []

    def test_katakana_input(self, temp_index):
        """カタカナ入力でも検索できる"""
        with AnagramIndex(temp_index) as idx:
            input_str = "リンゴ"  # カタカナ
            normalized = normalize_hiragana(input_str)
            key = anagram_key(normalized)

            results = idx.lookup(key)
            assert "りんご" in results
