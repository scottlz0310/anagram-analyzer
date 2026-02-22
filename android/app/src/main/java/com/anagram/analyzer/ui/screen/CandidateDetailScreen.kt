package com.anagram.analyzer.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
internal fun CandidateDetailScreen(
    candidate: String,
    kanji: String?,
    meaning: String?,
    isLoading: Boolean,
    errorMessage: String?,
    onFetchDetail: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val shareMeaning = meaning
    var isMeaningSelectionMode by rememberSaveable(candidate, shareMeaning) { mutableStateOf(false) }
    BackHandler(onBack = onBack)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            "候補詳細",
            modifier = Modifier.testTag("candidate_detail_screen_title"),
            style = MaterialTheme.typography.titleLarge,
        )
        Text("読み: $candidate", modifier = Modifier.testTag("candidate_detail_reading"))
        Text(
            "漢字表記: ${kanji ?: "（未対応）"}",
            modifier = Modifier.testTag("candidate_detail_kanji"),
        )
        if (shareMeaning == null) {
            Text(
                "意味: （未対応）",
                modifier = Modifier.testTag("candidate_detail_meaning"),
            )
        } else if (isMeaningSelectionMode) {
            SelectionContainer {
                Text(
                    "意味: $shareMeaning",
                    modifier = Modifier.testTag("candidate_detail_meaning"),
                )
            }
        } else {
            Text(
                "意味: $shareMeaning",
                modifier = Modifier
                    .testTag("candidate_detail_meaning")
                    .combinedClickable(
                        onClick = { isMeaningSelectionMode = true },
                        onLongClick = { isMeaningSelectionMode = true },
                    ),
            )
        }
        if (shareMeaning != null) {
            if (isMeaningSelectionMode) {
                Text(
                    "意味テキストを選択中です",
                    modifier = Modifier.testTag("candidate_detail_meaning_selection_state"),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    onClick = {
                        shareCandidateDetail(
                            context = context,
                            candidate = candidate,
                            kanji = kanji,
                            meaning = shareMeaning,
                        )
                    },
                    modifier = Modifier.testTag("candidate_detail_share_button"),
                ) {
                    Text("共有")
                }
                if (isMeaningSelectionMode) {
                    TextButton(
                        onClick = { isMeaningSelectionMode = false },
                        modifier = Modifier.testTag("candidate_detail_selection_clear_button"),
                    ) {
                        Text("選択解除")
                    }
                }
            }
        }
        if (kanji == null || meaning == null) {
            TextButton(
                onClick = onFetchDetail,
                enabled = !isLoading,
                modifier = Modifier.testTag("candidate_detail_fetch_button"),
            ) {
                Text(
                    when {
                        isLoading -> "詳細を取得中..."
                        errorMessage != null -> "詳細を再取得"
                        else -> "詳細を取得"
                    },
                )
            }
            if (isLoading) {
                Text(
                    "オンライン辞書から候補詳細を取得しています...",
                    modifier = Modifier.testTag("candidate_detail_loading_text"),
                )
            }
            if (errorMessage != null) {
                Text(
                    errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.testTag("candidate_detail_error_text"),
                )
            }
        }
        TextButton(
            onClick = onBack,
            modifier = Modifier.testTag("candidate_detail_back_button"),
        ) {
            Text("戻る")
        }
    }
}
