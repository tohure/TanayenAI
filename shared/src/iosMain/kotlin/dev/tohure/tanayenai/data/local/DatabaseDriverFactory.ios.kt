package dev.tohure.tanayenai.data.local

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import dev.tohure.tanayenai.db.TanayenDatabase

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver =
        NativeSqliteDriver(
            schema = TanayenDatabase.Schema,
            name = "tanayen.db",
        )
}
