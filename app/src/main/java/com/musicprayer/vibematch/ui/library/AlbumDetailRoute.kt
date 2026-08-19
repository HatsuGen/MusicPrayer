package com.musicprayer.vibematch.ui.library

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.musicprayer.vibematch.MainUiState
import com.musicprayer.vibematch.model.Track
import com.musicprayer.vibematch.model.albumCoverTrack
import com.musicprayer.vibematch.model.albumDisplayArtist
import com.musicprayer.vibematch.ui.components.AddToPlaylistDialog
import com.musicprayer.vibematch.ui.components.TrackActionsBottomSheet
import com.musicprayer.vibematch.ui.components.TrackDetailsDialog

@Composable
fun AlbumDetailRoute(
    state: MainUiState,
    albumName: String,
    tracks: List<Track>,
    onBack: () -> Unit,
    onPlayContext: (Track, List<Track>, String) -> Unit,
    onPlayNext: (Track) -> Unit,
    onAddToQueue: (Track) -> Unit,
    onAddToPlaylist: (Long, Long) -> Unit,
    onCreatePlaylistWithTrack: (String, Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var actionTrack by remember { mutableStateOf<Track?>(null) }
    var playlistTrack by remember { mutableStateOf<Track?>(null) }
    var detailsTrack by remember { mutableStateOf<Track?>(null) }
    val queueTitle = "Album: $albumName"

    AlbumDetailScreen(
        albumName = albumName,
        albumArtist = tracks.albumDisplayArtist(),
        tracks = tracks,
        coverTrack = tracks.albumCoverTrack(),
        currentTrackId = state.playback.currentId,
        onBack = onBack,
        onPlayAll = {
            tracks.firstOrNull()?.let { first -> onPlayContext(first, tracks, queueTitle) }
        },
        onPlayTrack = { track -> onPlayContext(track, tracks, queueTitle) },
        onTrackHold = { actionTrack = it },
        modifier = modifier,
    )

    actionTrack?.let { track ->
        TrackActionsBottomSheet(
            track = track,
            onDismiss = { actionTrack = null },
            onPlayNow = {
                actionTrack = null
                onPlayContext(track, tracks, queueTitle)
            },
            onPlayNext = {
                actionTrack = null
                onPlayNext(track)
            },
            onAddToQueue = {
                actionTrack = null
                onAddToQueue(track)
            },
            onAddToPlaylist = {
                actionTrack = null
                playlistTrack = track
            },
            onShowDetails = {
                actionTrack = null
                detailsTrack = track
            },
        )
    }
    playlistTrack?.let { track ->
        AddToPlaylistDialog(
            track = track,
            playlists = state.playlists,
            onAdd = onAddToPlaylist,
            onCreateWithTrack = onCreatePlaylistWithTrack,
            onDismiss = { playlistTrack = null },
        )
    }
    detailsTrack?.let { track ->
        TrackDetailsDialog(track = track, onDismiss = { detailsTrack = null })
    }
}
