package com.lyricslides.service

import com.lyricslides.model.SongInfo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertTrue

class PptxExporterTest {
    @TempDir
    lateinit var tempDir: File

    @Test
    fun `exports text-based slides with title and lyric text`() {
        val exporter = PptxExporter()
        val outputFile = File(tempDir, "test.pptx")
        val slides = listOf(
            SlideContent(
                title = "Test Song",
                subtitle = "Test Artist",
                lines = listOf("Hello world", "This should be text"),
                ccliLine = "CCLI Song # 12345"
            ),
            SlideContent(
                title = null,
                subtitle = null,
                lines = listOf("Verse 1", "Line 2"),
                ccliLine = ""
            )
        )

        exporter.exportSlidesToPptx(slides, outputFile)

        assertTrue(outputFile.exists())
        assertTrue(outputFile.length() > 0)
    }
}
