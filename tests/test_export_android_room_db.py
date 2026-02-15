from __future__ import annotations

import gzip
import sqlite3
import subprocess
import sys
from pathlib import Path

from anagram_cli.normalize import anagram_key

SCRIPT_PATH = (
    Path(__file__).resolve().parents[1] / "scripts" / "export_android_room_db.py"
)


def test_jmdict_xmlからRoom向けSQLiteを生成できる(tmp_path: Path) -> None:
    xml_path = tmp_path / "JMdict_sample.xml"
    xml_path.write_text(
        """<?xml version="1.0" encoding="UTF-8"?>
<JMdict>
  <entry><r_ele><reb>りんご</reb></r_ele></entry>
  <entry><r_ele><reb>リンゴ</reb></r_ele></entry>
  <entry><r_ele><reb>おなじ</reb></r_ele></entry>
  <entry><r_ele><reb>abc</reb></r_ele></entry>
  <entry><r_ele><reb>あ</reb></r_ele></entry>
</JMdict>
""",
        encoding="utf-8",
    )
    db_path = tmp_path / "anagram_seed.db"

    result = subprocess.run(
        [
            sys.executable,
            str(SCRIPT_PATH),
            "--xml",
            str(xml_path),
            "--output",
            str(db_path),
            "--min-len",
            "2",
            "--max-len",
            "8",
            "--force",
        ],
        check=True,
        capture_output=True,
        text=True,
    )

    assert "出力完了" in result.stdout
    with sqlite3.connect(db_path) as connection:
        rows = connection.execute(
            "SELECT sorted_key, word, length FROM anagram_entries ORDER BY word",
        ).fetchall()
        user_version = connection.execute("PRAGMA user_version").fetchone()
        cache_table = connection.execute(
            "SELECT name FROM sqlite_master WHERE type='table' AND name=?",
            ("candidate_detail_cache",),
        ).fetchone()

    assert rows == [
        (anagram_key("おなじ"), "おなじ", 3),
        (anagram_key("りんご"), "りんご", 3),
    ]
    assert user_version == (3,)
    assert cache_table == ("candidate_detail_cache",)


def test_gzip形式のJMdictXMLも変換できる(tmp_path: Path) -> None:
    xml_gz_path = tmp_path / "JMdict_sample.xml.gz"
    with gzip.open(xml_gz_path, "wt", encoding="utf-8") as stream:
        stream.write(
            """<?xml version="1.0" encoding="UTF-8"?>
<JMdict>
  <entry><r_ele><reb>さくら</reb></r_ele></entry>
</JMdict>
""",
        )
    db_path = tmp_path / "anagram_seed.db"

    subprocess.run(
        [
            sys.executable,
            str(SCRIPT_PATH),
            "--xml",
            str(xml_gz_path),
            "--output",
            str(db_path),
            "--force",
        ],
        check=True,
        capture_output=True,
        text=True,
    )

    with sqlite3.connect(db_path) as connection:
        rows = connection.execute(
            "SELECT sorted_key, word, length FROM anagram_entries",
        ).fetchall()

    assert rows == [(anagram_key("さくら"), "さくら", 3)]
