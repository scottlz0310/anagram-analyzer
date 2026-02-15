package com.anagram.analyzer.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anagram.analyzer.ui.viewmodel.MainUiState
import com.anagram.analyzer.ui.viewmodel.MainViewModel

private const val MIN_SEARCH_LENGTH = 1
private const val MAX_SEARCH_LENGTH = 20

@Composable
fun MainScreen(
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    viewModel: MainViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    MainScreenContent(
        state = state,
        isDarkTheme = isDarkTheme,
        onToggleTheme = onToggleTheme,
        onInputChanged = viewModel::onInputChanged,
        onSearchLengthRangeChanged = viewModel::onSearchLengthRangeChanged,
        onAdditionalDictionaryDownloadRequested = viewModel::onAdditionalDictionaryDownloadRequested,
    )
}

@Composable
fun MainScreenContent(
    state: MainUiState,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onInputChanged: (String) -> Unit,
    onSearchLengthRangeChanged: (Int, Int) -> Unit,
    onAdditionalDictionaryDownloadRequested: () -> Unit,
) {
    var showAboutDialog by rememberSaveable { mutableStateOf(false) }
    var showSettingsDialog by rememberSaveable { mutableStateOf(false) }
    var selectedCandidate by rememberSaveable { mutableStateOf<String?>(null) }
    var isInputHistoryExpanded by rememberSaveable { mutableStateOf(false) }
    val errorMessage = state.errorMessage
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedTextField(
            value = state.input,
            onValueChange = onInputChanged,
            label = { Text("ひらがな入力") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("input_field"),
            singleLine = true,
        )

        if (errorMessage != null) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
            )
        } else if (state.normalized.isNotEmpty()) {
            Text("正規化: ${state.normalized}")
            Text("キー: ${state.anagramKey}")
            if (state.candidates.isEmpty()) {
                Text("候補: なし")
            } else {
                Text("候補:")
                state.candidates.forEach { candidate ->
                    TextButton(onClick = { selectedCandidate = candidate }) {
                        Text("・$candidate")
                    }
                }
            }
        } else {
            Text("文字を入力すると正規化結果とキーを表示します。")
        }

        if (state.inputHistory.isNotEmpty()) {
            TextButton(
                onClick = { isInputHistoryExpanded = !isInputHistoryExpanded },
                modifier = Modifier.testTag("input_history_toggle_button"),
            ) {
                Text(if (isInputHistoryExpanded) "入力履歴を隠す" else "入力履歴を表示")
            }
            if (isInputHistoryExpanded) {
                Text("入力履歴:", modifier = Modifier.testTag("input_history_title"))
                state.inputHistory.forEachIndexed { index, history ->
                    TextButton(
                        onClick = { onInputChanged(history) },
                        modifier = Modifier.testTag("input_history_item_$index"),
                    ) {
                        Text("履歴: $history")
                    }
                }
            }
        }

        TextButton(
            onClick = { showSettingsDialog = true },
            modifier = Modifier.testTag("settings_button"),
        ) {
            Text("設定")
        }

        TextButton(
            onClick = { showAboutDialog = true },
            modifier = Modifier.testTag("about_button"),
        ) {
            Text("辞書クレジット")
        }
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("辞書クレジット", modifier = Modifier.testTag("about_dialog_title")) },
            text = {
                Text(
                    "このアプリはElectronic Dictionary Research and Development GroupのJMdictデータを使用しています。ライセンス: CC BY-SA 4.0",
                )
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("閉じる")
                }
            },
        )
    }

    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = { Text("設定", modifier = Modifier.testTag("settings_dialog_title")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "文字数範囲: ${state.minSearchLength}〜${state.maxSearchLength}",
                        modifier = Modifier.testTag("settings_length_range"),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(
                            onClick = {
                                onSearchLengthRangeChanged(
                                    (state.minSearchLength - 1).coerceAtLeast(MIN_SEARCH_LENGTH),
                                    state.maxSearchLength,
                                )
                            },
                            modifier = Modifier.testTag("settings_min_decrease_button"),
                        ) {
                            Text("最小-")
                        }
                        TextButton(
                            onClick = {
                                onSearchLengthRangeChanged(
                                    (state.minSearchLength + 1).coerceAtMost(state.maxSearchLength),
                                    state.maxSearchLength,
                                )
                            },
                            modifier = Modifier.testTag("settings_min_increase_button"),
                        ) {
                            Text("最小+")
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(
                            onClick = {
                                onSearchLengthRangeChanged(
                                    state.minSearchLength,
                                    (state.maxSearchLength - 1).coerceAtLeast(state.minSearchLength),
                                )
                            },
                            modifier = Modifier.testTag("settings_max_decrease_button"),
                        ) {
                            Text("最大-")
                        }
                        TextButton(
                            onClick = {
                                onSearchLengthRangeChanged(
                                    state.minSearchLength,
                                    (state.maxSearchLength + 1).coerceAtMost(MAX_SEARCH_LENGTH),
                                )
                            },
                            modifier = Modifier.testTag("settings_max_increase_button"),
                        ) {
                            Text("最大+")
                        }
                    }
                    TextButton(
                        onClick = onToggleTheme,
                        modifier = Modifier.testTag("settings_theme_toggle_button"),
                    ) {
                        Text(if (isDarkTheme) "テーマ: ダーク" else "テーマ: ライト")
                    }
                    TextButton(
                        onClick = onAdditionalDictionaryDownloadRequested,
                        modifier = Modifier.testTag("settings_download_button"),
                    ) {
                        Text("追加辞書をダウンロード")
                    }
                    Text(
                        text = state.settingsMessage ?: "追加辞書ダウンロード機能は準備中です",
                        modifier = Modifier.testTag("settings_download_status"),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showSettingsDialog = false }) {
                    Text("閉じる")
                }
            },
        )
    }

    val detailCandidate = selectedCandidate
    if (detailCandidate != null) {
        val detail = state.candidateDetails[detailCandidate]
        AlertDialog(
            onDismissRequest = { selectedCandidate = null },
            title = { Text("候補詳細", modifier = Modifier.testTag("candidate_detail_dialog_title")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("読み: $detailCandidate", modifier = Modifier.testTag("candidate_detail_reading"))
                    Text(
                        "漢字表記: ${detail?.kanji ?: "（未対応）"}",
                        modifier = Modifier.testTag("candidate_detail_kanji"),
                    )
                    Text(
                        "意味: ${detail?.meaning ?: "（未対応）"}",
                        modifier = Modifier.testTag("candidate_detail_meaning"),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedCandidate = null }) {
                    Text("閉じる")
                }
            },
        )
    }
}
