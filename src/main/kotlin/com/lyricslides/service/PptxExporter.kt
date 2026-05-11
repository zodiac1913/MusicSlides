package com.lyricslides.service

import org.apache.poi.xslf.usermodel.XMLSlideShow
import org.apache.poi.xslf.usermodel.XSLFSlide
import java.awt.Dimension
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import javax.imageio.ImageIO

/**
 * Exports generated slides to PowerPoint (PPTX) format using Apache POI.
 */
class PptxExporter {

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

            // Add image as picture data
            val pictureData = pptx.addPicture(
                ByteArrayInputStream(imageBytes),
                org.apache.poi.sl.usermodel.PictureData.PictureType.PNG
            )

            // Add picture to slide, filling the entire slide
            val picture = slide.createPicture(pictureData)
            picture.anchor = java.awt.Rectangle(
                0, 0,
                SlideGenerator.SLIDE_WIDTH,
                SlideGenerator.SLIDE_HEIGHT
            )
        }

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
