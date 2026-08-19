package com.musicprayer.vibematch.model

import java.text.Normalizer
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.Locale

/** Stable-enough album identity until the scanner stores a dedicated album ID. */
fun Track.albumCollectionKey(): String {
    return albumCollectionKey(
        album = album,
        artist = artist,
        albumArtist = albumArtist,
        albumSourceId = albumSourceId,
        contentUri = uri.toString(),
        artworkUri = artworkUri?.toString(),
    )
}

internal fun albumCollectionKey(
    album: String,
    artist: String,
    albumArtist: String? = null,
    albumSourceId: String? = null,
    contentUri: String,
    artworkUri: String? = null,
): String {
    val normalizedAlbum = album.normalizedMetadata()
    val source = albumSourceId?.normalizedSourceIdentifier()?.takeIf(String::isNotBlank)
        ?: contentUri.documentFolderKey()
        ?: artworkUri?.documentFolderKey()
        ?: albumArtist?.normalizedMetadata()?.takeIf(String::isNotBlank)
        ?: artist.normalizedMetadata()
    return "$normalizedAlbum\u0000$source"
}

fun Track.hasBrowsableAlbum(): Boolean {
    val normalized = album.normalizedMetadata()
    return normalized.isNotBlank() && !normalized.isUnknownAlbumValue()
}

fun List<Track>.tracksInAlbum(albumKey: String): List<Track> =
    filter { it.albumCollectionKey() == albumKey }

fun List<Track>.albumDisplayArtist(): String? {
    val albumArtists = mapNotNull(Track::albumArtist)
        .filter(String::isNotBlank)
        .distinctBy(String::normalizedMetadata)
    if (albumArtists.size == 1) return albumArtists.first()
    val artists = map(Track::artist)
        .filterNot { it.isBlank() || it.normalizedMetadata() == "unknown artist" }
        .distinctBy(String::normalizedMetadata)
    return when (artists.size) {
        0 -> null
        1 -> artists.first()
        else -> "Various artists"
    }
}

fun List<Track>.albumCoverTrack(): Track? =
    firstOrNull { it.artworkUri != null } ?: firstOrNull()

private fun String.documentFolderKey(): String? {
    val markerIndex = indexOf(DOCUMENT_PATH_MARKER)
    if (markerIndex < 0) return null
    val encodedDocumentId = substring(markerIndex + DOCUMENT_PATH_MARKER.length)
        .substringBefore('?')
        .substringBefore('#')
    val documentId = runCatching {
        URLDecoder.decode(encodedDocumentId, StandardCharsets.UTF_8.name())
    }.getOrNull() ?: return null
    var folderId = documentId.substringBeforeLast('/', missingDelimiterValue = "")
        .takeIf(String::isNotBlank)
        ?: return null
    if (folderId.substringAfterLast('/').matches(DISC_FOLDER_PATTERN)) {
        folderId = folderId.substringBeforeLast('/', missingDelimiterValue = folderId)
    }
    val authority = substringAfter(URI_SCHEME_MARKER, missingDelimiterValue = "")
        .substringBefore('/')
        .lowercase(Locale.ROOT)
    return "$authority:${folderId.normalizedMetadata()}"
}

private fun String.normalizedMetadata(): String = Normalizer.normalize(trim(), Normalizer.Form.NFKC)
    .replace(WHITESPACE_PATTERN, " ")
    .lowercase(Locale.ROOT)

private fun String.normalizedSourceIdentifier(): String =
    Normalizer.normalize(trim(), Normalizer.Form.NFKC)

private fun String.isUnknownAlbumValue(): Boolean =
    this == "unknown" || this == "unknown album" || this == "<unknown>"

private val WHITESPACE_PATTERN = Regex("\\s+")
private val DISC_FOLDER_PATTERN = Regex("(?i)^(?:cd|disc|disk)[ _-]*\\d{1,2}$")
private const val URI_SCHEME_MARKER = "://"
private const val DOCUMENT_PATH_MARKER = "/document/"
