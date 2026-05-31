package com.lyricslides.service

import com.lyricslides.model.SlideBackground
import java.awt.*
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

/**
 * Provides background images from the bundled resources folder
 * and from user-imported custom backgrounds.
 *
 * Bundled backgrounds: src/main/resources/backgrounds/ (shipped with the app)
 * Custom backgrounds: ~/.lyricslides/imported_backgrounds/ (user-added)
 *
 * To add more bundled backgrounds: drop image files into
 * src/main/resources/backgrounds/ and rebuild.
 */
class BackgroundProvider {

    companion object {
        const val WIDTH = SlideGenerator.SLIDE_WIDTH
        const val HEIGHT = SlideGenerator.SLIDE_HEIGHT
    }

    private val cache = mutableMapOf<String, BufferedImage>()

    /**
     * Get the list of bundled backgrounds by scanning the backgrounds directory.
     */
    fun getBuiltInBackgrounds(): List<SlideBackground> {
        val backgrounds = mutableListOf<SlideBackground>()

        // Find the backgrounds directory — check multiple possible locations
        val possibleDirs = mutableListOf(
            // Dev mode (running from source)
            File("src/main/resources/backgrounds"),
            // Packaged app — resources dir set by Compose
            File(System.getProperty("compose.application.resources.dir", "."), "backgrounds"),
            // Packaged app — relative to working directory
            File("resources/backgrounds"),
            // Packaged app — relative to app location
            File(System.getProperty("user.dir"), "resources/backgrounds"),
            File(System.getProperty("user.dir"), "app/resources/backgrounds"),
        )

        // Also check relative to the JAR/class location
        try {
            val codeSource = javaClass.protectionDomain?.codeSource?.location
            if (codeSource != null) {
                val jarDir = File(codeSource.toURI()).parentFile
                possibleDirs.add(File(jarDir, "resources/backgrounds"))
                possibleDirs.add(File(jarDir, "backgrounds"))
                possibleDirs.add(File(jarDir.parentFile, "resources/backgrounds"))
            }
        } catch (_: Exception) {}

        val dir = possibleDirs.firstOrNull { it.isDirectory }

        if (dir != null) {
            dir.listFiles()
                ?.filter { it.isFile && isImageFile(it.name) }
                ?.sortedBy { it.name.lowercase() }
                ?.forEach { file ->
                    val name = file.nameWithoutExtension
                        .replace("-", " ")
                        .replace("_", " ")
                        .split(" ")
                        .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }

                    val category = categorize(file.nameWithoutExtension)

                    backgrounds.add(
                        SlideBackground(
                            name = name,
                            path = file.absolutePath,
                            isBuiltIn = true,
                            category = category
                        )
                    )
                }
        }

        return backgrounds
    }

    /**
     * Load a background as a BufferedImage.
     */
    fun loadBackground(background: SlideBackground?): BufferedImage? {
        if (background == null) return null
        val path = background.path

        cache[path]?.let { return it }

        val image = try {
            val file = File(path)
            if (file.exists()) ImageIO.read(file) else null
        } catch (_: Exception) { null }

        if (image != null) {
            cache[path] = image
        }
        return image
    }

    /**
     * Generate a thumbnail for preview in the picker.
     */
    fun loadThumbnail(background: SlideBackground, width: Int = 320, height: Int = 180): BufferedImage? {
        val full = loadBackground(background) ?: return null
        val thumb = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        val g2d = thumb.createGraphics()
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
        g2d.drawImage(full.getScaledInstance(width, height, Image.SCALE_SMOOTH), 0, 0, null)
        g2d.dispose()
        return thumb
    }

    /**
     * Categorize a background by its filename.
     */
    private fun categorize(name: String): String {
        val lower = name.lowercase()
        return when {
            lower.contains("jesus") || lower.contains("risen") || lower.contains("crux") ||
                lower.contains("cross") || lower.contains("shepherd") || lower.contains("sheperd") ||
                lower.contains("pray") || lower.contains("babe") || lower.contains("moses") ||
                lower.contains("david") || lower.contains("woman") || lower.contains("calms") ||
                lower.contains("commandment") || lower.contains("damascus") -> "Biblical"
            lower.contains("abstract") || lower.contains("purple") || lower.contains("blue") -> "Abstract"
            lower.contains("dark") || lower.contains("stars") || lower.contains("black") ||
                lower.contains("gray") || lower.contains("soft") -> "Dark"
            lower.contains("sunset") || lower.contains("ocean") || lower.contains("forest") ||
                lower.contains("sky") || lower.contains("nature") -> "Nature"
            lower.contains("plain") || lower.contains("white") -> "Simple"
            else -> "General"
        }
    }

    private fun isImageFile(name: String): Boolean {
        val ext = name.substringAfterLast('.', "").lowercase()
        return ext in setOf("jpg", "jpeg", "png", "bmp", "gif")
    }
}
