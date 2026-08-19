package com.musicprayer.vibematch.ui.player

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.musicprayer.vibematch.MainUiState
import com.musicprayer.vibematch.R
import com.musicprayer.vibematch.model.Track
import com.musicprayer.vibematch.model.hasBrowsableAlbum
import com.musicprayer.vibematch.playback.PlaybackMode
import com.musicprayer.vibematch.ui.components.AlbumArtwork

@Composable fun PlayerScreen(
    state: MainUiState, modifier: Modifier = Modifier, onCollapse: () -> Unit,
    onToggle: () -> Unit, onPrevious: () -> Unit, onNext: () -> Unit,
    onSeek: (Long) -> Unit, onMode: (PlaybackMode) -> Unit,
    onSelectUpcoming: (Long) -> Unit,
    onOpenAlbum: (Track) -> Unit,
) {
    val track = state.currentTrack
    if (track == null) {
        Box(modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(painterResource(R.drawable.ic_music_note), null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.secondary)
                Text("Nothing is playing", fontSize = 24.sp, fontWeight = FontWeight.Black)
                Text("Choose a track from Music to start.", color = MaterialTheme.colorScheme.onBackground.copy(.6f))
            }
        }
        return
    }
    var showingUpcoming by rememberSaveable { mutableStateOf(false) }
    BackHandler(enabled = showingUpcoming) { showingUpcoming = false }
    if (showingUpcoming) {
        UpcomingQueueScreen(
            state = state,
            modifier = modifier,
            onBack = { showingUpcoming = false },
            onSelectTrack = {
                onSelectUpcoming(it)
                showingUpcoming = false
            },
        )
        return
    }
    var scrubbing by remember(track.id) { mutableStateOf(false) }
    var draftPosition by remember(track.id) { mutableFloatStateOf(state.playback.positionMs.toFloat()) }
    LaunchedEffect(state.playback.positionMs, scrubbing) { if (!scrubbing) draftPosition = state.playback.positionMs.toFloat() }
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().navigationBarsPadding().padding(horizontal = 26.dp, vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.fillMaxWidth()) {
            IconButton(onClick = onCollapse, modifier = Modifier.align(Alignment.CenterStart)) { Icon(painterResource(R.drawable.ic_expand_more), contentDescription = "Minimize player") }
            Text("NOW PLAYING", Modifier.align(Alignment.Center), color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Black, letterSpacing = 2.sp, fontSize = 12.sp)
            IconButton(onClick = { showingUpcoming = true }, modifier = Modifier.align(Alignment.CenterEnd)) {
                Icon(
                    painterResource(R.drawable.ic_queue_music),
                    contentDescription = "Open upcoming songs, ${state.playback.upcomingIds.size} queued",
                )
            }
        }
        Spacer(Modifier.height(18.dp))
        AlbumArtwork(
            track = track,
            modifier = Modifier.fillMaxWidth().aspectRatio(1f),
            containerColor = Color.Transparent,
        )
        Spacer(Modifier.height(24.dp))
        Text(track.title, Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontSize = 25.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
        PlayerTrackMetadata(track = track, onOpenAlbum = onOpenAlbum)
        state.playback.errorMessage?.let { message -> Text(message, Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center) }
        Spacer(Modifier.height(12.dp))
        Slider(
            value = draftPosition.coerceIn(0f, state.playback.durationMs.coerceAtLeast(1).toFloat()),
            onValueChange = { scrubbing = true; draftPosition = it },
            onValueChangeFinished = { scrubbing = false; onSeek(draftPosition.toLong()) },
            valueRange = 0f..state.playback.durationMs.coerceAtLeast(1).toFloat(),
            enabled = state.playback.durationMs > 0 && state.playback.isSeekable,
        )
        Row(Modifier.fillMaxWidth()) {
            Text(formatTime(draftPosition.toLong()), style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.weight(1f))
            Text("-${formatTime((state.playback.durationMs - draftPosition.toLong()).coerceAtLeast(0))}", style = MaterialTheme.typography.labelSmall)
        }
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onPrevious) { Icon(painterResource(R.drawable.ic_skip_previous), contentDescription = "Previous", Modifier.size(32.dp)) }
            FilledIconButton(onClick = onToggle, Modifier.size(68.dp), colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.secondary, contentColor = MaterialTheme.colorScheme.background)) {
                Icon(painterResource(if (state.playback.isPlaying) R.drawable.ic_pause else R.drawable.ic_play), contentDescription = if (state.playback.isPlaying) "Pause" else "Play", Modifier.size(30.dp))
            }
            IconButton(onClick = onNext) { Icon(painterResource(R.drawable.ic_skip_next), contentDescription = "Next", Modifier.size(32.dp)) }
        }
        Spacer(Modifier.height(18.dp))
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            PlaybackMode.entries.forEachIndexed { index, mode ->
                val icon = when (mode) { PlaybackMode.SEQUENTIAL -> R.drawable.ic_queue_music; PlaybackMode.SHUFFLE -> R.drawable.ic_shuffle; PlaybackMode.REPEAT_ONE -> R.drawable.ic_repeat_one }
                SegmentedButton(
                    selected = state.playback.mode == mode, onClick = { onMode(mode) },
                    shape = SegmentedButtonDefaults.itemShape(index, PlaybackMode.entries.size),
                    icon = { Icon(painterResource(icon), null, Modifier.size(17.dp)) },
                    label = { Text(when (mode) { PlaybackMode.SEQUENTIAL -> "Order"; PlaybackMode.SHUFFLE -> "Shuffle"; PlaybackMode.REPEAT_ONE -> "Loop one" }, fontSize = 10.sp) },
                )
            }
        }
    }
}

@Composable
private fun PlayerTrackMetadata(track: Track, onOpenAlbum: (Track) -> Unit) {
    val muted = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.58f)
    val albumIsBrowsable = track.hasBrowsableAlbum()
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = track.artist,
            modifier = Modifier.weight(1f),
            color = muted,
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text("  •  ", color = muted)
        Row(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 48.dp)
                .clip(RoundedCornerShape(7.dp))
                .clickable(
                    enabled = albumIsBrowsable,
                    onClickLabel = "Open album ${track.album}",
                    role = Role.Button,
                    onClick = { onOpenAlbum(track) },
                )
                .padding(horizontal = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = track.album,
                color = if (albumIsBrowsable) MaterialTheme.colorScheme.secondary else muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (albumIsBrowsable) {
                Spacer(Modifier.width(2.dp))
                Icon(
                    painter = painterResource(R.drawable.ic_chevron_right),
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.secondary,
                )
            }
        }
    }
}

private fun formatTime(milliseconds: Long): String { val seconds = milliseconds / 1_000; return "%d:%02d".format(seconds / 60, seconds % 60) }
