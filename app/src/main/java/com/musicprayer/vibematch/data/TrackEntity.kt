package com.musicprayer.vibematch.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.musicprayer.vibematch.model.AudioFeatures
import com.musicprayer.vibematch.model.Track
import android.net.Uri

@Entity(tableName = "tracks")
data class TrackEntity(
    @PrimaryKey val id: Long,
    val contentUri: String,
    val title: String,
    val artist: String,
    val album: String,
    val albumArtist: String? = null,
    val genre: String,
    val durationMs: Long,
    val mimeType: String?,
    val dateModified: Long,
    val sourceTreeUri: String? = null,
    val artworkUri: String? = null,
    val artworkModifiedMs: Long? = null,
    val albumSourceId: String? = null,
    val discNumber: Int? = null,
    val trackNumber: Int? = null,
    val rms: Float? = null,
    val centroid: Float? = null,
    val bpm: Float? = null,
    val genreProxy: Float? = null,
) {
    fun toModel() = Track(
        id = id, uri = Uri.parse(contentUri), title = title, artist = artist,
        album = album, albumArtist = albumArtist, genre = genre, durationMs = durationMs, mimeType = mimeType,
        artworkUri = artworkUri?.let(Uri::parse),
        artworkModifiedMs = artworkModifiedMs,
        albumSourceId = albumSourceId,
        discNumber = discNumber,
        trackNumber = trackNumber,
        sourceModifiedMs = dateModified,
        features = if (rms != null && centroid != null && bpm != null && genreProxy != null)
            AudioFeatures(rms, centroid, bpm, genreProxy) else null,
    )
}
