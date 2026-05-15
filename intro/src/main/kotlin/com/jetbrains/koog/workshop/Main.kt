package com.jetbrains.koog.workshop

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import java.awt.Dimension

fun main() = application {
    Window(
        title = "Koog Agents Workshop",
        state = rememberWindowState(width = 1200.dp, height = 800.dp),
        onCloseRequest = ::exitApplication,
    ) {
        window.minimumSize = Dimension(350, 600)
        KoinApp()
    }
}
