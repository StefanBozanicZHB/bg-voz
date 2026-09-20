package zhb.company.bgvoz.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.material3.ColorProviders
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.glance.action.actionStartActivity
import zhb.company.bgvoz.MainActivity
import zhb.company.bgvoz.data.TimetableData
import zhb.company.bgvoz.data.TimetableRepository
import zhb.company.bgvoz.data.UpcomingCall
import zhb.company.bgvoz.data.Validity
import zhb.company.bgvoz.data.formatTime
import zhb.company.bgvoz.data.minuteOfDay
import zhb.company.bgvoz.ui.theme.DarkColors
import zhb.company.bgvoz.ui.theme.LightColors
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.LocalTime

private const val VISIBLE_DEPARTURES = 3

/** Vidžet sa sledećim vozovima sa jedne stanice. */
class NextTrainWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // Red vožnje ne zavisi od podešavanja vidžeta, pa se učitava ovde – jednom po sesiji.
        val data = TimetableRepository.forWidget(context)
        provideContent { WidgetContent(data) }
    }
}

/**
 * Sve što zavisi od podešavanja mora da se računa ovde, a ne u `provideGlance`.
 * Glance `provideGlance` poziva jednom po sesiji, a kasnija osvežavanja samo ponovo
 * izvršavaju ovu funkciju – vrednosti izračunate napolju bi ostale zamrznute.
 */
@Composable
private fun WidgetContent(data: TimetableData) {
    val context = LocalContext.current
    val settings = WidgetSettings.from(currentState())
    val today = LocalDate.now()
    val nowMinutes = LocalTime.now().minuteOfDay()
    val stationIndex = settings
        ?.let { s -> data.route.indexOfFirst { it.id == s.stationId } }
        ?: -1
    val upcoming = if (settings == null || stationIndex < 0) {
        emptyList()
    } else {
        data.upcoming(stationIndex, settings.direction, today, nowMinutes, VISIBLE_DEPARTURES)
    }

    // probudi se kad prvi prikazani voz krene, da spadne sa liste i da se
    // odbrojavanje preračuna; ponovo se zakazuje pri svakom iscrtavanju
    SideEffect {
        val prviDanas = upcoming.firstOrNull()?.takeIf { !it.isTomorrow }?.call?.minuteOfDay
        WidgetRefresh.scheduleAt(context, WidgetRefresh.nextWakeUp(today, prviDanas))
    }
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.widgetBackground)
            .cornerRadius(16.dp)
            .padding(12.dp)
            .clickable(actionStartActivity<MainActivity>()),
    ) {
        if (settings == null || stationIndex < 0) {
            Text(
                "Dodirni da podesiš stanicu",
                style = TextStyle(color = GlanceTheme.colors.onSurface),
            )
            return@Column
        }

        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                data.route[stationIndex].name,
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontWeight = FontWeight.Bold,
                ),
                maxLines = 1,
            )
            Spacer(GlanceModifier.width(6.dp))
            Text(
                if (settings.direction == zhb.company.bgvoz.data.Direction.BATAJNICA_OVCA) "→ Ovča"
                else "→ Batajnica",
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant),
                maxLines = 1,
            )
        }

        if (data.validityOn(today) == Validity.EXPIRED) {
            Text(
                "red vožnje istekao",
                style = TextStyle(color = GlanceTheme.colors.error, fontWeight = FontWeight.Bold),
                maxLines = 1,
            )
        }

        Spacer(GlanceModifier.height(6.dp))

        if (upcoming.isEmpty()) {
            Text(
                "Nema polazaka",
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant),
            )
            return@Column
        }

        upcoming.forEachIndexed { index, item ->
            DepartureLine(item, nowMinutes, isNext = index == 0)
            if (index < upcoming.lastIndex) Spacer(GlanceModifier.height(4.dp))
        }
    }
}

@Composable
private fun DepartureLine(item: UpcomingCall, nowMinutes: Int, isNext: Boolean) {
    val call = item.call
    val primary: ColorProvider = GlanceTheme.colors.primary
    val onSurface = GlanceTheme.colors.onSurface
    val muted = GlanceTheme.colors.onSurfaceVariant

    Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            formatTime(call.minuteOfDay),
            style = TextStyle(
                color = if (isNext) primary else onSurface,
                fontWeight = if (isNext) FontWeight.Bold else FontWeight.Normal,
            ),
            maxLines = 1,
        )
        Spacer(GlanceModifier.width(8.dp))
        Text(
            when {
                item.isTomorrow -> "sutra"
                isNext -> countdown(call.minuteOfDay - nowMinutes)
                else -> ""
            },
            style = TextStyle(color = if (isNext) primary else muted),
            maxLines = 1,
        )
        Spacer(GlanceModifier.width(8.dp))
        Text(
            if (call.train.isShortTurn) "samo do: ${call.train.terminus.name}" else "",
            style = TextStyle(color = GlanceTheme.colors.error, fontWeight = FontWeight.Bold),
            maxLines = 1,
        )
    }
}

private fun countdown(minutes: Int): String = when {
    minutes <= 0 -> "sada"
    minutes < 60 -> "za $minutes min"
    else -> "za ${minutes / 60}h ${minutes % 60}min"
}

/** Sistemski ulaz u vidžet. */
class NextTrainWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NextTrainWidget()

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        WidgetRefresh.cancel(context) // poslednji vidžet uklonjen – nema šta da se budi
    }
}
