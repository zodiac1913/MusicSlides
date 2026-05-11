package com.lyricslides.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lyricslides.service.SlideGenerator
import com.lyricslides.viewmodel.AppViewModel

@Composable
fun GenerateScreen(viewModel: AppViewModel) {
    val lyrics by viewModel.lyrics.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val progress by viewModel.generationProgress.collectAsState()
    val selectedBackground by viewModel.selectedBackground.collectAsState()

    val slideGenerator = remember { SlideGenerator() }
    val slideCount = remember(lyrics) { slideGenerator.parseLyricsIntoSlides(lyrics).size }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.AutoAwesome,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Ready to Generate",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Your presentation will have ${slideCount + 2} slides",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "(1 title + $slideCount lyrics + 1 closing)",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Background: ${selectedBackground?.name ?: "Default Gradient"}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Generation info card
        Card(
            modifier = Modifier.widthIn(max = 500.dp).fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("What happens next:", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(8.dp))
                Text("• Title slide with song name, artist & CCLI #")
                Text("• Text will be maximized to fill each slide")
                Text("• Contrast will be checked for accessibility (WCAG AA)")
                Text("• Text color will be adjusted if needed for readability")
                Text("• A text shadow will be added for extra legibility")
                Text("• Closing slide with full CCLI/legal reference")
                Text("• Slides exported at 1920×1080 (Full HD)")
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        if (isGenerating) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.widthIn(max = 400.dp).fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Generating slides... ${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = { viewModel.previousStep() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Back")
                }

                Button(
                    onClick = { viewModel.generateSlides() }
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Generate Slides")
                }
            }
        }
    }
}
