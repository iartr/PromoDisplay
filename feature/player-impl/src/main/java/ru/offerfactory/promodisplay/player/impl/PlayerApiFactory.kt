package ru.offerfactory.promodisplay.player.impl

import android.content.Context
import ru.offerfactory.promodisplay.player.api.PlayerApi

object PlayerApiFactory {

    fun create(context: Context): PlayerApi {
        val engine = PlayerEngine(appContext = context.applicationContext)

        val feature = PlayerFeatureImpl(engine)
        val screen = PlayerScreenImpl(engine)

        return PlayerApiImpl(feature = feature, screen = screen)
    }
}