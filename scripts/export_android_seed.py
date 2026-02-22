#!/usr/bin/env python3
"""JMdict語彙をAndroid向けseed TSVへ変換する。"""

from __future__ import annotations

import argparse
from pathlib import Path

from export_android_room_db import (
    anagram_key,
    is_all_hiragana,
    iter_jmdict_readings,
    normalize_hiragana,
    resolve_jmdict_xml_path,
)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="JMdict語彙をAndroid向けanagram_seed.tsvへ変換します。",
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
        default=Path("android/app/src/main/assets/anagram_seed.tsv"),
        help="出力先TSVパス",
    )
    parser.add_argument("--min-len", type=int, default=2, help="最小文字数")
    parser.add_argument("--max-len", type=int, default=8, help="最大文字数")
    parser.add_argument(
        "--limit",
        type=int,
        default=0,
        help="出力件数上限（0で無制限）",
    )
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    xml_path = resolve_jmdict_xml_path(args.xml)
    if not xml_path.exists():
        raise RuntimeError(f"JMdict XMLが見つかりません: {xml_path}")

    args.output.parent.mkdir(parents=True, exist_ok=True)

    count = 0
    seen_words: set[str] = set()
    with args.output.open("w", encoding="utf-8") as output:
        output.write("# sorted_key<TAB>word<TAB>length\n")
        for reading in iter_jmdict_readings(xml_path):
            word = normalize_hiragana(reading)
            if len(word) < args.min_len or len(word) > args.max_len:
                continue
            if not is_all_hiragana(word):
                continue
            if word in seen_words:
                continue
            seen_words.add(word)
            output.write(f"{anagram_key(word)}\t{word}\t{len(word)}\n")
            count += 1
            if args.limit > 0 and count >= args.limit:
                break

    print(f"出力完了: {args.output} ({count}件)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
