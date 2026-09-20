package zhb.company.bgvoz

import org.apache.pdfbox.pdmodel.PDDocument
import zhb.company.bgvoz.data.TimetableData
import zhb.company.bgvoz.data.pdf.PdfTimetableParser
import java.io.File

/**
 * Regeneriše `data/BundledTimetable.kt` iz PDF-a u `src/test/resources`.
 *
 * Kada izađe novi red vožnje: zameni PDF u test resursima pa pokreni
 * `./gradlew :app:regenerateBundledTimetable`.
 */
object BundledTimetableGenerator {

    @JvmStatic
    fun main(args: Array<String>) {
        val pdf = File(args.getOrElse(0) { "app/src/test/resources/rv_bg_voza_za_2025.pdf" })
        val out = File(
            args.getOrElse(1) { "app/src/main/java/zhb/company/bgvoz/data/BundledTimetable.kt" },
        )
        val data = PDDocument.load(pdf).use { doc ->
            PdfTimetableParser.parse(DesktopPdf.readLines(doc), DesktopPdf.probe(doc))
        }
        out.writeText(render(data))
        println("Upisano: ${out.absolutePath} (${data.trains.size} vozova)")
    }

    private fun localDate(date: java.time.LocalDate) =
        "LocalDate.of(${date.year}, ${date.monthValue}, ${date.dayOfMonth})"

    private fun render(data: TimetableData): String = buildString {
        appendLine("package zhb.company.bgvoz.data")
        appendLine()
        appendLine("import java.time.LocalDate")
        appendLine()
        appendLine("/**")
        appendLine(" * Ugrađena kopija reda vožnje BG:VOZ-a za relaciju Batajnica – Ovča.")
        appendLine(" *")
        appendLine(" * Aplikacija na svakom pokretanju preuzima i parsira zvanični PDF")
        appendLine(" * ([TimetableRepository]); ovo je rezerva za prvi start bez mreže. Izvor:")
        appendLine(" * https://www.srbvoz.rs/wp-content/redvoznje/rv_bg_voza_za_2025.pdf")
        appendLine(" *")
        appendLine(" * NE MENJATI RUČNO – regeneriše ga BundledTimetableGenerator")
        appendLine(" * (./gradlew :app:regenerateBundledTimetable).")
        appendLine(" */")
        appendLine("object BundledTimetable {")
        appendLine()
        appendLine("    private val route: List<Station> = listOf(")
        data.route.forEach {
            val stops = if (it.stops) "" else ", stops = false"
            appendLine("        Station(\"${it.id}\", \"${it.name}\"$stops),")
        }
        appendLine("    )")
        appendLine()
        appendLine("    private val holidays: Set<Triple<Int, Int, Int>> = setOf(")
        data.holidays.sortedWith(compareBy({ it.first }, { it.second }, { it.third })).forEach {
            appendLine("        Triple(${it.first}, ${it.second}, ${it.third}),")
        }
        appendLine("    )")
        appendLine()
        appendLine("    private val raw: List<Array<Any>> = listOf(")
        data.trains.forEach { train ->
            val times = train.times.joinToString(", ") { it?.toString() ?: "null" }
            appendLine(
                "        arrayOf(\"${train.number}\", Direction.${train.direction.name}, " +
                    "${train.weekdayOnly}, listOf($times)),",
            )
        }
        appendLine("    )")
        appendLine()
        appendLine("    @Suppress(\"UNCHECKED_CAST\")")
        appendLine("    val data: TimetableData by lazy {")
        appendLine("        TimetableData(")
        appendLine("            route = route,")
        appendLine("            trains = raw.map {")
        appendLine("                Train(")
        appendLine("                    number = it[0] as String,")
        appendLine("                    direction = it[1] as Direction,")
        appendLine("                    weekdayOnly = it[2] as Boolean,")
        appendLine("                    times = it[3] as List<Int?>,")
        appendLine("                    route = route,")
        appendLine("                )")
        appendLine("            },")
        appendLine("            validFrom = ${data.validFrom?.let(::localDate)},")
        appendLine("            validTo = ${data.validTo?.let(::localDate)},")
        appendLine("            holidays = holidays,")
        appendLine("            origin = DataOrigin.BUNDLED,")
        appendLine("            fetchedAt = null,")
        appendLine("        )")
        appendLine("    }")
        appendLine("}")
    }
}
