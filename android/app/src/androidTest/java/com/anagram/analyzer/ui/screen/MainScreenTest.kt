package com.anagram.analyzer.ui.screen

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.anagram.analyzer.ui.viewmodel.MainUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun ひらがな入力で候補を表示できる() {
        var uiState by mutableStateOf(MainUiState())

        composeRule.setContent {
            MainScreenContent(
                state = uiState,
                onInputChanged = { input ->
                    uiState = when (input) {
                        "りんご" ->
                            MainUiState(
                                input = input,
                                normalized = "りんご",
                                anagramKey = "ごりん",
                                candidates = listOf("りんご"),
                            )

                        else ->
                            MainUiState(
                                input = input,
                                normalized = input,
                                anagramKey = input.toList().sorted().joinToString(""),
                            )
                    }
                },
            )
        }

        composeRule.onNodeWithTag("input_field").performTextInput("りんご")

        composeRule.onNodeWithText("正規化: りんご").assertIsDisplayed()
        composeRule.onNodeWithText("キー: ごりん").assertIsDisplayed()
        composeRule.onNodeWithText("候補:").assertIsDisplayed()
        composeRule.onNodeWithText("・りんご").assertIsDisplayed()
    }

    @Test
    fun 非ひらがな入力でエラーを表示できる() {
        var uiState by mutableStateOf(MainUiState())

        composeRule.setContent {
            MainScreenContent(
                state = uiState,
                onInputChanged = { input ->
                    val hasNonHiragana = input.any { it !in 'ぁ'..'ゖ' }
                    uiState =
                        if (hasNonHiragana) {
                            MainUiState(
                                input = input,
                                errorMessage = "ひらがな以外の文字が含まれています",
                            )
                        } else {
                            MainUiState(
                                input = input,
                                normalized = input,
                                anagramKey = input.toList().sorted().joinToString(""),
                            )
                        }
                },
            )
        }

        composeRule.onNodeWithTag("input_field").performTextInput("abc")

        composeRule.onNodeWithText("ひらがな以外の文字が含まれています").assertIsDisplayed()
    }
}
