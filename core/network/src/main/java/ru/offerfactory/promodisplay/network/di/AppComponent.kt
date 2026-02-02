package ru.offerfactory.promodisplay.network.di

import dagger.Component
import ru.offerfactory.promodisplay.network.domain.repository.ConfigRepository
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        NetworkModule::class,
        RepositoryModule::class
    ]
)
interface AppComponent {
    fun configRepository(): ConfigRepository
}