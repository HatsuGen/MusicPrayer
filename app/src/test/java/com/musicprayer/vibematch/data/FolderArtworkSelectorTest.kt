package com.musicprayer.vibematch.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FolderArtworkSelectorTest {
    @Test
    fun coverSelectionIsStableWhenProviderOrderChanges() {
        val folder = image("folder.png", size = 2_000)
        val smallCover = image("cover.jpg", size = 1_000)
        val largeCover = image("cover_large.jpg", size = 4_000)

        assertEquals(largeCover, selectFolderArtwork(listOf(folder, smallCover, largeCover)))
        assertEquals(largeCover, selectFolderArtwork(listOf(largeCover, folder, smallCover)))
    }

    @Test
    fun rejectedBackOrDiscImageIsNeverUsedAsSingletonFallback() {
        assertNull(selectFolderArtwork(listOf(image("back.jpg"))))
        assertNull(selectFolderArtwork(listOf(image("disc.png"))))
    }

    @Test
    fun oneUnnamedImageCanBeUsedButSeveralUnnamedImagesAreAmbiguous() {
        val portrait = image("scan0001.webp")
        assertEquals(portrait, selectFolderArtwork(listOf(portrait)))
        assertNull(selectFolderArtwork(listOf(portrait, image("scan0002.webp"))))
    }

    @Test
    fun albumArtistIsNotMistakenForAlbumArtwork() {
        assertNull(selectFolderArtwork(listOf(image("albumartist.jpg"), image("photo.jpg"))))
    }

    @Test
    fun recognizesCommonDiscFolderNamesOnly() {
        listOf("CD1", "CD 2", "Disc 1", "Disk_2", "Side A", "Bonus Disc").forEach { name ->
            assertTrue("Expected $name to be a disc folder", isDiscFolder(name))
        }
        assertEquals(false, isDiscFolder("Album 1"))
        assertEquals(false, isDiscFolder("Discography"))
    }

    private fun image(name: String, size: Long = 1_000) = ScannedDocument(
        documentId = name,
        name = name,
        mimeType = "image/${name.substringAfterLast('.')}",
        modifiedMs = 1,
        sizeBytes = size,
    )
}
