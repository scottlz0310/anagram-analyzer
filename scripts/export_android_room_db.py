#!/usr/bin/env python3
"""JMdict XMLをAndroid Room向けSQLite DBへ変換する。"""

from __future__ import annotations

import argparse
import gzip
import importlib
import sqlite3
import sys
import xml.etree.ElementTree as ET
from collections.abc import Iterator
from contextlib import contextmanager
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "src"))

normalize_module = importlib.import_module("anagram_cli.normalize")
anagram_key = normalize_module.anagram_key
is_all_hiragana = normalize_module.is_all_hiragana
katakana_to_hiragana = normalize_module.katakana_to_hiragana


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="JMdict XMLをAndroid Room向けSQLite DBへ変換します。",
    )
    parser.add_argument(
        "--xml",
        type=Path,
        default=None,
        help="JMdict XML(.xml/.gz) のパス。未指定時は jamdict-data 既定パスを使用",
    )
    parser.add_argument(
        "--output",
        type=Path,
        default=Path("android/app/src/main/assets/anagram_seed.db"),
        help="出力先SQLite DBパス",
    )
    parser.add_argument("--min-len", type=int, default=2, help="最小文字数")
    parser.add_argument("--max-len", type=int, default=8, help="最大文字数")
    parser.add_argument(
        "--limit",
        type=int,
        default=0,
        help="出力件数上限（0で無制限）",
    )
    parser.add_argument(
        "--batch-size",
        type=int,
        default=5000,
        help="一括INSERT件数",
    )
    parser.add_argument(
        "--force",
        action="store_true",
        help="既存出力DBを上書き",
    )
    return parser.parse_args()


def resolve_jmdict_xml_path(path: Path | None) -> Path:
    if path is not None:
        return path

    try:
        from jamdict import Jamdict  # type: ignore[import-untyped]
    except ImportError as exc:
        msg = (
            "JMdict XMLパスが未指定です。\n"
            "--xml を指定するか、以下で jamdict-data を導入してください:\n"
            "  uv pip install -e \".[dict]\""
        )
        raise RuntimeError(msg) from exc

    jmd = Jamdict()
    return Path(jmd.jmd_xml_file)


@contextmanager
def open_jmdict_source(path: Path) -> Iterator[object]:
    if path.suffix == ".gz":
        with gzip.open(path, "rt", encoding="utf-8") as source:
            yield source
    else:
        with path.open("r", encoding="utf-8") as source:
            yield source


def iter_jmdict_readings(path: Path) -> Iterator[str]:
    with open_jmdict_source(path) as source:
        for _event, element in ET.iterparse(source, events=("end",)):
            if local_tag(element.tag) != "entry":
                continue

            for reading_element in element.iter():
                if local_tag(reading_element.tag) != "reb":
                    continue
                text = (reading_element.text or "").strip()
                if text:
                    yield text

            element.clear()


def iter_anagram_rows(
    xml_path: Path,
    min_len: int,
    max_len: int,
    limit: int,
) -> Iterator[tuple[str, str, int]]:
    seen_words: set[str] = set()
    count = 0

    for reading in iter_jmdict_readings(xml_path):
        word = katakana_to_hiragana(reading)
        if len(word) < min_len or len(word) > max_len:
            continue
        if not is_all_hiragana(word):
            continue
        if word in seen_words:
            continue

        seen_words.add(word)
        yield (anagram_key(word), word, len(word))
        count += 1
        if limit > 0 and count >= limit:
            return


def initialize_database(connection: sqlite3.Connection) -> None:
    connection.execute(
        """
        CREATE TABLE IF NOT EXISTS anagram_entries (
            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            sorted_key TEXT NOT NULL,
            word TEXT NOT NULL,
            length INTEGER NOT NULL
        )
        """,
    )
    connection.execute(
        """
        CREATE UNIQUE INDEX IF NOT EXISTS index_anagram_entries_sorted_key_word
        ON anagram_entries (sorted_key, word)
        """,
    )
    connection.execute(
        """
        CREATE INDEX IF NOT EXISTS index_anagram_entries_sorted_key
        ON anagram_entries (sorted_key)
        """,
    )
    connection.execute(
        """
        CREATE INDEX IF NOT EXISTS index_anagram_entries_length
        ON anagram_entries (length)
        """,
    )
    connection.execute(
        """
        CREATE TABLE IF NOT EXISTS candidate_detail_cache (
            word TEXT NOT NULL PRIMARY KEY,
            kanji TEXT NOT NULL,
            meaning TEXT NOT NULL,
            updated_at INTEGER NOT NULL
        )
        """,
    )
    connection.execute(
        """
        CREATE INDEX IF NOT EXISTS index_candidate_detail_cache_updated_at
        ON candidate_detail_cache (updated_at)
        """,
    )
    connection.execute("PRAGMA user_version = 3")


def export_room_database(
    xml_path: Path,
    output_path: Path,
    min_len: int,
    max_len: int,
    limit: int,
    batch_size: int,
    force: bool,
) -> int:
    if output_path.exists():
        if not force:
            raise RuntimeError(
                "出力先が既に存在します: "
                f"{output_path} "
                "（上書きする場合は --force を指定）",
            )
        output_path.unlink()

    output_path.parent.mkdir(parents=True, exist_ok=True)

    inserted = 0
    with sqlite3.connect(output_path) as connection:
        initialize_database(connection)
        batch: list[tuple[str, str, int]] = []
        for row in iter_anagram_rows(
            xml_path=xml_path,
            min_len=min_len,
            max_len=max_len,
            limit=limit,
        ):
            batch.append(row)
            if len(batch) < batch_size:
                continue
            connection.executemany(
                """
                INSERT OR IGNORE INTO anagram_entries (sorted_key, word, length)
                VALUES (?, ?, ?)
                """,
                batch,
            )
            inserted += len(batch)
            batch.clear()

        if batch:
            connection.executemany(
                """
                INSERT OR IGNORE INTO anagram_entries (sorted_key, word, length)
                VALUES (?, ?, ?)
                """,
                batch,
            )
            inserted += len(batch)

        connection.commit()

    return inserted


def local_tag(tag: str) -> str:
    return tag.rsplit("}", maxsplit=1)[-1]


def main() -> int:
    args = parse_args()

    if args.min_len < 1:
        raise RuntimeError("--min-len は1以上を指定してください")
    if args.max_len < args.min_len:
        raise RuntimeError("--max-len は --min-len 以上を指定してください")
    if args.batch_size < 1:
        raise RuntimeError("--batch-size は1以上を指定してください")

    xml_path = resolve_jmdict_xml_path(args.xml)
    if not xml_path.exists():
        raise RuntimeError(f"JMdict XMLが見つかりません: {xml_path}")

    inserted = export_room_database(
        xml_path=xml_path,
        output_path=args.output,
        min_len=args.min_len,
        max_len=args.max_len,
        limit=args.limit,
        batch_size=args.batch_size,
        force=args.force,
    )
    print(f"出力完了: {args.output} ({inserted}件)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
