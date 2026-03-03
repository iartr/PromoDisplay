package ru.offerfactory.promodisplay.player.api.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.UiComposable

interface PlayerScreen {
    @Composable
    @UiComposable
    fun Content(modifier: Modifier)
}