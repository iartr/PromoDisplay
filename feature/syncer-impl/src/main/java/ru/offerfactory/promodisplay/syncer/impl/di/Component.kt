package ru.offerfactory.promodisplay.syncer.impl.di

import dagger.Component
import ru.offerfactory.promodisplay.syncer.impl.domain.repository.ConfigRepository
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        RepositoryModule::class
    ]
)
interface AppComponent {
    fun configRepository(): ConfigRepository
}