package app.hermes.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.hermes.R
import app.hermes.core.ui.component.HermesScreen
import app.hermes.core.ui.theme.HermesSpacing
import app.hermes.data.ProviderChoice
import app.hermes.data.ThemeMode

/**
 * Settings: theme selection (persisted immediately) and the current AI provider, with a
 * re-entry into onboarding to change it. Changing the provider never resets
 * onboardingComplete (D34); the API-key entry itself is M1.
 */
@Composable
fun SettingsScreen(
    onChangeProvider: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    HermesScreen(modifier = modifier) {
        Text(stringResource(R.string.settings_theme), style = MaterialTheme.typography.titleMedium)
        ThemeMode.entries.forEach { mode ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.setTheme(mode) }
                    .padding(vertical = HermesSpacing.xs),
                horizontalArrangement = Arrangement.spacedBy(HermesSpacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = settings.theme == mode, onClick = { viewModel.setTheme(mode) })
                Text(stringResource(themeLabel(mode)))
            }
        }

        Spacer(Modifier.height(HermesSpacing.lg))

        Text(stringResource(R.string.settings_provider), style = MaterialTheme.typography.titleMedium)
        Text(
            text = stringResource(providerLabel(settings.provider)),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(HermesSpacing.sm))
        OutlinedButton(onClick = onChangeProvider) {
            Text(stringResource(R.string.settings_change_provider))
        }
    }
}

private fun themeLabel(mode: ThemeMode): Int = when (mode) {
    ThemeMode.SYSTEM -> R.string.theme_system
    ThemeMode.LIGHT -> R.string.theme_light
    ThemeMode.DARK -> R.string.theme_dark
}

private fun providerLabel(provider: ProviderChoice?): Int = when (provider) {
    ProviderChoice.API_KEY -> R.string.provider_api_key
    ProviderChoice.ON_DEVICE -> R.string.provider_on_device
    ProviderChoice.DEMO -> R.string.provider_demo
    null -> R.string.provider_none
}
