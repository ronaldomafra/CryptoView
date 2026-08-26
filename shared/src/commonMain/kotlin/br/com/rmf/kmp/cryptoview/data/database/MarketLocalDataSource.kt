package br.com.rmf.kmp.cryptoview.data.database

import br.com.rmf.kmp.cryptoview.currentTimeMillis
import br.com.rmf.kmp.cryptoview.database.CryptoDatabase
import br.com.rmf.kmp.cryptoview.domain.model.CoinExchangeMarket
import br.com.rmf.kmp.cryptoview.domain.model.CoinHistoryPoint
import br.com.rmf.kmp.cryptoview.domain.model.CoinHistoryRange
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

internal class MarketLocalDataSource(
    private val database: CryptoDatabase,
    private val pool: CryptoDatabasePool,
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
        return pool.changeVersion.map {
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
                        percentChange24h, marketCap, quoteUpdatedAt, quoteFetchedAt, logoUrl)
                },
            ).executeAsList()
        }.flowOn(Dispatchers.Default)
    }

    fun observeExchanges(query: String, limit: Long, offset: Long): Flow<List<ExchangeSummary>> {
        val normalized = query.trim().lowercase()
        return pool.changeVersion.map {
            database.marketQueries.selectExchanges(
                query = normalized,
                queryLike = "%$normalized%",
                limit = limit,
                offset = offset,
                mapper = ::mapExchange,
            ).executeAsList()
        }.flowOn(Dispatchers.Default)
    }

    fun observeCachedMarketExchanges(limit: Long): Flow<List<ExchangeSummary>> = pool.changeVersion.map {
        database.marketQueries.selectCachedMarketExchanges(limit, mapper = ::mapExchange).executeAsList()
    }.flowOn(Dispatchers.Default)

    fun observeCoin(id: Long): Flow<CoinSummary?> = pool.changeVersion.map {
        database.marketQueries.selectCoinById(
            id,
            mapper = { coinId, name, symbol, slug, rank, numMarketPairs, price, volume24h,
                percentChange24h, marketCap, quoteUpdatedAt, quoteFetchedAt, logoUrl, _, _ ->
                CoinSummary(coinId, name, symbol, slug, rank, numMarketPairs, price, volume24h,
                    percentChange24h, marketCap, quoteUpdatedAt, quoteFetchedAt, logoUrl)
            },
        ).executeAsOneOrNull()
    }.flowOn(Dispatchers.Default)

    fun observeExchange(id: Long): Flow<ExchangeSummary?> = pool.changeVersion.map {
        database.marketQueries.selectExchangeById(id, mapper = ::mapExchange).executeAsOneOrNull()
    }.flowOn(Dispatchers.Default)

    fun observeAssets(exchangeId: Long): Flow<List<ExchangeAsset>> = pool.changeVersion.map {
        database.marketQueries.selectExchangeAssets(exchangeId) { id, currencyId, name, symbol, price, balance, value, _ ->
            ExchangeAsset(id, currencyId, name, symbol, price, balance, value)
        }.executeAsList()
    }.flowOn(Dispatchers.Default)

    fun observeMarkets(coinId: Long): Flow<List<CoinExchangeMarket>> = pool.changeVersion.map {
        database.marketQueries.selectCoinMarkets(coinId) { id, exchangeId, exchangeName, logo, pair, category,
                price, volume, updated ->
            CoinExchangeMarket(id, exchangeId, exchangeName, logo, pair, category, price, volume, updated)
        }.executeAsList()
    }.flowOn(Dispatchers.Default)

    fun observeHistory(coinId: Long, range: CoinHistoryRange): Flow<List<CoinHistoryPoint>> = pool.changeVersion.map {
        database.marketQueries.selectCoinHistory(coinId, range.name) { _, _, timestamp, price, _ ->
            CoinHistoryPoint(timestamp, price)
        }.executeAsList()
    }.flowOn(Dispatchers.Default)

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
        connection.database.transaction {
            items.forEach { dto ->
                connection.database.marketQueries.insertCoinIfMissing(
                    id = dto.id,
                    name = dto.name.orEmpty().ifBlank { "#${dto.id}" },
                    normalizedName = dto.name.orEmpty().lowercase(),
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
                connection.database.marketQueries.updateCoin(
                    name = dto.name.orEmpty().ifBlank { "#${dto.id}" },
                    normalizedName = dto.name.orEmpty().lowercase(),
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
                val usd = dto.quote.usdQuote()
                connection.database.marketQueries.replaceCoinQuote(
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
        pool.onBatchCommitted(connection.driver)
    }

    suspend fun persistCoinMetadata(
        runId: String,
        page: Int,
        items: Map<String, CoinMetadataDto>,
    ) = pool.withConnection { connection ->
        val now = currentTimeMillis()
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
        pool.onBatchCommitted(connection.driver)
    }

    suspend fun persistExchangePage(
        runId: String,
        page: Int,
        items: List<ExchangeListingDto>,
    ) = pool.withConnection { connection ->
        val now = currentTimeMillis()
        connection.database.transaction {
            items.forEach { dto ->
                val usd = dto.quote.usdQuote()
                connection.database.marketQueries.insertExchangeIfMissing(
                    id = dto.id,
                    name = dto.name.orEmpty().ifBlank { "#${dto.id}" },
                    normalizedName = dto.name.orEmpty().lowercase(),
                    slug = dto.slug.orEmpty(),
                    rank = dto.rank,
                    numMarketPairs = dto.numMarketPairs,
                    spotVolumeUsd = usd.doubleValue("spot_volume_usd")
                        ?: usd.doubleValue("volume_24h"),
                    dateLaunched = dto.dateLaunched,
                    remoteUpdatedAt = dto.lastUpdated,
                    lastSeenRunId = runId,
                )
                connection.database.marketQueries.updateExchange(
                    name = dto.name.orEmpty().ifBlank { "#${dto.id}" },
                    normalizedName = dto.name.orEmpty().lowercase(),
                    slug = dto.slug.orEmpty(),
                    rank = dto.rank,
                    numMarketPairs = dto.numMarketPairs,
                    spotVolumeUsd = usd.doubleValue("spot_volume_usd")
                        ?: usd.doubleValue("volume_24h"),
                    dateLaunched = dto.dateLaunched,
                    remoteUpdatedAt = dto.lastUpdated,
                    lastSeenRunId = runId,
                    id = dto.id,
                )
            }
            connection.database.marketQueries.replaceCheckpoint(
                runId, SyncPhase.EXCHANGES.name, page.toLong(), "COMMITTED", items.size.toLong(), now,
            )
        }
        pool.onBatchCommitted(connection.driver)
    }

    suspend fun persistExchangeMetadata(
        runId: String,
        page: Int,
        items: Map<String, ExchangeMetadataDto>,
    ) = pool.withConnection { connection ->
        val now = currentTimeMillis()
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
        pool.onBatchCommitted(connection.driver)
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
            pool.onBatchCommitted(connection.driver)
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
                    connection.database.marketQueries.updateExchange(
                        exchange.name.orEmpty().ifBlank { "#$exchangeId" },
                        exchange.name.orEmpty().lowercase(),
                        exchange.slug.orEmpty(),
                        null, null, null, null, null,
                        "on-demand:$coinId",
                        exchangeId,
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
            pool.onBatchCommitted(connection.driver)
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
            pool.onBatchCommitted(connection.driver)
        }

    fun historyResourceKey(coinId: Long, range: CoinHistoryRange): String =
        "coin_history:$coinId:${range.name}"

    suspend fun replaceQuote(coinId: Long, dto: CoinListingDto) = pool.withConnection { connection ->
        val usd = dto.quote.usdQuote()
        connection.database.marketQueries.replaceCoinQuote(
            coinId,
            usd.doubleValue("price"),
            usd.doubleValue("volume_24h"),
            usd.doubleValue("percent_change_24h"),
            usd.doubleValue("market_cap"),
            usd.stringValue("last_updated") ?: dto.lastUpdated,
            currentTimeMillis(),
        )
        pool.onBatchCommitted(connection.driver)
    }

    suspend fun markCoinsComplete(runId: String) = pool.withConnection { connection ->
        connection.database.marketQueries.markCoinsInactiveOutsideRun(runId)
        pool.onBatchCommitted(connection.driver)
    }

    suspend fun markExchangesComplete(runId: String) = pool.withConnection { connection ->
        connection.database.marketQueries.markExchangesInactiveOutsideRun(runId)
        pool.onBatchCommitted(connection.driver)
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
        pool.onBatchCommitted(connection.driver)
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
}

private inline fun <reified T : Enum<T>> enumValueOfOrNull(value: String): T? =
    enumValues<T>().firstOrNull { it.name == value }
