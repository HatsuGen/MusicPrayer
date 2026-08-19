package com.musicprayer.vibematch.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class AlbumCollectionTest {
    @Test
    fun compilationTracksInTheSameFolderShareAnAlbum() {
        val first = key("AD:HOUSE 12", "Artist A", "ADHOUSE12/01.flac")
        val second = key("ad:house 12", "Artist B", "ADHOUSE12/02.flac")

        assertEquals(first, second)
    }

    @Test
    fun albumsWithTheSameNameInDifferentFoldersStaySeparate() {
        val first = key("Greatest Hits", "Artist A", "Artist A/Greatest Hits/01.flac")
        val second = key("Greatest Hits", "Artist B", "Artist B/Greatest Hits/01.flac")

        assertNotEquals(first, second)
    }

    @Test
    fun discFoldersCollapseIntoTheirParentAlbumFolder() {
        val discOne = key("Long Album", "Artist", "Artist/Long Album/CD1/01.flac")
        val discTwo = key("Long Album", "Artist", "Artist/Long Album/Disc 2/01.flac")

        assertEquals(discOne, discTwo)
    }

    @Test
    fun metadataWhitespaceCaseAndUnicodeWidthAreNormalized() {
        val first = key("ＡＤ：ＨＯＵＳＥ   １２", "Artist A", "Album/01.flac")
        val second = key("ad:house 12", "Artist B", "Album/02.flac")

        assertEquals(first, second)
    }

    @Test
    fun unknownAlbumsWithoutDocumentFoldersUseArtistAsFallback() {
        val first = albumCollectionKey(
            album = "Unknown album",
            artist = "Artist A",
            contentUri = "content://media/external/audio/media/1",
        )
        val second = albumCollectionKey(
            album = "Unknown album",
            artist = "Artist B",
            contentUri = "content://media/external/audio/media/2",
        )

        assertNotEquals(first, second)
    }

    @Test
    fun knownAlbumsWithoutSourceIdsUseArtistAsFallback() {
        val first = albumCollectionKey(
            album = "Greatest Hits",
            artist = "Artist A",
            contentUri = "content://provider/opaque/1",
        )
        val second = albumCollectionKey(
            album = "Greatest Hits",
            artist = "Artist B",
            contentUri = "content://provider/opaque/2",
        )

        assertNotEquals(first, second)
    }

    private fun key(album: String, artist: String, documentPath: String): String = albumCollectionKey(
        album = album,
        artist = artist,
        contentUri = "content://com.android.externalstorage.documents/tree/primary%3AMusic/document/primary%3AMusic%2F${encodePath(documentPath)}",
    )

    private fun encodePath(path: String): String = path
        .replace(" ", "%20")
        .replace("/", "%2F")
}
