package zhb.company.bgvoz

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import zhb.company.bgvoz.data.BundledTimetable
import zhb.company.bgvoz.data.Direction
import java.time.LocalDate

/**
 * Vidžet prikazuje samo nekoliko sledećih vozova, pa mora da ume da pređe na sutra
 * kada se današnji polasci potroše – i da pritom uzme režim saobraćaja za taj dan.
 */
class UpcomingCallsTest {

    private val data = BundledTimetable.data
    private val pancevackiMost = data.route.indexOfFirst { it.name == "Pančevački Most" }

    /** 2026-10-15 je četvrtak, 2026-10-17 subota, 2026-10-18 nedelja. */
    private val cetvrtak = LocalDate.of(2026, 10, 15)
    private val subota = LocalDate.of(2026, 10, 17)

    @Test
    fun `vraca prve polaske posle zadatog vremena`() {
        val upcoming = data.upcoming(
            stationIndex = pancevackiMost,
            direction = Direction.BATAJNICA_OVCA,
            today = cetvrtak,
            nowMinutes = 16 * 60,
            limit = 3,
        )
        assertEquals(3, upcoming.size)
        assertTrue(upcoming.all { !it.isTomorrow })
        assertTrue(upcoming.all { it.call.minuteOfDay >= 16 * 60 })
        assertEquals(upcoming.map { it.call.minuteOfDay }.sorted(), upcoming.map { it.call.minuteOfDay })
    }

    @Test
    fun `posle poslednjeg voza prelazi na sutra`() {
        val upcoming = data.upcoming(
            stationIndex = pancevackiMost,
            direction = Direction.BATAJNICA_OVCA,
            today = cetvrtak,
            nowMinutes = 23 * 60 + 30,
            limit = 3,
        )
        assertEquals(3, upcoming.size)
        assertTrue(upcoming.all { it.isTomorrow })
        // petak je radni dan – prvi sutrašnji polazak je prvi iz celog reda vožnje
        val prviPetak = data.callsAt(pancevackiMost, Direction.BATAJNICA_OVCA, weekendOrHoliday = false)
            .first().minuteOfDay
        assertEquals(prviPetak, upcoming.first().call.minuteOfDay)
    }

    @Test
    fun `sutrasnji dan se racuna po svom rezimu, ne po danasnjem`() {
        // u petak kasno uveče sledeći polasci su subotnji – bez vozova „samo radnim danom"
        val upcoming = data.upcoming(
            stationIndex = pancevackiMost,
            direction = Direction.BATAJNICA_OVCA,
            today = subota.minusDays(1),
            nowMinutes = 23 * 60 + 59,
            limit = 5,
        )
        assertTrue(upcoming.isNotEmpty())
        assertTrue(upcoming.all { it.isTomorrow })
        assertTrue(upcoming.none { it.call.train.weekdayOnly })
    }

    @Test
    fun `nepoznata stanica ne ruši racun`() {
        val noviBeograd = data.route.indexOfFirst { !it.stops }
        val upcoming = data.upcoming(noviBeograd, Direction.BATAJNICA_OVCA, cetvrtak, 0, 3)
        assertTrue(upcoming.isEmpty())
    }
}
