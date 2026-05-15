package com.lyricslides.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.lyricslides.model.SlideBackground
import com.lyricslides.service.BackgroundProvider
import com.lyricslides.viewmodel.AppViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

@Composable
fun BackgroundScreen(viewModel: AppViewModel) {
    val backgrounds by viewModel.availableBackgrounds.collectAsState()
    val selectedBackground by viewModel.selectedBackground.collectAsState()
    val backgroundProvider = remember { viewModel.getBackgroundProvider() }

    val categories = remember(backgrounds) {
        backgrounds.map { it.category }.distinct()
    }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var showOnlineBrowser by remember { mutableStateOf(false) }

    val filteredBackgrounds = remember(backgrounds, selectedCategory) {
        if (selectedCategory == null) backgrounds
        else backgrounds.filter { it.category == selectedCategory }
    }

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
                    text = "Choose Background",
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = "Select a background for your lyric slides",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { viewModel.previousStep() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Back")
                }
                Button(
                    onClick = { viewModel.nextStep() }
                ) {
                    Text("Generate Slides")
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Category filter and import button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = selectedCategory == null,
                onClick = { selectedCategory = null },
                label = { Text("All") }
            )
            categories.forEach { category ->
                FilterChip(
                    selected = selectedCategory == category,
                    onClick = { selectedCategory = category },
                    label = { Text(category) }
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Browse LibreOffice templates online
            OutlinedButton(
                onClick = { showOnlineBrowser = true }
            ) {
                Icon(Icons.Default.CloudDownload, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Browse Online")
            }

            // Import custom background image
            Button(
                onClick = {
                    val chooser = JFileChooser()
                    chooser.fileFilter = FileNameExtensionFilter(
                        "Images & Templates", "png", "jpg", "jpeg", "bmp", "gif", "otp"
                    )
                    chooser.dialogTitle = "Select Background Image or .otp Template"
                    chooser.isMultiSelectionEnabled = true
                    val result = chooser.showOpenDialog(null)
                    if (result == JFileChooser.APPROVE_OPTION) {
                        chooser.selectedFiles.forEach { file ->
                            if (file.name.endsWith(".otp", ignoreCase = true)) {
                                viewModel.importFromOtp(file.absolutePath)
                            } else {
                                viewModel.addCustomBackground(file.absolutePath)
                            }
                        }
                    }
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Import File")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // No background option
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { viewModel.selectBackground(null) }
                .then(
                    if (selectedBackground == null)
                        Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                    else Modifier
                ),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.FormatColorFill, contentDescription = null)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("No Background (Default Gradient)", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Plain dark gradient — no image",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Background grid
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 240.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(filteredBackgrounds) { background ->
                BackgroundCard(
                    background = background,
                    isSelected = selectedBackground == background,
                    backgroundProvider = backgroundProvider,
                    onClick = { viewModel.selectBackground(background) },
                    onDelete = if (!background.isBuiltIn) {
                        { viewModel.removeBackground(background) }
                    } else null
                )
            }
        }
    }

    // Online template browser dialog
    if (showOnlineBrowser) {
        OnlineTemplateBrowser(
            viewModel = viewModel,
            onDismiss = { showOnlineBrowser = false }
        )
    }
}

@Composable
fun BackgroundCard(
    background: SlideBackground,
    isSelected: Boolean,
    backgroundProvider: BackgroundProvider,
    onClick: () -> Unit,
    onDelete: (() -> Unit)? = null
) {
    // Load thumbnail asynchronously so the UI stays responsive
    var thumbnail by remember(background.path) { mutableStateOf<ImageBitmap?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(background.path) {
        val thumb = withContext(Dispatchers.IO) {
            try {
                backgroundProvider.loadThumbnail(background)?.toComposeImageBitmap()
            } catch (_: Exception) {
                null
            }
        }
        thumbnail = thumb
    }

    // Delete confirmation dialog
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Remove Background") },
            text = { Text("Remove \"${background.name}\" from the list?") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onDelete?.invoke()
                }) {
                    Text("Remove", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Card(
        modifier = Modifier
            .aspectRatio(16f / 9f)
            .clickable(onClick = onClick)
            .then(
                if (isSelected)
                    Modifier.border(3.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                else Modifier
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp))
        ) {
            // Thumbnail or loading placeholder
            if (thumbnail != null) {
                Image(
                    bitmap = thumbnail!!,
                    contentDescription = background.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                }
            }

            // Name overlay at bottom
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.5f)
                    )
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Column {
                    Text(
                        text = background.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = androidx.compose.ui.graphics.Color.White
                    )
                    Text(
                        text = background.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f)
                    )
                }
            }

            // Top-right: checkmark if selected, delete button if custom
            if (isSelected) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            } else if (onDelete != null) {
                IconButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Remove ${background.name}",
                        tint = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
