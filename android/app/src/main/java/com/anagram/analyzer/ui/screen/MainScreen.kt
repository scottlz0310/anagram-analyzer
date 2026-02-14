package com.anagram.analyzer.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
    )
}

@Composable
fun MainScreenContent(
    state: MainUiState,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onInputChanged: (String) -> Unit,
) {
    var showAboutDialog by rememberSaveable { mutableStateOf(false) }
    var selectedCandidate by rememberSaveable { mutableStateOf<String?>(null) }
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

        TextButton(
            onClick = { showAboutDialog = true },
            modifier = Modifier.testTag("about_button"),
        ) {
            Text("辞書クレジット")
        }

        TextButton(
            onClick = onToggleTheme,
            modifier = Modifier.testTag("theme_toggle_button"),
        ) {
            Text(if (isDarkTheme) "テーマ: ダーク" else "テーマ: ライト")
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

    val detailCandidate = selectedCandidate
    if (detailCandidate != null) {
        AlertDialog(
            onDismissRequest = { selectedCandidate = null },
            title = { Text("候補詳細", modifier = Modifier.testTag("candidate_detail_dialog_title")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("読み: $detailCandidate", modifier = Modifier.testTag("candidate_detail_reading"))
                    Text("漢字表記: （未対応）")
                    Text("意味: （未対応）")
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
