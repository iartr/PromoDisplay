package ru.offerfactory.promodisplay.player.impl

import ru.offerfactory.promodisplay.player.api.PlayerApi
import ru.offerfactory.promodisplay.player.api.PlayerFeature
import ru.offerfactory.promodisplay.player.api.ui.PlayerScreen

internal class PlayerApiImpl(
    private val feature: PlayerFeature,
    private val screen: PlayerScreen
) : PlayerApi {

    override fun feature(): PlayerFeature = feature

    override fun screen(): PlayerScreen = screen
}