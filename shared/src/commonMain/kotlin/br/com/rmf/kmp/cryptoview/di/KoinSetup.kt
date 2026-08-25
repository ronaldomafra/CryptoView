package br.com.rmf.kmp.cryptoview.di

import br.com.rmf.kmp.cryptoview.platformModule
import org.koin.core.context.startKoin
import org.koin.mp.KoinPlatformTools

fun initKoin() {
    if (KoinPlatformTools.defaultContext().getOrNull() != null) return

    startKoin {
        modules(
            platformModule,
            networkModule,
            repositoryModule,
            useCaseModule,
            viewModelModule,
        )
    }
}
