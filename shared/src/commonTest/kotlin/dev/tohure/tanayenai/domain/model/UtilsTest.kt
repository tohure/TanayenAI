package dev.tohure.tanayenai.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UtilsTest {
    private val isoDateRegex = Regex("""^\d{4}-\d{2}-\d{2}$""")

    @Test
    fun currentIsoDate_returnsIsoDateFormat() {
        val date = currentIsoDate()
        assertTrue(isoDateRegex.matches(date), "currentIsoDate() debe ser 'YYYY-MM-DD', fue: $date")
    }

    @Test
    fun currentIsoDateTime_startsWithCurrentIsoDate() {
        // Ambas usan la misma zona local: el prefijo de fecha del timestamp debe
        // coincidir con currentIsoDate(). Es lo que permite que el filtro
        // logged_at LIKE 'YYYY-MM-DD%' encuentre los registros del día actual.
        val dateTime = currentIsoDateTime()
        assertEquals(currentIsoDate(), dateTime.take(10))
    }

    @Test
    fun currentIsoDateTime_firstTenCharsAreIsoDate() {
        val prefix = currentIsoDateTime().take(10)
        assertTrue(isoDateRegex.matches(prefix), "El prefijo del timestamp debe ser 'YYYY-MM-DD', fue: $prefix")
    }
}
