package com.musicprayer.vibematch.data

import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

class MediaStoreScanner(private val context: Context) {
    suspend fun scan(folderUri: Uri? = null): List<TrackEntity> = withContext(Dispatchers.IO) {
        if (folderUri == null) scanMediaStore() else scanDocumentTree(folderUri)
    }

    private fun scanMediaStore(): List<TrackEntity> {
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL) else MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val albumArtistColumn = ALBUM_ARTIST_COLUMN.takeIf { Build.VERSION.SDK_INT >= Build.VERSION_CODES.R }
        val columns = buildList {
            add(MediaStore.Audio.Media._ID)
            add(MediaStore.Audio.Media.TITLE)
            add(MediaStore.Audio.Media.ARTIST)
            add(MediaStore.Audio.Media.ALBUM)
            add(MediaStore.Audio.Media.ALBUM_ID)
            add(MediaStore.Audio.Media.TRACK)
            add(MediaStore.Audio.Media.DURATION)
            add(MediaStore.Audio.Media.MIME_TYPE)
            add(MediaStore.Audio.Media.DATE_MODIFIED)
            albumArtistColumn?.let(::add)
        }.toTypedArray()
        return buildList {
            context.contentResolver.query(collection, columns, "${MediaStore.Audio.Media.IS_MUSIC} != 0", null, "${MediaStore.Audio.Media.DATE_MODIFIED} DESC")?.use { cursor ->
                val idIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idIndex)
                    val encodedTrackNumber = cursor.long(MediaStore.Audio.Media.TRACK).toInt()
                    add(TrackEntity(
                        id = id, contentUri = ContentUris.withAppendedId(collection, id).toString(),
                        title = cursor.text(MediaStore.Audio.Media.TITLE, "Unknown title"), artist = cursor.text(MediaStore.Audio.Media.ARTIST, "Unknown artist"),
                        album = cursor.text(MediaStore.Audio.Media.ALBUM, "Unknown album"),
                        albumArtist = albumArtistColumn?.let(cursor::nullableText),
                        albumSourceId = cursor.long(MediaStore.Audio.Media.ALBUM_ID).takeIf { it > 0 }?.let { "media-store:$collection:$it" },
                        discNumber = encodedTrackNumber.takeIf { it >= 1_000 }?.div(1_000)?.takeIf { it > 0 },
                        trackNumber = encodedTrackNumber.takeIf { it > 0 }?.rem(1_000)?.takeIf { it > 0 },
                        genre = "Unknown genre",
                        durationMs = cursor.long(MediaStore.Audio.Media.DURATION), mimeType = cursor.nullableText(MediaStore.Audio.Media.MIME_TYPE),
                        dateModified = cursor.long(MediaStore.Audio.Media.DATE_MODIFIED),
                    ))
                }
            }
        }
    }

    private fun scanDocumentTree(treeUri: Uri): List<TrackEntity> {
        val rootId = DocumentsContract.getTreeDocumentId(treeUri)
        return buildList {
            scanChildren(
                treeUri = treeUri,
                parentId = rootId,
                albumRootId = rootId,
                sourceTree = treeUri.toString(),
                inheritedArtwork = null,
                output = this,
                visitedDocumentIds = mutableSetOf(),
            )
        }
    }

    private fun scanChildren(
        treeUri: Uri,
        parentId: String,
        albumRootId: String,
        sourceTree: String,
        inheritedArtwork: ScannedDocument?,
        output: MutableList<TrackEntity>,
        visitedDocumentIds: MutableSet<String>,
    ) {
        if (!visitedDocumentIds.add(parentId)) return

        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentId)
        val columns = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
            DocumentsContract.Document.COLUMN_SIZE,
        )
        val entries = buildList {
            val cursor = checkNotNull(context.contentResolver.query(childrenUri, columns, null, null, null)) {
                "The selected music folder could not be read"
            }
            cursor.use {
                while (it.moveToNext()) {
                    val mime = it.nullableText(DocumentsContract.Document.COLUMN_MIME_TYPE)
                    add(
                        ScannedDocument(
                            documentId = it.text(DocumentsContract.Document.COLUMN_DOCUMENT_ID, ""),
                            name = it.text(DocumentsContract.Document.COLUMN_DISPLAY_NAME, "Unknown title"),
                            mimeType = mime,
                            modifiedMs = it.long(DocumentsContract.Document.COLUMN_LAST_MODIFIED),
                            sizeBytes = it.long(DocumentsContract.Document.COLUMN_SIZE),
                            isDirectory = mime == DocumentsContract.Document.MIME_TYPE_DIR,
                        ),
                    )
                }
            }
        }

        val folderArtwork = selectFolderArtwork(entries) ?: inheritedArtwork
        entries.filter(::isAudioDocument).forEach { entry ->
            val uri = DocumentsContract.buildDocumentUriUsingTree(treeUri, entry.documentId)
            val artworkUri = folderArtwork?.let { artwork ->
                DocumentsContract.buildDocumentUriUsingTree(treeUri, artwork.documentId).toString()
            }
            output += readDocumentMetadata(
                uri = uri,
                fileName = entry.name,
                mime = entry.mimeType,
                modified = entry.modifiedMs,
                sourceTree = sourceTree,
                albumSourceId = "$sourceTree#$albumRootId",
                artworkUri = artworkUri,
                artworkModifiedMs = folderArtwork?.modifiedMs,
            )
        }

        entries.filter(ScannedDocument::isDirectory).forEach { directory ->
            scanChildren(
                treeUri = treeUri,
                parentId = directory.documentId,
                albumRootId = if (isDiscFolder(directory.name)) albumRootId else directory.documentId,
                sourceTree = sourceTree,
                inheritedArtwork = folderArtwork?.takeIf { isDiscFolder(directory.name) },
                output = output,
                visitedDocumentIds = visitedDocumentIds,
            )
        }
    }

    private fun readDocumentMetadata(
        uri: Uri,
        fileName: String,
        mime: String?,
        modified: Long,
        sourceTree: String,
        albumSourceId: String,
        artworkUri: String?,
        artworkModifiedMs: Long?,
    ): TrackEntity {
        var title = fileName.substringBeforeLast('.'); var artist = "Unknown artist"; var album = "Unknown album"; var genre = "Unknown genre"; var duration = 0L
        var albumArtist: String? = null; var discNumber: Int? = null; var trackNumber: Int? = null
        runCatching {
            MediaMetadataRetriever().use { retriever ->
                retriever.setDataSource(context, uri)
                title = retriever.value(MediaMetadataRetriever.METADATA_KEY_TITLE, title); artist = retriever.value(MediaMetadataRetriever.METADATA_KEY_ARTIST, artist)
                album = retriever.value(MediaMetadataRetriever.METADATA_KEY_ALBUM, album); genre = retriever.value(MediaMetadataRetriever.METADATA_KEY_GENRE, genre)
                albumArtist = retriever.optionalValue(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST)
                discNumber = retriever.optionalValue(MediaMetadataRetriever.METADATA_KEY_DISC_NUMBER).metadataNumber()
                trackNumber = retriever.optionalValue(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER).metadataNumber()
                duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            }
        }
        val uuid = UUID.nameUUIDFromBytes(uri.toString().toByteArray())
        return TrackEntity(
            id = uuid.mostSignificantBits xor uuid.leastSignificantBits,
            contentUri = uri.toString(),
            title = title,
            artist = artist,
            album = album,
            albumArtist = albumArtist,
            genre = genre,
            durationMs = duration,
            mimeType = mime,
            dateModified = modified,
            sourceTreeUri = sourceTree,
            albumSourceId = albumSourceId,
            discNumber = discNumber,
            trackNumber = trackNumber,
            artworkUri = artworkUri,
            artworkModifiedMs = artworkModifiedMs,
        )
    }
}

private fun MediaMetadataRetriever.value(key: Int, fallback: String): String = extractMetadata(key)?.takeIf(String::isNotBlank) ?: fallback
private fun MediaMetadataRetriever.optionalValue(key: Int): String? = extractMetadata(key)?.trim()?.takeIf(String::isNotBlank)
private fun String?.metadataNumber(): Int? = this?.let { value -> Regex("\\d+").find(value)?.value?.toIntOrNull() }?.takeIf { it > 0 }
private fun android.database.Cursor.text(column: String, fallback: String): String = nullableText(column)?.takeIf(String::isNotBlank) ?: fallback
private fun android.database.Cursor.nullableText(column: String): String? = getColumnIndex(column).takeIf { it >= 0 && !isNull(it) }?.let(::getString)
private fun android.database.Cursor.long(column: String): Long = getColumnIndex(column).takeIf { it >= 0 && !isNull(it) }?.let(::getLong) ?: 0L

private const val ALBUM_ARTIST_COLUMN = "album_artist"
