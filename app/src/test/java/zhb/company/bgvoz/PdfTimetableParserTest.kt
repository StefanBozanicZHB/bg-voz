package zhb.company.bgvoz

import org.apache.pdfbox.pdmodel.PDDocument
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import zhb.company.bgvoz.data.DataOrigin
import zhb.company.bgvoz.data.Direction
import zhb.company.bgvoz.data.TimetableData
import zhb.company.bgvoz.data.pdf.PdfTimetableParser
import java.io.File

/** Parsira pravi PDF Srbija Voza (RV 2025/2026) i poredi sa onim što u njemu zaista piše. */
class PdfTimetableParserTest {

    private val parsed: TimetableData by lazy {
        val file = File(javaClass.classLoader!!.getResource("rv_bg_voza_za_2025.pdf")!!.toURI())
        PDDocument.load(file).use { doc ->
            PdfTimetableParser.parse(
                pages = DesktopPdf.readLines(doc),
                probe = DesktopPdf.probe(doc),
                origin = DataOrigin.NETWORK,
                fetchedAt = 0L,
            )
        }
    }

    @Test
    fun `relacija ima 15 stanica, od Batajnice do Ovce`() {
        assertEquals(15, parsed.route.size)
        assertEquals("Batajnica", parsed.route.first().name)
        assertEquals("Ovča", parsed.route.last().name)
        assertEquals(
            listOf("Zemunsko Polje", "Tošin Bunar", "Krnjača Ukrsnica"),
            listOf(parsed.route[2].name, parsed.route[5].name, parsed.route[12].name),
        )
    }

    @Test
    fun `Novi Beograd je u listi ali bez stajanja`() {
        val noviBeograd = parsed.route.single { it.name == "Novi Beograd" }
        assertEquals(false, noviBeograd.stops)
        // sve ostale stanice imaju vremena
        assertEquals(listOf("Novi Beograd"), parsed.route.filter { !it.stops }.map { it.name })

        val index = parsed.route.indexOf(noviBeograd)
        assertTrue(parsed.trains.all { it.times[index] == null })
        assertTrue(parsed.callsAt(index, Direction.BATAJNICA_OVCA, weekendOrHoliday = false).isEmpty())
    }

    @Test
    fun `oba smera imaju tacan broj polazaka`() {
        assertEquals(23, parsed.trains.count { it.direction == Direction.BATAJNICA_OVCA })
        assertEquals(27, parsed.trains.count { it.direction == Direction.OVCA_BATAJNICA })
    }

    @Test
    fun `vremena prvog i poslednjeg polaska odgovaraju PDF-u`() {
        val t8001 = parsed.trains.single { it.number == "8001" }
        assertEquals(6 * 60, t8001.times.first())
        assertEquals(6 * 60 + 48, t8001.times.last())
        assertEquals("Ovča", t8001.terminus.name)

        val t8000 = parsed.trains.single { it.number == "8000" }
        assertEquals(5 * 60 + 40, t8000.times.last())
        assertEquals(6 * 60 + 28, t8000.times.first())
        assertEquals("Batajnica", t8000.terminus.name)

        // Beograd Centar se u tabeli pojavljuje dvaput (dolazak i polazak) – uzima se polazak
        val bgCentar = parsed.route.indexOfFirst { it.name == "Beograd Centar" }
        assertEquals(6 * 60 + 23, t8001.times[bgCentar])
    }

    @Test
    fun `vozovi koji ne idu do krajnje stanice su prepoznati`() {
        val shortTurns = parsed.trains.filter { it.isShortTurn }.associate { it.number to it.terminus.name }
        assertEquals(mapOf("8020" to "Zemun", "8042" to "Zemun", "8340" to "Zemun"), shortTurns)

        val t8020 = parsed.trains.single { it.number == "8020" }
        assertEquals(null, t8020.times.first()) // do Batajnice ne ide
        assertEquals(11 * 60 + 56, t8020.times[parsed.route.indexOfFirst { it.name == "Zemun" }])
    }

    @Test
    fun `zeleno obojene kolone su vozovi koji ne saobracaju vikendom`() {
        val weekdayOnly = parsed.trains.filter { it.weekdayOnly }.map { it.number }.sorted()
        assertEquals(
            listOf(
                "8000", "8003", "8004", "8007", "8008", "8011", "8012", "8015",
                "8020", "8027", "8028", "8031", "8032", "8035", "8036", "8042",
                "8043", "8048",
            ),
            weekdayOnly,
        )
    }

    @Test
    fun `vazenje i drzavni praznici su procitani iz napomene`() {
        assertEquals("09.09.2026.", parsed.validFrom)
        assertEquals("12.12.2026.", parsed.validTo)
        assertTrue(Triple(2026, 11, 11) in parsed.holidays)
        assertTrue(Triple(2026, 1, 7) in parsed.holidays)
        assertTrue(Triple(2026, 4, 12) in parsed.holidays)
        // datum važenja ne sme da se protumači kao praznik
        assertTrue(Triple(2026, 9, 9) !in parsed.holidays)
    }

    @Test
    fun `polasci sa stanice su sortirani, vikendom ih je manje`() {
        val zemun = parsed.route.indexOfFirst { it.name == "Zemun" }
        val radniDan = parsed.callsAt(zemun, Direction.BATAJNICA_OVCA, weekendOrHoliday = false)
        val vikend = parsed.callsAt(zemun, Direction.BATAJNICA_OVCA, weekendOrHoliday = true)
        assertEquals(23, radniDan.size)
        assertEquals(15, vikend.size)
        assertEquals(radniDan.map { it.minuteOfDay }.sorted(), radniDan.map { it.minuteOfDay })
        assertTrue(radniDan.none { it.isArrival })
    }

    @Test
    fun `na krajnjoj stanici se prikazuju dolasci`() {
        val ovca = parsed.route.lastIndex
        val dolasci = parsed.callsAt(ovca, Direction.BATAJNICA_OVCA, weekendOrHoliday = false)
        assertEquals(23, dolasci.size)
        assertTrue(dolasci.all { it.isArrival })
        assertEquals(6 * 60 + 48, dolasci.first().minuteOfDay)

        // voz koji se okreće u Zemunu tamo takođe „stiže", a ne polazi
        val zemun = parsed.route.indexOfFirst { it.name == "Zemun" }
        val u8042 = parsed.callsAt(zemun, Direction.OVCA_BATAJNICA, weekendOrHoliday = false)
            .single { it.train.number == "8042" }
        assertTrue(u8042.isArrival)
        assertTrue(u8042.train.isShortTurn)
    }
}
