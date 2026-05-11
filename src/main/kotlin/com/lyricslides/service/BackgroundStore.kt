package com.lyricslides.service

import com.lyricslides.model.SlideBackground
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Serializable representation of a custom background entry.
 */
@Serializable
data class SavedBackground(
    val name: String,
    val path: String,
    val category: String = "Custom"
)

/**
 * Persists user-imported custom backgrounds to disk so they survive app restarts.
 * File location: ~/.lyricslides/custom_backgrounds.json
 */
class BackgroundStore {

    private val storageDir: File = File(System.getProperty("user.home"), ".lyricslides")
    private val storageFile: File = File(storageDir, "custom_backgrounds.json")
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    /**
     * Load saved custom backgrounds from disk.
     * Returns only entries whose image files still exist.
     */
    fun loadCustomBackgrounds(): List<SlideBackground> {
        return try {
            if (!storageFile.exists()) return emptyList()
            val content = storageFile.readText()
            if (content.isBlank()) return emptyList()

            val saved = json.decodeFromString<List<SavedBackground>>(content)
            saved.filter { File(it.path).exists() }
                .map { SlideBackground(name = it.name, path = it.path, isBuiltIn = false, category = it.category) }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Save the current list of custom backgrounds to disk.
     */
    fun saveCustomBackgrounds(backgrounds: List<SlideBackground>) {
        try {
            storageDir.mkdirs()
            val toSave = backgrounds
                .filter { !it.isBuiltIn }
                .map { SavedBackground(name = it.name, path = it.path, category = it.category) }
            storageFile.writeText(json.encodeToString(toSave))
        } catch (_: Exception) {
            // Silent failure — not critical
        }
    }

    /**
     * Add a single background and persist.
     */
    fun addBackground(background: SlideBackground, allCustom: List<SlideBackground>) {
        saveCustomBackgrounds(allCustom + background)
    }

    /**
     * Remove a background and persist.
     */
    fun removeBackground(background: SlideBackground, allCustom: List<SlideBackground>) {
        saveCustomBackgrounds(allCustom.filter { it != background })
    }
}
