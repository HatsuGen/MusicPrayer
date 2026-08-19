package com.musicprayer.vibematch.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.musicprayer.vibematch.MainUiState
import com.musicprayer.vibematch.R
import com.musicprayer.vibematch.model.Playlist
import com.musicprayer.vibematch.model.Track
import com.musicprayer.vibematch.ui.components.AlbumArtwork

@Composable fun HomeScreen(
    state: MainUiState, onPlay: (Track) -> Unit, onPlayPlaylist: (Playlist) -> Unit,
    onCreatePlaylist: (String) -> Unit, onShowMixes: (Boolean) -> Unit, onOpenLibrary: () -> Unit,
) {
    var creatingPlaylist by remember { mutableStateOf(false) }
    if (creatingPlaylist) CreatePlaylistDialog(onCreatePlaylist) { creatingPlaylist = false }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            Box(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).statusBarsPadding().padding(22.dp, 18.dp, 22.dp, 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondary), contentAlignment = Alignment.Center) { Text("MP", color = MaterialTheme.colorScheme.onSecondary, fontWeight = FontWeight.Black) }
                    Spacer(Modifier.width(13.dp)); Column { Text("Welcome back!", fontWeight = FontWeight.Bold, fontSize = 17.sp); Text("Your sound, your way", color = MaterialTheme.colorScheme.onBackground.copy(.58f), fontSize = 12.sp) }
                }
            }
        }
        item { SectionHeading("Continue Listening") }
        item {
            val tracks = state.continueListeningTracks
            if (tracks.isEmpty()) EmptyListeningCard(onOpenLibrary)
            else Column(Modifier.padding(horizontal = 18.dp)) { tracks.chunked(2).forEach { row -> Row(Modifier.fillMaxWidth()) { row.forEach { ContinueCard(it, onPlay, Modifier.weight(1f)) }; if (row.size == 1) Spacer(Modifier.weight(1f)) } } }
        }
        if (state.preferences.showTopMixes) {
            item {
                Row(Modifier.fillMaxWidth().padding(start = 22.dp, top = 22.dp, end = 14.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Your Top Mixes", Modifier.weight(1f), fontWeight = FontWeight.ExtraBold, fontSize = 21.sp)
                    TextButton(onClick = { creatingPlaylist = true }) { Icon(painterResource(R.drawable.ic_playlist_add), null, Modifier.size(18.dp)); Spacer(Modifier.width(5.dp)); Text("PLAYLIST") }
                    TextButton(onClick = { onShowMixes(false) }) { Text("HIDE") }
                }
            }
            item {
                if (state.playlists.isEmpty()) EmptyPlaylistCard { creatingPlaylist = true }
                else Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 22.dp)) { state.playlists.forEachIndexed { index, playlist -> PlaylistCard(playlist, index, state, onPlayPlaylist) } }
            }
        } else item {
            TextButton(onClick = { onShowMixes(true) }, modifier = Modifier.padding(start = 14.dp, top = 16.dp)) { Text("SHOW YOUR TOP MIXES") }
        }
    }
}

@Composable private fun SectionHeading(text: String) { Text(text, Modifier.padding(start = 22.dp, top = 22.dp, bottom = 12.dp), fontWeight = FontWeight.ExtraBold, fontSize = 21.sp) }

@Composable private fun ContinueCard(track: Track, onPlay: (Track) -> Unit, modifier: Modifier) {
    Row(modifier.padding(4.dp).height(62.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surface).clickable { onPlay(track) }, verticalAlignment = Alignment.CenterVertically) {
        AlbumArtwork(track, Modifier.fillMaxHeight().width(62.dp)); Text(track.title, Modifier.padding(horizontal = 11.dp).weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable private fun EmptyListeningCard(onOpenLibrary: () -> Unit) {
    Row(Modifier.padding(horizontal = 22.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).clickable(onClick = onOpenLibrary).padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(painterResource(R.drawable.ic_music_note), null, Modifier.size(30.dp), tint = MaterialTheme.colorScheme.secondary); Spacer(Modifier.width(14.dp)); Column { Text("Start listening", fontWeight = FontWeight.Bold); Text("Choose a folder and play your first track", color = MaterialTheme.colorScheme.onSurface.copy(.55f), fontSize = 12.sp) }
    }
}

@Composable private fun EmptyPlaylistCard(create: () -> Unit) {
    Column(Modifier.padding(horizontal = 22.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).clickable(onClick = create).padding(20.dp)) { Text("No playlists yet", fontWeight = FontWeight.Bold); Spacer(Modifier.height(4.dp)); Text("Create a playlist to make it appear in Top Mixes.", color = MaterialTheme.colorScheme.onSurface.copy(.6f), fontSize = 12.sp) }
}

@Composable private fun PlaylistCard(playlist: Playlist, index: Int, state: MainUiState, onPlay: (Playlist) -> Unit) {
    val colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.tertiary, MaterialTheme.colorScheme.error); val accent = colors[index % colors.size]
    Box(Modifier.padding(end = 14.dp).size(158.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surface).clickable { onPlay(playlist) }) {
        Box(Modifier.size(86.dp).offset(96.dp, 84.dp).background(MaterialTheme.colorScheme.surface.copy(.72f), CircleShape))
        Column(Modifier.padding(16.dp)) { Text(playlist.name, maxLines = 2, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Black, fontSize = 18.sp); Spacer(Modifier.height(5.dp)); Text("${playlist.trackIds.size} tracks", fontSize = 11.sp) }
        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(7.dp).background(accent))
    }
}

@Composable private fun CreatePlaylistDialog(create: (String) -> Unit, dismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = dismiss, title = { Text("New playlist") }, text = { OutlinedTextField(name, { name = it }, label = { Text("Playlist name") }, singleLine = true) }, confirmButton = { TextButton(onClick = { create(name); dismiss() }, enabled = name.isNotBlank()) { Text("CREATE") } }, dismissButton = { TextButton(onClick = dismiss) { Text("CANCEL") } })
}
