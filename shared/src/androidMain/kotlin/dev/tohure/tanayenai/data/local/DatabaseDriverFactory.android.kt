package dev.tohure.tanayenai.data.local

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import dev.tohure.tanayenai.db.TanayenDatabase

actual class DatabaseDriverFactory(
    private val context: Context,
) {
    actual fun createDriver(): SqlDriver =
        AndroidSqliteDriver(
            schema = TanayenDatabase.Schema,
            context = context,
            name = "tanayen.db",
        )
}
