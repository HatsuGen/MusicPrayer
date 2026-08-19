package com.musicprayer.vibematch.playback

import android.content.ComponentName
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.PlaybackException
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.musicprayer.vibematch.model.Track
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class PlaybackMode { SEQUENTIAL, SHUFFLE, REPEAT_ONE }
enum class PlaybackQueueKind { CONTEXT, SOLO_RANDOM }

data class PlaybackState(
    val currentId: Long? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val bufferedPositionMs: Long = 0,
    val isSeekable: Boolean = false,
    val mode: PlaybackMode = PlaybackMode.SEQUENTIAL,
    val queueKind: PlaybackQueueKind? = null,
    val queueTitle: String? = null,
    val upcomingIds: List<Long> = emptyList(),
    val isConnected: Boolean = false,
    val errorMessage: String? = null,
)

class PlaybackConnection(
    context: Context,
    private val onLoop: (Long) -> Unit = {},
    private val onTrackStarted: (Long, Long) -> Unit = { _, _ -> },
) : Player.Listener {
    private sealed interface PlayRequest {
        data class Solo(val track: Track, val candidates: List<Track>) : PlayRequest
        data class Context(val track: Track, val queue: List<Track>, val title: String) : PlayRequest
    }

    private val appContext = context.applicationContext
    private val handler = Handler(Looper.getMainLooper())
    private var future: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    private var pendingPlay: PlayRequest? = null
    private var playerError: String? = null
    private var upcomingIds: List<Long> = emptyList()
    private var lastStartedId: Long? = null
    private val _state = MutableStateFlow(PlaybackState())
    val state = _state.asStateFlow()
    private val ticker = object : Runnable {
        override fun run() {
            publish()
            if (controller?.isPlaying == true) handler.postDelayed(this, POSITION_UPDATE_INTERVAL_MS)
        }
    }

    fun connect() {
        if (future != null) return
        val token = SessionToken(appContext, ComponentName(appContext, MusicService::class.java))
        future = MediaController.Builder(appContext, token).buildAsync().also { pending ->
            pending.addListener({
                runCatching { pending.get() }
                    .onSuccess { connected ->
                        controller = connected.also { it.addListener(this) }
                        playerError = null
                        if (PlaybackQueueCoordinator.snapshot().kind == null && connected.mediaItemCount > 0) {
                            PlaybackQueueCoordinator.beginContext("Current queue")
                        }
                        pendingPlay?.also { request ->
                            pendingPlay = null
                            execute(connected, request)
                        }
                        refreshUpcoming(connected)
                        publish()
                        refreshTicker()
                    }
                    .onFailure { error ->
                        future = null
                        playerError = error.message ?: "Unable to connect to the audio service"
                        publishDisconnected()
                    }
            }, ContextCompat.getMainExecutor(appContext))
        }
    }

    fun disconnect() {
        handler.removeCallbacks(ticker)
        controller?.removeListener(this)
        future?.let(MediaController::releaseFuture)
        future = null
        controller = null
    }

    fun playSolo(track: Track, candidates: List<Track>) = submit(PlayRequest.Solo(track, candidates))

    fun playContext(track: Track, queue: List<Track>, title: String) =
        submit(PlayRequest.Context(track, queue, title))

    private fun submit(request: PlayRequest) {
        val connected = controller
        if (connected == null) {
            pendingPlay = request
            connect()
        } else {
            execute(connected, request)
        }
    }

    private fun execute(player: MediaController, request: PlayRequest) {
        when (request) {
            is PlayRequest.Solo -> playSoloNow(player, request.track, request.candidates)
            is PlayRequest.Context -> playContextNow(player, request.track, request.queue, request.title)
        }
    }

    private fun playSoloNow(player: MediaController, track: Track, candidates: List<Track>) {
        val start = PlaybackQueueCoordinator.beginSolo(track, candidates)
        upcomingIds = start.initialUpcoming.map(Track::id)
        lastStartedId = null
        player.shuffleModeEnabled = false
        player.repeatMode = Player.REPEAT_MODE_OFF
        replacePlayerQueue(player, track, listOf(track) + start.initialUpcoming, start.sessionId)
    }

    private fun playContextNow(player: MediaController, track: Track, queue: List<Track>, title: String) {
        PlaybackQueueCoordinator.beginContext(title)
        upcomingIds = emptyList()
        lastStartedId = null
        player.shuffleModeEnabled = false
        player.repeatMode = Player.REPEAT_MODE_OFF
        val normalized = queue.distinctBy(Track::id)
        val targetIndex = normalized.indexOfFirst { it.id == track.id }
        val sourceQueue = if (targetIndex >= 0) normalized else listOf(track)
        replacePlayerQueue(player, track, sourceQueue, queueSessionId = null)
    }

    private fun replacePlayerQueue(player: MediaController, track: Track, sourceQueue: List<Track>, queueSessionId: Long?) {
        val sourceIndex = sourceQueue.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
        runCatching {
            val items = sourceQueue.map { it.asMediaItem(queueSessionId) }
            playerError = null
            player.setMediaItems(items.take(MAX_MEDIA_ITEMS_PER_COMMAND))
            items.drop(MAX_MEDIA_ITEMS_PER_COMMAND)
                .chunked(MAX_MEDIA_ITEMS_PER_COMMAND)
                .forEach(player::addMediaItems)
            player.seekTo(sourceIndex, 0)
            player.prepare()
            player.play()
        }.onFailure { error ->
            PlaybackQueueCoordinator.clear()
            playerError = error.localizedMessage ?: "The selected track could not be queued"
            publish()
        }
    }

    fun toggle() {
        controller?.let { if (it.isPlaying) it.pause() else it.play() }
    }

    fun next() = controller?.seekToNextMediaItem()
    fun previous() = controller?.seekToPreviousMediaItem()

    fun seekTo(positionMs: Long) {
        controller?.let { player ->
            val duration = player.duration.takeUnless { it == C.TIME_UNSET } ?: Long.MAX_VALUE
            player.seekTo(positionMs.coerceIn(0, duration))
            publish()
        }
    }

    fun seekToQueueItem(trackId: Long) {
        controller?.let { player ->
            val index = player.indexOf(trackId, startIndex = player.currentMediaItemIndex + 1)
                .takeIf { it >= 0 }
                ?: player.indexOf(trackId)
            if (index >= 0 && index != player.currentMediaItemIndex) {
                player.seekToDefaultPosition(index)
                player.play()
            }
        }
    }

    fun playNext(track: Track) = enqueue(track, playNext = true)
    fun addToQueue(track: Track) = enqueue(track, playNext = false)

    private fun enqueue(track: Track, playNext: Boolean) {
        val player = controller ?: return
        if (player.mediaItemCount == 0 || player.currentMediaItemIndex == C.INDEX_UNSET) return
        if (!player.isCommandAvailable(Player.COMMAND_CHANGE_MEDIA_ITEMS)) {
            playerError = "This playback session cannot change its queue"
            publish()
            return
        }
        val currentIndex = player.currentMediaItemIndex
        val existingIndex = player.indexOf(track.id)
        if (existingIndex == currentIndex) return

        runCatching {
            if (existingIndex >= 0) {
                if (!playNext && existingIndex > currentIndex) return@runCatching
                val targetIndex = if (playNext) {
                    if (existingIndex < currentIndex) currentIndex else currentIndex + 1
                } else {
                    player.mediaItemCount - 1
                }
                player.moveMediaItem(existingIndex, targetIndex.coerceIn(0, player.mediaItemCount - 1))
            } else {
                val insertionIndex = if (playNext) {
                    (player.currentMediaItemIndex + 1).coerceAtMost(player.mediaItemCount)
                } else {
                    player.mediaItemCount
                }
                val soloSessionId = PlaybackQueueCoordinator.snapshot().soloSessionId
                    ?.takeIf { PlaybackQueueCoordinator.isCurrentSoloItem(player.currentMediaItem) }
                player.addMediaItem(insertionIndex, track.asMediaItem(soloSessionId))
            }
            PlaybackQueueCoordinator.reserve(track.id)
        }.onFailure { error ->
            playerError = error.localizedMessage ?: "The track could not be added to the queue"
            publish()
        }
    }

    fun setMode(mode: PlaybackMode) {
        controller?.let { player ->
            if (PlaybackQueueCoordinator.isCurrentSoloItem(player.currentMediaItem)) {
                PlaybackQueueCoordinator.setSoloMode(mode)
                player.shuffleModeEnabled = false
                player.repeatMode = if (mode == PlaybackMode.REPEAT_ONE) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
            } else {
                player.shuffleModeEnabled = mode == PlaybackMode.SHUFFLE
                player.repeatMode = if (mode == PlaybackMode.REPEAT_ONE) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
            }
        }
        controller?.let(::refreshUpcoming)
        publish()
    }

    override fun onEvents(player: Player, events: Player.Events) {
        refreshUpcoming(player)
        publish()
        refreshTicker()
    }

    override fun onPlayerError(error: PlaybackException) {
        playerError = error.localizedMessage ?: "This audio file could not be opened"
        publish()
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        val id = mediaItem?.mediaId?.toLongOrNull()
        if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT) {
            id?.let(onLoop)
        } else if (id != null) {
            if (lastStartedId != id) {
                onTrackStarted(id, mediaItem.mediaMetadata.durationMs ?: 0L)
                lastStartedId = id
            }
        }
        controller?.let { player ->
            refreshUpcoming(player)
        }
        publish()
    }

    private fun refreshUpcoming(player: Player) {
        upcomingIds = timelineUpcomingIds(player)
    }

    private fun timelineUpcomingIds(player: Player): List<Long> = runCatching {
        val timeline = player.currentTimeline
        var index = player.currentMediaItemIndex
        if (timeline.isEmpty || index == C.INDEX_UNSET) return@runCatching emptyList()
        val visited = hashSetOf(index)
        buildList {
            while (true) {
                index = timeline.getNextWindowIndex(index, Player.REPEAT_MODE_OFF, player.shuffleModeEnabled)
                if (index == C.INDEX_UNSET || !visited.add(index)) break
                player.getMediaItemAt(index).mediaId.toLongOrNull()?.let(::add)
            }
        }
    }.getOrElse { emptyList() }

    private fun Player.indexOf(trackId: Long, startIndex: Int = 0): Int {
        for (index in startIndex.coerceAtLeast(0) until mediaItemCount) {
            if (getMediaItemAt(index).mediaId == trackId.toString()) return index
        }
        return -1
    }

    private fun refreshTicker() {
        handler.removeCallbacks(ticker)
        if (controller?.isPlaying == true) handler.post(ticker)
    }

    private fun publish() {
        val player = controller ?: return
        val queue = PlaybackQueueCoordinator.snapshot()
        val duration = player.duration.takeUnless { it == C.TIME_UNSET || it < 0 }
            ?: player.mediaMetadata.durationMs
            ?: 0
        _state.value = PlaybackState(
            currentId = player.currentMediaItem?.mediaId?.toLongOrNull(),
            isPlaying = player.isPlaying,
            positionMs = player.currentPosition.coerceAtLeast(0),
            durationMs = duration,
            bufferedPositionMs = player.bufferedPosition.coerceAtLeast(0),
            isSeekable = player.isCurrentMediaItemSeekable,
            mode = when {
                queue.kind == PlaybackQueueKind.SOLO_RANDOM -> queue.soloMode
                player.repeatMode == Player.REPEAT_MODE_ONE -> PlaybackMode.REPEAT_ONE
                player.shuffleModeEnabled -> PlaybackMode.SHUFFLE
                else -> PlaybackMode.SEQUENTIAL
            },
            queueKind = queue.kind,
            queueTitle = queue.title,
            upcomingIds = upcomingIds,
            isConnected = true,
            errorMessage = playerError,
        )
    }

    private fun publishDisconnected() {
        _state.value = _state.value.copy(isConnected = false, errorMessage = playerError)
    }

    private companion object {
        const val MAX_MEDIA_ITEMS_PER_COMMAND = 75
        const val POSITION_UPDATE_INTERVAL_MS = 500L
    }
}
