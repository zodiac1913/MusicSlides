package com.lyricslides.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lyricslides.service.AppSettings
import com.lyricslides.viewmodel.AppViewModel

@Composable
fun SettingsScreen(viewModel: AppViewModel, onBack: () -> Unit) {
    val settings by viewModel.settings.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium
            )
            OutlinedButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Back")
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Card(
            modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Text(
                    text = "Slide Layout",
                    style = MaterialTheme.typography.titleMedium
                )

                // 1. Max lines per slide
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Max lines per slide", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "Slides with more lines will be split evenly",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (settings.maxLinesPerSlide > 2) {
                                    viewModel.updateSettings(settings.copy(
                                        maxLinesPerSlide = settings.maxLinesPerSlide - 1
                                    ))
                                }
                            },
                            modifier = Modifier.size(40.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) { Text("−") }

                        Text(
                            text = "${settings.maxLinesPerSlide}",
                            style = MaterialTheme.typography.titleLarge
                        )

                        OutlinedButton(
                            onClick = {
                                if (settings.maxLinesPerSlide < 12) {
                                    viewModel.updateSettings(settings.copy(
                                        maxLinesPerSlide = settings.maxLinesPerSlide + 1
                                    ))
                                }
                            },
                            modifier = Modifier.size(40.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) { Text("+") }
                    }
                }

                HorizontalDivider()

                // 2. Allow line breaking to maximize text size
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Maximize text size", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "Break long lines to fill more of the slide with larger text",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    Switch(
                        checked = settings.allowLineBreaking,
                        onCheckedChange = { checked ->
                            viewModel.updateSettings(settings.copy(allowLineBreaking = checked))
                        }
                    )
                }

                HorizontalDivider()

                // 3. Vertical percentage
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Vertical slide usage", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "How much of the slide height to use for text",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (settings.verticalPercent > 50) {
                                    viewModel.updateSettings(settings.copy(
                                        verticalPercent = settings.verticalPercent - 5
                                    ))
                                }
                            },
                            modifier = Modifier.size(40.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) { Text("−") }

                        Text(
                            text = "${settings.verticalPercent}%",
                            style = MaterialTheme.typography.titleLarge
                        )

                        OutlinedButton(
                            onClick = {
                                if (settings.verticalPercent < 100) {
                                    viewModel.updateSettings(settings.copy(
                                        verticalPercent = settings.verticalPercent + 5
                                    ))
                                }
                            },
                            modifier = Modifier.size(40.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) { Text("+") }
                    }
                }

                HorizontalDivider()

                // 4. Horizontal percentage
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Horizontal slide usage", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "How much of the slide width to use for text",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (settings.horizontalPercent > 50) {
                                    viewModel.updateSettings(settings.copy(
                                        horizontalPercent = settings.horizontalPercent - 5
                                    ))
                                }
                            },
                            modifier = Modifier.size(40.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) { Text("−") }

                        Text(
                            text = "${settings.horizontalPercent}%",
                            style = MaterialTheme.typography.titleLarge
                        )

                        OutlinedButton(
                            onClick = {
                                if (settings.horizontalPercent < 100) {
                                    viewModel.updateSettings(settings.copy(
                                        horizontalPercent = settings.horizontalPercent + 5
                                    ))
                                }
                            },
                            modifier = Modifier.size(40.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) { Text("+") }
                    }
                }

                HorizontalDivider()

                // 5. Text backdrop (scrim) coverage
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Text backdrop size", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "Dark overlay behind text on busy backgrounds (0% = off)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (settings.scrimPercent > 0) {
                                    viewModel.updateSettings(settings.copy(
                                        scrimPercent = settings.scrimPercent - 10
                                    ))
                                }
                            },
                            modifier = Modifier.size(40.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) { Text("−") }

                        Text(
                            text = if (settings.scrimPercent == 0) "Off" else "${settings.scrimPercent}%",
                            style = MaterialTheme.typography.titleLarge
                        )

                        OutlinedButton(
                            onClick = {
                                if (settings.scrimPercent < 100) {
                                    viewModel.updateSettings(settings.copy(
                                        scrimPercent = settings.scrimPercent + 10
                                    ))
                                }
                            },
                            modifier = Modifier.size(40.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) { Text("+") }
                    }
                }
            }
        }
    }
}
