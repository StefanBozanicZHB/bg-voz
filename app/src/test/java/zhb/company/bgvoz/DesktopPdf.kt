package zhb.company.bgvoz

import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.rendering.PDFRenderer
import org.apache.pdfbox.text.PDFTextStripper
import org.apache.pdfbox.text.TextPosition
import zhb.company.bgvoz.data.pdf.LineBuilder
import zhb.company.bgvoz.data.pdf.ShadingProbe
import zhb.company.bgvoz.data.pdf.TextLine
import zhb.company.bgvoz.data.pdf.Word
import java.awt.image.BufferedImage
import kotlin.math.roundToInt

/**
 * Desktop pandan [zhb.company.bgvoz.data.pdf.PdfBoxTextReader]-a i
 * [zhb.company.bgvoz.data.pdf.AndroidShadingProbe]-a – ista logika, samo Apache PDFBox
 * umesto PdfBox-Android porta, da bi parser mogao da se testira na JVM-u.
 */
object DesktopPdf {

    fun readLines(document: PDDocument): List<List<TextLine>> =
        (0 until document.numberOfPages).map { pageIndex ->
            val glyphs = mutableListOf<Word>()
            val stripper = object : PDFTextStripper() {
                override fun processTextPosition(text: TextPosition) {
                    val s = text.unicode
                    if (!s.isNullOrBlank()) {
                        glyphs += Word(text.xDirAdj, text.xDirAdj + text.widthDirAdj, text.yDirAdj, s)
                    }
                    super.processTextPosition(text)
                }
            }
            stripper.startPage = pageIndex + 1
            stripper.endPage = pageIndex + 1
            stripper.getText(document)
            LineBuilder.build(glyphs)
        }

    fun probe(document: PDDocument, scale: Float = 2f): ShadingProbe {
        val renderer = PDFRenderer(document)
        val cache = HashMap<Int, BufferedImage>()
        return ShadingProbe { page, x, y ->
            val image = cache.getOrPut(page) { renderer.renderImage(page, scale) }
            val px = (x * scale).roundToInt()
            val py = (y * scale).roundToInt()
            if (px in 0 until image.width && py in 0 until image.height) image.getRGB(px, py) else null
        }
    }
}
