package ru.offerfactory.promodisplay.player.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.PlayerView
import ru.offerfactory.promodisplay.player.api.ui.PlayerScreen

internal class PlayerScreenImpl(
    private val engine: PlayerEngine
) : PlayerScreen {

    @Composable
    override fun Content(modifier: Modifier) {
        val player by engine.playerState.collectAsState()

        AndroidView(
            modifier = Modifier,
            factory = { context ->
                PlayerView(context).apply {
                    useController = false
                    this.player = player
                }
            },
            update = { view ->
                view.player = player
            }
        )
    }
}