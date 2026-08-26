package br.com.rmf.kmp.cryptoview.data.database

import br.com.rmf.kmp.cryptoview.currentTimeMillis
import br.com.rmf.kmp.cryptoview.database.CryptoDatabase
import br.com.rmf.kmp.cryptoview.domain.model.CoinExchangeMarket
import br.com.rmf.kmp.cryptoview.domain.model.CoinHistoryPoint
import br.com.rmf.kmp.cryptoview.domain.model.CoinHistoryRange
import br.com.rmf.kmp.cryptoview.domain.model.CoinInformation
import br.com.rmf.kmp.cryptoview.domain.model.CoinPaprikaMapping
import br.com.rmf.kmp.cryptoview.domain.model.CoinSummary
import br.com.rmf.kmp.cryptoview.domain.model.CoinSortOrder
import br.com.rmf.kmp.cryptoview.domain.model.CoinVariationFilter
import br.com.rmf.kmp.cryptoview.domain.model.ExchangeAsset
import br.com.rmf.kmp.cryptoview.domain.model.ExchangeSummary
import br.com.rmf.kmp.cryptoview.domain.model.SyncPhase
import br.com.rmf.kmp.cryptoview.domain.model.SyncProgress
import br.com.rmf.kmp.cryptoview.domain.model.SyncResumeData
import br.com.rmf.kmp.cryptoview.domain.model.SyncStatus
import br.com.rmf.kmp.cryptoview.domain.model.SyncTrigger
import br.com.rmf.kmp.cryptoview.domain.model.api.CoinHistoryDto
import br.com.rmf.kmp.cryptoview.domain.model.api.CoinListingDto
import br.com.rmf.kmp.cryptoview.domain.model.api.CoinMarketPairsDto
import br.com.rmf.kmp.cryptoview.domain.model.api.CoinMetadataDto
import br.com.rmf.kmp.cryptoview.domain.model.api.ExchangeAssetDto
import br.com.rmf.kmp.cryptoview.domain.model.api.ExchangeListingDto
import br.com.rmf.kmp.cryptoview.domain.model.api.ExchangeMetadataDto
import br.com.rmf.kmp.cryptoview.data.api.doubleValue
import br.com.rmf.kmp.cryptoview.data.api.stringValue
import br.com.rmf.kmp.cryptoview.data.api.usdQuote
import br.com.rmf.kmp.cryptoview.utils.SyncPerformanceTracker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

internal class MarketLocalDataSource(
    private val database: CryptoDatabase,
    private val pool: CryptoDatabasePool,
    private val performanceTracker: SyncPerformanceTracker = SyncPerformanceTracker(),
) {
    fun observeCoins(
        query: String,
        limit: Long,
        sortOrder: CoinSortOrder,
        variation: CoinVariationFilter,
        exchangeId: Long?,
        offset: Long,
    ): Flow<List<CoinSummary>> {
        val normalized = query.trim().lowercase()
        return pool.observeChanges(
            DatabaseChange.COINS,
            DatabaseChange.COIN_QUOTES,
            DatabaseChange.COIN_METADATA,
            DatabaseChange.COIN_MARKETS,
        ).map {
            database.marketQueries.selectCoins(
                query = normalized,
                queryLike = "%$normalized%",
                variation = variation.name,
                sortOrder = sortOrder.name,
                exchangeId = exchangeId ?: -1L,
                limit = limit,
                offset = offset,
                mapper = { id, name, symbol, slug, rank, numMarketPairs, price, volume24h,
                    percentChange24h, marketCap, quoteUpdatedAt, quoteFetchedAt, logoUrl ->
                    CoinSummary(id, name, symbol, slug, rank, numMarketPairs, price, volume24h,
                        percentChange24h, marketCap, quoteUpdatedAt, quoteFetchedAt,
                        resolveCoinLogoUrl(id, logoUrl))
                },
            ).executeAsList()
        }.flowOn(Dispatchers.Default)
    }

    fun observeExchanges(query: String, limit: Long, offset: Long): Flow<List<ExchangeSummary>> {
        val normalized = query.trim().lowercase()
        return pool.observeChanges(
            DatabaseChange.EXCHANGES,
            DatabaseChange.EXCHANGE_METADATA,
        ).map {
            database.marketQueries.selectExchanges(
                query = normalized,
                queryLike = "%$normalized%",
                limit = limit,
                offset = offset,
                mapper = ::mapExchange,
            ).executeAsList()
        }.flowOn(Dispatchers.Default)
    }

    fun observeCachedMarketExchanges(limit: Long): Flow<List<ExchangeSummary>> = pool.observeChanges(
        DatabaseChange.EXCHANGES,
        DatabaseChange.EXCHANGE_METADATA,
        DatabaseChange.COIN_MARKETS,
    ).map {
        database.marketQueries.selectCachedMarketExchanges(limit, mapper = ::mapExchange).executeAsList()
    }.flowOn(Dispatchers.Default)

    fun observeCoin(id: Long): Flow<CoinSummary?> = pool.observeChanges(
        DatabaseChange.COINS,
        DatabaseChange.COIN_QUOTES,
        DatabaseChange.COIN_METADATA,
    ).map {
        database.marketQueries.selectCoinById(
            id,
            mapper = { coinId, name, symbol, slug, rank, numMarketPairs, price, volume24h,
                percentChange24h, marketCap, quoteUpdatedAt, quoteFetchedAt, logoUrl, _, _ ->
                CoinSummary(coinId, name, symbol, slug, rank, numMarketPairs, price, volume24h,
                    percentChange24h, marketCap, quoteUpdatedAt, quoteFetchedAt,
                    resolveCoinLogoUrl(coinId, logoUrl))
            },
        ).executeAsOneOrNull()
    }.flowOn(Dispatchers.Default)

    fun observeExchange(id: Long): Flow<ExchangeSummary?> = pool.observeChanges(
        DatabaseChange.EXCHANGES,
        DatabaseChange.EXCHANGE_METADATA,
    ).map {
        database.marketQueries.selectExchangeById(id, mapper = ::mapExchange).executeAsOneOrNull()
    }.flowOn(Dispatchers.Default)

    fun observeAssets(exchangeId: Long): Flow<List<ExchangeAsset>> = pool.observeChanges(
        DatabaseChange.EXCHANGE_ASSETS,
    ).map {
        database.marketQueries.selectExchangeAssets(exchangeId) { id, currencyId, name, symbol, price, balance, value, _ ->
            ExchangeAsset(id, currencyId, name, symbol, price, balance, value)
        }.executeAsList()
    }.flowOn(Dispatchers.Default)

    fun observeMarkets(coinId: Long): Flow<List<CoinExchangeMarket>> = pool.observeChanges(
        DatabaseChange.COIN_MARKETS,
        DatabaseChange.EXCHANGES,
        DatabaseChange.EXCHANGE_METADATA,
    ).map {
        database.marketQueries.selectCoinMarkets(coinId) { id, exchangeId, exchangeName, logo, pair, category,
                price, volume, updated ->
            CoinExchangeMarket(id, exchangeId, exchangeName, logo, pair, category, price, volume, updated)
        }.executeAsList()
    }.flowOn(Dispatchers.Default)

    fun observeHistory(coinId: Long, range: CoinHistoryRange): Flow<List<CoinHistoryPoint>> = pool.observeChanges(
        DatabaseChange.COIN_HISTORY,
    ).map {
        database.marketQueries.selectCoinHistory(coinId, range.name) { _, _, timestamp, price, _ ->
            CoinHistoryPoint(timestamp, price)
        }.executeAsList()
    }.flowOn(Dispatchers.Default)

    fun observeCoinPaprikaMapping(coinId: Long): Flow<CoinPaprikaMapping?> = pool.observeChanges(
        DatabaseChange.PAPRIKA_MAPPING,
    ).map {
        coinPaprikaMapping(coinId)
    }.flowOn(Dispatchers.Default)

    fun coinPaprikaMapping(coinId: Long): CoinPaprikaMapping? = database.marketQueries
        .selectCoinPaprikaMapping(coinId) { id, paprikaId, resolvedAt ->
            CoinPaprikaMapping(id, paprikaId, resolvedAt)
        }
        .executeAsOneOrNull()

    fun observeCoinInformation(coinId: Long): Flow<CoinInformation?> = pool.observeChanges(
        DatabaseChange.PAPRIKA_INFO,
    ).map {
        coinInformation(coinId)
    }.flowOn(Dispatchers.Default)

    fun coinInformation(coinId: Long): CoinInformation? = database.marketQueries
        .selectCoinPaprikaInfo(coinId, mapper = ::mapCoinInformation)
        .executeAsOneOrNull()

    fun coinDescription(id: Long): Pair<String?, String?> = database.marketQueries
        .selectCoinById(id) { _, _, _, _, _, _, _, _, _, _, _, _, _, description, website ->
            description to website
        }
        .executeAsOneOrNull() ?: (null to null)

    fun countCoins(): Long = database.marketQueries.countCoins().executeAsOne()
    fun countExchanges(): Long = database.marketQueries.countExchanges().executeAsOne()

    fun coinIdsMissingMetadata(minimumFetchedAt: Long, limit: Long, offset: Long): List<Long> =
        database.marketQueries.selectCoinIdsMissingMetadata(minimumFetchedAt, limit, offset).executeAsList()

    fun exchangeIdsMissingMetadata(minimumFetchedAt: Long, limit: Long, offset: Long): List<Long> =
        database.marketQueries.selectExchangeIdsMissingMetadata(minimumFetchedAt, limit, offset).executeAsList()

    suspend fun persistCoinPage(
        runId: String,
        page: Int,
        items: List<CoinListingDto>,
    ) = pool.withConnection { connection ->
        val now = currentTimeMillis()
        val transactionStartedAt = performanceTracker.mark()
        connection.database.transaction {
            items.forEach { dto ->
                connection.upsertCoin(dto, runId)
                val usd = dto.quote.usdQuote()
                connection.upsertCoinQuote(
                    coinId = dto.id,
                    price = usd.doubleValue("price"),
                    volume24h = usd.doubleValue("volume_24h"),
                    percentChange24h = usd.doubleValue("percent_change_24h"),
                    marketCap = usd.doubleValue("market_cap"),
                    remoteUpdatedAt = usd.stringValue("last_updated") ?: dto.lastUpdated,
                    fetchedAt = now,
                )
            }
            connection.database.marketQueries.replaceCheckpoint(
                runId, SyncPhase.COINS.name, page.toLong(), "COMMITTED", items.size.toLong(), now,
            )
        }
        performanceTracker.recordElapsed("database.coins", transactionStartedAt, items.size)
        pool.onBatchCommitted(
            connection.driver,
            DatabaseChange.COINS,
            DatabaseChange.COIN_QUOTES,
        )
    }

    suspend fun persistCoinMetadata(
        runId: String,
        page: Int,
        items: Map<String, CoinMetadataDto>,
    ) = pool.withConnection { connection ->
        val now = currentTimeMillis()
        val transactionStartedAt = performanceTracker.mark()
        connection.database.transaction {
            items.forEach { (key, dto) ->
                val id = dto.id ?: key.toLongOrNull() ?: return@forEach
                connection.database.marketQueries.replaceCoinMetadata(
                    id,
                    dto.logo,
                    dto.description,
                    dto.urls?.website?.firstOrNull(),
                    now,
                )
            }
            connection.database.marketQueries.replaceCheckpoint(
                runId, SyncPhase.COIN_METADATA.name, page.toLong(), "COMMITTED", items.size.toLong(), now,
            )
        }
        performanceTracker.recordElapsed("database.coin_metadata", transactionStartedAt, items.size)
        pool.onBatchCommitted(connection.driver, DatabaseChange.COIN_METADATA)
    }

    suspend fun persistExchangePage(
        runId: String,
        page: Int,
        items: List<ExchangeListingDto>,
    ) = pool.withConnection { connection ->
        val now = currentTimeMillis()
        val transactionStartedAt = performanceTracker.mark()
        connection.database.transaction {
            items.forEach { dto ->
                val usd = dto.quote.usdQuote()
                connection.upsertExchange(
                    dto = dto,
                    runId = runId,
                    spotVolumeUsd = usd.doubleValue("spot_volume_usd")
                        ?: usd.doubleValue("volume_24h"),
                )
            }
            connection.database.marketQueries.replaceCheckpoint(
                runId, SyncPhase.EXCHANGES.name, page.toLong(), "COMMITTED", items.size.toLong(), now,
            )
        }
        performanceTracker.recordElapsed("database.exchanges", transactionStartedAt, items.size)
        pool.onBatchCommitted(connection.driver, DatabaseChange.EXCHANGES)
    }

    suspend fun persistExchangeMetadata(
        runId: String,
        page: Int,
        items: Map<String, ExchangeMetadataDto>,
    ) = pool.withConnection { connection ->
        val now = currentTimeMillis()
        val transactionStartedAt = performanceTracker.mark()
        connection.database.transaction {
            items.forEach { (key, dto) ->
                val id = dto.id ?: key.toLongOrNull() ?: return@forEach
                connection.database.marketQueries.replaceExchangeMetadata(
                    id,
                    dto.logo,
                    dto.description,
                    dto.urls?.website?.firstOrNull(),
                    dto.makerFee,
                    dto.takerFee,
                    dto.dateLaunched,
                    now,
                )
            }
            connection.database.marketQueries.replaceCheckpoint(
                runId, SyncPhase.EXCHANGE_METADATA.name, page.toLong(), "COMMITTED", items.size.toLong(), now,
            )
        }
        performanceTracker.recordElapsed("database.exchange_metadata", transactionStartedAt, items.size)
        pool.onBatchCommitted(connection.driver, DatabaseChange.EXCHANGE_METADATA)
    }

    suspend fun replaceAssets(exchangeId: Long, items: List<ExchangeAssetDto>) =
        pool.withConnection { connection ->
            val now = currentTimeMillis()
            connection.database.transaction {
                connection.database.marketQueries.deleteExchangeAssets(exchangeId)
                items.forEach { dto ->
                    val currency = dto.currency ?: return@forEach
                    val currencyId = currency.id ?: return@forEach
                    val value = dto.balance?.let { balance -> currency.priceUsd?.times(balance) }
                    connection.database.marketQueries.replaceExchangeAsset(
                        exchangeId, currencyId, currency.name.orEmpty(), currency.symbol.orEmpty(),
                        currency.priceUsd, dto.balance, value, now,
                    )
                }
                connection.database.marketQueries.upsertResourceState(
                    "exchange_assets:$exchangeId", now, now, "SUCCESS", null,
                )
            }
            pool.onBatchCommitted(connection.driver, DatabaseChange.EXCHANGE_ASSETS)
        }

    suspend fun replaceMarkets(coinId: Long, data: CoinMarketPairsDto) =
        pool.withConnection { connection ->
            val now = currentTimeMillis()
            connection.database.transaction {
                connection.database.marketQueries.deleteCoinMarkets(coinId)
                data.marketPairs.orEmpty().forEach { dto ->
                    val exchange = dto.exchange ?: return@forEach
                    val exchangeId = exchange.id ?: return@forEach
                    connection.database.marketQueries.insertExchangeIfMissing(
                        exchangeId,
                        exchange.name.orEmpty().ifBlank { "#$exchangeId" },
                        exchange.name.orEmpty().lowercase(),
                        exchange.slug.orEmpty(),
                        null, null, null, null, null,
                        "on-demand:$coinId",
                    )
                    val usd = dto.quote.usdQuote()
                    connection.database.marketQueries.replaceCoinExchangeMarket(
                        coinId, exchangeId, dto.marketPair.orEmpty(), dto.category,
                        usd.doubleValue("price"), usd.doubleValue("volume_24h"),
                        usd.stringValue("last_updated"), now,
                    )
                }
                connection.database.marketQueries.upsertResourceState(
                    "coin_markets:$coinId", now, now, "SUCCESS", null,
                )
            }
            pool.onBatchCommitted(
                connection.driver,
                DatabaseChange.COIN_MARKETS,
                DatabaseChange.EXCHANGES,
            )
        }

    suspend fun replaceHistory(coinId: Long, range: CoinHistoryRange, data: CoinHistoryDto) =
        pool.withConnection { connection ->
            val now = currentTimeMillis()
            connection.database.transaction {
                connection.database.marketQueries.deleteCoinHistory(coinId, range.name)
                data.quotes.orEmpty().forEach { dto ->
                    val timestamp = dto.timestamp ?: return@forEach
                    val price = dto.quote.usdQuote().doubleValue("price") ?: return@forEach
                    connection.database.marketQueries.replaceHistoryPoint(coinId, range.name, timestamp, price, now)
                }
                connection.database.marketQueries.upsertResourceState(
                    historyResourceKey(coinId, range), now, now, "SUCCESS", null,
                )
            }
            pool.onBatchCommitted(connection.driver, DatabaseChange.COIN_HISTORY)
        }

    fun historyResourceKey(coinId: Long, range: CoinHistoryRange): String =
        "coin_history:$coinId:${range.name}"

    suspend fun replaceQuote(coinId: Long, dto: CoinListingDto) = pool.withConnection { connection ->
        val usd = dto.quote.usdQuote()
        connection.upsertCoinQuote(
            coinId,
            usd.doubleValue("price"),
            usd.doubleValue("volume_24h"),
            usd.doubleValue("percent_change_24h"),
            usd.doubleValue("market_cap"),
            usd.stringValue("last_updated") ?: dto.lastUpdated,
            currentTimeMillis(),
        )
        pool.onBatchCommitted(connection.driver, DatabaseChange.COIN_QUOTES)
    }

    suspend fun replaceCoinPaprikaMapping(coinId: Long, paprikaId: String) =
        pool.withConnection { connection ->
            connection.database.marketQueries.replaceCoinPaprikaMapping(
                coinId = coinId,
                paprikaId = paprikaId,
                resolvedAt = currentTimeMillis(),
            )
            pool.onBatchCommitted(connection.driver, DatabaseChange.PAPRIKA_MAPPING)
        }

    suspend fun replaceCoinInformation(information: CoinInformation) =
        pool.withConnection { connection ->
            connection.database.marketQueries.replaceCoinPaprikaInfo(
                coinId = information.coinId,
                paprikaId = information.paprikaId,
                name = information.name,
                symbol = information.symbol,
                rank = information.rank,
                isActive = information.isActive.toLong(),
                type = information.type,
                logoUrl = information.logoUrl,
                description = information.description,
                message = information.message,
                openSource = information.openSource?.toLong(),
                hardwareWallet = information.hardwareWallet?.toLong(),
                startedAt = information.startedAt,
                developmentStatus = information.developmentStatus,
                proofType = information.proofType,
                orgStructure = information.orgStructure,
                hashAlgorithm = information.hashAlgorithm,
                websiteUrl = information.websiteUrl,
                explorerUrl = information.explorerUrl,
                sourceCodeUrl = information.sourceCodeUrl,
                whitepaperUrl = information.whitepaperUrl,
                fetchedAt = information.fetchedAt,
            )
            pool.onBatchCommitted(connection.driver, DatabaseChange.PAPRIKA_INFO)
        }

    suspend fun invalidateCoinPaprikaMapping(coinId: Long) = pool.withConnection { connection ->
        connection.database.transaction {
            connection.database.marketQueries.deleteCoinPaprikaInfo(coinId)
            connection.database.marketQueries.deleteCoinPaprikaMapping(coinId)
        }
        pool.onBatchCommitted(
            connection.driver,
            DatabaseChange.PAPRIKA_MAPPING,
            DatabaseChange.PAPRIKA_INFO,
        )
    }

    suspend fun markCoinsComplete(runId: String) = pool.withConnection { connection ->
        connection.database.marketQueries.markCoinsInactiveOutsideRun(runId)
        pool.onBatchCommitted(connection.driver, DatabaseChange.COINS)
    }

    suspend fun markExchangesComplete(runId: String) = pool.withConnection { connection ->
        connection.database.marketQueries.markExchangesInactiveOutsideRun(runId)
        pool.onBatchCommitted(connection.driver, DatabaseChange.EXCHANGES)
    }

    fun committedPages(runId: String, phase: SyncPhase): Set<Int> = database.marketQueries
        .selectCommittedPages(runId, phase.name)
        .executeAsList()
        .map(Long::toInt)
        .toSet()

    fun resourceLastSuccess(key: String): Long? = database.marketQueries
        .selectResourceState(key)
        .executeAsOneOrNull()
        ?.lastSuccessAt

    suspend fun markResourceFailure(key: String, errorCode: String) = pool.withConnection { connection ->
        val previous = connection.database.marketQueries.selectResourceState(key).executeAsOneOrNull()
        connection.database.marketQueries.upsertResourceState(
            key,
            previous?.lastSuccessAt,
            currentTimeMillis(),
            "FAILED",
            errorCode,
        )
    }

    suspend fun markResourceSuccess(key: String) = pool.withConnection { connection ->
        val now = currentTimeMillis()
        connection.database.marketQueries.upsertResourceState(key, now, now, "SUCCESS", null)
    }

    fun latestIncompleteRun(): SyncResumeData? = database.marketQueries
        .selectLatestIncompleteRun { runId, trigger, phase, _, _, persistedItems,
                requestedPages, committedPages, failedPages, _, _, _, _ ->
            SyncResumeData(
                runId = runId,
                trigger = enumValueOfOrNull<SyncTrigger>(trigger) ?: SyncTrigger.RESUME,
                phase = enumValueOfOrNull<SyncPhase>(phase) ?: SyncPhase.PREPARING,
                persistedItems = persistedItems,
                requestedPages = requestedPages.toInt(),
                committedPages = committedPages.toInt(),
                failedPages = failedPages.toInt(),
            )
        }
        .executeAsOneOrNull()

    suspend fun createRun(progress: SyncProgress) = pool.withConnection { connection ->
        val now = currentTimeMillis()
        connection.database.marketQueries.insertSyncRun(
            runId = requireNotNull(progress.runId),
            trigger = requireNotNull(progress.trigger).name,
            phase = progress.phase.name,
            status = progress.status.name,
            plannedItems = progress.plannedItems,
            persistedItems = progress.persistedItems,
            requestedPages = progress.requestedPages.toLong(),
            committedPages = progress.committedPages.toLong(),
            failedPages = progress.failedPages.toLong(),
            startedAt = now,
            updatedAt = now,
            completedAt = null,
            errorCode = null,
        )
    }

    suspend fun updateRun(progress: SyncProgress) = pool.withConnection { connection ->
        val now = currentTimeMillis()
        connection.database.marketQueries.updateSyncRunProgress(
            phase = progress.phase.name,
            status = progress.status.name,
            plannedItems = progress.plannedItems,
            persistedItems = progress.persistedItems,
            requestedPages = progress.requestedPages.toLong(),
            committedPages = progress.committedPages.toLong(),
            failedPages = progress.failedPages.toLong(),
            updatedAt = now,
            completedAt = if (
                progress.status == SyncStatus.COMPLETED || progress.status == SyncStatus.PARTIAL
            ) now else null,
            errorCode = progress.error?.let { it::class.simpleName },
            runId = requireNotNull(progress.runId),
        )
    }

    suspend fun clear() = pool.withConnection { connection ->
        connection.database.transaction {
            connection.database.marketQueries.clearHistory()
            connection.database.marketQueries.clearCoinPaprikaInfo()
            connection.database.marketQueries.clearCoinPaprikaMappings()
            connection.database.marketQueries.clearMarkets()
            connection.database.marketQueries.clearAssets()
            connection.database.marketQueries.clearCoinMetadata()
            connection.database.marketQueries.clearCoinQuotes()
            connection.database.marketQueries.clearExchangeMetadata()
            connection.database.marketQueries.clearCoins()
            connection.database.marketQueries.clearExchanges()
            connection.database.marketQueries.clearCheckpoints()
            connection.database.marketQueries.clearRuns()
            connection.database.marketQueries.clearResourceStates()
        }
        pool.onBatchCommitted(connection.driver, DatabaseChange.ALL)
    }

    private fun mapExchange(
        id: Long,
        name: String,
        slug: String,
        rank: Long?,
        numMarketPairs: Long?,
        spotVolumeUsd: Double?,
        resolvedDateLaunched: String?,
        logoUrl: String?,
        description: String?,
        websiteUrl: String?,
        makerFee: Double?,
        takerFee: Double?,
    ) = ExchangeSummary(
        id, name, slug, rank, numMarketPairs, spotVolumeUsd, resolvedDateLaunched,
        logoUrl, description, websiteUrl, makerFee, takerFee,
    )

    private fun mapCoinInformation(
        coinId: Long,
        paprikaId: String,
        name: String,
        symbol: String,
        rank: Long?,
        isActive: Long,
        type: String?,
        logoUrl: String?,
        description: String?,
        message: String?,
        openSource: Long?,
        hardwareWallet: Long?,
        startedAt: String?,
        developmentStatus: String?,
        proofType: String?,
        orgStructure: String?,
        hashAlgorithm: String?,
        websiteUrl: String?,
        explorerUrl: String?,
        sourceCodeUrl: String?,
        whitepaperUrl: String?,
        fetchedAt: Long,
    ) = CoinInformation(
        coinId = coinId,
        paprikaId = paprikaId,
        name = name,
        symbol = symbol,
        rank = rank,
        isActive = isActive != 0L,
        type = type,
        logoUrl = logoUrl,
        description = description,
        message = message,
        openSource = openSource?.let { it != 0L },
        hardwareWallet = hardwareWallet?.let { it != 0L },
        startedAt = startedAt,
        developmentStatus = developmentStatus,
        proofType = proofType,
        orgStructure = orgStructure,
        hashAlgorithm = hashAlgorithm,
        websiteUrl = websiteUrl,
        explorerUrl = explorerUrl,
        sourceCodeUrl = sourceCodeUrl,
        whitepaperUrl = whitepaperUrl,
        fetchedAt = fetchedAt,
    )
}

private fun Boolean.toLong(): Long = if (this) 1L else 0L

private inline fun <reified T : Enum<T>> enumValueOfOrNull(value: String): T? =
    enumValues<T>().firstOrNull { it.name == value }

internal fun coinMarketCapLogoUrl(coinId: Long): String =
    "https://s2.coinmarketcap.com/static/img/coins/64x64/$coinId.png"

internal fun resolveCoinLogoUrl(coinId: Long, cachedLogoUrl: String?): String =
    cachedLogoUrl?.takeIf(String::isNotBlank) ?: coinMarketCapLogoUrl(coinId)

private fun CryptoDatabaseConnection.upsertCoin(
    dto: CoinListingDto,
    runId: String,
) {
    val name = dto.name.orEmpty().ifBlank { "#${dto.id}" }
    val normalizedName = dto.name.orEmpty().lowercase()
    if (supportsNativeUpsert) {
        driver.execute(null, NATIVE_COIN_UPSERT, 13) {
            bindLong(0, dto.id)
            bindString(1, name)
            bindString(2, normalizedName)
            bindString(3, dto.symbol.orEmpty())
            bindString(4, dto.slug.orEmpty())
            bindLong(5, dto.rank)
            bindLong(6, dto.numMarketPairs)
            bindDouble(7, dto.circulatingSupply)
            bindDouble(8, dto.totalSupply)
            bindDouble(9, dto.maxSupply)
            bindString(10, dto.dateAdded)
            bindString(11, dto.lastUpdated)
            bindString(12, runId)
        }.value
        return
    }

    database.marketQueries.insertCoinIfMissing(
        id = dto.id,
        name = name,
        normalizedName = normalizedName,
        symbol = dto.symbol.orEmpty(),
        slug = dto.slug.orEmpty(),
        rank = dto.rank,
        numMarketPairs = dto.numMarketPairs,
        circulatingSupply = dto.circulatingSupply,
        totalSupply = dto.totalSupply,
        maxSupply = dto.maxSupply,
        dateAdded = dto.dateAdded,
        remoteUpdatedAt = dto.lastUpdated,
        lastSeenRunId = runId,
    )
    database.marketQueries.updateCoin(
        name = name,
        normalizedName = normalizedName,
        symbol = dto.symbol.orEmpty(),
        slug = dto.slug.orEmpty(),
        rank = dto.rank,
        numMarketPairs = dto.numMarketPairs,
        circulatingSupply = dto.circulatingSupply,
        totalSupply = dto.totalSupply,
        maxSupply = dto.maxSupply,
        dateAdded = dto.dateAdded,
        remoteUpdatedAt = dto.lastUpdated,
        lastSeenRunId = runId,
        id = dto.id,
    )
}

private fun CryptoDatabaseConnection.upsertCoinQuote(
    coinId: Long,
    price: Double?,
    volume24h: Double?,
    percentChange24h: Double?,
    marketCap: Double?,
    remoteUpdatedAt: String?,
    fetchedAt: Long,
) {
    if (supportsNativeUpsert) {
        driver.execute(null, NATIVE_COIN_QUOTE_UPSERT, 7) {
            bindLong(0, coinId)
            bindDouble(1, price)
            bindDouble(2, volume24h)
            bindDouble(3, percentChange24h)
            bindDouble(4, marketCap)
            bindString(5, remoteUpdatedAt)
            bindLong(6, fetchedAt)
        }.value
    } else {
        database.marketQueries.replaceCoinQuote(
            coinId,
            price,
            volume24h,
            percentChange24h,
            marketCap,
            remoteUpdatedAt,
            fetchedAt,
        )
    }
}

private fun CryptoDatabaseConnection.upsertExchange(
    dto: ExchangeListingDto,
    runId: String,
    spotVolumeUsd: Double?,
) {
    val name = dto.name.orEmpty().ifBlank { "#${dto.id}" }
    val normalizedName = dto.name.orEmpty().lowercase()
    if (supportsNativeUpsert) {
        driver.execute(null, NATIVE_EXCHANGE_UPSERT, 10) {
            bindLong(0, dto.id)
            bindString(1, name)
            bindString(2, normalizedName)
            bindString(3, dto.slug.orEmpty())
            bindLong(4, dto.rank)
            bindLong(5, dto.numMarketPairs)
            bindDouble(6, spotVolumeUsd)
            bindString(7, dto.dateLaunched)
            bindString(8, dto.lastUpdated)
            bindString(9, runId)
        }.value
        return
    }

    database.marketQueries.insertExchangeIfMissing(
        id = dto.id,
        name = name,
        normalizedName = normalizedName,
        slug = dto.slug.orEmpty(),
        rank = dto.rank,
        numMarketPairs = dto.numMarketPairs,
        spotVolumeUsd = spotVolumeUsd,
        dateLaunched = dto.dateLaunched,
        remoteUpdatedAt = dto.lastUpdated,
        lastSeenRunId = runId,
    )
    database.marketQueries.updateExchange(
        name = name,
        normalizedName = normalizedName,
        slug = dto.slug.orEmpty(),
        rank = dto.rank,
        numMarketPairs = dto.numMarketPairs,
        spotVolumeUsd = spotVolumeUsd,
        dateLaunched = dto.dateLaunched,
        remoteUpdatedAt = dto.lastUpdated,
        lastSeenRunId = runId,
        id = dto.id,
    )
}

private const val NATIVE_COIN_UPSERT = """
    INSERT INTO coin(
        id, name, normalizedName, symbol, slug, rank, numMarketPairs,
        circulatingSupply, totalSupply, maxSupply, dateAdded, remoteUpdatedAt,
        lastSeenRunId, active
    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1)
    ON CONFLICT(id) DO UPDATE SET
        name = excluded.name,
        normalizedName = excluded.normalizedName,
        symbol = excluded.symbol,
        slug = excluded.slug,
        rank = excluded.rank,
        numMarketPairs = excluded.numMarketPairs,
        circulatingSupply = excluded.circulatingSupply,
        totalSupply = excluded.totalSupply,
        maxSupply = excluded.maxSupply,
        dateAdded = excluded.dateAdded,
        remoteUpdatedAt = excluded.remoteUpdatedAt,
        lastSeenRunId = excluded.lastSeenRunId,
        active = 1
"""

private const val NATIVE_COIN_QUOTE_UPSERT = """
    INSERT INTO coin_quote(
        coinId, price, volume24h, percentChange24h, marketCap, remoteUpdatedAt, fetchedAt
    ) VALUES (?, ?, ?, ?, ?, ?, ?)
    ON CONFLICT(coinId) DO UPDATE SET
        price = excluded.price,
        volume24h = excluded.volume24h,
        percentChange24h = excluded.percentChange24h,
        marketCap = excluded.marketCap,
        remoteUpdatedAt = excluded.remoteUpdatedAt,
        fetchedAt = excluded.fetchedAt
"""

private const val NATIVE_EXCHANGE_UPSERT = """
    INSERT INTO market_exchange(
        id, name, normalizedName, slug, rank, numMarketPairs, spotVolumeUsd,
        dateLaunched, remoteUpdatedAt, lastSeenRunId, active
    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1)
    ON CONFLICT(id) DO UPDATE SET
        name = excluded.name,
        normalizedName = excluded.normalizedName,
        slug = excluded.slug,
        rank = excluded.rank,
        numMarketPairs = excluded.numMarketPairs,
        spotVolumeUsd = excluded.spotVolumeUsd,
        dateLaunched = excluded.dateLaunched,
        remoteUpdatedAt = excluded.remoteUpdatedAt,
        lastSeenRunId = excluded.lastSeenRunId,
        active = 1
"""
