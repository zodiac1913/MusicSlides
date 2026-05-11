package com.lyricslides.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.lyricslides.viewmodel.AppViewModel

@Composable
fun SearchScreen(viewModel: AppViewModel) {
    val artist by viewModel.artist.collectAsState()
    val title by viewModel.title.collectAsState()
    val ccliNumber by viewModel.ccliNumber.collectAsState()
    val copyright by viewModel.copyright.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val searchError by viewModel.searchError.collectAsState()
    val isSearchingCcli by viewModel.isSearchingCcli.collectAsState()
    val ccliSearchResults by viewModel.ccliSearchResults.collectAsState()
    val ccliSearchError by viewModel.ccliSearchError.collectAsState()

    val songQuery by viewModel.songQuery.collectAsState()
    val suggestions by viewModel.suggestions.collectAsState()
    val isLoadingSuggestions by viewModel.isLoadingSuggestions.collectAsState()

    // Local text-field state using TextFieldValue to avoid the first-keystroke
    // character-swap bug that occurs when binding a TextField directly to a StateFlow.
    var songQueryTfv by remember { mutableStateOf(TextFieldValue(songQuery)) }
    var artistTfv by remember { mutableStateOf(TextFieldValue(artist)) }
    var titleTfv by remember { mutableStateOf(TextFieldValue(title)) }
    var ccliTfv by remember { mutableStateOf(TextFieldValue(ccliNumber)) }
    var copyrightTfv by remember { mutableStateOf(TextFieldValue(copyright)) }

    // If the ViewModel updates these values from elsewhere (e.g. selecting a
    // suggestion), sync them back into the local TextFieldValue state.
    LaunchedEffect(songQuery) {
        if (songQuery != songQueryTfv.text) {
            songQueryTfv = TextFieldValue(songQuery, TextRange(songQuery.length))
        }
    }
    LaunchedEffect(artist) {
        if (artist != artistTfv.text) {
            artistTfv = TextFieldValue(artist, TextRange(artist.length))
        }
    }
    LaunchedEffect(title) {
        if (title != titleTfv.text) {
            titleTfv = TextFieldValue(title, TextRange(title.length))
        }
    }
    LaunchedEffect(ccliNumber) {
        if (ccliNumber != ccliTfv.text) {
            ccliTfv = TextFieldValue(ccliNumber, TextRange(ccliNumber.length))
        }
    }
    LaunchedEffect(copyright) {
        if (copyright != copyrightTfv.text) {
            copyrightTfv = TextFieldValue(copyright, TextRange(copyright.length))
        }
    }

    var showManualEntry by remember { mutableStateOf(false) }
    var manualLyrics by remember { mutableStateOf(TextFieldValue("")) }
    var showAdvanced by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Find Song Lyrics",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Type a song title (or artist name) to search",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Smart search card with autocomplete
        Card(
            modifier = Modifier.widthIn(max = 700.dp).fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Main search field — just song title or "title artist"
                OutlinedTextField(
                    value = songQueryTfv,
                    onValueChange = { newValue ->
                        songQueryTfv = newValue
                        viewModel.updateSongQuery(newValue.text)
                    },
                    label = { Text("Song title (or title + artist)") },
                    placeholder = { Text("e.g. Revelation Song, Goodness of God, Amazing Grace") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null)
                    },
                    trailingIcon = {
                        when {
                            isLoadingSuggestions -> CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                            songQueryTfv.text.isNotBlank() -> IconButton(onClick = {
                                songQueryTfv = TextFieldValue("")
                                viewModel.updateSongQuery("")
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    }
                )

                // Autocomplete suggestions dropdown
                if (suggestions.isNotEmpty()) {
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .heightIn(max = 350.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            suggestions.forEach { suggestion ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.selectSuggestion(suggestion) }
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = suggestion.title,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = suggestion.artist,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                .copy(alpha = 0.7f)
                                        )
                                    }
                                    Text(
                                        text = suggestion.source,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                        .copy(alpha = 0.1f)
                                )
                            }
                        }
                    }
                }

                // Show suggestion helper text
                if (suggestions.isEmpty() && songQueryTfv.text.length >= 2 && !isLoadingSuggestions) {
                    Text(
                        text = "No matches yet — keep typing, or use Advanced below.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }

                // Advanced toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { showAdvanced = !showAdvanced }) {
                        Text(if (showAdvanced) "Hide advanced" else "Advanced (manual fields)")
                    }

                    OutlinedButton(
                        onClick = { showManualEntry = !showManualEntry }
                    ) {
                        Icon(Icons.Default.ContentPaste, contentDescription = null,
                            modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Paste Lyrics")
                    }
                }
            }
        }

        // Advanced fields (CCLI, copyright, manual artist/title search)
        if (showAdvanced) {
            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.widthIn(max = 700.dp).fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Advanced",
                        style = MaterialTheme.typography.titleMedium
                    )

                    // Artist + Title manual entry
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = artistTfv,
                            onValueChange = { newValue ->
                                artistTfv = newValue
                                viewModel.updateArtist(newValue.text)
                            },
                            label = { Text("Artist") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = titleTfv,
                            onValueChange = { newValue ->
                                titleTfv = newValue
                                viewModel.updateTitle(newValue.text)
                            },
                            label = { Text("Song Title") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    // Manual search button (uses artist + title directly)
                    Button(
                        onClick = { viewModel.searchLyrics() },
                        enabled = artistTfv.text.isNotBlank() && titleTfv.text.isNotBlank() && !isSearching,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isSearching) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Searching...")
                        } else {
                            Icon(Icons.Default.Search, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Search by Artist + Title")
                        }
                    }

                    HorizontalDivider()

                    // CCLI row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        OutlinedTextField(
                            value = ccliTfv,
                            onValueChange = { newValue ->
                                ccliTfv = newValue
                                viewModel.updateCcliNumber(newValue.text)
                            },
                            label = { Text("CCLI Song #") },
                            placeholder = { Text("e.g. 4447960") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            supportingText = { Text("Shown on title & closing slides") },
                            trailingIcon = {
                                if (isSearchingCcli) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp
                                    )
                                }
                            }
                        )

                        OutlinedButton(
                            onClick = { viewModel.searchCcli() },
                            enabled = titleTfv.text.isNotBlank() && !isSearchingCcli,
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Icon(Icons.Default.Tag, contentDescription = null,
                                modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Find #")
                        }
                    }

                    OutlinedTextField(
                        value = copyrightTfv,
                        onValueChange = { newValue ->
                            copyrightTfv = newValue
                            viewModel.updateCopyright(newValue.text)
                        },
                        label = { Text("Copyright (optional)") },
                        placeholder = { Text("e.g. © 2004 Gateway Create Publishing") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Helper: open SongSelect in browser to look up CCLI manually
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Text(
                            text = "Don't know the CCLI #?",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        TextButton(
                            onClick = {
                                val query = "${titleTfv.text} ${artistTfv.text}".trim()
                                    .replace(" ", "+")
                                val url = if (query.isNotBlank())
                                    "https://songselect.ccli.com/search/results?SearchText=$query"
                                else
                                    "https://songselect.ccli.com/"
                                try {
                                    java.awt.Desktop.getDesktop().browse(java.net.URI(url))
                                } catch (_: Exception) {}
                            },
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                horizontal = 8.dp, vertical = 0.dp
                            )
                        ) {
                            Text("Look up on SongSelect")
                        }
                        Text(
                            text = "(remembered next time)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }

                    // CCLI search results
                    if (ccliSearchResults.isNotEmpty()) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "CCLI Results (click to select):",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                ccliSearchResults.forEach { result ->
                                    Text(
                                        text = "# ${result.ccliNumber} — ${result.songTitle}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { viewModel.selectCcliResult(result) }
                                            .padding(vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    ccliSearchError?.let { error ->
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        // Error display
        searchError?.let { error ->
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                modifier = Modifier.widthIn(max = 700.dp).fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        // Manual lyrics entry
        if (showManualEntry) {
            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.widthIn(max = 700.dp).fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Paste Lyrics Below",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text(
                        text = "Separate verses/sections with blank lines — each section becomes one slide",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )

                    OutlinedTextField(
                        value = manualLyrics,
                        onValueChange = { manualLyrics = it },
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        placeholder = { Text("Paste lyrics here...") }
                    )

                    Button(
                        onClick = { viewModel.setManualLyrics(manualLyrics.text) },
                        enabled = manualLyrics.text.isNotBlank()
                    ) {
                        Text("Use These Lyrics")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
