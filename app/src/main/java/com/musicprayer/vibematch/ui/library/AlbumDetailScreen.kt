package com.musicprayer.vibematch.ui.library

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.musicprayer.vibematch.R
import com.musicprayer.vibematch.model.Track
import com.musicprayer.vibematch.ui.components.AlbumArtwork
import com.musicprayer.vibematch.ui.components.holdClickable

/**
 * Reusable album playlist page used by Library and the Now Playing album shortcut.
 *
 * Track actions stay behind the same two-second hold gesture used by the rest of
 * the app, so this screen does not bring back an inline add/menu button.
 */
@Composable
fun AlbumDetailScreen(
    albumName: String,
    tracks: List<Track>,
    currentTrackId: Long?,
    onBack: () -> Unit,
    onPlayAll: () -> Unit,
    onPlayTrack: (Track) -> Unit,
    onTrackHold: (Track) -> Unit,
    modifier: Modifier = Modifier,
    albumArtist: String? = null,
    coverTrack: Track? = tracks.firstOrNull(),
) {
    BackHandler(onBack = onBack)
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 16.dp),
    ) {
        item(key = "album_header") {
            AlbumHeader(
                albumName = albumName,
                albumArtist = albumArtist,
                coverTrack = coverTrack,
                trackCount = tracks.size,
                totalDurationMs = tracks.fold(0L) { total, track -> total + track.durationMs },
                onBack = onBack,
            )
        }
        item(key = "album_controls") {
            AlbumPlaybackControls(
                enabled = tracks.isNotEmpty(),
                onPlayAll = onPlayAll,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.10f))
        }
        itemsIndexed(
            items = tracks,
            key = { _, track -> track.id },
        ) { index, track ->
            AlbumTrackRow(
                number = index + 1,
                track = track,
                active = track.id == currentTrackId,
                onPlay = { onPlayTrack(track) },
                onHold = { onTrackHold(track) },
            )
        }
    }
}

@Composable
private fun AlbumHeader(
    albumName: String,
    albumArtist: String?,
    coverTrack: Track?,
    trackCount: Int,
    totalDurationMs: Long,
    onBack: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 220.dp, max = 250.dp)
            .aspectRatio(1.72f)
            .clip(RoundedCornerShape(bottomStart = 26.dp, bottomEnd = 26.dp)),
    ) {
        AlbumArtwork(
            track = coverTrack,
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.surface,
            contentScale = ContentScale.Crop,
        )
        // A flat scrim keeps text readable without introducing a gradient.
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.62f)))

        IconButton(
            onClick = onBack,
            modifier = Modifier
                .padding(start = 8.dp, top = 4.dp)
                .align(Alignment.TopStart),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_back),
                contentDescription = "Back",
                tint = Color.White,
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 28.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AlbumArtwork(
                track = coverTrack,
                modifier = Modifier
                    .size(112.dp)
                    .clip(RoundedCornerShape(12.dp)),
                containerColor = Color.Black.copy(alpha = 0.22f),
                contentDescription = "Cover for $albumName",
            )
            Spacer(Modifier.width(16.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = albumName.ifBlank { "Unknown album" },
                    color = Color.White,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.semantics { heading() },
                )
                if (!albumArtist.isNullOrBlank()) {
                    Spacer(Modifier.height(5.dp))
                    Text(
                        text = albumArtist,
                        color = Color.White.copy(alpha = 0.76f),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text = albumSummary(trackCount, totalDurationMs),
                    color = Color.White.copy(alpha = 0.88f),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                )
            }
        }
    }
}

@Composable
private fun AlbumPlaybackControls(
    enabled: Boolean,
    onPlayAll: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onPlayAll)
            .padding(horizontal = 20.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier.size(46.dp),
            shape = CircleShape,
            color = if (enabled) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
            },
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(R.drawable.ic_play),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = if (enabled) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                    },
                )
            }
        }
        Spacer(Modifier.width(15.dp))
        Text(
            text = "Play all",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = if (enabled) {
                MaterialTheme.colorScheme.onBackground
            } else {
                MaterialTheme.colorScheme.onBackground.copy(alpha = 0.38f)
            },
        )
    }
}

@Composable
private fun AlbumTrackRow(
    number: Int,
    track: Track,
    active: Boolean,
    onPlay: () -> Unit,
    onHold: () -> Unit,
) {
    val rowColor = if (active) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
    } else {
        Color.Transparent
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(rowColor)
            .holdClickable(
                selected = active,
                clickLabel = "Play ${track.title}",
                holdLabel = "Show options for ${track.title}",
                onClick = onPlay,
                onHold = onHold,
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = number.toString(),
            modifier = Modifier.width(30.dp),
            color = if (active) {
                MaterialTheme.colorScheme.secondary
            } else {
                MaterialTheme.colorScheme.onBackground.copy(alpha = 0.68f)
            },
            style = MaterialTheme.typography.labelLarge,
        )
        AlbumArtwork(
            track = track,
            modifier = Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(8.dp)),
        )
        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = track.title,
                color = if (active) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = track.artist.ifBlank { "Unknown artist" },
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.70f),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = formatTrackDuration(track.durationMs),
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.70f),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

private fun albumSummary(trackCount: Int, totalDurationMs: Long): String {
    val songs = if (trackCount == 1) "1 song" else "$trackCount songs"
    return "$songs in total (${formatAlbumDuration(totalDurationMs)})"
}

private fun formatTrackDuration(durationMs: Long): String {
    val totalSeconds = (durationMs.coerceAtLeast(0L) / 1_000L)
    return "%d:%02d".format(totalSeconds / 60L, totalSeconds % 60L)
}

private fun formatAlbumDuration(durationMs: Long): String {
    val totalSeconds = durationMs.coerceAtLeast(0L) / 1_000L
    val hours = totalSeconds / 3_600L
    val minutes = totalSeconds % 3_600L / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}
