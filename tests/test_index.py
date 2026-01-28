"""index モジュールのテスト"""

import tempfile
from pathlib import Path

import pytest

from anagram_cli.index import AnagramIndex


@pytest.fixture
def temp_db():
    """一時データベースを作成するフィクスチャ"""
    with tempfile.TemporaryDirectory() as tmpdir:
        db_path = Path(tmpdir) / "test.db"
        yield db_path


class TestAnagramIndex:
    """AnagramIndexのテスト"""

    def test_init_db(self, temp_db):
        with AnagramIndex(temp_db) as idx:
            idx.init_db()
            assert temp_db.exists()

    def test_add_and_lookup(self, temp_db):
        with AnagramIndex(temp_db) as idx:
            idx.init_db()
            idx.add("ごりん", "りんご")
            idx.commit()

            results = idx.lookup("ごりん")
            assert "りんご" in results

    def test_lookup_not_found(self, temp_db):
        with AnagramIndex(temp_db) as idx:
            idx.init_db()
            results = idx.lookup("ない")
            assert results == []

    def test_add_multiple_words_same_key(self, temp_db):
        with AnagramIndex(temp_db) as idx:
            idx.init_db()
            idx.add("いろは", "いろは")
            idx.add("いろは", "はろい")
            idx.add("いろは", "ろいは")
            idx.commit()

            results = idx.lookup("いろは")
            assert len(results) == 3
            assert set(results) == {"いろは", "はろい", "ろいは"}

    def test_add_duplicate_ignored(self, temp_db):
        with AnagramIndex(temp_db) as idx:
            idx.init_db()
            idx.add("ごりん", "りんご")
            idx.add("ごりん", "りんご")  # 重複
            idx.commit()

            results = idx.lookup("ごりん")
            assert len(results) == 1

    def test_count(self, temp_db):
        with AnagramIndex(temp_db) as idx:
            idx.init_db()
            idx.add("あ", "あ")
            idx.add("い", "い")
            idx.add("う", "う")
            idx.commit()

            assert idx.count() == 3

    def test_clear(self, temp_db):
        with AnagramIndex(temp_db) as idx:
            idx.init_db()
            idx.add("あ", "あ")
            idx.commit()
            assert idx.count() == 1

            idx.clear()
            assert idx.count() == 0

    def test_add_batch(self, temp_db):
        items = [
            ("ごりん", "りんご"),
            ("くさら", "さくら"),
            ("なばな", "ばなな"),
        ]

        with AnagramIndex(temp_db) as idx:
            idx.init_db()
            count = idx.add_batch(iter(items))

            assert count == 3
            assert idx.count() == 3
            assert "りんご" in idx.lookup("ごりん")
            assert "さくら" in idx.lookup("くさら")

    def test_exists(self, temp_db):
        idx = AnagramIndex(temp_db)
        assert not idx.exists()

        idx.init_db()
        idx.close()

        assert AnagramIndex(temp_db).exists()
