package zhb.company.bgvoz.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidgetManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * Sistem vidžet osvežava najviše na pola sata, što je premalo za odbrojavanje do voza.
 * Zato se posle svakog iscrtavanja zakazuje buđenje za trenutak kada prvi prikazani voz
 * kreće – tada voz spada sa liste, a odbrojavanje se preračuna.
 */
object WidgetRefresh {

    private const val REQUEST_CODE = 4201

    /**
     * Iscrtava sve postavljene vidžete.
     *
     * Namerno se ne koristi `updateAll()`: on ide kroz Glance-ov sopstveni registar,
     * a vidžet sa ekranom za podešavanje u njega nikad ne uđe – za takve vidžete sistem
     * ne šalje početni APPWIDGET_UPDATE. Sistemski spisak instanci je jedini pouzdan.
     */
    suspend fun refreshNow(context: Context) {
        val component = ComponentName(context, NextTrainWidgetReceiver::class.java)
        val ids = AppWidgetManager.getInstance(context).getAppWidgetIds(component)
        if (ids.isEmpty()) return

        val manager = GlanceAppWidgetManager(context)
        val widget = NextTrainWidget()
        for (id in ids) {
            runCatching { widget.update(context, manager.getGlanceIdBy(id)) }
        }
    }

    fun scheduleAt(context: Context, time: LocalDateTime, windowMillis: Long = 60_000L) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val trigger = time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        // neprecizan alarm – ne treba dozvola, a minut-dva razlike ovde ništa ne menja
        alarmManager.setWindow(AlarmManager.RTC, trigger, windowMillis, pendingIntent(context))
    }

    fun cancel(context: Context) {
        context.getSystemService(AlarmManager::class.java)?.cancel(pendingIntent(context))
    }

    /** Sledeći trenutak vredan buđenja: polazak prvog voza, ili ponoć ako ga nema. */
    fun nextWakeUp(today: LocalDate, departureMinute: Int?): LocalDateTime =
        if (departureMinute == null) {
            today.plusDays(1).atStartOfDay().plusMinutes(1)
        } else {
            today.atTime(LocalTime.of(departureMinute / 60 % 24, departureMinute % 60)).plusSeconds(30)
        }

    private fun pendingIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        REQUEST_CODE,
        Intent(context, WidgetRefreshReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}

/** Budi se na zakazani alarm i ponovo iscrtava sve vidžete. */
class WidgetRefreshReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                WidgetRefresh.refreshNow(context)
            } finally {
                pending.finish()
            }
        }
    }
}
