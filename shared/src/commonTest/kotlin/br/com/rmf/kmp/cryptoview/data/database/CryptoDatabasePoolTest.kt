package br.com.rmf.kmp.cryptoview.data.database

import app.cash.sqldelight.Query
import app.cash.sqldelight.Transacter
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlPreparedStatement
import br.com.rmf.kmp.cryptoview.utils.CryptoProcessConfig
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class CryptoDatabasePoolTest {
    @Test
    fun onlyObserversOfChangedResourcesAreInvalidated() = runTest {
        val pool = CryptoDatabasePool(
            driverFactory = UnusedDriverFactory,
            config = CryptoProcessConfig(parallelIoValue = 1, parallelDbValue = 1),
        )
        var coinEmissions = 0
        var exchangeEmissions = 0
        val dispatcher = UnconfinedTestDispatcher(testScheduler)

        backgroundScope.launch(dispatcher) {
            pool.observeChanges(DatabaseChange.COINS).collect { coinEmissions += 1 }
        }
        backgroundScope.launch(dispatcher) {
            pool.observeChanges(DatabaseChange.EXCHANGES).collect { exchangeEmissions += 1 }
        }

        assertEquals(1, coinEmissions)
        assertEquals(1, exchangeEmissions)

        pool.onBatchCommitted(UnusedSqlDriver, DatabaseChange.COINS)
        assertEquals(2, coinEmissions)
        assertEquals(1, exchangeEmissions)

        pool.onBatchCommitted(UnusedSqlDriver, DatabaseChange.ALL)
        assertEquals(3, coinEmissions)
        assertEquals(2, exchangeEmissions)
    }

    private data object UnusedDriverFactory : CryptoDatabaseDriverFactory {
        override fun createDriver(): SqlDriver = error("A conexão não deve ser criada neste teste")
    }

    private data object UnusedSqlDriver : SqlDriver {
        override fun <R> executeQuery(
            identifier: Int?,
            sql: String,
            mapper: (SqlCursor) -> QueryResult<R>,
            parameters: Int,
            binders: (SqlPreparedStatement.() -> Unit)?,
        ): QueryResult<R> = error("SQL inesperado")

        override fun execute(
            identifier: Int?,
            sql: String,
            parameters: Int,
            binders: (SqlPreparedStatement.() -> Unit)?,
        ): QueryResult<Long> = error("SQL inesperado")

        override fun newTransaction(): QueryResult<Transacter.Transaction> = error("Transação inesperada")
        override fun currentTransaction(): Transacter.Transaction? = null
        override fun addListener(vararg queryKeys: String, listener: Query.Listener) = Unit
        override fun removeListener(vararg queryKeys: String, listener: Query.Listener) = Unit
        override fun notifyListeners(vararg queryKeys: String) = Unit
        override fun close() = Unit
    }
}
