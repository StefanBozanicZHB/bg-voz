package zhb.company.bgvoz

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import zhb.company.bgvoz.data.BundledTimetable
import zhb.company.bgvoz.data.TimetableData
import zhb.company.bgvoz.data.TimetableRepository
import zhb.company.bgvoz.data.pdf.PdfParseException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

enum class SyncStatus { LOADING, FRESH, STALE }

data class TimetableUiState(
    val data: TimetableData,
    val status: SyncStatus,
    val error: String? = null,
)

private fun describe(failure: Throwable): String = when (failure) {
    is UnknownHostException -> "nema internet veze"
    is SocketTimeoutException -> "sajt se ne odaziva"
    is PdfParseException -> "PDF je u neočekivanom obliku: ${failure.message}"
    is IOException -> failure.message ?: "greška u vezi sa sajtom"
    else -> failure.message ?: failure::class.java.simpleName
}

class TimetableViewModel(application: Application) : AndroidViewModel(application) {

    var state by mutableStateOf(
        TimetableUiState(BundledTimetable.data, SyncStatus.LOADING),
    )
        private set

    init {
        refresh()
    }

    /** Uvek ide na mrežu; keš i ugrađena kopija su samo rezerva. */
    fun refresh() {
        state = state.copy(status = SyncStatus.LOADING, error = null)
        viewModelScope.launch {
            val context = getApplication<Application>()
            runCatching { TimetableRepository.fetchFresh(context) }
                .onSuccess { state = TimetableUiState(it, SyncStatus.FRESH) }
                .onFailure { failure ->
                    val fallback = runCatching { TimetableRepository.cached(context) }.getOrNull()
                        ?: BundledTimetable.data
                    state = TimetableUiState(
                        data = fallback,
                        status = SyncStatus.STALE,
                        error = describe(failure),
                    )
                }
        }
    }
}
