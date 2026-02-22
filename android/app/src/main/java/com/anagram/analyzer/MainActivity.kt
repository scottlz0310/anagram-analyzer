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
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.anagram.analyzer.data.datastore.ThemePreferenceStore
import com.anagram.analyzer.ui.screen.MainScreen
import com.anagram.analyzer.ui.screen.QuizScreen
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
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
            var showQuiz by rememberSaveable { mutableStateOf(false) }
            MaterialTheme(
                colorScheme = if (isDarkTheme) anagramDarkColorScheme() else anagramLightColorScheme(),
            ) {
                Surface {
                    if (showQuiz) {
                        QuizScreen(
                            onNavigateBack = { showQuiz = false },
                        )
                    } else {
                        MainScreen(
                            isDarkTheme = isDarkTheme,
                            onToggleTheme = {
                                scope.launch {
                                    themePreferenceStore.setDarkTheme(!isDarkTheme)
                                }
                            },
                            onNavigateToQuiz = { showQuiz = true },
                        )
                    }
                }
            }
        }
    }
}

private fun anagramLightColorScheme() = lightColorScheme(
    primary = Color(0xFFFF8AAE),
    onPrimary = Color(0xFF4F1D31),
    primaryContainer = Color(0xFFFFD9E5),
    onPrimaryContainer = Color(0xFF3A0E22),
    secondary = Color(0xFF6EDDD3),
    onSecondary = Color(0xFF003732),
    secondaryContainer = Color(0xFFCFFAF4),
    onSecondaryContainer = Color(0xFF00201D),
    tertiary = Color(0xFFC39BFF),
    onTertiary = Color(0xFF34175C),
    tertiaryContainer = Color(0xFFEBDFFF),
    onTertiaryContainer = Color(0xFF280B4D),
    background = Color(0xFFFFF8E7),
    onBackground = Color(0xFF2F2430),
    surface = Color(0xFFFFF8E7),
    onSurface = Color(0xFF2F2430),
)

private fun anagramDarkColorScheme() = darkColorScheme(
    primary = Color(0xFFFFB3C8),
    onPrimary = Color(0xFF5A1C33),
    primaryContainer = Color(0xFF7A2D49),
    onPrimaryContainer = Color(0xFFFFDCE7),
    secondary = Color(0xFF8AE9E1),
    onSecondary = Color(0xFF003A35),
    secondaryContainer = Color(0xFF00504A),
    onSecondaryContainer = Color(0xFFB7FFF8),
    tertiary = Color(0xFFD7BEFF),
    onTertiary = Color(0xFF43206E),
    tertiaryContainer = Color(0xFF5A378B),
    onTertiaryContainer = Color(0xFFF1E7FF),
    background = Color(0xFF1F1A23),
    onBackground = Color(0xFFF2E8F2),
    surface = Color(0xFF1F1A23),
    onSurface = Color(0xFFF2E8F2),
)
