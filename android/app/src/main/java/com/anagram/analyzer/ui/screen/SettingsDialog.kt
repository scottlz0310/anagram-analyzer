package com.anagram.analyzer.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.anagram.analyzer.ui.viewmodel.MainUiState

@Composable
internal fun AboutDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("辞書クレジット", modifier = Modifier.testTag("about_dialog_title")) },
        text = {
            Text(
                "このアプリはElectronic Dictionary Research and Development GroupのJMdictデータを使用しています。ライセンス: CC BY-SA 4.0",
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("閉じる")
            }
        },
    )
}

@Composable
internal fun SettingsDialog(
    state: MainUiState,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onSearchLengthRangeChanged: (Int, Int) -> Unit,
    onAdditionalDictionaryDownloadRequested: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("設定", modifier = Modifier.testTag("settings_dialog_title")) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    "文字数範囲: ${state.minSearchLength}〜${state.maxSearchLength}",
                    modifier = Modifier.testTag("settings_length_range"),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            onSearchLengthRangeChanged(
                                state.minSearchLength - 1,
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
                                state.minSearchLength + 1,
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
                                state.maxSearchLength - 1,
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
                                state.maxSearchLength + 1,
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
                    enabled = !state.isAdditionalDictionaryDownloading,
                    modifier = Modifier.testTag("settings_download_button"),
                ) {
                    Text(if (state.isAdditionalDictionaryDownloading) "追加辞書を適用中..." else "追加辞書をダウンロード")
                }
                Text(
                    text = state.settingsMessage ?: "追加辞書は未適用です",
                    modifier = Modifier.testTag("settings_download_status"),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("閉じる")
            }
        },
    )
}
