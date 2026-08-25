package br.com.rmf.kmp.cryptoview.di

import br.com.rmf.kmp.cryptoview.platformModule
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.mp.KoinPlatformTools

internal fun initKoin(additionalModules: List<Module>) {
    if (KoinPlatformTools.defaultContext().getOrNull() != null) return

    startKoin {
        modules(
            listOf(
                platformModule,
                securityModule,
                networkModule,
                repositoryModule,
                useCaseModule,
                viewModelModule,
            ) + additionalModules,
        )
    }
}
