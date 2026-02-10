package syncer

interface AdvertisementSyncer {
    // Запускает периодическую задачу по обновлению конфига и роликов
    // Вызывается один раз при старте приложения
    fun schedulePeriodicSync()

    // Делаем единоразовый запрос к Worker'у для получения конфига
    suspend fun syncNow()
}