package ru.offerfactory.promodisplay.syncer.impl.domain.usecase

import ru.offerfactory.promodisplay.settings.domain.model.ConfigEntity

interface ApplyRemoteConfigUseCase {
    suspend fun applyIfChanged(newConfig: ConfigEntity)
}