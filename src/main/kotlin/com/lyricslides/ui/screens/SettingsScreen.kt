package com.lyricslides.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.lyricslides.service.StoredCredentials
import com.lyricslides.viewmodel.AppViewModel

@Composable
fun SettingsScreen(viewModel: AppViewModel, onBack: () -> Unit) {
    val settings by viewModel.settings.collectAsState()
    val credentials by viewModel.credentials.collectAsState()

    var ccliUsername by remember(credentials) { mutableStateOf(TextFieldValue(credentials.ccliUsername)) }
    var ccliPassword by remember(credentials) { mutableStateOf(TextFieldValue(credentials.ccliPassword)) }
    var ccliLicense by remember(credentials) { mutableStateOf(TextFieldValue(credentials.ccliLicenseNumber)) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Settings", style = MaterialTheme.typography.headlineMedium)
            OutlinedButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Back")
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Slide Layout Settings
        Card(
            modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Text("Slide Layout", style = MaterialTheme.typography.titleMedium)

                // Max lines per slide
                SettingRow(
                    title = "Max lines per slide",
                    description = "Slides with more lines will be split evenly",
                    value = "${settings.maxLinesPerSlide}",
                    onDecrease = {
                        if (settings.maxLinesPerSlide > 2)
                            viewModel.updateSettings(settings.copy(maxLinesPerSlide = settings.maxLinesPerSlide - 1))
                    },
                    onIncrease = {
                        if (settings.maxLinesPerSlide < 12)
                            viewModel.updateSettings(settings.copy(maxLinesPerSlide = settings.maxLinesPerSlide + 1))
                    }
                )

                HorizontalDivider()

                // Maximize text size toggle
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
                        onCheckedChange = { viewModel.updateSettings(settings.copy(allowLineBreaking = it)) }
                    )
                }

                HorizontalDivider()

                // Vertical percentage
                SettingRow(
                    title = "Vertical slide usage",
                    description = "How much of the slide height to use for text",
                    value = "${settings.verticalPercent}%",
                    onDecrease = {
                        if (settings.verticalPercent > 50)
                            viewModel.updateSettings(settings.copy(verticalPercent = settings.verticalPercent - 5))
                    },
                    onIncrease = {
                        if (settings.verticalPercent < 100)
                            viewModel.updateSettings(settings.copy(verticalPercent = settings.verticalPercent + 5))
                    }
                )

                HorizontalDivider()

                // Horizontal percentage
                SettingRow(
                    title = "Horizontal slide usage",
                    description = "How much of the slide width to use for text",
                    value = "${settings.horizontalPercent}%",
                    onDecrease = {
                        if (settings.horizontalPercent > 50)
                            viewModel.updateSettings(settings.copy(horizontalPercent = settings.horizontalPercent - 5))
                    },
                    onIncrease = {
                        if (settings.horizontalPercent < 100)
                            viewModel.updateSettings(settings.copy(horizontalPercent = settings.horizontalPercent + 5))
                    }
                )

                HorizontalDivider()

                // Text backdrop (scrim) size
                SettingRow(
                    title = "Text backdrop size",
                    description = "Dark overlay behind text on busy backgrounds (0% = off)",
                    value = if (settings.scrimPercent == 0) "Off" else "${settings.scrimPercent}%",
                    onDecrease = {
                        if (settings.scrimPercent > 0)
                            viewModel.updateSettings(settings.copy(scrimPercent = settings.scrimPercent - 10))
                    },
                    onIncrease = {
                        if (settings.scrimPercent < 100)
                            viewModel.updateSettings(settings.copy(scrimPercent = settings.scrimPercent + 10))
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // CCLI / SongSelect Credentials
        Card(
            modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("CCLI SongSelect Login", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Enter your CCLI SongSelect credentials to search their worship song database. " +
                        "Credentials are stored locally on this device only — never uploaded or shared.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                OutlinedTextField(
                    value = ccliLicense,
                    onValueChange = { ccliLicense = it },
                    label = { Text("CCLI License Number") },
                    placeholder = { Text("e.g. 1234567") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = ccliUsername,
                    onValueChange = { ccliUsername = it },
                    label = { Text("SongSelect Username (email)") },
                    placeholder = { Text("worship@yourchurch.org") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = ccliPassword,
                    onValueChange = { ccliPassword = it },
                    label = { Text("SongSelect Password") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = {
                            viewModel.saveCcliCredentials(
                                StoredCredentials(
                                    ccliUsername = ccliUsername.text,
                                    ccliPassword = ccliPassword.text,
                                    ccliLicenseNumber = ccliLicense.text
                                )
                            )
                        }
                    ) {
                        Text("Save Credentials")
                    }

                    OutlinedButton(
                        onClick = {
                            viewModel.clearCcliCredentials()
                            ccliUsername = TextFieldValue("")
                            ccliPassword = TextFieldValue("")
                            ccliLicense = TextFieldValue("")
                        }
                    ) {
                        Text("Clear")
                    }
                }

                if (credentials.ccliUsername.isNotBlank()) {
                    Text(
                        "✓ Credentials saved for ${credentials.ccliUsername}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun SettingRow(
    title: String,
    description: String,
    value: String,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onDecrease,
                modifier = Modifier.size(40.dp),
                contentPadding = PaddingValues(0.dp)
            ) { Text("−") }

            Text(value, style = MaterialTheme.typography.titleLarge)

            OutlinedButton(
                onClick = onIncrease,
                modifier = Modifier.size(40.dp),
                contentPadding = PaddingValues(0.dp)
            ) { Text("+") }
        }
    }
}
