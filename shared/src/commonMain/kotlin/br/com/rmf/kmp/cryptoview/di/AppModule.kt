package br.com.rmf.kmp.cryptoview.di

import br.com.rmf.kmp.cryptoview.data.api.CoinMarketCapRequestExecutor
import br.com.rmf.kmp.cryptoview.data.api.CoinMarketCapService
import br.com.rmf.kmp.cryptoview.data.api.ApiRateLimiter
import br.com.rmf.kmp.cryptoview.data.api.AuthenticatedRequestExecutor
import br.com.rmf.kmp.cryptoview.data.api.CoinMarketCapRemoteDataSource
import br.com.rmf.kmp.cryptoview.data.api.CoinPaprikaRemoteDataSource
import br.com.rmf.kmp.cryptoview.data.api.CoinPaprikaRequestExecutor
import br.com.rmf.kmp.cryptoview.data.api.CoinPaprikaService
import br.com.rmf.kmp.cryptoview.data.api.createCoinMarketCapService
import br.com.rmf.kmp.cryptoview.data.api.createCoinPaprikaService
import br.com.rmf.kmp.cryptoview.data.database.CryptoDatabaseDriverFactory
import br.com.rmf.kmp.cryptoview.data.database.CryptoDatabasePool
import br.com.rmf.kmp.cryptoview.data.database.MarketLocalDataSource
import br.com.rmf.kmp.cryptoview.data.database.createConfiguredDriver
import br.com.rmf.kmp.cryptoview.database.CryptoDatabase
import br.com.rmf.kmp.cryptoview.domain.repository.MarketRepository
import br.com.rmf.kmp.cryptoview.domain.repository.CoinInformationRepository
import br.com.rmf.kmp.cryptoview.domain.sync.CryptoSyncCoordinator
import br.com.rmf.kmp.cryptoview.domain.sync.CryptoSyncManager
import br.com.rmf.kmp.cryptoview.domain.sync.DefaultCryptoSyncCoordinator
import br.com.rmf.kmp.cryptoview.domain.sync.DefaultCryptoSyncManager
import br.com.rmf.kmp.cryptoview.domain.repository.CoinMarketCapDataRepository
import br.com.rmf.kmp.cryptoview.domain.usecase.GetKeyInfoUseCase
import br.com.rmf.kmp.cryptoview.security.DataStoreEncryptedApiKeyEnvelopeStore
import br.com.rmf.kmp.cryptoview.security.DefaultSecureApiKeyStorage
import br.com.rmf.kmp.cryptoview.security.EncryptedApiKeyEnvelopeStore
import br.com.rmf.kmp.cryptoview.security.SecureApiKeyStorage
import br.com.rmf.kmp.cryptoview.ui.viewmodel.AppViewModel
import br.com.rmf.kmp.cryptoview.ui.viewmodel.ExchangeDetailViewModel
import br.com.rmf.kmp.cryptoview.ui.viewmodel.MarketViewModel
import br.com.rmf.kmp.cryptoview.ui.viewmodel.CoinMarketsViewModel
import br.com.rmf.kmp.cryptoview.ui.viewmodel.CoinInformationViewModel
import br.com.rmf.kmp.cryptoview.ui.viewmodel.SettingsViewModel
import de.jensklingenberg.ktorfit.Ktorfit
import de.jensklingenberg.ktorfit.converter.FlowConverterFactory
import de.jensklingenberg.ktorfit.converter.ResponseConverterFactory
import io.ktor.client.HttpClient
import org.koin.dsl.module
import org.koin.core.qualifier.named
import app.cash.sqldelight.db.SqlDriver
import org.koin.core.module.dsl.onClose
import org.koin.core.module.dsl.withOptions

internal const val COIN_MARKET_CAP_BASE_URL = "https://pro-api.coinmarketcap.com/"
internal const val COIN_PAPRIKA_BASE_URL = "https://api.coinpaprika.com/v1/"
private const val COIN_PAPRIKA_KTORFIT = "coinPaprikaKtorfit"

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
    single { AuthenticatedRequestExecutor(get()) }
    single { ApiRateLimiter() }
    single {
        CoinMarketCapRemoteDataSource(
            service = get(),
            requestExecutor = get(),
            authenticatedExecutor = get(),
            rateLimiter = get(),
        )
    }
    single(named(COIN_PAPRIKA_KTORFIT)) {
        Ktorfit.Builder()
            .baseUrl(COIN_PAPRIKA_BASE_URL)
            .httpClient(get<HttpClient>())
            .converterFactories(
                FlowConverterFactory(),
                ResponseConverterFactory(),
            )
            .build()
    }
    single<CoinPaprikaService> {
        get<Ktorfit>(named(COIN_PAPRIKA_KTORFIT)).createCoinPaprikaService()
    }
    single { CoinPaprikaRequestExecutor() }
    single {
        CoinPaprikaRemoteDataSource(
            service = get(),
            requestExecutor = get(),
        )
    }
}

internal val databaseModule = module {
    single<SqlDriver> {
        get<CryptoDatabaseDriverFactory>().createConfiguredDriver()
    } withOptions {
        onClose { driver -> driver?.close() }
    }
    single { CryptoDatabase(get()) }
    single { CryptoDatabasePool(get(), get()) }
    single { MarketLocalDataSource(get(), get()) }
}

internal val repositoryModule = module {
    single {
        CoinMarketCapDataRepository(
            getKeyInfoUseCase = get(),
        )
    }
    single { MarketRepository(local = get(), remote = get()) }
    single { CoinInformationRepository(local = get(), remote = get()) }
}

internal val syncModule = module {
    single<CryptoSyncCoordinator> {
        DefaultCryptoSyncCoordinator(
            secureApiKeyStorage = get(),
            remote = get(),
            local = get(),
            config = get(),
        )
    }
    single<CryptoSyncManager> { DefaultCryptoSyncManager(coordinator = get()) }
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
    factory { AppViewModel(get(), get(), get(), get()) }
    factory { MarketViewModel(get(), get(), get()) }
    factory { ExchangeDetailViewModel(get()) }
    factory { CoinMarketsViewModel(get()) }
    factory { CoinInformationViewModel(get(), get()) }
    factory { SettingsViewModel(get(), get()) }
}
