package ru.offerfactory.promodisplay.player.api

import kotlinx.coroutines.flow.Flow
import ru.offerfactory.promodisplay.player.api.model.Clip

interface PlayerFeature {

    fun attach(clipsFlow: Flow<List<Clip>>)

    fun detach()

    fun resumePlayback()

    fun pausePlayback()
}