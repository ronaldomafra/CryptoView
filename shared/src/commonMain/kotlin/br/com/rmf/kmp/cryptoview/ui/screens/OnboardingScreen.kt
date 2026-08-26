package br.com.rmf.kmp.cryptoview.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import br.com.rmf.kmp.cryptoview.ui.components.CryptoIcon
import br.com.rmf.kmp.cryptoview.ui.components.OutlinedCard
import br.com.rmf.kmp.cryptoview.ui.components.PrimaryActionButton
import br.com.rmf.kmp.cryptoview.ui.theme.CryptoBorder
import br.com.rmf.kmp.cryptoview.ui.theme.CryptoOrange
import br.com.rmf.kmp.cryptoview.ui.theme.CryptoNegative
import br.com.rmf.kmp.cryptoview.ui.model.AppUiState

@Composable
fun OnboardingScreen(
    state: AppUiState.NeedsApiKey,
    onContinue: (String) -> Unit,
    onCancel: (() -> Unit)? = null,
) {
    var apiKey by rememberSaveable { mutableStateOf("") }
    var showKey by rememberSaveable { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CryptoViewLogo()
        Spacer(Modifier.height(30.dp))
        OnboardingCard(
            apiKey = apiKey,
            showKey = showKey,
            onApiKeyChange = { apiKey = it },
            onShowKeyChange = { showKey = !showKey },
            state = state,
            onContinue = { onContinue(apiKey) },
            onCancel = onCancel,
            onCreateKey = { uriHandler.openUri("https://coinmarketcap.com/api/") },
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxWidth(),
        )
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
    state: AppUiState.NeedsApiKey,
    onCancel: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    OutlinedCard(modifier = modifier, elevation = 5.dp) {
        Column(Modifier.padding(horizontal = 26.dp, vertical = 28.dp)) {
            Text(
                "Olá! Vamos começar",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "Conecte sua conta da CoinMarketCap para acessar o mercado.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))
            OutlinedTextField(
                value = apiKey,
                onValueChange = onApiKeyChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("API key") },
                singleLine = true,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = onShowKeyChange) {
                        CryptoIcon(
                            if (showKey) CryptoIcon.EyeOff else CryptoIcon.Eye,
                            if (showKey) "Ocultar chave" else "Mostrar chave",
                            Modifier.size(22.dp),
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
            )
            Spacer(Modifier.height(20.dp))
            PrimaryActionButton(
                text = if (state.submitting) "Validando..." else "Validar e salvar",
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth(),
                enabled = apiKey.isNotBlank() && !state.submitting,
                loading = state.submitting,
            )
            state.errorMessage?.let { message ->
                Spacer(Modifier.height(12.dp))
                Text(message, color = CryptoNegative, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(20.dp))
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                CryptoIcon(
                    CryptoIcon.Lock,
                    "Armazenamento seguro",
                    Modifier.size(20.dp),
                    MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "Sua chave será armazenada com segurança neste dispositivo.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            HorizontalDivider(Modifier.padding(vertical = 20.dp), color = CryptoBorder)
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
            if (state.replacing && onCancel != null) {
                TextButton(
                    onClick = onCancel,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                ) { Text("Cancelar substituição") }
            }
        }
    }
}
