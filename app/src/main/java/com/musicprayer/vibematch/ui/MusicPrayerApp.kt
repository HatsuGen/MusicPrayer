package com.musicprayer.vibematch.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.musicprayer.vibematch.MainViewModel
import com.musicprayer.vibematch.model.Track
import com.musicprayer.vibematch.model.albumCollectionKey
import com.musicprayer.vibematch.model.tracksInAlbum
import com.musicprayer.vibematch.ui.components.AppBottomBar
import com.musicprayer.vibematch.ui.components.MiniPlayer
import com.musicprayer.vibematch.ui.home.HomeScreen
import com.musicprayer.vibematch.ui.library.AlbumDetailRoute
import com.musicprayer.vibematch.ui.library.LibraryScreen
import com.musicprayer.vibematch.ui.navigation.AppDestination
import com.musicprayer.vibematch.ui.player.PlayerScreen
import com.musicprayer.vibematch.ui.settings.SettingsScreen
import com.musicprayer.vibematch.ui.theme.MusicPrayerTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun MusicPrayerApp(viewModel: MainViewModel, chooseFolder: () -> Unit) {
    val state by viewModel.state.collectAsState()
    var destination by rememberSaveable { mutableStateOf(AppDestination.HOME) }
    var playerExpanded by rememberSaveable { mutableStateOf(false) }
    var albumRouteKey by rememberSaveable { mutableStateOf<String?>(null) }
    var albumRouteName by rememberSaveable { mutableStateOf<String?>(null) }
    var albumReturnsToPlayer by rememberSaveable { mutableStateOf(false) }
    val playerSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    LaunchedEffect(state.playback.errorMessage) { state.playback.errorMessage?.let { snackbar.showSnackbar(it) } }
    LaunchedEffect(state.libraryError) { state.libraryError?.let { snackbar.showSnackbar(it) } }
    LaunchedEffect(state.currentTrack?.id) { if (state.currentTrack == null) playerExpanded = false }
    val albumTracks = remember(state.tracks, albumRouteKey) {
        albumRouteKey?.let(state.tracks::tracksInAlbum).orEmpty()
    }
    fun openAlbum(track: Track, returnToPlayer: Boolean) {
        albumRouteKey = track.albumCollectionKey()
        albumRouteName = track.album
        albumReturnsToPlayer = returnToPlayer
    }
    fun closeAlbum() {
        val reopenPlayer = albumReturnsToPlayer && state.currentTrack != null
        albumRouteKey = null
        albumRouteName = null
        albumReturnsToPlayer = false
        if (reopenPlayer) playerExpanded = true
    }

    MusicPrayerTheme(state.preferences.themeName) {
        Scaffold(
            modifier = Modifier.fillMaxSize(), containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(snackbar) },
            bottomBar = {
                Column {
                    MiniPlayer(state, { playerExpanded = true }, viewModel::toggle, viewModel::next)
                    if (albumRouteKey == null) AppBottomBar(destination) { destination = it }
                }
            },
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                when (destination) {
                    AppDestination.HOME -> HomeScreen(state, { viewModel.playSolo(it) }, viewModel::playPlaylist, viewModel::createPlaylist, viewModel::setShowTopMixes) { destination = AppDestination.LIBRARY }
                    AppDestination.LIBRARY -> LibraryScreen(
                        state = state,
                        onRefresh = viewModel::refresh,
                        onSearch = viewModel::search,
                        onPlaySolo = viewModel::playSolo,
                        onPlayContext = viewModel::playContext,
                        onPlayNext = viewModel::playNext,
                        onAddToQueue = viewModel::addToQueue,
                        onAddToPlaylist = viewModel::addTrackToPlaylist,
                        onCreatePlaylistWithTrack = viewModel::createPlaylistWithTrack,
                        onOpenAlbum = { openAlbum(it, returnToPlayer = false) },
                    )
                    AppDestination.SETTINGS -> SettingsScreen(state, chooseFolder, viewModel::selectTheme, viewModel::setShowTopMixes, viewModel::setEqualizerEnabled, viewModel::setPreamp, viewModel::setEqBand, viewModel::resetEqualizer)
                }
                if (albumRouteKey != null) {
                    key(albumRouteKey) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.background,
                        ) {
                            AlbumDetailRoute(
                                state = state,
                                albumName = albumRouteName ?: albumTracks.firstOrNull()?.album.orEmpty(),
                                tracks = albumTracks,
                                onBack = ::closeAlbum,
                                onPlayContext = viewModel::playContext,
                                onPlayNext = viewModel::playNext,
                                onAddToQueue = viewModel::addToQueue,
                                onAddToPlaylist = viewModel::addTrackToPlaylist,
                                onCreatePlaylistWithTrack = viewModel::createPlaylistWithTrack,
                            )
                        }
                    }
                }
            }
        }

        if (playerExpanded && state.currentTrack != null) {
            ModalBottomSheet(
                onDismissRequest = { playerExpanded = false }, sheetState = playerSheetState,
                modifier = Modifier.fillMaxSize(), sheetMaxWidth = Dp.Unspecified,
                shape = RectangleShape, dragHandle = null,
                containerColor = MaterialTheme.colorScheme.background, tonalElevation = 0.dp,
                contentWindowInsets = { WindowInsets(0) },
            ) {
                PlayerScreen(
                    state = state, modifier = Modifier.fillMaxHeight(),
                    onCollapse = { scope.launch { playerSheetState.hide(); playerExpanded = false } },
                    onToggle = viewModel::toggle, onPrevious = viewModel::previous, onNext = viewModel::next,
                    onSeek = viewModel::seekTo, onMode = viewModel::setPlaybackMode,
                    onSelectUpcoming = viewModel::seekToQueueItem,
                    onOpenAlbum = { track ->
                        scope.launch {
                            playerSheetState.hide()
                            playerExpanded = false
                            openAlbum(track, returnToPlayer = true)
                        }
                    },
                )
            }
        }
    }
}
