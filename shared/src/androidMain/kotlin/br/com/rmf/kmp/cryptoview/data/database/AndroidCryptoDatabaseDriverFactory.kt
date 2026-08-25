package br.com.rmf.kmp.cryptoview.data.database

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import br.com.rmf.kmp.cryptoview.database.CryptoDatabase

internal class AndroidCryptoDatabaseDriverFactory(
    private val context: Context,
) : CryptoDatabaseDriverFactory {
    override fun createDriver(): SqlDriver = AndroidSqliteDriver(
        schema = CryptoDatabase.Schema,
        context = context,
        name = DATABASE_NAME,
    )

    private companion object {
        const val DATABASE_NAME = "cryptoview.db"
    }
}

