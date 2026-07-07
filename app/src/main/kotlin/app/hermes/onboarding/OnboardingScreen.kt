package app.hermes.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.hermes.R
import app.hermes.core.ui.component.HermesScreen
import app.hermes.core.ui.theme.HermesSpacing

/**
 * Onboarding UI over the [OnboardingViewModel]. It renders per [OnboardingState] and
 * calls [onFinished] once the flow is terminal (the provider is persisted by the VM).
 */
@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state) {
        if (state is OnboardingState.Ready) onFinished()
    }
    HermesScreen(modifier = modifier) {
        when (state) {
            OnboardingState.PathSelection -> PathSelection(onEvent = viewModel::dispatch)
            OnboardingState.OnDeviceChecking -> Checking()
            OnboardingState.OnDeviceUnavailable -> Unavailable(onEvent = viewModel::dispatch)
            is OnboardingState.Ready -> Unit
        }
    }
}

@Composable
private fun PathSelection(onEvent: (OnboardingEvent) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(HermesSpacing.md)) {
        Text(stringResource(R.string.onboarding_title), style = MaterialTheme.typography.headlineSmall)
        Text(
            text = stringResource(R.string.onboarding_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(onClick = { onEvent(OnboardingEvent.SelectApiKey) }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.onboarding_path_api_key))
        }
        Button(onClick = { onEvent(OnboardingEvent.SelectOnDevice) }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.onboarding_path_on_device))
        }
        OutlinedButton(onClick = { onEvent(OnboardingEvent.SelectDemo) }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.onboarding_path_demo))
        }
    }
}

@Composable
private fun Checking() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(HermesSpacing.md, Alignment.CenterVertically),
    ) {
        CircularProgressIndicator()
        Text(stringResource(R.string.onboarding_checking), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun Unavailable(onEvent: (OnboardingEvent) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(HermesSpacing.md)) {
        Text(stringResource(R.string.onboarding_unavailable_title), style = MaterialTheme.typography.titleLarge)
        Text(
            text = stringResource(R.string.onboarding_unavailable_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(onClick = { onEvent(OnboardingEvent.FallbackToApiKey) }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.onboarding_use_api_key))
        }
        OutlinedButton(onClick = { onEvent(OnboardingEvent.FallbackToDemo) }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.onboarding_try_demo))
        }
        TextButton(onClick = { onEvent(OnboardingEvent.Back) }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.onboarding_back))
        }
    }
}
