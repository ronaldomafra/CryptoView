package br.com.rmf.kmp.cryptoview.data.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.QueryResult
import br.com.rmf.kmp.cryptoview.database.CryptoDatabase
import br.com.rmf.kmp.cryptoview.utils.CryptoProcessConfig
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface CryptoDatabaseDriverFactory {
    fun createDriver(): SqlDriver
}

internal fun CryptoDatabaseDriverFactory.createConfiguredDriver(
    config: CryptoProcessConfig,
): SqlDriver = createDriver().also { driver ->
    driver.executePragmaQuery("PRAGMA foreign_keys=ON")
    driver.executePragmaQuery("PRAGMA journal_mode=WAL")
    driver.executePragmaQuery("PRAGMA synchronous=NORMAL")
    driver.executePragmaQuery("PRAGMA busy_timeout=${config.databaseBusyTimeoutMillis}")
}

internal data class CryptoDatabaseConnection(
    val driver: SqlDriver,
    val database: CryptoDatabase,
)

internal class CryptoDatabasePool(
    private val driverFactory: CryptoDatabaseDriverFactory,
    private val config: CryptoProcessConfig,
) {
    private val size = config.databasePoolSize.coerceIn(1, config.databasePoolMaxSize)
    private val semaphore = Semaphore(size)
    private val lock = Mutex()
    private val available = ArrayDeque<CryptoDatabaseConnection>()
    private val all = mutableListOf<CryptoDatabaseConnection>()
    private var committedBatches = 0
    private val _changeVersion = MutableStateFlow(0L)
    val changeVersion: StateFlow<Long> = _changeVersion.asStateFlow()

    suspend fun <T> withConnection(
        block: suspend (CryptoDatabaseConnection) -> T,
    ): T {
        semaphore.acquire()
        val connection = lock.withLock {
            available.removeFirstOrNull() ?: createConnection()
        }
        return try {
            block(connection)
        } finally {
            lock.withLock { available.addLast(connection) }
            semaphore.release()
        }
    }

    suspend fun onBatchCommitted(driver: SqlDriver) {
        val shouldCheckpoint = lock.withLock {
            committedBatches += 1
            _changeVersion.value += 1
            committedBatches % config.walCheckpointEveryCommittedBatches == 0
        }
        if (shouldCheckpoint) {
            runCatching { driver.executePragmaQuery("PRAGMA wal_checkpoint(PASSIVE)") }
        }
    }

    suspend fun checkpoint(mode: String = "PASSIVE") {
        withConnection { connection ->
            runCatching {
                connection.driver.executePragmaQuery("PRAGMA wal_checkpoint($mode)")
            }
        }
    }

    suspend fun close() {
        lock.withLock {
            all.forEach { connection -> runCatching { connection.driver.close() } }
            all.clear()
            available.clear()
        }
    }

    private fun createConnection(): CryptoDatabaseConnection {
        val driver = driverFactory.createConfiguredDriver(config)
        return CryptoDatabaseConnection(driver, CryptoDatabase(driver)).also(all::add)
    }
}

private fun SqlDriver.executePragmaQuery(sql: String) {
    executeQuery(
        identifier = null,
        sql = sql,
        mapper = { cursor ->
            cursor.next().value
            QueryResult.Value(Unit)
        },
        parameters = 0,
        binders = null,
    ).value
}
