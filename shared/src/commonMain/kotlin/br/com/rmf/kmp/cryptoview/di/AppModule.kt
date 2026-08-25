package br.com.rmf.kmp.cryptoview.di

import br.com.rmf.kmp.cryptoview.data.api.CoinMarketCapRequestExecutor
import br.com.rmf.kmp.cryptoview.data.api.CoinMarketCapService
import br.com.rmf.kmp.cryptoview.data.api.createCoinMarketCapService
import br.com.rmf.kmp.cryptoview.domain.repository.CoinMarketCapDataRepository
import br.com.rmf.kmp.cryptoview.domain.usecase.GetKeyInfoUseCase
import br.com.rmf.kmp.cryptoview.security.DataStoreEncryptedApiKeyEnvelopeStore
import br.com.rmf.kmp.cryptoview.security.DefaultSecureApiKeyStorage
import br.com.rmf.kmp.cryptoview.security.EncryptedApiKeyEnvelopeStore
import br.com.rmf.kmp.cryptoview.security.SecureApiKeyStorage
import br.com.rmf.kmp.cryptoview.ui.viewmodel.CoinMarketCapTestViewModel
import de.jensklingenberg.ktorfit.Ktorfit
import de.jensklingenberg.ktorfit.converter.FlowConverterFactory
import de.jensklingenberg.ktorfit.converter.ResponseConverterFactory
import io.ktor.client.HttpClient
import org.koin.dsl.module

internal const val COIN_MARKET_CAP_BASE_URL = "https://pro-api.coinmarketcap.com/"

internal val networkModule = module {
    single {
        Ktorfit.Builder()
            .baseUrl(COIN_MARKET_CAP_BASE_URL)
            .httpClient(get<HttpClient>())
            .converterFactories(
                FlowConverterFactory(),
                ResponseConverterFactory(),
            )
            .build()
    }
    single<CoinMarketCapService> { get<Ktorfit>().createCoinMarketCapService() }
    single { CoinMarketCapRequestExecutor() }
}

internal val repositoryModule = module {
    single {
        CoinMarketCapDataRepository(
            getKeyInfoUseCase = get(),
        )
    }
}

internal val useCaseModule = module {
    factory {
        GetKeyInfoUseCase(
            service = get(),
            requestExecutor = get(),
        )
    }
}

internal val securityModule = module {
    single<EncryptedApiKeyEnvelopeStore> {
        DataStoreEncryptedApiKeyEnvelopeStore(dataStore = get())
    }
    single<SecureApiKeyStorage> {
        DefaultSecureApiKeyStorage(
            cipher = get(),
            envelopeStore = get(),
        )
    }
}

internal val viewModelModule = module {
    factory {
        CoinMarketCapTestViewModel(
            repository = get(),
            secureApiKeyStorage = get(),
        )
    }
}
