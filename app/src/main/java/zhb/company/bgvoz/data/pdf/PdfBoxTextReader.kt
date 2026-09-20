package zhb.company.bgvoz.data.pdf

import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.text.TextPosition

/**
 * Vadi glifove sa njihovim pozicijama iz PDF-a (PdfBox-Android).
 *
 * `xDirAdj`/`yDirAdj` već uračunavaju rotaciju strane, pa su koordinate iste
 * kao ono što se vidi na ekranu – i poklapaju se sa [AndroidShadingProbe].
 */
object PdfBoxTextReader {

    fun read(document: PDDocument): List<List<TextLine>> =
        (0 until document.numberOfPages).map { pageIndex ->
            val glyphs = mutableListOf<Word>()
            val stripper = object : PDFTextStripper() {
                override fun processTextPosition(text: TextPosition) {
                    val s = text.unicode
                    if (!s.isNullOrBlank()) {
                        glyphs += Word(
                            x0 = text.xDirAdj,
                            x1 = text.xDirAdj + text.widthDirAdj,
                            y = text.yDirAdj,
                            text = s,
                        )
                    }
                    super.processTextPosition(text)
                }
            }
            stripper.startPage = pageIndex + 1
            stripper.endPage = pageIndex + 1
            stripper.getText(document)
            LineBuilder.build(glyphs)
        }

    /** Vizuelne dimenzije strana (sa uračunatom rotacijom). */
    fun pageSizes(document: PDDocument): List<Pair<Float, Float>> =
        (0 until document.numberOfPages).map { i ->
            val page = document.getPage(i)
            val rotation = ((page.rotation % 360) + 360) % 360
            val box = page.mediaBox
            if (rotation == 90 || rotation == 270) box.height to box.width
            else box.width to box.height
        }
}
