package ru.offerfactory.promodisplay.player.api

import ru.offerfactory.promodisplay.player.api.ui.PlayerScreen

interface PlayerApi {

    fun feature(): PlayerFeature

    fun screen(): PlayerScreen
}