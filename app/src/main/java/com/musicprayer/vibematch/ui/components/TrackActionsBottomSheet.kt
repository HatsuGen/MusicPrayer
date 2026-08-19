package com.musicprayer.vibematch.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.musicprayer.vibematch.R
import com.musicprayer.vibematch.model.Track

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackActionsBottomSheet(
    track: Track,
    onDismiss: () -> Unit,
    onPlayNow: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onShowDetails: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
    ) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AlbumArtwork(track, Modifier.size(58.dp).clip(RoundedCornerShape(10.dp)))
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(track.title, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(track.artist, color = MaterialTheme.colorScheme.onSurface.copy(.58f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            HorizontalDivider(Modifier.padding(horizontal = 20.dp, vertical = 6.dp))
            TrackAction(R.drawable.ic_play, "Play now", onPlayNow)
            TrackAction(R.drawable.ic_play_next, "Play next", onPlayNext)
            TrackAction(R.drawable.ic_queue_music, "Add to queue", onAddToQueue)
            TrackAction(R.drawable.ic_playlist_add, "Add to playlist", onAddToPlaylist)
            TrackAction(R.drawable.ic_info, "Track details", onShowDetails)
            Spacer(Modifier.navigationBarsPadding().height(10.dp))
        }
    }
}

@Composable
private fun TrackAction(@DrawableRes icon: Int, label: String, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(label, fontWeight = FontWeight.SemiBold) },
        leadingContent = { Icon(painterResource(icon), contentDescription = null) },
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 6.dp),
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
    )
}

@Composable
fun TrackDetailsDialog(track: Track, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Track details") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                DetailRow("Title", track.title)
                DetailRow("Artist", track.artist)
                DetailRow("Album", track.album)
                DetailRow("Genre", track.genre)
                DetailRow("Duration", formatDuration(track.durationMs))
                track.mimeType?.let { DetailRow("Format", it.substringAfter('/').uppercase()) }
                track.sampleRate?.let { DetailRow("Sample rate", "${it / 1_000f} kHz") }
                track.bitDepth?.let { DetailRow("Bit depth", "$it-bit") }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("CLOSE") } },
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, Modifier.width(92.dp), color = MaterialTheme.colorScheme.onSurface.copy(.55f), style = MaterialTheme.typography.labelMedium)
        Text(value, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
    }
}

private fun formatDuration(milliseconds: Long): String {
    val seconds = milliseconds / 1_000
    return "%d:%02d".format(seconds / 60, seconds % 60)
}
