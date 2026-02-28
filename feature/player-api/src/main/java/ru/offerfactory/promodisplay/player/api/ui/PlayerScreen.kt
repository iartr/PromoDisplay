package ru.offerfactory.promodisplay.player.api.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

interface PlayerScreen {
    @Composable
    fun Content(modifier: Modifier = Modifier)
}