package br.com.rmf.kmp.cryptoview.data.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import br.com.rmf.kmp.cryptoview.database.CryptoDatabase

internal class IosCryptoDatabaseDriverFactory : CryptoDatabaseDriverFactory {
    override fun createDriver(): SqlDriver = NativeSqliteDriver(
        schema = CryptoDatabase.Schema,
        name = "cryptoview.db",
    )
}

