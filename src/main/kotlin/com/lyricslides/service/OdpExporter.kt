package com.lyricslides.service

import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.imageio.ImageIO

/**
 * Exports slides to ODP (OpenDocument Presentation) format.
 * Compatible with LibreOffice Impress, OpenOffice Impress, and other ODP readers.
 *
 * ODP is a ZIP file containing:
 *   mimetype (uncompressed, first entry)
 *   META-INF/manifest.xml
 *   content.xml
 *   styles.xml
 *   Pictures/slide_001.png, slide_002.png, ...
 */
class OdpExporter {

    fun exportToOdp(slideImages: List<BufferedImage>, outputFile: File) {
        val width = SlideGenerator.SLIDE_WIDTH
        val height = SlideGenerator.SLIDE_HEIGHT

        // Convert cm for ODP (1920px at 96dpi ≈ 50.8cm, 1080px ≈ 28.575cm)
        // ODP uses cm for dimensions
        val widthCm = "50.80cm"
        val heightCm = "28.575cm"

        outputFile.parentFile?.mkdirs()

        ZipOutputStream(FileOutputStream(outputFile)).use { zip ->
            // 1. mimetype (must be first, uncompressed)
            val mimeEntry = ZipEntry("mimetype")
            mimeEntry.method = ZipEntry.STORED
            val mimeBytes = "application/vnd.oasis.opendocument.presentation".toByteArray()
            mimeEntry.size = mimeBytes.size.toLong()
            mimeEntry.compressedSize = mimeBytes.size.toLong()
            mimeEntry.crc = java.util.zip.CRC32().apply { update(mimeBytes) }.value
            zip.putNextEntry(mimeEntry)
            zip.write(mimeBytes)
            zip.closeEntry()

            // 2. META-INF/manifest.xml
            zip.putNextEntry(ZipEntry("META-INF/manifest.xml"))
            zip.write(buildManifest(slideImages.size).toByteArray())
            zip.closeEntry()

            // 3. styles.xml
            zip.putNextEntry(ZipEntry("styles.xml"))
            zip.write(buildStyles(widthCm, heightCm).toByteArray())
            zip.closeEntry()

            // 4. Pictures
            for ((index, image) in slideImages.withIndex()) {
                val filename = "Pictures/slide_${String.format("%03d", index + 1)}.png"
                zip.putNextEntry(ZipEntry(filename))
                val baos = ByteArrayOutputStream()
                ImageIO.write(image, "PNG", baos)
                zip.write(baos.toByteArray())
                zip.closeEntry()
            }

            // 5. content.xml
            zip.putNextEntry(ZipEntry("content.xml"))
            zip.write(buildContent(slideImages.size, widthCm, heightCm).toByteArray())
            zip.closeEntry()
        }
    }

    private fun buildManifest(slideCount: Int): String {
        val sb = StringBuilder()
        sb.appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
        sb.appendLine("""<manifest:manifest xmlns:manifest="urn:oasis:names:tc:opendocument:xmlns:manifest:1.0" manifest:version="1.2">""")
        sb.appendLine("""  <manifest:file-entry manifest:media-type="application/vnd.oasis.opendocument.presentation" manifest:full-path="/"/>""")
        sb.appendLine("""  <manifest:file-entry manifest:media-type="text/xml" manifest:full-path="content.xml"/>""")
        sb.appendLine("""  <manifest:file-entry manifest:media-type="text/xml" manifest:full-path="styles.xml"/>""")
        for (i in 1..slideCount) {
            sb.appendLine("""  <manifest:file-entry manifest:media-type="image/png" manifest:full-path="Pictures/slide_${String.format("%03d", i)}.png"/>""")
        }
        sb.appendLine("""</manifest:manifest>""")
        return sb.toString()
    }

    private fun buildStyles(widthCm: String, heightCm: String): String {
        return """<?xml version="1.0" encoding="UTF-8"?>
<office:document-styles xmlns:office="urn:oasis:names:tc:opendocument:xmlns:office:1.0"
    xmlns:style="urn:oasis:names:tc:opendocument:xmlns:style:1.0"
    xmlns:fo="urn:oasis:names:tc:opendocument:xmlns:xsl-fo-compatible:1.0"
    xmlns:draw="urn:oasis:names:tc:opendocument:xmlns:drawing:1.0"
    xmlns:presentation="urn:oasis:names:tc:opendocument:xmlns:presentation:1.0"
    office:version="1.2">
  <office:automatic-styles>
    <style:page-layout style:name="PM0">
      <style:page-layout-properties fo:page-width="$widthCm" fo:page-height="$heightCm" style:print-orientation="landscape"/>
    </style:page-layout>
  </office:automatic-styles>
  <office:master-styles>
    <style:master-page style:name="Default" style:page-layout-name="PM0" draw:style-name="dp1"/>
  </office:master-styles>
</office:document-styles>"""
    }

    private fun buildContent(slideCount: Int, widthCm: String, heightCm: String): String {
        val sb = StringBuilder()
        sb.appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
        sb.appendLine("""<office:document-content xmlns:office="urn:oasis:names:tc:opendocument:xmlns:office:1.0"
    xmlns:style="urn:oasis:names:tc:opendocument:xmlns:style:1.0"
    xmlns:draw="urn:oasis:names:tc:opendocument:xmlns:drawing:1.0"
    xmlns:xlink="http://www.w3.org/1999/xlink"
    xmlns:presentation="urn:oasis:names:tc:opendocument:xmlns:presentation:1.0"
    xmlns:svg="urn:oasis:names:tc:opendocument:xmlns:svg-compatible:1.0"
    office:version="1.2">""")

        // Automatic styles for the image frames
        sb.appendLine("""  <office:automatic-styles>""")
        sb.appendLine("""    <style:style style:name="dp1" style:family="drawing-page"/>""")
        sb.appendLine("""    <style:style style:name="gr1" style:family="graphic">""")
        sb.appendLine("""      <style:graphic-properties draw:stroke="none" draw:fill="none"/>""")
        sb.appendLine("""    </style:style>""")
        sb.appendLine("""  </office:automatic-styles>""")

        sb.appendLine("""  <office:body>""")
        sb.appendLine("""    <office:presentation>""")

        for (i in 1..slideCount) {
            val picFile = "Pictures/slide_${String.format("%03d", i)}.png"
            sb.appendLine("""      <draw:page draw:name="Slide${i}" draw:style-name="dp1" draw:master-page-name="Default" presentation:presentation-page-layout-name="">""")
            sb.appendLine("""        <draw:frame draw:style-name="gr1" svg:x="0cm" svg:y="0cm" svg:width="$widthCm" svg:height="$heightCm">""")
            sb.appendLine("""          <draw:image xlink:href="$picFile" xlink:type="simple" xlink:show="embed" xlink:actuate="onLoad"/>""")
            sb.appendLine("""        </draw:frame>""")
            sb.appendLine("""      </draw:page>""")
        }

        sb.appendLine("""    </office:presentation>""")
        sb.appendLine("""  </office:body>""")
        sb.appendLine("""</office:document-content>""")
        return sb.toString()
    }
}
