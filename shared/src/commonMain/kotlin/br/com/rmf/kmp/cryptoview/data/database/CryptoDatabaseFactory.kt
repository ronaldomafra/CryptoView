package br.com.rmf.kmp.cryptoview.data.database

import app.cash.sqldelight.db.SqlDriver
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
    driver.execute(null, "PRAGMA foreign_keys=ON", 0)
    driver.execute(null, "PRAGMA journal_mode=WAL", 0)
    driver.execute(null, "PRAGMA synchronous=NORMAL", 0)
    driver.execute(null, "PRAGMA busy_timeout=${config.databaseBusyTimeoutMillis}", 0)
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
            runCatching { driver.execute(null, "PRAGMA wal_checkpoint(PASSIVE)", 0) }
        }
    }

    suspend fun checkpoint(mode: String = "PASSIVE") {
        withConnection { connection ->
            runCatching {
                connection.driver.execute(null, "PRAGMA wal_checkpoint($mode)", 0)
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
