"""
CLIエントリポイント（Typer + Rich）
"""

from enum import Enum
from pathlib import Path
from typing import Annotated

import typer
from rich.console import Console
from rich.panel import Panel
from rich.progress import (
    BarColumn,
    Progress,
    SpinnerColumn,
    TaskProgressColumn,
    TextColumn,
)
from rich.table import Table

from .config import get_cache_dir, get_default_db_path
from .index import AnagramIndex
from .normalize import NormalizationError, anagram_key, normalize_hiragana

app = typer.Typer(
    name="anagram",
    help="ひらがなアナグラム解析CLI",
    no_args_is_help=True,
)
console = Console()


class SpoilerMode(str, Enum):
    """ネタバレ表示モード"""

    OFF = "off"  # 非表示
    HINT = "hint"  # 最初の文字のみ
    FULL = "full"  # 全表示


@app.command()
def build(
    min_len: Annotated[int, typer.Option("--min-len", help="最小文字数")] = 2,
    max_len: Annotated[int, typer.Option("--max-len", help="最大文字数")] = 20,
    db: Annotated[
        Path | None, typer.Option("--db", help="データベースファイルのパス")
    ] = None,
    force: Annotated[
        bool, typer.Option("--force", "-f", help="既存DBを上書き")
    ] = False,
):
    """辞書からアナグラムインデックスを構築する"""
    from .lexicon.jmdict import (
        check_jamdict_available,
        check_jamdict_data_available,
        iter_words,
    )

    db_path = db or get_default_db_path()

    # 依存チェック
    if not check_jamdict_available():
        console.print("[red]エラー:[/red] jamdictがインストールされていません")
        console.print("以下のコマンドでインストールしてください:")
        console.print("  [cyan]uv add jamdict jamdict-data[/cyan]")
        raise typer.Exit(1)

    if not check_jamdict_data_available():
        console.print("[red]エラー:[/red] jamdict-dataが利用できません")
        console.print("以下のコマンドでインストールしてください:")
        console.print("  [cyan]uv add jamdict-data[/cyan]")
        raise typer.Exit(1)

    # 既存DBの確認
    if db_path.exists() and not force:
        console.print(f"[yellow]警告:[/yellow] データベースが既に存在します: {db_path}")
        console.print(
            "上書きするには [cyan]--force[/cyan] オプションを使用してください"
        )
        raise typer.Exit(1)

    console.print("[bold]アナグラムインデックスを構築します[/bold]")
    console.print(f"  データベース: {db_path}")
    console.print(f"  文字数範囲: {min_len} - {max_len}")
    console.print()

    with Progress(
        SpinnerColumn(),
        TextColumn("[progress.description]{task.description}"),
        BarColumn(),
        TaskProgressColumn(),
        console=console,
    ) as progress:
        task = progress.add_task("[cyan]辞書を読み込み中...", total=None)

        # インデックス作成
        with AnagramIndex(db_path) as idx:
            idx.init_db()
            idx.clear()  # 既存データをクリア

            def progress_callback(current: int, total: int):
                progress.update(task, completed=current, total=total)

            # 語彙を抽出してインデックス化
            def generate_pairs():
                for word in iter_words(
                    min_len=min_len,
                    max_len=max_len,
                    progress_callback=progress_callback,
                ):
                    key = anagram_key(word)
                    yield (key, word)

            count = idx.add_batch(generate_pairs())
            progress.update(task, description="[green]完了!")

    console.print()
    console.print(f"[green]✓[/green] {count:,} 語をインデックス化しました")
    console.print(f"  保存先: {db_path}")


@app.command()
def solve(
    letters: Annotated[str, typer.Argument(help="並べ替える文字列（ひらがな）")],
    top: Annotated[
        int | None, typer.Option("--top", "-n", help="上位N件を表示")
    ] = None,
    spoiler: Annotated[
        SpoilerMode, typer.Option("--spoiler", "-s", help="ネタバレ表示モード")
    ] = SpoilerMode.FULL,
    db: Annotated[
        Path | None, typer.Option("--db", help="データベースファイルのパス")
    ] = None,
):
    """アナグラム候補を検索する"""
    db_path = db or get_default_db_path()

    # DB存在確認
    if not db_path.exists():
        console.print("[red]エラー:[/red] データベースが見つかりません")
        console.print("先に [cyan]anagram build[/cyan] を実行してください")
        raise typer.Exit(1)

    # 入力を正規化
    try:
        normalized = normalize_hiragana(letters)
    except NormalizationError as e:
        console.print(f"[red]エラー:[/red] {e}")
        raise typer.Exit(1) from None

    # キー生成
    key = anagram_key(normalized)

    # 検索
    with AnagramIndex(db_path) as idx:
        results = idx.lookup(key)

    if not results:
        console.print(
            f"[yellow]「{letters}」のアナグラム候補は見つかりませんでした[/yellow]"
        )
        raise typer.Exit(0)

    # 結果をソート（アルファベット順）
    results = sorted(results)

    # 上位N件に制限
    if top is not None and top > 0:
        results = results[:top]

    # テーブル表示
    table = Table(title=f"「{letters}」のアナグラム候補")
    table.add_column("#", justify="right", style="dim")
    table.add_column("候補", style="cyan")

    for i, word in enumerate(results, 1):
        display_word = format_spoiler(word, spoiler)
        table.add_row(str(i), display_word)

    console.print(table)
    console.print(f"\n[dim]合計 {len(results)} 件[/dim]")


def format_spoiler(word: str, mode: SpoilerMode) -> str:
    """ネタバレモードに応じて単語をフォーマット"""
    if mode == SpoilerMode.FULL:
        return word
    elif mode == SpoilerMode.HINT:
        if len(word) <= 1:
            return "○"
        return word[0] + "○" * (len(word) - 1)
    else:  # OFF
        return "○" * len(word)


@app.command()
def doctor():
    """環境診断を行う"""
    from .lexicon.jmdict import check_jamdict_available, check_jamdict_data_available

    console.print(Panel("[bold]anagram-analyzer 環境診断[/bold]"))
    console.print()

    db_path = get_default_db_path()
    cache_dir = get_cache_dir()

    # キャッシュディレクトリ
    console.print("[bold]キャッシュ設定[/bold]")
    console.print(f"  ディレクトリ: {cache_dir}")
    console.print(f"  データベース: {db_path}")
    console.print()

    # データベース状態
    console.print("[bold]データベース状態[/bold]")
    if db_path.exists():
        with AnagramIndex(db_path) as idx:
            count = idx.count()
        console.print(f"  [green]✓[/green] 存在します ({count:,} エントリ)")
    else:
        console.print(
            "  [yellow]✗[/yellow] 存在しません（[cyan]anagram build[/cyan] で作成）"
        )
    console.print()

    # 依存関係
    console.print("[bold]辞書依存関係[/bold]")

    if check_jamdict_available():
        console.print("  [green]✓[/green] jamdict: インストール済み")
    else:
        console.print("  [red]✗[/red] jamdict: 未インストール")

    if check_jamdict_data_available():
        console.print("  [green]✓[/green] jamdict-data: 利用可能")
    else:
        console.print("  [red]✗[/red] jamdict-data: 利用不可")

    console.print()
    console.print("[dim]辞書をインストールするには: uv add jamdict jamdict-data[/dim]")


if __name__ == "__main__":
    app()
