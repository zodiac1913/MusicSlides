package com.lyricslides.service

import com.lyricslides.model.LyricSlide
import com.lyricslides.model.SongInfo
import java.awt.*
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import java.io.File

/**
 * Generates slides from lyrics text.
 * Handles text sizing, accessibility checks, and image composition.
 * Produces a title slide (first), lyrics slides, and a closing slide (last)
 * with CCLI licensing information.
 */
class SlideGenerator {

    companion object {
        const val SLIDE_WIDTH = 1920
        const val SLIDE_HEIGHT = 1080
        const val PADDING = 100
        const val MIN_FONT_SIZE = 24f
        const val MAX_FONT_SIZE = 120f
        const val WCAG_AA_CONTRAST_RATIO = 4.5
        const val TITLE_FONT_SIZE = 80f
        const val SUBTITLE_FONT_SIZE = 40f
        const val LEGAL_FONT_SIZE = 32f
        const val MAX_LINES_PER_SLIDE = 8
        const val HYMN_LINES_PER_SLIDE = 4
    }

    /**
     * Parse lyrics into slide groups based on blank line breaks.
     * Each group of consecutive non-empty lines becomes one slide.
     *
     * If a group exceeds MAX_LINES_PER_SLIDE, it's split into multiple slides
     * with balanced line counts (equal, or off by one line).
     */
    fun parseLyricsIntoSlides(lyrics: String, maxLines: Int = MAX_LINES_PER_SLIDE): List<List<String>> {
        val rawSlides = mutableListOf<List<String>>()
        val currentSlide = mutableListOf<String>()

        for (line in lyrics.lines()) {
            if (line.isBlank()) {
                if (currentSlide.isNotEmpty()) {
                    rawSlides.add(currentSlide.toList())
                    currentSlide.clear()
                }
            } else {
                currentSlide.add(line.trim())
            }
        }

        // Don't forget the last group
        if (currentSlide.isNotEmpty()) {
            rawSlides.add(currentSlide.toList())
        }

        // Second pass: split any slide with more than maxLines
        val finalSlides = mutableListOf<List<String>>()
        for (slide in rawSlides) {
            if (slide.size <= maxLines) {
                finalSlides.add(slide)
            } else {
                finalSlides.addAll(splitLongSlide(slide, maxLines))
            }
        }

        // Third pass: if most slides are single-line (bad formatting from source),
        // merge them into groups of HYMN_LINES_PER_SLIDE (typically 4 for hymns).
        // This handles sources like Hymnary that sometimes return one line per "verse."
        return if (isMostlySingleLine(finalSlides)) {
            mergeIntoStanzas(finalSlides)
        } else {
            finalSlides
        }
    }

    /**
     * Check if the majority of slides are single-line (indicating bad source formatting).
     */
    private fun isMostlySingleLine(slides: List<List<String>>): Boolean {
        if (slides.size < 4) return false
        val singleLineCount = slides.count { it.size == 1 }
        return singleLineCount.toFloat() / slides.size > 0.7f
    }

    /**
     * Merge a list of mostly-single-line slides into groups of 4 lines (standard hymn stanza).
     * If total lines aren't evenly divisible by 4, uses balanced splitting.
     */
    private fun mergeIntoStanzas(slides: List<List<String>>): List<List<String>> {
        // Flatten all lines
        val allLines = slides.flatMap { it }
        val total = allLines.size

        // Standard hymn stanza is 4 lines. Use 4 as the target.
        val targetLinesPerSlide = HYMN_LINES_PER_SLIDE
        val numStanzas = (total + targetLinesPerSlide - 1) / targetLinesPerSlide

        // Use balanced splitting
        val baseSize = total / numStanzas
        val remainder = total % numStanzas

        val result = mutableListOf<List<String>>()
        var index = 0
        for (i in 0 until numStanzas) {
            val chunkSize = if (i < remainder) baseSize + 1 else baseSize
            result.add(allLines.subList(index, index + chunkSize))
            index += chunkSize
        }

        return result
    }

    /**
     * Split a slide with too many lines into multiple balanced slides.
     * Tries to keep line counts equal (or off by at most 1) while ensuring
     * no slide exceeds MAX_LINES_PER_SLIDE.
     *
     * Examples (max=8):
     *   18 lines → 3 slides of 6,6,6
     *   17 lines → 3 slides of 6,6,5 (or 6,5,6)
     *   9  lines → 2 slides of 5,4
     *   16 lines → 2 slides of 8,8
     *   10 lines → 2 slides of 5,5
     */
    private fun splitLongSlide(lines: List<String>, maxLines: Int = MAX_LINES_PER_SLIDE): List<List<String>> {
        val total = lines.size
        val numChunks = (total + maxLines - 1) / maxLines

        // Balanced split: base size, with some chunks getting one extra line
        val baseSize = total / numChunks
        val remainder = total % numChunks

        val result = mutableListOf<List<String>>()
        var index = 0
        for (i in 0 until numChunks) {
            // Distribute the remainder across the first `remainder` chunks
            val chunkSize = if (i < remainder) baseSize + 1 else baseSize
            result.add(lines.subList(index, index + chunkSize))
            index += chunkSize
        }

        return result
    }

    /**
     * Calculate the maximum font size that fits all lines within the given area.
     */
    fun calculateOptimalFontSize(
        lines: List<String>,
        graphics: Graphics2D,
        availableWidth: Int = SLIDE_WIDTH - (PADDING * 2),
        availableHeight: Int = SLIDE_HEIGHT - (PADDING * 2)
    ): Float {

        var fontSize = MAX_FONT_SIZE

        while (fontSize >= MIN_FONT_SIZE) {
            val font = Font("SansSerif", Font.BOLD, fontSize.toInt())
            graphics.font = font
            val metrics = graphics.fontMetrics

            val totalTextHeight = metrics.height * lines.size
            val maxLineWidth = lines.maxOfOrNull { metrics.stringWidth(it) } ?: 0

            if (totalTextHeight <= availableHeight && maxLineWidth <= availableWidth) {
                return fontSize
            }

            fontSize -= 2f
        }

        return MIN_FONT_SIZE
    }

    /**
     * Calculate relative luminance of a color per WCAG 2.1 spec.
     */
    fun relativeLuminance(color: Color): Double {
        fun linearize(channel: Int): Double {
            val sRGB = channel / 255.0
            return if (sRGB <= 0.03928) {
                sRGB / 12.92
            } else {
                Math.pow((sRGB + 0.055) / 1.055, 2.4)
            }
        }

        val r = linearize(color.red)
        val g = linearize(color.green)
        val b = linearize(color.blue)

        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }

    /**
     * Calculate contrast ratio between two colors per WCAG 2.1.
     */
    fun contrastRatio(foreground: Color, background: Color): Double {
        val lum1 = relativeLuminance(foreground)
        val lum2 = relativeLuminance(background)
        val lighter = maxOf(lum1, lum2)
        val darker = minOf(lum1, lum2)
        return (lighter + 0.05) / (darker + 0.05)
    }

    /**
     * Sample the average color of the background image in the text area.
     */
    fun sampleBackgroundColor(image: BufferedImage): Color {
        val startX = PADDING
        val startY = PADDING
        val endX = SLIDE_WIDTH - PADDING
        val endY = SLIDE_HEIGHT - PADDING

        var totalR = 0L
        var totalG = 0L
        var totalB = 0L
        var count = 0L

        // Sample every 10th pixel for performance
        var y = startY
        while (y < endY && y < image.height) {
            var x = startX
            while (x < endX && x < image.width) {
                val pixel = Color(image.getRGB(x, y))
                totalR += pixel.red
                totalG += pixel.green
                totalB += pixel.blue
                count++
                x += 10
            }
            y += 10
        }

        return if (count > 0) {
            Color(
                (totalR / count).toInt(),
                (totalG / count).toInt(),
                (totalB / count).toInt()
            )
        } else {
            Color.DARK_GRAY
        }
    }

    /**
     * Choose an accessible text color that meets WCAG AA contrast ratio
     * against the given background color.
     */
    fun chooseAccessibleTextColor(backgroundColor: Color): Color {
        // Try white first
        if (contrastRatio(Color.WHITE, backgroundColor) >= WCAG_AA_CONTRAST_RATIO) {
            return Color.WHITE
        }

        // Try black
        if (contrastRatio(Color.BLACK, backgroundColor) >= WCAG_AA_CONTRAST_RATIO) {
            return Color.BLACK
        }

        // Try light yellow (good for dark/medium backgrounds)
        val lightYellow = Color(255, 255, 200)
        if (contrastRatio(lightYellow, backgroundColor) >= WCAG_AA_CONTRAST_RATIO) {
            return lightYellow
        }

        // Try dark navy (good for light backgrounds)
        val darkNavy = Color(10, 10, 60)
        if (contrastRatio(darkNavy, backgroundColor) >= WCAG_AA_CONTRAST_RATIO) {
            return darkNavy
        }

        // Fallback: use whichever of black/white has better contrast
        return if (contrastRatio(Color.WHITE, backgroundColor) > contrastRatio(Color.BLACK, backgroundColor)) {
            Color.WHITE
        } else {
            Color.BLACK
        }
    }

    /**
     * Render a single slide as a BufferedImage.
     */
    fun renderSlide(
        lines: List<String>,
        backgroundImage: BufferedImage?,
        slideNumber: Int,
        ccliLine: String = "",
        horizontalPercent: Int = 80,
        verticalPercent: Int = 85,
        allowLineBreaking: Boolean = false,
        scrimPercent: Int = 90
    ): BufferedImage {
        val image = BufferedImage(SLIDE_WIDTH, SLIDE_HEIGHT, BufferedImage.TYPE_INT_ARGB)
        val g2d = image.createGraphics()

        // Enable anti-aliasing
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB)

        // Draw background
        drawBackground(g2d, backgroundImage)

        // Check if background is too "busy" (high variance) — if so, add a scrim
        val bgColor: Color
        val needsScrim: Boolean
        if (backgroundImage != null) {
            bgColor = sampleBackgroundColor(image)
            needsScrim = backgroundHasHighVariance(image)
        } else {
            bgColor = Color(30, 30, 65)
            needsScrim = false
        }

        // Draw semi-transparent scrim if the background is too varied for clean text
        if (needsScrim && scrimPercent > 0) {
            drawTextScrim(g2d, scrimPercent)
        }

        val textColor = chooseAccessibleTextColor(
            if (needsScrim && scrimPercent > 0) Color(0, 0, 0, 180) else bgColor
        )

        // Calculate available area based on percentage settings
        val availableWidth = (SLIDE_WIDTH * horizontalPercent / 100)
        val availableHeight = (SLIDE_HEIGHT * verticalPercent / 100)

        // Calculate optimal font size using the configured area
        val fontSize = calculateOptimalFontSize(lines, g2d, availableWidth, availableHeight)
        val font = Font("SansSerif", Font.BOLD, fontSize.toInt())
        g2d.font = font
        g2d.color = textColor

        val metrics = g2d.fontMetrics
        val totalTextHeight = metrics.height * lines.size
        val startY = (SLIDE_HEIGHT - totalTextHeight) / 2 + metrics.ascent

        // Draw text shadow for better readability
        for ((index, line) in lines.withIndex()) {
            val lineWidth = metrics.stringWidth(line)
            val x = (SLIDE_WIDTH - lineWidth) / 2
            val y = startY + (index * metrics.height)

            // Shadow
            g2d.color = Color(0, 0, 0, 150)
            g2d.drawString(line, x + 3, y + 3)

            // Main text
            g2d.color = textColor
            g2d.drawString(line, x, y)
        }

        // CCLI reference in bottom-right corner
        if (ccliLine.isNotBlank()) {
            drawCcliCorner(g2d, ccliLine, textColor)
        }

        g2d.dispose()
        return image
    }

    /**
     * Render a title slide with song name, artist, and CCLI reference.
     */
    fun renderTitleSlide(
        songInfo: SongInfo,
        backgroundImage: BufferedImage?,
        scrimPercent: Int = 90
    ): BufferedImage {
        val image = BufferedImage(SLIDE_WIDTH, SLIDE_HEIGHT, BufferedImage.TYPE_INT_ARGB)
        val g2d = image.createGraphics()

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB)

        // Draw background
        drawBackground(g2d, backgroundImage)

        // Check variance and add scrim if needed
        val bgColor: Color
        val needsScrim: Boolean
        if (backgroundImage != null) {
            bgColor = sampleBackgroundColor(image)
            needsScrim = backgroundHasHighVariance(image)
        } else {
            bgColor = Color(30, 30, 65)
            needsScrim = false
        }

        if (needsScrim && scrimPercent > 0) {
            drawTextScrim(g2d, scrimPercent)
        }

        val textColor = chooseAccessibleTextColor(
            if (needsScrim && scrimPercent > 0) Color(0, 0, 0, 180) else bgColor
        )

        // Draw title (large, centered)
        val titleFont = Font("SansSerif", Font.BOLD, TITLE_FONT_SIZE.toInt())
        g2d.font = titleFont
        var metrics = g2d.fontMetrics

        val titleText = "\"${songInfo.title}\""
        val titleWidth = metrics.stringWidth(titleText)
        val titleX = (SLIDE_WIDTH - titleWidth) / 2
        val titleY = SLIDE_HEIGHT / 2 - 60

        // Shadow
        g2d.color = Color(0, 0, 0, 150)
        g2d.drawString(titleText, titleX + 3, titleY + 3)
        g2d.color = textColor
        g2d.drawString(titleText, titleX, titleY)

        // Draw artist (medium, below title)
        val subtitleFont = Font("SansSerif", Font.PLAIN, SUBTITLE_FONT_SIZE.toInt())
        g2d.font = subtitleFont
        metrics = g2d.fontMetrics

        val artistText = songInfo.artist
        val artistWidth = metrics.stringWidth(artistText)
        val artistX = (SLIDE_WIDTH - artistWidth) / 2
        val artistY = titleY + 70

        g2d.color = Color(0, 0, 0, 150)
        g2d.drawString(artistText, artistX + 2, artistY + 2)
        g2d.color = textColor
        g2d.drawString(artistText, artistX, artistY)

        // Draw CCLI reference (smaller, bottom-right corner)
        if (songInfo.ccliNumber.isNotBlank()) {
            drawCcliCorner(g2d, songInfo.ccliLine, textColor)
        }

        g2d.dispose()
        return image
    }

    /**
     * Render a closing slide with full legal/CCLI information.
     */
    fun renderClosingSlide(
        songInfo: SongInfo,
        backgroundImage: BufferedImage?,
        scrimPercent: Int = 90
    ): BufferedImage {
        val image = BufferedImage(SLIDE_WIDTH, SLIDE_HEIGHT, BufferedImage.TYPE_INT_ARGB)
        val g2d = image.createGraphics()

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB)

        // Draw background
        drawBackground(g2d, backgroundImage)

        // Check variance and add scrim if needed
        val bgColor: Color
        val needsScrim: Boolean
        if (backgroundImage != null) {
            bgColor = sampleBackgroundColor(image)
            needsScrim = backgroundHasHighVariance(image)
        } else {
            bgColor = Color(30, 30, 65)
            needsScrim = false
        }

        if (needsScrim && scrimPercent > 0) {
            drawTextScrim(g2d, scrimPercent)
        }

        val textColor = chooseAccessibleTextColor(
            if (needsScrim && scrimPercent > 0) Color(0, 0, 0, 180) else bgColor
        )

        // Build the closing slide content
        val legalLines = songInfo.legalBlock
        if (legalLines.isEmpty()) {
            g2d.dispose()
            return image
        }

        // Use a moderate font for the legal block
        val legalFont = Font("SansSerif", Font.PLAIN, SUBTITLE_FONT_SIZE.toInt())
        g2d.font = legalFont
        val metrics = g2d.fontMetrics

        val totalHeight = metrics.height * legalLines.size
        val startY = (SLIDE_HEIGHT - totalHeight) / 2 + metrics.ascent

        for ((index, line) in legalLines.withIndex()) {
            val lineWidth = metrics.stringWidth(line)
            val x = (SLIDE_WIDTH - lineWidth) / 2
            val y = startY + (index * metrics.height)

            // Shadow
            g2d.color = Color(0, 0, 0, 150)
            g2d.drawString(line, x + 2, y + 2)

            // Main text
            g2d.color = textColor
            g2d.drawString(line, x, y)
        }

        g2d.dispose()
        return image
    }

    /**
     * Draw the background (image or default gradient) onto a Graphics2D context.
     */
    private fun drawBackground(g2d: Graphics2D, backgroundImage: BufferedImage?) {
        if (backgroundImage != null) {
            g2d.drawImage(
                backgroundImage.getScaledInstance(SLIDE_WIDTH, SLIDE_HEIGHT, Image.SCALE_SMOOTH),
                0, 0, null
            )
        } else {
            val gradient = GradientPaint(
                0f, 0f, Color(20, 20, 50),
                0f, SLIDE_HEIGHT.toFloat(), Color(40, 40, 80)
            )
            g2d.paint = gradient
            g2d.fillRect(0, 0, SLIDE_WIDTH, SLIDE_HEIGHT)
        }
    }

    /**
     * Check if the background image has high luminance variance in the text area.
     * High variance means both very bright and very dark regions exist,
     * making it impossible for a single text color to be readable everywhere.
     */
    private fun backgroundHasHighVariance(image: BufferedImage): Boolean {
        val startX = PADDING
        val startY = PADDING
        val endX = SLIDE_WIDTH - PADDING
        val endY = SLIDE_HEIGHT - PADDING

        var minLum = 1.0
        var maxLum = 0.0

        // Sample every 20th pixel for performance
        var y = startY
        while (y < endY && y < image.height) {
            var x = startX
            while (x < endX && x < image.width) {
                val pixel = Color(image.getRGB(x, y))
                val lum = relativeLuminance(pixel)
                if (lum < minLum) minLum = lum
                if (lum > maxLum) maxLum = lum
                x += 20
            }
            y += 20
        }

        // If the range between darkest and brightest is large, it's "busy"
        // A range > 0.4 means there's both very dark and very bright areas
        return (maxLum - minLum) > 0.4
    }

    /**
     * Draw a semi-transparent dark scrim over the text area to ensure readability.
     * Coverage is a percentage of the slide (100 = full, 0 = none).
     */
    private fun drawTextScrim(g2d: Graphics2D, coveragePercent: Int = 90) {
        if (coveragePercent <= 0) return

        val scrimW = SLIDE_WIDTH * coveragePercent / 100
        val scrimH = SLIDE_HEIGHT * coveragePercent / 100
        val scrimX = (SLIDE_WIDTH - scrimW) / 2
        val scrimY = (SLIDE_HEIGHT - scrimH) / 2

        g2d.color = Color(0, 0, 0, 160) // ~63% opacity black
        val cornerRadius = if (coveragePercent >= 100) 0 else 40
        g2d.fillRoundRect(scrimX, scrimY, scrimW, scrimH, cornerRadius, cornerRadius)
    }

    /**
     * Draw the CCLI reference line in the bottom-right corner of the slide.
     * Uses a small font and a subtle shadow for readability on any background.
     */
    private fun drawCcliCorner(g2d: Graphics2D, ccliText: String, textColor: Color) {
        if (ccliText.isBlank()) return

        val font = Font("SansSerif", Font.PLAIN, LEGAL_FONT_SIZE.toInt())
        g2d.font = font
        val metrics = g2d.fontMetrics

        val textWidth = metrics.stringWidth(ccliText)
        val margin = 40
        val x = SLIDE_WIDTH - textWidth - margin
        val y = SLIDE_HEIGHT - margin

        // Shadow for readability on any background
        g2d.color = Color(0, 0, 0, 160)
        g2d.drawString(ccliText, x + 2, y + 2)

        // Main text with slight transparency to stay subtle
        g2d.color = Color(textColor.red, textColor.green, textColor.blue, 220)
        g2d.drawString(ccliText, x, y)
    }

    /**
     * Generate a complete presentation: title slide + lyrics slides + closing slide.
     * Every slide gets the CCLI reference in the bottom-right corner (if provided).
     */
    fun generateFullPresentation(
        lyrics: String,
        songInfo: SongInfo,
        backgroundImage: BufferedImage?,
        maxLines: Int = MAX_LINES_PER_SLIDE,
        horizontalPercent: Int = 80,
        verticalPercent: Int = 85,
        allowLineBreaking: Boolean = false,
        scrimPercent: Int = 90
    ): List<BufferedImage> {
        val slides = mutableListOf<BufferedImage>()
        val ccliLine = songInfo.ccliLine  // "CCLI Song # 4447960" or ""

        // 1. Title slide
        slides.add(renderTitleSlide(songInfo, backgroundImage, scrimPercent))

        // 2. Lyrics slides (each includes CCLI in the corner)
        val slideGroups = parseLyricsIntoSlides(lyrics, maxLines)
        for ((index, lines) in slideGroups.withIndex()) {
            slides.add(renderSlide(lines, backgroundImage, index + 1, ccliLine,
                horizontalPercent = horizontalPercent,
                verticalPercent = verticalPercent,
                allowLineBreaking = allowLineBreaking,
                scrimPercent = scrimPercent
            ))
        }

        // 3. Closing slide with CCLI/legal info
        slides.add(renderClosingSlide(songInfo, backgroundImage, scrimPercent))

        return slides
    }

    /**
     * Generate all slides from lyrics and save them.
     */
    fun generateSlides(
        lyrics: String,
        backgroundImage: BufferedImage?,
        outputDir: File
    ): List<File> {
        outputDir.mkdirs()
        val slideGroups = parseLyricsIntoSlides(lyrics)
        val outputFiles = mutableListOf<File>()

        for ((index, lines) in slideGroups.withIndex()) {
            val slideImage = renderSlide(lines, backgroundImage, index + 1)
            val outputFile = File(outputDir, "slide_${String.format("%03d", index + 1)}.png")
            ImageIO.write(slideImage, "PNG", outputFile)
            outputFiles.add(outputFile)
        }

        return outputFiles
    }
}
