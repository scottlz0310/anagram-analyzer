package com.anagram.analyzer.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anagram.analyzer.domain.model.QuizDifficulty
import com.anagram.analyzer.ui.viewmodel.QuizPhase
import com.anagram.analyzer.ui.viewmodel.QuizUiState
import com.anagram.analyzer.ui.viewmodel.QuizViewModel

@Composable
fun QuizScreen(
    onNavigateBack: () -> Unit,
    viewModel: QuizViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    QuizScreenContent(
        state = state,
        onNavigateBack = onNavigateBack,
        onDifficultySelected = viewModel::onDifficultySelected,
        onStartQuiz = viewModel::onStartQuiz,
        onInputAnswerChanged = viewModel::onInputAnswerChanged,
        onSubmitAnswer = viewModel::onSubmitAnswer,
        onNextQuestion = viewModel::onNextQuestion,
        onReset = viewModel::onReset,
    )
}

@Composable
fun QuizScreenContent(
    state: QuizUiState,
    onNavigateBack: () -> Unit,
    onDifficultySelected: (QuizDifficulty) -> Unit,
    onStartQuiz: () -> Unit,
    onInputAnswerChanged: (String) -> Unit,
    onSubmitAnswer: () -> Unit,
    onNextQuestion: () -> Unit,
    onReset: () -> Unit,
) {
    val gradient = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
            MaterialTheme.colorScheme.background,
        ),
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(gradient)
            .padding(16.dp),
    ) {
        // ヘッダー
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onNavigateBack) {
                Text("← もどる")
            }
            Text(
                text = "クイズモード",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            TextButton(onClick = onReset) {
                Text("リセット", color = MaterialTheme.colorScheme.error)
            }
        }

        // スコアバー
        ScoreBar(
            score = state.score,
            streak = state.streak,
            bestStreak = state.bestStreak,
        )

        Spacer(modifier = Modifier.height(16.dp))

        when (state.phase) {
            QuizPhase.IDLE -> IdleSection(
                selectedDifficulty = state.difficulty,
                errorMessage = state.errorMessage,
                onDifficultySelected = onDifficultySelected,
                onStartQuiz = onStartQuiz,
            )
            QuizPhase.LOADING -> Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(modifier = Modifier.size(48.dp))
            }
            QuizPhase.ANSWERING -> AnsweringSection(
                shuffledChars = state.question?.shuffledChars ?: "",
                inputAnswer = state.inputAnswer,
                errorMessage = state.errorMessage,
                onInputAnswerChanged = onInputAnswerChanged,
                onSubmitAnswer = onSubmitAnswer,
            )
            QuizPhase.CORRECT -> ResultSection(
                isCorrect = true,
                correctWords = state.question?.correctWords ?: emptyList(),
                onNextQuestion = onNextQuestion,
            )
            QuizPhase.INCORRECT -> ResultSection(
                isCorrect = false,
                correctWords = state.question?.correctWords ?: emptyList(),
                onNextQuestion = onNextQuestion,
            )
        }
    }
}

@Composable
private fun ScoreBar(score: Int, streak: Int, bestStreak: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            ScoreItem(label = "スコア", value = "$score")
            ScoreItem(label = "連続正解", value = "$streak")
            ScoreItem(label = "最高連続", value = "$bestStreak")
        }
    }
}

@Composable
private fun ScoreItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.labelSmall)
        Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun IdleSection(
    selectedDifficulty: QuizDifficulty,
    errorMessage: String?,
    onDifficultySelected: (QuizDifficulty) -> Unit,
    onStartQuiz: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "難易度を選んでください",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            QuizDifficulty.entries.forEach { difficulty ->
                val isSelected = difficulty == selectedDifficulty
                OutlinedButton(
                    onClick = { onDifficultySelected(difficulty) },
                    modifier = Modifier.weight(1f),
                    colors = if (isSelected) {
                        ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        ButtonDefaults.outlinedButtonColors()
                    },
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = difficulty.label, fontWeight = FontWeight.Bold)
                        Text(
                            text = "${difficulty.minLen}〜${difficulty.maxLen}文字",
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }
        }

        if (errorMessage != null) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
        }

        Button(
            onClick = onStartQuiz,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("スタート！", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun AnsweringSection(
    shuffledChars: String,
    inputAnswer: String,
    errorMessage: String?,
    onInputAnswerChanged: (String) -> Unit,
    onSubmitAnswer: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "この文字を並べ替えてできる単語は？",
            style = MaterialTheme.typography.titleMedium,
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
            ),
            shape = RoundedCornerShape(16.dp),
        ) {
            Text(
                text = shuffledChars,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                letterSpacing = 8.sp,
            )
        }

        OutlinedTextField(
            value = inputAnswer,
            onValueChange = onInputAnswerChanged,
            label = { Text("答えをひらがなで入力") },
            isError = errorMessage != null,
            supportingText = errorMessage?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        Button(
            onClick = onSubmitAnswer,
            enabled = inputAnswer.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("こたえる", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ResultSection(
    isCorrect: Boolean,
    correctWords: List<String>,
    onNextQuestion: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = if (isCorrect) "⭕ 正解！" else "❌ 不正解",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = if (isCorrect) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
            shape = RoundedCornerShape(12.dp),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "正解の単語（${correctWords.size}件）:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                correctWords.take(10).forEach { word ->
                    Text(
                        text = word,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                    )
                }
                if (correctWords.size > 10) {
                    Text(
                        text = "…ほか${correctWords.size - 10}件",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Button(
            onClick = onNextQuestion,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("つぎの問題", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}
