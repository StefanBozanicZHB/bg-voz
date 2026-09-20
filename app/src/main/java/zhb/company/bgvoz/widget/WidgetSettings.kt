package zhb.company.bgvoz.widget

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import zhb.company.bgvoz.data.Direction

/**
 * Šta jedan vidžet prikazuje. Čuva se po instanci vidžeta (Glance state),
 * a bira se pri dodavanju na početni ekran ([WidgetConfigActivity]).
 */
data class WidgetSettings(val stationId: String, val direction: Direction) {

    companion object {
        val STATION_ID: Preferences.Key<String> = stringPreferencesKey("station_id")
        val DIRECTION: Preferences.Key<String> = stringPreferencesKey("direction")

        fun from(prefs: Preferences): WidgetSettings? {
            val station = prefs[STATION_ID] ?: return null
            val direction = prefs[DIRECTION]
                ?.let { name -> Direction.entries.firstOrNull { it.name == name } }
                ?: return null
            return WidgetSettings(station, direction)
        }
    }
}
