package ru.offerfactory.promodisplay

import android.app.Application
import androidx.work.Configuration
import ru.offerfactory.promodisplay.di.AppComponent
import ru.offerfactory.promodisplay.di.DaggerAppComponent
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
    lateinit var appComponent: AppComponent
        private set

    override fun onCreate() {
        super.onCreate()

        appComponent = DaggerAppComponent.factory().create(context = this)

        // Важно: инициализируем ad-source как можно раньше, чтобы:
        // 1) он начал слушать config DataStore
        // 2) при появлении конфигурации — сразу начал качать файлы
        appComponent.advertisementApi()

        appComponent.advertisementSyncer().schedulePeriodicSync()
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(appComponent.syncWorkerFactory())
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