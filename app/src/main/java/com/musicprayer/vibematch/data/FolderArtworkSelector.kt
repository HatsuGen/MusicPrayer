package com.musicprayer.vibematch.data

import java.util.Locale

internal data class ScannedDocument(
    val documentId: String,
    val name: String,
    val mimeType: String?,
    val modifiedMs: Long,
    val sizeBytes: Long,
    val isDirectory: Boolean = false,
)

internal fun selectFolderArtwork(entries: List<ScannedDocument>): ScannedDocument? {
    val images = entries.filter(::isImageDocument)
    val safeImages = images.filterNot { image -> isRejectedArtworkName(image.name) }
    val namedCandidates = safeImages.mapNotNull { image ->
        artworkNamePriority(image.name)?.let { priority -> image to priority }
    }

    return namedCandidates
        .sortedWith(
            compareBy<Pair<ScannedDocument, Int>> { it.second }
                .thenByDescending { it.first.sizeBytes }
                .thenBy { it.first.name.lowercase(Locale.ROOT) },
        )
        .firstOrNull()
        ?.first
        ?: safeImages.singleOrNull()
}

internal fun isDiscFolder(name: String): Boolean {
    val normalized = name.substringBeforeLast('.', name).trim().lowercase(Locale.ROOT)
    return DISC_FOLDER_PATTERNS.any { pattern -> pattern.matches(normalized) }
}

internal fun isAudioDocument(entry: ScannedDocument): Boolean =
    !entry.isDirectory && (
        entry.mimeType?.startsWith("audio/", ignoreCase = true) == true ||
            entry.extension() in AUDIO_EXTENSIONS
        )

private fun isImageDocument(entry: ScannedDocument): Boolean =
    !entry.isDirectory && (
        entry.mimeType?.startsWith("image/", ignoreCase = true) == true ||
            entry.extension() in IMAGE_EXTENSIONS
        )

private fun artworkNamePriority(name: String): Int? {
    val stem = normalizedStem(name)
    if (isRejectedArtworkName(name)) return null

    return ARTWORK_PREFIXES.indexOfFirst { prefix ->
        if (!stem.startsWith(prefix)) false
        else stem.removePrefix(prefix).firstOrNull()?.let { suffix ->
            suffix in ARTWORK_NAME_SEPARATORS || suffix.isDigit()
        } ?: true
    }.takeIf { it >= 0 }
}

private fun isRejectedArtworkName(name: String): Boolean =
    normalizedStem(name)
        .split(NON_ALPHANUMERIC)
        .filter(String::isNotBlank)
        .any(REJECTED_ARTWORK_TOKENS::contains)

private fun normalizedStem(name: String): String =
    name.substringBeforeLast('.', name).trim().trimStart('.').lowercase(Locale.ROOT)

private fun ScannedDocument.extension(): String = name.substringAfterLast('.', "").lowercase(Locale.ROOT)

private val ARTWORK_PREFIXES = listOf("cover", "folder", "front", "albumart", "album")
private val REJECTED_ARTWORK_TOKENS = setOf("back", "rear", "booklet", "inside", "disc", "disk", "tray", "logo")
private val ARTWORK_NAME_SEPARATORS = setOf(' ', '_', '-', '.')
private val NON_ALPHANUMERIC = Regex("[^a-z0-9]+")
private val DISC_FOLDER_PATTERNS = listOf(
    Regex("^(cd|disc|disk)[\\s_-]*[0-9]+$"),
    Regex("^side[\\s_-]*[a-z0-9]+$"),
    Regex("^bonus[\\s_-]*(cd|disc|disk)([\\s_-]*[0-9]+)?$"),
)

internal val AUDIO_EXTENSIONS = setOf("mp3", "m4a", "aac", "flac", "wav", "ogg", "opus", "amr", "aiff", "aif", "alac", "wma")
private val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp", "heic", "heif")
