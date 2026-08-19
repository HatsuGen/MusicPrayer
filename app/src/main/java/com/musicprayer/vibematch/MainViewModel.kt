package com.musicprayer.vibematch

import android.app.Application
import android.net.Uri
import android.os.SystemClock
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.musicprayer.vibematch.data.*
import com.musicprayer.vibematch.model.ListeningRecord
import com.musicprayer.vibematch.model.Playlist
import com.musicprayer.vibematch.model.Track
import com.musicprayer.vibematch.playback.PlaybackConnection
import com.musicprayer.vibematch.playback.PlaybackMode
import com.musicprayer.vibematch.playback.PlaybackState
import com.musicprayer.vibematch.smartshuffle.SmartQueueGenerator
import com.musicprayer.vibematch.usbaudio.UsbDac
import com.musicprayer.vibematch.usbaudio.UsbDacMonitor
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class MainUiState(
    val tracks: List<Track> = emptyList(), val query: String = "",
    val playback: PlaybackState = PlaybackState(), val usbDacs: List<UsbDac> = emptyList(),
    val preferences: AppPreferencesState = AppPreferencesState(), val playlists: List<Playlist> = emptyList(),
    val history: Map<Long, ListeningRecord> = emptyMap(),
    val isScanning: Boolean = false,
    val libraryError: String? = null,
) {
    val visibleTracks get() = tracks.filter { query.isBlank() || listOf(it.title, it.artist, it.album, it.genre).any { value -> value.contains(query, true) } }
    val currentTrack get() = tracks.firstOrNull { it.id == playback.currentId }
    val continueListeningTracks: List<Track> get() {
        val recent = tracks.filter { history[it.id]?.lastPlayedAt ?: 0 > 0 }.sortedByDescending { history[it.id]?.lastPlayedAt }.take(3)
        val unfinished = tracks.filter { history[it.id]?.isUnfinished == true }.sortedByDescending { history[it.id]?.lastPlayedAt }
        val looped = tracks.filter { (history[it.id]?.loopCount ?: 0) > 0 }.sortedByDescending { history[it.id]?.loopCount }
        return (recent + unfinished + looped).distinctBy(Track::id).take(6)
    }
}

private data class CoreState(val tracks: List<Track>, val query: String, val playback: PlaybackState, val dacs: List<UsbDac>, val preferences: AppPreferencesState)
private data class LibraryStatus(val scanning: Boolean = false, val error: String? = null)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val database = MusicDatabase.create(application)
    private val repository = MusicRepository(database.tracks(), MediaStoreScanner(application))
    private val preferences = AppPreferences(application)
    private val historyStore = ListeningHistoryStore(application)
    private val playlistStore = PlaylistStore(application)
    private val playback = PlaybackConnection(
        application,
        onLoop = historyStore::recordLoop,
        onTrackStarted = historyStore::recordPlay,
    )
    private val usbMonitor = UsbDacMonitor(application)
    private val smartQueueGenerator = SmartQueueGenerator()
    private val query = MutableStateFlow("")
    private val libraryStatus = MutableStateFlow(LibraryStatus())
    private val core = combine(repository.tracks, query, playback.state, usbMonitor.devices, preferences.state, ::CoreState)

    val state: StateFlow<MainUiState> = combine(core, playlistStore.playlists, historyStore.records, libraryStatus) { value, playlists, history, library ->
        MainUiState(value.tracks, value.query, value.playback, value.dacs, value.preferences, playlists, history, library.scanning, library.error)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MainUiState())

    init {
        playback.connect(); usbMonitor.start()
        viewModelScope.launch {
            var lastSavedAt = 0L
            playback.state.collect { player ->
                val now = SystemClock.elapsedRealtime()
                if (player.currentId != null && (!player.isPlaying || now - lastSavedAt >= 5_000)) {
                    historyStore.saveProgress(player.currentId, player.positionMs, player.durationMs); lastSavedAt = now
                }
            }
        }
    }

    fun refresh() { preferences.state.value.folderUri?.let { uri -> scan(Uri.parse(uri), updateFolder = false) } }
    fun selectFolder(uri: Uri) {
        val resolver = getApplication<Application>().contentResolver
        val name = runCatching { resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { if (it.moveToFirst()) it.getString(0) else null } }.getOrNull() ?: uri.lastPathSegment
        scan(uri, updateFolder = true, folderName = name)
    }
    fun reportFolderError(message: String) { libraryStatus.value = LibraryStatus(error = message) }
    fun clearFolder() { preferences.setFolder(null, null); viewModelScope.launch { repository.clear() } }

    private fun scan(uri: Uri, updateFolder: Boolean, folderName: String? = null) {
        if (libraryStatus.value.scanning) return
        libraryStatus.value = LibraryStatus(scanning = true)
        viewModelScope.launch {
            val oldUri = preferences.state.value.folderUri
            runCatching { repository.refresh(uri) }
                .onSuccess {
                    if (updateFolder) {
                        preferences.setFolder(uri.toString(), folderName)
                        oldUri?.takeIf { it != uri.toString() }?.let { previous -> runCatching { getApplication<Application>().contentResolver.releasePersistableUriPermission(Uri.parse(previous), android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) } }
                    }
                    libraryStatus.value = LibraryStatus()
                }
                .onFailure { error ->
                    if (updateFolder && uri.toString() != oldUri) runCatching { getApplication<Application>().contentResolver.releasePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) }
                    libraryStatus.value = LibraryStatus(error = error.localizedMessage ?: "The selected folder could not be scanned")
                }
        }
    }
    fun search(value: String) { query.value = value }
    fun playSolo(track: Track, candidates: List<Track> = state.value.tracks) = playback.playSolo(track, candidates)
    fun playContext(track: Track, queue: List<Track>, title: String) = playback.playContext(track, queue, title)
    fun playPlaylist(playlist: Playlist) {
        val tracksById = state.value.tracks.associateBy(Track::id)
        val tracks = playlist.trackIds.mapNotNull(tracksById::get)
        tracks.firstOrNull()?.let { playContext(it, tracks, "Playlist: ${playlist.name}") }
    }
    fun smartShuffle(track: Track) = playContext(track, smartQueueGenerator.generate(track, state.value.tracks), "Smart queue")
    fun toggle() = playback.toggle(); fun next() = playback.next(); fun previous() = playback.previous()
    fun seekTo(positionMs: Long) = playback.seekTo(positionMs)
    fun seekToQueueItem(trackId: Long) = playback.seekToQueueItem(trackId)
    fun playNext(track: Track) {
        if (state.value.currentTrack == null) playSolo(track) else playback.playNext(track)
    }
    fun addToQueue(track: Track) {
        if (state.value.currentTrack == null) playSolo(track) else playback.addToQueue(track)
    }
    fun setPlaybackMode(mode: PlaybackMode) = playback.setMode(mode)

    fun selectTheme(name: String) = preferences.selectTheme(name)
    fun setShowTopMixes(show: Boolean) = preferences.setShowTopMixes(show)
    fun setEqualizerEnabled(enabled: Boolean) = preferences.setEqualizerEnabled(enabled)
    fun setPreamp(value: Float) = preferences.setPreamp(value)
    fun setEqBand(index: Int, value: Float) = preferences.setEqBand(index, value)
    fun resetEqualizer() = preferences.resetEqualizer()
    fun createPlaylist(name: String) = playlistStore.create(name)
    fun createPlaylistWithTrack(name: String, trackId: Long) = playlistStore.createWithTrack(name, trackId)
    fun deletePlaylist(id: Long) = playlistStore.delete(id)
    fun addTrackToPlaylist(playlistId: Long, trackId: Long) = playlistStore.addTrack(playlistId, trackId)
    fun removeTrackFromPlaylist(playlistId: Long, trackId: Long) = playlistStore.removeTrack(playlistId, trackId)

    override fun onCleared() { playback.disconnect(); usbMonitor.stop(); database.close() }
}
