package com.anagram.analyzer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import com.anagram.analyzer.data.datastore.ThemePreferenceStore
import com.anagram.analyzer.ui.screen.MainScreen
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect

@AndroidEntryPoint(ComponentActivity::class)
class MainActivity : Hilt_MainActivity() {
    @Inject
    lateinit var themePreferenceStore: ThemePreferenceStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val isDarkThemeState by produceState<Boolean?>(initialValue = null, themePreferenceStore) {
                themePreferenceStore.isDarkTheme.collect { value = it }
            }
            val isDarkTheme = isDarkThemeState ?: return@setContent
            val scope = rememberCoroutineScope()
            MaterialTheme(
                colorScheme = if (isDarkTheme) darkColorScheme() else lightColorScheme(),
            ) {
                Surface {
                    MainScreen(
                        isDarkTheme = isDarkTheme,
                        onToggleTheme = {
                            scope.launch {
                                themePreferenceStore.setDarkTheme(!isDarkTheme)
                            }
                        },
                    )
                }
            }
        }
    }
}
