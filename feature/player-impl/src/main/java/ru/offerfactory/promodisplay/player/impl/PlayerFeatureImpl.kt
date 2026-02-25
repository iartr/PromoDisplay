package ru.offerfactory.promodisplay.player.impl

import kotlinx.coroutines.flow.Flow
import ru.offerfactory.promodisplay.player.api.PlayerFeature
import ru.offerfactory.promodisplay.player.api.model.Clip

internal class PlayerFeatureImpl(
    private val engine: PlayerEngine
) : PlayerFeature {

    override fun attach(clipsFlow: Flow<List<Clip>>) {
        engine.attach(clipsFlow)
    }

    override fun detach() {
        engine.detach()
    }
}