package br.com.rmf.kmp.cryptoview.di

import br.com.rmf.kmp.cryptoview.domain.repository.CoinMarketCapDataRepository
import br.com.rmf.kmp.cryptoview.domain.usecase.GetKeyInfoUseCase
import br.com.rmf.kmp.cryptoview.ui.viewmodel.CoinMarketCapTestViewModel
import br.com.rmf.kmp.cryptoview.utils.CryptoProcessConfig
import br.com.rmf.kmp.cryptoview.security.SecureApiKeyResult
import br.com.rmf.kmp.cryptoview.security.SecureApiKeyError
import br.com.rmf.kmp.cryptoview.security.SecureApiKeyStatus
import br.com.rmf.kmp.cryptoview.security.SecureApiKeyStorage
import io.ktor.client.HttpClient
import org.koin.core.context.stopKoin
import org.koin.mp.KoinPlatformTools
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertSame

class KoinGraphAndroidHostTest {
    @Test
    fun compositionRootProvidesRepositorySingletonAndUseCaseFactory() {
        stopKoin()
        try {
            val testSecurityModule = module {
                single<SecureApiKeyStorage> { FakeSecureApiKeyStorage }
            }
            initKoin(listOf(testSecurityModule))
            initKoin(listOf(testSecurityModule))

            val koin = KoinPlatformTools.defaultContext().get()
            assertSame(koin.get<HttpClient>(), koin.get<HttpClient>())
            assertSame(
                koin.get<CoinMarketCapDataRepository>(),
                koin.get<CoinMarketCapDataRepository>(),
            )
            assertNotSame(koin.get<GetKeyInfoUseCase>(), koin.get<GetKeyInfoUseCase>())
            assertNotSame(
                koin.get<CoinMarketCapTestViewModel>(),
                koin.get<CoinMarketCapTestViewModel>(),
            )

            val config = koin.get<CryptoProcessConfig>()
            assertEquals(2, config.databasePoolSize)
            assertEquals(2, config.databaseWriteParallelism)
            assertEquals(10, config.networkParallelism)
        } finally {
            stopKoin()
        }
    }

    private data object FakeSecureApiKeyStorage : SecureApiKeyStorage {
        override suspend fun save(apiKey: String) = SecureApiKeyResult.Success(Unit)
        override suspend fun read() = SecureApiKeyResult.Failure(SecureApiKeyError.NOT_CONFIGURED)
        override suspend fun status() = SecureApiKeyStatus.NOT_CONFIGURED
        override suspend fun remove() = SecureApiKeyResult.Success(Unit)
    }
}
