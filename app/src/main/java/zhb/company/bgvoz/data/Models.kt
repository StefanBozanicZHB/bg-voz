package zhb.company.bgvoz.data

import java.time.LocalDate
import java.time.LocalTime

/** Smer vožnje na relaciji Batajnica – Ovča. */
enum class Direction(val title: String) {
    BATAJNICA_OVCA("Batajnica → Ovča"),
    OVCA_BATAJNICA("Ovča → Batajnica");

    val opposite: Direction
        get() = if (this == BATAJNICA_OVCA) OVCA_BATAJNICA else BATAJNICA_OVCA
}

/**
 * Stanica na relaciji. [stops] je `false` za stanice kroz koje voz samo prolazi –
 * u PDF-u je u toj koloni „│" umesto vremena (trenutno: Novi Beograd).
 */
data class Station(val id: String, val name: String, val stops: Boolean = true)

/**
 * Jedan polazak iz reda vožnje.
 *
 * [times] je poravnato sa [route] (uvek redosled Batajnica → Ovča), bez obzira na smer;
 * `null` znači da voz na toj stanici ne staje ili uopšte ne ide do nje.
 * Vrednosti su minuti od ponoći.
 */
data class Train(
    val number: String,
    val direction: Direction,
    val weekdayOnly: Boolean,
    val times: List<Int?>,
    val route: List<Station>,
) {
    /** Indeksi stanica na kojima voz staje, poređani onako kako ih voz stvarno prolazi. */
    val stopIndices: List<Int> = when (direction) {
        Direction.BATAJNICA_OVCA -> times.indices.filter { times[it] != null }
        Direction.OVCA_BATAJNICA -> times.indices.reversed().filter { times[it] != null }
    }

    val originIndex: Int get() = stopIndices.first()
    val terminusIndex: Int get() = stopIndices.last()

    val origin: Station get() = route[originIndex]
    val terminus: Station get() = route[terminusIndex]

    /** Krajnja stanica koju bi voz imao da ide celom relacijom. */
    private val plannedTerminusIndex: Int
        get() = if (direction == Direction.BATAJNICA_OVCA) route.lastIndex else 0

    /** `true` za vozove koji se okreću ranije (npr. samo do Zemuna). */
    val isShortTurn: Boolean get() = terminusIndex != plannedTerminusIndex

    /** Da li voz saobraća na dan sa datim režimom. */
    fun runsOn(weekendOrHoliday: Boolean): Boolean = !(weekdayOnly && weekendOrHoliday)
}

/** Odakle su podaci koje trenutno gledamo. */
enum class DataOrigin { NETWORK, CACHE, BUNDLED }

/** Da li je red vožnje uopšte na snazi za dan koji gledamo. */
enum class Validity {
    /** Datum je unutar perioda važenja iz PDF-a. */
    VALID,

    /** Red vožnje je istekao – vremena više ne moraju da važe. */
    EXPIRED,

    /** Red vožnje tek stupa na snagu. */
    NOT_YET_VALID,

    /** Period važenja nije pročitan iz PDF-a, pa se istek ne može proveriti. */
    UNKNOWN,
}

/** Kompletan red vožnje za relaciju Batajnica – Ovča. */
data class TimetableData(
    val route: List<Station>,
    val trains: List<Train>,
    val validFrom: LocalDate?,
    val validTo: LocalDate?,
    /** Dani državnog praznika iz napomene u PDF-u: (godina, mesec, dan). */
    val holidays: Set<Triple<Int, Int, Int>>,
    val origin: DataOrigin,
    /** Kada je PDF preuzet/pročitan, u epoch millis. */
    val fetchedAt: Long?,
) {
    /**
     * PDF nosi period važenja („Важи од … до …"). Ako ga pređemo, vremena su i dalje
     * ona stara – preuzimanje i parsiranje su uspeli, ali podaci više ne važe.
     */
    fun validityOn(date: LocalDate): Validity = when {
        validFrom == null || validTo == null -> Validity.UNKNOWN
        date.isAfter(validTo) -> Validity.EXPIRED
        date.isBefore(validFrom) -> Validity.NOT_YET_VALID
        else -> Validity.VALID
    }

    fun isWeekendOrHoliday(date: LocalDate): Boolean =
        date.dayOfWeek.value >= 6 || Triple(date.year, date.monthValue, date.dayOfMonth) in holidays

    /**
     * Sva vremena kad je voz na stanici [stationIndex] u datom smeru, sortirano.
     * Na krajnjoj stanici to su dolasci ([StationCall.isArrival]), inače polasci.
     * Vozovi koji tu ne staju – ili se okrenu pre te stanice – se ne prikazuju.
     */
    fun callsAt(
        stationIndex: Int,
        direction: Direction,
        weekendOrHoliday: Boolean,
    ): List<StationCall> = trains
        .asSequence()
        .filter { it.direction == direction }
        .filter { it.runsOn(weekendOrHoliday) }
        .filter { it.times[stationIndex] != null }
        .map { StationCall(it, stationIndex, it.times[stationIndex]!!) }
        .sortedBy { it.minuteOfDay }
        .toList()
}

/** Koji dan gledamo – današnji ili „tipski" radni dan / vikend. */
enum class DayFilter(val label: String) {
    TODAY("Danas"),
    WEEKDAY("Radnim danom"),
    WEEKEND("Vikend / praznik"),
}

/** Jedno stajanje voza na konkretnoj stanici – polazak, ili dolazak ako je stanica krajnja. */
data class StationCall(
    val train: Train,
    val stationIndex: Int,
    val minuteOfDay: Int,
) {
    /** Voz se ovde okreće – dalje ne ide. */
    val isArrival: Boolean get() = stationIndex == train.terminusIndex

    val arrivalAtTerminus: Int get() = train.times[train.terminusIndex]!!
    val rideMinutes: Int get() = arrivalAtTerminus - minuteOfDay
}

fun formatDate(date: LocalDate): String =
    "%02d.%02d.%04d.".format(date.dayOfMonth, date.monthValue, date.year)

fun formatTime(minuteOfDay: Int): String =
    "%02d:%02d".format(minuteOfDay / 60 % 24, minuteOfDay % 60)

fun LocalTime.minuteOfDay(): Int = hour * 60 + minute
