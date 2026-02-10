package ru.offerfactory.promodisplay.syncer.impl.di

import dagger.Component
import ru.offerfactory.promodisplay.network.di.NetworkComponent
import ru.offerfactory.promodisplay.syncer.impl.domain.repository.ConfigRepository
import javax.inject.Singleton

@Singleton
@Component(
    dependencies = [NetworkComponent::class],
    modules = [SyncerDataModule::class]
)
interface SyncerComponent {
    fun configRepository(): ConfigRepository
}