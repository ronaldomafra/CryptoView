package br.com.rmf.kmp.cryptoview

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.rmf.kmp.cryptoview.ui.model.AppUiState
import br.com.rmf.kmp.cryptoview.ui.navigation.AppNavigation
import br.com.rmf.kmp.cryptoview.ui.screens.OnboardingScreen
import br.com.rmf.kmp.cryptoview.ui.theme.CryptoViewTheme
import br.com.rmf.kmp.cryptoview.ui.viewmodel.AppViewModel
import org.koin.mp.KoinPlatformTools

@Composable
@Preview
fun App() {
    val appViewModel = viewModel<AppViewModel> {
        KoinPlatformTools.defaultContext().get().get<AppViewModel>()
    }
    val state by appViewModel.uiState.collectAsStateWithLifecycle()

    CryptoViewTheme {
        Surface(Modifier.fillMaxSize()) {
            when (val current = state) {
                AppUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                is AppUiState.NeedsApiKey -> OnboardingScreen(
                    state = current,
                    onContinue = appViewModel::validateAndSave,
                    onCancel = if (current.replacing) appViewModel::cancelReplacement else null,
                )
                is AppUiState.Ready -> AppNavigation(
                    keyInfo = current.keyInfo,
                    validationMessage = current.validationMessage,
                    onRevalidateKey = appViewModel::revalidate,
                    onReplaceKey = appViewModel::beginReplacement,
                    onRemoveKey = appViewModel::removeApiKey,
                )
                is AppUiState.Unavailable -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(current.message, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
