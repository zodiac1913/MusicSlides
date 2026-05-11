package com.lyricslides.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lyricslides.model.AppStep
import com.lyricslides.ui.screens.*
import com.lyricslides.viewmodel.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricSlidesApp() {
    val viewModel = remember { AppViewModel() }
    val currentStep by viewModel.currentStep.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("LyricSlides") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            StepIndicator(currentStep, viewModel)
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (currentStep) {
                AppStep.SEARCH -> SearchScreen(viewModel)
                AppStep.EDIT -> EditScreen(viewModel)
                AppStep.BACKGROUND -> BackgroundScreen(viewModel)
                AppStep.GENERATE -> GenerateScreen(viewModel)
                AppStep.PREVIEW -> PreviewScreen(viewModel)
            }
        }
    }
}

@Composable
fun StepIndicator(currentStep: AppStep, viewModel: AppViewModel) {
    val steps = AppStep.entries

    NavigationBar {
        steps.forEachIndexed { index, step ->
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = when (step) {
                            AppStep.SEARCH -> Icons.Default.Search
                            AppStep.EDIT -> Icons.Default.Edit
                            AppStep.BACKGROUND -> Icons.Default.Image
                            AppStep.GENERATE -> Icons.Default.AutoAwesome
                            AppStep.PREVIEW -> Icons.Default.Slideshow
                        },
                        contentDescription = step.name
                    )
                },
                label = { Text(step.name.lowercase().replaceFirstChar { it.uppercase() }) },
                selected = currentStep == step,
                onClick = { viewModel.goToStep(step) }
            )
        }
    }
}
