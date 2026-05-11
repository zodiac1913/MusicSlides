package com.lyricslides

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.lyricslides.ui.LyricSlidesApp
import com.lyricslides.ui.theme.LyricSlidesTheme

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "LyricSlides",
        state = rememberWindowState(width = 1200.dp, height = 800.dp)
    ) {
        LyricSlidesTheme {
            LyricSlidesApp()
        }
    }
}
