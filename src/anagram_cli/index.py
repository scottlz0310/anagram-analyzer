"""
SQLiteアナグラムインデックス管理モジュール
"""

import sqlite3
from collections.abc import Iterator
from pathlib import Path
from types import TracebackType
from typing import final


@final
class AnagramIndex:
    """アナグラムインデックスのSQLiteストレージ"""

    SCHEMA = """
    CREATE TABLE IF NOT EXISTS anagram_index (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        key TEXT NOT NULL,
        word TEXT NOT NULL,
        UNIQUE(key, word)
    );
    CREATE INDEX IF NOT EXISTS idx_key ON anagram_index(key);
    """

    def __init__(self, db_path: Path | str):
        """
        Args:
            db_path: データベースファイルのパス
        """
        self.db_path = Path(db_path)
        self._conn: sqlite3.Connection | None = None

    @property
    def conn(self) -> sqlite3.Connection:
        """データベース接続を取得（遅延初期化）"""
        if self._conn is None:
            self._conn = sqlite3.connect(self.db_path)
        return self._conn

    def init_db(self) -> None:
        """データベースを初期化する"""
        self.db_path.parent.mkdir(parents=True, exist_ok=True)
        _ = self.conn.executescript(self.SCHEMA)
        self.conn.commit()

    def add(self, key: str, word: str) -> None:
        """
        キーと単語のペアを追加する。

        Args:
            key: アナグラムキー
            word: 元の単語
        """
        try:
            _ = self.conn.execute(
                "INSERT OR IGNORE INTO anagram_index (key, word) VALUES (?, ?)",
                (key, word),
            )
        except sqlite3.Error as e:
            raise RuntimeError(f"データベースエラー: {e}") from None

    def add_batch(
        self, items: Iterator[tuple[str, str]], batch_size: int = 1000
    ) -> int:
        """
        キーと単語のペアをバッチで追加する。

        Args:
            items: (key, word) のイテレータ
            batch_size: バッチサイズ

        Returns:
            追加された件数
        """
        count = 0
        batch: list[tuple[str, str]] = []

        for item in items:
            batch.append(item)
            count += 1

            if len(batch) >= batch_size:
                self._insert_batch(batch)
                batch = []

        if batch:
            self._insert_batch(batch)

        return count

    def _insert_batch(self, batch: list[tuple[str, str]]) -> None:
        """バッチインサートを実行"""
        _ = self.conn.executemany(
            "INSERT OR IGNORE INTO anagram_index (key, word) VALUES (?, ?)", batch
        )
        self.conn.commit()

    def lookup(self, key: str) -> list[str]:
        """
        キーに対応する単語を検索する。

        Args:
            key: アナグラムキー

        Returns:
            マッチする単語のリスト
        """
        cursor = self.conn.execute(
            "SELECT word FROM anagram_index WHERE key = ?", (key,)
        )
        rows: list[tuple[str]] = cursor.fetchall()  # type: ignore[assignment]
        return [row[0] for row in rows]

    def count(self) -> int:
        """登録されているエントリ数を取得"""
        cursor = self.conn.execute("SELECT COUNT(*) FROM anagram_index")
        row = cursor.fetchone()
        return int(row[0]) if row is not None else 0  # type: ignore[index]

    def exists(self) -> bool:
        """データベースファイルが存在するか確認"""
        return self.db_path.exists()

    def clear(self) -> None:
        """全データを削除"""
        _ = self.conn.execute("DELETE FROM anagram_index")
        self.conn.commit()

    def commit(self) -> None:
        """変更をコミット"""
        if self._conn:
            self._conn.commit()

    def close(self) -> None:
        """データベース接続を閉じる"""
        if self._conn:
            self._conn.close()
            self._conn = None

    def __enter__(self) -> "AnagramIndex":
        return self

    def __exit__(
        self,
        exc_type: type[BaseException] | None,
        exc_val: BaseException | None,
        exc_tb: TracebackType | None,
    ) -> None:
        self.close()
