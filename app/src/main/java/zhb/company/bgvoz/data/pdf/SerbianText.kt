package zhb.company.bgvoz.data.pdf

/** Preslovljavanje ćirilice i normalizacija naziva stanica iz PDF-a. */
internal object SerbianText {

    private val MAP: Map<Char, String> = buildMap {
        val upper = "АБВГДЂЕЖЗИЈКЛЉМНЊОПРСТЋУФХЦЧЏШ"
        val upperLat = listOf(
            "A", "B", "V", "G", "D", "Đ", "E", "Ž", "Z", "I", "J", "K", "L", "Lj",
            "M", "N", "Nj", "O", "P", "R", "S", "T", "Ć", "U", "F", "H", "C", "Č", "Dž", "Š",
        )
        upper.forEachIndexed { i, c ->
            put(c, upperLat[i])
            put(c.lowercaseChar(), upperLat[i].lowercase())
        }
    }

    fun toLatin(s: String): String = buildString {
        for (c in s) append(MAP[c] ?: c.toString())
    }

    /** Sufiksi koji opisuju tip stajališta – ne nose ime stanice. */
    private val DROP = setOf("stajaliste", "staj", "stajalište", "stajališta")

    /**
     * „Камендин стајалиште" → „Kamendin", „Крњача укр." → „Krnjača Ukrsnica".
     * Vraća `null` za redove koji nisu naziv stanice.
     */
    fun stationName(raw: String): String? {
        val latin = toLatin(raw).trim()
        if (latin.isEmpty()) return null
        if (latin.none { it.isLetter() }) return null
        val parts = latin.split(' ', ' ').filter { it.isNotBlank() }
        val kept = mutableListOf<String>()
        for (p in parts) {
            val bare = p.trimEnd('.', ',', ':').lowercase()
            when {
                bare in DROP -> Unit
                bare == "ukr" || bare == "ukrsnica" -> kept += "Ukrsnica"
                else -> kept += p.trimEnd(',')
            }
        }
        if (kept.isEmpty()) return null
        return kept.joinToString(" ") { w ->
            if (w.length <= 1) w.uppercase()
            else w[0].uppercaseChar() + w.substring(1).lowercase()
        }
    }

    private val FOLD = mapOf('Đ' to "DJ", 'Ž' to "Z", 'Ć' to "C", 'Č' to "C", 'Š' to "S")

    /** Stabilan identifikator stanice, bez dijakritika. */
    fun stationId(name: String): String = buildString {
        for (c in name.uppercase()) {
            val f = FOLD[c]
            when {
                f != null -> append(f)
                c.isLetterOrDigit() -> append(c)
                else -> append('_')
            }
        }
    }
}
