package com.musicprayer.vibematch.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import com.musicprayer.vibematch.R
import com.musicprayer.vibematch.MainUiState

@Composable fun MiniPlayer(state: MainUiState, onOpen: () -> Unit, onToggle: () -> Unit, onNext: () -> Unit) {
    val track = state.currentTrack ?: return
    Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 12.dp) {
        Column(Modifier.clickable(onClick = onOpen)) {
            LinearProgressIndicator(
                progress = { if (state.playback.durationMs > 0) state.playback.positionMs.toFloat() / state.playback.durationMs else 0f },
                Modifier.fillMaxWidth().height(2.dp), trackColor = MaterialTheme.colorScheme.surface,
            )
            Row(Modifier.fillMaxWidth().padding(10.dp, 7.dp), verticalAlignment = Alignment.CenterVertically) {
                AlbumArtwork(track, Modifier.size(44.dp).clip(RoundedCornerShape(7.dp)))
                Spacer(Modifier.width(11.dp)); Column(Modifier.weight(1f)) {
                    Text(track.title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(track.artist, color = MaterialTheme.colorScheme.onSurface.copy(.55f), style = MaterialTheme.typography.labelSmall, maxLines = 1)
                }
                FilledIconButton(onClick = onToggle) { Icon(painterResource(if (state.playback.isPlaying) R.drawable.ic_pause else R.drawable.ic_play), contentDescription = if (state.playback.isPlaying) "Pause" else "Play") }
                IconButton(onClick = onNext) { Icon(painterResource(R.drawable.ic_skip_next), contentDescription = "Next") }
            }
        }
    }
}
