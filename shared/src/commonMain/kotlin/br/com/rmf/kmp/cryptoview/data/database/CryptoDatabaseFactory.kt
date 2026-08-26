package br.com.rmf.kmp.cryptoview.data.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.QueryResult
import br.com.rmf.kmp.cryptoview.database.CryptoDatabase
import br.com.rmf.kmp.cryptoview.utils.CryptoProcessConfig
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

interface CryptoDatabaseDriverFactory {
    fun createDriver(): SqlDriver
}

internal fun CryptoDatabaseDriverFactory.createConfiguredDriver(
): SqlDriver = createDriver().also { driver ->
    driver.executePragmaQuery("PRAGMA foreign_keys=ON")
    driver.executePragmaQuery("PRAGMA journal_mode=WAL")
    driver.executePragmaQuery("PRAGMA synchronous=NORMAL")
    driver.executePragmaQuery("PRAGMA busy_timeout=$DATABASE_BUSY_TIMEOUT_MILLIS")
}

internal data class CryptoDatabaseConnection(
    val driver: SqlDriver,
    val database: CryptoDatabase,
    val supportsNativeUpsert: Boolean,
)

internal enum class DatabaseChange {
    COINS,
    COIN_QUOTES,
    COIN_METADATA,
    EXCHANGES,
    EXCHANGE_METADATA,
    EXCHANGE_ASSETS,
    COIN_MARKETS,
    COIN_HISTORY,
    PAPRIKA_MAPPING,
    PAPRIKA_INFO,
    ALL,
}

internal class CryptoDatabasePool(
    private val driverFactory: CryptoDatabaseDriverFactory,
    private val config: CryptoProcessConfig,
) {
    private val size = config.parallelDbValue.coerceIn(1, MAX_DATABASE_CONNECTIONS)
    private val semaphore = Semaphore(size)
    private val lock = Mutex()
    private val available = ArrayDeque<CryptoDatabaseConnection>()
    private val all = mutableListOf<CryptoDatabaseConnection>()
    private var committedBatches = 0
    private val changeVersions = MutableStateFlow(
        DatabaseChange.entries.associateWith { 0L },
    )

    fun observeChanges(vararg resources: DatabaseChange): Flow<Unit> = changeVersions
        .map { versions ->
            buildList {
                add(versions.getValue(DatabaseChange.ALL))
                resources.forEach { resource -> add(versions.getValue(resource)) }
            }
        }
        .distinctUntilChanged()
        .map { }

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

    suspend fun onBatchCommitted(
        driver: SqlDriver,
        vararg resources: DatabaseChange,
    ) {
        require(resources.isNotEmpty()) { "Informe ao menos um recurso alterado" }
        val shouldCheckpoint = lock.withLock {
            committedBatches += 1
            val nextVersions = changeVersions.value.toMutableMap()
            resources.forEach { resource ->
                nextVersions[resource] = nextVersions.getValue(resource) + 1
            }
            changeVersions.value = nextVersions
            committedBatches % WAL_CHECKPOINT_BATCH_INTERVAL == 0
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
        val driver = driverFactory.createConfiguredDriver()
        return CryptoDatabaseConnection(
            driver = driver,
            database = CryptoDatabase(driver),
            supportsNativeUpsert = driver.sqliteVersionAtLeast(major = 3, minor = 24),
        ).also(all::add)
    }
}

private fun SqlDriver.sqliteVersionAtLeast(major: Int, minor: Int): Boolean {
    val version = runCatching {
        executeQuery(
            identifier = null,
            sql = "SELECT sqlite_version()",
            mapper = { cursor ->
                val value = if (cursor.next().value) cursor.getString(0) else null
                QueryResult.Value(value)
            },
            parameters = 0,
            binders = null,
        ).value
    }.getOrNull() ?: return false
    val parts = version.split('.')
    val installedMajor = parts.getOrNull(0)?.toIntOrNull() ?: return false
    val installedMinor = parts.getOrNull(1)?.toIntOrNull() ?: return false
    return installedMajor > major || (installedMajor == major && installedMinor >= minor)
}

private fun SqlDriver.executePragmaQuery(sql: String) {
    val executedAsStatement = runCatching {
        execute(
            identifier = null,
            sql = sql,
            parameters = 0,
            binders = null,
        ).value
    }.isSuccess
    if (executedAsStatement) return

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

private const val MAX_DATABASE_CONNECTIONS = 4
private const val DATABASE_BUSY_TIMEOUT_MILLIS = 5_000L
private const val WAL_CHECKPOINT_BATCH_INTERVAL = 10
