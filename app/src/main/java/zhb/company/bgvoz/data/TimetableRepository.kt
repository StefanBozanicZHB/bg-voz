package zhb.company.bgvoz.data

import android.content.Context
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.tom_roush.pdfbox.pdmodel.PDDocument
import zhb.company.bgvoz.data.pdf.AndroidShadingProbe
import zhb.company.bgvoz.data.pdf.PdfBoxTextReader
import zhb.company.bgvoz.data.pdf.PdfTimetableParser
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Red vožnje se pri svakom pokretanju povlači sa sajta Srbija Voza i parsira iz PDF-a.
 *
 * Redosled: sveže preuzet PDF → poslednji uspešno preuzet PDF iz keša → ugrađena kopija.
 */
object TimetableRepository {

    const val PDF_URL = "https://www.srbvoz.rs/wp-content/redvoznje/rv_bg_voza_za_2025.pdf"
    private const val CACHE_NAME = "red_voznje.pdf"
    private const val TIMEOUT_MS = 20_000

    private var initialized = false

    /**
     * Poslednji uspešno isparsiran red vožnje. Vidžet se osvežava u istom procesu
     * kao i aplikacija, pa mu ovo često uštedi ponovno parsiranje PDF-a.
     */
    @Volatile
    private var inMemory: TimetableData? = null

    /** Preuzima PDF i parsira ga. Baca izuzetak ako mreža ili PDF ne valjaju. */
    suspend fun fetchFresh(context: Context): TimetableData = withContext(Dispatchers.IO) {
        ensureInit(context)
        val temp = File(context.cacheDir, "$CACHE_NAME.tmp")
        download(PDF_URL, temp)
        val data = parse(temp, DataOrigin.NETWORK, System.currentTimeMillis())
        inMemory = data
        // tek kad je parsiranje prošlo, PDF postaje novi keš
        val cache = File(context.cacheDir, CACHE_NAME)
        temp.copyTo(cache, overwrite = true)
        temp.delete()
        data
    }

    /** Poslednji uspešno preuzet PDF, ako ga ima. */
    suspend fun cached(context: Context): TimetableData? = withContext(Dispatchers.IO) {
        ensureInit(context)
        val cache = File(context.cacheDir, CACHE_NAME)
        if (!cache.exists()) return@withContext null
        runCatching { parse(cache, DataOrigin.CACHE, cache.lastModified()) }
            .getOrNull()
            ?.also { inMemory = it }
    }

    /**
     * Podaci za vidžet: bez odlaska na mrežu. Prvo ono što je već u memoriji,
     * pa poslednji sačuvani PDF, pa ugrađena kopija – vidžet uvek nešto pokaže.
     */
    suspend fun forWidget(context: Context): TimetableData =
        inMemory ?: cached(context) ?: BundledTimetable.data

    private fun ensureInit(context: Context) {
        if (!initialized) {
            PDFBoxResourceLoader.init(context.applicationContext)
            initialized = true
        }
    }

    private fun parse(file: File, origin: DataOrigin, fetchedAt: Long): TimetableData =
        PDDocument.load(file).use { doc ->
            val pages = PdfBoxTextReader.read(doc)
            AndroidShadingProbe(file, PdfBoxTextReader.pageSizes(doc)).use { probe ->
                PdfTimetableParser.parse(pages, probe, origin, fetchedAt)
            }
        }

    private fun download(url: String, target: File) {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "BgVoz/1.0 (Android)")
            setRequestProperty("Accept", "application/pdf,*/*")
        }
        try {
            val code = connection.responseCode
            if (code !in 200..299) error("Server je vratio HTTP $code")
            target.outputStream().use { out -> connection.inputStream.use { it.copyTo(out) } }
            if (target.length() < 1024) error("Preuzeta datoteka je prazna ili neispravna")
        } finally {
            connection.disconnect()
        }
    }
}
