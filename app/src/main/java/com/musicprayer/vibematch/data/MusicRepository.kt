package com.musicprayer.vibematch.data

import android.net.Uri
import com.musicprayer.vibematch.model.Track
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MusicRepository(
    private val dao: TrackDao,
    private val scanner: MediaStoreScanner,
) {
    val tracks: Flow<List<Track>> = dao.observeAll().map { rows -> rows.map(TrackEntity::toModel) }

    suspend fun refresh(folderUri: Uri? = null) {
        val rows = scanner.scan(folderUri)
        dao.replaceAll(rows)
    }

    suspend fun clear() = dao.deleteAll()
}
