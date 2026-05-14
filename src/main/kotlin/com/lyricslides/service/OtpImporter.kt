package com.lyricslides.service

import java.io.File
import java.util.zip.ZipFile
import javax.imageio.ImageIO

/**
 * Extracts background images from LibreOffice Impress template (.otp) files.
 *
 * OTP files are ZIP archives with this structure:
 *   META-INF/manifest.xml
 *   content.xml
 *   styles.xml
 *   Pictures/         ← background images live here
 *     image1.png
 *     image2.jpg
 *   ...
 *
 * We extract the largest image from the Pictures/ folder as the background.
 * If no Pictures/ folder exists, we look for embedded images anywhere in the archive.
 */
class OtpImporter {

    private val outputDir: File = File(System.getProperty("user.home"), ".lyricslides/imported_backgrounds")

    init {
        outputDir.mkdirs()
    }

    /**
     * Extract the background image from an .otp file.
     * Returns the path to the extracted image file, or null if extraction failed.
     */
    fun extractBackground(otpFilePath: String): String? {
        val otpFile = File(otpFilePath)
        if (!otpFile.exists() || !otpFile.name.endsWith(".otp", ignoreCase = true)) {
            return null
        }

        return try {
            val zipFile = ZipFile(otpFile)
            val imageEntry = findBestImageEntry(zipFile)

            if (imageEntry != null) {
                // Determine output filename based on the template name
                val templateName = otpFile.nameWithoutExtension
                    .replace(Regex("""[^a-zA-Z0-9_-]"""), "_")
                val extension = imageEntry.name.substringAfterLast('.', "png")
                val outputFile = File(outputDir, "${templateName}.$extension")

                // Extract the image
                zipFile.getInputStream(imageEntry).use { input ->
                    outputFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                // Verify it's a valid image
                val img = ImageIO.read(outputFile)
                if (img != null && img.width >= 400 && img.height >= 300) {
                    zipFile.close()
                    return outputFile.absolutePath
                } else {
                    outputFile.delete()
                }
            }

            zipFile.close()
            null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Find the best image entry in the OTP zip file.
     * Prefers images in the Pictures/ folder, picks the largest file.
     */
    private fun findBestImageEntry(zipFile: ZipFile): java.util.zip.ZipEntry? {
        val imageExtensions = setOf("png", "jpg", "jpeg", "bmp", "gif", "svg")

        val allImageEntries = zipFile.entries().asSequence()
            .filter { entry ->
                !entry.isDirectory &&
                    imageExtensions.any { ext -> entry.name.endsWith(".$ext", ignoreCase = true) }
            }
            .toList()

        if (allImageEntries.isEmpty()) return null

        // Prefer images in Pictures/ folder (these are typically the backgrounds)
        val picturesEntries = allImageEntries.filter {
            it.name.startsWith("Pictures/", ignoreCase = true)
        }

        // Pick the largest image (most likely the full-slide background)
        val candidates = picturesEntries.ifEmpty { allImageEntries }
        return candidates.maxByOrNull { it.size }
    }

    /**
     * Extract backgrounds from multiple .otp files.
     * Returns a list of extracted image paths.
     */
    fun extractBackgrounds(otpFilePaths: List<String>): List<String> {
        return otpFilePaths.mapNotNull { extractBackground(it) }
    }
}
