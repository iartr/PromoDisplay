package ru.offerfactory.promodisplay.di

import android.content.Context
import dagger.Module
import dagger.Provides
import ru.offerfactory.promodisplay.player.api.PlayerApi
import ru.offerfactory.promodisplay.player.impl.PlayerApiFactory
import javax.inject.Singleton

@Module
object PlayerModule {

    @Provides
    @Singleton
    fun providePlayerApi(context: Context): PlayerApi {
        // Важно: используем applicationContext внутри фабрики
        return PlayerApiFactory.create(context)
    }
}