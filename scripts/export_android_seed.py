#!/usr/bin/env python3
"""JMdict語彙をAndroid向けseed TSVへ変換する。"""

from __future__ import annotations

import argparse
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "src"))

from anagram_cli.lexicon.jmdict import iter_words
from anagram_cli.normalize import anagram_key


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="JMdict語彙をAndroid向けanagram_seed.tsvへ変換します。",
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
    args.output.parent.mkdir(parents=True, exist_ok=True)

    count = 0
    with args.output.open("w", encoding="utf-8") as output:
        output.write("# sorted_key<TAB>word<TAB>length\n")
        for word in iter_words(min_len=args.min_len, max_len=args.max_len):
            output.write(f"{anagram_key(word)}\t{word}\t{len(word)}\n")
            count += 1
            if args.limit > 0 and count >= args.limit:
                break

    print(f"出力完了: {args.output} ({count}件)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
