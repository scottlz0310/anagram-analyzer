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
import com.anagram.analyzer.data.datastore.ThemePreferenceStore
import com.anagram.analyzer.ui.screen.MainScreen
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
            MaterialTheme(
                colorScheme = if (isDarkTheme) anagramDarkColorScheme() else anagramLightColorScheme(),
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

private fun anagramLightColorScheme() = lightColorScheme(
    primary = Color(0xFF8E24AA),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFF3E5F5),
    onPrimaryContainer = Color(0xFF2B0A3D),
    secondary = Color(0xFF00897B),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFB2DFDB),
    onSecondaryContainer = Color(0xFF00201D),
    tertiary = Color(0xFFF9A825),
    onTertiary = Color(0xFF2B1D00),
    tertiaryContainer = Color(0xFFFFE082),
    onTertiaryContainer = Color(0xFF2A1800),
)

private fun anagramDarkColorScheme() = darkColorScheme(
    primary = Color(0xFFD6A5F3),
    onPrimary = Color(0xFF3D1A58),
    primaryContainer = Color(0xFF5A2C79),
    onPrimaryContainer = Color(0xFFF5E8FF),
    secondary = Color(0xFF4DD0C4),
    onSecondary = Color(0xFF003731),
    secondaryContainer = Color(0xFF005047),
    onSecondaryContainer = Color(0xFFB7FFF8),
    tertiary = Color(0xFFFFD54F),
    onTertiary = Color(0xFF3E2E00),
    tertiaryContainer = Color(0xFF5A4300),
    onTertiaryContainer = Color(0xFFFFE082),
)
