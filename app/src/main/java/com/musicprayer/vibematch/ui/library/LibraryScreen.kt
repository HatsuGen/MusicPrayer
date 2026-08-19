package com.musicprayer.vibematch.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import com.musicprayer.vibematch.MainUiState
import com.musicprayer.vibematch.R
import com.musicprayer.vibematch.model.Track
import com.musicprayer.vibematch.model.albumCollectionKey
import com.musicprayer.vibematch.ui.components.AddToPlaylistDialog
import com.musicprayer.vibematch.ui.components.AlbumArtwork
import com.musicprayer.vibematch.ui.components.TrackActionsBottomSheet
import com.musicprayer.vibematch.ui.components.TrackDetailsDialog
import com.musicprayer.vibematch.ui.components.TrackListRow
import java.util.Locale

private enum class LibraryView(val label: String) { SONGS("Songs"), ALBUMS("Albums"), ARTISTS("Artists"), GENRES("Genres") }
private data class LibraryGroup(val id: String, val label: String, val tracks: List<Track>)
private data class TrackActionTarget(val track: Track, val queue: List<Track>, val queueTitle: String?)

@Composable fun LibraryScreen(
    state: MainUiState, onRefresh: () -> Unit, onSearch: (String) -> Unit,
    onPlaySolo: (Track, List<Track>) -> Unit,
    onPlayContext: (Track, List<Track>, String) -> Unit,
    onPlayNext: (Track) -> Unit,
    onAddToQueue: (Track) -> Unit,
    onAddToPlaylist: (Long, Long) -> Unit,
    onCreatePlaylistWithTrack: (String, Long) -> Unit,
    onOpenAlbum: (Track) -> Unit,
) {
    var view by rememberSaveable { mutableStateOf(LibraryView.SONGS) }
    var selectedGroup by rememberSaveable { mutableStateOf<String?>(null) }
    var actionTarget by remember { mutableStateOf<TrackActionTarget?>(null) }
    var playlistTrack by remember { mutableStateOf<Track?>(null) }
    var detailsTrack by remember { mutableStateOf<Track?>(null) }
    val searchHint = when (view) { LibraryView.SONGS -> "Search songs, artists, albums or genres"; LibraryView.ALBUMS -> "Search albums"; LibraryView.ARTISTS -> "Search artists"; LibraryView.GENRES -> "Search genres" }
    actionTarget?.let { target ->
        TrackActionsBottomSheet(
            track = target.track,
            onDismiss = { actionTarget = null },
            onPlayNow = {
                actionTarget = null
                target.queueTitle?.let { onPlayContext(target.track, target.queue, it) }
                    ?: onPlaySolo(target.track, target.queue)
            },
            onPlayNext = { actionTarget = null; onPlayNext(target.track) },
            onAddToQueue = { actionTarget = null; onAddToQueue(target.track) },
            onAddToPlaylist = { actionTarget = null; playlistTrack = target.track },
            onShowDetails = { actionTarget = null; detailsTrack = target.track },
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
    detailsTrack?.let { track -> TrackDetailsDialog(track) { detailsTrack = null } }
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(22.dp, 16.dp, 22.dp, 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text("Your Music", fontSize = 29.sp, fontWeight = FontWeight.Black); Text("${state.tracks.size} tracks", color = MaterialTheme.colorScheme.secondary) }
            TextButton(onClick = onRefresh, enabled = state.preferences.folderUri != null && !state.isScanning) { Icon(painterResource(R.drawable.ic_refresh), null, Modifier.size(17.dp)); Spacer(Modifier.width(5.dp)); Text(if (state.isScanning) "SCANNING" else "RESCAN") }
        }
        if (state.isScanning) LinearProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 4.dp))
        OutlinedTextField(state.query, onSearch, Modifier.fillMaxWidth().padding(18.dp, 10.dp), placeholder = { Text(searchHint) }, leadingIcon = { Icon(painterResource(R.drawable.ic_search), null) }, singleLine = true, shape = RoundedCornerShape(15.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 18.dp)) {
            LibraryView.entries.forEach { item -> FilterChip(view == item, { view = item; selectedGroup = null }, { Text(item.label) }, Modifier.padding(end = 8.dp)) }
        }
        Spacer(Modifier.height(8.dp))
        if (state.preferences.folderUri == null) NoFolderConfiguredState()
        else if (view == LibraryView.SONGS) {
            val tracks = state.visibleTracks
            TrackList(
                tracks = tracks,
                state = state,
                onPlay = { onPlaySolo(it, tracks) },
                onHold = { actionTarget = TrackActionTarget(it, tracks, null) },
            )
        } else GroupBrowser(
            view = view,
            selected = selectedGroup,
            state = state,
            onSelect = { selectedGroup = it },
            onBack = { selectedGroup = null },
            onPlay = onPlayContext,
            onOpenAlbum = onOpenAlbum,
            onAction = { track, queue, title -> actionTarget = TrackActionTarget(track, queue, title) },
        )
    }
}

@Composable private fun NoFolderConfiguredState() { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(painterResource(R.drawable.ic_music_note), null, Modifier.size(58.dp), tint = MaterialTheme.colorScheme.secondary); Text("No music folder selected", fontSize = 21.sp, fontWeight = FontWeight.Black); Spacer(Modifier.height(6.dp)); Text("Choose a folder from Settings, then scan it once.", color = MaterialTheme.colorScheme.onBackground.copy(.6f)) } } }

@Composable private fun TrackList(tracks: List<Track>, state: MainUiState, onPlay: (Track) -> Unit, onHold: (Track) -> Unit) {
    if (tracks.isEmpty()) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No matching music found") }; return }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
        items(tracks, key = Track::id) { track ->
            TrackListRow(track, track.id == state.playback.currentId, { onPlay(track) }, { onHold(track) })
        }
    }
}

@Composable private fun GroupBrowser(
    view: LibraryView,
    selected: String?,
    state: MainUiState,
    onSelect: (String) -> Unit,
    onBack: () -> Unit,
    onPlay: (Track, List<Track>, String) -> Unit,
    onOpenAlbum: (Track) -> Unit,
    onAction: (Track, List<Track>, String) -> Unit,
) {
    val selector: (Track) -> String = when (view) { LibraryView.GENRES -> Track::genre; LibraryView.ARTISTS -> Track::artist; else -> Track::album }
    val groups = remember(state.tracks, state.query, view) {
        val matchingTracks = state.tracks.filter { track -> state.query.isBlank() || selector(track).contains(state.query, ignoreCase = true) }
        buildLibraryGroups(view, matchingTracks)
    }
    val selectedGroup = groups.firstOrNull { it.id == selected }
    val groupIds = remember(groups) { groups.mapTo(mutableSetOf(), LibraryGroup::id) }
    LaunchedEffect(selected, groupIds) { if (selected != null && selected !in groupIds) onBack() }
    if (selectedGroup != null) {
        val tracks = selectedGroup.tracks
        val queueTitle = "${view.label.dropLast(1)}: ${selectedGroup.label}"
        Row(Modifier.fillMaxWidth().clickable(onClick = onBack).padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) { IconButton(onClick = onBack) { Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = "Back") }; if (view == LibraryView.ALBUMS) { AlbumArtwork(tracks.representativeArtworkTrack(), Modifier.size(52.dp).clip(RoundedCornerShape(9.dp))); Spacer(Modifier.width(12.dp)) } else Spacer(Modifier.width(2.dp)); Column(Modifier.weight(1f)) { Text(selectedGroup.label, fontWeight = FontWeight.Black, fontSize = 20.sp, maxLines = 1, overflow = TextOverflow.Ellipsis); Text("${tracks.size} tracks", color = MaterialTheme.colorScheme.onBackground.copy(.55f), fontSize = 12.sp) } }
        TrackList(
            tracks = tracks,
            state = state,
            onPlay = { onPlay(it, tracks, queueTitle) },
            onHold = { onAction(it, tracks, queueTitle) },
        )
    } else if (groups.isEmpty()) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No matching ${view.label.lowercase()} found") }
    else LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 18.dp)) {
        items(groups, key = LibraryGroup::id) { group -> Row(Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(13.dp)).background(MaterialTheme.colorScheme.surface).clickable { if (view == LibraryView.ALBUMS) group.tracks.firstOrNull()?.let(onOpenAlbum) else onSelect(group.id) }.padding(if (view == LibraryView.ALBUMS) 8.dp else 17.dp), verticalAlignment = Alignment.CenterVertically) { if (view == LibraryView.ALBUMS) { AlbumArtwork(group.tracks.representativeArtworkTrack(), Modifier.size(58.dp).clip(RoundedCornerShape(10.dp))); Spacer(Modifier.width(13.dp)) }; Column(Modifier.weight(1f)) { Text(group.label, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis); Text("${group.tracks.size} tracks", color = MaterialTheme.colorScheme.onSurface.copy(.55f), fontSize = 12.sp) }; Icon(painterResource(R.drawable.ic_chevron_right), null, tint = MaterialTheme.colorScheme.primary) } }
    }
}

private fun buildLibraryGroups(view: LibraryView, tracks: List<Track>): List<LibraryGroup> = tracks
    .groupBy { track ->
        when (view) {
            LibraryView.ALBUMS -> track.albumCollectionKey()
            LibraryView.ARTISTS -> track.artist.normalizedGroupValue()
            LibraryView.GENRES -> track.genre.normalizedGroupValue()
            LibraryView.SONGS -> track.title.normalizedGroupValue()
        }
    }
    .map { (id, groupedTracks) ->
        val label = when (view) { LibraryView.ALBUMS -> groupedTracks.first().album; LibraryView.ARTISTS -> groupedTracks.first().artist; LibraryView.GENRES -> groupedTracks.first().genre; LibraryView.SONGS -> groupedTracks.first().title }
        LibraryGroup(id, label, groupedTracks)
    }
    .sortedWith { left, right -> String.CASE_INSENSITIVE_ORDER.compare(left.label, right.label).takeIf { it != 0 } ?: left.id.compareTo(right.id) }

private fun String.normalizedGroupValue(): String = trim().lowercase(Locale.ROOT)
private fun List<Track>.representativeArtworkTrack(): Track? = firstOrNull { it.artworkUri != null } ?: firstOrNull()
