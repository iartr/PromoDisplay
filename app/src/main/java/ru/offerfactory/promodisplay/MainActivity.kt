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
    val clipsFlow = remember(advertisementApi) { advertisementApi.getClips() }
    val progressFlow =
        remember(advertisementApi) { advertisementApi.getFirstClipDownloadProgress() }

    val clips by clipsFlow.collectAsState(initial = emptyList())
    val progress by progressFlow.collectAsState(initial = 0)

    DisposableEffect(playerApi, clipsFlow) {
        playerApi.feature().attach(clipsFlow)
        onDispose { playerApi.feature().detach() }
    }

    LaunchedEffect(clips.size) {
        if (clips.isNotEmpty()) {
            playerApi.feature().resumePlayback()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        playerApi.screen().Content(modifier = Modifier.fillMaxSize())

        if (clips.isEmpty()) {
            FallbackOverlay(
                modifier = Modifier.fillMaxSize(),
                progress = progress.coerceIn(0, 100)
            )
        }
    }
}

@Composable
private fun FallbackOverlay(
    modifier: Modifier = Modifier,
    progress: Int
) {
    Box(
        modifier = modifier.background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Загружаем ролики $progress%",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White
        )
    }
}