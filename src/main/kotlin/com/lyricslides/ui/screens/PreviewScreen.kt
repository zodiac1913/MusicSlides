package com.lyricslides.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.lyricslides.viewmodel.AppViewModel
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

@Composable
fun PreviewScreen(viewModel: AppViewModel) {
    val generatedSlides by viewModel.generatedSlides.collectAsState()
    val exportPath by viewModel.exportPath.collectAsState()
    val artist by viewModel.artist.collectAsState()
    val title by viewModel.title.collectAsState()
    val ccliNumber by viewModel.ccliNumber.collectAsState()

    var currentSlideIndex by remember { mutableStateOf(0) }

    // Build the default export filename: Artist-Title[-ccli].pptx
    val defaultFilenameBase = remember(artist, title, ccliNumber) {
        buildExportFilename(artist, title, ccliNumber)
    }

    if (generatedSlides.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "No slides generated yet",
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { viewModel.previousStep() }) {
                    Text("Go Back to Generate")
                }
            }
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        // Header with export buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Preview Slides",
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = "Slide ${currentSlideIndex + 1} of ${generatedSlides.size}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { viewModel.goToStep(com.lyricslides.model.AppStep.SEARCH) }) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Start Over")
                }

                // Export as PPTX
                Button(
                    onClick = {
                        val chooser = JFileChooser()
                        chooser.fileFilter = FileNameExtensionFilter("PowerPoint", "pptx")
                        chooser.dialogTitle = "Save as PowerPoint"
                        chooser.selectedFile = java.io.File("$defaultFilenameBase.pptx")
                        val result = chooser.showSaveDialog(null)
                        if (result == JFileChooser.APPROVE_OPTION) {
                            var path = chooser.selectedFile.absolutePath
                            if (!path.endsWith(".pptx")) path += ".pptx"
                            viewModel.exportToPptx(path)
                        }
                    }
                ) {
                    Icon(Icons.Default.SaveAlt, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Export PPTX")
                }

                // Export as images
                OutlinedButton(
                    onClick = {
                        val chooser = JFileChooser()
                        chooser.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
                        chooser.dialogTitle = "Select Output Folder"
                        chooser.selectedFile = java.io.File(defaultFilenameBase)
                        val result = chooser.showSaveDialog(null)
                        if (result == JFileChooser.APPROVE_OPTION) {
                            viewModel.exportAsImages(chooser.selectedFile.absolutePath)
                        }
                    }
                ) {
                    Icon(Icons.Default.Image, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Export PNGs")
                }
            }
        }

        // Export success message
        exportPath?.let { path ->
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Exported to: $path")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Main slide preview
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Previous button
                IconButton(
                    onClick = { if (currentSlideIndex > 0) currentSlideIndex-- },
                    enabled = currentSlideIndex > 0
                ) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Previous slide")
                }

                // Slide image
                Box(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    if (currentSlideIndex < generatedSlides.size) {
                        val bitmap = remember(currentSlideIndex, generatedSlides) {
                            generatedSlides[currentSlideIndex].toComposeImageBitmap()
                        }
                        Image(
                            bitmap = bitmap,
                            contentDescription = "Slide ${currentSlideIndex + 1}",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                }

                // Next button
                IconButton(
                    onClick = { if (currentSlideIndex < generatedSlides.size - 1) currentSlideIndex++ },
                    enabled = currentSlideIndex < generatedSlides.size - 1
                ) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Next slide")
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Thumbnail strip
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.height(80.dp).fillMaxWidth()
        ) {
            itemsIndexed(generatedSlides) { index, slide ->
                Card(
                    modifier = Modifier
                        .width(142.dp)
                        .fillMaxHeight(),
                    onClick = { currentSlideIndex = index },
                    border = if (index == currentSlideIndex)
                        CardDefaults.outlinedCardBorder().copy(
                            width = 2.dp
                        ) else null
                ) {
                    val bitmap = remember(slide) { slide.toComposeImageBitmap() }
                    Image(
                        bitmap = bitmap,
                        contentDescription = "Slide ${index + 1} thumbnail",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }
    }
}

/**
 * Build a default export filename from song metadata.
 * Format: Artist-Title[-ccli].
 * Falls back to "lyrics_slides" if no metadata is available.
 *
 * Examples:
 *   Kari Jobe / Revelation Song / 4447960 → "Kari-Jobe-Revelation-Song-4447960"
 *   Bethel Music / This Is Amazing Grace / ""  → "Bethel-Music-This-Is-Amazing-Grace"
 *   "" / Amazing Grace / "" → "Amazing-Grace"
 */
private fun buildExportFilename(artist: String, title: String, ccli: String): String {
    val parts = mutableListOf<String>()
    if (artist.isNotBlank()) parts.add(sanitize(artist))
    if (title.isNotBlank()) parts.add(sanitize(title))
    if (ccli.isNotBlank()) parts.add(sanitize(ccli))

    val name = parts.joinToString("-").trim('-')
    return name.ifBlank { "lyrics_slides" }
}

/**
 * Sanitize a string for use in a filename.
 * Strips filesystem-unsafe characters and collapses whitespace into single hyphens.
 */
private fun sanitize(input: String): String {
    return input.trim()
        // Remove characters that are illegal in filenames on any major OS
        .replace(Regex("""[\\/:*?"<>|]"""), "")
        // Collapse any run of whitespace or dashes into a single hyphen
        .replace(Regex("""[\s-]+"""), "-")
        .trim('-')
}
