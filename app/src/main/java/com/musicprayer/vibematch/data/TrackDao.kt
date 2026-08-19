package com.musicprayer.vibematch.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackDao {
    @Query(
        """SELECT * FROM tracks
            ORDER BY album COLLATE NOCASE,
                CASE WHEN discNumber IS NULL THEN 1 ELSE 0 END,
                discNumber,
                CASE WHEN trackNumber IS NULL THEN 1 ELSE 0 END,
                trackNumber,
                title COLLATE NOCASE,
                id""",
    )
    fun observeAll(): Flow<List<TrackEntity>>

    @Upsert
    suspend fun upsertAll(tracks: List<TrackEntity>)

    @Query("DELETE FROM tracks")
    suspend fun deleteAll()

    @Transaction
    suspend fun replaceAll(tracks: List<TrackEntity>) {
        deleteAll()
        if (tracks.isNotEmpty()) upsertAll(tracks)
    }
}
