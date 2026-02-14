package com.anagram.analyzer.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anagram.analyzer.ui.viewmodel.MainUiState
import com.anagram.analyzer.ui.viewmodel.MainViewModel

@Composable
fun MainScreen(viewModel: MainViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    MainScreenContent(
        state = state,
        onInputChanged = viewModel::onInputChanged,
    )
}

@Composable
fun MainScreenContent(
    state: MainUiState,
    onInputChanged: (String) -> Unit,
) {
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
                    Text("・$candidate")
                }
            }
        } else {
            Text("文字を入力すると正規化結果とキーを表示します。")
        }
    }
}
