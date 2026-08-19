package com.musicprayer.vibematch.playback

import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

class MusicService : MediaSessionService() {
    private var session: MediaSession? = null
    private val queueListener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            session?.player?.let { PlaybackQueueCoordinator.onMediaItemTransition(it, mediaItem, reason) }
        }

        override fun onTimelineChanged(timeline: Timeline, reason: Int) {
            session?.player?.let(PlaybackQueueCoordinator::onTimelineChanged)
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            if (shuffleModeEnabled) session?.player?.let(PlaybackQueueCoordinator::enforceSoloTraversal)
        }
    }

    override fun onCreate() {
        super.onCreate()
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()
        val player = ExoPlayer.Builder(this).build().apply {
            setAudioAttributes(audioAttributes, true)
            setHandleAudioBecomingNoisy(true)
            addListener(queueListener)
        }
        session = MediaSession.Builder(this, player).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onDestroy() {
        session?.run {
            player.removeListener(queueListener)
            player.release()
            release()
        }
        session = null
        PlaybackQueueCoordinator.clear()
        super.onDestroy()
    }
}
