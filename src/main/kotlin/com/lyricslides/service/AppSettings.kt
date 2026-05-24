package com.lyricslides.service

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Persistent application settings.
 */
@Serializable
data class AppSettings(
    val maxLinesPerSlide: Int = 8,
    val allowLineBreaking: Boolean = true,
    val verticalPercent: Int = 85,
    val horizontalPercent: Int = 80,
    val scrimPercent: Int = 90  // How much of the slide the dark overlay covers (0 = off, 100 = full)
)

/**
 * Manages loading and saving of application settings.
 * Stored at ~/.lyricslides/settings.json
 */
class SettingsManager {

    private val storageDir = File(System.getProperty("user.home"), ".lyricslides")
    private val storageFile = File(storageDir, "settings.json")
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    private var _settings: AppSettings = load()
    val settings: AppSettings get() = _settings

    private fun load(): AppSettings {
        return try {
            if (storageFile.exists()) {
                val content = storageFile.readText()
                if (content.isNotBlank()) {
                    json.decodeFromString<AppSettings>(content)
                } else AppSettings()
            } else AppSettings()
        } catch (_: Exception) {
            AppSettings()
        }
    }

    fun save(settings: AppSettings) {
        _settings = settings
        try {
            storageDir.mkdirs()
            storageFile.writeText(json.encodeToString(settings))
        } catch (_: Exception) {}
    }

    fun update(block: AppSettings.() -> AppSettings) {
        save(block(_settings))
    }
}
