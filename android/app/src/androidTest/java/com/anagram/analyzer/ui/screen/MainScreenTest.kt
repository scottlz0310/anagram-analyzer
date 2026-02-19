package com.anagram.analyzer.ui.screen

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.espresso.Espresso.pressBack
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.anagram.analyzer.MainActivity
import org.junit.Assert.assertTrue
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
        composeRule.waitUntil(timeoutMillis = 30_000) {
            composeRule.onAllNodesWithText("・りんご").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithText("正規化: りんご").assertIsDisplayed()
        composeRule.onNodeWithText("キー: ごりん").assertIsDisplayed()
        composeRule.onNodeWithText("候補:").assertIsDisplayed()
        composeRule.onNodeWithText("・りんご").assertIsDisplayed()
    }

    @Test
    fun 入力履歴を表示できる() {
        composeRule.onNodeWithTag("input_field").performTextInput("りんご")
        composeRule.waitUntil(timeoutMillis = 30_000) {
            composeRule.onAllNodesWithTag("input_history_toggle_button").fetchSemanticsNodes().isNotEmpty()
        }

        assertTrue(
            composeRule.onAllNodesWithTag("input_history_title").fetchSemanticsNodes().isEmpty(),
        )
        composeRule.onNodeWithTag("input_history_toggle_button").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("履歴: りんご").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithTag("input_history_title").assertIsDisplayed()
        composeRule.onNodeWithText("履歴: りんご").assertIsDisplayed()

        composeRule.onNodeWithTag("input_history_toggle_button").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("履歴: りんご").fetchSemanticsNodes().isEmpty()
        }
    }

    @Test
    fun 設定画面で文字数範囲と追加辞書項目を表示できる() {
        composeRule.onNodeWithTag("settings_button").performClick()
        composeRule.onNodeWithTag("settings_dialog_title").assertIsDisplayed()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("文字数範囲", substring = true).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("文字数範囲", substring = true).assertIsDisplayed()

        composeRule.onNodeWithTag("settings_max_decrease_button").performClick()

        composeRule.onNodeWithTag("settings_download_button").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule
                .onAllNodesWithText("追加辞書を適用しました", substring = true)
                .fetchSemanticsNodes()
                .isNotEmpty() ||
                composeRule
                    .onAllNodesWithText("追加辞書は最新です", substring = true)
                    .fetchSemanticsNodes()
                    .isNotEmpty()
        }
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
        composeRule.onNodeWithTag("about_dialog_title").assertIsDisplayed()
        composeRule.onNodeWithText("閉じる").assertIsDisplayed()
    }

    @Test
    fun 候補詳細画面を表示できる() {
        composeRule.onNodeWithTag("input_field").performTextInput("りんご")
        composeRule.waitUntil(timeoutMillis = 30_000) {
            composeRule.onAllNodesWithText("・りんご").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithText("・りんご").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("candidate_detail_screen_title").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithTag("candidate_detail_screen_title").assertIsDisplayed()
        composeRule.onNodeWithTag("candidate_detail_reading").assertIsDisplayed()
        composeRule.onNodeWithTag("candidate_detail_kanji").assertTextContains("漢字表記: 林檎")
        composeRule.onNodeWithTag("candidate_detail_meaning").assertTextContains("意味: apple")
        composeRule.onNodeWithTag("candidate_detail_back_button").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("candidate_detail_screen_title").fetchSemanticsNodes().isEmpty()
        }
        composeRule.onNodeWithTag("input_field").assertIsDisplayed()
    }

    @Test
    fun 候補詳細が未対応の場合はフォールバック表示になる() {
        composeRule.onNodeWithTag("input_field").performTextInput("おなじ")
        composeRule.waitUntil(timeoutMillis = 30_000) {
            composeRule.onAllNodesWithText("・おなじ").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithText("・おなじ").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("candidate_detail_screen_title").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithTag("candidate_detail_screen_title").assertIsDisplayed()
        composeRule.onNodeWithTag("candidate_detail_kanji").assertTextContains("漢字表記: （未対応）")
        composeRule.onNodeWithTag("candidate_detail_meaning").assertTextContains("意味: （未対応）")
        composeRule.onNodeWithTag("candidate_detail_fetch_button").assertIsDisplayed()
    }

    @Test
    fun 候補詳細画面でシステム戻るキー操作で戻れる() {
        composeRule.onNodeWithTag("input_field").performTextInput("りんご")
        composeRule.waitUntil(timeoutMillis = 30_000) {
            composeRule.onAllNodesWithText("・りんご").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithText("・りんご").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("candidate_detail_screen_title").fetchSemanticsNodes().isNotEmpty()
        }

        pressBack()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithTag("candidate_detail_screen_title").fetchSemanticsNodes().isEmpty()
        }
        composeRule.onNodeWithTag("input_field").assertIsDisplayed()
    }

    @Test
    fun テーマ切替ボタンで表示を変更できる() {
        composeRule.onNodeWithTag("settings_button").performClick()
        composeRule.onNodeWithTag("settings_theme_toggle_button").assertIsDisplayed()

        val lightLabel = "テーマ: ライト"
        val darkLabel = "テーマ: ダーク"
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(lightLabel).fetchSemanticsNodes().isNotEmpty() ||
                composeRule.onAllNodesWithText(darkLabel).fetchSemanticsNodes().isNotEmpty()
        }
        val beforeToggle = if (composeRule.onAllNodesWithText(lightLabel).fetchSemanticsNodes().isNotEmpty()) {
            lightLabel
        } else {
            darkLabel
        }
        assertTrue(beforeToggle == lightLabel || beforeToggle == darkLabel)
        val expectedAfterToggle = if (beforeToggle == lightLabel) darkLabel else lightLabel

        composeRule.onNodeWithTag("settings_theme_toggle_button").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(expectedAfterToggle).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(expectedAfterToggle).assertIsDisplayed()

    }
}
