package zhb.company.bgvoz.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import zhb.company.bgvoz.SyncStatus
import zhb.company.bgvoz.TimetableUiState
import zhb.company.bgvoz.data.DataOrigin
import zhb.company.bgvoz.data.DayFilter
import zhb.company.bgvoz.data.Direction
import zhb.company.bgvoz.data.Station
import zhb.company.bgvoz.data.StationCall
import zhb.company.bgvoz.data.TimetableData
import zhb.company.bgvoz.data.formatTime
import zhb.company.bgvoz.data.minuteOfDay
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

private val DAY_NAMES = listOf("pon", "uto", "sre", "čet", "pet", "sub", "ned")

/** Stanice koje se najčešće biraju – prečice ispod padajuće liste. */
private val QUICK_STATION_IDS = listOf("PANCEVACKI_MOST", "NOVI_BEOGRAD", "BATAJNICA", "OVCA")

@Composable
fun DeparturesScreen(
    state: TimetableUiState,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val data = state.data
    var direction by remember { mutableStateOf(Direction.BATAJNICA_OVCA) }
    var stationId by remember { mutableStateOf(data.route.first().id) }
    var dayFilter by remember { mutableStateOf(DayFilter.TODAY) }

    // stanica se pamti po oznaci – preživi osvežavanje reda vožnje
    val stationIndex = data.route.indexOfFirst { it.id == stationId }.takeIf { it >= 0 } ?: 0
    val station = data.route[stationIndex]

    // sat koji kuca, da odbrojavanje i „sledeći voz" uvek budu tačni
    var now by remember { mutableStateOf(LocalTime.now()) }
    val today = remember { LocalDate.now() }
    LaunchedEffect(Unit) {
        while (true) {
            delay(10_000)
            now = LocalTime.now()
        }
    }

    val todayIsWeekend = data.isWeekendOrHoliday(today)
    val weekendOrHoliday = when (dayFilter) {
        DayFilter.TODAY -> todayIsWeekend
        DayFilter.WEEKDAY -> false
        DayFilter.WEEKEND -> true
    }

    val nowMinutes = now.minuteOfDay()
    val calls = remember(data, stationIndex, direction, weekendOrHoliday) {
        data.callsAt(stationIndex, direction, weekendOrHoliday)
    }
    val nextIndex = calls.indexOfFirst { it.minuteOfDay >= nowMinutes }

    val listState = rememberLazyListState()
    // prvi sledeći voz ide na vrh, sa jednim prošlim iznad njega za kontekst
    LaunchedEffect(data, stationIndex, direction, weekendOrHoliday) {
        listState.scrollToItem(if (nextIndex > 0) nextIndex - 1 else 0)
    }

    Column(modifier.fillMaxSize()) {
        Header(
            data = data,
            direction = direction,
            onDirectionChange = { direction = it },
            stationIndex = stationIndex,
            onStationChange = { stationId = data.route[it].id },
            dayFilter = dayFilter,
            onDayFilterChange = { dayFilter = it },
            today = today,
            todayIsWeekend = todayIsWeekend,
            now = now,
            next = calls.getOrNull(nextIndex),
        )
        HorizontalDivider()
        when {
            !station.stops -> NoStopNotice(data, station, Modifier.weight(1f))
            calls.isEmpty() -> EmptyState(station, direction, Modifier.weight(1f))
            else -> LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                itemsIndexed(calls, key = { _, c -> c.train.number }) { index, call ->
                    CallCard(
                        call = call,
                        isNext = index == nextIndex,
                        isPast = call.minuteOfDay < nowMinutes,
                        nowMinutes = nowMinutes,
                        showWeekdayBadge = dayFilter != DayFilter.TODAY,
                    )
                }
            }
        }
        SyncBar(state, onRefresh)
    }
}

@Composable
private fun Header(
    data: TimetableData,
    direction: Direction,
    onDirectionChange: (Direction) -> Unit,
    stationIndex: Int,
    onStationChange: (Int) -> Unit,
    dayFilter: DayFilter,
    onDayFilterChange: (DayFilter) -> Unit,
    today: LocalDate,
    todayIsWeekend: Boolean,
    now: LocalTime,
    next: StationCall?,
) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "BG:VOZ",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Batajnica – Ovča",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    formatTime(now.minuteOfDay()),
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Direction.entries.forEach { dir ->
                    FilterChip(
                        selected = direction == dir,
                        onClick = { onDirectionChange(dir) },
                        label = {
                            Text(dir.title, maxLines = 1, style = MaterialTheme.typography.labelLarge)
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            StationPicker(data, stationIndex, direction, onStationChange)

            val quick = QUICK_STATION_IDS.mapNotNull { id ->
                data.route.indexOfFirst { it.id == id }.takeIf { it >= 0 }
            }
            if (quick.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                QuickStations(data, quick, stationIndex, onStationChange)
            }

            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DayFilter.entries.forEach { filter ->
                    val label = if (filter == DayFilter.TODAY) {
                        "Danas (${DAY_NAMES[today.dayOfWeek.value - 1]})"
                    } else {
                        filter.label
                    }
                    FilterChip(
                        selected = dayFilter == filter,
                        onClick = { onDayFilterChange(filter) },
                        label = { Text(label, maxLines = 1, style = MaterialTheme.typography.labelMedium) },
                    )
                }
            }

            if (dayFilter == DayFilter.TODAY && todayIsWeekend) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "Vikend/praznik – polasci koji voze samo radnim danom su sakriveni.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(10.dp))
            NextSummary(next, now.minuteOfDay(), data.route[stationIndex])
        }
    }
}

/** Prečice do stanica koje se najčešće biraju; prelamaju se u više redova. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuickStations(
    data: TimetableData,
    quick: List<Int>,
    stationIndex: Int,
    onStationChange: (Int) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        quick.forEach { i ->
            FilterChip(
                selected = stationIndex == i,
                onClick = { onStationChange(i) },
                label = {
                    Text(
                        data.route[i].name,
                        maxLines = 1,
                        style = MaterialTheme.typography.labelMedium,
                    )
                },
            )
        }
    }
}

@Composable
private fun NextSummary(next: StationCall?, nowMinutes: Int, station: Station) {
    Surface(
        color = if (next != null) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (next == null) {
                Text(
                    if (!station.stops) "Voz ne staje na ovoj stanici."
                    else "Nema više vozova danas sa ove stanice.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                Column(Modifier.weight(1f)) {
                    Text(
                        if (next.isArrival) "SLEDEĆI DOLAZAK" else "SLEDEĆI POLAZAK",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        if (next.isArrival) "${formatTime(next.minuteOfDay)}  ←  ${next.train.origin.name}"
                        else "${formatTime(next.minuteOfDay)}  →  ${next.train.terminus.name}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                Text(
                    countdown(next.minuteOfDay - nowMinutes),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    textAlign = TextAlign.End,
                )
            }
        }
    }
}

@Composable
private fun StationPicker(
    data: TimetableData,
    stationIndex: Int,
    direction: Direction,
    onStationChange: (Int) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val order = when (direction) {
        Direction.BATAJNICA_OVCA -> data.route.indices.toList()
        Direction.OVCA_BATAJNICA -> data.route.indices.reversed().toList()
    }
    Box(Modifier.fillMaxWidth()) {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text("Stanica:  ", style = MaterialTheme.typography.bodyMedium)
            Text(
                data.route[stationIndex].name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.weight(1f))
            Text("▾")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            order.forEach { i ->
                val s = data.route[i]
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(
                                s.name,
                                fontWeight = if (i == stationIndex) FontWeight.Bold else FontWeight.Normal,
                            )
                            if (!s.stops) {
                                Text(
                                    "voz trenutno ne staje",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    },
                    onClick = {
                        onStationChange(i)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun CallCard(
    call: StationCall,
    isNext: Boolean,
    isPast: Boolean,
    nowMinutes: Int,
    showWeekdayBadge: Boolean,
) {
    var expanded by remember { mutableStateOf(false) }
    val scheme = MaterialTheme.colorScheme
    val container = when {
        isNext -> scheme.primaryContainer
        isPast -> scheme.surfaceContainerLowest
        else -> scheme.surfaceContainer
    }
    val alpha = if (isPast && !isNext) 0.45f else 1f

    Card(
        colors = CardDefaults.cardColors(containerColor = container),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isNext) 6.dp else 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isNext) Modifier.border(2.dp, scheme.primary, RoundedCornerShape(12.dp))
                else Modifier
            )
            .clickable { expanded = !expanded }
            .animateContentSize(),
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    formatTime(call.minuteOfDay),
                    style = MaterialTheme.typography.headlineSmall,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = if (isNext) FontWeight.Black else FontWeight.Bold,
                    color = scheme.onSurface.copy(alpha = alpha),
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        if (call.isArrival) "← ${call.train.origin.name}"
                        else "→ ${call.train.terminus.name}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = scheme.onSurface.copy(alpha = alpha),
                    )
                    Text(
                        if (call.isArrival) "voz ${call.train.number} · dolazak, ovde je krajnja"
                        else "voz ${call.train.number} · stiže ${formatTime(call.arrivalAtTerminus)}" +
                            " (${call.rideMinutes} min)",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant.copy(alpha = alpha),
                    )
                }
                if (isNext) {
                    Text(
                        countdown(call.minuteOfDay - nowMinutes),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = scheme.primary,
                    )
                }
            }

            val badges = buildList {
                if (call.train.isShortTurn) {
                    add(
                        Badge(
                            if (call.isArrival) "NE IDE DALJE · ovde se okreće"
                            else "NE IDE DO KRAJA · samo do: ${call.train.terminus.name}",
                            warning = true,
                        )
                    )
                }
                if (call.train.weekdayOnly && showWeekdayBadge) {
                    add(Badge("samo radnim danom", warning = false))
                }
            }
            if (badges.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    badges.forEach { BadgeChip(it, alpha) }
                }
            }

            AnimatedVisibility(expanded) { RouteDetail(call) }
        }
    }
}

private data class Badge(val text: String, val warning: Boolean)

@Composable
private fun BadgeChip(badge: Badge, alpha: Float) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        color = (if (badge.warning) scheme.errorContainer else scheme.secondaryContainer).copy(alpha = alpha),
        shape = RoundedCornerShape(6.dp),
    ) {
        Text(
            badge.text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = (if (badge.warning) scheme.onErrorContainer else scheme.onSecondaryContainer).copy(alpha = alpha),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun RouteDetail(call: StationCall) {
    val scheme = MaterialTheme.colorScheme
    val train = call.train
    val boardingPosition = train.stopIndices.indexOf(call.stationIndex)
    Column(Modifier.padding(top = 12.dp)) {
        HorizontalDivider()
        Spacer(Modifier.height(8.dp))
        train.stopIndices.forEachIndexed { position, i ->
            val isSelected = i == call.stationIndex
            val isTerminus = i == train.terminusIndex
            val before = position < boardingPosition
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 2.dp),
            ) {
                Box(
                    Modifier
                        .size(if (isSelected || isTerminus) 10.dp else 6.dp)
                        .background(
                            when {
                                isSelected -> scheme.primary
                                isTerminus -> scheme.error
                                before -> scheme.outlineVariant
                                else -> scheme.onSurfaceVariant
                            },
                            CircleShape,
                        ),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    formatTime(train.times[i]!!),
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (before) scheme.onSurfaceVariant.copy(alpha = 0.5f) else scheme.onSurface,
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    train.route[i].name + if (isTerminus) "  (krajnja)" else "",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (before) scheme.onSurfaceVariant.copy(alpha = 0.5f) else scheme.onSurface,
                )
            }
        }
        if (train.isShortTurn) {
            Spacer(Modifier.height(6.dp))
            Text(
                "Ovaj voz se okreće u ${train.terminus.name} i ne ide dalje ka " +
                    if (train.direction == Direction.BATAJNICA_OVCA) "Ovči." else "Batajnici.",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = scheme.error,
            )
        }
    }
}

@Composable
private fun NoStopNotice(data: TimetableData, station: Station, modifier: Modifier = Modifier) {
    val nearest = data.route
        .filter { it.stops }
        .let { stops ->
            val i = data.route.indexOf(station)
            listOfNotNull(
                data.route.take(i).lastOrNull { it.stops },
                data.route.drop(i + 1).firstOrNull { it.stops },
            ).ifEmpty { stops.take(2) }
        }
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.padding(20.dp),
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    "Stanica ${station.name} – vozovi trenutno ne staju",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "U važećem redu vožnje na srbvoz.rs za ovu stanicu u svakoj koloni stoji " +
                        "„│\" – voz prolazi bez zaustavljanja. Čim počnu da staju, vremena će se " +
                        "pojaviti ovde sama, jer se red vožnje preuzima pri svakom pokretanju.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (nearest.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Najbliža stajališta: " + nearest.joinToString(" i ") { it.name },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

@Composable
private fun SyncBar(state: TimetableUiState, onRefresh: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val stale = state.status == SyncStatus.STALE
    Surface(color = if (stale) scheme.errorContainer else scheme.surfaceContainerLow) {
        Column {
            if (state.status == SyncStatus.LOADING) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }
            Row(
                Modifier.padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    syncText(state),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (stale) scheme.onErrorContainer else scheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                if (state.status != SyncStatus.LOADING) {
                    TextButton(onClick = onRefresh) { Text("Osveži") }
                }
            }
        }
    }
}

private fun syncText(state: TimetableUiState): String {
    val data = state.data
    val validity = if (data.validFrom != null && data.validTo != null) {
        " · važi ${data.validFrom} – ${data.validTo}"
    } else {
        ""
    }
    return when (state.status) {
        SyncStatus.LOADING -> "Preuzimanje reda vožnje sa srbvoz.rs…"
        SyncStatus.FRESH -> "Red vožnje preuzet sa srbvoz.rs ${fetchedAt(data.fetchedAt)}$validity"
        SyncStatus.STALE -> {
            val source = when (data.origin) {
                DataOrigin.CACHE -> "poslednji sačuvani PDF ${fetchedAt(data.fetchedAt)}"
                else -> "ugrađena kopija reda vožnje"
            }
            "Preuzimanje nije uspelo (${state.error ?: "nepoznata greška"}). Prikazan je $source$validity"
        }
    }
}

private fun fetchedAt(millis: Long?): String {
    if (millis == null) return ""
    val t = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault())
    return "(%02d.%02d. u %02d:%02d)".format(t.dayOfMonth, t.monthValue, t.hour, t.minute)
}

@Composable
private fun EmptyState(station: Station, direction: Direction, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            "Nema vozova na stanici ${station.name} u smeru ${direction.title} za izabrani dan.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(32.dp),
        )
    }
}

private fun countdown(minutes: Int): String = when {
    minutes < 0 -> "prošao"
    minutes == 0 -> "sada"
    minutes < 60 -> "za $minutes min"
    else -> "za ${minutes / 60}h ${minutes % 60}min"
}
