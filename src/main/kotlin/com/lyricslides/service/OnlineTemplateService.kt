package com.lyricslides.service

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.*
import java.io.File
import javax.imageio.ImageIO

/**
 * Represents a LibreOffice Impress template available for download.
 */
data class OnlineTemplate(
    val name: String,
    val collection: String,
    val thumbnailUrl: String,
    val picturesApiUrl: String
) {
    val displayName: String
        get() = name.replace("-", " ").replaceFirstChar { it.uppercase() }
}

/**
 * Service for browsing and downloading LibreOffice Impress template backgrounds
 * from the dohliam/libreoffice-impress-templates GitHub repository.
 */
class OnlineTemplateService {

    private val client = HttpClient(CIO) {
        engine { requestTimeout = 30000 }
    }

    private val baseApiUrl = "https://api.github.com/repos/dohliam/libreoffice-impress-templates/contents"
    private val baseRawUrl = "https://raw.githubusercontent.com/dohliam/libreoffice-impress-templates/master"
    private val outputDir = File(System.getProperty("user.home"), ".lyricslides/imported_backgrounds")

    // Collections that tend to have good slide backgrounds
    private val collections = listOf(
        "apache-oo",
        "lo51-templates",
        "lo5-design-candidates",
        "lo4-design-candidates",
        "modern-impress-templates",
        "kde-files-org",
        "user-contrib"
    )

    init {
        outputDir.mkdirs()
    }

    /**
     * Fetch the list of available templates with their thumbnail URLs.
     * Returns templates grouped by collection.
     */
    suspend fun fetchTemplateList(): Result<List<OnlineTemplate>> {
        val allTemplates = mutableListOf<OnlineTemplate>()

        for (collection in collections) {
            try {
                val response: HttpResponse = client.get("$baseApiUrl/$collection") {
                    header(HttpHeaders.UserAgent, "LyricSlides-App")
                    header(HttpHeaders.Accept, "application/json")
                }

                if (response.status != HttpStatusCode.OK) continue

                val body = response.bodyAsText()
                val json = Json { ignoreUnknownKeys = true }
                val items = json.decodeFromString<JsonArray>(body)

                for (item in items) {
                    val obj = item.jsonObject
                    val type = obj["type"]?.jsonPrimitive?.content ?: continue
                    val name = obj["name"]?.jsonPrimitive?.content ?: continue

                    if (type == "dir" && name != "scripts") {
                        allTemplates.add(
                            OnlineTemplate(
                                name = name,
                                collection = collection,
                                thumbnailUrl = "$baseRawUrl/$collection/$name/Thumbnails/thumbnail.png",
                                picturesApiUrl = "$baseApiUrl/$collection/$name/Pictures"
                            )
                        )
                    }
                }
            } catch (_: Exception) {
                // Skip collections that fail
            }
        }

        return if (allTemplates.isNotEmpty()) {
            Result.success(allTemplates)
        } else {
            Result.failure(Exception("Could not fetch template list from GitHub"))
        }
    }

    /**
     * Download a template's background image and save it locally.
     * Returns the local file path of the downloaded image, or null on failure.
     */
    suspend fun downloadTemplateBackground(template: OnlineTemplate): String? {
        return try {
            // First, get the list of files in the Pictures folder
            val response: HttpResponse = client.get(template.picturesApiUrl) {
                header(HttpHeaders.UserAgent, "LyricSlides-App")
                header(HttpHeaders.Accept, "application/json")
            }

            if (response.status != HttpStatusCode.OK) return null

            val body = response.bodyAsText()
            val json = Json { ignoreUnknownKeys = true }
            val items = json.decodeFromString<JsonArray>(body)

            // Find the largest image file (most likely the background)
            val imageExtensions = setOf("png", "jpg", "jpeg", "bmp", "gif", "svg")
            var bestFile: Pair<String, Long>? = null // (download_url, size)

            for (item in items) {
                val obj = item.jsonObject
                val name = obj["name"]?.jsonPrimitive?.content ?: continue
                val ext = name.substringAfterLast('.', "").lowercase()
                if (ext !in imageExtensions) continue

                val size = obj["size"]?.jsonPrimitive?.long ?: 0
                val downloadUrl = obj["download_url"]?.jsonPrimitive?.content ?: continue

                if (bestFile == null || size > bestFile.second) {
                    bestFile = downloadUrl to size
                }
            }

            if (bestFile == null) {
                // No images in Pictures/ — try downloading the thumbnail as fallback
                return downloadThumbnail(template)
            }

            // Download the image
            val imageUrl = bestFile.first
            val ext = imageUrl.substringAfterLast('.', "png")
            val outputFile = File(outputDir, "${template.collection}-${template.name}.$ext")

            val imageResponse: HttpResponse = client.get(imageUrl) {
                header(HttpHeaders.UserAgent, "LyricSlides-App")
            }

            if (imageResponse.status != HttpStatusCode.OK) return null

            outputFile.writeBytes(imageResponse.body<ByteArray>())

            // Verify it's a valid image
            val img = ImageIO.read(outputFile)
            if (img != null) {
                outputFile.absolutePath
            } else {
                outputFile.delete()
                // Fall back to thumbnail
                downloadThumbnail(template)
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Download just the thumbnail as a fallback background.
     */
    private suspend fun downloadThumbnail(template: OnlineTemplate): String? {
        return try {
            val outputFile = File(outputDir, "${template.collection}-${template.name}-thumb.png")
            val response = client.get(template.thumbnailUrl) {
                header(HttpHeaders.UserAgent, "LyricSlides-App")
            }
            if (response.status != HttpStatusCode.OK) return null

            outputFile.writeBytes(response.body<ByteArray>())

            val img = ImageIO.read(outputFile)
            if (img != null) {
                outputFile.absolutePath
            } else {
                outputFile.delete()
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Download a thumbnail image as bytes (for preview in the UI).
     */
    suspend fun fetchThumbnailBytes(url: String): ByteArray? {
        return try {
            val response = client.get(url) {
                header(HttpHeaders.UserAgent, "LyricSlides-App")
            }
            if (response.status == HttpStatusCode.OK) {
                response.body<ByteArray>()
            } else null
        } catch (_: Exception) {
            null
        }
    }

    fun close() {
        client.close()
    }
}
