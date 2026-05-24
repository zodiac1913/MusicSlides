package com.lyricslides.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.lyricslides.service.SlideGenerator
import com.lyricslides.viewmodel.AppViewModel

@Composable
fun EditScreen(viewModel: AppViewModel) {
    val lyrics by viewModel.lyrics.collectAsState()
    val lyricsSource by viewModel.lyricsSource.collectAsState()
    val artist by viewModel.artist.collectAsState()
    val title by viewModel.title.collectAsState()
    val ccliNumber by viewModel.ccliNumber.collectAsState()

    // Use TextFieldValue to avoid the Compose Desktop first-keystroke swap bug.
    var lyricsTfv by remember { mutableStateOf(TextFieldValue(lyrics)) }
    LaunchedEffect(lyrics) {
        if (lyrics != lyricsTfv.text) {
            lyricsTfv = TextFieldValue(lyrics, TextRange(lyrics.length))
        }
    }

    val slideGenerator = remember { SlideGenerator() }
    val slideGroups = remember(lyrics) { slideGenerator.parseLyricsIntoSlides(lyrics) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Edit Lyrics",
                    style = MaterialTheme.typography.headlineMedium
                )
                if (artist.isNotBlank() || title.isNotBlank()) {
                    Text(
                        text = "$title by $artist",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                }
                Text(
                    text = "Source: $lyricsSource • ${slideGroups.size} lyrics slides" +
                        if (ccliNumber.isNotBlank()) " • CCLI # $ccliNumber" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { viewModel.previousStep() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Back")
                }
                Button(onClick = { viewModel.nextStep() }) {
                    Text("Choose Background")
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Lyrics editor
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Lyrics (separate slides with blank lines)",
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = lyricsTfv,
                    onValueChange = { newValue ->
                        lyricsTfv = newValue
                        viewModel.updateLyrics(newValue.text)
                    },
                    modifier = Modifier.fillMaxSize(),
                    textStyle = MaterialTheme.typography.bodyMedium
                )
            }

            // Slide preview panel
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Slide Breakdown Preview",
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(modifier = Modifier.height(8.dp))

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    slideGroups.forEachIndexed { index, lines ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Slide ${index + 1}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                lines.forEach { line ->
                                    Text(
                                        text = line,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
