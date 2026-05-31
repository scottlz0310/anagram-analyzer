package com.anagram.analyzer.ui.screen

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anagram.analyzer.domain.model.CharCard
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
        onCardTapped = viewModel::onCardTapped,
        onSlotTapped = viewModel::onSlotTapped,
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
    onCardTapped: (Int) -> Unit,
    onSlotTapped: (Int) -> Unit,
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(
                onClick = onNavigateBack,
                modifier = Modifier.testTag("quiz_to_analysis_button"),
            ) {
                Text("🔍 解析モード")
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
                shuffledCards = state.shuffledCards,
                answerSlots = state.answerSlots,
                selectedCardId = state.selectedCardId,
                errorMessage = state.errorMessage,
                onCardTapped = onCardTapped,
                onSlotTapped = onSlotTapped,
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
    shuffledCards: List<CharCard>,
    answerSlots: List<Int?>,
    selectedCardId: Int?,
    errorMessage: String?,
    onCardTapped: (Int) -> Unit,
    onSlotTapped: (Int) -> Unit,
    onSubmitAnswer: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val selectedChar = shuffledCards.firstOrNull { it.id == selectedCardId }?.char

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "カードをタップして単語を完成させてください",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )

        Text(
            text = selectedChar?.let { "選択中: 「$it」を置くマスをタップ" }
                ?: "カードをタップで追加、マスをタップで取り消しや入れ替えができます",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        QuizSectionCard(title = "もじカード") {
            CharCardGrid(
                cards = shuffledCards,
                selectedCardId = selectedCardId,
                onCardTapped = { cardId ->
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onCardTapped(cardId)
                },
            )
        }

        QuizSectionCard(title = "こたえ") {
            AnswerSlotGrid(
                cards = shuffledCards,
                answerSlots = answerSlots,
                selectedCardId = selectedCardId,
                onSlotTapped = { slotIndex ->
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onSlotTapped(slotIndex)
                },
            )
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
            onClick = onSubmitAnswer,
            enabled = answerSlots.isNotEmpty() && answerSlots.all { it != null },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("こたえる", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun QuizSectionCard(
    title: String,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.82f),
        ),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            content()
        }
    }
}

@Composable
private fun CharCardGrid(
    cards: List<CharCard>,
    selectedCardId: Int?,
    onCardTapped: (Int) -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val cardsPerRow = calculateCardsPerRow(maxWidth)
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            cards.chunked(cardsPerRow).forEach { rowCards ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    rowCards.forEach { card ->
                        QuizCharCard(
                            char = card.char.toString(),
                            isSelected = selectedCardId == card.id,
                            isPlaced = card.isPlaced,
                            onClick = { onCardTapped(card.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AnswerSlotGrid(
    cards: List<CharCard>,
    answerSlots: List<Int?>,
    selectedCardId: Int?,
    onSlotTapped: (Int) -> Unit,
) {
    val cardsById = cards.associateBy { it.id }
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val cardsPerRow = calculateCardsPerRow(maxWidth)
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            answerSlots.chunked(cardsPerRow).forEachIndexed { rowIndex, rowSlots ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    rowSlots.forEachIndexed { columnIndex, cardId ->
                        val slotIndex = rowIndex * cardsPerRow + columnIndex
                        val isActionable = selectedCardId != null || cardId != null
                        AnswerSlot(
                            value = cardId?.let { cardsById[it]?.char?.toString() }.orEmpty(),
                            isFilled = cardId != null,
                            isHighlighted = selectedCardId != null,
                            enabled = isActionable,
                            onClick = { onSlotTapped(slotIndex) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuizCharCard(
    char: String,
    isSelected: Boolean,
    isPlaced: Boolean,
    onClick: () -> Unit,
) {
    val containerColor by animateColorAsState(
        targetValue = when {
            isSelected -> MaterialTheme.colorScheme.primary
            isPlaced -> MaterialTheme.colorScheme.surfaceVariant
            else -> MaterialTheme.colorScheme.secondaryContainer
        },
        label = "charCardColor",
    )
    val contentColor by animateColorAsState(
        targetValue = when {
            isSelected -> MaterialTheme.colorScheme.onPrimary
            isPlaced -> MaterialTheme.colorScheme.onSurfaceVariant
            else -> MaterialTheme.colorScheme.onSecondaryContainer
        },
        label = "charCardContentColor",
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
        } else {
            MaterialTheme.colorScheme.outline.copy(alpha = if (isPlaced) 0.45f else 0.8f)
        },
        label = "charCardBorderColor",
    )

    Card(
        modifier = Modifier
            .padding(horizontal = QUIZ_CARD_HORIZONTAL_PADDING)
            .size(QUIZ_CARD_SIZE)
            .graphicsLayer {
                alpha = if (isPlaced) 0.4f else 1f
            }
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(width = 2.dp, color = borderColor),
        shape = RoundedCornerShape(16.dp),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = char,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = contentColor,
            )
        }
    }
}

@Composable
private fun AnswerSlot(
    value: String,
    isFilled: Boolean,
    isHighlighted: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val containerColor by animateColorAsState(
        targetValue = when {
            isFilled -> MaterialTheme.colorScheme.tertiaryContainer
            isHighlighted -> MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
            else -> MaterialTheme.colorScheme.surface
        },
        label = "answerSlotColor",
    )
    val borderColor by animateColorAsState(
        targetValue = when {
            isHighlighted -> MaterialTheme.colorScheme.primary
            isFilled -> MaterialTheme.colorScheme.tertiary
            else -> MaterialTheme.colorScheme.outline
        },
        label = "answerSlotBorderColor",
    )
    val borderWidth by animateDpAsState(
        targetValue = when {
            isHighlighted -> 3.dp
            isFilled -> 2.dp
            else -> 1.dp
        },
        label = "answerSlotBorderWidth",
    )
    val scale by animateFloatAsState(
        targetValue = if (isFilled) 1f else 0.88f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "answerSlotScale",
    )

    Card(
        modifier = Modifier
            .padding(horizontal = QUIZ_CARD_HORIZONTAL_PADDING)
            .size(QUIZ_CARD_SIZE)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = enabled, onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(width = borderWidth, color = borderColor),
        shape = RoundedCornerShape(16.dp),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = value.ifEmpty { "・" },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = if (isFilled) {
                    MaterialTheme.colorScheme.onTertiaryContainer
                } else {
                    MaterialTheme.colorScheme.outline
                },
            )
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

private fun calculateCardsPerRow(maxWidth: Dp): Int = (maxWidth / QUIZ_CARD_FOOTPRINT).toInt().coerceAtLeast(1)

private val QUIZ_CARD_SIZE = 56.dp
private val QUIZ_CARD_HORIZONTAL_PADDING = 6.dp
private val QUIZ_CARD_FOOTPRINT = QUIZ_CARD_SIZE + (QUIZ_CARD_HORIZONTAL_PADDING * 2)
