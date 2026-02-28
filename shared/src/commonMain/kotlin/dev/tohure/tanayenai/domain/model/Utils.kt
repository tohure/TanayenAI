package dev.tohure.tanayenai.domain.model

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
