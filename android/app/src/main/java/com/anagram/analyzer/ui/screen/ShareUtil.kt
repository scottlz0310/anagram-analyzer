package com.anagram.analyzer.ui.screen

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log

private const val SHARE_LOG_TAG = "CandidateDetailShare"

internal fun shareCandidateDetail(
    context: Context,
    candidate: String,
    kanji: String?,
    meaning: String,
) {
    val shareText = buildString {
        appendLine("候補: $candidate")
        appendLine("漢字表記: ${kanji ?: "（未対応）"}")
        append("意味: $meaning")
    }
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Anagram Analyzer 候補詳細")
        putExtra(Intent.EXTRA_TEXT, shareText)
    }
    if (sendIntent.resolveActivity(context.packageManager) == null) {
        Log.w(SHARE_LOG_TAG, "共有先アプリが見つからないため共有をスキップしました")
        return
    }
    val chooserIntent = Intent.createChooser(sendIntent, "共有先を選択").apply {
        if (context !is Activity) {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
    context.startActivity(chooserIntent)
}
