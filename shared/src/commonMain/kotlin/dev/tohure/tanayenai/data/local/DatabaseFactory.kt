package dev.tohure.tanayenai.data.local

import dev.tohure.tanayenai.db.TanayenDatabase

fun createDatabase(driverFactory: DatabaseDriverFactory): TanayenDatabase =
    TanayenDatabase(driverFactory.createDriver())
