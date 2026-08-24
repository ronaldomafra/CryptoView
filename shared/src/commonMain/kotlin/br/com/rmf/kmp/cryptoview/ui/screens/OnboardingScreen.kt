package br.com.rmf.kmp.cryptoview.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import br.com.rmf.kmp.cryptoview.ui.components.CryptoViewLogo
import br.com.rmf.kmp.cryptoview.ui.components.OutlinedCard
import br.com.rmf.kmp.cryptoview.ui.components.PrimaryActionButton
import br.com.rmf.kmp.cryptoview.ui.theme.CryptoBorder
import br.com.rmf.kmp.cryptoview.ui.theme.CryptoOrange

@Composable
fun OnboardingScreen(onContinue: () -> Unit) {
    var apiKey by rememberSaveable { mutableStateOf("cmc_demo_key_for_navigation") }
    var showKey by rememberSaveable { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 24.dp),
    ) {
        if (maxHeight >= 600.dp) {
            CryptoViewLogo(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 64.dp),
            )
            OnboardingCard(
                apiKey = apiKey,
                showKey = showKey,
                onApiKeyChange = { apiKey = it },
                onShowKeyChange = { showKey = !showKey },
                onContinue = onContinue,
                onCreateKey = { uriHandler.openUri("https://coinmarketcap.com/api/") },
                modifier = Modifier
                    .align(Alignment.Center)
                    .widthIn(max = 680.dp)
                    .fillMaxWidth(),
            )
        } else {
            Column(
                modifier = Modifier
                    .widthIn(max = 680.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(top = 24.dp, bottom = 24.dp)
                    .align(Alignment.TopCenter),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CryptoViewLogo()
                Spacer(Modifier.height(30.dp))
                OnboardingCard(
                    apiKey = apiKey,
                    showKey = showKey,
                    onApiKeyChange = { apiKey = it },
                    onShowKeyChange = { showKey = !showKey },
                    onContinue = onContinue,
                    onCreateKey = { uriHandler.openUri("https://coinmarketcap.com/api/") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun OnboardingCard(
    apiKey: String,
    showKey: Boolean,
    onApiKeyChange: (String) -> Unit,
    onShowKeyChange: () -> Unit,
    onContinue: () -> Unit,
    onCreateKey: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedCard(modifier) {
        Column(Modifier.padding(20.dp)) {
            Text(
                "Olá! Vamos começar",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Conecte sua conta da CoinMarketCap para acessar o mercado.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(20.dp))
            OutlinedTextField(
                value = apiKey,
                onValueChange = onApiKeyChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("API key") },
                singleLine = true,
                visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = onShowKeyChange) {
                        Text(if (showKey) "◉" else "◎", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
            )
            Spacer(Modifier.height(18.dp))
            PrimaryActionButton(
                text = "Validar e salvar",
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth(),
                enabled = apiKey.isNotBlank(),
            )
            Spacer(Modifier.height(18.dp))
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                Text("▣", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    "Sua chave será armazenada com segurança neste dispositivo.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            HorizontalDivider(Modifier.padding(vertical = 16.dp), color = CryptoBorder)
            Text(
                text = "Criar uma API key",
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .clickable(onClick = onCreateKey)
                    .padding(8.dp),
                color = CryptoOrange,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
