package ru.offerfactory.promodisplay

import android.app.Application
import ru.offerfactory.promodisplay.di.AppComponent
import ru.offerfactory.promodisplay.di.DaggerAppComponent

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

class Application : Application(), HasTracerConfiguration {

    lateinit var appComponent: AppComponent
    lateinit var syncerComponent: SyncerComponent

    override fun onCreate() {
        super.onCreate()

        appComponent = DaggerAppComponent.factory().create(context = this)

        syncerComponent = DaggerSyncerComponent.factory()
            .create(
                context = this,
                workManager = appComponent.workManager()
            )

        val workManager = syncerComponent.advertisementSyncer()
    }

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