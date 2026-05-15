package com.lyricslides.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.lyricslides.service.OnlineTemplate
import com.lyricslides.service.OnlineTemplateService
import com.lyricslides.viewmodel.AppViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO

@Composable
fun OnlineTemplateBrowser(
    viewModel: AppViewModel,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val templateService = remember { OnlineTemplateService() }

    var templates by remember { mutableStateOf<List<OnlineTemplate>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var downloadingTemplate by remember { mutableStateOf<String?>(null) }

    // Fetch template list on first display
    LaunchedEffect(Unit) {
        val result = withContext(Dispatchers.IO) {
            templateService.fetchTemplateList()
        }
        result.onSuccess {
            templates = it
            isLoading = false
        }.onFailure {
            error = it.message
            isLoading = false
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "LibreOffice Template Backgrounds",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            "Click a template to download its background",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                when {
                    isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator()
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("Loading templates from GitHub...")
                            }
                        }
                    }
                    error != null -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Failed to load: $error",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    else -> {
                        Text(
                            "${templates.size} templates available",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 180.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(templates) { template ->
                                OnlineTemplateCard(
                                    template = template,
                                    templateService = templateService,
                                    isDownloading = downloadingTemplate == template.name,
                                    onDownload = {
                                        downloadingTemplate = template.name
                                        scope.launch {
                                            val path = withContext(Dispatchers.IO) {
                                                templateService.downloadTemplateBackground(template)
                                            }
                                            if (path != null) {
                                                viewModel.addCustomBackground(path)
                                            }
                                            downloadingTemplate = null
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OnlineTemplateCard(
    template: OnlineTemplate,
    templateService: OnlineTemplateService,
    isDownloading: Boolean,
    onDownload: () -> Unit
) {
    var thumbnail by remember(template.thumbnailUrl) { mutableStateOf<ImageBitmap?>(null) }

    // Load thumbnail
    LaunchedEffect(template.thumbnailUrl) {
        val bytes = withContext(Dispatchers.IO) {
            templateService.fetchThumbnailBytes(template.thumbnailUrl)
        }
        if (bytes != null) {
            val img = withContext(Dispatchers.IO) {
                try {
                    ImageIO.read(ByteArrayInputStream(bytes))?.toComposeImageBitmap()
                } catch (_: Exception) { null }
            }
            thumbnail = img
        }
    }

    Card(
        modifier = Modifier
            .aspectRatio(16f / 9f)
            .clickable(enabled = !isDownloading, onClick = onDownload),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Box(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp))) {
            if (thumbnail != null) {
                Image(
                    bitmap = thumbnail!!,
                    contentDescription = template.displayName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                }
            }

            // Overlay with name and download indicator
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = template.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        color = androidx.compose.ui.graphics.Color.White,
                        modifier = Modifier.weight(1f)
                    )
                    if (isDownloading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                            color = androidx.compose.ui.graphics.Color.White
                        )
                    } else {
                        Icon(
                            Icons.Default.Download,
                            contentDescription = "Download",
                            modifier = Modifier.size(16.dp),
                            tint = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}
