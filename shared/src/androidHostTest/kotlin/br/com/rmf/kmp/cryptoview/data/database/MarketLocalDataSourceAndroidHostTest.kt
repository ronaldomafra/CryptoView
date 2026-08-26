package br.com.rmf.kmp.cryptoview.data.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import br.com.rmf.kmp.cryptoview.database.CryptoDatabase
import br.com.rmf.kmp.cryptoview.domain.model.SyncProgress
import br.com.rmf.kmp.cryptoview.domain.model.SyncStatus
import br.com.rmf.kmp.cryptoview.domain.model.SyncTrigger
import br.com.rmf.kmp.cryptoview.domain.model.api.CoinListingDto
import br.com.rmf.kmp.cryptoview.domain.model.api.CoinMetadataDto
import br.com.rmf.kmp.cryptoview.utils.CryptoProcessConfig
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MarketLocalDataSourceAndroidHostTest {
    private lateinit var driver: JdbcSqliteDriver
    private lateinit var database: CryptoDatabase
    private lateinit var pool: CryptoDatabasePool
    private lateinit var local: MarketLocalDataSource

    @BeforeTest
    fun setUp() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        CryptoDatabase.Schema.create(driver).value
        database = CryptoDatabase(driver)
        pool = CryptoDatabasePool(
            driverFactory = SameDriverFactory(driver),
            config = CryptoProcessConfig(parallelIoValue = 1, parallelDbValue = 1),
        )
        local = MarketLocalDataSource(database, pool)
    }

    @AfterTest
    fun tearDown() = runTest {
        pool.close()
    }

    @Test
    fun nativeUpsertInsertsThenUpdatesWithoutDeletingCachedMetadata() = runTest {
        local.createRun(
            SyncProgress(
                runId = RUN_ID,
                trigger = SyncTrigger.MANUAL_FULL,
                status = SyncStatus.RUNNING,
            ),
        )
        local.persistCoinPage(RUN_ID, page = 1, items = listOf(coin(name = "Bitcoin", price = 100.0)))
        local.persistCoinMetadata(
            RUN_ID,
            page = 1,
            items = mapOf("1" to CoinMetadataDto(id = 1, logo = CACHED_LOGO)),
        )
        local.persistCoinPage(RUN_ID, page = 2, items = listOf(coin(name = "Bitcoin Updated", price = 125.0)))

        val stored = local.observeCoin(1).first()
        assertEquals(1, local.countCoins())
        assertEquals("Bitcoin Updated", stored?.name)
        assertEquals(125.0, stored?.priceUsd)
        assertEquals(CACHED_LOGO, stored?.logoUrl)
    }

    @Test
    fun databaseTransactionRollsBackAllRowsWhenPageFails() = runTest {
        var failed = false
        try {
            pool.withConnection { connection ->
                connection.database.transaction {
                    connection.database.marketQueries.insertCoinIfMissing(
                        id = 2,
                        name = "Should Roll Back",
                        normalizedName = "should roll back",
                        symbol = "ROLL",
                        slug = "should-roll-back",
                        rank = null,
                        numMarketPairs = null,
                        circulatingSupply = null,
                        totalSupply = null,
                        maxSupply = null,
                        dateAdded = null,
                        remoteUpdatedAt = null,
                        lastSeenRunId = RUN_ID,
                    )
                    error("falha simulada")
                }
            }
        } catch (_: IllegalStateException) {
            failed = true
        }

        assertTrue(failed)
        assertEquals(0, local.countCoins())
    }

    private fun coin(name: String, price: Double) = CoinListingDto(
        id = 1,
        name = name,
        symbol = "BTC",
        slug = "bitcoin",
        rank = 1,
        quote = buildJsonObject {
            put("USD", buildJsonObject {
                put("price", price)
                put("volume_24h", 10.0)
                put("percent_change_24h", 2.0)
                put("market_cap", 1_000.0)
            })
        },
    )

    private class SameDriverFactory(
        private val driver: SqlDriver,
    ) : CryptoDatabaseDriverFactory {
        override fun createDriver(): SqlDriver = driver
    }

    private companion object {
        const val RUN_ID = "upsert-test"
        const val CACHED_LOGO = "https://cache.example/bitcoin.png"
    }
}
