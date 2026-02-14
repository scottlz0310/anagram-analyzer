package com.anagram.analyzer.ui.screen

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.anagram.analyzer.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun ひらがな入力で候補を表示できる() {
        composeRule.onNodeWithTag("input_field").performTextInput("りんご")
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("・りんご").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithText("正規化: りんご").assertIsDisplayed()
        composeRule.onNodeWithText("キー: ごりん").assertIsDisplayed()
        composeRule.onNodeWithText("候補:").assertIsDisplayed()
        composeRule.onNodeWithText("・りんご").assertIsDisplayed()
    }

    @Test
    fun 非ひらがな入力でエラーを表示できる() {
        composeRule.onNodeWithTag("input_field").performTextInput("abc")
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule
                .onAllNodesWithText("ひらがな以外の文字が含まれています", substring = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        composeRule.onNodeWithText("ひらがな以外の文字が含まれています", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun 辞書クレジットを表示できる() {
        composeRule.onNodeWithTag("about_button").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule
                .onAllNodesWithText("ライセンス: CC BY-SA 4.0", substring = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        composeRule
            .onNodeWithText("このアプリはElectronic Dictionary Research and Development GroupのJMdictデータを使用しています。", substring = true)
            .assertIsDisplayed()
        composeRule.onNodeWithText("閉じる").assertIsDisplayed()
    }
}
