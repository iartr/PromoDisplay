package ru.offerfactory.promodisplay

import android.app.Application
import androidx.work.Configuration
import androidx.work.WorkManager
import ru.offerfactory.promodisplay.syncer.impl.di.DaggerSyncerComponent
import ru.offerfactory.promodisplay.syncer.impl.di.SyncerComponent
import ru.ok.tracer.CoreTracerConfiguration
import ru.ok.tracer.HasTracerConfiguration
import ru.ok.tracer.TracerConfiguration
import ru.ok.tracer.crash.report.CrashFreeConfiguration
import ru.ok.tracer.crash.report.CrashReportConfiguration
import ru.ok.tracer.disk.usage.DiskUsageConfiguration
import ru.ok.tracer.heap.dumps.HeapDumpConfiguration
import ru.ok.tracer.profiler.sampling.SamplingProfilerConfiguration
import ru.ok.tracer.profiler.systrace.SystraceProfilerConfiguration

class Application : Application(), HasTracerConfiguration, Configuration.Provider {

    lateinit var syncerComponent: SyncerComponent

    override fun onCreate() {
        super.onCreate()

        syncerComponent = DaggerSyncerComponent.factory().create(context = this)

        WorkManager.initialize(this, workManagerConfiguration)

        syncerComponent.advertisementSyncer().schedulePeriodicSync()
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(syncerComponent.syncWorkerFactory())
            .build()

    override val tracerConfiguration: List<TracerConfiguration>
        get() = listOf(
            CoreTracerConfiguration.build {
                // опции ядра трейсера
            },
            CrashReportConfiguration.build {
                // опции сборщика крэшей
            },
            CrashFreeConfiguration.build {
                // опции подсчета crash free
            },
            HeapDumpConfiguration.build {
                // опции сборщика хипдампов при ООМ
            },
            DiskUsageConfiguration.build {
                // опции анализатора дискового пространства
            },
            SystraceProfilerConfiguration.build {
                // опции systrace-профайлера в продакшене
            },
            SamplingProfilerConfiguration.build {
                // опции семплирующего профайлера
            },
        )
}