package ru.offerfactory.promodisplay.player.impl

import android.content.Context
import android.util.Log
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import ru.offerfactory.promodisplay.player.api.model.Clip

internal class PlayerEngine(
    private val appContext: Context,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate
) {

    private val scope = CoroutineScope(SupervisorJob() + dispatcher)

    private var attachJob: Job? = null

    private val _playerState = MutableStateFlow<ExoPlayer?>(null)
    val playerState: StateFlow<ExoPlayer?> = _playerState

    val player: ExoPlayer?
        get() = _playerState.value

    /**
     * Ключи текущего плейлиста (стабильные, с учетом repeatInCycle через occurrence).
     * Нужны, чтобы:
     * 1) не пересобирать плейлист без причины
     * 2) уметь делать append через addMediaItems без reset/prepare (фикс мерцания)
     */
    private var currentPlaylistKeys: List<String> = emptyList()

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

    /**
     * Вызывать из MainActivity.onStart()/onResume(), чтобы после screen off/on
     * воспроизведение сразу продолжалось.
     *
     */
    fun resumePlayback() {
        val exo = _playerState.value ?: return

        if (exo.playbackState == Player.STATE_ENDED) {
            exo.seekToDefaultPosition()
        }

        if (exo.playbackState == Player.STATE_IDLE) {
            exo.prepare()
        }

        exo.playWhenReady = true

        if (!exo.isPlaying) {
            exo.play()
        }
    }

    fun pausePlayback() {
        _playerState.value?.pause()
    }

    private fun applyPlaylist(clips: List<Clip>) {
        val playable = clips.filter { it.isReady && !it.videoUri.isNullOrBlank() }

        if (playable.isEmpty()) {
            currentPlaylistKeys = emptyList()
            releasePlayer()
            return
        }

        val exo = ensurePlayer()

        val oldMediaId = exo.currentMediaItem?.mediaId
        val oldPositionMs = exo.currentPosition
        val oldIndex = exo.currentMediaItemIndex

        val (mediaItems, newKeys) = buildMediaItemsWithStableKeys(playable)

        // 1) Ничего не изменилось — вообще не трогаем pipeline
        if (newKeys == currentPlaylistKeys && exo.mediaItemCount == mediaItems.size) {
            exo.repeatMode = Player.REPEAT_MODE_ALL
            if (!exo.playWhenReady) exo.playWhenReady = true
            return
        }

        // Если новый плейлист является расширением старого (старое = префикс нового),
        // значит у нас "докачались/стали готовы" новые клипы и список подрос.
        if (currentPlaylistKeys.isNotEmpty()
            && newKeys.size > currentPlaylistKeys.size
            && newKeys.startsWithPrefix(currentPlaylistKeys)
        ) {
            val appendFrom = currentPlaylistKeys.size
            val toAppend = mediaItems.subList(appendFrom, mediaItems.size)

            Log.d(TAG, "Playlist expanded: +${toAppend.size} items (append only)")

            exo.addMediaItems(toAppend)
            currentPlaylistKeys = newKeys

            exo.repeatMode = Player.REPEAT_MODE_ALL
            if (exo.playbackState == Player.STATE_IDLE) {
                exo.prepare()
            }
            if (!exo.playWhenReady) exo.playWhenReady = true
            return
        }

        // 3) Иначе — реальное изменение (порядок/удаление/сильный reset).
        // Тут придётся пересобирать плейлист.
        currentPlaylistKeys = newKeys

        val resolved = resolveStart(
            oldMediaId = oldMediaId,
            oldIndex = oldIndex,
            newKeys = newKeys
        )

        val startIndex = resolved.startIndex
        val keepPosition = resolved.keepPosition
        val startPositionMs = if (keepPosition) oldPositionMs.coerceAtLeast(0L) else 0L

        Log.d(TAG, "Playlist reset: count=${mediaItems.size}, startIndex=$startIndex, keepPos=$keepPosition")

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

        if (!oldMediaId.isNullOrBlank()) {
            val exactIdx = newKeys.indexOf(oldMediaId)
            if (exactIdx >= 0) return StartResolution(startIndex = exactIdx, keepPosition = true)

            val oldClipId = oldMediaId.substringBefore('#')
            val byIdIdx = newKeys.indexOfFirst { it.substringBefore('#') == oldClipId }
            if (byIdIdx >= 0) return StartResolution(startIndex = byIdIdx, keepPosition = true)
        }

        if (oldIndex in newKeys.indices) {
            return StartResolution(startIndex = oldIndex, keepPosition = false)
        }

        return StartResolution(startIndex = 0, keepPosition = false)
    }

    /**
     * Делаем стабильные mediaId ключи (id#occurrence) чтобы:
     * - одинаковые клипы при repeatInCycle имели разные mediaId
     * - можно было правильно резолвить текущий клип при обновлениях
     */
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
                .setMediaId(key)
                .setUri(uri)
                .build()

            items.add(mediaItem)
            keys.add(key)
        }

        return items to keys
    }

    private fun ensurePlayer(): ExoPlayer {
        val existing = _playerState.value
        if (existing != null) return existing

        val created = ExoPlayer.Builder(appContext).build().apply {
            repeatMode = Player.REPEAT_MODE_ALL
            playWhenReady = true

            val attrs = AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                .build()

            // handleAudioFocus=true важно для “после screen off/on не стоит на паузе”
            setAudioAttributes(attrs, /* handleAudioFocus= */ true)
        }

        _playerState.value = created
        Log.d(TAG, "ExoPlayer created")
        return created
    }

    private fun releasePlayer() {
        _playerState.value?.release()
        _playerState.value = null
        Log.d(TAG, "ExoPlayer released")
    }

    private fun List<String>.startsWithPrefix(prefix: List<String>): Boolean {
        if (prefix.size > size) return false
        for (i in prefix.indices) {
            if (this[i] != prefix[i]) return false
        }
        return true
    }

    private companion object {
        const val TAG = "PlayerEngine"
    }
}