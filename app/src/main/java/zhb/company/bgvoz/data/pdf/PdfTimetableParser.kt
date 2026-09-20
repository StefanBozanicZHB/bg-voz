package zhb.company.bgvoz.data.pdf

import zhb.company.bgvoz.data.DataOrigin
import zhb.company.bgvoz.data.Direction
import zhb.company.bgvoz.data.Station
import zhb.company.bgvoz.data.TimetableData
import zhb.company.bgvoz.data.Train
import java.time.DateTimeException
import java.time.LocalDate
import kotlin.math.abs

class PdfParseException(message: String) : Exception(message)

/**
 * Čita zvanični PDF red vožnje BG:VOZ-a i iz njega vadi relaciju Batajnica – Ovča.
 *
 * PDF nema nikakav mašinski format – sve se rekonstruiše iz pozicija teksta:
 *  * red sa ≥ 8 četvorocifrenih brojeva = zaglavlje tabele (brojevi vozova = kolone),
 *  * svaki sledeći red = jedna stanica, a vreme se dodeljuje koloni po x koordinati,
 *  * prazna ćelija znači da voz tu ne staje / ne ide do te stanice,
 *  * kolona obojena zeleno = voz ne saobraća vikendom i praznikom (vidi [ShadingProbe]).
 */
object PdfTimetableParser {

    private const val MIN_COLUMNS = 8

    private val TIME = Regex("""^(\d{1,2})[,.:](\d{2})$""")
    private val TRAIN_NO = Regex("""^\d{4}$""")
    private val FULL_DATE = Regex("""(\d{1,2})\.(\d{1,2})\.(\d{4})""")
    private val DAY_OR_DATE = Regex("""\d{1,2}\.\d{1,2}\.\d{4}|\d{1,2}\.""")

    /** [pages] su redovi teksta po stranama, u vizuelnim koordinatama (vidi [LineBuilder]). */
    fun parse(
        pages: List<List<TextLine>>,
        probe: ShadingProbe? = null,
        origin: DataOrigin = DataOrigin.NETWORK,
        fetchedAt: Long? = System.currentTimeMillis(),
    ): TimetableData {
        val tables = pages.flatMapIndexed { pageIndex, lines ->
            findTables(lines).map { pageIndex to it }
        }.mapNotNull { (pageIndex, table) -> table.toDirected(pageIndex) }

        val forward = tables.firstOrNull { it.direction == Direction.BATAJNICA_OVCA }
            ?: throw PdfParseException("U PDF-u nije nađena tabela Batajnica → Ovča.")
        val backward = tables.firstOrNull { it.direction == Direction.OVCA_BATAJNICA }
            ?: throw PdfParseException("U PDF-u nije nađena tabela Ovča → Batajnica.")

        val route = forward.stations
        val trains = forward.toTrains(route, probe) + backward.toTrains(route, probe)
        if (trains.isEmpty()) throw PdfParseException("Tabele su nađene ali nemaju nijedan polazak.")

        val allLines = pages.flatten()
        val validity = validity(allLines)
        return TimetableData(
            route = route,
            trains = trains,
            validFrom = validity?.first,
            validTo = validity?.second,
            holidays = holidays(allLines),
            origin = origin,
            fetchedAt = fetchedAt,
        )
    }

    // --------------------------------------------------------------- tabele

    private class RawTable(
        val numbers: List<String>,
        val centers: List<Float>,
        val columnWidth: Float,
        /** naziv stanice → vremena po kolonama (null = ne staje) */
        val rows: List<Pair<String, List<Int?>>>,
        val bodyTop: Float,
        val bodyBottom: Float,
    )

    private fun findTables(lines: List<TextLine>): List<RawTable> {
        val result = mutableListOf<RawTable>()
        lines.forEachIndexed { index, line ->
            val nums = line.words.filter { TRAIN_NO.matches(it.text) }.sortedBy { it.centerX }
            if (nums.size < MIN_COLUMNS) return@forEachIndexed
            val centers = nums.map { it.centerX }
            val width = centers.zipWithNext { a, b -> b - a }.sorted().let { it[it.size / 2] }
            val firstLeft = centers.first() - width / 2f

            val rows = mutableListOf<Pair<String, List<Int?>>>()
            var started = false
            var top = line.y
            var bottom = line.y
            for (i in index + 1 until lines.size) {
                val l = lines[i]
                if (l.words.count { TRAIN_NO.matches(it.text) } >= MIN_COLUMNS) break

                val cells = centers.map { cx ->
                    l.words.firstOrNull {
                        abs(it.centerX - cx) <= width * 0.45f && TIME.matches(it.text)
                    }?.let { toMinutes(it.text) }
                }
                val passes = centers.count { cx ->
                    l.words.any { abs(it.centerX - cx) <= width * 0.45f && (it.text == "|" || it.text == "│") }
                }
                val hasContent = cells.any { it != null } || passes > 0
                if (!hasContent) {
                    if (started) break else continue
                }
                started = true
                if (rows.isEmpty()) top = l.y
                bottom = l.y

                val label = l.words.filter { it.x1 <= firstLeft + 2f }.joinToString(" ") { it.text }
                val station = SerbianText.stationName(label) ?: continue
                // red bez ijednog vremena, a sa „│" u svakoj koloni = voz prolazi bez stajanja
                if (cells.all { it == null } && passes < centers.size / 2) continue
                rows += station to cells
            }
            if (rows.size >= 3) {
                result += RawTable(nums.map { it.text }, centers, width, rows, top, bottom)
            }
        }
        return result
    }

    private class DirectedTable(
        val raw: RawTable,
        val pageIndex: Int,
        val direction: Direction,
        val stations: List<Station>,
        /** za svaku kolonu: vremena poravnata sa [stations] (redosled Batajnica → Ovča) */
        val columns: List<List<Int?>>,
    )

    private fun RawTable.toDirected(pageIndex: Int): DirectedTable? {
        // spoj ponovljene stanice (Beograd Centar: dolazak + polazak) – zadrži polazak
        val merged = LinkedHashMap<String, List<Int?>>()
        for ((name, cells) in rows) {
            merged[name] = merged[name]?.let { prev -> prev.zip(cells) { a, b -> b ?: a } } ?: cells
        }
        val names = merged.keys.toList()
        val ids = names.map { SerbianText.stationId(it) }
        if ("BATAJNICA" !in ids || "OVCA" !in ids) return null

        val direction = if (ids.first() == "BATAJNICA") Direction.BATAJNICA_OVCA else Direction.OVCA_BATAJNICA
        val stations = names.map { name ->
            Station(
                id = SerbianText.stationId(name),
                name = name,
                stops = merged.getValue(name).any { it != null },
            )
        }.let { if (direction == Direction.BATAJNICA_OVCA) it else it.reversed() }

        val columns = numbers.indices.map { col ->
            val byStation = names.map { merged.getValue(it)[col] }
            if (direction == Direction.BATAJNICA_OVCA) byStation else byStation.reversed()
        }
        return DirectedTable(this, pageIndex, direction, stations, columns)
    }

    private fun DirectedTable.toTrains(route: List<Station>, probe: ShadingProbe?): List<Train> {
        val index = route.withIndex().associate { (i, s) -> s.id to i }
        return raw.numbers.indices.mapNotNull { col ->
            val times = arrayOfNulls<Int>(route.size)
            var any = false
            stations.forEachIndexed { i, station ->
                val t = columns[col][i] ?: return@forEachIndexed
                val at = index[station.id] ?: return@forEachIndexed
                times[at] = t
                any = true
            }
            if (!any) return@mapNotNull null
            Train(
                number = raw.numbers[col],
                direction = direction,
                weekdayOnly = isShaded(probe, pageIndex, raw, col),
                times = times.toList(),
                route = route,
            )
        }
    }

    // ------------------------------------------------- boja kolone (vikend)

    /**
     * Kolona vozova koji ne saobraćaju vikendom je u PDF-u obojena (zeleno).
     * Uzima se najčešća boja pozadine kolone, van teksta.
     */
    private fun isShaded(probe: ShadingProbe?, pageIndex: Int, table: RawTable, col: Int): Boolean {
        if (probe == null) return false
        val cx = table.centers[col]
        val dx = table.columnWidth * 0.38f
        val counts = HashMap<Int, Int>()
        var y = table.bodyTop
        while (y <= table.bodyBottom) {
            for (x in listOf(cx - dx, cx + dx)) {
                probe.sample(pageIndex, x, y)?.let { counts[it] = (counts[it] ?: 0) + 1 }
            }
            y += 3f
        }
        val modal = counts.maxByOrNull { it.value }?.key ?: return false
        val r = (modal shr 16) and 0xFF
        val g = (modal shr 8) and 0xFF
        val b = modal and 0xFF
        return g - r > 20 && g - b > 20
    }

    // -------------------------------------------------------------- napomene

    /** Iz napomene „Важи од 09.09.2026. до 12.12.2026. године". */
    private fun validity(lines: List<TextLine>): Pair<LocalDate, LocalDate>? {
        val line = lines.firstOrNull { SerbianText.toLatin(it.text).contains("Važi od", ignoreCase = true) }
            ?: return null
        val dates = FULL_DATE.findAll(line.text).mapNotNull { toDate(it) }.toList()
        return if (dates.size >= 2) dates[0] to dates[1] else null
    }

    private fun toDate(match: MatchResult): LocalDate? {
        val (day, month, year) = match.destructured
        return try {
            LocalDate.of(year.toInt(), month.toInt(), day.toInt())
        } catch (e: DateTimeException) {
            null
        }
    }

    /**
     * Iz napomene „дан државног празника * 1.,2. и 7.01.2026., 15.,16. и 17.02.2026., …"
     * – goli dani važe za mesec/godinu prve pune datuma koja ih sledi.
     */
    private fun holidays(lines: List<TextLine>): Set<Triple<Int, Int, Int>> {
        val out = mutableSetOf<Triple<Int, Int, Int>>()
        for (line in lines) {
            val latin = SerbianText.toLatin(line.text)
            if (!latin.contains("praznik", ignoreCase = true)) continue
            if (latin.contains("Važi", ignoreCase = true)) continue
            val pending = mutableListOf<Int>()
            for (m in DAY_OR_DATE.findAll(line.text)) {
                val full = FULL_DATE.matchEntire(m.value.trimEnd('.'))
                    ?: FULL_DATE.find(m.value)
                if (full != null) {
                    val (d, mo, y) = full.destructured
                    pending.forEach { out += Triple(y.toInt(), mo.toInt(), it) }
                    pending.clear()
                    out += Triple(y.toInt(), mo.toInt(), d.toInt())
                } else {
                    m.value.trimEnd('.').toIntOrNull()?.let { pending += it }
                }
            }
        }
        return out
    }

    private fun toMinutes(text: String): Int? {
        val m = TIME.matchEntire(text) ?: return null
        val h = m.groupValues[1].toInt()
        val min = m.groupValues[2].toInt()
        if (h > 23 || min > 59) return null
        return h * 60 + min
    }
}
