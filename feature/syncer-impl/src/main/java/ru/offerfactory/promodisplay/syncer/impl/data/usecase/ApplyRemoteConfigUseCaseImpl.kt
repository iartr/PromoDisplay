package ru.offerfactory.promodisplay.syncer.impl.data.usecase

import kotlinx.coroutines.flow.firstOrNull
import ru.offerfactory.promodisplay.settings.ConfigManager
import ru.offerfactory.promodisplay.settings.domain.model.ConfigEntity
import ru.offerfactory.promodisplay.syncer.impl.domain.usecase.ApplyRemoteConfigUseCase

class ApplyRemoteConfigUseCaseImpl(
    private val configManager: ConfigManager
) : ApplyRemoteConfigUseCase {

    override suspend fun applyIfChanged(newConfig: ConfigEntity) {
        val currentConfig = configManager.getConfig().firstOrNull()

        if (currentConfig?.items != newConfig.items) {
            configManager.saveConfig(newConfig)
        }
    }
}