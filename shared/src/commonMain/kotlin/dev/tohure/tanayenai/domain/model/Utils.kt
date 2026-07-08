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

// Fecha y hora en la zona horaria local del dispositivo. Es clave que ambas usen
// la misma zona: loggedAt (currentIsoDateTime) y el prefijo "hoy" (currentIsoDate)
// se comparan con LIKE 'YYYY-MM-DD%', así que si difieren (p. ej. una en UTC y otra
// local) el registro del día "desaparece" al cruzar la medianoche UTC en zonas UTC-5.
fun currentIsoDateTime(): String {
    val tz = TimeZone.currentSystemDefault()
    return Clock.System
        .now()
        .toLocalDateTime(tz)
        .toString()
}

fun currentIsoDate(): String {
    val tz = TimeZone.currentSystemDefault()
    return Clock.System
        .now()
        .toLocalDateTime(tz)
        .date
        .toString()
}

fun daysAgo(n: Int): String {
    val tz = TimeZone.currentSystemDefault()
    val today =
        Clock.System
            .now()
            .toLocalDateTime(tz)
            .date
    return today.minus(n, DateTimeUnit.DAY).toString()
}
