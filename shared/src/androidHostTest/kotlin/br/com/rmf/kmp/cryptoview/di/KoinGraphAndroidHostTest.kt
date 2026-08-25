package br.com.rmf.kmp.cryptoview.di

import br.com.rmf.kmp.cryptoview.domain.repository.CoinMarketCapDataRepository
import br.com.rmf.kmp.cryptoview.domain.usecase.GetKeyInfoUseCase
import br.com.rmf.kmp.cryptoview.ui.viewmodel.CoinMarketCapTestViewModel
import br.com.rmf.kmp.cryptoview.utils.CryptoProcessConfig
import io.ktor.client.HttpClient
import org.koin.core.context.stopKoin
import org.koin.mp.KoinPlatformTools
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertSame

class KoinGraphAndroidHostTest {
    @Test
    fun compositionRootProvidesRepositorySingletonAndUseCaseFactory() {
        stopKoin()
        try {
            initKoin()
            initKoin()

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
}
