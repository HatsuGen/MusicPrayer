package com.musicprayer.vibematch.playback

import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.musicprayer.vibematch.model.Track

internal fun Track.asMediaItem(queueSessionId: Long? = null): MediaItem {
    val metadata = MediaMetadata.Builder()
        .setTitle(title)
        .setArtist(artist)
        .setAlbumTitle(album)
        .setGenre(genre)
        .setDurationMs(durationMs)
        .apply {
            artworkUri?.let(::setArtworkUri)
            queueSessionId?.let { id -> setExtras(Bundle().apply { putLong(QUEUE_SESSION_ID_KEY, id) }) }
        }
        .build()
    return MediaItem.Builder()
        .setMediaId(id.toString())
        .setUri(uri)
        .setMediaMetadata(metadata)
        .build()
}

internal fun MediaItem.queueSessionId(): Long? = mediaMetadata.extras
    ?.takeIf { it.containsKey(QUEUE_SESSION_ID_KEY) }
    ?.getLong(QUEUE_SESSION_ID_KEY)

private const val QUEUE_SESSION_ID_KEY = "musicprayer.queue_session_id"
