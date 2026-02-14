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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.anagram.analyzer.ui.viewmodel.MainViewModel
import com.anagram.analyzer.ui.viewmodel.MainViewModelFactory

@Composable
fun MainScreen() {
    val context = LocalContext.current.applicationContext
    val viewModel: MainViewModel = viewModel(
        factory = MainViewModelFactory(context),
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
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
            onValueChange = viewModel::onInputChanged,
            label = { Text("ひらがな入力") },
            modifier = Modifier.fillMaxWidth(),
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
