package com.anagram.analyzer.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anagram.analyzer.R
import com.anagram.analyzer.ui.viewmodel.MainUiState
import com.anagram.analyzer.ui.viewmodel.MainViewModel

private const val MAX_VISIBLE_CANDIDATES = 50

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
        onCandidateDetailFetchRequested = viewModel::onCandidateDetailFetchRequested,
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
    onCandidateDetailFetchRequested: (String) -> Unit,
) {
    var showAboutDialog by rememberSaveable { mutableStateOf(false) }
    var showSettingsDialog by rememberSaveable { mutableStateOf(false) }
    var selectedCandidate by rememberSaveable { mutableStateOf<String?>(null) }
    var isInputHistoryExpanded by rememberSaveable { mutableStateOf(false) }
    val detailCandidate = selectedCandidate
    if (detailCandidate != null) {
        val detail = state.candidateDetails[detailCandidate]
        CandidateDetailScreen(
            candidate = detailCandidate,
            kanji = detail?.kanji,
            meaning = detail?.meaning,
            isLoading = state.loadingCandidateDetailWord == detailCandidate,
            errorMessage = if (state.candidateDetailErrorWord == detailCandidate) {
                state.candidateDetailErrorMessage
            } else {
                null
            },
            onFetchDetail = { onCandidateDetailFetchRequested(detailCandidate) },
            onBack = { selectedCandidate = null },
        )
        return
    }
    val errorMessage = state.errorMessage
    val colorScheme = MaterialTheme.colorScheme
    val backgroundBrush = remember(colorScheme.primary, colorScheme.tertiary, colorScheme.background) {
        Brush.verticalGradient(
            listOf(
                colorScheme.primary.copy(alpha = 0.14f),
                colorScheme.tertiary.copy(alpha = 0.10f),
                colorScheme.background,
            ),
        )
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = backgroundBrush)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(id = R.drawable.charactor1),
                contentDescription = null,
                modifier = Modifier.size(92.dp),
                contentScale = ContentScale.Fit,
            )
            Image(
                painter = painterResource(id = R.drawable.charactor2),
                contentDescription = null,
                modifier = Modifier.size(92.dp),
                contentScale = ContentScale.Fit,
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.92f),
            ),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    "Anagram Analyzer",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    "ひらがなを入力して、カラフルに候補を探しましょう。",
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
            ),
        ) {
            OutlinedTextField(
                value = state.input,
                onValueChange = onInputChanged,
                label = { Text("ひらがな入力") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
                    .testTag("input_field"),
                singleLine = true,
            )
        }

        if (errorMessage != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
            ) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(12.dp),
                )
            }
        } else if (state.normalized.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.92f),
                ),
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("正規化: ${state.normalized}", color = MaterialTheme.colorScheme.onSecondaryContainer)
                    Text("キー: ${state.anagramKey}", color = MaterialTheme.colorScheme.onSecondaryContainer)
                    if (state.candidates.isEmpty()) {
                        Text("候補: なし", color = MaterialTheme.colorScheme.onSecondaryContainer)
                    } else {
                        val visibleCandidates = state.candidates.take(MAX_VISIBLE_CANDIDATES)
                        Text("候補:", color = MaterialTheme.colorScheme.onSecondaryContainer)
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 280.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            items(items = visibleCandidates, key = { it }) { candidate ->
                                TextButton(
                                    onClick = { selectedCandidate = candidate },
                                    colors = ButtonDefaults.textButtonColors(
                                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                                    ),
                                ) {
                                    Text("・$candidate")
                                }
                            }
                        }
                        if (state.candidates.size > visibleCandidates.size) {
                            Text(
                                "…ほか ${state.candidates.size - visibleCandidates.size} 件",
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                        }
                    }
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
            ) {
                Text(
                    "文字を入力すると正規化結果とキーを表示します。",
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.padding(12.dp),
                )
            }
        }

        if (state.inputHistory.isNotEmpty()) {
            TextButton(
                onClick = { isInputHistoryExpanded = !isInputHistoryExpanded },
                colors = ButtonDefaults.textButtonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_history_toggle_button"),
            ) {
                Text(if (isInputHistoryExpanded) "入力履歴を隠す" else "入力履歴を表示")
            }
            if (isInputHistoryExpanded) {
                Text("入力履歴:", modifier = Modifier.testTag("input_history_title"))
                state.inputHistory.forEachIndexed { index, history ->
                    TextButton(
                        onClick = { onInputChanged(history) },
                        colors = ButtonDefaults.textButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                        modifier = Modifier.testTag("input_history_item_$index"),
                    ) {
                        Text("履歴: $history")
                    }
                }
            }
        }

        TextButton(
            onClick = { showSettingsDialog = true },
            colors = ButtonDefaults.textButtonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("settings_button"),
        ) {
            Text("設定")
        }

        TextButton(
            onClick = { showAboutDialog = true },
            colors = ButtonDefaults.textButtonColors(
                containerColor = MaterialTheme.colorScheme.tertiary,
                contentColor = MaterialTheme.colorScheme.onTertiary,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("about_button"),
        ) {
            Text("辞書クレジット")
        }

        Image(
            painter = painterResource(id = R.drawable.spot_illustration),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 160.dp),
            contentScale = ContentScale.Fit,
        )
    }

    if (showAboutDialog) {
        AboutDialog(onDismiss = { showAboutDialog = false })
    }

    if (showSettingsDialog) {
        SettingsDialog(
            state = state,
            isDarkTheme = isDarkTheme,
            onToggleTheme = onToggleTheme,
            onSearchLengthRangeChanged = onSearchLengthRangeChanged,
            onAdditionalDictionaryDownloadRequested = onAdditionalDictionaryDownloadRequested,
            onDismiss = { showSettingsDialog = false },
        )
    }
}

