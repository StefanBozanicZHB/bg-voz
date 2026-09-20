package zhb.company.bgvoz

import org.apache.pdfbox.pdmodel.PDDocument
import org.junit.Assert.assertEquals
import org.junit.Test
import zhb.company.bgvoz.data.BundledTimetable
import zhb.company.bgvoz.data.Train
import zhb.company.bgvoz.data.pdf.PdfTimetableParser
import java.io.File

/**
 * Ugrađena kopija ([BundledTimetable]) se koristi samo pri prvom pokretanju bez mreže.
 * Ovaj test hvata ako odluta od PDF-a iz `src/test/resources` – tada je treba
 * regenerisati ([BundledTimetableGenerator]).
 */
class BundledTimetableTest {

    @Test
    fun `ugradjena kopija se poklapa sa PDF-om`() {
        val file = File(javaClass.classLoader!!.getResource("rv_bg_voza_za_2025.pdf")!!.toURI())
        val fromPdf = PDDocument.load(file).use { doc ->
            PdfTimetableParser.parse(DesktopPdf.readLines(doc), DesktopPdf.probe(doc))
        }
        val bundled = BundledTimetable.data

        assertEquals(fromPdf.route, bundled.route)
        assertEquals(fromPdf.holidays, bundled.holidays)
        assertEquals(fromPdf.validFrom, bundled.validFrom)
        assertEquals(fromPdf.validTo, bundled.validTo)
        assertEquals(fromPdf.trains.map(::signature), bundled.trains.map(::signature))
    }

    private fun signature(train: Train) =
        listOf(train.number, train.direction, train.weekdayOnly, train.times)
}
