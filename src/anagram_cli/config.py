"""
設定モジュール - キャッシュディレクトリなどの設定
"""

from pathlib import Path

import platformdirs

APP_NAME = "anagram-analyzer"
APP_AUTHOR = "anagram-analyzer"


def get_cache_dir() -> Path:
    """ユーザーキャッシュディレクトリを取得"""
    return Path(platformdirs.user_cache_dir(APP_NAME, APP_AUTHOR))


def get_default_db_path() -> Path:
    """デフォルトのデータベースパスを取得"""
    return get_cache_dir() / "anagram_index.db"
