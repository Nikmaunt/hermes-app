package app.hermes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.hermes.core.ui.theme.HermesTheme
import app.hermes.data.ThemeMode
import app.hermes.navigation.HermesAppRoot
import dagger.hilt.android.AndroidEntryPoint

/**
 * The single activity. It waits for the first settings emission, then applies the theme
 * and hands off to the navigation root, whose start destination is the onboarding gate.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            settings?.let { current ->
                HermesTheme(darkTheme = current.theme.isDark()) {
                    HermesAppRoot(startOnboarded = current.onboardingComplete)
                }
            }
        }
    }
}

@Composable
private fun ThemeMode.isDark(): Boolean = when (this) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}
