package dev.tohure.tanayenai.domain.model

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import kotlin.random.Random
import kotlin.time.Clock

fun generateId(): String {
    val timestamp = Clock.System.now().toEpochMilliseconds()
    val random = Random.nextLong(1000, 9999)
    return "${timestamp}_$random"
}

fun currentIsoDateTime() = Clock.System.now().toString()

fun currentIsoDate() =
    Clock.System
        .now()
        .toString()
        .take(10)

fun daysAgo(n: Int): String {
    val tz = TimeZone.currentSystemDefault()
    val today =
        Clock.System
            .now()
            .toLocalDateTime(tz)
            .date
    return today.minus(n, DateTimeUnit.DAY).toString()
}
