package ru.offerfactory.promodisplay.player.impl

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import ru.offerfactory.promodisplay.player.api.model.Clip

internal class PlayerEngine(
    private val appContext: Context,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate
) {

    private val scope = CoroutineScope(SupervisorJob() + dispatcher)

    private var attachJob: Job? = null
    private var _player: ExoPlayer? = null

    // Чтобы не пересобирать плейлист без причины
    private var currentPlaylistKeys: List<String> = emptyList()

    val player: ExoPlayer?
        get() = _player

    fun attach(clipsFlow: Flow<List<Clip>>) {
        attachJob?.cancel()
        attachJob = scope.launch {
            clipsFlow.collectLatest { clips ->
                applyPlaylist(clips)
            }
        }
    }

    fun detach() {
        attachJob?.cancel()
        attachJob = null
        currentPlaylistKeys = emptyList()
        releasePlayer()
    }

    private fun applyPlaylist(clips: List<Clip>) {
        // Воспроизводим только готовые клипы с непустым uri/path
        val playable = clips.filter { it.isReady && !it.videoUri.isNullOrBlank() }

        if (playable.isEmpty()) {
            // Нет готовых клипов — останавливаем и освобождаем ресурсы
            currentPlaylistKeys = emptyList()
            releasePlayer()
            return
        }

        val exo = ensurePlayer()

        // Сохраняем текущее положение ДО замены списка
        val oldMediaId = exo.currentMediaItem?.mediaId
        val oldPositionMs = exo.currentPosition
        val oldIndex = exo.currentMediaItemIndex

        // Собираем MediaItem’ы с ключами вида "clipId#occurrence"
        val (mediaItems, newKeys) = buildMediaItemsWithStableKeys(playable)

        // Если плейлист по сути не изменился — ничего не делаем (без дёрганья)
        if (newKeys == currentPlaylistKeys && exo.mediaItemCount == mediaItems.size) {
            // На всякий случай убеждаемся, что луп и autoplay включены
            exo.repeatMode = Player.REPEAT_MODE_ALL
            if (!exo.playWhenReady) exo.playWhenReady = true
            return
        }

        currentPlaylistKeys = newKeys

        // Выбираем откуда продолжать:
        // 1) точное совпадение oldMediaId (лучший вариант)
        // 2) совпадение по clipId (если схема mediaId раньше была другая)
        // 3) fallback на старый индекс
        // 4) иначе 0
        val resolved = resolveStart(
            oldMediaId = oldMediaId,
            oldIndex = oldIndex,
            newKeys = newKeys
        )

        val startIndex = resolved.startIndex
        val keepPosition = resolved.keepPosition
        val startPositionMs = if (keepPosition) oldPositionMs.coerceAtLeast(0L) else 0L

        exo.setMediaItems(mediaItems, startIndex, startPositionMs)
        exo.repeatMode = Player.REPEAT_MODE_ALL
        exo.prepare()
        exo.playWhenReady = true
    }

    private data class StartResolution(
        val startIndex: Int,
        val keepPosition: Boolean
    )

    private fun resolveStart(
        oldMediaId: String?,
        oldIndex: Int,
        newKeys: List<String>
    ): StartResolution {
        if (newKeys.isEmpty()) return StartResolution(startIndex = 0, keepPosition = false)

        // 1) точное совпадение mediaId
        if (!oldMediaId.isNullOrBlank()) {
            val exactIdx = newKeys.indexOf(oldMediaId)
            if (exactIdx >= 0) return StartResolution(startIndex = exactIdx, keepPosition = true)

            // 2) совпадение по clipId (на случай если раньше mediaId был просто id)
            val oldClipId = oldMediaId.substringBefore('#')
            val byIdIdx = newKeys.indexOfFirst { it.substringBefore('#') == oldClipId }
            if (byIdIdx >= 0) return StartResolution(startIndex = byIdIdx, keepPosition = true)
        }

        // 3) fallback на индекс
        if (oldIndex in newKeys.indices) {
            return StartResolution(startIndex = oldIndex, keepPosition = false)
        }

        // 4) дефолт
        return StartResolution(startIndex = 0, keepPosition = false)
    }

    private fun buildMediaItemsWithStableKeys(clips: List<Clip>): Pair<List<MediaItem>, List<String>> {
        val counters = HashMap<String, Int>(clips.size)
        val items = ArrayList<MediaItem>(clips.size)
        val keys = ArrayList<String>(clips.size)

        for (clip in clips) {
            val id = clip.id
            val occurrence = counters[id] ?: 0
            counters[id] = occurrence + 1

            val key = "$id#$occurrence"
            val uri = ClipUri.toUri(clip.videoUri!!)

            val mediaItem = MediaItem.Builder()
                .setMediaId(key) // ключ для стабильного восстановления позиции
                .setUri(uri)
                .build()

            items.add(mediaItem)
            keys.add(key)
        }

        return items to keys
    }

    private fun ensurePlayer(): ExoPlayer {
        val existing = _player
        if (existing != null) return existing

        val created = ExoPlayer.Builder(appContext).build().apply {
            repeatMode = Player.REPEAT_MODE_ALL
            playWhenReady = true
        }
        _player = created
        return created
    }

    private fun releasePlayer() {
        _player?.release()
        _player = null
    }
}