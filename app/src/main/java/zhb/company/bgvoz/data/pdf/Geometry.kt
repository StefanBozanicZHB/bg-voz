package zhb.company.bgvoz.data.pdf

/** Jedna reč iz PDF-a, u „vizuelnom" koordinatnom sistemu strane (tačke, y raste nadole). */
data class Word(val x0: Float, val x1: Float, val y: Float, val text: String) {
    val centerX: Float get() = (x0 + x1) / 2f
}

/** Red teksta – reči sa približno istom y koordinatom. */
data class TextLine(val y: Float, val words: List<Word>) {
    val text: String get() = words.joinToString(" ") { it.text }
}

/**
 * Čita boju pozadine strane na zadatoj tački (iste koordinate kao [Word]).
 *
 * Potrebno je zato što PDF red vožnje vozove koji ne saobraćaju vikendom
 * označava isključivo bojom kolone, bez ikakvog teksta.
 */
fun interface ShadingProbe {
    /** ARGB boja, ili `null` ako se tačka ne može očitati. */
    fun sample(pageIndex: Int, x: Float, y: Float): Int?
}

/** Spaja pojedinačne glifove u reči i redove. Ista logika i na Androidu i u testovima. */
object LineBuilder {

    private const val LINE_TOLERANCE = 4f
    private const val WORD_GAP = 2.0f

    fun build(glyphs: List<Word>): List<TextLine> {
        val rows = mutableListOf<MutableList<Word>>()
        for (g in glyphs.sortedBy { it.y }) {
            val last = rows.lastOrNull()
            if (last != null && kotlin.math.abs(last.first().y - g.y) <= LINE_TOLERANCE) {
                last += g
            } else {
                rows += mutableListOf(g)
            }
        }
        return rows.map { row ->
            val words = mutableListOf<Word>()
            var current: Word? = null
            for (g in row.sortedBy { it.x0 }) {
                val c = current
                current = if (c != null && g.x0 - c.x1 <= WORD_GAP) {
                    c.copy(x1 = maxOf(c.x1, g.x1), text = c.text + g.text)
                } else {
                    if (c != null) words += c
                    g
                }
            }
            current?.let { words += it }
            TextLine(y = row.minOf { it.y }, words = words)
        }
    }
}
