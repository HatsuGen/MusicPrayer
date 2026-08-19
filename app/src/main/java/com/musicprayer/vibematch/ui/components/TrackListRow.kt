package com.musicprayer.vibematch.ui.components

import androidx.compose.foundation.background
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
import com.musicprayer.vibematch.model.Track

@Composable fun TrackListRow(track: Track, active: Boolean, onPlay: () -> Unit, onHold: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 3.dp).clip(RoundedCornerShape(12.dp))
            .background(if (active) MaterialTheme.colorScheme.primary.copy(.20f) else MaterialTheme.colorScheme.surface)
            .holdClickable(
                selected = active,
                clickLabel = "Play ${track.title}",
                holdLabel = "Show options for ${track.title}",
                onClick = onPlay,
                onHold = onHold,
            )
            .padding(10.dp), verticalAlignment = Alignment.CenterVertically,
    ) {
        AlbumArtwork(track, Modifier.size(50.dp).clip(RoundedCornerShape(8.dp)))
        Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) {
            Text(track.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold)
            Text("${track.artist} - ${track.album}", color = MaterialTheme.colorScheme.onSurface.copy(.55f), maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelSmall)
        }
    }
}
