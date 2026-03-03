package ru.offerfactory.promodisplay

import android.app.admin.DevicePolicyManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import ru.offerfactory.promodisplay.ad.source.api.AdvertisementApi
import ru.offerfactory.promodisplay.auto_boot.AdminReceiver
import ru.offerfactory.promodisplay.player.api.PlayerApi
import ru.offerfactory.promodisplay.ui.theme.PromoDisplayTheme

class MainActivity : ComponentActivity() {

    private val appComponent by lazy { (application as ru.offerfactory.promodisplay.Application).appComponent }

    private val playerApi: PlayerApi by lazy { appComponent.playerApi() }
    private val advertisementApi: AdvertisementApi by lazy { appComponent.advertisementApi() }

    // Страховка на случай, если конкретная ТВ-прошивка “просыпается” без ожидаемого пайплайна lifecycle.
    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_ON,
                Intent.ACTION_USER_PRESENT -> {
                    playerApi.feature().resumePlayback()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Для режима 24/7: не гасим экран по таймауту.
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        enableEdgeToEdge()

        setContent {
            PromoDisplayTheme {
                PromoDisplayRoot(
                    playerApi = playerApi,
                    advertisementApi = advertisementApi
                )
            }
        }

        enableAutoStart()
    }

    override fun onStart() {
        super.onStart()

        // Ресивер только пока Activity видима
        registerReceiver(
            screenReceiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_USER_PRESENT)
            }
        )

        // Главное: при возвращении Activity на экран — сразу продолжить воспроизведение
        playerApi.feature().resumePlayback()
    }

    override fun onStop() {
        // Если на устройстве экран выключили/приложение ушло в фон — ставим паузу.
        // (Если захочешь, чтобы в фоне продолжало играть — убери эту строку.)
        playerApi.feature().pausePlayback()

        unregisterReceiver(screenReceiver)

        super.onStop()
    }

    private fun enableAutoStart() {
        val dpm = getSystemService(DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val admin = AdminReceiver.getComponentName(this)

        if (!dpm.isDeviceOwnerApp(packageName)) return
        dpm.setLockTaskPackages(admin, arrayOf(packageName))
    }
}

@Composable
private fun PromoDisplayRoot(
    playerApi: PlayerApi,
    advertisementApi: AdvertisementApi
) {
    // Получаем один Flow клипов на весь срок жизни композиции
    val clipsFlow = remember(advertisementApi) { advertisementApi.getClips() }

    // Только для fallback UI
    val clips by clipsFlow.collectAsState(initial = emptyList())

    // attach/detach выполняются строго один раз на жизненный цикл этого composable.
    DisposableEffect(playerApi, clipsFlow) {
        playerApi.feature().attach(clipsFlow)
        onDispose { playerApi.feature().detach() }
    }

    // Если клипы появились/обновились — пробуем гарантированно запустить проигрывание.
    // Это полезно в кейсах, когда плеер был создан раньше, но play не начался из-за lifecycle/focus.
    LaunchedEffect(clips.size) {
        if (clips.isNotEmpty()) {
            playerApi.feature().resumePlayback()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        playerApi.screen().Content(modifier = Modifier.fillMaxSize())

        if (clips.isEmpty()) {
            FallbackOverlay(modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun FallbackOverlay(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Загрузка роликов…",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White
        )
    }
}