package com.lyricslides.service

import com.lyricslides.model.SongInfo
import org.apache.poi.sl.usermodel.PictureData
import org.apache.poi.sl.usermodel.TextParagraph
import org.apache.poi.xslf.usermodel.XMLSlideShow
import org.apache.poi.xslf.usermodel.XSLFSlide
import org.apache.poi.xslf.usermodel.XSLFTextBox
import org.apache.poi.xslf.usermodel.XSLFTextParagraph
import java.awt.Color
import java.awt.Dimension
import java.awt.Rectangle
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import javax.imageio.ImageIO

/**
 * Exports slides to PowerPoint (PPTX) format with proper editable text boxes
 * and background images. NOT just images-as-slides.
 *
 * Each slide has:
 * - A background image (stretched to fill)
 * - A centered text box with the lyrics (editable in PowerPoint)
 * - CCLI reference in the corner
 */
class PptxExporter {

    companion object {
        private const val SLIDE_WIDTH = SlideGenerator.SLIDE_WIDTH
        private const val SLIDE_HEIGHT = SlideGenerator.SLIDE_HEIGHT
        private const val EMU_PER_PIXEL = 9525 // English Metric Units per pixel
    }

    /**
     * Export slides as proper PPTX with editable text and background images.
     */
    fun exportToPptx(
        slideData: List<SlideContent>,
        backgroundImage: BufferedImage?,
        outputFile: File
    ) {
        val pptx = XMLSlideShow()
        pptx.pageSize = Dimension(SLIDE_WIDTH, SLIDE_HEIGHT)

        // Prepare background image bytes if we have one
        val bgPictureData = if (backgroundImage != null) {
            val baos = ByteArrayOutputStream()
            ImageIO.write(backgroundImage, "PNG", baos)
            pptx.addPicture(baos.toByteArray(), PictureData.PictureType.PNG)
        } else null

        for (content in slideData) {
            val slide = pptx.createSlide()

            // Add background image
            if (bgPictureData != null) {
                val bg = slide.createPicture(bgPictureData)
                bg.anchor = Rectangle(0, 0, SLIDE_WIDTH, SLIDE_HEIGHT)
            } else {
                // Set a solid dark background
                val slideBackground = slide.background
                slideBackground.fillColor = Color(20, 20, 50)
            }

            // Add semi-transparent dark scrim overlay (if scrimPercent > 0)
            if (content.scrimPercent > 0 && bgPictureData != null) {
                addScrimShape(slide, content.scrimPercent)
            }

            // Add text box with lyrics
            addTextBox(slide, content)

            // Add CCLI in bottom-right if present
            if (content.ccliLine.isNotBlank()) {
                addCcliBox(slide, content.ccliLine)
            }
        }

        outputFile.parentFile?.mkdirs()
        outputFile.outputStream().use { fos ->
            pptx.write(fos)
        }
        pptx.close()
    }

    /**
     * Legacy method: export pre-rendered images as slides (for backward compat).
     */
    fun exportToPptx(slideImages: List<BufferedImage>, outputFile: File) {
        val pptx = XMLSlideShow()
        pptx.pageSize = Dimension(SLIDE_WIDTH, SLIDE_HEIGHT)

        for (slideImage in slideImages) {
            val slide: XSLFSlide = pptx.createSlide()
            val baos = ByteArrayOutputStream()
            ImageIO.write(slideImage, "PNG", baos)
            val pictureData = pptx.addPicture(baos.toByteArray(), PictureData.PictureType.PNG)
            val picture = slide.createPicture(pictureData)
            picture.anchor = Rectangle(0, 0, SLIDE_WIDTH, SLIDE_HEIGHT)
        }

        outputFile.parentFile?.mkdirs()
        outputFile.outputStream().use { fos ->
            pptx.write(fos)
        }
        pptx.close()
    }

    private fun addTextBox(slide: XSLFSlide, content: SlideContent) {
        // Calculate text area based on the content — center it on the slide
        val textWidth = (SLIDE_WIDTH * 0.85).toInt()  // slightly wider than render to account for font differences
        val textHeight = (SLIDE_HEIGHT * 0.85).toInt()
        val marginX = (SLIDE_WIDTH - textWidth) / 2
        val marginY = (SLIDE_HEIGHT - textHeight) / 2

        val textBox: XSLFTextBox = slide.createTextBox()
        textBox.anchor = Rectangle(marginX, marginY, textWidth, textHeight)
        textBox.wordWrap = true
        textBox.setVerticalAlignment(org.apache.poi.sl.usermodel.VerticalAlignment.MIDDLE)

        // Clear default paragraph
        textBox.textParagraphs.firstOrNull()?.let { firstPara ->
            firstPara.textRuns.forEach { it.setText("") }
        }

        // Add each line
        var isFirst = true
        for (line in content.lines) {
            val para: XSLFTextParagraph = if (isFirst) {
                textBox.textParagraphs[0]
            } else {
                textBox.addNewTextParagraph()
            }
            para.textAlign = TextParagraph.TextAlign.CENTER

            val run = para.addNewTextRun()
            run.setText(line)
            run.fontSize = content.fontSize
            run.isBold = true
            run.fontFamily = "Arial"
            run.setFontColor(content.textColor)

            isFirst = false
        }
    }

    private fun addCcliBox(slide: XSLFSlide, ccliLine: String) {
        val boxWidth = 500
        val boxHeight = 40
        val margin = 30

        val textBox: XSLFTextBox = slide.createTextBox()
        textBox.anchor = Rectangle(
            SLIDE_WIDTH - boxWidth - margin,
            SLIDE_HEIGHT - boxHeight - margin,
            boxWidth,
            boxHeight
        )

        val para = textBox.textParagraphs[0]
        para.textAlign = TextParagraph.TextAlign.RIGHT

        val run = para.addNewTextRun()
        run.setText(ccliLine)
        run.fontSize = 14.0
        run.fontFamily = "Arial"
        run.setFontColor(Color(255, 255, 255, 200))
    }

    /**
     * Add a semi-transparent dark rectangle as a scrim/backdrop behind text.
     * Uses a pre-rendered semi-transparent PNG for maximum compatibility across
     * PowerPoint, Keynote, and LibreOffice.
     */
    private fun addScrimShape(slide: XSLFSlide, scrimPercent: Int) {
        val scrimW = SLIDE_WIDTH * scrimPercent / 100
        val scrimH = SLIDE_HEIGHT * scrimPercent / 100
        val scrimX = (SLIDE_WIDTH - scrimW) / 2
        val scrimY = (SLIDE_HEIGHT - scrimH) / 2

        // Create a semi-transparent black image
        val scrimImage = BufferedImage(scrimW, scrimH, BufferedImage.TYPE_INT_ARGB)
        val g2d = scrimImage.createGraphics()
        g2d.color = Color(0, 0, 0, 160) // ~63% opacity black
        val cornerRadius = if (scrimPercent >= 100) 0 else 40
        g2d.fillRoundRect(0, 0, scrimW, scrimH, cornerRadius, cornerRadius)
        g2d.dispose()

        // Add as a PNG picture on the slide
        val baos = ByteArrayOutputStream()
        ImageIO.write(scrimImage, "PNG", baos)
        val pictureData = slide.slideShow.addPicture(baos.toByteArray(), PictureData.PictureType.PNG)
        val picture = slide.createPicture(pictureData)
        picture.anchor = Rectangle(scrimX, scrimY, scrimW, scrimH)
    }
}

/**
 * Content for a single slide in the editable PPTX export.
 */
data class SlideContent(
    val lines: List<String>,
    val fontSize: Double,
    val textColor: Color,
    val ccliLine: String = "",
    val isTitle: Boolean = false,
    val scrimPercent: Int = 90
)
