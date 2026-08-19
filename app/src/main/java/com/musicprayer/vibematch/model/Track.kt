package com.musicprayer.vibematch.model

import android.net.Uri

data class Track(
    val id: Long,
    val uri: Uri,
    val title: String,
    val artist: String,
    val album: String,
    val albumArtist: String? = null,
    val genre: String,
    val durationMs: Long,
    val mimeType: String?,
    val artworkUri: Uri? = null,
    val artworkModifiedMs: Long? = null,
    val albumSourceId: String? = null,
    val discNumber: Int? = null,
    val trackNumber: Int? = null,
    val sourceModifiedMs: Long = 0,
    val sampleRate: Int? = null,
    val bitDepth: Int? = null,
    val features: AudioFeatures? = null,
)

data class Playlist(
    val id: Long,
    val name: String,
    val trackIds: List<Long> = emptyList(),
)

data class ListeningRecord(
    val trackId: Long,
    val lastPlayedAt: Long = 0,
    val lastPositionMs: Long = 0,
    val durationMs: Long = 0,
    val playCount: Int = 0,
    val loopCount: Int = 0,
) {
    val isUnfinished: Boolean
        get() = durationMs > 0 && lastPositionMs > 10_000 && lastPositionMs < durationMs - 10_000
}

data class AudioFeatures(
    val rms: Float,
    val spectralCentroid: Float,
    val normalizedBpm: Float,
    val genreProxy: Float,
) {
    fun vector() = floatArrayOf(rms, spectralCentroid, normalizedBpm, genreProxy)
}
