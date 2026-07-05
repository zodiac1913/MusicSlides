package com.lyricslides.model

import kotlinx.serialization.Serializable

/**
 * Represents a lyrics source provider.
 */
data class LyricsSource(
    val name: String,
    val baseUrl: String,
    val isCustom: Boolean = false
)

/**
 * A single slide containing lyrics text and styling info.
 */
data class LyricSlide(
    val slideNumber: Int,
    val lines: List<String>,
    val fontSize: Float = 48f,
    val textColor: Long = 0xFFFFFFFF,
    val backgroundPath: String? = null
)

/**
 * Structured content for a single slide when exporting to editable PowerPoint text.
 */
data class SlideContent(
    val title: String? = null,
    val subtitle: String? = null,
    val lines: List<String> = emptyList(),
    val ccliLine: String = ""
)

/**
 * Result from a lyrics fetch operation.
 */
@Serializable
data class LyricsResult(
    val lyrics: String = "",
    val artist: String = "",
    val title: String = "",
    val source: String = ""
)

/**
 * Song metadata including CCLI licensing information.
 */
data class SongInfo(
    val title: String = "",
    val artist: String = "",
    val ccliNumber: String = "",  // e.g. "4447960"
    val copyright: String = "",   // Optional additional copyright line
) {
    /**
     * Formatted CCLI reference line for display on slides.
     */
    val ccliLine: String
        get() = if (ccliNumber.isNotBlank()) "CCLI Song # $ccliNumber" else ""

    /**
     * Full legal reference block for the closing slide.
     */
    val legalBlock: List<String>
        get() {
            val lines = mutableListOf<String>()
            if (title.isNotBlank()) lines.add("\"$title\"")
            if (artist.isNotBlank()) lines.add(artist)
            if (ccliNumber.isNotBlank()) lines.add("CCLI Song # $ccliNumber")
            if (copyright.isNotBlank()) lines.add(copyright)
            return lines
        }
}

/**
 * Represents a background option for slides.
 */
data class SlideBackground(
    val name: String,
    val path: String,
    val isBuiltIn: Boolean = true,
    val category: String = "General"
)

/**
 * Application state for the entire workflow.
 */
enum class AppStep {
    SEARCH,       // Search for lyrics
    EDIT,         // View/edit lyrics
    BACKGROUND,   // Choose background
    GENERATE,     // Generate slides
    PREVIEW       // Preview and export
}
