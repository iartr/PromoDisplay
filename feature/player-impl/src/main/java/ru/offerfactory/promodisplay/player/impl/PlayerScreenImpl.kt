package ru.offerfactory.promodisplay.player.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.PlayerView
import ru.offerfactory.promodisplay.player.api.ui.PlayerScreen

internal class PlayerScreenImpl(
    private val engine: PlayerEngine
) : PlayerScreen {

    @Composable
    override fun Content(modifier: Modifier) {
        // Safety-net: если экран исчез — освобождаем ресурсы
        DisposableEffect(Unit) {
            onDispose { engine.detach() }
        }

        AndroidView(
            modifier = modifier,
            factory = { context ->
                PlayerView(context).apply {
                    useController = false
                    player = engine.player
                }
            },
            update = { view ->
                // На случай, если player создался позже (после attach)
                view.player = engine.player
            }
        )
    }
}