package zhb.company.bgvoz.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import zhb.company.bgvoz.data.Direction
import zhb.company.bgvoz.data.Station
import zhb.company.bgvoz.data.TimetableData
import zhb.company.bgvoz.data.TimetableRepository
import zhb.company.bgvoz.ui.theme.BgVozTheme
import java.time.LocalDateTime

/**
 * Bira se pri dodavanju vidžeta na početni ekran: koja stanica i koji smer.
 * Izbor se čuva uz samu instancu vidžeta, pa ih može biti više sa različitim stanicama.
 */
class WidgetConfigActivity : ComponentActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        // ako korisnik odustane, vidžet ne sme da ostane dodat
        setResult(Activity.RESULT_CANCELED, resultIntent())
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setContent {
            BgVozTheme {
                var data by remember { mutableStateOf<TimetableData?>(null) }
                LaunchedEffect(Unit) { data = TimetableRepository.forWidget(this@WidgetConfigActivity) }

                Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
                    val loaded = data
                    if (loaded == null) {
                        Text("Učitavanje reda vožnje…", Modifier.padding(padding).padding(16.dp))
                    } else {
                        ConfigScreen(
                            data = loaded,
                            onConfirm = ::save,
                            modifier = Modifier.padding(padding),
                        )
                    }
                }
            }
        }
    }

    private fun save(station: Station, direction: Direction) {
        lifecycleScope.launch {
            val glanceId = GlanceAppWidgetManager(this@WidgetConfigActivity).getGlanceIdBy(appWidgetId)
            updateAppWidgetState(this@WidgetConfigActivity, glanceId) { prefs ->
                prefs[WidgetSettings.STATION_ID] = station.id
                prefs[WidgetSettings.DIRECTION] = direction.name
            }
            // ciljano, baš ovaj vidžet – updateAll ume da promaši instancu
            // koju domaćin još nije do kraja registrovao
            // Vidžet je vezan tek kad ova aktivnost vrati rezultat – do tada ga
            // AppWidgetManager ne poznaje i svako iscrtavanje odavde propada.
            // Zato se prvo iscrtavanje zakazuje za par sekundi kasnije.
            WidgetRefresh.scheduleAt(
                this@WidgetConfigActivity,
                LocalDateTime.now().plusSeconds(2),
                windowMillis = 2_000L,
            )
            setResult(Activity.RESULT_OK, resultIntent())
            finish()
        }
    }

    private fun resultIntent() = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
}

@Composable
private fun ConfigScreen(
    data: TimetableData,
    onConfirm: (Station, Direction) -> Unit,
    modifier: Modifier = Modifier,
) {
    var direction by remember { mutableStateOf(Direction.BATAJNICA_OVCA) }
    var stationId by remember { mutableStateOf(data.route.first { it.stops }.id) }

    Column(modifier.fillMaxSize().padding(16.dp)) {
        Text(
            "Vidžet: sledeći vozovi",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Direction.entries.forEach { dir ->
                FilterChip(
                    selected = direction == dir,
                    onClick = { direction = dir },
                    label = { Text(dir.title, maxLines = 1) },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        Text("Stanica", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(4.dp))

        val stations = when (direction) {
            Direction.BATAJNICA_OVCA -> data.route
            Direction.OVCA_BATAJNICA -> data.route.reversed()
        }.filter { it.stops }

        LazyColumn(Modifier.weight(1f)) {
            items(stations, key = { it.id }) { station ->
                FilterChip(
                    selected = stationId == station.id,
                    onClick = { stationId = station.id },
                    label = { Text(station.name) },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        Button(
            onClick = { onConfirm(data.route.first { it.id == stationId }, direction) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Dodaj vidžet")
        }
    }
}
