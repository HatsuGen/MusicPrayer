package com.musicprayer.vibematch.playback

import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.musicprayer.vibematch.model.Track
import java.util.concurrent.atomic.AtomicLong

internal data class SoloQueueStart(
    val sessionId: Long,
    val initialUpcoming: List<Track>,
)

internal data class PlaybackQueueSnapshot(
    val kind: PlaybackQueueKind? = null,
    val title: String? = null,
    val soloMode: PlaybackMode = PlaybackMode.SEQUENTIAL,
    val soloSessionId: Long? = null,
)

/**
 * Process-local queue state owned by the playback service lifecycle.
 *
 * The media service and controller run in the same app process. Keeping the rolling random
 * session here lets ExoPlayer replenish its own timeline while the Activity/ViewModel is gone.
 */
internal object PlaybackQueueCoordinator {
    private data class SoloSession(
        val id: Long,
        val pool: SoloRandomQueue,
        val heardIds: MutableSet<Long>,
        var mode: PlaybackMode = PlaybackMode.SEQUENTIAL,
    )

    private val nextSessionId = AtomicLong(0)
    private var kind: PlaybackQueueKind? = null
    private var title: String? = null
    private var soloSession: SoloSession? = null
    private var refilling = false

    fun beginSolo(seed: Track, candidates: List<Track>): SoloQueueStart {
        val id = nextSessionId.incrementAndGet()
        val pool = SoloRandomQueue(seed, candidates)
        val initial = pool.take(SOLO_UPCOMING_SIZE)
        soloSession = SoloSession(id, pool, mutableSetOf(seed.id))
        kind = PlaybackQueueKind.SOLO_RANDOM
        title = "Random session"
        return SoloQueueStart(id, initial)
    }

    fun beginContext(sourceTitle: String) {
        soloSession = null
        kind = PlaybackQueueKind.CONTEXT
        title = sourceTitle
    }

    fun clear() {
        soloSession = null
        kind = null
        title = null
        refilling = false
    }

    fun snapshot(): PlaybackQueueSnapshot {
        val solo = soloSession
        return PlaybackQueueSnapshot(kind, title, solo?.mode ?: PlaybackMode.SEQUENTIAL, solo?.id)
    }

    fun reserve(trackId: Long) {
        soloSession?.pool?.reserve(trackId)
    }

    fun setSoloMode(mode: PlaybackMode) {
        soloSession?.mode = mode
    }

    fun isCurrentSoloItem(item: MediaItem?): Boolean {
        val session = soloSession ?: return false
        return item?.queueSessionId() == session.id
    }

    fun onMediaItemTransition(player: Player, mediaItem: MediaItem?, reason: Int) {
        if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT || !isCurrentSoloItem(mediaItem)) return
        mediaItem?.mediaId?.toLongOrNull()?.let { soloSession?.heardIds?.add(it) }
        ensureUpcoming(player)
    }

    fun onTimelineChanged(player: Player) {
        if (isCurrentSoloItem(player.currentMediaItem)) ensureUpcoming(player)
    }

    fun enforceSoloTraversal(player: Player) {
        if (isCurrentSoloItem(player.currentMediaItem) && player.shuffleModeEnabled) {
            player.shuffleModeEnabled = false
        }
    }

    private fun ensureUpcoming(player: Player) {
        val session = soloSession ?: return
        if (refilling || player.currentMediaItem?.queueSessionId() != session.id) return
        enforceSoloTraversal(player)
        val currentIndex = player.currentMediaItemIndex
        if (currentIndex == C.INDEX_UNSET) return
        val futureCount = (currentIndex + 1 until player.mediaItemCount).count { index ->
            player.getMediaItemAt(index).queueSessionId() == session.id
        }
        val additions = session.pool.take((SOLO_UPCOMING_SIZE - futureCount).coerceAtLeast(0))
        if (additions.isEmpty()) return

        refilling = true
        try {
            player.addMediaItems(additions.map { it.asMediaItem(session.id) })
        } finally {
            refilling = false
        }
    }

    private const val SOLO_UPCOMING_SIZE = 20
}
