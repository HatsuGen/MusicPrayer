package com.musicprayer.vibematch.ui.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.musicprayer.vibematch.MainUiState
import com.musicprayer.vibematch.R
import com.musicprayer.vibematch.playback.PlaybackQueueKind
import com.musicprayer.vibematch.ui.components.AlbumArtwork

@Composable
fun UpcomingQueueScreen(
    state: MainUiState,
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    onSelectTrack: (Long) -> Unit,
) {
    val current = state.currentTrack
    val upcoming = remember(state.tracks, state.playback.upcomingIds) {
        val tracksById = state.tracks.associateBy(com.musicprayer.vibematch.model.Track::id)
        state.playback.upcomingIds.mapNotNull(tracksById::get)
    }
    Column(
        modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(),
    ) {
        Box(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 10.dp)) {
            IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
                Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = "Back to Now Playing")
            }
            Text(
                "UP NEXT",
                Modifier.align(Alignment.Center),
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
                fontSize = 12.sp,
            )
            Text(
                upcoming.size.toString(),
                Modifier.align(Alignment.CenterEnd).padding(end = 16.dp),
                color = MaterialTheme.colorScheme.onBackground.copy(.55f),
                style = MaterialTheme.typography.labelLarge,
            )
        }

        Text(
            text = state.playback.queueTitle ?: "Current queue",
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 6.dp),
            fontWeight = FontWeight.Black,
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            text = if (state.playback.queueKind == PlaybackQueueKind.SOLO_RANDOM) {
                "A no-repeat random session. New songs are added as you listen."
            } else {
                "Songs follow the selected playlist, album, artist or genre."
            },
            modifier = Modifier.padding(horizontal = 22.dp),
            color = MaterialTheme.colorScheme.onBackground.copy(.58f),
            style = MaterialTheme.typography.bodySmall,
        )

        current?.let { track ->
            Surface(
                modifier = Modifier.fillMaxWidth().padding(18.dp, 18.dp, 18.dp, 10.dp),
                shape = RoundedCornerShape(15.dp),
                color = MaterialTheme.colorScheme.primary.copy(.16f),
            ) {
                Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    AlbumArtwork(track, Modifier.size(52.dp).clip(RoundedCornerShape(9.dp)))
                    Spacer(Modifier.width(13.dp))
                    Column(Modifier.weight(1f)) {
                        Text("NOW PLAYING", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Black, fontSize = 10.sp, letterSpacing = 1.sp)
                        Text(track.title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(track.artist, color = MaterialTheme.colorScheme.onSurface.copy(.55f), style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }

        HorizontalDivider(Modifier.padding(horizontal = 18.dp))
        if (upcoming.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(28.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(painterResource(R.drawable.ic_queue_music), null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onBackground.copy(.38f))
                    Spacer(Modifier.height(10.dp))
                    Text("No more songs queued", fontWeight = FontWeight.Bold)
                    Text("Choose another track to start a new session.", color = MaterialTheme.colorScheme.onBackground.copy(.55f), style = MaterialTheme.typography.bodySmall)
                }
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
            ) {
                itemsIndexed(upcoming, key = { _, track -> track.id }) { index, track ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { onSelectTrack(track.id) }.padding(9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            (index + 1).toString(),
                            Modifier.width(30.dp),
                            color = MaterialTheme.colorScheme.onBackground.copy(.42f),
                            style = MaterialTheme.typography.labelMedium,
                        )
                        AlbumArtwork(track, Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(track.title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${track.artist} - ${track.album}", color = MaterialTheme.colorScheme.onBackground.copy(.52f), style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}
