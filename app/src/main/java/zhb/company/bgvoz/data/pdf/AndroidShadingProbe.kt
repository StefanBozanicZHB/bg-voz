package zhb.company.bgvoz.data.pdf

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import java.io.Closeable
import java.io.File
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Očitava boju pozadine PDF strane preko Androidovog [PdfRenderer]-a.
 *
 * [pageSizes] su „vizuelne" dimenzije strana (već uračunata rotacija) onako kako ih
 * vidi PDFBox, pa se koordinate reči i piksela poklapaju. Ako renderer stranu vrati
 * nerotiranu, koordinate se preračunavaju.
 */
class AndroidShadingProbe(
    file: File,
    private val pageSizes: List<Pair<Float, Float>>,
    private val scale: Float = 2f,
) : ShadingProbe, Closeable {

    private val fd: ParcelFileDescriptor =
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    private val renderer = PdfRenderer(fd)
    private val cache = HashMap<Int, Bitmap?>()
    private val rotated = HashMap<Int, Boolean>()

    override fun sample(pageIndex: Int, x: Float, y: Float): Int? {
        val bitmap = bitmap(pageIndex) ?: return null
        val (vw, vh) = pageSizes.getOrNull(pageIndex) ?: return null
        // renderer je stranu vratio u istoj orijentaciji kao PDFBox?
        val swapped = rotated[pageIndex] == true
        val px: Int
        val py: Int
        if (!swapped) {
            px = (x * scale).roundToInt()
            py = (y * scale).roundToInt()
        } else {
            // PDF /Rotate 90: vizuelno (x, y) → nerotirano (y, vw - x)
            px = (y * scale).roundToInt()
            py = ((vw - x) * scale).roundToInt()
        }
        if (px < 0 || py < 0 || px >= bitmap.width || py >= bitmap.height) return null
        @Suppress("DEPRECATION")
        return bitmap.getPixel(px, py)
    }

    private fun bitmap(pageIndex: Int): Bitmap? = cache.getOrPut(pageIndex) {
        if (pageIndex >= renderer.pageCount) return@getOrPut null
        renderer.openPage(pageIndex).use { page ->
            val (vw, vh) = pageSizes.getOrNull(pageIndex) ?: return@getOrPut null
            val matchesVisual = abs(page.width - vw) < 2f && abs(page.height - vh) < 2f
            rotated[pageIndex] = !matchesVisual
            val bmp = Bitmap.createBitmap(
                (page.width * scale).roundToInt().coerceAtLeast(1),
                (page.height * scale).roundToInt().coerceAtLeast(1),
                Bitmap.Config.ARGB_8888,
            )
            bmp.eraseColor(android.graphics.Color.WHITE)
            page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            bmp
        }
    }

    override fun close() {
        cache.values.forEach { it?.recycle() }
        cache.clear()
        runCatching { renderer.close() }
        runCatching { fd.close() }
    }
}
