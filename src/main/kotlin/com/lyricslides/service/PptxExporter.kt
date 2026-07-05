package com.lyricslides.service

import com.lyricslides.model.SlideContent
import org.apache.poi.xslf.usermodel.XMLSlideShow
import org.apache.poi.xslf.usermodel.XSLFSlide
import java.awt.Color
import java.awt.Dimension
import java.awt.Rectangle
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import javax.imageio.ImageIO

/**
 * Exports generated slides to PowerPoint (PPTX) format using Apache POI.
 */
class PptxExporter {

    /**
     * Export editable text-based slide content to a PPTX file.
     * Includes background image and semi-transparent scrim for readability.
     */
    fun exportSlidesToPptx(slideContents: List<SlideContent>, outputFile: File, backgroundImage: BufferedImage? = null, scrimPercent: Int = 90) {
        val pptx = XMLSlideShow()
        pptx.pageSize = Dimension(SlideGenerator.SLIDE_WIDTH, SlideGenerator.SLIDE_HEIGHT)

        // Prepare background image if available
        val bgPictureData = if (backgroundImage != null) {
            val baos = ByteArrayOutputStream()
            ImageIO.write(backgroundImage, "PNG", baos)
            pptx.addPicture(baos.toByteArray(), org.apache.poi.sl.usermodel.PictureData.PictureType.PNG)
        } else null

        // Prepare scrim image (semi-transparent dark overlay)
        val scrimPictureData = if (scrimPercent > 0) {
            val scrimW = SlideGenerator.SLIDE_WIDTH * scrimPercent / 100
            val scrimH = SlideGenerator.SLIDE_HEIGHT * scrimPercent / 100
            val scrimImage = BufferedImage(scrimW, scrimH, BufferedImage.TYPE_INT_ARGB)
            val g2d = scrimImage.createGraphics()
            g2d.color = Color(0, 0, 0, 160)
            val corner = if (scrimPercent >= 100) 0 else 40
            g2d.fillRoundRect(0, 0, scrimW, scrimH, corner, corner)
            g2d.dispose()
            val baos = ByteArrayOutputStream()
            ImageIO.write(scrimImage, "PNG", baos)
            pptx.addPicture(baos.toByteArray(), org.apache.poi.sl.usermodel.PictureData.PictureType.PNG)
        } else null

        for (content in slideContents) {
            val slide: XSLFSlide = pptx.createSlide()

            // Layer 1: Background image
            if (bgPictureData != null) {
                val bg = slide.createPicture(bgPictureData)
                bg.anchor = Rectangle(0, 0, SlideGenerator.SLIDE_WIDTH, SlideGenerator.SLIDE_HEIGHT)
            }

            // Layer 2: Scrim overlay
            if (scrimPictureData != null) {
                val scrimW = SlideGenerator.SLIDE_WIDTH * scrimPercent / 100
                val scrimH = SlideGenerator.SLIDE_HEIGHT * scrimPercent / 100
                val scrimX = (SlideGenerator.SLIDE_WIDTH - scrimW) / 2
                val scrimY = (SlideGenerator.SLIDE_HEIGHT - scrimH) / 2
                val scrim = slide.createPicture(scrimPictureData)
                scrim.anchor = Rectangle(scrimX, scrimY, scrimW, scrimH)
            }

            // Layer 3: Text content
            val textColor = Color.WHITE

            if (!content.title.isNullOrBlank()) {
                addTextBox(slide, content.title, 120, 140, 1680, 180, 48, true, textColor)
            }

            if (!content.subtitle.isNullOrBlank()) {
                addTextBox(slide, content.subtitle, 120, 340, 1680, 120, 28, false, textColor)
            }

            if (content.lines.isNotEmpty()) {
                val bodyText = content.lines.joinToString("\n")
                val margin = 100
                val yStart = margin
                val textWidth = SlideGenerator.SLIDE_WIDTH - (margin * 2)
                val textHeight = SlideGenerator.SLIDE_HEIGHT - (margin * 2)
                
                // Calculate optimal font size to fill the text area
                val numLines = content.lines.size
                // Height constraint: available height / lines / line spacing
                val maxFontByHeight = (textHeight / numLines / 1.3).toInt().coerceAtMost(120)
                // Width constraint: available width / longest line chars / avg char width factor
                val longestLine = content.lines.maxOfOrNull { it.length } ?: 1
                val maxFontByWidth = (textWidth / (longestLine * 0.52)).toInt().coerceAtMost(120)
                val fontSize = minOf(maxFontByHeight, maxFontByWidth).coerceAtLeast(24)

                addTextBox(slide, bodyText, margin, yStart, textWidth, textHeight, fontSize, true, textColor)
            }

            // Layer 4: CCLI in bottom-right
            if (content.ccliLine.isNotBlank()) {
                addTextBox(slide, content.ccliLine, 1320, 1000, 500, 50, 14, false, Color(220, 220, 220))
            }
        }

        outputFile.parentFile?.mkdirs()
        outputFile.outputStream().use { fos ->
            pptx.write(fos)
        }
        pptx.close()
    }

    private fun addTextBox(
        slide: XSLFSlide,
        text: String,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        fontSize: Int,
        bold: Boolean = false,
        color: Color = Color(35, 35, 60)
    ) {
        val textBox = slide.createTextBox()
        textBox.anchor = Rectangle(x, y, width, height)
        textBox.wordWrap = true
        textBox.setVerticalAlignment(org.apache.poi.sl.usermodel.VerticalAlignment.MIDDLE)

        // Split text into lines — each line becomes its own centered paragraph
        val lines = text.split("\n")
        var isFirst = true
        for (line in lines) {
            val para = if (isFirst) {
                textBox.textParagraphs[0].also {
                    it.textRuns.forEach { run -> run.setText("") }
                }
            } else {
                textBox.addNewTextParagraph()
            }
            para.textAlign = org.apache.poi.sl.usermodel.TextParagraph.TextAlign.CENTER

            val run = para.addNewTextRun()
            run.setText(line)
            run.setFontColor(color)
            run.fontSize = fontSize.toDouble()
            run.isBold = bold
            run.fontFamily = "Arial"
            isFirst = false
        }
    }

    /**
     * Export a list of slide images to a PPTX file.
     */
    fun exportToPptx(slideImages: List<BufferedImage>, outputFile: File) {
        val pptx = XMLSlideShow()

        // Set slide size to 1920x1080 (widescreen)
        pptx.pageSize = Dimension(
            SlideGenerator.SLIDE_WIDTH,
            SlideGenerator.SLIDE_HEIGHT
        )

        for (slideImage in slideImages) {
            val slide: XSLFSlide = pptx.createSlide()

            // Convert BufferedImage to byte array
            val baos = ByteArrayOutputStream()
            ImageIO.write(slideImage, "PNG", baos)
            val imageBytes = baos.toByteArray()

            // Add image as picture data (byte array version — works reliably in POI 5.x)
            val pictureData = pptx.addPicture(
                imageBytes,
                org.apache.poi.sl.usermodel.PictureData.PictureType.PNG
            )

            // Add picture to slide, filling the entire slide
            val picture = slide.createPicture(pictureData)
            picture.anchor = Rectangle(
                0, 0,
                SlideGenerator.SLIDE_WIDTH,
                SlideGenerator.SLIDE_HEIGHT
            )
        }

        // Ensure parent directory exists
        outputFile.parentFile?.mkdirs()

        // Write to file
        outputFile.outputStream().use { fos ->
            pptx.write(fos)
        }
        pptx.close()
    }

    /**
     * Export slide image files (PNGs) to a PPTX file.
     */
    fun exportFilesToPptx(slideFiles: List<File>, outputFile: File) {
        val images = slideFiles.map { file ->
            ImageIO.read(file)
        }
        exportToPptx(images, outputFile)
    }
}
