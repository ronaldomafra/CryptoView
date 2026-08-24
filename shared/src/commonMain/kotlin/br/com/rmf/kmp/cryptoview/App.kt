package br.com.rmf.kmp.cryptoview

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import br.com.rmf.kmp.cryptoview.ui.navigation.AppNavigation
import br.com.rmf.kmp.cryptoview.ui.screens.OnboardingScreen
import br.com.rmf.kmp.cryptoview.ui.theme.CryptoViewTheme

@Composable
@Preview
fun App() {
    var apiKeyConfigured by rememberSaveable { mutableStateOf(false) }

    CryptoViewTheme {
        Surface(Modifier.fillMaxSize()) {
            if (apiKeyConfigured) {
                AppNavigation(onApiKeyReset = { apiKeyConfigured = false })
            } else {
                OnboardingScreen(onContinue = { apiKeyConfigured = true })
            }
        }
    }
}
